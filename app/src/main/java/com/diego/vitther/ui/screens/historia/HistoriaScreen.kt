package com.diego.vitther.ui.screens.historia

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.domain.CapsulaVista
import com.diego.vitther.domain.Fechas
import com.diego.vitther.domain.Hito
import com.diego.vitther.ui.theme.AccentGold
import com.diego.vitther.ui.theme.TextPrimary
import com.diego.vitther.ui.theme.VitherSerif
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Serializable
data class RecorridoMuseo(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val emblema: String = "star",
    val tema: String = "oro",
    val subtitulo: String = "",
    val orden: String = "manual", // "manual", "fecha", "sala"
    val piezaIds: List<Long> = emptyList()
)

private data class TemaRecorrido(
    val id: String,
    val acento: Color,
    val resplandor: Color,
    val borde: Color,
    val swatchBg: List<Color>
)

private val TEMAS_RECORRIDO = listOf(
    TemaRecorrido(
        id = "oro",
        acento = Color(0xFFE8C97A),
        resplandor = Color(0x38FFE196),
        borde = Color(0xFF6B5832),
        swatchBg = listOf(Color(0xFFE8C97A), Color(0xFF6B5832))
    ),
    TemaRecorrido(
        id = "ambar",
        acento = Color(0xFFE8B472),
        resplandor = Color(0x38FFB46E),
        borde = Color(0xFF6E4B28),
        swatchBg = listOf(Color(0xFFE8B472), Color(0xFF6E4B28))
    ),
    TemaRecorrido(
        id = "carmesi",
        acento = Color(0xFFE58A96),
        resplandor = Color(0x33FF8296),
        borde = Color(0xFF6A333C),
        swatchBg = listOf(Color(0xFFE58A96), Color(0xFF6A333C))
    ),
    TemaRecorrido(
        id = "esmeralda",
        acento = Color(0xFF7FD1A6),
        resplandor = Color(0x3378DCB4),
        borde = Color(0xFF2F5E48),
        swatchBg = listOf(Color(0xFF7FD1A6), Color(0xFF2F5E48))
    ),
    TemaRecorrido(
        id = "noche",
        acento = Color(0xFF9BB8FF),
        resplandor = Color(0x338CAAFF),
        borde = Color(0xFF354872),
        swatchBg = listOf(Color(0xFF9BB8FF), Color(0xFF354872))
    )
)

private val EMBLEMAS_RECORRIDO = listOf("star", "heart", "sword", "crown", "flame", "moon")

private data class GrupoRecorrido(
    val etiqueta: String,
    val numeroGrande: String,
    val titulo: String,
    val cita: String,
    val piezas: List<PiezaEntity>
)

private val jsonRecorridos = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private const val PREFS_RECORRIDOS = "vitther_recorridos_prefs"
private const val KEY_RECORRIDOS = "recorridos_json"

private fun cargarRecorridos(context: Context): List<RecorridoMuseo> {
    val prefs = context.getSharedPreferences(PREFS_RECORRIDOS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_RECORRIDOS, null) ?: return emptyList()
    return runCatching { jsonRecorridos.decodeFromString<List<RecorridoMuseo>>(raw) }
        .getOrDefault(emptyList())
}

private fun guardarRecorridos(context: Context, lista: List<RecorridoMuseo>) {
    val prefs = context.getSharedPreferences(PREFS_RECORRIDOS, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_RECORRIDOS, jsonRecorridos.encodeToString(lista)).apply()
}

private enum class VistaHistoria {
    ENTRADA,
    RECORRIDO,
    EDITOR
}

@Composable
fun HistoriaScreen(
    hitos: List<Hito> = emptyList(),
    piezas: List<PiezaEntity>,
    archivo: (String?) -> File?,
    volver: () -> Unit = {},
    abrir: (Long) -> Unit,
    capsulas: List<CapsulaVista> = emptyList(),
    wishes: List<WishEntity> = emptyList(),
    ahora: Long = System.currentTimeMillis()
) {
    val context = LocalContext.current

    var recorridos by remember { mutableStateOf(cargarRecorridos(context)) }
    var vistaActual by rememberSaveable { mutableStateOf(VistaHistoria.ENTRADA) }
    var recorridoActivoId by rememberSaveable { mutableStateOf<String?>(null) }
    var recorridoEditandoId by rememberSaveable { mutableStateOf<String?>(null) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2200)
            toastMsg = null
        }
    }

    fun persistir(nuevos: List<RecorridoMuseo>) {
        recorridos = nuevos
        guardarRecorridos(context, nuevos)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0A08))
    ) {
        when (vistaActual) {
            VistaHistoria.ENTRADA -> {
                VistaEntradaBoleteria(
                    recorridos = recorridos,
                    piezas = piezas,
                    resolverArchivo = archivo,
                    onIniciarRecorrido = { rec ->
                        val piezasValidas = rec.piezaIds.mapNotNull { id -> piezas.find { it.id == id } }
                        if (piezasValidas.isEmpty()) {
                            recorridoEditandoId = rec.id
                            vistaActual = VistaHistoria.EDITOR
                            toastMsg = "Elige las piezas de este recorrido"
                        } else {
                            recorridoActivoId = rec.id
                            vistaActual = VistaHistoria.RECORRIDO
                        }
                    },
                    onEditarRecorrido = { rec ->
                        recorridoEditandoId = rec.id
                        vistaActual = VistaHistoria.EDITOR
                    },
                    onNuevoRecorrido = {
                        recorridoEditandoId = null
                        vistaActual = VistaHistoria.EDITOR
                    }
                )
            }

            VistaHistoria.RECORRIDO -> {
                val rec = recorridos.find { it.id == recorridoActivoId }
                if (rec == null) {
                    vistaActual = VistaHistoria.ENTRADA
                } else {
                    VistaRecorridoGuiado(
                        recorrido = rec,
                        todasLasPiezas = piezas,
                        resolverArchivo = archivo,
                        onSalir = { vistaActual = VistaHistoria.ENTRADA },
                        onEditar = {
                            recorridoEditandoId = rec.id
                            vistaActual = VistaHistoria.EDITOR
                        },
                        onAbrirPieza = abrir
                    )
                }
            }

            VistaHistoria.EDITOR -> {
                val inicial = recorridos.find { it.id == recorridoEditandoId }
                VistaEditorRecorrido(
                    recorridoInicial = inicial,
                    piezas = piezas,
                    resolverArchivo = archivo,
                    onCancelar = { vistaActual = VistaHistoria.ENTRADA },
                    onMostrarToast = { toastMsg = it },
                    onGuardar = { guardado, iniciarAlGuardar ->
                        val existe = recorridos.any { it.id == guardado.id }
                        val actualizados = if (existe) {
                            recorridos.map { if (it.id == guardado.id) guardado else it }
                        } else {
                            recorridos + guardado
                        }
                        persistir(actualizados)
                        if (iniciarAlGuardar) {
                            recorridoActivoId = guardado.id
                            vistaActual = VistaHistoria.RECORRIDO
                        } else {
                            toastMsg = "Recorrido guardado"
                            vistaActual = VistaHistoria.ENTRADA
                        }
                    },
                    onEliminar = { idEliminar ->
                        persistir(recorridos.filterNot { it.id == idEliminar })
                        toastMsg = "Recorrido eliminado"
                        vistaActual = VistaHistoria.ENTRADA
                    }
                )
            }
        }

        // Toast flotante
        androidx.compose.animation.AnimatedVisibility(
            visible = toastMsg != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xF51A160E))
                    .border(1.dp, Color(0xFF4A3D24), RoundedCornerShape(999.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = toastMsg.orEmpty(),
                    color = AccentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun VistaEntradaBoleteria(
    recorridos: List<RecorridoMuseo>,
    piezas: List<PiezaEntity>,
    resolverArchivo: (String?) -> File?,
    onIniciarRecorrido: (RecorridoMuseo) -> Unit,
    onEditarRecorrido: (RecorridoMuseo) -> Unit,
    onNuevoRecorrido: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp)
    ) {
        // Cabecera (.ehd)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 18.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Historia",
                fontFamily = VitherSerif,
                fontSize = 34.sp,
                color = TextPrimary,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Elige tu recorrido",
                fontFamily = VitherSerif,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                color = Color(0xFF9A9184)
            )
        }

        // Lista de boletos (.tks)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            recorridos.forEach { rec ->
                val piezasRecorrido = rec.piezaIds.mapNotNull { id -> piezas.find { it.id == id } }
                BoletoRecorridoCard(
                    recorrido = rec,
                    piezasRecorrido = piezasRecorrido,
                    resolverArchivo = resolverArchivo,
                    mostrarBotonEditar = true,
                    onClick = { onIniciarRecorrido(rec) },
                    onEditar = { onEditarRecorrido(rec) }
                )
            }

            // Boleto punteado "+ Nuevo recorrido" (.tk.new)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .drawBehind {
                        drawRoundRect(
                            color = Color(0xFF332B1D),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            ),
                            cornerRadius = CornerRadius(14.dp.toPx())
                        )
                    }
                    .clickable(onClick = onNuevoRecorrido)
                    .padding(vertical = 18.dp, horizontal = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF3A3120), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Nuevo recorrido",
                        tint = AccentGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Nuevo recorrido",
                    fontFamily = VitherSerif,
                    fontSize = 19.sp,
                    color = AccentGold
                )
            }
        }

        // Pie de página (.efoot)
        Text(
            text = "${recorridos.size} ${if (recorridos.size == 1) "RECORRIDO" else "RECORRIDOS"} · ${piezas.size} ${if (piezas.size == 1) "PIEZA" else "PIEZAS"}",
            color = Color(0xFF6B6358),
            fontSize = 9.5.sp,
            letterSpacing = 2.4.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun BoletoRecorridoCard(
    recorrido: RecorridoMuseo,
    piezasRecorrido: List<PiezaEntity>,
    resolverArchivo: (String?) -> File?,
    mostrarBotonEditar: Boolean,
    onClick: () -> Unit,
    onEditar: () -> Unit = {}
) {
    val tema = TEMAS_RECORRIDO.find { it.id == recorrido.tema } ?: TEMAS_RECORRIDO.first()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF17130C), Color(0xFF0E0C08))
                )
            )
            .border(1.dp, Color(0xFF2F2719), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(94.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Talón izquierdo del boleto (.nm) con borde derecho punteado
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        drawLine(
                            color = Color(0xFF2C2418),
                            start = Offset(size.width, 8.dp.toPx()),
                            end = Offset(size.width, size.height - 8.dp.toPx()),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                EmblemaIcono(
                    emblema = recorrido.emblema,
                    color = tema.acento,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Información central (.in)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = recorrido.nombre.ifBlank { "Sin título" },
                    fontFamily = VitherSerif,
                    fontSize = 22.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val n = piezasRecorrido.size
                Text(
                    text = "$n ${if (n == 1) "PIEZA" else "PIEZAS"}",
                    color = tema.acento,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.2.sp
                )
            }

            // Mini marcos superpuestos a la derecha (.mn / .mf) + botón editar (.pe)
            Row(
                modifier = Modifier.padding(end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val minis = piezasRecorrido.take(3)
                if (minis.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .width((26 + (minis.size - 1) * 16).dp)
                            .height(38.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        minis.forEachIndexed { idx, p ->
                            val rot = when (idx) {
                                0 -> -5f
                                1 -> 2f
                                else -> 6f
                            }
                            val archivo = remember(p.fotoPath) { resolverArchivo(p.fotoPath) }
                            Box(
                                modifier = Modifier
                                    .offset(x = (idx * 16).dp)
                                    .zIndex(idx.toFloat())
                                    .rotate(rot)
                                    .size(width = 26.dp, height = 35.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF4A3D22), Color(0xFF1F190E))
                                        )
                                    )
                                    .border(1.dp, Color(0xFF6B5832), RoundedCornerShape(3.dp))
                                    .padding(2.dp)
                            ) {
                                if (archivo != null) {
                                    SubcomposeAsyncImage(
                                        model = archivo,
                                        contentDescription = p.nombre,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF120F0A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = p.nombre.take(1).uppercase(),
                                            fontFamily = VitherSerif,
                                            fontSize = 10.sp,
                                            color = tema.acento
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (mostrarBotonEditar) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF17130C))
                            .border(1.dp, Color(0xFF2D2619), CircleShape)
                            .clickable(onClick = onEditar),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar recorrido",
                            tint = Color(0xFF8A8073),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Muescas semicirculares laterales de boleto (.tk::before / .tk::after)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-7).dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(0xFF0B0A08))
                .border(1.dp, Color(0xFF2F2719), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 7.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(0xFF0B0A08))
                .border(1.dp, Color(0xFF2F2719), CircleShape)
        )
    }
}

@Composable
private fun VistaRecorridoGuiado(
    recorrido: RecorridoMuseo,
    todasLasPiezas: List<PiezaEntity>,
    resolverArchivo: (String?) -> File?,
    onSalir: () -> Unit,
    onEditar: () -> Unit,
    onAbrirPieza: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val tema = TEMAS_RECORRIDO.find { it.id == recorrido.tema } ?: TEMAS_RECORRIDO.first()

    val grupos = remember(recorrido, todasLasPiezas) {
        construirGruposRecorrido(recorrido, todasLasPiezas)
    }
    val idPrimeraGlobal = remember(todasLasPiezas) {
        todasLasPiezas.minByOrNull { it.fechaAdquisicion }?.id
    }

    val contexto = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsListo by remember { mutableStateOf(false) }

    DisposableEffect(contexto) {
        var motor: TextToSpeech? = null
        motor = TextToSpeech(contexto.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                motor?.let { m ->
                    val resultado = m.setLanguage(Locale("es", "MX"))
                    if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
                        m.setLanguage(Locale("es", "ES"))
                    }
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

    var grupoIdx by rememberSaveable(recorrido.id) { mutableIntStateOf(0) }
    var paradaIdx by rememberSaveable(recorrido.id) { mutableIntStateOf(-1) } // -1 = Puerta de época/sala
    var enFinal by rememberSaveable(recorrido.id) { mutableStateOf(false) }
    var abriendoPuerta by remember { mutableStateOf(false) }
    var audioguiaActiva by rememberSaveable { mutableStateOf(false) }

    val grupoActual = grupos.getOrElse(grupoIdx) { grupos.firstOrNull() }
    val piezaActual = if (!enFinal && paradaIdx >= 0) grupoActual?.piezas?.getOrNull(paradaIdx) else null

    // Texto de guía del curador + efecto máquina de escribir
    val todasEnRecorrido = remember(grupos) { grupos.flatMap { it.piezas } }
    val indiceGlobal = remember(grupoIdx, paradaIdx, grupos) {
        if (paradaIdx < 0) 0
        else grupos.take(grupoIdx).sumOf { it.piezas.size } + paradaIdx + 1
    }

    val textoGuiaCompleto = remember(piezaActual, indiceGlobal, todasEnRecorrido.size) {
        if (piezaActual == null) ""
        else {
            val partes = mutableListOf<String>()
            if (piezaActual.id == idPrimeraGlobal) {
                partes.add("Aquí empezó tu colección.")
            } else {
                partes.add("Pieza $indiceGlobal de ${todasEnRecorrido.size} en este recorrido.")
            }
            val sub = listOfNotNull(piezaActual.franquicia, piezaActual.linea).joinToString(" · ")
            if (sub.isNotBlank()) partes.add("$sub.")
            partes.joinToString(" ")
        }
    }

    var textoGuiaVisible by remember(piezaActual, audioguiaActiva) {
        mutableStateOf(if (audioguiaActiva) "" else textoGuiaCompleto)
    }

    fun avanzar() {
        tts?.stop()
        val g = grupoActual ?: return
        if (enFinal) return
        if (paradaIdx < 0) {
            if (abriendoPuerta) return
            scope.launch {
                abriendoPuerta = true
                delay(480)
                abriendoPuerta = false
                paradaIdx = 0
            }
            return
        }
        if (paradaIdx < g.piezas.size - 1) {
            paradaIdx++
        } else if (grupoIdx < grupos.size - 1) {
            grupoIdx++
            paradaIdx = -1
        } else {
            enFinal = true
            audioguiaActiva = false
        }
    }

    fun retroceder() {
        tts?.stop()
        if (enFinal) {
            enFinal = false
            grupoIdx = (grupos.size - 1).coerceAtLeast(0)
            paradaIdx = ((grupos.getOrNull(grupoIdx)?.piezas?.size ?: 1) - 1).coerceAtLeast(0)
            return
        }
        if (paradaIdx > 0) {
            paradaIdx--
        } else if (paradaIdx == 0) {
            paradaIdx = -1
        } else if (grupoIdx > 0) {
            grupoIdx--
            paradaIdx = ((grupos.getOrNull(grupoIdx)?.piezas?.size ?: 1) - 1).coerceAtLeast(0)
        }
    }

    // Efecto Audioguía (Voz TextToSpeech + máquina de escribir + avance automático)
    LaunchedEffect(audioguiaActiva, grupoIdx, paradaIdx, enFinal, ttsListo) {
        if (!audioguiaActiva || enFinal) {
            tts?.stop()
            return@LaunchedEffect
        }
        if (paradaIdx < 0) {
            val textoPuerta = listOfNotNull(grupoActual?.titulo, grupoActual?.cita)
                .filter { it.isNotBlank() }
                .joinToString(". ")
            if (textoPuerta.isNotBlank() && ttsListo) {
                tts?.speak(textoPuerta, TextToSpeech.QUEUE_FLUSH, null, "guia_puerta")
            }
            delay(2600)
            if (audioguiaActiva) avanzar()
        } else {
            val textoHablar = listOfNotNull(
                piezaActual?.nombre,
                piezaActual?.franquicia?.let { "Colección $it" },
                textoGuiaCompleto,
                piezaActual?.notaPersonal
            ).filter { it.isNotBlank() }.joinToString(". ")

            if (textoHablar.isNotBlank() && ttsListo) {
                tts?.speak(textoHablar, TextToSpeech.QUEUE_FLUSH, null, "guia_pieza")
            }

            textoGuiaVisible = ""
            for (i in 1..textoGuiaCompleto.length) {
                textoGuiaVisible = textoGuiaCompleto.take(i)
                delay(24)
            }
            delay(3200)
            if (audioguiaActiva) avanzar()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
    ) {
        // Barra superior (.tb)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF15130F))
                    .border(1.dp, Color(0xFF2B251B), CircleShape)
                    .clickable {
                        audioguiaActiva = false
                        tts?.stop()
                        onSalir()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Salir del recorrido",
                    tint = Color(0xFFD8D0C2),
                    modifier = Modifier.size(18.dp)
                )
            }

            val etiquetaSuperior = when {
                enFinal -> "SALIDA"
                grupoActual != null -> grupoActual.etiqueta
                else -> "RECORRIDO"
            }
            Text(
                text = etiquetaSuperior.uppercase(),
                color = Color(0xFF8A8073),
                fontSize = 9.5.sp,
                letterSpacing = 2.6.sp
            )

            // Botón Audioguía (#au)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (audioguiaActiva) Color(0xFF221C11) else Color(0xFF15130F))
                    .border(
                        1.dp,
                        if (audioguiaActiva) tema.acento else Color(0xFF2B251B),
                        CircleShape
                    )
                    .clickable {
                        audioguiaActiva = !audioguiaActiva
                        if (!audioguiaActiva) {
                            tts?.stop()
                            textoGuiaVisible = textoGuiaCompleto
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Headphones,
                    contentDescription = "Audioguía",
                    tint = if (audioguiaActiva) tema.acento else Color(0xFFD8D0C2),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Escenario central (.vp)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            when {
                enFinal -> {
                    // Fin del recorrido (.end)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EmblemaIcono(
                            emblema = recorrido.emblema,
                            color = tema.acento,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "FIN DEL RECORRIDO",
                            color = tema.acento,
                            fontSize = 9.5.sp,
                            letterSpacing = 3.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = recorrido.nombre,
                            fontFamily = VitherSerif,
                            fontSize = 34.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${todasEnRecorrido.size} ${if (todasEnRecorrido.size == 1) "PARADA" else "PARADAS"}",
                            color = Color(0xFF7A7267),
                            fontSize = 9.5.sp,
                            letterSpacing = 2.4.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cada pieza guarda el momento en que llegó a tu vida.",
                            fontFamily = VitherSerif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 16.sp,
                            color = Color(0xFFC9C0B0),
                            textAlign = TextAlign.Center,
                            lineHeight = 23.sp
                        )
                        Spacer(modifier = Modifier.height(22.dp))

                        // Botón primario: Elegir otro recorrido
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF282112), Color(0xFF1B160C))
                                    )
                                )
                                .border(1.dp, tema.borde, RoundedCornerShape(13.dp))
                                .clickable(onClick = onSalir)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Elegir otro recorrido",
                                color = tema.acento,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Volver a empezar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFF17140E))
                                .border(1.dp, Color(0xFF262117), RoundedCornerShape(13.dp))
                                .clickable {
                                    enFinal = false
                                    grupoIdx = 0
                                    paradaIdx = -1
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Volver a empezar",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Editar recorrido
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFF17140E))
                                .border(1.dp, Color(0xFF262117), RoundedCornerShape(13.dp))
                                .clickable(onClick = onEditar)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Editar recorrido",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                paradaIdx < 0 && grupoActual != null -> {
                    // Puerta doble de entrada a la Sala / Época (.ep + .door)
                    PuertaMuseoSala(
                        grupo = grupoActual,
                        tema = tema,
                        abriendo = abriendoPuerta,
                        onAbrirPuerta = { avanzar() }
                    )
                }

                piezaActual != null -> {
                    // Parada de pieza en el escenario iluminado (.stg + .plq)
                    ParadaPiezaMuseo(
                        pieza = piezaActual,
                        esPrimeraPieza = piezaActual.id == idPrimeraGlobal,
                        textoGuia = textoGuiaVisible,
                        tema = tema,
                        resolverArchivo = resolverArchivo,
                        onClickPieza = { onAbrirPieza(piezaActual.id) }
                    )
                }
            }
        }

        // Plano interactivo de salas (.pln)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            grupos.forEachIndexed { gi, g ->
                val esSalaActiva = !enFinal && gi == grupoIdx
                val esSalaPasada = enFinal || gi < grupoIdx
                Row(
                    modifier = Modifier
                        .weight(g.piezas.size.coerceAtLeast(1).toFloat())
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (esSalaActiva) Color(0xFF17130B) else Color(0xFF12100C))
                        .border(
                            1.dp,
                            when {
                                esSalaActiva -> tema.borde
                                esSalaPasada -> Color(0xFF3A3120)
                                else -> Color(0xFF242018)
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    g.piezas.forEachIndexed { pi, _ ->
                        val puntoActivo = esSalaActiva && paradaIdx == pi
                        val puntoPasado = esSalaPasada || (esSalaActiva && paradaIdx > pi)
                        Box(
                            modifier = Modifier
                                .size(if (puntoActivo) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        puntoActivo -> tema.acento
                                        puntoPasado -> Color(0xFF6B5832)
                                        else -> Color(0xFF2D271C)
                                    }
                                )
                                .clickable {
                                    audioguiaActiva = false
                                    enFinal = false
                                    grupoIdx = gi
                                    paradaIdx = pi
                                }
                        )
                    }
                }
            }
        }

        // Controles Anterior / Parada X de Y / Siguiente (.ctl)
        val enInicio = !enFinal && grupoIdx == 0 && paradaIdx < 0
        val etiquetaProgreso = when {
            enFinal -> "FIN · ${todasEnRecorrido.size} DE ${todasEnRecorrido.size}"
            paradaIdx < 0 -> "${grupoActual?.etiqueta.orEmpty()} · PUERTA"
            else -> "PARADA $indiceGlobal DE ${todasEnRecorrido.size}"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF15130F))
                    .border(1.dp, Color(0xFF2B251B), CircleShape)
                    .alpha(if (enInicio) 0.3f else 1f)
                    .clickable(enabled = !enInicio) {
                        audioguiaActiva = false
                        retroceder()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Anterior",
                    tint = tema.acento,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = etiquetaProgreso.uppercase(),
                color = Color(0xFF7A7267),
                fontSize = 9.5.sp,
                letterSpacing = 2.4.sp
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF15130F))
                    .border(1.dp, Color(0xFF2B251B), CircleShape)
                    .alpha(if (enFinal) 0.3f else 1f)
                    .clickable(enabled = !enFinal) {
                        audioguiaActiva = false
                        avanzar()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Siguiente",
                    tint = tema.acento,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PuertaMuseoSala(
    grupo: GrupoRecorrido,
    tema: TemaRecorrido,
    abriendo: Boolean,
    onAbrirPuerta: () -> Unit
) {
    val progresoApertura by animateFloatAsState(
        targetValue = if (abriendo) 1f else 0f,
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "doorSlide"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Marca de agua gigante (.big) + Puerta doble (.door)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = grupo.numeroGrande,
                fontFamily = VitherSerif,
                fontStyle = FontStyle.Italic,
                fontSize = 86.sp,
                color = tema.acento.copy(alpha = 0.16f),
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Marco de las puertas dobles del museo (.door)
            Box(
                modifier = Modifier
                    .padding(top = 34.dp)
                    .size(width = 148.dp, height = 196.dp)
                    .clip(RoundedCornerShape(topStart = 74.dp, topEnd = 74.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(tema.resplandor, Color(0xFF0A0806))
                        )
                    )
                    .border(
                        1.dp,
                        tema.borde,
                        RoundedCornerShape(topStart = 74.dp, topEnd = 74.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    )
                    .clickable(onClick = onAbrirPuerta)
            ) {
                // Hoja izquierda (.dl)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(74.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = (-62.dp) * progresoApertura)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1F1910), Color(0xFF120E09))
                            )
                        )
                        .border(0.5.dp, Color(0xFF332A1A))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 7.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, tema.acento, CircleShape)
                    )
                }

                // Hoja derecha (.dr)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(74.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 62.dp * progresoApertura)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1F1910), Color(0xFF120E09))
                            )
                        )
                        .border(0.5.dp, Color(0xFF332A1A))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 7.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, tema.acento, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = grupo.etiqueta.uppercase(),
            color = tema.acento,
            fontSize = 9.5.sp,
            letterSpacing = 3.2.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = grupo.titulo,
            fontFamily = VitherSerif,
            fontSize = 34.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${grupo.piezas.size} ${if (grupo.piezas.size == 1) "PIEZA" else "PIEZAS"}",
            color = Color(0xFF7A7267),
            fontSize = 9.5.sp,
            letterSpacing = 2.4.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = grupo.cita,
            fontFamily = VitherSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 15.5.sp,
            color = Color(0xFFC9C0B0),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun ParadaPiezaMuseo(
    pieza: PiezaEntity,
    esPrimeraPieza: Boolean,
    textoGuia: String,
    tema: TemaRecorrido,
    resolverArchivo: (String?) -> File?,
    onClickPieza: () -> Unit
) {
    val archivo = remember(pieza.fotoPath) { resolverArchivo(pieza.fotoPath) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Escenario iluminado (.stg)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(262.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val railY = 8.dp.toPx()

                // Riel
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF2E271B),
                            Color(0xFF473C27),
                            Color(0xFF2E271B),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.12f, railY),
                    end = Offset(w * 0.88f, railY),
                    strokeWidth = 2.dp.toPx()
                )

                // Dos lámparas y conos de luz con el color del tema
                listOf(w * 0.28f, w * 0.72f).forEach { lx ->
                    val cone = Path().apply {
                        moveTo(lx - 16.dp.toPx(), railY + 8.dp.toPx())
                        lineTo(lx + 16.dp.toPx(), railY + 8.dp.toPx())
                        lineTo(w * 0.5f + (lx - w * 0.5f) * 1.35f + 65.dp.toPx(), h)
                        lineTo(w * 0.5f + (lx - w * 0.5f) * 1.35f - 65.dp.toPx(), h)
                        close()
                    }
                    drawPath(
                        path = cone,
                        brush = Brush.verticalGradient(
                            colors = listOf(tema.resplandor, Color.Transparent),
                            startY = railY,
                            endY = h
                        )
                    )
                    drawRoundRect(
                        color = Color(0xFF4A3E28),
                        topLeft = Offset(lx - 6.dp.toPx(), railY - 2.dp.toPx()),
                        size = Size(12.dp.toPx(), 7.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                    drawCircle(
                        color = tema.acento,
                        radius = 3.5.dp.toPx(),
                        center = Offset(lx, railY + 7.dp.toPx())
                    )
                }

                // Línea de horizonte dorada (.pond)
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, tema.borde, Color.Transparent)
                    ),
                    start = Offset(w * 0.5f - 90.dp.toPx(), h - 2.dp.toPx()),
                    end = Offset(w * 0.5f + 90.dp.toPx(), h - 2.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Marco de museo (.fm)
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .width(156.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF4A3D22),
                                Color(0xFF241D11),
                                Color(0xFF56472A),
                                Color(0xFF1F190E)
                            )
                        )
                    )
                    .border(1.dp, tema.borde, RoundedCornerShape(6.dp))
                    .clickable(onClick = onClickPieza)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF0C0A07)),
                    contentAlignment = Alignment.Center
                ) {
                    if (archivo != null) {
                        SubcomposeAsyncImage(
                            model = archivo,
                            contentDescription = pieza.nombre,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = pieza.nombre.take(1).uppercase().ifEmpty { "V" },
                            fontFamily = VitherSerif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 42.sp,
                            color = tema.acento.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        // Cédula y relato de la parada (.plq)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Insignia de hito (.ml)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF17130B))
                    .border(1.dp, Color(0xFF3A301E), RoundedCornerShape(999.dp))
                    .padding(horizontal = 11.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (esPrimeraPieza) "✦ TU PRIMERA PIEZA" else "✦ ADQUIRIDA",
                    color = tema.acento,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pieza.nombre,
                fontFamily = VitherSerif,
                fontSize = 30.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Adquirida el ${Fechas.formatearLargo(pieza.fechaAdquisicion)}",
                color = tema.acento,
                fontSize = 11.sp
            )

            if (textoGuia.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = textoGuia,
                    color = Color(0xFF8A8073),
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            val historiaTexto = pieza.notaPersonal?.takeIf { it.isNotBlank() }
                ?: pieza.procedencia?.takeIf { it.isNotBlank() }?.let { "Procedencia: $it" }
            if (!historiaTexto.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "“$historiaTexto”",
                    fontFamily = VitherSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 15.5.sp,
                    color = Color(0xFFCFC6B6),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun VistaEditorRecorrido(
    recorridoInicial: RecorridoMuseo?,
    piezas: List<PiezaEntity>,
    resolverArchivo: (String?) -> File?,
    onCancelar: () -> Unit,
    onMostrarToast: (String) -> Unit,
    onGuardar: (RecorridoMuseo, Boolean) -> Unit,
    onEliminar: (String) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf(recorridoInicial?.nombre.orEmpty()) }
    var emblema by rememberSaveable { mutableStateOf(recorridoInicial?.emblema ?: "star") }
    var temaId by rememberSaveable { mutableStateOf(recorridoInicial?.tema ?: "oro") }
    var subtitulo by rememberSaveable { mutableStateOf(recorridoInicial?.subtitulo.orEmpty()) }
    var orden by rememberSaveable { mutableStateOf(recorridoInicial?.orden ?: "manual") }

    val seleccionadas = remember {
        mutableStateListOf<Long>().apply {
            val iniciales = recorridoInicial?.piezaIds
                ?: piezas.take(3).map { it.id }
            addAll(iniciales)
        }
    }

    val temaActual = TEMAS_RECORRIDO.find { it.id == temaId } ?: TEMAS_RECORRIDO.first()
    val previewRecorrido = RecorridoMuseo(
        id = recorridoInicial?.id ?: "preview",
        nombre = nombre.ifBlank { "Sin título" },
        emblema = emblema,
        tema = temaId,
        subtitulo = subtitulo,
        orden = orden,
        piezaIds = seleccionadas.toList()
    )
    val piezasPreview = seleccionadas.mapNotNull { id -> piezas.find { it.id == id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp)
    ) {
        // Barra superior (.tb)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF15130F))
                    .border(1.dp, Color(0xFF2B251B), CircleShape)
                    .clickable(onClick = onCancelar),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Volver",
                    tint = Color(0xFFD8D0C2),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "DISEÑA TU RECORRIDO",
                color = Color(0xFF8A8073),
                fontSize = 9.5.sp,
                letterSpacing = 2.6.sp
            )
            Spacer(modifier = Modifier.size(34.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
        ) {
            // Vista previa del boleto en tiempo real (#prv)
            Box(modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)) {
                BoletoRecorridoCard(
                    recorrido = previewRecorrido,
                    piezasRecorrido = piezasPreview,
                    resolverArchivo = resolverArchivo,
                    mostrarBotonEditar = false,
                    onClick = {}
                )
            }

            // Input centrado del nombre (.in.big)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF17140E))
                    .border(1.dp, Color(0xFF252017), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                if (nombre.isEmpty()) {
                    Text(
                        text = "Nombre del recorrido",
                        fontFamily = VitherSerif,
                        fontSize = 22.sp,
                        color = Color(0xFF6B6358),
                        textAlign = TextAlign.Center
                    )
                }
                BasicTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    singleLine = true,
                    cursorBrush = SolidColor(temaActual.acento),
                    textStyle = TextStyle(
                        fontFamily = VitherSerif,
                        fontSize = 22.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // EMBLEMA (#emR)
            EtiquetaSeccionEditor("EMBLEMA")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMBLEMAS_RECORRIDO.forEach { key ->
                    val activo = emblema == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (activo) Color(0xFF221C11) else Color(0xFF15120D))
                            .border(
                                1.dp,
                                if (activo) temaActual.acento else Color(0xFF252017),
                                RoundedCornerShape(11.dp)
                            )
                            .clickable { emblema = key },
                        contentAlignment = Alignment.Center
                    ) {
                        EmblemaIcono(
                            emblema = key,
                            color = if (activo) temaActual.acento else Color(0xFF8A8073),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // TEMA (#thR)
            EtiquetaSeccionEditor("TEMA")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TEMAS_RECORRIDO.forEach { t ->
                    val activo = temaId == t.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.linearGradient(t.swatchBg))
                            .border(
                                width = if (activo) 2.dp else 1.5.dp,
                                color = if (activo) TextPrimary else Color.Transparent,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable { temaId = t.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Palabras del curador (opcional)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF17140E))
                    .border(1.dp, Color(0xFF252017), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                if (subtitulo.isEmpty()) {
                    Text(
                        text = "Palabras del curador (opcional)",
                        fontSize = 13.sp,
                        color = Color(0xFF6B6358)
                    )
                }
                BasicTextField(
                    value = subtitulo,
                    onValueChange = { subtitulo = it },
                    singleLine = true,
                    cursorBrush = SolidColor(temaActual.acento),
                    textStyle = TextStyle(
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // CÓMO RECORRERLO (#ordR)
            EtiquetaSeccionEditor("CÓMO RECORRERLO")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF0E0C09))
                    .border(1.dp, Color(0xFF242018), RoundedCornerShape(11.dp))
                    .padding(3.dp)
            ) {
                listOf(
                    "manual" to "Mi orden",
                    "fecha" to "Por años",
                    "sala" to "Por salas"
                ).forEach { (clave, etiqueta) ->
                    val activo = orden == clave
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activo) Color(0xFF211C13) else Color.Transparent)
                            .border(
                                1.dp,
                                if (activo) Color(0xFF4A3D24) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { orden = clave }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = etiqueta,
                            color = if (activo) temaActual.acento else Color(0xFF7A7267),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ELIGE LAS PIEZAS (#pgR)
            EtiquetaSeccionEditor("ELIGE LAS PIEZAS")
            if (piezas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF14110C))
                        .border(1.dp, Color(0xFF252017), RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Agrega primero tus piezas en el museo para incluirlas en un recorrido.",
                        fontFamily = VitherSerif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        color = Color(0xFF9A9184),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val filas = (piezas.size + 2) / 3
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (f in 0 until filas) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (c in 0..2) {
                                val idx = f * 3 + c
                                if (idx < piezas.size) {
                                    val p = piezas[idx]
                                    val ordenSel = seleccionadas.indexOf(p.id)
                                    val estaSeleccionada = ordenSel >= 0
                                    val archivo = remember(p.fotoPath) { resolverArchivo(p.fotoPath) }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .alpha(if (estaSeleccionada) 1f else 0.45f)
                                            .clickable {
                                                if (estaSeleccionada) {
                                                    seleccionadas.remove(p.id)
                                                } else {
                                                    seleccionadas.add(p.id)
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(3f / 4f)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF3A301C), Color(0xFF1B160D))
                                                    )
                                                )
                                                .border(
                                                    1.dp,
                                                    if (estaSeleccionada) temaActual.acento else Color(0xFF4A3D22),
                                                    RoundedCornerShape(5.dp)
                                                )
                                                .padding(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color(0xFF0C0A07)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (archivo != null) {
                                                    SubcomposeAsyncImage(
                                                        model = archivo,
                                                        contentDescription = p.nombre,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text(
                                                        text = p.nombre.take(1).uppercase(),
                                                        fontFamily = VitherSerif,
                                                        fontSize = 20.sp,
                                                        color = temaActual.acento
                                                    )
                                                }
                                            }

                                            if (estaSeleccionada) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(3.dp)
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(temaActual.acento),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = (ordenSel + 1).toString(),
                                                        color = Color(0xFF14110A),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = p.nombre,
                                            fontFamily = VitherSerif,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botón primario: "Abrir las puertas" / "Guardar"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF282112), Color(0xFF1B160C))
                        )
                    )
                    .border(1.dp, temaActual.borde, RoundedCornerShape(13.dp))
                    .clickable {
                        if (nombre.isBlank()) {
                            onMostrarToast("Ponle nombre al recorrido")
                            return@clickable
                        }
                        if (seleccionadas.isEmpty()) {
                            onMostrarToast("Elige al menos una pieza")
                            return@clickable
                        }
                        val nuevo = RecorridoMuseo(
                            id = recorridoInicial?.id ?: UUID.randomUUID().toString(),
                            nombre = nombre.trim(),
                            emblema = emblema,
                            tema = temaId,
                            subtitulo = subtitulo.trim(),
                            orden = orden,
                            piezaIds = seleccionadas.toList()
                        )
                        onGuardar(nuevo, recorridoInicial == null)
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (recorridoInicial != null) "Guardar" else "Abrir las puertas",
                    color = temaActual.acento,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (recorridoInicial != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF17140E))
                        .border(1.dp, Color(0xFF262117), RoundedCornerShape(13.dp))
                        .clickable { onEliminar(recorridoInicial.id) }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Eliminar recorrido",
                        color = Color(0xFFE07A6E),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EtiquetaSeccionEditor(texto: String) {
    Text(
        text = texto,
        color = Color(0xFF7A7267),
        fontSize = 9.sp,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 7.dp, start = 2.dp)
    )
}

@Composable
private fun EmblemaIcono(
    emblema: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (emblema) {
            "heart" -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.85f)
                    cubicTo(w * 0.15f, h * 0.62f, w * 0.10f, h * 0.30f, w * 0.30f, h * 0.20f)
                    cubicTo(w * 0.42f, h * 0.14f, w * 0.50f, h * 0.25f, w * 0.50f, h * 0.32f)
                    cubicTo(w * 0.50f, h * 0.25f, w * 0.58f, h * 0.14f, w * 0.70f, h * 0.20f)
                    cubicTo(w * 0.90f, h * 0.30f, w * 0.85f, h * 0.62f, w * 0.50f, h * 0.85f)
                    close()
                }
                drawPath(p, color = color, style = stroke)
            }

            "sword" -> {
                drawLine(color, Offset(w * 0.62f, h * 0.15f), Offset(w * 0.85f, h * 0.15f), stroke.width)
                drawLine(color, Offset(w * 0.85f, h * 0.15f), Offset(w * 0.85f, h * 0.38f), stroke.width)
                drawLine(color, Offset(w * 0.85f, h * 0.15f), Offset(w * 0.28f, h * 0.72f), stroke.width)
                drawLine(color, Offset(w * 0.20f, h * 0.62f), Offset(w * 0.38f, h * 0.80f), stroke.width)
                drawLine(color, Offset(w * 0.14f, h * 0.86f), Offset(w * 0.26f, h * 0.74f), stroke.width)
            }

            "crown" -> {
                val p = Path().apply {
                    moveTo(w * 0.14f, h * 0.76f)
                    lineTo(w * 0.22f, h * 0.30f)
                    lineTo(w * 0.50f, h * 0.54f)
                    lineTo(w * 0.78f, h * 0.30f)
                    lineTo(w * 0.86f, h * 0.76f)
                    close()
                }
                drawPath(p, color = color, style = stroke)
            }

            "flame" -> {
                val p = Path().apply {
                    moveTo(w * 0.50f, h * 0.12f)
                    cubicTo(w * 0.68f, h * 0.34f, w * 0.82f, h * 0.48f, w * 0.76f, h * 0.70f)
                    cubicTo(w * 0.72f, h * 0.84f, w * 0.58f, h * 0.90f, w * 0.50f, h * 0.90f)
                    cubicTo(w * 0.35f, h * 0.90f, w * 0.22f, h * 0.78f, w * 0.24f, h * 0.62f)
                    cubicTo(w * 0.26f, h * 0.46f, w * 0.40f, h * 0.34f, w * 0.50f, h * 0.12f)
                    close()
                }
                drawPath(p, color = color, style = stroke)
            }

            "moon" -> {
                val p = Path().apply {
                    moveTo(w * 0.78f, h * 0.60f)
                    cubicTo(w * 0.65f, h * 0.84f, w * 0.34f, h * 0.84f, w * 0.22f, h * 0.62f)
                    cubicTo(w * 0.12f, h * 0.42f, w * 0.24f, h * 0.18f, w * 0.48f, h * 0.15f)
                    cubicTo(w * 0.34f, h * 0.32f, w * 0.38f, h * 0.58f, w * 0.58f, h * 0.64f)
                    cubicTo(w * 0.66f, h * 0.66f, w * 0.72f, h * 0.64f, w * 0.78f, h * 0.60f)
                    close()
                }
                drawPath(p, color = color, style = stroke)
            }

            else -> {
                // "star" (estrella de 4 puntas)
                val p = Path().apply {
                    moveTo(w * 0.50f, h * 0.10f)
                    lineTo(w * 0.60f, h * 0.40f)
                    lineTo(w * 0.90f, h * 0.50f)
                    lineTo(w * 0.60f, h * 0.60f)
                    lineTo(w * 0.50f, h * 0.90f)
                    lineTo(w * 0.40f, h * 0.60f)
                    lineTo(w * 0.10f, h * 0.50f)
                    lineTo(w * 0.40f, h * 0.40f)
                    close()
                }
                drawPath(p, color = color, style = stroke)
            }
        }
    }
}

private fun construirGruposRecorrido(
    recorrido: RecorridoMuseo,
    todasLasPiezas: List<PiezaEntity>
): List<GrupoRecorrido> {
    val lista = recorrido.piezaIds.mapNotNull { id -> todasLasPiezas.find { it.id == id } }
    if (lista.isEmpty()) return emptyList()

    val romanos = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

    return when (recorrido.orden) {
        "fecha" -> {
            val ordenadas = lista.sortedBy { it.fechaAdquisicion }
            val porAnio = ordenadas.groupBy { p ->
                Instant.ofEpochMilli(p.fechaAdquisicion)
                    .atZone(ZoneId.systemDefault())
                    .year
            }
            porAnio.entries.mapIndexed { idx, (anio, piezasAnio) ->
                val rom = romanos.getOrElse(idx) { (idx + 1).toString() }
                GrupoRecorrido(
                    etiqueta = "ÉPOCA $rom",
                    numeroGrande = anio.toString(),
                    titulo = "El año $anio",
                    cita = recorrido.subtitulo.ifBlank { "Las piezas que llegaron a tu museo en $anio." },
                    piezas = piezasAnio
                )
            }
        }

        "sala" -> {
            val porSala = lista.groupBy { it.franquicia.ifBlank { "Colección" } }
            porSala.entries.mapIndexed { idx, (franquicia, piezasSala) ->
                val num = (idx + 1).toString().padStart(2, '0')
                val rom = romanos.getOrElse(idx) { (idx + 1).toString() }
                GrupoRecorrido(
                    etiqueta = "SALA $num",
                    numeroGrande = rom,
                    titulo = franquicia,
                    cita = recorrido.subtitulo.ifBlank { "Sala dedicada a $franquicia." },
                    piezas = piezasSala
                )
            }
        }

        else -> {
            listOf(
                GrupoRecorrido(
                    etiqueta = "RECORRIDO",
                    numeroGrande = "I",
                    titulo = recorrido.nombre,
                    cita = recorrido.subtitulo.ifBlank { "Un recorrido íntimo por tus piezas." },
                    piezas = lista
                )
            )
        }
    }
}
