package com.diego.vitther.data.local

/**
 * La aplicación inicia completamente limpia (de fábrica) para que el coleccionista
 * registre únicamente sus propias piezas, cápsulas y wishes desde cero.
 */
object SeedData {
    suspend fun sembrarSiEstaVacio(
        piezaDao: PiezaDao,
        capsulaDao: CapsulaDao,
        wishDao: WishDao? = null
    ) {
        // Sin datos por defecto: el museo comienza vacío de fábrica.
    }
}
