package com.diego.vitther.presentation.capsulas

import com.diego.vitther.domain.CapsulaVista

/**
 * Estado observable del módulo de Cápsulas de Tiempo en presentation/capsulas/
 * conforme a las secciones 5, 6 y 7 de la guía técnica MVVM.
 */
data class CapsulasUiState(
    val cargando: Boolean = false,
    val capsulas: List<CapsulaVista> = emptyList(),
    val ahoraMillis: Long = System.currentTimeMillis(),
    val mensajeExito: String? = null,
    val mensajeError: String? = null
)
