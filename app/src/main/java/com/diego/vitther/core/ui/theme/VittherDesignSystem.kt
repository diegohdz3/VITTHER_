package com.diego.vitther.core.ui.theme

import androidx.compose.runtime.Composable
import com.diego.vitther.ui.theme.AccentGold
import com.diego.vitther.ui.theme.BackgroundPrimary
import com.diego.vitther.ui.theme.VITTHERTheme

/**
 * Punto de entrada del tema dentro de core/ui/theme/ según la guía técnica MVVM.
 */
object VittherDesignSystem {
    val backgroundPrimary = BackgroundPrimary
    val goldAccent = AccentGold
}

@Composable
fun CoreVittherTheme(content: @Composable () -> Unit) {
    VITTHERTheme(content = content)
}
