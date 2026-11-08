package com.diego.vitther

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.diego.vitther.ui.screens.home.*
import com.diego.vitther.ui.theme.VITTHERTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeCarouselTest {
    @get:Rule val compose = createComposeRule()

    @Test fun flechasGestoYClickConFotosAusentes() {
        var elegido: Long? = null
        val piezas = listOf(PiezaHome(11, "Goku", "Dragon Ball"),
            PiezaHome(22, "Spider-Man", "Marvel"), PiezaHome(33, "Guts", "Berserk"))
        compose.setContent { VITTHERTheme { HomeScreen(HomeUiState.Contenido(piezas, 12), { elegido = it }) } }
        compose.onNodeWithContentDescription("Pieza anterior").assertIsNotEnabled()
        compose.onNodeWithText("Una cápsula sellada se abre en 12 días").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pieza siguiente").performClick()
        compose.onNodeWithText("Spider-Man").assertIsDisplayed()
        compose.onNodeWithContentDescription("Foto de Spider-Man").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Guts").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pieza siguiente").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Foto de Guts").performClick()
        compose.runOnIdle { assertEquals(33L, elegido) }
        compose.onNodeWithContentDescription("Pieza anterior").performClick()
        compose.onNodeWithText("Spider-Man").assertIsDisplayed()
    }

    @Test fun quitarPiezasDuranteLaSesionYMostrarVacio() {
        val estado = mutableStateOf<HomeUiState>(HomeUiState.Contenido(listOf(
            PiezaHome(1, "Primera", "Museo"), PiezaHome(2, "Segunda", "Museo")
        )))
        compose.setContent { VITTHERTheme { HomeScreen(estado.value, {}) } }
        compose.onNodeWithContentDescription("Pieza siguiente").performClick()
        compose.runOnIdle { estado.value = HomeUiState.Contenido(listOf(PiezaHome(1, "Primera", "Museo"))) }
        compose.onNodeWithText("Primera").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pieza siguiente").assertIsNotEnabled()
        compose.runOnIdle { estado.value = HomeUiState.Vacia() }
        compose.onNodeWithText("Tu vitrina está vacía").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pieza siguiente").assertDoesNotExist()
    }
}
