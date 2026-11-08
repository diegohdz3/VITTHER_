package com.diego.vitther.domain

import java.time.ZoneId

/**
 * Regla de negocio de las cápsulas de tiempo: el contenido no puede revelarse
 * antes de la fecha de apertura. La lógica vive aquí, no en la pantalla, para
 * que cualquier futura vista (notificación, widget) respete la misma regla.
 */
sealed interface EstadoCapsula {
    /** Todavía sellada. [diasRestantes] puede ser 0 si se abre hoy. */
    data class Sellada(val diasRestantes: Long) : EstadoCapsula

    /** Ya llegó la fecha, pero el usuario aún no la ha abierto. */
    data object Disponible : EstadoCapsula

    /** El usuario ya la abrió; su contenido es visible. */
    data object Abierta : EstadoCapsula
}

fun estadoDeCapsula(
    abiertaEn: Long?,
    fechaApertura: Long,
    ahora: Long = System.currentTimeMillis(),
    zona: ZoneId = ZoneId.systemDefault()
): EstadoCapsula = when {
    abiertaEn != null -> EstadoCapsula.Abierta
    ahora >= fechaApertura -> EstadoCapsula.Disponible
    else -> EstadoCapsula.Sellada(
        diasRestantes = Fechas.diasHasta(fechaApertura, ahora, zona).coerceAtLeast(0)
    )
}
