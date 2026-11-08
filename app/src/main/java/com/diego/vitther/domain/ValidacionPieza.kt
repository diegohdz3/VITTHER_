package com.diego.vitther.domain

/**
 * Registro en dos niveles: solo nombre, franquicia y fecha son obligatorios.
 * Las reglas son funciones puras para poder probarlas sin Android.
 */
data class ErroresPieza(
    val nombre: String? = null,
    val franquicia: String? = null,
    val fechaAdquisicion: String? = null
) {
    val hayErrores: Boolean
        get() = nombre != null || franquicia != null || fechaAdquisicion != null
}

const val NOMBRE_MAX_CARACTERES = 80
const val FRANQUICIA_MAX_CARACTERES = 60

fun validarPieza(
    nombre: String,
    franquicia: String,
    fechaAdquisicion: Long?,
    ahora: Long = System.currentTimeMillis()
): ErroresPieza {
    val nombreLimpio = nombre.trim()
    val franquiciaLimpia = franquicia.trim()

    return ErroresPieza(
        nombre = when {
            nombreLimpio.isEmpty() -> "Ponle un nombre a la pieza."
            nombreLimpio.length > NOMBRE_MAX_CARACTERES ->
                "Máximo $NOMBRE_MAX_CARACTERES caracteres."
            else -> null
        },
        franquicia = when {
            franquiciaLimpia.isEmpty() -> "Indica la franquicia o línea."
            franquiciaLimpia.length > FRANQUICIA_MAX_CARACTERES ->
                "Máximo $FRANQUICIA_MAX_CARACTERES caracteres."
            else -> null
        },
        fechaAdquisicion = when {
            fechaAdquisicion == null -> "Elige la fecha de adquisición."
            fechaAdquisicion > Fechas.finDelDia(ahora) -> "La fecha no puede ser futura."
            Fechas.anioDe(fechaAdquisicion) < 1900 -> "Revisa esa fecha: parece muy antigua."
            else -> null
        }
    )
}
