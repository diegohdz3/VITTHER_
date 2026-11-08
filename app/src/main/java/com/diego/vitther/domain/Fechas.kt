package com.diego.vitther.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Todas las fechas circulan como epoch millis y se normalizan al inicio del día
 * local. Así "12 de marzo" significa lo mismo sin importar la hora en que se
 * registró la pieza ni el cambio de horario.
 */
object Fechas {

    private val locale = Locale.forLanguageTag("es-MX")
    private val formatoCorto = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    private val formatoLargo = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", locale)

    fun inicioDelDia(millis: Long, zona: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(millis)
            .atZone(zona)
            .toLocalDate()
            .atStartOfDay(zona)
            .toInstant()
            .toEpochMilli()

    fun finDelDia(millis: Long, zona: ZoneId = ZoneId.systemDefault()): Long {
        val dia = Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()
        return dia.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli() - 1
    }

    fun desdeFecha(fecha: LocalDate, zona: ZoneId = ZoneId.systemDefault()): Long =
        fecha.atStartOfDay(zona).toInstant().toEpochMilli()

    /**
     * Días de calendario entre hoy y la fecha objetivo.
     * 0 = hoy, negativo = ya pasó.
     */
    fun diasHasta(
        objetivo: Long,
        ahora: Long = System.currentTimeMillis(),
        zona: ZoneId = ZoneId.systemDefault()
    ): Long {
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        val diaObjetivo = Instant.ofEpochMilli(objetivo).atZone(zona).toLocalDate()
        return ChronoUnit.DAYS.between(hoy, diaObjetivo)
    }

    fun anioDe(millis: Long, zona: ZoneId = ZoneId.systemDefault()): Int =
        Instant.ofEpochMilli(millis).atZone(zona).year

    fun mismoDia(a: Long, b: Long, zona: ZoneId = ZoneId.systemDefault()): Boolean =
        Instant.ofEpochMilli(a).atZone(zona).toLocalDate() ==
            Instant.ofEpochMilli(b).atZone(zona).toLocalDate()

    fun formatearCorto(millis: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDate().format(formatoCorto)

    fun formatearLargo(millis: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDate().format(formatoLargo)
}
