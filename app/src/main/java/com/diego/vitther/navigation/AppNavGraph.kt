package com.diego.vitther.navigation

import androidx.compose.runtime.Composable

/**
 * Punto de entrada estándar del grafo de navegación (navigation/AppNavGraph.kt)
 * requerido por los lineamientos de estructura MVVM.
 */
@Composable
fun AppNavGraph(
    piezaPendiente: Long? = null,
    onPiezaPendienteConsumida: () -> Unit = {}
) {
    VittherNavHost(
        piezaPendiente = piezaPendiente,
        onPiezaPendienteConsumida = onPiezaPendienteConsumida
    )
}
