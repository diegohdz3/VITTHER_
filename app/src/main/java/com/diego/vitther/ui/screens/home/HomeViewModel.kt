package com.diego.vitther.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diego.vitther.VittherApp
import com.diego.vitther.di.AppContainer
import com.diego.vitther.domain.diasHastaProximaCapsula
import com.diego.vitther.domain.model.ResumenMuseoModel
import com.diego.vitther.domain.model.calcularResumenMuseo
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

data class PiezaHome(
    val id: Long,
    val nombre: String,
    val franquicia: String,
    val foto: File? = null,
    val linea: String? = null,
    val edicionLimitada: Boolean = false,
    val numeroEdicion: String? = null
)

data class ProximaCapsulaHome(
    val piezaId: Long,
    val nombrePieza: String,
    val diasRestantes: Long,
    val progresoAnillo: Float
)

data class RecuerdoHoyHome(
    val piezaId: Long,
    val nombrePieza: String,
    val nota: String,
    val haceDias: Long
)

sealed interface HomeUiState {
    data object Cargando : HomeUiState
    data class Vacia(
        val diasHastaCapsula: Long? = null,
        val resumen: ResumenMuseoModel = ResumenMuseoModel(),
        val proximaCapsula: ProximaCapsulaHome? = null,
        val recuerdoHoy: RecuerdoHoyHome? = null
    ) : HomeUiState
    data class Contenido(
        val piezas: List<PiezaHome>,
        val diasHastaCapsula: Long? = null,
        val resumen: ResumenMuseoModel = ResumenMuseoModel(),
        val proximaCapsula: ProximaCapsulaHome? = null,
        val recuerdoHoy: RecuerdoHoyHome? = null
    ) : HomeUiState
    data object Error : HomeUiState
}

class HomeViewModel(container: AppContainer) : ViewModel() {
    private val piezasVitrina = container.piezaRepository.observarVitrina().map { lista ->
        lista.map {
            PiezaHome(
                id = it.id,
                nombre = it.nombre,
                franquicia = it.franquicia,
                foto = container.fotos.archivo(it.fotoPath),
                linea = it.linea,
                edicionLimitada = it.edicionLimitada,
                numeroEdicion = it.numeroEdicion
            )
        }
    }.flowOn(Dispatchers.IO)

    private val todasPiezas = container.piezaRepository.observarTodas().flowOn(Dispatchers.IO)

    private val reloj = flow {
        while (true) {
            val ahora = System.currentTimeMillis()
            emit(ahora)
            delay(60_000 - ahora % 60_000)
        }
    }

    val estado: StateFlow<HomeUiState> = combine(
        piezasVitrina, container.capsulaRepository.observarTodas(), todasPiezas, reloj
    ) { lista, capsulas, todas, ahora ->
        val resumen = todas.calcularResumenMuseo()
        val dias = diasHastaProximaCapsula(capsulas, ahora)

        val proximaEntidad = capsulas
            .filter { it.abiertaEn == null }
            .minByOrNull { it.fechaApertura }

        val proximaCapsula = proximaEntidad?.let { cap ->
            val piezaAsociada = todas.find { it.id == cap.piezaId }
            val diasRestantes = dias ?: 0L
            val progreso = (1f - (diasRestantes.toFloat() / 30f)).coerceIn(0.22f, 1f)
            ProximaCapsulaHome(
                piezaId = cap.piezaId,
                nombrePieza = piezaAsociada?.nombre ?: "tu colección",
                diasRestantes = diasRestantes,
                progresoAnillo = progreso
            )
        }

        val piezaRecuerdo = todas
            .filter { !it.notaPersonal.isNullOrBlank() }
            .maxByOrNull { it.fechaAdquisicion }
            ?: todas.maxByOrNull { it.fechaAdquisicion }

        val recuerdoHoy = piezaRecuerdo?.let { p ->
            val diasDesde = TimeUnit.MILLISECONDS.toDays((ahora - p.fechaAdquisicion).coerceAtLeast(0L)).coerceAtLeast(1L)
            RecuerdoHoyHome(
                piezaId = p.id,
                nombrePieza = p.nombre,
                nota = p.notaPersonal?.takeIf { it.isNotBlank() }
                    ?: "Llegó antes de lo que esperaba. Ya tiene su lugar.",
                haceDias = diasDesde
            )
        }

        if (lista.isEmpty()) HomeUiState.Vacia(dias, resumen, proximaCapsula, recuerdoHoy)
        else HomeUiState.Contenido(lista, dias, resumen, proximaCapsula, recuerdoHoy)
    }.distinctUntilChanged()
        .catch { emit(HomeUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), HomeUiState.Cargando)

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel((this[APPLICATION_KEY] as VittherApp).container) }
        }
    }
}
