package com.diego.vitther.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diego.vitther.VittherApp
import com.diego.vitther.data.auth.VittherUserAccount
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.data.remote.api.VittherRemoteApiClient
import com.diego.vitther.data.remote.api.toCloudDto
import com.diego.vitther.data.remote.dto.ColeccionCompartidaDto
import com.diego.vitther.data.remote.dto.RespaldoFirebaseDto
import com.diego.vitther.data.repository.CatalogoRemotoRepository
import com.diego.vitther.data.repository.FichaCatalogoRemoto
import com.diego.vitther.data.repository.ResultadoSincronizacionFirebase
import com.diego.vitther.di.AppContainer
import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.domain.EstadoWish
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ColeccionViewModel(private val container: AppContainer) : ViewModel() { // INYECCION AL VIEW MODEL
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

    // Autenticación con Google y exposiciones compartidas en solo lectura
    val usuarioActual = container.authManager.usuarioActual
    val cuentasRecordadas = container.authManager.cuentasRecordadas

    private val _exposicionInvitada = MutableStateFlow<ColeccionCompartidaDto?>(null)
    val exposicionInvitada = _exposicionInvitada.asStateFlow()
    private val exposicionesLocalesCache = mutableMapOf<String, ColeccionCompartidaDto>()

    fun iniciarSesionGoogle(nombre: String, correo: String): VittherUserAccount =
        container.authManager.iniciarSesionConGoogle(nombre, correo)

    fun seleccionarCuentaGoogle(cuenta: VittherUserAccount): VittherUserAccount =
        container.authManager.seleccionarCuentaRecordada(cuenta)

    fun cerrarSesionGoogle() {
        container.authManager.cerrarSesion()
        _estadoFirebase.value = null
    }

    fun cerrarExposicionInvitada() {
        _exposicionInvitada.value = null
    }

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

    private fun resolverCatalogoRepo(urlPersonalizada: String?): CatalogoRemotoRepository =
        if (!urlPersonalizada.isNullOrBlank()) {
            val client = VittherRemoteApiClient(
                firebaseBaseUrl = urlPersonalizada.trim().removeSuffix("/")
            )
            CatalogoRemotoRepository(client, client)
        } else {
            container.catalogoRemotoRepository
        }

    private suspend fun respaldarConFotos(
        repo: CatalogoRemotoRepository,
        cuenta: VittherUserAccount?
    ): ResultadoSincronizacionFirebase {
        val actualesPiezas = container.piezaRepository.observarTodas().first()
        val actualesCapsulas = container.capsulaRepository.observarTodas().first()
        val actualesWishes = container.wishRepository.observarTodos().first()
        val fotosPiezas = actualesPiezas.associate { it.id to container.fotos.codificarBase64(it.fotoPath) }
        val fotosWishes = actualesWishes.associate { it.id to container.fotos.codificarBase64(it.fotoPath) }
        return repo.respaldarEnFirebase(
            piezas = actualesPiezas,
            capsulas = actualesCapsulas,
            wishes = actualesWishes,
            coleccionista = cuenta?.nombre ?: "Curador VITTHER",
            fotosPiezas = fotosPiezas,
            fotosWishes = fotosWishes,
            uid = cuenta?.uid
        )
    }

    private suspend fun restaurarSnapshot(respaldo: RespaldoFirebaseDto): Pair<Int, Int> {
        val piezasLocales = container.piezaRepository.observarTodas().first()
        val wishesLocales = container.wishRepository.observarTodos().first()
        var piezasRestauradas = 0
        var wishesRestaurados = 0

        for (dto in respaldo.piezas) {
            val existe = piezasLocales.any {
                it.nombre.equals(dto.nombre.trim(), ignoreCase = true) &&
                    it.fechaAdquisicion == dto.fechaAdquisicion
            }
            if (!existe) {
                val rutaFoto = container.fotos.decodificarBase64(dto.fotoBase64)
                val estadoPieza = EstadoPieza.entries.find { it.name == dto.estado } ?: EstadoPieza.EXHIBICION
                val entidad = PiezaEntity(
                    nombre = dto.nombre,
                    franquicia = dto.franquicia,
                    linea = dto.linea,
                    fechaAdquisicion = dto.fechaAdquisicion,
                    fotoPath = rutaFoto,
                    estado = estadoPieza,
                    destacada = dto.destacada,
                    ubicacion = dto.ubicacion,
                    procedencia = dto.procedencia,
                    notaPersonal = dto.notaPersonal,
                    edicionLimitada = dto.edicionLimitada,
                    numeroEdicion = dto.numeroEdicion,
                    valorEstimadoCentavos = dto.valorEstimadoCentavos
                )
                runCatching { container.piezaRepository.guardar(entidad) }
                    .onSuccess { piezasRestauradas++ }
            }
        }

        for (dto in respaldo.wishes) {
            val existe = wishesLocales.any {
                it.nombre.equals(dto.nombre.trim(), ignoreCase = true) &&
                    it.franquicia.equals(dto.franquicia.trim(), ignoreCase = true)
            }
            if (!existe) {
                val rutaFoto = container.fotos.decodificarBase64(dto.fotoBase64)
                val estadoWish = EstadoWish.entries.find { it.name == dto.estado } ?: EstadoWish.DESEADA
                val entidad = WishEntity(
                    nombre = dto.nombre,
                    franquicia = dto.franquicia,
                    linea = dto.linea,
                    estado = estadoWish,
                    fechaEstimadaLlegada = dto.fechaEstimadaLlegada,
                    progresoPorcentaje = dto.progresoPorcentaje,
                    metaPesos = dto.metaPesos,
                    fotoPath = rutaFoto,
                    creadoEn = dto.creadoEn
                )
                runCatching { container.wishRepository.guardar(entidad) }
                    .onSuccess { wishesRestaurados++ }
            }
        }
        return piezasRestauradas to wishesRestaurados
    }

    fun sincronizarConFirebase(urlPersonalizada: String? = null) = ejecutar {
        val repo = resolverCatalogoRepo(urlPersonalizada)
        val resultado = respaldarConFotos(repo, usuarioActual.value)
        _estadoFirebase.value = resultado.mensaje
    }

    fun restaurarDesdeFirebase(urlPersonalizada: String? = null) = ejecutar {
        val repo = resolverCatalogoRepo(urlPersonalizada)
        val uid = usuarioActual.value?.uid
        val respaldo = repo.obtenerRespaldoDeFirebase(uid)
        if (respaldo == null) {
            _estadoFirebase.value = "No se encontró ningún respaldo en la nube para tu cuenta."
            return@ejecutar
        }
        val (piezasRestauradas, wishesRestaurados) = restaurarSnapshot(respaldo)
        _estadoFirebase.value = "Colección restaurada: $piezasRestauradas piezas nuevas (de ${respaldo.piezas.size}) y $wishesRestaurados wishes (de ${respaldo.wishes.size})."
    }

    fun sincronizarAlIniciarSesion(cuenta: VittherUserAccount) {
        viewModelScope.launch {
            runCatching {
                val repo = container.catalogoRemotoRepository
                val respaldo = repo.obtenerRespaldoDeFirebase(cuenta.uid)
                if (respaldo != null) {
                    val (p, w) = restaurarSnapshot(respaldo)
                    if (p > 0 || w > 0) {
                        _estadoFirebase.value = "Recuperamos $p piezas y $w wishes de tu cuenta."
                    }
                } else {
                    val locales = container.piezaRepository.observarTodas().first()
                    if (locales.isNotEmpty()) {
                        respaldarConFotos(repo, cuenta)
                    }
                }
            }
        }
    }

    fun compartirSalaOColeccion(
        franquicia: String?,
        uid: String,
        curador: String,
        nombreMuseo: String,
        piezasExplicitas: List<PiezaEntity>? = null,
        tituloPersonalizado: String? = null,
        onListo: (codigo: String, mensajeShare: String) -> Unit
    ) = ejecutar {
        val elegidas = if (!piezasExplicitas.isNullOrEmpty()) {
            piezasExplicitas
        } else {
            val todas = container.piezaRepository.observarTodas().first()
            if (!franquicia.isNullOrBlank()) {
                todas.filter { it.franquicia.equals(franquicia.trim(), ignoreCase = true) }
            } else {
                todas.filter { it.estado == EstadoPieza.EXHIBICION }.ifEmpty { todas }
            }
        }
        check(elegidas.isNotEmpty()) {
            "No hay piezas disponibles para compartir en esta exposición."
        }

        val fuenteCodigo = tituloPersonalizado?.trim()?.takeIf { it.isNotBlank() }
            ?: franquicia?.trim()?.takeIf { it.isNotBlank() }
            ?: "MUSEO"
        val prefijoBase = fuenteCodigo
            .let { Normalizer.normalize(it, Normalizer.Form.NFD) }
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9]"), "")
            .take(8)
            .ifBlank { "VITTHER" }
        val sufijo = uid.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(4)
            .ifBlank { "EXPO" }
        val codigo = "$prefijoBase-$sufijo"

        val ahoraMs = System.currentTimeMillis()
        val piezasDto = elegidas.map { p ->
            val fotoB64 = container.fotos.codificarBase64(p.fotoPath)
            // Por privacidad nunca se comparte el valor monetario estimado con visitantes
            p.toCloudDto(ahoraMs, fotoB64).copy(valorEstimadoCentavos = null)
        }

        val tituloExposicion = when {
            !tituloPersonalizado.isNullOrBlank() -> tituloPersonalizado.trim()
            !franquicia.isNullOrBlank() -> "Colección ${franquicia.trim()}"
            else -> nombreMuseo.ifBlank { "Museo de $curador" }
        }

        val expo = ColeccionCompartidaDto(
            codigo = codigo,
            propietarioUid = uid,
            propietarioNombre = curador,
            nombreMuseo = nombreMuseo,
            tituloExposicion = tituloExposicion,
            franquiciaFiltro = franquicia?.trim()?.takeIf { it.isNotBlank() },
            compartidoEn = ahoraMs,
            piezas = piezasDto
        )

        exposicionesLocalesCache[codigo] = expo
        val publicadaEnNube = container.catalogoRemotoRepository.publicarExposicionCompartida(expo)
        if (!publicadaEnNube) {
            _estadoFirebase.value = "Exposición lista en código $codigo (conecta a internet para compartir con otros teléfonos)."
        } else {
            _estadoFirebase.value = "Exposición compartida ($codigo)."
        }

        val mensajeShare = buildString {
            append("Te invito a visitar mi $tituloExposicion en VITTHER (Solo lectura).\n\n")
            append("Abre VITTHER → Colección → Visitar colección e ingresa este código:\n")
            append("$codigo\n\n")
            append("Link de acceso: https://vitther.app/expo/$codigo")
        }
        onListo(codigo, mensajeShare)
    }

    fun abrirExposicionPorCodigo(entrada: String) = ejecutar {
        val codigo = extraerCodigoExposicion(entrada)
        check(codigo.isNotBlank()) { "Ingresa un código o link válido de exposición VITTHER." }
        val expo = container.catalogoRemotoRepository.obtenerExposicionCompartida(codigo)
            ?: exposicionesLocalesCache[codigo]
        check(expo != null) { "No encontramos ninguna colección compartida con el código \"$codigo\"." }
        _exposicionInvitada.value = expo
    }

    private fun extraerCodigoExposicion(entrada: String): String {
        val limpia = entrada.trim().uppercase(Locale.US)
        val regexCodigo = Regex("[A-Z0-9]{2,10}-[A-Z0-9]{3,8}")
        regexCodigo.find(limpia)?.value?.let { return it }
        if (limpia.contains("/EXPO/")) {
            return limpia.substringAfterLast("/EXPO/")
                .substringBefore("?")
                .substringBefore("/")
                .replace(Regex("[^A-Z0-9_-]"), "")
        }
        return limpia.replace(Regex("[^A-Z0-9_-]"), "")
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
        container.sincronizarEnSegundoPlano()
        terminado(id)
    }
    fun eliminar(pieza: PiezaEntity, terminado: () -> Unit) = ejecutar {
        container.piezaRepository.eliminar(pieza)
        container.fotos.eliminar(pieza.fotoPath)
        container.sincronizarEnSegundoPlano()
        terminado()
    }
    fun mover(pieza: PiezaEntity) = ejecutar {
        container.piezaRepository.moverA(pieza.id,
            if (pieza.estado == EstadoPieza.EXHIBICION) EstadoPieza.BODEGA else EstadoPieza.EXHIBICION)
        container.sincronizarEnSegundoPlano()
    }
    fun destacar(pieza: PiezaEntity) = ejecutar {
        container.piezaRepository.destacar(pieza.id, !pieza.destacada)
        container.sincronizarEnSegundoPlano()
    }
    fun sellar(piezaId: Long, titulo: String, nota: String, fecha: Long, terminado: () -> Unit) = ejecutar {
        container.capsulaRepository.sellar(piezaId, titulo, nota, fecha)
        container.sincronizarEnSegundoPlano()
        terminado()
    }
    fun abrirCapsula(id: Long) = ejecutar {
        check(container.capsulaRepository.abrir(id)) { "Esta cápsula todavía no está disponible." }
        container.sincronizarEnSegundoPlano()
    }
    companion object {
        val Factory = viewModelFactory {
            initializer { ColeccionViewModel((this[APPLICATION_KEY] as VittherApp).container) }
        }
    }
}
