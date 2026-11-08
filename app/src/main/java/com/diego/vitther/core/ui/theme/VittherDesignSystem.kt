package com.diego.vitther.core.ui.theme

import com.diego.vitther.ui.theme.BackgroundPrimary
import com.diego.vitther.ui.theme.GoldPrimary
import com.diego.vitther.ui.theme.VITTHERTheme

/**
 * Punto de entrada del tema dentro de core/ui/theme/ según la guía técnica MVVM.
 */
object VittherDesignSystem {
    val backgroundPrimary = BackgroundPrimary
    val goldAccent = GoldPrimary
}

typealias CoreVittherTheme = @androidx.compose.runtime.Composable (content: @androidx.compose.runtime.Composable () -> Unit) -> Unit
