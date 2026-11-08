package com.diego.vitther.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diego.vitther.VittherApp
import com.diego.vitther.data.repository.PiezaRepository
import com.diego.vitther.domain.EstadoPieza
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Estado observable y ViewModel propio para la pantalla de Perfil en presentation/perfil/
 * conforme a las secciones 2, 5, 6 y 7 de la guía técnica MVVM.
 */
data class PerfilUiState(
    val cargando: Boolean = true,
    val totalPiezas: Int = 0,
    val totalCategorias: Int = 0,
    val piezasEnExhibicion: Int = 0,
    val piezasEnBodega: Int = 0,
    val valorTotalCentavos: Long = 0L,
    val primeraAdquisicionMillis: Long? = null,
    val ocultarValorEstimado: Boolean = true,
    val estadoSincronizacionCloud: String? = null,
    val mensajeError: String? = null
)

class PerfilViewModel(
    private val piezaRepository: PiezaRepository
) : ViewModel() {

    private val ocultarValor = MutableStateFlow(true)
    private val estadoCloud = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PerfilUiState> = combine(
        piezaRepository.observarTodas(),
        piezaRepository.observarTotalFranquicias(),
        piezaRepository.observarPrimeraAdquisicion(),
        ocultarValor,
        estadoCloud
    ) { piezas, categorias, primera, ocultar, cloud ->
        PerfilUiState(
            cargando = false,
            totalPiezas = piezas.size,
            totalCategorias = categorias,
            piezasEnExhibicion = piezas.count { it.estado == EstadoPieza.EXHIBICION },
            piezasEnBodega = piezas.count { it.estado == EstadoPieza.BODEGA },
            valorTotalCentavos = piezas.sumOf { it.valorEstimadoCentavos ?: 0L },
            primeraAdquisicionMillis = primera,
            ocultarValorEstimado = ocultar,
            estadoSincronizacionCloud = cloud,
            mensajeError = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PerfilUiState(cargando = true)
    )

    fun alternarPrivacidadValor() {
        ocultarValor.value = !ocultarValor.value
    }

    fun actualizarMensajeSincronizacion(mensaje: String?) {
        estadoCloud.value = mensaje
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VittherApp
                PerfilViewModel(app.container.piezaRepository)
            }
        }
    }
}
