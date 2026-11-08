package com.diego.vitther.presentation.historia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diego.vitther.VittherApp
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.repository.CapsulaRepository
import com.diego.vitther.data.repository.PiezaRepository
import com.diego.vitther.domain.Hito
import com.diego.vitther.domain.historiaDe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Estado observable y ViewModel propio de la pantalla de Historia en presentation/historia/
 * cumpliendo la regla de un ViewModel por pantalla con estado de carga, contenido y error.
 */
data class HistoriaUiState(
    val cargando: Boolean = true,
    val hitos: List<Hito> = emptyList(),
    val piezas: List<PiezaEntity> = emptyList(),
    val mensajeError: String? = null
)

class HistoriaViewModel(
    piezaRepository: PiezaRepository,
    capsulaRepository: CapsulaRepository
) : ViewModel() {

    val uiState: StateFlow<HistoriaUiState> = combine(
        piezaRepository.observarTodas(),
        capsulaRepository.observarTodas()
    ) { piezas, capsulas ->
        HistoriaUiState(
            cargando = false,
            hitos = historiaDe(piezas, capsulas),
            piezas = piezas,
            mensajeError = null
        )
    }.catch { error ->
        emit(
            HistoriaUiState(
                cargando = false,
                mensajeError = error.message ?: "No se pudo cargar la línea del tiempo."
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoriaUiState(cargando = true)
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VittherApp
                HistoriaViewModel(
                    piezaRepository = app.container.piezaRepository,
                    capsulaRepository = app.container.capsulaRepository
                )
            }
        }
    }
}
