package com.diego.vitther.ui.screens.coleccion

import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.diego.vitther.data.remote.dto.ColeccionCompartidaDto
import com.diego.vitther.data.remote.dto.PiezaCloudDto
import com.diego.vitther.domain.Fechas
import java.util.Locale

private val GoldExpo = Color(0xFFE8C97A)
private val MutedExpo = Color(0xFF8D8777)
private val TextExpo = Color(0xFFF2EAD8)
private val BorderExpo = Color(0xFF5A4A22)

/**
 * Visor virtual a pantalla completa en modo estrictamente de Solo Lectura
 * para recorrer la colección o sala compartida por otro coleccionista.
 */
@Composable
fun ExposicionCompartidaDialog(
    exposicion: ColeccionCompartidaDto,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsListo by remember { mutableStateOf(false) }
    var narrandoIndice by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(context) {
        var motor: TextToSpeech? = null
        motor = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                motor?.let { m ->
                    val res = m.setLanguage(Locale.forLanguageTag("es-MX"))
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        m.setLanguage(Locale("es", "ES"))
                    }
                    m.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            ContextCompat.getMainExecutor(context).execute { narrandoIndice = null }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            ContextCompat.getMainExecutor(context).execute { narrandoIndice = null }
                        }
                    })
                    ttsListo = true
                }
            }
        }
        tts = motor
        onDispose {
            motor?.stop()
            motor?.shutdown()
        }
    }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val bg = Brush.verticalGradient(
            0.0f to Color(0xFF0B1224),
            0.35f to Color(0xFF0A0D17),
            1.0f to Color(0xFF0A0A0A)
        )
        val pagerState = rememberPagerState(pageCount = { exposicion.piezas.size.coerceAtLeast(1) })
        LaunchedEffect(pagerState.currentPage) {
            if (narrandoIndice != null && narrandoIndice != pagerState.currentPage) {
                tts?.stop()
                narrandoIndice = null
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Barra superior con insignia Solo Lectura y botón cerrar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF211B10))
                        .border(1.dp, BorderExpo, RoundedCornerShape(999.dp))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = GoldExpo,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "SOLO LECTURA · ${exposicion.codigo}",
                        color = GoldExpo,
                        fontSize = 9.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onCerrar,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF13151C))
                        .border(1.dp, Color(0xFF2A2418), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar exposición",
                        tint = TextExpo,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Encabezado del museo compartido
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = exposicion.tituloExposicion,
                    color = TextExpo,
                    fontSize = 25.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Curada por ${exposicion.propietarioNombre} · ${exposicion.piezas.size} ${if (exposicion.piezas.size == 1) "pieza" else "piezas"}",
                    color = GoldExpo.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(14.dp))

            if (exposicion.piezas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Esta exposición todavía no tiene piezas visibles.",
                        color = MutedExpo,
                        fontSize = 14.sp
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 16.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { idx ->
                    val pieza = exposicion.piezas[idx]
                    val estaNarrando = narrandoIndice == idx
                    TarjetaPiezaInvitada(
                        pieza = pieza,
                        numero = idx + 1,
                        total = exposicion.piezas.size,
                        ttsListo = ttsListo,
                        narrando = estaNarrando,
                        onToggleVoz = {
                            if (estaNarrando) {
                                tts?.stop()
                                narrandoIndice = null
                            } else if (ttsListo) {
                                val historia = pieza.notaPersonal?.takeIf { it.isNotBlank() }
                                    ?: "Pieza exhibida en ${exposicion.tituloExposicion}."
                                val textoVoz = "${pieza.nombre}. Franquicia ${pieza.franquicia}. $historia"
                                narrandoIndice = idx
                                tts?.speak(textoVoz, TextToSpeech.QUEUE_FLUSH, null, "expo_$idx")
                            }
                        }
                    )
                }

                // Indicador de página inferior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PIEZA ${pagerState.currentPage + 1} DE ${exposicion.piezas.size} · VISTA VIRTUAL INMUTABLE",
                        color = MutedExpo,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaPiezaInvitada(
    pieza: PiezaCloudDto,
    numero: Int,
    total: Int,
    ttsListo: Boolean,
    narrando: Boolean,
    onToggleVoz: () -> Unit
) {
    val imagen: ImageBitmap? = remember(pieza.fotoBase64) {
        val raw = pieza.fotoBase64?.takeIf { it.isNotBlank() } ?: return@remember null
        runCatching {
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF10131C),
        border = BorderStroke(1.dp, BorderExpo),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Marco de foto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(topStart = 125.dp, topEnd = 125.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1B1E2B), Color(0xFF0A0B0F))
                        )
                    )
                    .border(
                        1.dp,
                        GoldExpo.copy(alpha = 0.45f),
                        RoundedCornerShape(topStart = 125.dp, topEnd = 125.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imagen != null) {
                    Image(
                        bitmap = imagen,
                        contentDescription = pieza.nombre,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )
                } else {
                    Text(
                        text = pieza.nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V",
                        color = GoldExpo.copy(alpha = 0.35f),
                        fontSize = 72.sp,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = String.format(Locale.US, "VITRINA %02d / %02d", numero, total),
                color = GoldExpo,
                fontSize = 9.sp,
                letterSpacing = 2.4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = pieza.nombre,
                color = TextExpo,
                fontSize = 23.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(3.dp))
            val sub = buildString {
                append(pieza.franquicia)
                pieza.linea?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
            }
            Text(
                text = sub,
                color = MutedExpo,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, BorderExpo, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = Fechas.formatearCorto(pieza.fechaAdquisicion),
                        color = GoldExpo,
                        fontSize = 10.5.sp
                    )
                }
                if (pieza.edicionLimitada) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF5D527F), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = pieza.numeroEdicion?.takeIf { it.isNotBlank() }?.let { "Ed. Limitada $it" }
                                ?: "Edición limitada",
                            color = Color(0xFFB9A3E6),
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF26231C))
            Spacer(Modifier.height(12.dp))

            val historia = pieza.notaPersonal?.takeIf { it.isNotBlank() }
                ?: "Pieza exhibida en este museo virtual."
            Text(
                text = "“$historia”",
                color = Color(0xFFDFD7C8),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                onClick = onToggleVoz,
                enabled = ttsListo || narrando,
                shape = RoundedCornerShape(999.dp),
                color = if (narrando) Color(0xFF282012) else Color(0xFF17130C),
                border = BorderStroke(1.dp, if (narrando) GoldExpo else BorderExpo)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (narrando) Icons.Outlined.Stop else Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = GoldExpo,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (narrando) "Detener narración" else "Escuchar historia",
                        color = GoldExpo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Hoja modal unificada para ingresar el código o link de una exposición compartida por otro coleccionista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitarExposicionSheet(
    onDismiss: () -> Unit,
    onVisitarCodigo: (String) -> Unit
) {
    var codigoInput by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF13110D),
        contentColor = TextExpo
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.TravelExplore,
                    contentDescription = null,
                    tint = GoldExpo,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "VISITAR COLECCIÓN COMPARTIDA",
                    color = GoldExpo,
                    fontSize = 10.sp,
                    letterSpacing = 2.2.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Explora la vitrina de un amigo",
                color = TextExpo,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pega el link de invitación o escribe el código de sala (ej. NARUTO-A4F9). La visita es de solo lectura.",
                color = MutedExpo,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = codigoInput,
                onValueChange = { codigoInput = it },
                singleLine = true,
                label = { Text("Código o link de exposición", color = MutedExpo) },
                placeholder = { Text("Ej. NARUTO-A4F9 o https://vitther.app/expo/...", color = Color(0xFF5A5348)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldExpo,
                    unfocusedBorderColor = Color(0xFF332D22),
                    focusedTextColor = TextExpo,
                    unfocusedTextColor = TextExpo,
                    cursorColor = GoldExpo
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val limpia = codigoInput.trim()
                    if (limpia.isNotBlank()) {
                        onDismiss()
                        onVisitarCodigo(limpia)
                    }
                },
                enabled = codigoInput.isNotBlank(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldExpo,
                    contentColor = Color(0xFF1A1405)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Abrir vitrina virtual",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
