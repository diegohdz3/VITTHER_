package com.diego.vitther.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diego.vitther.VittherApp
import com.diego.vitther.di.AppContainer
import com.diego.vitther.domain.diasHastaProximaCapsula
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

data class PiezaHome(val id: Long, val nombre: String, val franquicia: String, val foto: File? = null)

sealed interface HomeUiState {
    data object Cargando : HomeUiState
    data class Vacia(val diasHastaCapsula: Long? = null) : HomeUiState
    data class Contenido(val piezas: List<PiezaHome>, val diasHastaCapsula: Long? = null) : HomeUiState
    data object Error : HomeUiState
}

class HomeViewModel(container: AppContainer) : ViewModel() {
    private val piezas = container.piezaRepository.observarVitrina().map { lista ->
        lista.map { PiezaHome(it.id, it.nombre, it.franquicia, container.fotos.archivo(it.fotoPath)) }
    }.flowOn(Dispatchers.IO)

    private val reloj = flow {
        while (true) {
            val ahora = System.currentTimeMillis()
            emit(ahora)
            delay(60_000 - ahora % 60_000)
        }
    }

    val estado: StateFlow<HomeUiState> = combine(
        piezas, container.capsulaRepository.observarTodas(), reloj
    ) { lista, capsulas, ahora ->
        val dias = diasHastaProximaCapsula(capsulas, ahora)
        if (lista.isEmpty()) HomeUiState.Vacia(dias) else HomeUiState.Contenido(lista, dias)
    }.distinctUntilChanged()
        .catch { emit(HomeUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), HomeUiState.Cargando)

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel((this[APPLICATION_KEY] as VittherApp).container) }
        }
    }
}
