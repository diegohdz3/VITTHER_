package com.diego.vitther.presentation.coleccion

import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.ui.ColeccionViewModel

/**
 * Estado observable y ViewModel del módulo de Colección en presentation/coleccion/
 * conforme a las secciones 5, 6 y 7 de la guía técnica MVVM.
 */
data class ColeccionUiState(
    val cargando: Boolean = false,
    val piezas: List<PiezaEntity> = emptyList(),
    val busqueda: String = "",
    val mostrarBodega: Boolean = false,
    val mensajeError: String? = null
)

typealias ColeccionModuleViewModel = ColeccionViewModel
