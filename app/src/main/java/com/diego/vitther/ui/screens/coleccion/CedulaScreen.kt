package com.diego.vitther.ui.screens.coleccion

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.*
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.*
import java.text.NumberFormat
import java.util.Locale

private val GoldAccent = Color(0xFFE8C97A)
private val GoldCavaAccent = Color(0xFFE8B472)
private val GoldBorderActive = Color(0xFF5A4A22)
private val LineDivider = Color(0xFF26231C)
private val MutedText = Color(0xFF8D8777)
private val VioletLimited = Color(0xFFB9A3E6)
private val VioletBorder = Color(0xFF5D527F)
private val DangerRed = Color(0xFFE07A6E)
private val SheetSurface = Color(0xFF12141A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CedulaScreen(
    pieza: PiezaEntity?,
    todasLasPiezas: List<PiezaEntity> = emptyList(),
    vm: ColeccionViewModel,
    ocupado: Boolean,
    capsulas: List<CapsulaVista>,
    ahora: Long,
    volver: () -> Unit,
    editar: (Long) -> Unit,
    eliminado: () -> Unit
) {
    if (pieza == null) {
        EstadoVacio("Pieza no disponible", "Esta pieza ya no pertenece a tu colección.", "Volver", volver)
        return
    }

    var tabSeleccionada by rememberSaveable(pieza.id) { mutableIntStateOf(0) }
    var mostrarValor by rememberSaveable(pieza.id) { mutableStateOf(false) }
    var mostrarSheet by rememberSaveable(pieza.id) { mutableStateOf(false) }
    var modoContemplar by rememberSaveable(pieza.id) { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }

    val formatoMoneda = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")) }
    val contexto = LocalContext.current
    val archivoFoto = vm.archivo(pieza.fotoPath)
    val paleta = rememberPaletaPieza(pieza.nombre)

    val enBodega = pieza.estado == EstadoPieza.BODEGA
    val gold = if (enBodega) GoldCavaAccent else GoldAccent
    val lineColor = if (enBodega) Color(0xFF33231A) else LineDivider
    val g1 = if (enBodega) Color(0xFF2A170B) else Color(0xFF0D1426)
    val g2 = if (enBodega) Color(0xFF0D0804) else Color(0xFF0A0A0A)

    // Cálculo de SALA / RACK y N.º global para la placa de bronce del museo (.plate)
    val (numeroSala, numeroGlobal) = remember(pieza, todasLasPiezas) {
        val salas = todasLasPiezas.map { it.franquicia }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        val sIdx = (salas.indexOfFirst { it.equals(pieza.franquicia, ignoreCase = true) } + 1).coerceAtLeast(1)
        val ordenadas = todasLasPiezas.sortedBy { it.id }
        val gIdx = (ordenadas.indexOfFirst { it.id == pieza.id } + 1).coerceAtLeast(pieza.id.toInt().coerceAtLeast(1))
        sIdx to gIdx
    }

    // Animaciones de entrada y de Modo Contemplar (.imm en el prototipo HTML)
    val altoStage by animateDpAsState(
        targetValue = if (modoContemplar) 720.dp else 404.dp,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "altoStage"
    )
    val escalaMarco by animateFloatAsState(
        targetValue = if (modoContemplar) 1.32f else 1.0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "escalaMarco"
    )
    val desplazamientoMarcoY by animateDpAsState(
        targetValue = if (modoContemplar) 86.dp else 0.dp,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "offsetMarcoY"
    )
    val opacidadCromo by animateFloatAsState(
        targetValue = if (modoContemplar) 0f else 1f,
        animationSpec = tween(durationMillis = 380),
        label = "opacidadCromo"
    )

    // Partículas de polvo dorado flotando en los haces de luz (.mote)
    val transicionMotes = rememberInfiniteTransition(label = "motes")
    val derivaMote by transicionMotes.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "derivaMote"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to g1,
                    0.62f to g2,
                    1.0f to g2
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // =========================================================================
            // 1. ESCENARIO DE EXHIBICIÓN DE MUSEO (.stage): Riel, Lámparas, Conos de Luz y Marco
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(altoStage)
            ) {
                // Iluminación cenital radial + Riel (.rail) + Lámparas (.lamp) + Haces de luz (.cone) + Motas (.mote)
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Resplandor ambiental central detrás del cuadro
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(gold.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(w * 0.50f, h * 0.40f),
                            radius = w * 0.58f
                        )
                    )

                    // Riel de iluminación (.rail: top 70dp, left 8% a right 8%)
                    val railY = 70.dp.toPx()
                    drawLine(
                        brush = Brush.horizontalGradient(
                            0.0f to Color.Transparent,
                            0.20f to Color(0xFF5A4A22),
                            0.80f to Color(0xFF5A4A22),
                            1.0f to Color.Transparent
                        ),
                        start = Offset(w * 0.08f, railY),
                        end = Offset(w * 0.92f, railY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Dos focos / lámparas de museo al 27% y 73% (.lamp) y sus conos de luz (.cone)
                    val lampPositions = listOf(w * 0.27f to 9f, w * 0.73f to -9f)
                    val coneHeight = if (modoContemplar) 500.dp.toPx() else 290.dp.toPx()
                    val coneWidth = 136.dp.toPx()

                    lampPositions.forEach { (lx, angleDeg) ->
                        // Cono de luz inclinado (.cone)
                        rotate(degrees = angleDeg, pivot = Offset(lx, railY + 6.dp.toPx())) {
                            val left = lx - coneWidth / 2f
                            val top = railY + 6.dp.toPx()
                            val conePath = Path().apply {
                                moveTo(left + coneWidth * 0.42f, top)
                                lineTo(left + coneWidth * 0.58f, top)
                                lineTo(left + coneWidth, top + coneHeight)
                                lineTo(left, top + coneHeight)
                                close()
                            }
                            drawPath(
                                path = conePath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(gold.copy(alpha = 0.20f), Color.Transparent),
                                    startY = top,
                                    endY = top + coneHeight
                                )
                            )
                        }

                        // Halo y cuerpo de la lámpara (.lamp)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(gold.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(lx, railY),
                                radius = 18.dp.toPx()
                            ),
                            radius = 18.dp.toPx(),
                            center = Offset(lx, railY)
                        )
                        drawRoundRect(
                            color = gold,
                            topLeft = Offset(lx - 10.dp.toPx(), railY - 4.dp.toPx()),
                            size = Size(20.dp.toPx(), 10.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    // 4 partículas doradas flotando en la luz (.mote)
                    val motes = listOf(
                        Triple(0.40f, 130.dp.toPx(), 0.0f),
                        Triple(0.58f, 150.dp.toPx(), 0.28f),
                        Triple(0.48f, 110.dp.toPx(), 0.56f),
                        Triple(0.64f, 125.dp.toPx(), 0.80f)
                    )
                    motes.forEach { (rx, baseTop, phase) ->
                        val p = (derivaMote + phase) % 1f
                        val alphaMote = when {
                            p < 0.2f -> (p / 0.2f) * 0.75f
                            p > 0.8f -> ((1f - p) / 0.2f) * 0.75f
                            else -> 0.75f
                        }
                        drawCircle(
                            color = gold.copy(alpha = alphaMote),
                            radius = 1.6.dp.toPx(),
                            center = Offset(
                                x = w * rx + p * 10.dp.toPx(),
                                y = baseTop + p * 54.dp.toPx()
                            )
                        )
                    }
                }

                // MARCO CENTRAL DE MUSEO (.fw / .fm: 216dp x 288dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 92.dp)
                        .offset(y = desplazamientoMarcoY)
                        .width(216.dp)
                        .height(288.dp)
                        .graphicsLayer {
                            scaleX = escalaMarco
                            scaleY = escalaMarco
                        }
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(2.dp),
                            spotColor = gold
                        )
                        .background(Color(0xFF0D0F14), RoundedCornerShape(2.dp))
                        .border(1.dp, Color(0xFF7A6733), RoundedCornerShape(2.dp))
                        .clickable { modoContemplar = !modoContemplar }
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, gold.copy(alpha = 0.32f))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(paleta.first, paleta.second, Color(0xFF0A0A0A)),
                                    radius = 420f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (archivoFoto != null) {
                            // Fondo ambiental sutil + figura completa centrada (ContentScale.Fit) sin recortar cabeza ni pies
                            SubcomposeAsyncImage(
                                model = archivoFoto,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.24f
                            )
                            SubcomposeAsyncImage(
                                model = archivoFoto,
                                contentDescription = "Foto de ${pieza.nombre}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit,
                                loading = { PlaceholderMarcoCedula(pieza.nombre, gold) },
                                error = { PlaceholderMarcoCedula(pieza.nombre, gold) }
                            )
                        } else {
                            PlaceholderMarcoCedula(pieza.nombre, gold)
                        }

                        // Viñeta de galería (.grade) y reflejo diagonal de cristal (.glass)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        0.0f to Color.Transparent,
                                        0.36f to Color.Transparent,
                                        0.43f to Color.White.copy(alpha = 0.08f),
                                        0.50f to Color.Transparent,
                                        1.0f to Color(0x5005060A)
                                    )
                                )
                        )
                    }
                }

                // Reflejo dorado en el piso (.glow) + Piso de duela de madera (.floor)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .alpha(opacidadCromo)
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(200.dp)
                            .height(20.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (-16).dp)
                    ) {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(gold.copy(alpha = 0.30f), Color.Transparent),
                                center = Offset(size.width / 2f, 0f),
                                radius = size.width * 0.50f
                            )
                        )
                    }
                    PisoMaderaMuseo(height = 34.dp)
                }

                // Inscripción minimalista cuando está en Modo Contemplar (.mini)
                androidx.compose.animation.AnimatedVisibility(
                    visible = modoContemplar,
                    enter = fadeIn(tween(450)),
                    exit = fadeOut(tween(250)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 34.dp)
                ) {
                    Text(
                        text = String.format(
                            Locale.forLanguageTag("es-MX"),
                            "N.º %02d · %s",
                            numeroGlobal,
                            pieza.nombre.uppercase(Locale.forLanguageTag("es-MX"))
                        ),
                        fontSize = 9.5.sp,
                        letterSpacing = 2.6.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }

                // Botón circular "Contemplar" (.ex) en la esquina inferior derecha del escenario
                Surface(
                    onClick = { modoContemplar = !modoContemplar },
                    shape = CircleShape,
                    color = Color(0xB30A0A0A),
                    border = BorderStroke(1.dp, GoldBorderActive),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 46.dp)
                        .size(34.dp)
                        .semantics { contentDescription = "Contemplar" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconoMirillaContemplar(color = gold)
                    }
                }
            }

            // =========================================================================
            // 2. SECCIÓN INFERIOR (.below): PLACA DE BRONCE + PESTAÑAS + CONTENIDO
            // =========================================================================
            AnimatedVisibility(
                visible = !modoContemplar,
                enter = fadeIn(tween(350)),
                exit = fadeOut(tween(250))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // PLACA METÁLICA DE MUSEO SUPERPUESTA (.plate)
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color(0xFF3A3322)),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .offset(y = (-18).dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF16130D), Color(0xFF0E0C08))
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val prefijoSala = if (enBodega) "RACK" else "SALA"
                            Text(
                                text = String.format(Locale.US, "%s %02d · N.º %02d", prefijoSala, numeroSala, numeroGlobal),
                                fontSize = 9.sp,
                                letterSpacing = 3.sp,
                                color = gold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = pieza.nombre,
                                fontSize = 31.sp,
                                lineHeight = 33.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF2EAD8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            val subtitulo = buildString {
                                append(pieza.franquicia)
                                pieza.linea?.takeIf { it.isNotBlank() }?.let {
                                    append(" · ")
                                    append(it)
                                }
                            }
                            Text(
                                text = subtitulo,
                                fontSize = 13.sp,
                                color = MutedText,
                                fontWeight = FontWeight.Light,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(12.dp))

                            // Chips centrados (.chips)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChipPlacaMuseo(
                                    texto = pieza.estado.etiqueta,
                                    colorTexto = gold,
                                    colorBorde = GoldBorderActive
                                )
                                if (pieza.edicionLimitada) {
                                    val etiquetaEdicion = pieza.numeroEdicion?.takeIf { it.isNotBlank() }
                                        ?.let { "Edición limitada · $it" } ?: "Edición limitada"
                                    ChipPlacaMuseo(
                                        texto = etiquetaEdicion,
                                        colorTexto = VioletLimited,
                                        colorBorde = VioletBorder
                                    )
                                }
                                if (pieza.destacada) {
                                    ChipPlacaMuseo(
                                        texto = "Destacada",
                                        colorTexto = gold,
                                        colorBorde = GoldBorderActive
                                    )
                                }
                            }
                        }
                    }

                    // 3. PESTAÑAS (.tabs): Cédula | Historia | Cápsulas
                    val pestanas = listOf("Cédula", "Historia", "Cápsulas")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .drawBehind {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                    ) {
                        pestanas.forEachIndexed { indice, titulo ->
                            val seleccionada = tabSeleccionada == indice
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { tabSeleccionada = indice }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = titulo,
                                    color = if (seleccionada) gold else MutedText,
                                    fontSize = 14.sp
                                )
                                if (seleccionada) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .offset(y = 12.dp)
                                            .fillMaxWidth(0.56f)
                                            .height(2.dp)
                                            .background(gold, RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }

                    // 4. CONTENIDO DE LA PESTAÑA ACTIVA (.pn)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 6.dp, bottom = 40.dp)
                            .heightIn(min = 230.dp)
                    ) {
                        when (tabSeleccionada) {
                            0 -> {
                                // PESTAÑA 0: CÉDULA (.rw horizontales limpias)
                                FilaCedulaMuseo(
                                    etiqueta = "ADQUIRIDA",
                                    valor = Fechas.formatearCorto(pieza.fechaAdquisicion),
                                    lineColor = lineColor
                                )
                                FilaCedulaMuseo(
                                    etiqueta = "PROCEDENCIA",
                                    valor = pieza.procedencia?.takeIf { it.isNotBlank() } ?: "Aún no registrada",
                                    lineColor = lineColor
                                )
                                FilaCedulaMuseo(
                                    etiqueta = "UBICACIÓN",
                                    valor = pieza.ubicacion?.takeIf { it.isNotBlank() } ?: "Aún no registrada",
                                    lineColor = lineColor
                                )
                                val valorFormateado = pieza.valorEstimadoCentavos?.let {
                                    formatoMoneda.format(it / 100.0) + " MXN"
                                } ?: "No registrado"
                                FilaCedulaMuseo(
                                    etiqueta = "VALOR ESTIMADO",
                                    valor = if (mostrarValor) valorFormateado else "•••••• MXN",
                                    lineColor = lineColor,
                                    accionDerecha = {
                                        Text(
                                            text = if (mostrarValor) "Ocultar" else "Mostrar",
                                            fontSize = 12.5.sp,
                                            color = gold,
                                            modifier = Modifier
                                                .padding(start = 10.dp)
                                                .clickable { mostrarValor = !mostrarValor }
                                        )
                                    }
                                )
                            }

                            1 -> {
                                // PESTAÑA 1: HISTORIA (.quote con comilla monumental “ y fecha en mayúsculas)
                                val textoHistoria = pieza.notaPersonal?.takeIf { it.isNotBlank() }
                                    ?: "Cada pieza tiene una historia. Puedes escribir la suya al editar la cédula."
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 18.dp)
                                ) {
                                    Text(
                                        text = "“",
                                        fontSize = 56.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = GoldBorderActive,
                                        modifier = Modifier.offset(x = (-4).dp, y = (-14).dp)
                                    )
                                    Text(
                                        text = textoHistoria,
                                        fontSize = 25.sp,
                                        lineHeight = 32.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFFF2EAD8),
                                        modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = Fechas.formatearCorto(pieza.fechaAdquisicion)
                                        .uppercase(Locale.forLanguageTag("es-MX")),
                                    fontSize = 9.sp,
                                    letterSpacing = 2.4.sp,
                                    color = MutedText
                                )
                            }

                            2 -> {
                                // PESTAÑA 2: CÁPSULAS
                                CapsulasSection(pieza.id, capsulas, ahora, vm, ocupado)
                            }
                        }
                    }
                }
            }
        }

        // 5. BARRA SUPERIOR FLOTANTE (.sk): Volver | Editar | Más opciones
        androidx.compose.animation.AnimatedVisibility(
            visible = !modoContemplar,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotonFlotanteCircular(
                    descripcion = "Volver",
                    lineColor = lineColor,
                    onClick = volver
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFFF2EAD8),
                        modifier = Modifier.size(19.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonFlotanteCircular(
                        descripcion = "Editar cédula",
                        lineColor = lineColor,
                        enabled = !ocupado,
                        onClick = { editar(pieza.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = Color(0xFFF2EAD8),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    BotonFlotanteCircular(
                        descripcion = "Más opciones",
                        lineColor = lineColor,
                        enabled = !ocupado,
                        onClick = { mostrarSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null,
                            tint = Color(0xFFF2EAD8),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }

    // 6. HOJA MODAL DE OPCIONES (#sh): Mover a bodega, Destacar, Notificaciones, Eliminar
    if (mostrarSheet) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSheet = false },
            containerColor = SheetSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 8.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF3A3830))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 26.dp)
            ) {
                AccionSheetMuseoItem(
                    texto = if (pieza.estado == EstadoPieza.EXHIBICION) "Mover a bodega" else "Poner en exhibición",
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    enabled = !ocupado,
                    onClick = {
                        mostrarSheet = false
                        vm.mover(pieza)
                    }
                )
                AccionSheetMuseoItem(
                    texto = if (pieza.destacada) "Quitar de destacadas" else "Destacar en Inicio",
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    enabled = !ocupado,
                    onClick = {
                        mostrarSheet = false
                        vm.destacar(pieza)
                    }
                )
                AccionSheetMuseoItem(
                    texto = "Editar cédula",
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    enabled = !ocupado,
                    onClick = {
                        mostrarSheet = false
                        editar(pieza.id)
                    }
                )
                AccionSheetMuseoItem(
                    texto = "Ajustes de notificaciones",
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    enabled = !ocupado,
                    onClick = {
                        mostrarSheet = false
                        contexto.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                        )
                    }
                )
                AccionSheetMuseoItem(
                    texto = "Eliminar pieza",
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    esPeligro = true,
                    ultimo = true,
                    enabled = !ocupado,
                    onClick = {
                        mostrarSheet = false
                        confirmarEliminar = true
                    }
                )
            }
        }
    }

    // 7. DIÁLOGO DE CONFIRMACIÓN PARA ELIMINAR PIEZA
    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { if (!ocupado) confirmarEliminar = false },
            title = { Text("¿Eliminar ${pieza.nombre}?") },
            text = { Text("Se eliminarán la pieza y todas sus cápsulas de tiempo. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    enabled = !ocupado,
                    onClick = { vm.eliminar(pieza, eliminado) }
                ) {
                    Text("Eliminar", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !ocupado,
                    onClick = { confirmarEliminar = false }
                ) {
                    Text("Conservar")
                }
            }
        )
    }
}

@Composable
private fun PlaceholderMarcoCedula(nombre: String, gold: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val inicial = nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
        Text(
            text = inicial,
            fontSize = 88.sp,
            fontFamily = FontFamily.Serif,
            color = gold.copy(alpha = 0.32f)
        )
        Spacer(Modifier.height(6.dp))
        CompositionLocalProvider(LocalContentColor provides gold.copy(alpha = 0.55f)) {
            Emblema(2, Modifier.size(24.dp))
        }
    }
}

@Composable
private fun BotonFlotanteCircular(
    descripcion: String,
    lineColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color(0xBF0E1016),
        border = BorderStroke(1.dp, lineColor),
        modifier = modifier
            .size(40.dp)
            .semantics { contentDescription = descripcion }
    ) {
        Box(contentAlignment = Alignment.Center) {
            contenido()
        }
    }
}

@Composable
private fun ChipPlacaMuseo(
    texto: String,
    colorTexto: Color,
    colorBorde: Color
) {
    Box(
        modifier = Modifier
            .height(26.dp)
            .border(1.dp, colorBorde, RoundedCornerShape(99.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontSize = 11.5.sp
        )
    }
}

@Composable
private fun FilaCedulaMuseo(
    etiqueta: String,
    valor: String,
    lineColor: Color,
    accionDerecha: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = etiqueta,
                color = MutedText,
                fontSize = 9.sp,
                letterSpacing = 2.0.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = valor,
                    color = Color(0xFFF2EAD8),
                    fontSize = 14.5.sp,
                    textAlign = TextAlign.End
                )
                accionDerecha?.invoke()
            }
        }
        HorizontalDivider(color = lineColor, thickness = 1.dp)
    }
}

@Composable
private fun AccionSheetMuseoItem(
    texto: String,
    icono: @Composable () -> Unit,
    esPeligro: Boolean = false,
    ultimo: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clickable(enabled = enabled, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            icono()
            Text(
                text = texto,
                color = if (esPeligro) DangerRed else Color(0xFFF2EAD8),
                fontSize = 15.sp,
                textAlign = TextAlign.Start
            )
        }
        if (!ultimo) {
            HorizontalDivider(color = LineDivider, thickness = 1.dp)
        }
    }
}

@Composable
private fun IconoMirillaContemplar(color: Color) {
    Canvas(Modifier.size(15.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path().apply {
            // Esquina superior izquierda
            moveTo(w * 0.14f, h * 0.38f); lineTo(w * 0.14f, h * 0.14f); lineTo(w * 0.38f, h * 0.14f)
            // Esquina superior derecha
            moveTo(w * 0.86f, h * 0.38f); lineTo(w * 0.86f, h * 0.14f); lineTo(w * 0.62f, h * 0.14f)
            // Esquina inferior izquierda
            moveTo(w * 0.14f, h * 0.62f); lineTo(w * 0.14f, h * 0.86f); lineTo(w * 0.38f, h * 0.86f)
            // Esquina inferior derecha
            moveTo(w * 0.86f, h * 0.62f); lineTo(w * 0.86f, h * 0.86f); lineTo(w * 0.62f, h * 0.86f)
        }
        drawPath(p, color, style = s)
    }
}
