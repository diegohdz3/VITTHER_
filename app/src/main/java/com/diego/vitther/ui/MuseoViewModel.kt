package com.diego.vitther.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diego.vitther.VittherApp
import com.diego.vitther.data.repository.PiezaRepository
import com.diego.vitther.domain.EstadoPieza
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ResumenMuseo(
    val totalPiezas: Int = 0,
    val totalFranquicias: Int = 0,
    val enBodega: Int = 0
)

class MuseoViewModel(repositorio: PiezaRepository) : ViewModel() {

    val resumen: StateFlow<ResumenMuseo> = combine(
        repositorio.observarTotal(),
        repositorio.observarTotalFranquicias(),
        repositorio.observarTodas()
    ) { total, franquicias, todas ->
        ResumenMuseo(
            totalPiezas = total,
            totalFranquicias = franquicias,
            enBodega = todas.count { it.estado == EstadoPieza.BODEGA }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ResumenMuseo()
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VittherApp
                MuseoViewModel(app.container.piezaRepository)
            }
        }
    }
}
