package com.diego.vitther.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        Row(Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 100.dp),
            verticalAlignment = Alignment.CenterVertically) {
            listOf("Inicio", "Categorías", "", "Historia", "Perfil").forEachIndexed { index, label ->
                val tint = if (seleccion == index) colors.primary else colors.onSurfaceVariant
                if (index == 2) {
                    Box(Modifier.weight(1f).align(Alignment.Top), contentAlignment = Alignment.TopCenter) {
                        VittherLogo(Modifier.clip(CircleShape)
                            .semantics {
                                contentDescription = "VITTHER, abrir colección"
                                selected = seleccion == 2
                            }
                            .clickable(role = Role.Button) { navegar(2) })
                    }
                } else {
                    Column(Modifier.weight(1f)
                        .selectable(selected = seleccion == index, role = Role.Tab, onClick = { navegar(index) })
                        .padding(top = 20.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        CompositionLocalProvider(LocalContentColor provides tint) { Emblema(index) }
                        Text(label, style = MaterialTheme.typography.labelSmall, color = tint,
                            modifier = Modifier.padding(top = 6.dp))
                        Box(Modifier.padding(top = 4.dp).size(24.dp, 1.dp)
                            .background(if (seleccion == index) colors.primary else colors.primary.copy(alpha = 0f)))
                    }
                }
            }
        }
    }
}
