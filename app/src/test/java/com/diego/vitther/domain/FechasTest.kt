package com.diego.vitther.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class FechasTest {

    private val zona = ZoneOffset.UTC

    @Test
    fun `inicioDelDia descarta horas minutos y segundos`() {
        val dia = Fechas.desdeFecha(LocalDate.of(2026, 10, 21), zona)
        val conHora = dia + 21 * 3_600_000L

        assertEquals(dia, Fechas.inicioDelDia(conHora, zona))
    }

    @Test
    fun `diasHasta devuelve cero para el mismo dia`() {
        val dia = Fechas.desdeFecha(LocalDate.of(2026, 10, 21), zona)

        assertEquals(0L, Fechas.diasHasta(dia + 22 * 3_600_000L, dia + 3_600_000L, zona))
    }

    @Test
    fun `diasHasta cuenta dias de calendario, no bloques de 24 horas`() {
        val hoy = Fechas.desdeFecha(LocalDate.of(2026, 10, 21), zona)
        val manana = Fechas.desdeFecha(LocalDate.of(2026, 10, 22), zona)

        assertEquals(1L, Fechas.diasHasta(manana, hoy + 23 * 3_600_000L, zona))
    }

    @Test
    fun `finDelDia queda dentro del mismo dia`() {
        val dia = Fechas.desdeFecha(LocalDate.of(2026, 10, 21), zona)

        assertEquals(dia, Fechas.inicioDelDia(Fechas.finDelDia(dia, zona), zona))
        assertTrue(Fechas.finDelDia(dia, zona) > dia)
    }
}
