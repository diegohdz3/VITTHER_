package com.diego.vitther.ui.screens.coleccion

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.*
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.FotoPieza
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val FranquiciasSugeridas = listOf("Berserk", "Dragon Ball", "One Piece", "Evangelion", "Marvel", "Star Wars")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistroScreen(
    pieza: PiezaEntity?,
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
    var calendario by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
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

    val pasoActual = when {
        linea.isNotBlank() || procedencia.isNotBlank() || ubicacion.isNotBlank() || nota.isNotBlank() || valorPesos.isNotBlank() -> 3
        nombre.isNotBlank() && franquicia.isNotBlank() -> 3
        nombre.isNotBlank() -> 2
        else -> 1
    }

    val salir: () -> Unit = {
        if (!ocupado) {
            vm.limpiarSugerenciasRemotas()
            if (modificado) confirmarSalida = true else volver()
        }
    }
    BackHandler { salir() }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.importar(it) { ruta -> foto = ruta } }
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) uriCamara?.let { uri ->
            vm.importar(Uri.parse(uri)) { ruta ->
                foto = ruta
                archivoCamara?.delete()
                archivoCamara = null
            }
        } else {
            archivoCamara?.delete()
            archivoCamara = null
        }
    }

    val ejecutarGuardado: () -> Unit = {
        validar = true
        if (!errores.hayErrores) {
            val centavos = valorPesos.toLongOrNull()?.times(100L)
            val estadoElegido = if (enExhibicion) EstadoPieza.EXHIBICION else EstadoPieza.BODEGA
            val base = pieza ?: PiezaEntity(
                nombre = nombre,
                franquicia = franquicia,
                fechaAdquisicion = fecha,
                estado = estadoElegido
            )
            vm.guardar(
                base.copy(
                    nombre = nombre,
                    franquicia = franquicia,
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
                ),
                guardado
            )
        }
    }

    val gold = MaterialTheme.colorScheme.primary
    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        focusedIndicatorColor = gold,
        unfocusedIndicatorColor = Color(0xFF26231C),
        focusedLabelColor = gold,
        unfocusedLabelColor = Color(0xFF8D8777)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = salir,
                    enabled = !ocupado,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = "Volver" }
                ) {
                    Canvas(Modifier.size(20.dp)) {
                        drawLine(
                            color = Color(0xFFF2EAD8),
                            start = Offset(size.width * 0.65f, size.height * 0.2f),
                            end = Offset(size.width * 0.30f, size.height * 0.5f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFF2EAD8),
                            start = Offset(size.width * 0.30f, size.height * 0.5f),
                            end = Offset(size.width * 0.65f, size.height * 0.8f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(
                                    if (step <= pasoActual) gold else Color(0xFF26231C),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
                Text(
                    text = "$pasoActual/3",
                    fontSize = 12.sp,
                    color = Color(0xFF8D8777),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // SECCIÓN 1: ¿Qué pieza llegó?
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (pieza == null) "UNA NUEVA HISTORIA" else "EDITAR CÉDULA",
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    color = gold
                )
                Text(
                    text = if (pieza == null) "¿Qué pieza llegó?" else "Actualiza tu pieza",
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Empieza con una foto y el nombre. Lo demás puede esperar.",
                    fontSize = 14.sp,
                    color = Color(0xFF8D8777)
                )
            }

            // Recuadro de Fotografía con borde punteado
            val archivoFoto = vm.archivo(foto)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (archivoFoto != null) 320.dp else 190.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (archivoFoto != null) {
                            Brush.radialGradient(listOf(Color(0xFF3A3222), Color(0xFF15120C)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF141414), Color(0xFF141414)))
                        }
                    )
                    .clickable(enabled = !ocupado) {
                        selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (archivoFoto != null) {
                    FotoPieza(archivoFoto, nombre.ifBlank { "tu pieza" }, Modifier.fillMaxSize())
                } else {
                    Canvas(Modifier.matchParentSize()) {
                        drawRoundRect(
                            color = Color(0xFF3A3424),
                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Canvas(Modifier.size(30.dp)) {
                            val stroke = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            drawRoundRect(
                                color = gold,
                                topLeft = Offset(size.width * 0.14f, size.height * 0.28f),
                                size = androidx.compose.ui.geometry.Size(size.width * 0.72f, size.height * 0.52f),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                style = stroke
                            )
                            drawCircle(
                                color = gold,
                                radius = size.minDimension * 0.14f,
                                center = Offset(size.width * 0.5f, size.height * 0.54f),
                                style = stroke
                            )
                        }
                        Text(
                            text = "Añade una fotografía",
                            fontSize = 13.sp,
                            color = Color(0xFF8D8777)
                        )
                    }
                }
            }

            // Botones píldora: Galería / Cámara / Quitar
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = {
                        selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !ocupado,
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xFF26231C))
                ) {
                    Text(
                        text = "Galería",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
                Surface(
                    onClick = {
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
                    },
                    enabled = !ocupado,
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xFF26231C))
                ) {
                    Text(
                        text = "Cámara",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
                if (foto != null) {
                    Surface(
                        onClick = { foto = null },
                        enabled = !ocupado,
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color(0xFF26231C))
                    ) {
                        Text(
                            text = "Quitar",
                            fontSize = 13.sp,
                            color = Color(0xFF8D8777),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            errorCamara?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            if (ocupado || buscandoApi) LinearProgressIndicator(Modifier.fillMaxWidth())

            // Campo Nombre *
            TextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre *") },
                singleLine = true,
                enabled = !ocupado,
                isError = validar && errores.nombre != null,
                supportingText = { if (validar) errores.nombre?.let { Text(it) } },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            // Botón sutil de autocompletado desde catálogo online (API REST)
            Text(
                text = if (buscandoApi) "✦ Consultando catálogo online…" else "✦ Autocompletar desde catálogo online",
                fontSize = 13.sp,
                color = gold,
                modifier = Modifier
                    .clickable(enabled = !ocupado && !buscandoApi) {
                        if (nombre.isBlank()) {
                            nombre = "Guts Berserker Armor"
                            if (franquicia.isBlank()) franquicia = "Berserk"
                            if (linea.isBlank()) linea = "S.H. Figuarts"
                        } else {
                            vm.buscarEnApiCatalogo(nombre)
                        }
                    }
                    .padding(vertical = 2.dp)
            )

            if (sugerenciasApi.isNotEmpty()) {
                Card(
                    border = BorderStroke(1.dp, Color(0xFF5A4A22)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sugerencias del catálogo online", style = MaterialTheme.typography.titleSmall, color = gold)
                            TextButton(onClick = { vm.limpiarSugerenciasRemotas() }) { Text("Cerrar") }
                        }
                        sugerenciasApi.forEach { item ->
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        nombre = item.nombre
                                        if (franquicia.isBlank()) franquicia = item.franquicia
                                        if (linea.isBlank()) linea = item.lineaSugerida
                                        if (nota.isBlank()) nota = item.notaSugerida
                                        vm.limpiarSugerenciasRemotas()
                                    }
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.nombre, style = MaterialTheme.typography.titleMedium, color = gold)
                                    Text("${item.franquicia} · ${item.lineaSugerida}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        item.notaSugerida,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // SECCIÓN 2: Lo esencial (Ubícala en tu museo)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "LO ESENCIAL",
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    color = gold
                )
                Text(
                    text = "Ubícala en tu museo",
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Dos datos y ya cuenta como parte de tu colección.",
                    fontSize = 14.sp,
                    color = Color(0xFF8D8777)
                )
            }

            TextField(
                value = franquicia,
                onValueChange = { franquicia = it },
                label = { Text("Franquicia *") },
                singleLine = true,
                enabled = !ocupado,
                isError = validar && errores.franquicia != null,
                supportingText = { if (validar) errores.franquicia?.let { Text(it) } },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            // Chips rápidos de franquicia
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FranquiciasSugeridas.forEach { sugerida ->
                    val activa = franquicia.equals(sugerida, ignoreCase = true)
                    Surface(
                        onClick = { franquicia = sugerida },
                        enabled = !ocupado,
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, if (activa) Color(0xFF5A4A22) else Color(0xFF26231C))
                    ) {
                        Text(
                            text = sugerida,
                            fontSize = 12.sp,
                            color = if (activa) gold else Color(0xFF8D8777),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Fila de Fecha de adquisición
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !ocupado) { calendario = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Adquisición", fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text("${Fechas.formatearCorto(fecha)} ›", fontSize = 13.sp, color = gold)
                }
                HorizontalDivider(color = Color(0xFF26231C))
            }
            if (validar) errores.fechaAdquisicion?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

            // Switch: Va en exhibición
            FilaToggleMuseo(
                etiqueta = "Va en exhibición",
                activo = enExhibicion,
                onToggle = { if (!ocupado) enExhibicion = !enExhibicion }
            )

            Spacer(Modifier.height(6.dp))

            // SECCIÓN 3: Su cédula · opcional
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SU CÉDULA · OPCIONAL",
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    color = gold
                )
                Text(
                    text = "Cuenta su historia",
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Estos detalles dan vida a la pieza. Puedes completarlos después.",
                    fontSize = 14.sp,
                    color = Color(0xFF8D8777)
                )
            }

            TextField(
                value = linea,
                onValueChange = { linea = it },
                label = { Text("Línea o fabricante") },
                singleLine = true,
                enabled = !ocupado,
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            FilaToggleMuseo(
                etiqueta = "Edición limitada / numerada",
                activo = edicionLimitada,
                onToggle = { if (!ocupado) edicionLimitada = !edicionLimitada }
            )

            if (edicionLimitada) {
                TextField(
                    value = numeroEdicion,
                    onValueChange = { numeroEdicion = it },
                    label = { Text("Número de serie o tiraje (ej. 0142/2000)") },
                    singleLine = true,
                    enabled = !ocupado,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TextField(
                value = procedencia,
                onValueChange = { procedencia = it },
                label = { Text("Procedencia") },
                singleLine = true,
                enabled = !ocupado,
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = ubicacion,
                onValueChange = { ubicacion = it },
                label = { Text("Ubicación física") },
                singleLine = true,
                enabled = !ocupado,
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = valorPesos,
                onValueChange = { nuevo -> valorPesos = nuevo.filter { it.isDigit() }.take(9) },
                label = { Text("Valor estimado (MXN)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !ocupado,
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = nota,
                onValueChange = { nota = it },
                label = { Text("¿Qué significa para ti?") },
                enabled = !ocupado,
                minLines = 3,
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Botón principal dorado tipo píldora
            Button(
                enabled = !ocupado,
                onClick = ejecutarGuardado,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = gold,
                    contentColor = Color(0xFF1A1405)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (pieza == null) "Incorporar al museo" else "Guardar cambios",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (pieza == null) {
                TextButton(
                    enabled = !ocupado,
                    onClick = ejecutarGuardado,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Omitir y guardar",
                        fontSize = 13.sp,
                        color = Color(0xFF8D8777)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Los cambios de esta cédula no se han guardado.") },
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
            .width(46.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (activo) Color(0xFF5A4A22) else Color(0xFF26231C))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp),
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
