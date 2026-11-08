package com.diego.vitther.presentation.wishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diego.vitther.VittherApp
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.local.WishEntity
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
    private val piezaRepository: PiezaRepository
) : ViewModel() {

    private val toast = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WishesUiState> = combine(
        wishRepository.observarTodos(),
        toast,
        error
    ) { todos, msgToast, msgError ->
        WishesUiState(
            cargando = false,
            porLlegar = todos.filter { it.estado == EstadoWish.POR_LLEGAR },
            deseadas = todos.filter { it.estado == EstadoWish.DESEADA },
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

    fun guardarWish(
        id: Long = 0L,
        nombre: String,
        franquicia: String,
        linea: String?,
        estado: EstadoWish,
        diasLlegada: Long?,
        metaPesos: Long?
    ) {
        viewModelScope.launch {
            runCatching {
                val fechaEstimada = if (estado == EstadoWish.POR_LLEGAR) {
                    val dias = (diasLlegada ?: 30L).coerceAtLeast(1L)
                    Fechas.desdeFecha(LocalDate.now().plusDays(dias))
                } else null
                val entidad = WishEntity(
                    id = id,
                    nombre = nombre,
                    franquicia = franquicia,
                    linea = linea,
                    estado = estado,
                    fechaEstimadaLlegada = fechaEstimada,
                    progresoPorcentaje = if (estado == EstadoWish.POR_LLEGAR) 35 else 0,
                    metaPesos = metaPesos
                )
                if (id == 0L) wishRepository.guardar(entidad) else wishRepository.actualizar(entidad)
                toast.value = if (id == 0L) "${nombre.trim()} añadida a Wishes" else "Wish actualizado"
            }.onFailure {
                error.value = it.message ?: "No se pudo guardar el wish."
            }
        }
    }

    fun marcarYaLaCompre(wish: WishEntity) {
        viewModelScope.launch {
            val fechaEstimada = Fechas.desdeFecha(LocalDate.now().plusDays(30))
            wishRepository.pasarAPorLlegar(wish, fechaEstimada)
            toast.value = "${wish.nombre} pasó a Por llegar"
        }
    }

    fun marcarYaLlego(wish: WishEntity, alIncorporar: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val nuevaPieza = PiezaEntity(
                nombre = wish.nombre,
                franquicia = wish.franquicia,
                linea = wish.linea,
                fechaAdquisicion = Fechas.desdeFecha(LocalDate.now()),
                estado = EstadoPieza.EXHIBICION,
                procedencia = "Preventa / Wishes",
                valorEstimadoCentavos = wish.metaPesos?.times(100L)
            )
            val nuevoId = piezaRepository.guardar(nuevaPieza)
            wishRepository.eliminar(wish)
            toast.value = "✦ ${wish.nombre} ya está en tu museo. Completa su cédula."
            alIncorporar(nuevoId)
        }
    }

    fun eliminarWish(wish: WishEntity) {
        viewModelScope.launch {
            wishRepository.eliminar(wish)
            toast.value = "Eliminada de Wishes"
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VittherApp
                WishesViewModel(
                    wishRepository = app.container.wishRepository,
                    piezaRepository = app.container.piezaRepository
                )
            }
        }
    }
}
