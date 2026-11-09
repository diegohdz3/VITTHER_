package com.diego.vitther

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** Recorre la UI real y recrea la Activity para comprobar el estado del formulario y el BottomSheet de Cédula. */
class ColeccionFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun registrarEditarMoverYEliminar() {
        val nombre = "Pieza prueba ${System.currentTimeMillis()}"
        compose.onNodeWithText("Colección").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Agregar pieza").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Agregar pieza").performClick()
        compose.onNodeWithContentDescription("Nombre *").performTextInput(nombre)
        compose.onNodeWithText("Berserk").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(nombre).assertExists()
        compose.onNodeWithText("Colocar en el museo").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Ver en mi museo").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ver en mi museo").performClick()
        compose.waitUntil(15_000) { compose.onAllNodes(hasContentDescription("Editar cédula") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Editar cédula").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Nombre *").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Nombre *").performTextReplacement("$nombre editada")
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Más opciones").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Mover a bodega").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Mover a bodega").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("En bodega").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Destacar en Inicio").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Destacar en Inicio").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Eliminar pieza").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Eliminar pieza").performClick()
        compose.onNodeWithText("Conservar").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Eliminar pieza").performClick()
        compose.onNodeWithText("Eliminar", substring = false).performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Agregar pieza").fetchSemanticsNodes().isNotEmpty() }
    }
}
