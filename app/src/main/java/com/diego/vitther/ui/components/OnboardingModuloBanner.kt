package com.diego.vitther.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow

private const val PREFS_ONBOARDING = "vitther_onboarding_prefs"
private val GoldBanner = Color(0xFFE8C97A)
private val TextBanner = Color(0xFFF2EAD8)
private val MutedBanner = Color(0xFFACA395)

object VittherOnboardingState {
    val resetVersion = MutableStateFlow(0)

    fun reiniciarTodos(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        resetVersion.value += 1
    }
}

fun reiniciarOnboardingModulos(context: Context) {
    VittherOnboardingState.reiniciarTodos(context)
}

/**
 * Nota curatorial flotante mostrada únicamente la primera vez que el usuario ingresa a cada módulo.
 * Al presionar "Entendido", se guarda el estado y deja la pantalla completamente despejada.
 */
@Composable
fun NotaCuratorialPrimerIngreso(
    moduloKey: String,
    titulo: String,
    descripcion: String,
    puntos: List<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
    }
    val resetVer by VittherOnboardingState.resetVersion.collectAsState()
    var visible by remember(moduloKey, resetVer) {
        mutableStateOf(!prefs.getBoolean("visto_$moduloKey", false))
    }

    val cerrar: () -> Unit = {
        prefs.edit().putBoolean("visto_$moduloKey", true).apply()
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF514110B),
            border = BorderStroke(1.dp, Color(0xFF5A4A22)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF241E11))
                            .border(1.dp, Color(0xFF5A4A28), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "GUÍA DE PRIMER INGRESO",
                            color = GoldBanner,
                            fontSize = 8.5.sp,
                            letterSpacing = 1.8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Omitir",
                        color = MutedBanner,
                        fontSize = 11.5.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = cerrar)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = titulo,
                    color = TextBanner,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = descripcion,
                    color = MutedBanner,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (puntos.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        puntos.forEach { punto ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✦",
                                    color = GoldBanner,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = punto,
                                    color = Color(0xFFD8D0C2),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF2B2313), Color(0xFF1A150B)))
                        )
                        .border(1.dp, Color(0xFF6B5832), RoundedCornerShape(12.dp))
                        .clickable(onClick = cerrar)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Entendido",
                        color = GoldBanner,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
