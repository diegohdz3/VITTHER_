package com.diego.vitther.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ValidacionPiezaTest {

    private val zona = ZoneOffset.UTC
    private val ahora = Fechas.desdeFecha(LocalDate.of(2026, 10, 21), zona)
    private val fechaValida = Fechas.desdeFecha(LocalDate.of(2024, 3, 12), zona)

    @Test
    fun `una pieza con los tres campos obligatorios es valida`() {
        val errores = validarPieza(
            nombre = "Goku Ultra Instinct",
            franquicia = "Dragon Ball",
            fechaAdquisicion = fechaValida,
            ahora = ahora
        )

        assertFalse(errores.hayErrores)
    }

    @Test
    fun `el nombre vacio se rechaza`() {
        val errores = validarPieza("   ", "Dragon Ball", fechaValida, ahora)

        assertTrue(errores.hayErrores)
        assertNotNull(errores.nombre)
    }

    @Test
    fun `la franquicia vacia se rechaza`() {
        val errores = validarPieza("Goku", " ", fechaValida, ahora)

        assertTrue(errores.hayErrores)
        assertNotNull(errores.franquicia)
    }

    @Test
    fun `sin fecha de adquisicion no se puede guardar`() {
        val errores = validarPieza("Goku", "Dragon Ball", null, ahora)

        assertTrue(errores.hayErrores)
        assertNotNull(errores.fechaAdquisicion)
    }

    @Test
    fun `una fecha futura se rechaza`() {
        val manana = Fechas.desdeFecha(LocalDate.of(2026, 10, 22), zona)

        val errores = validarPieza("Goku", "Dragon Ball", manana, ahora)

        assertNotNull(errores.fechaAdquisicion)
    }

    @Test
    fun `hoy es una fecha valida`() {
        val errores = validarPieza("Goku", "Dragon Ball", ahora, ahora)

        assertNull(errores.fechaAdquisicion)
    }

    @Test
    fun `una fecha anterior a 1900 se rechaza`() {
        val antigua = Fechas.desdeFecha(LocalDate.of(1850, 1, 1), zona)

        val errores = validarPieza("Goku", "Dragon Ball", antigua, ahora)

        assertNotNull(errores.fechaAdquisicion)
    }

    @Test
    fun `un nombre demasiado largo se rechaza`() {
        val largo = "G".repeat(NOMBRE_MAX_CARACTERES + 1)

        val errores = validarPieza(largo, "Dragon Ball", fechaValida, ahora)

        assertNotNull(errores.nombre)
    }

    @Test
    fun `el limite exacto de caracteres si es valido`() {
        val limite = "G".repeat(NOMBRE_MAX_CARACTERES)

        val errores = validarPieza(limite, "Dragon Ball", fechaValida, ahora)

        assertNull(errores.nombre)
        assertEquals(false, errores.hayErrores)
    }
}
