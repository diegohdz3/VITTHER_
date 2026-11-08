package com.diego.vitther.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.diego.vitther.R

/**
 * Serif de marca embebida (EB Garamond, OFL): la misma en todos los teléfonos.
 * Sans del sistema para datos y navegación: legibilidad y cero peso extra.
 */
val VitherSerif = FontFamily(Font(R.font.eb_garamond, FontWeight.Normal))
val VitherSans = FontFamily.Default

val Typography = Typography(
    // Wordmark "VITTHER" en Home/splash
    displaySmall = TextStyle(
        fontFamily = VitherSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.18.em
    ),
    // Nombre de pieza en cédula y tarjetas grandes
    headlineMedium = TextStyle(
        fontFamily = VitherSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = VitherSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    // Títulos de pantalla
    titleLarge = TextStyle(
        fontFamily = VitherSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = VitherSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // Franquicia y subtítulos: mayúsculas pequeñas con tracking
    titleSmall = TextStyle(
        fontFamily = VitherSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em
    ),
    // Cuerpo y notas personales
    bodyLarge = TextStyle(
        fontFamily = VitherSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VitherSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    // Metadatos secundarios: fechas, estados
    bodySmall = TextStyle(
        fontFamily = VitherSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    // Botones y acciones de texto
    labelLarge = TextStyle(
        fontFamily = VitherSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    // Labels de la barra de navegación inferior
    labelMedium = TextStyle(
        fontFamily = VitherSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Badges y pills de estado
    labelSmall = TextStyle(
        fontFamily = VitherSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.em
    )
)
