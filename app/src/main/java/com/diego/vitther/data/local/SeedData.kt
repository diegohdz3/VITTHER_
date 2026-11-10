package com.diego.vitther.data.local

import android.content.Context

/**
 * La aplicación inicia completamente limpia (de fábrica) para la exposición y uso real,
 * lista para demostrar cómo funciona agregando piezas, deseos y recorridos desde cero.
 */
object SeedData {

    suspend fun sembrarSiEstaVacio(
        piezaDao: PiezaDao,
        capsulaDao: CapsulaDao,
        wishDao: WishDao? = null,
        context: Context? = null
    ) {
        // Modo de fábrica: sin datos pregrabados para la demostración en vivo.
    }
}
