package com.diego.vitther

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.diego.vitther.data.local.PiezaEntity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Comprueba la promesa de la feature: la nota no existe en pantalla antes de revelar. */
class CapsulaFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun laNotaSigueOcultaHastaRevelar() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as VittherApp
        val nombre = "Cápsula UI ${System.currentTimeMillis()}"
        val nota = "Nota secreta ${System.currentTimeMillis()}"
        val piezaId = runBlocking {
            app.container.piezaRepository.guardar(
                PiezaEntity(nombre = nombre, franquicia = "Pruebas", fechaAdquisicion = System.currentTimeMillis())
            )
        }
        try {
            runBlocking {
                app.container.capsulaRepository.sellar(piezaId, "Prueba", nota, System.currentTimeMillis() + 500)
            }
            compose.onNodeWithText("Colección").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText(nombre).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(nombre).performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Cápsulas de tiempo").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Revelar cápsula").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(nota).assertDoesNotExist()
            compose.onNodeWithText("Revelar cápsula").performScrollTo().performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText(nota).fetchSemanticsNodes().isNotEmpty() }
        } finally {
            runBlocking { app.container.piezaRepository.eliminarPorId(piezaId) }
        }
    }
}
