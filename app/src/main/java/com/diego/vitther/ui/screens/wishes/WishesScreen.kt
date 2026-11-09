package com.diego.vitther.ui.screens.wishes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.domain.EstadoWish
import com.diego.vitther.presentation.wishes.WishesUiState
import com.diego.vitther.presentation.wishes.WishesViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.absoluteValue

private val WishBg = Color(0xFF0A0A0A)
private val WishSurface = Color(0xFF141414)
private val WishBorder = Color(0xFF26231C)
private val WishGold = Color(0xFFE8C97A)
private val WishGoldDark = Color(0xFF1A1405)
private val WishText = Color(0xFFF2EAD8)
private val WishMuted = Color(0xFF8D8777)
private val WishRed = Color(0xFFE0707A)

private val fechaCortaWish = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX"))

@Composable
fun WishesRoute(
    abrirCedula: (Long) -> Unit,
    vm: WishesViewModel = viewModel(factory = WishesViewModel.Factory)
) {
    val estado by vm.uiState.collectAsStateWithLifecycle()
    WishesScreen(
        estado = estado,
        onGuardarWish = vm::guardarWish,
        onYaLaCompre = vm::marcarYaLaCompre,
        onYaLlego = { wish -> vm.marcarYaLlego(wish, abrirCedula) },
        onEliminar = vm::eliminarWish,
        onLimpiarToast = vm::limpiarToast
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishesScreen(
    estado: WishesUiState,
    onGuardarWish: (Long, String, String, String?, EstadoWish, Long?, Long?) -> Unit,
    onYaLaCompre: (WishEntity) -> Unit,
    onYaLlego: (WishEntity) -> Unit,
    onEliminar: (WishEntity) -> Unit,
    onLimpiarToast: () -> Unit = {}
) {
    var tabActual by rememberSaveable { mutableStateOf(EstadoWish.POR_LLEGAR) }
    var wishSeleccionado by remember { mutableStateOf<WishEntity?>(null) }
    var mostrarDialogoFormulario by remember { mutableStateOf(false) }
    var wishEditando by remember { mutableStateOf<WishEntity?>(null) }
    val formatoNumero = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-MX")) }

    val listaVisible = if (tabActual == EstadoWish.POR_LLEGAR) estado.porLlegar else estado.deseadas
    val totalPorLlegar = estado.porLlegar.size
    val totalDeseadas = estado.deseadas.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WishBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera: Wishes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp)
            ) {
                Text(
                    text = "Wishes",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 36.sp
                    ),
                    color = WishText
                )
            }

            // Pestañas segmentadas: Por llegar | Deseadas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 12.dp)
                    .drawBehind {
                        drawLine(
                            color = WishBorder,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            ) {
                listOf(
                    EstadoWish.POR_LLEGAR to "Por llegar",
                    EstadoWish.DESEADA to "Deseadas"
                ).forEach { (modo, titulo) ->
                    val activo = tabActual == modo
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tabActual = modo }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = titulo,
                            color = if (activo) WishGold else WishMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (activo) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 11.dp)
                                    .fillMaxWidth(0.6f)
                                    .height(2.dp)
                                    .background(WishGold, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            // Lista de Wishes
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 100.dp)
            ) {
                if (listaVisible.isEmpty()) {
                    item(key = "vacio") {
                        val mensajeVacio = if (tabActual == EstadoWish.POR_LLEGAR) {
                            "Nada por llegar por ahora.\nCuando compres una preventa, aparecerá aquí."
                        } else {
                            "Tu lista de deseos está vacía.\nToca + para añadir tu próxima pieza."
                        }
                        Text(
                            text = mensajeVacio,
                            color = WishMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 60.dp)
                        )
                    }
                } else if (tabActual == EstadoWish.POR_LLEGAR) {
                    items(listaVisible, key = { it.id }) { wish ->
                        TarjetaPorLlegar(
                            wish = wish,
                            onClick = { wishSeleccionado = wish }
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                } else {
                    items(listaVisible, key = { it.id }) { wish ->
                        FilaDeseada(
                            wish = wish,
                            formatoNumero = formatoNumero,
                            onClick = { wishSeleccionado = wish }
                        )
                    }
                }

                if (totalPorLlegar + totalDeseadas > 0) {
                    item(key = "footer") {
                        val etiqDeseadas = if (totalDeseadas == 1) "deseada" else "deseadas"
                        Text(
                            text = "$totalPorLlegar POR LLEGAR · $totalDeseadas ${etiqDeseadas.uppercase(Locale.forLanguageTag("es-MX"))}",
                            color = WishMuted,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        // Botón flotante circular dorado (+)
        FloatingActionButton(
            onClick = {
                wishEditando = null
                mostrarDialogoFormulario = true
            },
            shape = CircleShape,
            containerColor = WishGold,
            contentColor = WishGoldDark,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Añadir deseo",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // Bottom Sheet de detalle / acciones de Wish
    wishSeleccionado?.let { sel ->
        val infoLlegada = remember(sel) { calcularDiasYFechaWish(sel.fechaEstimadaLlegada) }
        val subtitulo = if (sel.estado == EstadoWish.POR_LLEGAR) {
            "${sel.franquicia} · llega el ${infoLlegada.second}"
        } else {
            val metaTexto = sel.metaPesos?.let { "$${formatoNumero.format(it)} MXN" } ?: "Por definir"
            "${sel.franquicia} · meta $metaTexto"
        }

        ModalBottomSheet(
            onDismissRequest = { wishSeleccionado = null },
            containerColor = WishSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 14.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(WishBorder)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = sel.nombre,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                    color = WishText
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = WishMuted
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val actual = sel
                        wishSeleccionado = null
                        if (actual.estado == EstadoWish.POR_LLEGAR) {
                            onYaLlego(actual)
                        } else {
                            onYaLaCompre(actual)
                        }
                    },
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WishGold,
                        contentColor = WishGoldDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (sel.estado == EstadoWish.POR_LLEGAR) "Ya llegó" else "Ya la compré")
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = WishBorder, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val editar = sel
                            wishSeleccionado = null
                            wishEditando = editar
                            mostrarDialogoFormulario = true
                        }
                        .padding(vertical = 14.dp)
                ) {
                    Text("Editar", color = WishText, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider(color = WishBorder, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val borrar = sel
                            wishSeleccionado = null
                            onEliminar(borrar)
                        }
                        .padding(vertical = 14.dp)
                ) {
                    Text("Eliminar de Wishes", color = WishRed, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    // Diálogo para añadir o editar Wish
    if (mostrarDialogoFormulario) {
        val base = wishEditando
        var nombre by rememberSaveable(base?.id) { mutableStateOf(base?.nombre ?: "") }
        var franquicia by rememberSaveable(base?.id) { mutableStateOf(base?.franquicia ?: "") }
        var linea by rememberSaveable(base?.id) { mutableStateOf(base?.linea ?: "") }
        var modo by rememberSaveable(base?.id) { mutableStateOf(base?.estado ?: tabActual) }
        var diasTexto by rememberSaveable(base?.id) { mutableStateOf("30") }
        var metaTexto by rememberSaveable(base?.id) { mutableStateOf(base?.metaPesos?.toString() ?: "3500") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoFormulario = false },
            containerColor = WishSurface,
            title = {
                Text(
                    text = if (base == null) "Añadir a Wishes" else "Editar Wish",
                    color = WishText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = modo == EstadoWish.POR_LLEGAR,
                            onClick = { modo = EstadoWish.POR_LLEGAR },
                            label = { Text("Por llegar") }
                        )
                        FilterChip(
                            selected = modo == EstadoWish.DESEADA,
                            onClick = { modo = EstadoWish.DESEADA },
                            label = { Text("Deseada") }
                        )
                    }
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre de la figura *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = franquicia,
                        onValueChange = { franquicia = it },
                        label = { Text("Franquicia *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linea,
                        onValueChange = { linea = it },
                        label = { Text("Fabricante / Línea (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (modo == EstadoWish.POR_LLEGAR) {
                        OutlinedTextField(
                            value = diasTexto,
                            onValueChange = { nuevo -> diasTexto = nuevo.filter { it.isDigit() }.take(4) },
                            label = { Text("Llega en (días)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = metaTexto,
                            onValueChange = { nuevo -> metaTexto = nuevo.filter { it.isDigit() }.take(7) },
                            label = { Text("Meta estimada (MXN)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = nombre.isNotBlank() && franquicia.isNotBlank(),
                    onClick = {
                        onGuardarWish(
                            base?.id ?: 0L,
                            nombre,
                            franquicia,
                            linea.ifBlank { null },
                            modo,
                            diasTexto.toLongOrNull(),
                            metaTexto.toLongOrNull()
                        )
                        tabActual = modo
                        mostrarDialogoFormulario = false
                    }
                ) {
                    Text("Guardar", color = WishGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoFormulario = false }) {
                    Text("Cancelar", color = WishMuted)
                }
            }
        )
    }
}

@Composable
private fun TarjetaPorLlegar(wish: WishEntity, onClick: () -> Unit) {
    val (diasRestantes, fechaTexto) = remember(wish.fechaEstimadaLlegada) {
        calcularDiasYFechaWish(wish.fechaEstimadaLlegada)
    }
    val textoDias = if (diasRestantes == 1L) "1 día" else "$diasRestantes días"
    val progreso = (wish.progresoPorcentaje.coerceIn(5, 100)) / 100f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = WishSurface,
        border = BorderStroke(1.dp, WishBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PlaceholderWishImagen(
                nombre = wish.nombre,
                ancho = 84.dp,
                alto = 112.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = wish.franquicia.uppercase(Locale.forLanguageTag("es-MX")),
                        color = WishGold,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = wish.nombre,
                        color = WishText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            lineHeight = 21.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column {
                    // Barra de progreso de llegada
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(WishBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progreso)
                                .background(WishGold, RoundedCornerShape(3.dp))
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            append("Llega en ")
                            withStyle(SpanStyle(color = WishText, fontWeight = FontWeight.Normal)) {
                                append(textoDias)
                            }
                            append(" · $fechaTexto")
                        },
                        color = WishMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaDeseada(
    wish: WishEntity,
    formatoNumero: NumberFormat,
    onClick: () -> Unit
) {
    val metaFormateada = wish.metaPesos?.let { "$${formatoNumero.format(it)} MXN" } ?: "Por definir"
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PlaceholderWishImagen(
                nombre = wish.nombre,
                ancho = 56.dp,
                alto = 72.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wish.franquicia.uppercase(Locale.forLanguageTag("es-MX")),
                    color = WishGold,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = wish.nombre,
                    color = WishText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 20.sp
                    )
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Meta ")
                        withStyle(SpanStyle(color = WishGold)) {
                            append(metaFormateada)
                        }
                    },
                    color = WishMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            ChevronDerechaWish()
        }
        HorizontalDivider(color = WishBorder, thickness = 1.dp)
    }
}

/**
 * Overlay armónico con foco radial de vitrina (estilo Griffith / Evangelion)
 * aplicado de forma uniforme a TODAS las figuras de Wishes (tanto Por llegar como Deseadas,
 * incluidas las nuevas que agregue el usuario) hasta que la pieza llegue al museo y se le tome fotografía.
 */
@Composable
private fun PlaceholderWishImagen(
    nombre: String,
    ancho: Dp,
    alto: Dp
) {
    val paleta = remember(nombre) { gradientePorNombre(nombre) }
    Box(
        modifier = Modifier
            .size(ancho, alto)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(paleta.first, paleta.second, Color(0xFF0A0A0A))
                )
            )
            .border(1.dp, WishBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x55000000))
                    )
                )
        )
    }
}

@Composable
private fun ChevronDerechaWish() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        drawLine(
            color = WishMuted,
            start = Offset(w * 0.35f, h * 0.22f),
            end = Offset(w * 0.68f, h * 0.5f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = WishMuted,
            start = Offset(w * 0.68f, h * 0.5f),
            end = Offset(w * 0.35f, h * 0.78f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun gradientePorNombre(nombre: String): Pair<Color, Color> {
    val n = nombre.lowercase()
    return when {
        "evangelion" in n -> Color(0xFF4A3A6A) to Color(0xFF14101C)
        "griffith" in n || "guts" in n -> Color(0xFF5A5A62) to Color(0xFF121214)
        "gojo" in n || "link" in n || "goku" in n -> Color(0xFF2A4A6A) to Color(0xFF0F1620)
        "kratos" in n -> Color(0xFF5E3A36) to Color(0xFF140D0C)
        "zelda" in n -> Color(0xFF3E5648) to Color(0xFF0E1410)
        else -> {
            val paletasMuseo = listOf(
                Color(0xFF5A5A62) to Color(0xFF121214),
                Color(0xFF4A3A6A) to Color(0xFF14101C),
                Color(0xFF2A4A6A) to Color(0xFF0F1620),
                Color(0xFF564734) to Color(0xFF14110C),
                Color(0xFF5E3A36) to Color(0xFF140D0C),
                Color(0xFF3E5648) to Color(0xFF0E1410)
            )
            val idx = (nombre.hashCode().absoluteValue) % paletasMuseo.size
            paletasMuseo[idx]
        }
    }
}

private fun calcularDiasYFechaWish(fechaMillis: Long?): Pair<Long, String> {
    if (fechaMillis == null) return 30L to "próximamente"
    val zona = ZoneId.systemDefault()
    val hoy = LocalDate.now(zona)
    val destino = Instant.ofEpochMilli(fechaMillis).atZone(zona).toLocalDate()
    val dias = ChronoUnit.DAYS.between(hoy, destino).coerceAtLeast(1L)
    return dias to destino.format(fechaCortaWish)
}
