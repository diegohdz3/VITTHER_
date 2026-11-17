package com.diego.vitther.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun VittherNavigationBar(seleccion: Int, navegar: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val radio = with(LocalDensity.current) { 38.dp.toPx() }
    val notch = GenericShape { size, _ ->
        val centro = size.width / 2
        moveTo(0f, 0f)
        lineTo(centro - radio * 1.3f, 0f)
        cubicTo(centro - radio, 0f, centro - radio, radio, centro, radio)
        cubicTo(centro + radio, radio, centro + radio, 0f, centro + radio * 1.3f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    Box(Modifier.fillMaxWidth().background(colors.background)) {
        Box(Modifier.matchParentSize().padding(top = 20.dp).background(colors.surfaceContainer, notch))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Inicio", "Wishes", "", "Historia", "Perfil").forEachIndexed { index, label ->
                val activo = seleccion == index
                val tint by animateColorAsState(
                    targetValue = if (activo) colors.primary else colors.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "navTint_$index"
                )
                if (index == 2) {
                    val escalaCentro by animateFloatAsState(
                        targetValue = if (activo) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "navCenterScale"
                    )
                    Box(Modifier.weight(1f).align(Alignment.Top), contentAlignment = Alignment.TopCenter) {
                        VittherLogo(
                            Modifier
                                .graphicsLayer {
                                    scaleX = escalaCentro
                                    scaleY = escalaCentro
                                }
                                .clip(CircleShape)
                                .semantics {
                                    contentDescription = "Colección"
                                    selected = activo
                                }
                                .clickable(role = Role.Button) { navegar(2) }
                        )
                    }
                } else {
                    val escalaIcono by animateFloatAsState(
                        targetValue = if (activo) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "navIconScale_$index"
                    )
                    val anchoIndicador by animateDpAsState(
                        targetValue = if (activo) 24.dp else 0.dp,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                        label = "navIndicatorWidth_$index"
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .selectable(selected = activo, role = Role.Tab, onClick = { navegar(index) })
                            .padding(top = 20.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = escalaIcono
                                scaleY = escalaIcono
                            }
                        ) {
                            CompositionLocalProvider(LocalContentColor provides tint) { Emblema(index) }
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Box(
                            Modifier
                                .padding(top = 4.dp)
                                .size(anchoIndicador, 1.5.dp)
                                .background(tint.copy(alpha = if (activo) 1f else 0f))
                        )
                    }
                }
            }
        }
    }
}
