package com.diego.vitther.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.diego.vitther.ui.theme.VITTHERTheme

/** Insignia vectorial; las ocho caras y el anillo escalan con el diámetro. */
@Composable
fun VittherLogo(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier.size(size)) {
        val d = this.size.minDimension
        translate((this.size.width - d) / 2, (this.size.height - d) / 2) {
            val center = Offset(d / 2, d / 2)
            drawCircle(Brush.radialGradient(listOf(colors.surfaceVariant, colors.background),
                center = center, radius = d / 2), radius = d / 2, center = center)
            // Rayado limitado al disco interior, casi imperceptible.
            for (i in 1..29) {
                val x = i / 30f
                val halfHeight = kotlin.math.sqrt((.46f * .46f - (x - .5f) * (x - .5f)).coerceAtLeast(0f))
                drawLine(colors.primary.copy(alpha = .035f),
                    Offset(x * d, (.5f - halfHeight) * d), Offset(x * d, (.5f + halfHeight) * d),
                    strokeWidth = d * .004f)
            }
            drawCircle(colors.primary, radius = d * .475f, center = center, style = Stroke(d * .025f))

            fun face(tx: Float, ty: Float, cx: Float, cy: Float,
                     qx: Float, qy: Float, color: Color) {
                drawPath(Path().apply {
                    moveTo(d * .49f, d * .495f)
                    lineTo(d * tx, d * ty)
                    quadraticTo(d * qx, d * qy, d * cx, d * cy)
                    close()
                }, color)
            }
            val light = colors.onPrimaryContainer
            val shade = colors.secondary
            // Dos caras por punta; los encuentros desplazados sugieren un leve giro.
            face(.5f, .19f, .445f, .455f, .475f, .415f, light)
            face(.5f, .19f, .55f, .445f, .515f, .405f, shade)
            face(.755f, .5f, .55f, .445f, .59f, .48f, light)
            face(.755f, .5f, .555f, .55f, .60f, .515f, shade)
            face(.5f, .81f, .555f, .55f, .525f, .60f, shade)
            face(.5f, .81f, .45f, .56f, .48f, .61f, light)
            face(.245f, .5f, .45f, .56f, .405f, .525f, shade)
            face(.245f, .5f, .445f, .455f, .40f, .48f, light)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VittherLogoPreview() {
    VITTHERTheme {
        Row(Modifier.background(MaterialTheme.colorScheme.background).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            VittherLogo(size = 64.dp)
            VittherLogo(size = 160.dp)
        }
    }
}
