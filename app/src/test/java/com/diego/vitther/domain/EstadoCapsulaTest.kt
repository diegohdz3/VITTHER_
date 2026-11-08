package com.diego.vitther.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class EstadoCapsulaTest {

    private val zona = ZoneOffset.UTC
    private val base = 1_767_225_600_000L // 2026-01-01T00:00:00Z
    private val dia = 86_400_000L

    @Test
    fun `una capsula con fecha futura esta sellada y cuenta los dias`() {
        val estado = estadoDeCapsula(
            abiertaEn = null,
            fechaApertura = base + 12 * dia,
            ahora = base,
            zona = zona
        )

        assertEquals(EstadoCapsula.Sellada(12), estado)
    }

    @Test
    fun `se abre exactamente en la fecha, no un segundo despues`() {
        val apertura = base + 5 * dia

        val estado = estadoDeCapsula(
            abiertaEn = null,
            fechaApertura = apertura,
            ahora = apertura,
            zona = zona
        )

        assertEquals(EstadoCapsula.Disponible, estado)
    }

    @Test
    fun `sigue disponible si el usuario no la abre el mismo dia`() {
        val estado = estadoDeCapsula(
            abiertaEn = null,
            fechaApertura = base,
            ahora = base + 30 * dia,
            zona = zona
        )

        assertEquals(EstadoCapsula.Disponible, estado)
    }

    @Test
    fun `una capsula ya abierta permanece abierta aunque la fecha sea futura`() {
        val estado = estadoDeCapsula(
            abiertaEn = base,
            fechaApertura = base + 100 * dia,
            ahora = base,
            zona = zona
        )

        assertEquals(EstadoCapsula.Abierta, estado)
    }

    @Test
    fun `el dia de la apertura se muestra como cero dias restantes`() {
        val estado = estadoDeCapsula(
            abiertaEn = null,
            fechaApertura = base + 20 * 3_600_000L,
            ahora = base + 8 * 3_600_000L,
            zona = zona
        )

        assertEquals(EstadoCapsula.Sellada(0), estado)
    }

    @Test
    fun `los dias restantes nunca son negativos`() {
        val estado = estadoDeCapsula(
            abiertaEn = null,
            fechaApertura = base - dia,
            ahora = base,
            zona = zona
        )

        assertTrue(estado !is EstadoCapsula.Sellada)
    }
}
