package com.diego.vitther

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.diego.vitther.data.local.*
import com.diego.vitther.data.repository.CapsulaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CapsulasRepositoryTest {
    @Test fun selladoBloqueoAperturaAtomicaYReapertura() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, VittherDatabase::class.java).build()
        try {
            val pieza = db.piezaDao().insertar(PiezaEntity(nombre = "Figura", franquicia = "Prueba", fechaAdquisicion = 100))
            fun repo(ahora: Long) = CapsulaRepository(db.capsulaDao(), Clock.fixed(Instant.ofEpochMilli(ahora), ZoneOffset.UTC))
            val id = repo(1000).sellar(pieza, "Recuerdo", "Texto secreto", 2000)
            assertFalse(repo(1999).abrir(id))
            assertNull(repo(1999).observarPorPieza(pieza).first().single().nota)
            // Incluso estando disponible el texto requiere apertura explícita.
            assertNull(repo(2000).observarTodas().first().single().nota)
            assertTrue(repo(2000).abrir(id))
            assertEquals("Texto secreto", repo(2000).observarTodas().first().single().nota)
            assertTrue(repo(3000).abrir(id))
            assertEquals(2000L, db.capsulaDao().obtenerPorId(id)!!.abiertaEn)
            assertFalse(repo(3000).abrir(id + 100))
            try {
                repo(1000).sellar(pieza, null, " ", 2000)
                fail("No debe aceptar una nota vacía")
            } catch (_: IllegalArgumentException) { }
            try {
                repo(1000).sellar(pieza, null, "Nota", 1000)
                fail("No debe aceptar una fecha no futura")
            } catch (_: IllegalArgumentException) { }
            db.piezaDao().eliminarPorId(pieza)
            assertFalse(repo(4000).abrir(id))
        } finally { db.close() }
    }
}
