package com.diego.vitther.core.common

/**
 * Representa el resultado tipado de una operación asíncrona o de red/base de datos,
 * alineado con los lineamientos de estructura MVVM (core/common/).
 */
sealed interface ResultadoOperacion<out T> {
    data class Exito<T>(val datos: T) : ResultadoOperacion<T>
    data class Error(val mensaje: String, val causa: Throwable? = null) : ResultadoOperacion<Nothing>
    data object Cargando : ResultadoOperacion<Nothing>
}
