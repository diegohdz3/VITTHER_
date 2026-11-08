package com.diego.vitther.domain

import com.diego.vitther.data.local.PiezaEntity
import org.junit.Assert.*
import org.junit.Test

class HistoriaTest {
    @Test fun distingueAdquisicionRegistroYRevelacionSinDuplicados() {
        val dia = 86_400_000L
        val pieza = PiezaEntity(id = 1, nombre = "Gambito", franquicia = "Marvel",
            fechaAdquisicion = 100, fechaRegistro = 100 + 2 * dia)
        val sellada = CapsulaVista(1, 1, null, 300, null, null)
        val disponible = sellada.copy(id = 2, fechaApertura = 50)
        val abierta = sellada.copy(id = 3, abiertaEn = 100 + 3 * dia, nota = "Recuerdo")
        val hitos = historiaDe(listOf(pieza), listOf(sellada, disponible, abierta))
        assertEquals(listOf(TipoHito.CAPSULA, TipoHito.REGISTRO, TipoHito.ADQUISICION), hitos.map { it.tipo })
        assertEquals(3, hitos.map { it.clave }.distinct().size)
        assertEquals(hitos, historiaDe(listOf(pieza), listOf(sellada, disponible, abierta)))
        assertTrue(historiaDe(emptyList(), listOf(abierta)).isEmpty())
    }

    @Test fun noRepiteRegistroCuandoOcurrioElMismoDia() {
        val pieza = PiezaEntity(id = 1, nombre = "Gambito", franquicia = "Marvel",
            fechaAdquisicion = 100, fechaRegistro = 200)
        assertEquals(listOf(TipoHito.ADQUISICION), historiaDe(listOf(pieza), emptyList()).map { it.tipo })
    }
}
