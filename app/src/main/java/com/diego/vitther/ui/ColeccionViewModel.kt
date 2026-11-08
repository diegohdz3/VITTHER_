package com.diego.vitther.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diego.vitther.VittherApp
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.repository.FichaCatalogoRemoto
import com.diego.vitther.di.AppContainer
import com.diego.vitther.domain.EstadoPieza
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ColeccionViewModel(private val container: AppContainer) : ViewModel() {
    // Null distingue la carga inicial de una colección realmente vacía.
    val piezas = container.piezaRepository.observarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val capsulas = container.capsulaRepository.observarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val wishes = container.wishRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // El contador es en días y la disponibilidad se revisa cada pocos segundos:
    // no hace falta recomponer la pantalla sesenta veces por minuto.
    val ahora = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(5_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), System.currentTimeMillis())
    private val _ocupado = MutableStateFlow(false)
    val ocupado = _ocupado.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Estado para la consulta a la API externa (Jikan REST API v4) y sincronización con Firebase.
    private val _sugerenciasRemotas = MutableStateFlow<List<FichaCatalogoRemoto>>(emptyList())
    val sugerenciasRemotas = _sugerenciasRemotas.asStateFlow()
    private val _buscandoApi = MutableStateFlow(false)
    val buscandoApi = _buscandoApi.asStateFlow()
    private val _estadoFirebase = MutableStateFlow<String?>(null)
    val estadoFirebase = _estadoFirebase.asStateFlow()

    fun limpiarError() { _error.value = null }
    fun limpiarSugerenciasRemotas() { _sugerenciasRemotas.value = emptyList() }
    fun archivo(ruta: String?) = container.fotos.archivo(ruta)

    private fun ejecutar(accion: suspend () -> Unit) {
        if (_ocupado.value) return
        _ocupado.value = true
        viewModelScope.launch {
            try { accion() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.message ?: "No se pudo completar la operación. Inténtalo de nuevo." }
            finally { _ocupado.value = false }
        }
    }

    fun buscarEnApiCatalogo(consulta: String) {
        if (consulta.isBlank() || _buscandoApi.value) return
        _buscandoApi.value = true
        viewModelScope.launch {
            try {
                container.catalogoRemotoRepository.buscarEnCatalogoOnline(consulta)
                    .onSuccess { lista ->
                        _sugerenciasRemotas.value = lista
                        if (lista.isEmpty()) {
                            _error.value = "No se encontraron coincidencias en el catálogo en línea para \"$consulta\"."
                        }
                    }
                    .onFailure {
                        _error.value = "No fue posible conectar con la API del catálogo externo. Revisa tu conexión a internet."
                    }
            } finally {
                _buscandoApi.value = false
            }
        }
    }

    fun sincronizarConFirebase(urlPersonalizada: String? = null) = ejecutar {
        val actuales = piezas.value.orEmpty()
        val repo = if (!urlPersonalizada.isNullOrBlank()) {
            val client = com.diego.vitther.data.remote.api.VittherRemoteApiClient(firebaseBaseUrl = urlPersonalizada.trim().removeSuffix("/"))
            com.diego.vitther.data.repository.CatalogoRemotoRepository(client, client)
        } else {
            container.catalogoRemotoRepository
        }
        val resultado = repo.respaldarEnFirebase(actuales)
        _estadoFirebase.value = resultado.mensaje
    }

    fun importar(uri: Uri, resultado: (String) -> Unit) = ejecutar {
        resultado(container.fotos.importar(uri))
    }
    fun guardar(pieza: PiezaEntity, terminado: (Long) -> Unit) = ejecutar {
        val anterior = if (pieza.id == 0L) null else container.piezaRepository.obtenerPorId(pieza.id)
        val id = if (pieza.id == 0L) container.piezaRepository.guardar(pieza) else {
            check(anterior != null) { "La pieza ya no existe." }
            container.piezaRepository.actualizar(pieza)
            pieza.id
        }
        // Reemplazar o quitar una foto no debe dejar archivos huérfanos.
        if (anterior?.fotoPath != null && anterior.fotoPath != pieza.fotoPath) {
            container.fotos.eliminar(anterior.fotoPath)
        }
        terminado(id)
    }
    fun eliminar(pieza: PiezaEntity, terminado: () -> Unit) = ejecutar {
        container.piezaRepository.eliminar(pieza)
        container.fotos.eliminar(pieza.fotoPath)
        terminado()
    }
    fun mover(pieza: PiezaEntity) = ejecutar {
        container.piezaRepository.moverA(pieza.id,
            if (pieza.estado == EstadoPieza.EXHIBICION) EstadoPieza.BODEGA else EstadoPieza.EXHIBICION)
    }
    fun destacar(pieza: PiezaEntity) = ejecutar {
        container.piezaRepository.destacar(pieza.id, !pieza.destacada)
    }
    fun sellar(piezaId: Long, titulo: String, nota: String, fecha: Long, terminado: () -> Unit) = ejecutar {
        container.capsulaRepository.sellar(piezaId, titulo, nota, fecha)
        terminado()
    }
    fun abrirCapsula(id: Long) = ejecutar {
        check(container.capsulaRepository.abrir(id)) { "Esta cápsula todavía no está disponible." }
    }
    companion object {
        val Factory = viewModelFactory {
            initializer { ColeccionViewModel((this[APPLICATION_KEY] as VittherApp).container) }
        }
    }
}
