package com.diego.vitther.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.R
import java.io.File
import kotlin.math.absoluteValue

/** Coil resuelve el tamaño desde las restricciones de la tarjeta, no desde el original. */
@Composable
fun PiezaCarouselCard(
    foto: File?,
    nombre: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondo = MaterialTheme.colorScheme.background
    val descripcion = stringResource(R.string.home_foto, nombre)
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { contentDescription = descripcion },
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFF5A4A22)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (foto != null) {
                SubcomposeAsyncImage(
                    model = foto,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { PlaceholderCarrusel(nombre) },
                    error = { PlaceholderCarrusel(nombre) }
                )
            } else {
                PlaceholderCarrusel(nombre)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to fondo.copy(alpha = .18f),
                            .35f to fondo.copy(alpha = 0f),
                            .65f to fondo.copy(alpha = 0f),
                            1f to fondo.copy(alpha = .75f)
                        )
                    )
            )
        }
    }
}

@Composable
private fun PlaceholderCarrusel(nombre: String = "") {
    val paleta = rememberPaletaPieza(nombre)
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(paleta.first, paleta.second, Color(0xFF0A0A0A)),
                    radius = 520f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val inicial = nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
        Text(
            text = inicial,
            fontSize = 112.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            color = Color(0x28F3EFE6)
        )
    }
}

internal fun rememberPaletaPieza(semilla: String): Pair<Color, Color> {
    val paletas = listOf(
        Color(0xFF3A1F0E) to Color(0xFF1A0E06),
        Color(0xFF2B0E0E) to Color(0xFF120606),
        Color(0xFF0E1B2B) to Color(0xFF060B12),
        Color(0xFF1E1428) to Color(0xFF0B0710),
        Color(0xFF0E2620) to Color(0xFF06110E),
        Color(0xFF28200E) to Color(0xFF110D05)
    )
    val idx = (semilla.hashCode().absoluteValue) % paletas.size
    return paletas[idx]
}

@Composable
fun EstadoExhibidoPill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            stringResource(R.string.home_exhibido),
            Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
