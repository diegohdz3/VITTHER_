package com.diego.vitther.ui.screens.coleccion

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.*
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.FotoPieza
import com.diego.vitther.ui.components.rememberPaletaPieza
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private val FranquiciasBaseMuseo = listOf("Berserk", "Dragon Ball", "Marvel", "Zelda")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    pieza: PiezaEntity?,
    todasLasPiezas: List<PiezaEntity> = emptyList(),
    vm: ColeccionViewModel,
    ocupado: Boolean,
    franquiciaInicial: String? = null,
    volver: () -> Unit,
    guardado: (Long) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf(pieza?.nombre ?: "") }
    var franquicia by rememberSaveable { mutableStateOf(pieza?.franquicia ?: franquiciaInicial.orEmpty()) }
    var linea by rememberSaveable { mutableStateOf(pieza?.linea ?: "") }
    var edicionLimitada by rememberSaveable { mutableStateOf(pieza?.edicionLimitada ?: false) }
    var numeroEdicion by rememberSaveable { mutableStateOf(pieza?.numeroEdicion ?: "") }
    var valorPesos by rememberSaveable {
        mutableStateOf(pieza?.valorEstimadoCentavos?.let { (it / 100).toString() } ?: "")
    }
    var fecha by rememberSaveable { mutableStateOf(pieza?.fechaAdquisicion ?: Fechas.desdeFecha(LocalDate.now())) }
    val fechaInicial = rememberSaveable { pieza?.fechaAdquisicion ?: Fechas.desdeFecha(LocalDate.now()) }
    var foto by rememberSaveable { mutableStateOf(pieza?.fotoPath) }
    var enExhibicion by rememberSaveable { mutableStateOf((pieza?.estado ?: EstadoPieza.EXHIBICION) == EstadoPieza.EXHIBICION) }
    var procedencia by rememberSaveable { mutableStateOf(pieza?.procedencia ?: "") }
    var ubicacion by rememberSaveable { mutableStateOf(pieza?.ubicacion ?: "") }
    var nota by rememberSaveable { mutableStateOf(pieza?.notaPersonal ?: "") }
    var validar by rememberSaveable { mutableStateOf(false) }

    // Estados de hojas modales (#shP foto, #shC historia, calendario y "+ Nueva sala")
    var mostrarSheetFoto by rememberSaveable { mutableStateOf(false) }
    var mostrarSheetHistoria by rememberSaveable { mutableStateOf(false) }
    var calendario by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    var creandoNuevaSala by rememberSaveable { mutableStateOf(false) }
    var textoNuevaSala by rememberSaveable { mutableStateOf("") }
    var salasExtra by rememberSaveable { mutableStateOf(listOf<String>()) }

    // Estado de pantalla final de celebración (#s2 en el prototipo HTML)
    var idRecienCreado by rememberSaveable { mutableStateOf<Long?>(null) }

    var uriCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var archivoCamara by remember { mutableStateOf<File?>(null) }
    var errorCamara by remember { mutableStateOf<String?>(null) }

    val sugerenciasApi by vm.sugerenciasRemotas.collectAsStateWithLifecycle()
    val buscandoApi by vm.buscandoApi.collectAsStateWithLifecycle()

    val contexto = LocalContext.current
    val errores = validarPieza(nombre, franquicia, fecha)
    val modificado = nombre != (pieza?.nombre ?: "") || franquicia != (pieza?.franquicia ?: "") ||
        linea != (pieza?.linea ?: "") || edicionLimitada != (pieza?.edicionLimitada ?: false) ||
        numeroEdicion != (pieza?.numeroEdicion ?: "") ||
        foto != pieza?.fotoPath || nota != (pieza?.notaPersonal ?: "") ||
        procedencia != (pieza?.procedencia ?: "") || ubicacion != (pieza?.ubicacion ?: "") ||
        fecha != fechaInicial

    val tieneHistoriaOpcional = linea.isNotBlank() || edicionLimitada ||
        procedencia.isNotBlank() || ubicacion.isNotBlank() ||
        valorPesos.isNotBlank() || nota.isNotBlank()

    // Lista combinada de Salas/Franquicias disponibles
    val listaSalas = remember(todasLasPiezas, salasExtra, franquicia) {
        val existentes = todasLasPiezas.map { it.franquicia.trim() }.filter { it.isNotBlank() }
        (FranquiciasBaseMuseo + existentes + salasExtra + listOfNotNull(franquicia.takeIf { it.isNotBlank() }))
            .distinctBy { it.lowercase(Locale.ROOT) }
    }

    val numeroSalaActual = remember(franquicia, listaSalas) {
        val idx = listaSalas.indexOfFirst { it.equals(franquicia.trim(), ignoreCase = true) }
        if (idx >= 0) idx + 1 else listaSalas.size + 1
    }

    val totalPiezasMuseo = todasLasPiezas.size
    val totalSalasMuseo = todasLasPiezas.map { it.franquicia.lowercase(Locale.ROOT) }.distinct().size
        .coerceAtLeast(listaSalas.size)

    val salir: () -> Unit = {
        if (idRecienCreado != null) {
            guardado(idRecienCreado!!)
        } else if (!ocupado) {
            vm.limpiarSugerenciasRemotas()
            if (modificado) confirmarSalida = true else volver()
        }
    }
    BackHandler { salir() }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            vm.importar(it) { ruta ->
                foto = ruta
                mostrarSheetFoto = false
            }
        }
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) uriCamara?.let { uri ->
            vm.importar(Uri.parse(uri)) { ruta ->
                foto = ruta
                archivoCamara?.delete()
                archivoCamara = null
                mostrarSheetFoto = false
            }
        } else {
            archivoCamara?.delete()
            archivoCamara = null
        }
    }

    val lanzarCamara: () -> Unit = {
        try {
            val carpeta = File(contexto.cacheDir, "camara").apply { mkdirs() }
            val archivo = File.createTempFile("captura_", ".jpg", carpeta)
            archivoCamara = archivo
            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.files", archivo)
            uriCamara = uri.toString()
            camara.launch(uri)
        } catch (_: Exception) {
            archivoCamara?.delete()
            archivoCamara = null
            errorCamara = "No se pudo abrir la cámara. Prueba con Galería."
        }
    }

    val ejecutarGuardado: () -> Unit = {
        validar = true
        if (!errores.hayErrores) {
            val centavos = valorPesos.toLongOrNull()?.times(100L)
            val estadoElegido = if (enExhibicion) EstadoPieza.EXHIBICION else EstadoPieza.BODEGA
            val base = pieza ?: PiezaEntity(
                nombre = nombre.trim(),
                franquicia = franquicia.trim(),
                fechaAdquisicion = fecha,
                estado = estadoElegido,
                destacada = enExhibicion
            )
            vm.guardar(
                base.copy(
                    nombre = nombre.trim(),
                    franquicia = franquicia.trim(),
                    estado = estadoElegido,
                    linea = linea.ifBlank { null },
                    edicionLimitada = edicionLimitada,
                    numeroEdicion = if (edicionLimitada) numeroEdicion.ifBlank { null } else null,
                    valorEstimadoCentavos = centavos,
                    fechaAdquisicion = fecha,
                    fotoPath = foto,
                    procedencia = procedencia,
                    ubicacion = ubicacion,
                    notaPersonal = nota
                )
            ) { nuevoId ->
                if (pieza == null) {
                    idRecienCreado = nuevoId
                } else {
                    guardado(nuevoId)
                }
            }
        }
    }

    // Paleta dinámica: Museo (#0B1224 -> #0A0A0A) vs Bodega/Cava (#27150A -> #0D0804)
    val gold = if (enExhibicion) Color(0xFFE8C97A) else Color(0xFFE8B472)
    val lineColor = if (enExhibicion) Color(0xFF26231C) else Color(0xFF33231A)
    val muted = Color(0xFF8D8777)
    val topBg = if (enExhibicion) Color(0xFF0B1224) else Color(0xFF27150A)
    val botBg = if (enExhibicion) Color(0xFF0A0A0A) else Color(0xFF0D0804)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to topBg,
                    0.62f to botBg,
                    1.0f to botBg
                )
            )
    ) {
        // Retícula sutil superior de rombos dorados (.phone::before en el prototipo)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
        ) {
            val w = size.width
            val h = size.height
            val step = 34.dp.toPx()
            val c = gold.copy(alpha = 0.045f)
            var x = -h
            while (x < w + h) {
                drawLine(c, Offset(x, 0f), Offset(x + h * 0.6f, h), strokeWidth = 1.dp.toPx())
                drawLine(c, Offset(x + h * 0.6f, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
                x += step
            }
        }

        if (idRecienCreado != null) {
            // ==================== PANTALLA FINAL (#s2): CELEBRACIÓN EN SALA ====================
            val piezasMismaSala = remember(todasLasPiezas, franquicia, idRecienCreado) {
                todasLasPiezas.filter {
                    it.franquicia.equals(franquicia.trim(), ignoreCase = true) && it.id != idRecienCreado
                }.take(3)
            }
            PantallaCelebracionPieza(
                nombrePieza = nombre.trim(),
                franquicia = franquicia.trim(),
                numeroSala = numeroSalaActual,
                numeroGlobal = totalPiezasMuseo.coerceAtLeast(1),
                enExhibicion = enExhibicion,
                fotoNueva = vm.archivo(foto),
                piezasExistentesSala = piezasMismaSala,
                vm = vm,
                gold = gold,
                onAnadirOtra = {
                    idRecienCreado = null
                    nombre = ""
                    foto = null
                    linea = ""
                    edicionLimitada = false
                    numeroEdicion = ""
                    procedencia = ""
                    ubicacion = ""
                    valorPesos = ""
                    nota = ""
                    enExhibicion = true
                    validar = false
                },
                onVerEnMuseo = {
                    guardado(idRecienCreado!!)
                }
            )
        } else {
            // ==================== PASO ÚNICO (#s1): NICHO CENTRAL + CARTELA ====================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. BARRA SUPERIOR (.top): Botón circular Volver + "NUEVA PIEZA"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = salir,
                        enabled = !ocupado,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xB312141A), CircleShape)
                            .border(1.dp, lineColor, CircleShape)
                            .semantics { contentDescription = "Volver" }
                    ) {
                        Canvas(Modifier.size(18.dp)) {
                            drawLine(
                                color = Color(0xFFF2EAD8),
                                start = Offset(size.width * 0.64f, size.height * 0.22f),
                                end = Offset(size.width * 0.30f, size.height * 0.50f),
                                strokeWidth = 1.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFFF2EAD8),
                                start = Offset(size.width * 0.30f, size.height * 0.50f),
                                end = Offset(size.width * 0.64f, size.height * 0.78f),
                                strokeWidth = 1.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Text(
                        text = if (pieza == null) "NUEVA PIEZA" else "EDITAR PIEZA",
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        color = gold
                    )

                    Spacer(Modifier.size(40.dp))
                }

                Spacer(Modifier.height(6.dp))

                // 2. ESCENARIO DEL NICHO CENTRAL (.stage: 208dp x 262dp)
                val archivoFoto = vm.archivo(foto)
                Box(
                    modifier = Modifier.width(208.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(208.dp)
                                .height(262.dp)
                                .shadow(
                                    elevation = if (archivoFoto != null) 22.dp else 14.dp,
                                    shape = RoundedCornerShape(2.dp),
                                    spotColor = gold
                                )
                                .background(Color(0xFF0D0F14), RoundedCornerShape(2.dp))
                                .border(1.dp, Color(0xFF7A6733), RoundedCornerShape(2.dp))
                                .clickable(enabled = !ocupado) { mostrarSheetFoto = true }
                                .padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, gold.copy(alpha = 0.30f))
                            ) {
                                if (archivoFoto != null) {
                                    FotoPieza(
                                        archivo = archivoFoto,
                                        nombre = nombre.ifBlank { "Nueva pieza" },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Acabado de cristal (.glass)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    0.0f to Color.Transparent,
                                                    0.38f to Color.Transparent,
                                                    0.44f to Color.White.copy(alpha = 0.08f),
                                                    0.52f to Color.Transparent,
                                                    1.0f to Color(0x5505060A)
                                                )
                                            )
                                    )
                                } else {
                                    // Estado vacío con borde punteado y luz radial (.empty)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(gold.copy(alpha = 0.12f), Color.Transparent),
                                                    center = Offset(240f, 160f),
                                                    radius = 320f
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(Modifier.fillMaxSize()) {
                                            drawRect(
                                                color = Color(0xFF6B5A2C),
                                                style = Stroke(
                                                    width = 1.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                                )
                                            )
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Canvas(Modifier.size(30.dp)) {
                                                val cx = size.width / 2f
                                                val cy = size.height / 2f
                                                val arm = size.minDimension * 0.28f
                                                drawLine(gold, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
                                                drawLine(gold, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
                                            }
                                            Text(
                                                text = "Añade la foto",
                                                fontSize = 12.sp,
                                                color = muted,
                                                fontWeight = FontWeight.Light
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Línea dorada (.pond) y reflejo en el piso (.refl)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, gold, Color.Transparent)
                                    )
                                )
                        )
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                        ) {
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(gold.copy(alpha = 0.22f), Color.Transparent),
                                    center = Offset(size.width / 2f, 0f),
                                    radius = size.width * 0.45f
                                )
                            )
                        }
                    }

                    // Botón flotante para cambiar foto (.chg) cuando ya hay imagen
                    if (archivoFoto != null) {
                        Surface(
                            onClick = { mostrarSheetFoto = true },
                            shape = CircleShape,
                            color = Color(0xFF0A0A0A),
                            border = BorderStroke(1.dp, Color(0xFF5A4A22)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = (-14).dp)
                                .size(34.dp)
                                .semantics { contentDescription = "Cambiar foto" }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                IconoCamaraMuseo(color = gold, size = 16.dp)
                            }
                        }
                    }
                }

                errorCamara?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                if (ocupado || buscandoApi) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = gold)
                }

                Spacer(Modifier.height(4.dp))

                // 3. CARTELA DE NOMBRE (.cart): Input centrado Serif de 30sp + metadato "SALA XX · N.º YY"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .semantics { contentDescription = "Nombre *" },
                        contentAlignment = Alignment.Center
                    ) {
                        if (nombre.isEmpty()) {
                            Text(
                                text = "Nombre de la pieza",
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF5C574A),
                                textAlign = TextAlign.Center
                            )
                        }
                        BasicTextField(
                            value = nombre,
                            onValueChange = { if (it.length <= 45) nombre = it },
                            singleLine = true,
                            enabled = !ocupado,
                            cursorBrush = SolidColor(gold),
                            textStyle = TextStyle(
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF2EAD8),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )
                    }
                    HorizontalDivider(
                        color = if (validar && errores.nombre != null) MaterialTheme.colorScheme.error else lineColor,
                        thickness = 1.dp
                    )
                    if (validar && errores.nombre != null) {
                        Text(
                            text = errores.nombre!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Metadato dinámico: "SALA 01 · N.º 14"
                    Spacer(Modifier.height(8.dp))
                    if (franquicia.isNotBlank()) {
                        val prefijoLugar = if (enExhibicion) "SALA" else "RACK"
                        val numPieza = if (pieza != null) pieza.id.toInt() else (totalPiezasMuseo + 1)
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = gold)) {
                                    append(String.format(Locale.US, "%s %02d", prefijoLugar, numeroSalaActual))
                                }
                                append(String.format(Locale.US, " · N.º %02d", numPieza))
                            },
                            fontSize = 9.5.sp,
                            letterSpacing = 2.4.sp,
                            color = muted
                        )
                    } else {
                        Text(
                            text = "ELIGE UNA SALA ABAJO",
                            fontSize = 9.sp,
                            letterSpacing = 2.2.sp,
                            color = muted.copy(alpha = 0.7f)
                        )
                    }

                    // Autocompletado desde catálogo online (Jikan REST API v4)
                    Text(
                        text = if (buscandoApi) "✦ Consultando catálogo online…" else "✦ Autocompletar desde catálogo online",
                        fontSize = 11.5.sp,
                        color = gold.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable(enabled = !ocupado && !buscandoApi) {
                                if (nombre.isBlank()) {
                                    nombre = "Guts Berserker Armor"
                                    if (franquicia.isBlank()) franquicia = "Berserk"
                                    if (linea.isBlank()) linea = "Threezero"
                                } else {
                                    vm.buscarEnApiCatalogo(nombre)
                                }
                            }
                            .padding(vertical = 2.dp)
                    )
                }

                // Sugerencias de la API externa si existen
                if (sugerenciasApi.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        border = BorderStroke(1.dp, Color(0xFF5A4A22)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF12141A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Catálogo online", style = MaterialTheme.typography.labelLarge, color = gold)
                                Text(
                                    "Cerrar",
                                    fontSize = 12.sp,
                                    color = muted,
                                    modifier = Modifier.clickable { vm.limpiarSugerenciasRemotas() }
                                )
                            }
                            sugerenciasApi.forEach { item ->
                                Surface(
                                    onClick = {
                                        nombre = item.nombre
                                        franquicia = item.franquicia
                                        if (linea.isBlank()) linea = item.lineaSugerida
                                        if (nota.isBlank()) nota = item.notaSugerida
                                        vm.limpiarSugerenciasRemotas()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF181B22),
                                    border = BorderStroke(1.dp, lineColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(item.nombre, fontSize = 14.sp, fontFamily = FontFamily.Serif, color = gold)
                                        Text("${item.franquicia} · ${item.lineaSugerida}", fontSize = 11.sp, color = muted)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. CARRUSEL HORIZONTAL DE SALAS / FRANQUICIAS (.chips) + "+ Nueva sala"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listaSalas.forEach { sala ->
                        val activa = franquicia.equals(sala, ignoreCase = true)
                        Surface(
                            onClick = { franquicia = sala },
                            enabled = !ocupado,
                            shape = RoundedCornerShape(999.dp),
                            color = if (activa) gold.copy(alpha = 0.10f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (activa) gold else lineColor),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 15.dp)
                            ) {
                                Text(
                                    text = sala,
                                    fontSize = 13.sp,
                                    color = if (activa) gold else muted,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    }

                    // Chip "+ Nueva sala" con borde punteado (.chip.nw)
                    if (!creandoNuevaSala) {
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(enabled = !ocupado) {
                                    textoNuevaSala = ""
                                    creandoNuevaSala = true
                                }
                                .padding(horizontal = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.matchParentSize()) {
                                drawRoundRect(
                                    color = Color(0xFF6B5A2C),
                                    cornerRadius = CornerRadius(999f, 999f),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                                    )
                                )
                            }
                            Text(
                                text = "+ Nueva sala",
                                fontSize = 13.sp,
                                color = gold,
                                fontWeight = FontWeight.Light
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF14161D),
                            border = BorderStroke(1.dp, gold),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                BasicTextField(
                                    value = textoNuevaSala,
                                    onValueChange = { if (it.length <= 24) textoNuevaSala = it },
                                    singleLine = true,
                                    cursorBrush = SolidColor(gold),
                                    textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFFF2EAD8)),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val limpia = textoNuevaSala.trim()
                                            if (limpia.isNotBlank()) {
                                                salasExtra = salasExtra + limpia
                                                franquicia = limpia
                                            }
                                            creandoNuevaSala = false
                                        }
                                    ),
                                    modifier = Modifier.width(105.dp),
                                    decorationBox = { inner ->
                                        if (textoNuevaSala.isEmpty()) {
                                            Text("Franquicia", fontSize = 13.sp, color = muted)
                                        }
                                        inner()
                                    }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "OK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = gold,
                                    modifier = Modifier.clickable {
                                        val limpia = textoNuevaSala.trim()
                                        if (limpia.isNotBlank()) {
                                            salasExtra = salasExtra + limpia
                                            franquicia = limpia
                                        }
                                        creandoNuevaSala = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (validar && errores.franquicia != null) {
                    Text(
                        text = errores.franquicia!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 5. TRES FILAS DE CONFIGURACIÓN (.rows): Adquisición | Va en exhibición | Su historia
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Fila 1: Adquisición
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable(enabled = !ocupado) { calendario = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Adquisición",
                            fontSize = 15.sp,
                            color = Color(0xFFF2EAD8)
                        )
                        Text(
                            text = "${Fechas.formatearCorto(fecha)} ›",
                            fontSize = 13.sp,
                            color = gold
                        )
                    }
                    HorizontalDivider(color = lineColor, thickness = 1.dp)

                    // Fila 2: Va en exhibición
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable(enabled = !ocupado) { enExhibicion = !enExhibicion },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Va en exhibición",
                            fontSize = 15.sp,
                            color = Color(0xFFF2EAD8)
                        )
                        ToggleMuseo(
                            activo = enExhibicion,
                            onClick = { if (!ocupado) enExhibicion = !enExhibicion }
                        )
                    }
                    HorizontalDivider(color = lineColor, thickness = 1.dp)

                    // Fila 3: Su historia (abre #shC)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable(enabled = !ocupado) { mostrarSheetHistoria = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconoDocumentoMuseo(color = gold)
                            Text(
                                text = "Su historia",
                                fontSize = 15.sp,
                                color = Color(0xFFF2EAD8)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (tieneHistoriaOpcional) "Completada" else "Opcional",
                                fontSize = 13.sp,
                                color = if (tieneHistoriaOpcional) gold else muted,
                                fontWeight = FontWeight.Light
                            )
                            Text("›", fontSize = 15.sp, color = gold)
                            if (tieneHistoriaOpcional) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(gold, CircleShape)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = lineColor, thickness = 1.dp)
                }

                Spacer(Modifier.weight(1f, fill = true))
                Spacer(Modifier.height(18.dp))

                // 6. BOTÓN PRINCIPAL (.cta) Y PIE DE PÁGINA (.foot)
                val habilitado = !ocupado && nombre.isNotBlank() && franquicia.isNotBlank()
                val textoBoton = when {
                    pieza != null -> "Guardar cambios"
                    enExhibicion -> "Colocar en el museo"
                    else -> "Guardar en la bodega"
                }

                Button(
                    onClick = ejecutarGuardado,
                    enabled = habilitado,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = Color(0xFF1A1405),
                        disabledContainerColor = Color(0xFF1B1912),
                        disabledContentColor = Color(0xFF5C574A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = textoBoton,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(Modifier.height(12.dp))
                val etqPiezas = if (totalPiezasMuseo == 1) "1 PIEZA" else "$totalPiezasMuseo PIEZAS"
                val etqSalas = if (totalSalasMuseo == 1) "1 SALA" else "$totalSalasMuseo SALAS"
                Text(
                    text = "$etqPiezas · $etqSalas",
                    fontSize = 8.5.sp,
                    letterSpacing = 2.2.sp,
                    color = muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // ==================== HOJA MODAL 1 (#shP): FOTO DE LA PIEZA ====================
    if (mostrarSheetFoto) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSheetFoto = false },
            containerColor = Color(0xFF12141A),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 12.dp)
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
                Text(
                    text = "Foto de la pieza",
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF2EAD8)
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón Galería
                    Surface(
                        onClick = {
                            selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = gold.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color(0xFF5A4A22)),
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            IconoGaleriaMuseo(color = gold)
                            Spacer(Modifier.height(8.dp))
                            Text("Galería", fontSize = 14.sp, color = gold)
                        }
                    }

                    // Botón Cámara
                    Surface(
                        onClick = lanzarCamara,
                        shape = RoundedCornerShape(18.dp),
                        color = gold.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color(0xFF5A4A22)),
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            IconoCamaraMuseo(color = gold, size = 26.dp)
                            Spacer(Modifier.height(8.dp))
                            Text("Cámara", fontSize = 14.sp, color = gold)
                        }
                    }
                }

                if (foto != null) {
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            foto = null
                            mostrarSheetFoto = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quitar fotografía actual", color = muted, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Mejor con luz natural y fondo liso",
                    fontSize = 12.sp,
                    color = muted,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // ==================== HOJA MODAL 2 (#shC): SU HISTORIA ====================
    if (mostrarSheetHistoria) {
        val sheetFieldColors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = gold,
            unfocusedIndicatorColor = Color(0xFF26231C),
            focusedLabelColor = gold,
            unfocusedLabelColor = muted
        )
        ModalBottomSheet(
            onDismissRequest = { mostrarSheetHistoria = false },
            containerColor = Color(0xFF12141A),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 12.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF3A3830))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Su historia",
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF2EAD8)
                )

                TextField(
                    value = linea,
                    onValueChange = { linea = it },
                    label = { Text("Fabricante o línea") },
                    singleLine = true,
                    colors = sheetFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                FilaToggleMuseo(
                    etiqueta = "Edición limitada",
                    activo = edicionLimitada,
                    onToggle = { edicionLimitada = !edicionLimitada },
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )

                AnimatedVisibility(visible = edicionLimitada) {
                    TextField(
                        value = numeroEdicion,
                        onValueChange = { numeroEdicion = it },
                        label = { Text("Número de serie o tiraje (ej. 0142/2000)") },
                        singleLine = true,
                        colors = sheetFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TextField(
                    value = procedencia,
                    onValueChange = { procedencia = it },
                    label = { Text("Procedencia") },
                    singleLine = true,
                    colors = sheetFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Ubicación") },
                    singleLine = true,
                    colors = sheetFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = valorPesos,
                    onValueChange = { nuevo -> valorPesos = nuevo.filter { it.isDigit() }.take(9) },
                    label = { Text("Valor estimado (MXN)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = sheetFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("¿Qué significa para ti?") },
                    minLines = 2,
                    colors = sheetFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { mostrarSheetHistoria = false },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = Color(0xFF1A1405)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Guardar", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Los cambios de esta pieza no se han guardado.") },
            confirmButton = { TextButton(onClick = volver) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Seguir editando") } }
        )
    }

    if (calendario) {
        val dia = Instant.ofEpochMilli(fecha).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { calendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        fecha = Fechas.desdeFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    calendario = false
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { calendario = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}

// =====================================================================
// PANTALLA DE CELEBRACIÓN (#s2): "Ya está en tu museo" / "Ya está a resguardo"
// =====================================================================

@Composable
private fun PantallaCelebracionPieza(
    nombrePieza: String,
    franquicia: String,
    numeroSala: Int,
    numeroGlobal: Int,
    enExhibicion: Boolean,
    fotoNueva: File?,
    piezasExistentesSala: List<PiezaEntity>,
    vm: ColeccionViewModel,
    gold: Color,
    onAnadirOtra: () -> Unit,
    onVerEnMuseo: () -> Unit
) {
    val escalaNueva = remember { Animatable(1.45f) }
    LaunchedEffect(Unit) {
        escalaNueva.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Etiqueta superior: MUSEO o BODEGA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (enExhibicion) "MUSEO" else "BODEGA",
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = gold
            )
        }

        Spacer(Modifier.height(10.dp))

        // Nombre de la Sala / Rack (.rn)
        Text(
            text = String.format(
                Locale.US,
                "%s %02d",
                if (enExhibicion) "SALA" else "RACK",
                numeroSala
            ),
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = gold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = franquicia,
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFF2EAD8)
        )

        Spacer(Modifier.height(20.dp))

        // Pared 2x2 de la Sala mostrando la nueva pieza enmarcada con resplandor dorado (.wall)
        val totalSlots = 4
        val indiceNuevo = piezasExistentesSala.size.coerceAtMost(3)
        Column(
            modifier = Modifier.width(290.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for (fila in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (col in 0..1) {
                        val slotIdx = fila * 2 + col
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.75f)
                        ) {
                            when {
                                slotIdx < indiceNuevo -> {
                                    val existente = piezasExistentesSala[slotIdx]
                                    MiniMarcoCelebracion(
                                        nombre = existente.nombre,
                                        archivoFoto = vm.archivo(existente.fotoPath),
                                        esBiselado = slotIdx % 2 == 1,
                                        esNueva = false,
                                        gold = gold
                                    )
                                }
                                slotIdx == indiceNuevo -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = escalaNueva.value
                                                scaleY = escalaNueva.value
                                            }
                                    ) {
                                        MiniMarcoCelebracion(
                                            nombre = nombrePieza,
                                            archivoFoto = fotoNueva,
                                            esBiselado = slotIdx % 2 == 1,
                                            esNueva = true,
                                            gold = gold
                                        )
                                    }
                                }
                                else -> {
                                    // Hueco punteado disponible (.f.dash)
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(Modifier.fillMaxSize()) {
                                            drawRect(
                                                color = Color(0xFF6B5A2C),
                                                style = Stroke(
                                                    width = 1.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                                )
                                            )
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val arm = 10.dp.toPx()
                                            drawLine(gold, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
                                            drawLine(gold, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // Título de confirmación (#okT y #okN)
        Text(
            text = if (enExhibicion) "Ya está en tu museo" else "Ya está a resguardo",
            fontSize = 32.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFF2EAD8),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = String.format(
                Locale.forLanguageTag("es-MX"),
                "%s · N.º %02d",
                nombrePieza.uppercase(Locale.forLanguageTag("es-MX")),
                numeroGlobal
            ),
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = gold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        // Botones de acción finales (.acts)
        Button(
            onClick = onAnadirOtra,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = gold,
                contentColor = Color(0xFF1A1405)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Añadir otra pieza", fontSize = 15.sp)
        }
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = onVerEnMuseo,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Text(
                text = "Ver en mi museo",
                fontSize = 13.sp,
                color = Color(0xFF8D8777)
            )
        }
    }
}

@Composable
private fun MiniMarcoCelebracion(
    nombre: String,
    archivoFoto: File?,
    esBiselado: Boolean,
    esNueva: Boolean,
    gold: Color
) {
    val paleta = rememberPaletaPieza(nombre)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = if (esNueva) 20.dp else 8.dp,
                spotColor = if (esNueva) gold else Color.Black
            )
            .background(if (esBiselado) Color(0xFF1A1E27) else Color(0xFF0D0F14))
            .border(
                width = if (esBiselado) 4.dp else 1.dp,
                color = if (esNueva) gold else if (esBiselado) Color(0xFF2C323D) else Color(0xFF7A6733)
            )
            .padding(if (esBiselado) 6.dp else 8.dp)
    ) {
        if (archivoFoto != null) {
            FotoPieza(archivoFoto, nombre, Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(paleta.first, paleta.second, Color(0xFF0B0A07))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
                    fontSize = 40.sp,
                    fontFamily = FontFamily.Serif,
                    color = gold.copy(alpha = 0.42f)
                )
            }
        }
    }
}

// =====================================================================
// CONTROLES COMPARTIDOS DE REGISTRO Y PERFIL
// =====================================================================

@Composable
internal fun FilaToggleMuseo(
    etiqueta: String,
    activo: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        ToggleMuseo(activo = activo, onClick = onToggle)
    }
}

@Composable
internal fun ToggleMuseo(
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (activo) Color(0xFF5A4A22) else Color(0xFF2A2820))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = if (activo) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    if (activo) Color(0xFFE8C97A) else Color(0xFF8D8777),
                    CircleShape
                )
        )
    }
}

@Composable
private fun IconoDocumentoMuseo(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.12f)
            lineTo(w * 0.60f, h * 0.12f)
            lineTo(w * 0.78f, h * 0.30f)
            lineTo(w * 0.78f, h * 0.88f)
            lineTo(w * 0.22f, h * 0.88f)
            close()
        }
        drawPath(path, color, style = s)
        drawLine(color, Offset(w * 0.36f, h * 0.54f), Offset(w * 0.64f, h * 0.54f), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.36f, h * 0.70f), Offset(w * 0.56f, h * 0.70f), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun IconoGaleriaMuseo(color: Color) {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.16f),
            size = Size(w * 0.76f, h * 0.68f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = s
        )
        drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.36f, h * 0.40f), style = s)
        val mount = Path().apply {
            moveTo(w * 0.24f, h * 0.74f)
            lineTo(w * 0.54f, h * 0.46f)
            lineTo(w * 0.84f, h * 0.70f)
        }
        drawPath(mount, color, style = s)
    }
}

@Composable
private fun IconoCamaraMuseo(color: Color, size: androidx.compose.ui.unit.Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val body = Path().apply {
            moveTo(w * 0.16f, h * 0.34f)
            lineTo(w * 0.30f, h * 0.34f)
            lineTo(w * 0.38f, h * 0.20f)
            lineTo(w * 0.62f, h * 0.20f)
            lineTo(w * 0.70f, h * 0.34f)
            lineTo(w * 0.84f, h * 0.34f)
            lineTo(w * 0.84f, h * 0.80f)
            lineTo(w * 0.16f, h * 0.80f)
            close()
        }
        drawPath(body, color, style = s)
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.50f, h * 0.56f), style = s)
    }
}
