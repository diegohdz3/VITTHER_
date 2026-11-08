package com.diego.vitther.domain

/**
 * Estado de una pieza en el museo. Es mutuamente excluyente por diseño:
 * una pieza está exhibida o está en resguardo, nunca las dos.
 */
enum class EstadoPieza {
    EXHIBICION,
    BODEGA;

    val etiqueta: String
        get() = when (this) {
            EXHIBICION -> "En exhibición"
            BODEGA -> "En bodega"
        }
}
