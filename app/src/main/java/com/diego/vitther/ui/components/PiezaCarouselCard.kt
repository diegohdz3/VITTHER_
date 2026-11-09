package com.diego.vitther.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val NichoOuterShape = RoundedCornerShape(
    topStart = 130.dp,
    topEnd = 130.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp
)

private val NichoInnerShape = RoundedCornerShape(
    topStart = 123.dp,
    topEnd = 123.dp,
    bottomStart = 10.dp,
    bottomEnd = 10.dp
)

/** Nicho arqueado de museo inspirado en el Louvre. */
@Composable
fun PiezaCarouselCard(
    foto: File?,
    nombre: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondo = Color(0xFF0D0F16)
    val descripcion = stringResource(R.string.home_foto, nombre)
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { contentDescription = descripcion },
        shape = NichoOuterShape,
        border = BorderStroke(1.dp, Color(0xFF6B5A2C)),
        colors = CardDefaults.cardColors(
            containerColor = fondo,
            disabledContainerColor = fondo
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
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
            // Sombra atmosférica de vitrina
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to fondo.copy(alpha = .16f),
                            .38f to Color.Transparent,
                            .72f to Color.Transparent,
                            1f to fondo.copy(alpha = .78f)
                        )
                    )
            )
            // Marco interior arqueado dorado (inset 7.dp como en el prototipo Louvre)
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(7.dp)
                    .border(1.dp, Color(0x38E8C97A), NichoInnerShape)
            )
        }
    }
}

/** Nicho vacío punteado ("Un nicho te espera") para añadir una nueva pieza desde el carrusel de Inicio. */
@Composable
fun NichoAgregarCard(
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = Color(0xFFE8C97A)
    val dashBorder = Color(0xFF6B5A2C)
    Box(
        modifier = modifier
            .clip(NichoOuterShape)
            .background(Color(0x660D0F16))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "Un nicho te espera" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
            val w = size.width
            val h = size.height
            val rTop = (w / 2f).coerceAtMost(130.dp.toPx())
            val rBot = 16.dp.toPx()

            fun buildArchPath(inset: Float, topR: Float, botR: Float): Path = Path().apply {
                val left = inset
                val top = inset
                val right = w - inset
                val bottom = h - inset
                moveTo(left, top + topR)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left, top, right, top + topR * 2f),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(right, bottom - botR)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(right - botR * 2f, bottom - botR * 2f, right, bottom),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + botR, bottom)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(left, bottom - botR * 2f, left + botR * 2f, bottom),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }

            drawPath(
                path = buildArchPath(0.5.dp.toPx(), rTop, rBot),
                color = dashBorder,
                style = Stroke(width = 1.1.dp.toPx(), pathEffect = dash)
            )
            val insetPx = 7.dp.toPx()
            drawPath(
                path = buildArchPath(insetPx, (rTop - insetPx).coerceAtLeast(20f), 10.dp.toPx()),
                color = gold.copy(alpha = 0.22f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
            )

            // Símbolo "+" central dorado
            val cx = w / 2f
            val cy = h / 2f
            val arm = 15.dp.toPx()
            drawLine(
                color = gold,
                start = Offset(cx, cy - arm),
                end = Offset(cx, cy + arm),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = gold,
                start = Offset(cx - arm, cy),
                end = Offset(cx + arm, cy),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round
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
                    colors = listOf(paleta.first, paleta.second, Color(0xFF0B0A07)),
                    radius = 520f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val inicial = nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
        Text(
            text = inicial,
            fontSize = 84.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            color = Color(0x38E8C97A)
        )
    }
}

internal fun rememberPaletaPieza(semilla: String): Pair<Color, Color> {
    val paletas = listOf(
        Color(0xFF2A2210) to Color(0xFF0B0A07),
        Color(0xFF35506A) to Color(0xFF141A22),
        Color(0xFF6A2A2A) to Color(0xFF1A0F12),
        Color(0xFF3A4A6A) to Color(0xFF10131C),
        Color(0xFF2F5A3A) to Color(0xFF0F1A12),
        Color(0xFF4A3420) to Color(0xFF120A05)
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
