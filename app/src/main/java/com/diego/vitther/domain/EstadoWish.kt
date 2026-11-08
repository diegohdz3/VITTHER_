package com.diego.vitther.domain

/**
 * Estado de un deseo en la sección Wishes:
 * - POR_LLEGAR: preventa o pedido en camino hacia el museo.
 * - DESEADA: figura en lista de deseos con meta de ahorro/presupuesto.
 */
enum class EstadoWish(val etiqueta: String) {
    POR_LLEGAR("Por llegar"),
    DESEADA("Deseadas")
}
