package com.diego.vitther.presentation.wishes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diego.vitther.VittherApp
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.data.media.FotoStore
import com.diego.vitther.data.repository.PiezaRepository
import com.diego.vitther.data.repository.WishRepository
import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.domain.EstadoWish
import com.diego.vitther.domain.Fechas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class WishesUiState(
    val cargando: Boolean = true,
    val porLlegar: List<WishEntity> = emptyList(),
    val deseadas: List<WishEntity> = emptyList(),
    val mensajeToast: String? = null,
    val mensajeError: String? = null
)

class WishesViewModel(
    private val wishRepository: WishRepository,
    private val piezaRepository: PiezaRepository,
    private val fotoStore: FotoStore? = null,
    private val onSyncFirebase: () -> Unit = {}
) : ViewModel() {

    private val toast = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WishesUiState> = combine(
        wishRepository.observarTodos(),
        toast,
        error
    ) { todos, msgToast, msgError ->
        val listaPorLlegar = todos
            .filter { it.estado == EstadoWish.POR_LLEGAR }
            .sortedBy { it.fechaEstimadaLlegada ?: Long.MAX_VALUE }
        val listaDeseadas = todos
            .filter { it.estado == EstadoWish.DESEADA }
            .sortedByDescending { it.creadoEn }

        WishesUiState(
            cargando = false,
            porLlegar = listaPorLlegar,
            deseadas = listaDeseadas,
            mensajeToast = msgToast,
            mensajeError = msgError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WishesUiState(cargando = true)
    )

    fun limpiarToast() {
        toast.value = null
    }

    fun limpiarError() {
        error.value = null
    }

    fun archivo(path: String?): File? = fotoStore?.archivo(path)

    fun importarFoto(uri: Uri, onListo: (String) -> Unit) {
        val store = fotoStore ?: return
        viewModelScope.launch {
            runCatching { store.importar(uri) }
                .onSuccess { path -> onListo(path) }
                .onFailure { error.value = "No se pudo importar la foto de referencia." }
        }
    }

    fun guardarWish(
        id: Long = 0L,
        nombre: String,
        franquicia: String,
        linea: String?,
        estado: EstadoWish,
        diasLlegada: Long? = null,
        metaPesos: Long? = null,
        fechaCompraMillis: Long? = null,
        fechaLlegadaMillis: Long? = null,
        fotoPath: String? = null,
        creadoEnOriginal: Long? = null
    ) {
        viewModelScope.launch {
            runCatching {
                val hoyMillis = Fechas.desdeFecha(LocalDate.now())
                val fechaEstimada = when {
                    estado != EstadoWish.POR_LLEGAR -> null
                    fechaLlegadaMillis != null -> fechaLlegadaMillis
                    else -> {
                        val dias = (diasLlegada ?: 30L).coerceAtLeast(1L)
                        Fechas.desdeFecha(LocalDate.now().plusDays(dias))
                    }
                }
                val progreso = if (estado == EstadoWish.POR_LLEGAR && fechaEstimada != null) {
                    val inicio = fechaCompraMillis ?: (hoyMillis - 15L * 86_400_000L)
                    val total = (fechaEstimada - inicio).coerceAtLeast(86_400_000L).toDouble()
                    val transcurrido = (hoyMillis - inicio).coerceAtLeast(0L).toDouble()
                    ((transcurrido / total) * 100.0).toInt().coerceIn(8, 95)
                } else 0

                val entidad = WishEntity(
                    id = id,
                    nombre = nombre,
                    franquicia = franquicia,
                    linea = linea,
                    fotoPath = fotoPath,
                    estado = estado,
                    fechaEstimadaLlegada = fechaEstimada,
                    progresoPorcentaje = progreso,
                    metaPesos = metaPesos,
                    creadoEn = creadoEnOriginal ?: System.currentTimeMillis()
                )
                if (id == 0L) wishRepository.guardar(entidad) else wishRepository.actualizar(entidad)
                onSyncFirebase()
                toast.value = when {
                    id != 0L -> "Actualizado"
                    estado == EstadoWish.POR_LLEGAR -> "Lugar reservado en el museo"
                    else -> "Añadida a Deseadas"
                }
            }.onFailure {
                error.value = it.message ?: "No se pudo guardar el wish."
            }
        }
    }

    fun hacerGranDeseo(wish: WishEntity) {
        viewModelScope.launch {
            val maxCreado = uiState.value.deseadas.maxOfOrNull { it.creadoEn } ?: System.currentTimeMillis()
            val actualizado = wish.copy(creadoEn = maxOf(System.currentTimeMillis(), maxCreado) + 1000L)
            wishRepository.actualizar(actualizado)
            onSyncFirebase()
            toast.value = "${wish.nombre} es ahora tu gran deseo"
        }
    }

    fun marcarYaLaCompre(wish: WishEntity) {
        viewModelScope.launch {
            val fechaEstimada = Fechas.desdeFecha(LocalDate.now().plusDays(30))
            wishRepository.pasarAPorLlegar(wish, fechaEstimada)
            onSyncFirebase()
            toast.value = "Movida a Por llegar"
        }
    }

    fun marcarYaLlego(wish: WishEntity, alIncorporar: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val nuevaPieza = PiezaEntity(
                nombre = wish.nombre,
                franquicia = wish.franquicia,
                linea = wish.linea,
                fotoPath = wish.fotoPath,
                fechaAdquisicion = Fechas.desdeFecha(LocalDate.now()),
                estado = EstadoPieza.EXHIBICION,
                procedencia = "Preventa · Wishes",
                valorEstimadoCentavos = wish.metaPesos?.times(100L)
            )
            val nuevoId = piezaRepository.guardar(nuevaPieza)
            wishRepository.eliminar(wish)
            onSyncFirebase()
            toast.value = "✦ ${wish.nombre} llegó a tu museo"
            alIncorporar(nuevoId)
        }
    }

    fun eliminarWish(wish: WishEntity) {
        viewModelScope.launch {
            wishRepository.eliminar(wish)
            onSyncFirebase()
            toast.value = "Eliminada"
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VittherApp
                WishesViewModel(
                    wishRepository = app.container.wishRepository,
                    piezaRepository = app.container.piezaRepository,
                    fotoStore = app.container.fotos,
                    onSyncFirebase = { app.container.sincronizarEnSegundoPlano() }
                )
            }
        }
    }
}
