package com.diego.vitther.ui.screens.coleccion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.*
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColeccionScreen(
    piezas: List<PiezaEntity>,
    vm: ColeccionViewModel,
    agregar: () -> Unit,
    abrir: (Long) -> Unit,
    categoria: String? = null,
    volver: (() -> Unit)? = null,
    agregarEnSala: (String) -> Unit = { agregar() }
) {
    var bodega by rememberSaveable(categoria) {
        val deCategoria = piezas.filter { categoria == null || it.franquicia == categoria }
        mutableStateOf(
            categoria != null && deCategoria.isNotEmpty() &&
                deCategoria.all { it.estado == EstadoPieza.BODEGA }
        )
    }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var mostrarBusqueda by rememberSaveable { mutableStateOf(false) }
    var franquiciaSeleccionada by rememberSaveable(categoria) { mutableStateOf(categoria) }
    var mostrarSheetVista by rememberSaveable { mutableStateOf(false) }

    // Configuración de vista de marcos en el Museo ("var", "v", "h" y columnas 1, 2, 3)
    var modoProporcion by rememberSaveable { mutableStateOf("var") }
    var columnasMarcos by rememberSaveable { mutableIntStateOf(2) }

    // Paleta atmosférica dinámica: Museo (azul galería nocturno) vs Bodega (cava subterránea cálida)
    val pageBg = if (bodega) Color(0xFF110A05) else Color(0xFF0A0D17)
    val pageTop = if (bodega) Color(0xFF27150A) else Color(0xFF0B1224)
    val gold = if (bodega) Color(0xFFE8B472) else Color(0xFFE8C97A)
    val lineBorder = if (bodega) Color(0xFF33231A) else Color(0xFF26231C)
    val muted = Color(0xFF8D8777)

    // Índices globales de catálogo para mostrar "N.º 01", "N.º 02", etc.
    val indiceGlobalPorId = remember(piezas) {
        piezas.sortedBy { it.id }.mapIndexed { idx, p -> p.id to (idx + 1) }.toMap()
    }

    val visibles = remember(piezas, bodega, busqueda) {
        piezas.filter {
            (it.estado == EstadoPieza.BODEGA) == bodega &&
                (it.nombre.contains(busqueda, true) || it.franquicia.contains(busqueda, true))
        }
    }

    val salasPorFranquicia = remember(visibles) {
        visibles.groupBy { it.franquicia }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    Scaffold(
        containerColor = pageBg,
        floatingActionButton = {
            Surface(
                onClick = {
                    val salaActual = franquiciaSeleccionada
                    if (salaActual != null) agregarEnSala(salaActual) else agregar()
                },
                modifier = Modifier
                    .size(54.dp)
                    .semantics { contentDescription = "Agregar pieza" },
                shape = CircleShape,
                color = gold,
                contentColor = Color(0xFF1A1405),
                shadowElevation = 10.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF1A1405)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        if (franquiciaSeleccionada != null) {
            val listaSalas = salasPorFranquicia.keys.toList()
            val numeroSala = (listaSalas.indexOfFirst { it.equals(franquiciaSeleccionada, ignoreCase = true) } + 1)
                .coerceAtLeast(1)
            val piezasDeSala = piezas.filter {
                it.franquicia.equals(franquiciaSeleccionada, ignoreCase = true) &&
                    (it.estado == EstadoPieza.BODEGA) == bodega
            }.ifEmpty {
                piezas.filter { it.franquicia.equals(franquiciaSeleccionada, ignoreCase = true) }
            }

            VistaSalaCompleta(
                numeroSala = numeroSala,
                franquicia = franquiciaSeleccionada!!,
                piezas = piezasDeSala,
                indiceGlobalPorId = indiceGlobalPorId,
                vm = vm,
                modoProporcion = modoProporcion,
                columnas = columnasMarcos,
                abrirPieza = abrir,
                agregarEnSala = { agregarEnSala(franquiciaSeleccionada!!) },
                abrirOpcionesVista = { mostrarSheetVista = true },
                volver = {
                    if (categoria != null && volver != null) {
                        volver()
                    } else {
                        franquiciaSeleccionada = null
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(pageBg)
                    .padding(padding)
            ) {
                // 1. CABECERA (.head): "Colección" + Botón "Cómo ver tus figuras" + Botón "Buscar"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(pageTop, pageBg)))
                        .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Colección",
                        fontSize = 34.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF2EAD8)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!bodega) {
                            IconButton(
                                onClick = { mostrarSheetVista = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xB312141A), CircleShape)
                                    .border(1.dp, lineBorder, CircleShape)
                                    .semantics { contentDescription = "Cómo ver tus figuras" }
                            ) {
                                IconoMosaicoGaleria(color = Color(0xFFF2EAD8))
                            }
                        }
                        val busquedaActiva = mostrarBusqueda || busqueda.isNotBlank()
                        IconButton(
                            onClick = {
                                mostrarBusqueda = !mostrarBusqueda
                                if (!mostrarBusqueda) busqueda = ""
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xB312141A), CircleShape)
                                .border(
                                    1.dp,
                                    if (busquedaActiva) Color(0xFF5A4A22) else lineBorder,
                                    CircleShape
                                )
                                .semantics { contentDescription = "Buscar pieza o franquicia" }
                        ) {
                            val tint = if (busquedaActiva) gold else Color(0xFFF2EAD8)
                            Canvas(Modifier.size(18.dp)) {
                                val r = size.minDimension * 0.34f
                                val c = Offset(size.width * 0.44f, size.height * 0.44f)
                                drawCircle(tint, radius = r, center = c, style = Stroke(1.8.dp.toPx()))
                                drawLine(
                                    tint,
                                    Offset(size.width * 0.68f, size.height * 0.68f),
                                    Offset(size.width * 0.90f, size.height * 0.90f),
                                    strokeWidth = 1.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }

                // 2. PESTAÑAS SEGMENTADAS (.seg): Museo | Bodega
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(pageBg)
                        .padding(horizontal = 22.dp)
                        .padding(top = 8.dp)
                ) {
                    TabModoColeccion(
                        texto = "Museo",
                        activo = !bodega,
                        gold = gold,
                        lineColor = lineBorder,
                        onClick = { bodega = false },
                        modifier = Modifier.weight(1f)
                    )
                    TabModoColeccion(
                        texto = "Bodega",
                        activo = bodega,
                        gold = gold,
                        lineColor = lineBorder,
                        onClick = { bodega = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. BARRA DE BÚSQUEDA DESPLEGABLE (.search)
                AnimatedVisibility(visible = mostrarBusqueda || busqueda.isNotBlank()) {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        singleLine = true,
                        placeholder = { Text("Buscar pieza o franquicia", color = muted) },
                        shape = RoundedCornerShape(999.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF12141A),
                            unfocusedContainerColor = Color(0xFF12141A),
                            focusedBorderColor = Color(0xFF5A4A22),
                            unfocusedBorderColor = lineBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 10.dp)
                    )
                }

                // 4. CONTENIDO: SALAS DE MUSEO O CAVA DE VINOS (BODEGA)
                if (visibles.isEmpty()) {
                    val titulo = when {
                        busqueda.isNotBlank() -> "Ninguna pieza coincide"
                        bodega -> "Tu bodega está vacía"
                        else -> "Tu museo espera su primera pieza"
                    }
                    val descripcion = when {
                        busqueda.isNotBlank() -> "Prueba otro nombre o franquicia."
                        bodega -> "Guarda piezas en resguardo dentro de tu cava personal."
                        else -> "Registra una pieza y colócala en su propia sala de exhibición."
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EstadoVacio(titulo, descripcion, if (busqueda.isBlank()) "Agregar pieza" else null, agregar)
                    }
                } else if (!bodega) {
                    // ==================== MODO MUSEO (SALAS DIGITALES) ====================
                    val entradasSalas = salasPorFranquicia.entries.toList()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        itemsIndexed(entradasSalas, key = { _, item -> "sala_${item.key}" }) { idx, (franquicia, grupo) ->
                            SalaMuseoSection(
                                numeroSala = idx + 1,
                                franquicia = franquicia,
                                piezas = grupo,
                                indiceGlobalPorId = indiceGlobalPorId,
                                vm = vm,
                                modoProporcion = modoProporcion,
                                columnas = columnasMarcos,
                                hayBusqueda = busqueda.isNotBlank(),
                                onAbrirSala = { franquiciaSeleccionada = franquicia },
                                onAbrirPieza = abrir,
                                onAgregarEnSala = { agregarEnSala(franquicia) }
                            )
                        }
                        item {
                            val numSalas = entradasSalas.size
                            val etqSalas = if (numSalas == 1) "1 SALA" else "$numSalas SALAS"
                            val etqPiezas = if (visibles.size == 1) "1 PIEZA" else "${visibles.size} PIEZAS"
                            Text(
                                text = "$etqSalas · $etqPiezas",
                                fontSize = 10.sp,
                                letterSpacing = 2.2.sp,
                                color = muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp)
                            )
                        }
                    }
                } else {
                    // ==================== MODO BODEGA (CAVA DE VINOS) ====================
                    val entradasRacks = salasPorFranquicia.entries.toList()
                    val ultimaEnEntrar = remember(visibles) {
                        visibles.maxWithOrNull(compareBy<PiezaEntity> { it.fechaAdquisicion }.thenBy { it.id })
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        if (busqueda.isBlank() && ultimaEnEntrar != null) {
                            item(key = "hero_cava") {
                                HeroCavaBodega(
                                    pieza = ultimaEnEntrar,
                                    vm = vm,
                                    onClick = { abrir(ultimaEnEntrar.id) }
                                )
                            }
                        }
                        itemsIndexed(entradasRacks, key = { _, item -> "rack_${item.key}" }) { idx, (franquicia, grupo) ->
                            RackCavaFranquicia(
                                numeroRack = idx + 1,
                                franquicia = franquicia,
                                piezas = grupo,
                                vm = vm,
                                onAbrirPieza = abrir,
                                onGuardarEnRack = { agregarEnSala(franquicia) }
                            )
                        }
                        item {
                            val numRacks = entradasRacks.size
                            val etqRacks = if (numRacks == 1) "1 RACK" else "$numRacks RACKS"
                            val etqPiezas = if (visibles.size == 1) "1 PIEZA GUARDADA" else "${visibles.size} PIEZAS GUARDADAS"
                            Text(
                                text = "$etqRacks · $etqPiezas",
                                fontSize = 10.sp,
                                letterSpacing = 2.2.sp,
                                color = muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // HOJA MODAL: "Cómo ver tus figuras" (Distribución y Tamaño de los marcos)
    if (mostrarSheetVista) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSheetVista = false },
            containerColor = Color(0xFF14161C),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF26231C))
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
                    text = "Cómo ver tus figuras",
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF2EAD8)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "DISTRIBUCIÓN",
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = muted
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "var" to "Variadas",
                        "v" to "Verticales",
                        "h" to "Horizontales"
                    ).forEach { (clave, etiqueta) ->
                        val activo = modoProporcion == clave
                        BotonOpcionVista(
                            etiqueta = etiqueta,
                            activo = activo,
                            onClick = { modoProporcion = clave },
                            modifier = Modifier.weight(1f)
                        ) { tint ->
                            when (clave) {
                                "var" -> IconoMosaicoGaleria(tint)
                                "v" -> IconoMarcosVerticales(tint)
                                else -> IconoMarcosHorizontales(tint)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "TAMAÑO DE LOS MARCOS",
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = muted
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        3 to "Pequeño",
                        2 to "Mediano",
                        1 to "Grande"
                    ).forEach { (cols, etiqueta) ->
                        val activo = columnasMarcos == cols
                        BotonOpcionVista(
                            etiqueta = etiqueta,
                            activo = activo,
                            onClick = { columnasMarcos = cols },
                            modifier = Modifier.weight(1f)
                        ) { tint ->
                            IconoColumnasMarcos(columnas = cols, color = tint)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { mostrarSheetVista = false },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8C97A),
                        contentColor = Color(0xFF1A1405)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Listo", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun TabModoColeccion(
    texto: String,
    activo: Boolean,
    gold: Color,
    lineColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = texto,
            fontSize = 14.sp,
            color = if (activo) gold else Color(0xFF8D8777),
            modifier = Modifier.padding(vertical = 11.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(lineColor)
            )
            if (activo) {
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(2.dp)
                        .background(gold, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

// =====================================================================
// SECCIÓN MODO MUSEO: SALA CON PARED DE CUADROS Y PISO DE PARQUET
// =====================================================================

@Composable
private fun SalaMuseoSection(
    numeroSala: Int,
    franquicia: String,
    piezas: List<PiezaEntity>,
    indiceGlobalPorId: Map<Long, Int>,
    vm: ColeccionViewModel,
    modoProporcion: String,
    columnas: Int,
    hayBusqueda: Boolean,
    onAbrirSala: () -> Unit,
    onAbrirPieza: (Long) -> Unit,
    onAgregarEnSala: () -> Unit
) {
    val gold = Color(0xFFE8C97A)
    val muted = Color(0xFF8D8777)
    val mostradas = remember(piezas) {
        piezas
            .sortedWith(compareByDescending<PiezaEntity> { it.destacada }.thenBy { it.id })
            .take(4)
            .sortedBy { it.id }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.0f to Color(0xFF1A1E27),
                    0.7f to Color(0xFF13161D),
                    1.0f to Color(0xFF13161D)
                )
            )
    ) {
        // Doble iluminación cenital de techo de galería (.room::before)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(gold.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(size.width * 0.22f, 0f),
                    radius = size.width * 0.42f
                )
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(gold.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(size.width * 0.78f, 0f),
                    radius = size.width * 0.42f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            // Cabecera de Sala (.rt): SALA 01 + Franquicia + Conteo + Botón Galería
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = String.format(Locale.US, "SALA %02d", numeroSala),
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        color = gold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = franquicia,
                        fontSize = 28.sp,
                        lineHeight = 30.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF2EAD8)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${piezas.size} ${if (piezas.size == 1) "pieza" else "piezas"}",
                        fontSize = 12.sp,
                        color = muted,
                        fontWeight = FontWeight.Light
                    )
                    Surface(
                        onClick = onAbrirSala,
                        shape = CircleShape,
                        color = Color(0x12E8C97A),
                        border = BorderStroke(1.dp, Color(0xFF5A4A22)),
                        modifier = Modifier
                            .size(36.dp)
                            .semantics { contentDescription = "Ver sala $franquicia" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            IconoMosaicoGaleria(color = gold, size = 17.dp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Pared de cuadros enmarcados (.wall)
            ParedCuadrosMuseo(
                piezas = mostradas,
                indiceGlobalPorId = indiceGlobalPorId,
                vm = vm,
                modoProporcion = modoProporcion,
                columnas = columnas,
                incluirCuadroAgregar = piezas.size < 4 && !hayBusqueda,
                onAbrirPieza = onAbrirPieza,
                onAgregarEnSala = onAgregarEnSala,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
            )

            Spacer(Modifier.height(22.dp))

            // Botón "Ver colección · +N más" cuando hay más de 4 piezas en la sala
            if (piezas.size > 4) {
                Surface(
                    onClick = onAbrirSala,
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0x0FE8C97A),
                    border = BorderStroke(1.dp, Color(0xFF5A4A22)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 24.dp)
                        .height(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        IconoMosaicoGaleria(color = gold, size = 16.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Ver colección · +${piezas.size - 4} más",
                            fontSize = 13.sp,
                            color = gold
                        )
                    }
                }
            }

            // Piso de duela de madera de museo (.floor)
            PisoMaderaMuseo()
        }
    }
}

private sealed interface ElementoParedMuseo {
    data class Cuadro(val pieza: PiezaEntity, val numeroGlobal: Int, val aspectRatio: Float, val spanCompleto: Boolean) : ElementoParedMuseo
    data object CuadroAgregar : ElementoParedMuseo
}

private data class PosicionCuadro(
    val elemento: ElementoParedMuseo,
    val leftDp: Dp,
    val topDp: Dp,
    val widthDp: Dp,
    val frameHeightDp: Dp
)

@Composable
private fun ParedCuadrosMuseo(
    piezas: List<PiezaEntity>,
    indiceGlobalPorId: Map<Long, Int>,
    vm: ColeccionViewModel,
    modoProporcion: String,
    columnas: Int,
    incluirCuadroAgregar: Boolean,
    onAbrirPieza: (Long) -> Unit,
    onAgregarEnSala: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectosVariados = listOf(0.75f, 1.35f, 1.0f, 0.72f, 1.28f, 0.80f)
    val elementos = remember(piezas, indiceGlobalPorId, modoProporcion, incluirCuadroAgregar) {
        buildList {
            piezas.forEachIndexed { i, p ->
                val numGlobal = indiceGlobalPorId[p.id] ?: (i + 1)
                val arBase = aspectosVariados[(numGlobal - 1).coerceAtLeast(0) % aspectosVariados.size]
                val ar = when (modoProporcion) {
                    "v" -> 0.75f
                    "h" -> 1.33f
                    else -> arBase
                }
                val spanCompleto = p.destacada && ar >= 1.25f
                add(ElementoParedMuseo.Cuadro(p, numGlobal, ar, spanCompleto))
            }
            if (incluirCuadroAgregar) {
                add(ElementoParedMuseo.CuadroAgregar)
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val totalWidth = maxWidth
        val gap = 14.dp
        val placaHeight = 28.dp
        val pad = 10.dp
        val cols = columnas.coerceIn(1, 3)
        val colWidth = (totalWidth - gap * (cols - 1)) / cols

        val layoutCalculado = remember(elementos, totalWidth, cols) {
            val alturasCol = FloatArray(cols) { 0f }
            val posiciones = mutableListOf<PosicionCuadro>()
            val cwVal = colWidth.value
            val gapVal = gap.value
            val padVal = pad.value
            val pqVal = placaHeight.value

            elementos.forEach { elem ->
                val ar = when (elem) {
                    is ElementoParedMuseo.Cuadro -> elem.aspectRatio
                    ElementoParedMuseo.CuadroAgregar -> 1.0f
                }
                val span = if (elem is ElementoParedMuseo.Cuadro && elem.spanCompleto && cols > 1) cols else 1
                val wVal = span * cwVal + (span - 1) * gapVal
                val fhVal = ((wVal - 2f * padVal) / ar) + 2f * padVal
                val hTotal = fhVal + pqVal

                var bestCol = 0
                var bestTop = Float.MAX_VALUE
                for (c in 0..(cols - span)) {
                    var maxY = 0f
                    for (k in c until (c + span)) {
                        if (alturasCol[k] > maxY) maxY = alturasCol[k]
                    }
                    if (maxY < bestTop - 0.5f) {
                        bestTop = maxY
                        bestCol = c
                    }
                }
                for (k in bestCol until (bestCol + span)) {
                    alturasCol[k] = bestTop + hTotal + gapVal
                }

                posiciones.add(
                    PosicionCuadro(
                        elemento = elem,
                        leftDp = (bestCol * (cwVal + gapVal)).dp,
                        topDp = bestTop.dp,
                        widthDp = wVal.dp,
                        frameHeightDp = fhVal.dp
                    )
                )
            }
            val maxWallHeight = ((alturasCol.maxOrNull() ?: 0f) - gapVal).coerceAtLeast(0f).dp
            posiciones to maxWallHeight
        }

        val (posiciones, wallHeight) = layoutCalculado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(wallHeight)
        ) {
            posiciones.forEach { pos ->
                Box(
                    modifier = Modifier
                        .offset(x = pos.leftDp, y = pos.topDp)
                        .width(pos.widthDp)
                ) {
                    when (val elem = pos.elemento) {
                        is ElementoParedMuseo.Cuadro -> {
                            MarcoCuadroMuseo(
                                pieza = elem.pieza,
                                numeroGlobal = elem.numeroGlobal,
                                frameHeight = pos.frameHeightDp,
                                vm = vm,
                                onClick = { onAbrirPieza(elem.pieza.id) }
                            )
                        }
                        ElementoParedMuseo.CuadroAgregar -> {
                            MarcoAgregarMuseo(
                                frameHeight = pos.frameHeightDp,
                                onClick = onAgregarEnSala
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarcoCuadroMuseo(
    pieza: PiezaEntity,
    numeroGlobal: Int,
    frameHeight: Dp,
    vm: ColeccionViewModel,
    onClick: () -> Unit
) {
    val gold = Color(0xFFE8C97A)
    val muted = Color(0xFF8D8777)
    val archivoFoto = vm.archivo(pieza.fotoPath)
    val paleta = rememberPaletaPieza(pieza.nombre)
    val esMarcoBiselado = (numeroGlobal % 3) == 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Marco físico del cuadro (.t-f o .t-b)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(frameHeight)
                .shadow(10.dp, RoundedCornerShape(1.dp))
                .background(if (esMarcoBiselado) Color(0xFF1A1E27) else Color(0xFF0D0F14))
                .border(
                    width = if (esMarcoBiselado) 4.dp else 1.dp,
                    color = if (esMarcoBiselado) Color(0xFF2C323D) else Color(0xFF7A6733)
                )
                .padding(if (esMarcoBiselado) 6.dp else 9.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!esMarcoBiselado) {
                            Modifier.border(1.dp, gold.copy(alpha = 0.28f))
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (archivoFoto != null) {
                    FotoPieza(
                        archivo = archivoFoto,
                        nombre = pieza.nombre,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(paleta.first, paleta.second, Color(0xFF0B0A07)),
                                    radius = 280f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val inicial = pieza.nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
                        Text(
                            text = inicial,
                            fontSize = 42.sp,
                            fontFamily = FontFamily.Serif,
                            color = gold.copy(alpha = 0.30f)
                        )
                    }
                }

                // Viñeta de galería (.grade) y reflejo diagonal de cristal (.glass)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                0.0f to Color.Transparent,
                                0.38f to Color.Transparent,
                                0.44f to Color.White.copy(alpha = 0.07f),
                                0.52f to Color.Transparent,
                                1.0f to Color(0x4405060A)
                            )
                        )
                )

                // Estrella dorada en la esquina superior derecha si es destacada (.s)
                if (pieza.destacada) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(7.dp)
                            .size(20.dp)
                            .background(Color(0xB30A0A0A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("★", fontSize = 10.sp, color = gold)
                    }
                }
            }
        }

        // Placa inferior numerada (.pq): "N.º 01  Nombre de la pieza"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(top = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = String.format(Locale.US, "N.º %02d", numeroGlobal),
                fontSize = 8.5.sp,
                letterSpacing = 1.6.sp,
                color = gold
            )
            Text(
                text = pieza.nombre,
                fontSize = 11.sp,
                color = muted,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MarcoAgregarMuseo(
    frameHeight: Dp,
    onClick: () -> Unit
) {
    val gold = Color(0xFFE8C97A)
    val muted = Color(0xFF8D8777)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(frameHeight),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    color = Color(0xFF6B5A2C),
                    style = Stroke(
                        width = 1.1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
                    )
                )
                val cx = size.width / 2f
                val cy = size.height / 2f
                val arm = 11.dp.toPx()
                drawLine(gold, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(gold, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(top = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Añadir",
                fontSize = 11.sp,
                color = muted,
                fontWeight = FontWeight.Light
            )
        }
    }
}

/** Piso de duela de madera de museo (.floor). */
@Composable
internal fun PisoMaderaMuseo(modifier: Modifier = Modifier, height: Dp = 20.dp) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val zocaloH = 5.dp.toPx()
        val fileteH = 1.dp.toPx()

        // Base de madera con tablones verticales cada 46dp
        drawRect(
            color = Color(0xFF3D2514),
            topLeft = Offset(0f, zocaloH),
            size = Size(w, h - zocaloH)
        )
        val plankStep = 46.dp.toPx()
        var x = plankStep
        while (x < w) {
            drawRect(
                color = Color(0xFF4E3019),
                topLeft = Offset(x, zocaloH),
                size = Size(2.dp.toPx(), h - zocaloH)
            )
            x += plankStep
        }

        // Zócalo oscuro superior y filete cálido
        drawRect(color = Color(0xFF0B0C10), topLeft = Offset.Zero, size = Size(w, zocaloH))
        drawRect(color = Color(0xFF241509), topLeft = Offset(0f, zocaloH), size = Size(w, fileteH))
    }
}

@Composable
private fun VistaSalaCompleta(
    numeroSala: Int,
    franquicia: String,
    piezas: List<PiezaEntity>,
    indiceGlobalPorId: Map<Long, Int>,
    vm: ColeccionViewModel,
    modoProporcion: String,
    columnas: Int,
    abrirPieza: (Long) -> Unit,
    agregarEnSala: () -> Unit,
    abrirOpcionesVista: () -> Unit,
    volver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = Color(0xFFE8C97A)
    Column(
        modifier = modifier.background(Color(0xFF0A0D17))
    ) {
        // Barra superior (.dtop)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF0B1224), Color(0xFF0A0D17))))
                .padding(start = 10.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = volver,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = "Volver" }
            ) {
                Canvas(Modifier.size(20.dp)) {
                    drawLine(
                        Color(0xFFF2EAD8),
                        Offset(size.width * 0.62f, size.height * 0.22f),
                        Offset(size.width * 0.30f, size.height * 0.50f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        Color(0xFFF2EAD8),
                        Offset(size.width * 0.30f, size.height * 0.50f),
                        Offset(size.width * 0.62f, size.height * 0.78f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            IconButton(
                onClick = abrirOpcionesVista,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xB312141A), CircleShape)
                    .border(1.dp, Color(0xFF26231C), CircleShape)
                    .semantics { contentDescription = "Cómo ver tus figuras" }
            ) {
                IconoMosaicoGaleria(color = Color(0xFFF2EAD8))
            }
        }

        // Cabecera de Sala (.dh)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0D17))
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text(
                text = String.format(Locale.US, "SALA %02d", numeroSala),
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = gold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = franquicia,
                fontSize = 34.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF2EAD8)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1A1E27), Color(0xFF13161D))
                            )
                        )
                        .padding(top = 24.dp)
                ) {
                    ParedCuadrosMuseo(
                        piezas = piezas,
                        indiceGlobalPorId = indiceGlobalPorId,
                        vm = vm,
                        modoProporcion = modoProporcion,
                        columnas = columnas,
                        incluirCuadroAgregar = true,
                        onAbrirPieza = abrirPieza,
                        onAgregarEnSala = agregarEnSala,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                    )
                    Spacer(Modifier.height(22.dp))
                    PisoMaderaMuseo()
                }
            }
            item {
                Text(
                    text = "${piezas.size} ${if (piezas.size == 1) "PIEZA" else "PIEZAS"}",
                    fontSize = 10.sp,
                    letterSpacing = 2.2.sp,
                    color = Color(0xFF8D8777),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 20.dp)
                )
            }
        }
    }
}

// =====================================================================
// SECCIÓN MODO BODEGA: CAVA DE VINOS (HERO + RACKS DE MADERA + ETIQUETAS)
// =====================================================================

private fun anioCosecha(fechaMillis: Long): Int {
    return runCatching {
        Instant.ofEpochMilli(fechaMillis).atZone(ZoneId.systemDefault()).year
    }.getOrDefault(2026)
}

@Composable
private fun HeroCavaBodega(
    pieza: PiezaEntity,
    vm: ColeccionViewModel,
    onClick: () -> Unit
) {
    val archivoFoto = vm.archivo(pieza.fotoPath)
    val paleta = rememberPaletaPieza(pieza.nombre)
    val anio = remember(pieza.fechaAdquisicion) { anioCosecha(pieza.fechaAdquisicion) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(312.dp)
            .background(Color(0xFF2A170B))
            .clickable(onClick = onClick)
    ) {
        // Celosía de madera diagonal a 45° (.lat) + lámpara cálida cenital (.lamp)
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val step = 40.dp.toPx()
            val latticeColor = Color(0xFF7A4A22).copy(alpha = 0.55f)
            val stroke = 3.5.dp.toPx()

            var d = -h
            while (d < w + h) {
                drawLine(latticeColor, Offset(d, 0f), Offset(d + h, h), strokeWidth = stroke)
                drawLine(latticeColor, Offset(d + h, 0f), Offset(d, h), strokeWidth = stroke)
                d += step
            }

            // Viñeta profunda interior
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xAA000000)),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.65f
                )
            )

            // Resplandor cálido de lámpara cenital (.lamp)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x80FFD68C), Color.Transparent),
                    center = Offset(w / 2f, 12.dp.toPx()),
                    radius = 130.dp.toPx()
                ),
                topLeft = Offset(w / 2f - 120.dp.toPx(), 12.dp.toPx()),
                size = Size(240.dp.toPx(), 130.dp.toPx())
            )
        }

        // Viga superior de madera (.beam)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xFF9A6630), Color(0xFF4E2F15))))
        )

        // Arco iluminado de la cava (.arch)
        val archShape = RoundedCornerShape(topStart = 102.dp, topEnd = 102.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 38.dp)
                .width(204.dp)
                .height(236.dp)
                .clip(archShape)
                .background(
                    Brush.radialGradient(
                        0.0f to Color(0xFFF6E2B6),
                        0.42f to Color(0xFFC79A58),
                        1.0f to Color(0xFF4E3014),
                        center = Offset(280f, 180f),
                        radius = 420f
                    )
                )
                .border(9.dp, Color(0xFF8F5D2B), archShape)
        ) {
            // Cuadro de la figura dentro del arco (.pic)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 38.dp)
                    .width(104.dp)
                    .height(138.dp)
                    .shadow(10.dp)
                    .background(Color(0xFF14100C))
                    .border(4.dp, Color(0xFF14100C))
                    .padding(4.dp)
            ) {
                if (archivoFoto != null) {
                    FotoPieza(archivoFoto, pieza.nombre, Modifier.fillMaxSize())
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
                            text = pieza.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
                            fontSize = 42.sp,
                            fontFamily = FontFamily.Serif,
                            color = Color(0x55E8C97A)
                        )
                    }
                }
            }

            // Mostrador inferior de madera (.cnt)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color(0xFFE0B46F), Color(0xFF7A4C22))))
            )
        }

        // Etiqueta colgante de pergamino (.hero .tag)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .width(218.dp)
                .graphicsLayer { rotationZ = -1.5f }
                .shadow(6.dp, RoundedCornerShape(3.dp))
                .background(Color(0xFFD8C59A), RoundedCornerShape(3.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ÚLTIMA EN ENTRAR",
                    fontSize = 7.5.sp,
                    letterSpacing = 2.0.sp,
                    color = Color(0xFF6A4A22)
                )
                Text(
                    text = pieza.nombre,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2B1A0C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "COSECHA $anio",
                    fontSize = 7.sp,
                    letterSpacing = 1.6.sp,
                    color = Color(0xFF6A4A22)
                )
            }
        }

        // Divisor inferior de cava
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(Color(0xFF4A2D16))
        )
    }
}

@Composable
private fun RackCavaFranquicia(
    numeroRack: Int,
    franquicia: String,
    piezas: List<PiezaEntity>,
    vm: ColeccionViewModel,
    onAbrirPieza: (Long) -> Unit,
    onGuardarEnRack: () -> Unit
) {
    val muted = Color(0xFF8D8777)
    val totalCeldas = ceil((piezas.size + 1) / 3.0).toInt() * 3
    val filas = totalCeldas / 3

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B0F07), Color(0xFF0F0804))))
            .padding(top = 22.dp)
    ) {
        // Cabecera de Rack (.plate): Placa metálica dorada "RACK 01" + Franquicia + Conteo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFE6C27A), Color(0xFF9A7433))),
                        RoundedCornerShape(3.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "RACK %02d", numeroRack),
                    fontSize = 10.sp,
                    letterSpacing = 2.0.sp,
                    color = Color(0xFF2A1A07)
                )
            }
            Text(
                text = franquicia,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF2EAD8),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${piezas.size} ${if (piezas.size == 1) "pieza" else "piezas"}",
                fontSize = 12.sp,
                color = muted,
                fontWeight = FontWeight.Light
            )
        }

        // Estructura de madera del Rack (.rk) con casilleros 3xN (.cells)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF8D5C2C), Color(0xFF5A3719))))
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            for (fila in 0 until filas) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    for (col in 0 until 3) {
                        val idx = fila * 3 + col
                        val pieza = piezas.getOrNull(idx)
                        val esPrimerVacio = idx == piezas.size
                        CasilleroCava(
                            pieza = pieza,
                            esPrimerVacio = esPrimerVacio,
                            vm = vm,
                            onAbrirPieza = onAbrirPieza,
                            onGuardarEnRack = onGuardarEnRack,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Base de piedra de cava (.stone)
        PisoPiedraCava()
    }
}

@Composable
private fun CasilleroCava(
    pieza: PiezaEntity?,
    esPrimerVacio: Boolean,
    vm: ColeccionViewModel,
    onAbrirPieza: (Long) -> Unit,
    onGuardarEnRack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goldCava = Color(0xFFE8B472)

    Box(modifier = modifier) {
        // Hueco de madera oscuro (.cub, aspect-ratio 3/4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .background(Color(0xFF0C0603))
                .border(1.dp, Color(0x99000000))
                .then(
                    if (pieza != null) {
                        Modifier.clickable { onAbrirPieza(pieza.id) }
                    } else if (esPrimerVacio) {
                        Modifier.clickable { onGuardarEnRack() }
                    } else {
                        Modifier
                    }
                )
        ) {
            if (pieza != null) {
                val archivoFoto = vm.archivo(pieza.fotoPath)
                val paleta = rememberPaletaPieza(pieza.nombre)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp)
                        .background(Color.Black)
                ) {
                    if (archivoFoto != null) {
                        FotoPieza(archivoFoto, pieza.nombre, Modifier.fillMaxSize())
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
                                text = pieza.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
                                fontSize = 34.sp,
                                fontFamily = FontFamily.Serif,
                                color = goldCava.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            } else if (esPrimerVacio) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(goldCava.copy(alpha = 0.12f), Color.Transparent),
                                radius = 180f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(24.dp)) {
                        val c = goldCava.copy(alpha = 0.65f)
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val arm = size.minDimension * 0.36f
                        drawLine(c, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(c, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }

            // Repisa inferior de madera del casillero (.cub::after)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color(0xFFBF8A47), Color(0xFF6A4020))))
            )
        }

        // Etiqueta colgante de pergamino (.cub .tag)
        if (pieza != null) {
            val anio = remember(pieza.fechaAdquisicion) { anioCosecha(pieza.fechaAdquisicion) }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 22.dp)
                    .fillMaxWidth(0.92f)
                    .graphicsLayer { rotationZ = -1.5f },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hilo de sujeción (.tag::before)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(8.dp)
                        .background(Color(0xFFB89A62))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(3.dp))
                        .background(Color(0xFFD8C59A), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pieza.nombre,
                            fontSize = 9.5.sp,
                            color = Color(0xFF2B1A0C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "COSECHA $anio",
                            fontSize = 6.5.sp,
                            letterSpacing = 1.4.sp,
                            color = Color(0xFF6A4A22),
                            fontStyle = FontStyle.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PisoPiedraCava() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val w = size.width
        val h = size.height
        val topLip = 5.dp.toPx()
        drawRect(color = Color(0xFF2D2822), topLeft = Offset(0f, topLip), size = Size(w, h - topLip))
        val step = 70.dp.toPx()
        var x = step
        while (x < w) {
            drawRect(color = Color(0xFF1A1612), topLeft = Offset(x, topLip), size = Size(2.dp.toPx(), h - topLip))
            x += step
        }
        drawRect(color = Color(0xFF090604), topLeft = Offset.Zero, size = Size(w, topLip))
    }
}

// =====================================================================
// ICONOS VECTORIALES Y CONTROLES DE LA HOJA "CÓMO VER TUS FIGURAS"
// =====================================================================

@Composable
private fun BotonOpcionVista(
    etiqueta: String,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: @Composable (Color) -> Unit
) {
    val gold = Color(0xFFE8C97A)
    val tint = if (activo) gold else Color(0xFF8D8777)
    val border = if (activo) Color(0xFF5A4A22) else Color(0xFF26231C)
    val bg = if (activo) Color(0x12E8C97A) else Color.Transparent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icono(tint)
            Text(
                text = etiqueta,
                fontSize = 12.sp,
                color = tint
            )
        }
    }
}

@Composable
private fun IconoMosaicoGaleria(color: Color, size: Dp = 19.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = 1.5.dp.toPx())
        val r = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        drawRoundRect(color, Offset(w * 0.12f, h * 0.12f), Size(w * 0.34f, h * 0.42f), r, s)
        drawRoundRect(color, Offset(w * 0.54f, h * 0.12f), Size(w * 0.34f, h * 0.25f), r, s)
        drawRoundRect(color, Offset(w * 0.54f, h * 0.46f), Size(w * 0.34f, h * 0.42f), r, s)
        drawRoundRect(color, Offset(w * 0.12f, h * 0.62f), Size(w * 0.34f, h * 0.26f), r, s)
    }
}

@Composable
private fun IconoMarcosVerticales(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(width = 1.5.dp.toPx())
        val r = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        drawRoundRect(color, Offset(w * 0.16f, h * 0.14f), Size(w * 0.30f, h * 0.72f), r, s)
        drawRoundRect(color, Offset(w * 0.54f, h * 0.14f), Size(w * 0.30f, h * 0.72f), r, s)
    }
}

@Composable
private fun IconoMarcosHorizontales(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(width = 1.5.dp.toPx())
        val r = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        drawRoundRect(color, Offset(w * 0.14f, h * 0.16f), Size(w * 0.72f, h * 0.30f), r, s)
        drawRoundRect(color, Offset(w * 0.14f, h * 0.54f), Size(w * 0.72f, h * 0.30f), r, s)
    }
}

@Composable
private fun IconoColumnasMarcos(columnas: Int, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(width = 1.5.dp.toPx())
        val r = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        when (columnas) {
            3 -> {
                drawRoundRect(color, Offset(w * 0.12f, h * 0.16f), Size(w * 0.21f, h * 0.68f), r, s)
                drawRoundRect(color, Offset(w * 0.39f, h * 0.16f), Size(w * 0.21f, h * 0.68f), r, s)
                drawRoundRect(color, Offset(w * 0.66f, h * 0.16f), Size(w * 0.21f, h * 0.68f), r, s)
            }
            2 -> {
                drawRoundRect(color, Offset(w * 0.16f, h * 0.16f), Size(w * 0.30f, h * 0.68f), r, s)
                drawRoundRect(color, Offset(w * 0.54f, h * 0.16f), Size(w * 0.30f, h * 0.68f), r, s)
            }
            else -> {
                drawRoundRect(color, Offset(w * 0.25f, h * 0.16f), Size(w * 0.50f, h * 0.68f), r, s)
            }
        }
    }
}
