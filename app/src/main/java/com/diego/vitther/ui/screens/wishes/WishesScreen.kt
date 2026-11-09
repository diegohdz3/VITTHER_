package com.diego.vitther.ui.screens.wishes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.domain.EstadoWish
import com.diego.vitther.domain.Fechas
import com.diego.vitther.presentation.wishes.WishesViewModel
import com.diego.vitther.ui.theme.AccentGold
import com.diego.vitther.ui.theme.TextPrimary
import com.diego.vitther.ui.theme.TextSecondary
import com.diego.vitther.ui.theme.VitherSerif
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private val BgDark = Color(0xFF0B0A08)
private val CoralDelete = Color(0xFFE07A6E)

private fun formatoPesosEnteros(monto: Long): String {
    val nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-MX"))
    return "$${nf.format(monto)}"
}

@Composable
fun WishesRoute(
    abrirCedula: (Long) -> Unit,
    vm: WishesViewModel = viewModel(factory = WishesViewModel.Factory)
) {
    WishesScreen(
        viewModel = vm,
        alIncorporarPieza = abrirCedula
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishesScreen(
    viewModel: WishesViewModel,
    alIncorporarPieza: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var tabSeleccionada by rememberSaveable { mutableIntStateOf(0) } // 0 = Por llegar, 1 = Deseadas
    var wishAccion by remember { mutableStateOf<WishEntity?>(null) }
    var mostrarForm by rememberSaveable { mutableStateOf(false) }
    var wishEditando by remember { mutableStateOf<WishEntity?>(null) }
    var estadoInicialForm by rememberSaveable { mutableStateOf(EstadoWish.POR_LLEGAR) }

    // Animación de levantar el velo ("Ya llegó" en el hero de Por llegar)
    var levantandoVeloId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.mensajeToast) {
        if (state.mensajeToast != null) {
            delay(2200)
            viewModel.limpiarToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            // Cabecera (.hd)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wishes",
                    fontFamily = VitherSerif,
                    fontSize = 32.sp,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF15130F))
                        .border(1.dp, Color(0xFF2B251B), CircleShape)
                        .clickable {
                            wishEditando = null
                            estadoInicialForm = if (tabSeleccionada == 0) EstadoWish.POR_LLEGAR else EstadoWish.DESEADA
                            mostrarForm = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Reservar wish",
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Pestañas (.tabs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp)
            ) {
                WishTabButton(
                    titulo = "Por llegar",
                    conteo = state.porLlegar.size,
                    activo = tabSeleccionada == 0,
                    onClick = { tabSeleccionada = 0 },
                    modifier = Modifier.weight(1f)
                )
                WishTabButton(
                    titulo = "Deseadas",
                    conteo = state.deseadas.size,
                    activo = tabSeleccionada == 1,
                    onClick = { tabSeleccionada = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(1.dp)
                    .background(Color(0xFF1D1913))
            )

            // Contenido de la pestaña activa
            if (tabSeleccionada == 0) {
                SeccionPorLlegar(
                    items = state.porLlegar,
                    levantandoVeloId = levantandoVeloId,
                    resolverArchivo = viewModel::archivo,
                    onTapItem = { wishAccion = it },
                    onReservarNuevo = {
                        wishEditando = null
                        estadoInicialForm = EstadoWish.POR_LLEGAR
                        mostrarForm = true
                    }
                )
            } else {
                SeccionDeseadas(
                    items = state.deseadas,
                    resolverArchivo = viewModel::archivo,
                    onTapItem = { wishAccion = it },
                    onAgregarDeseo = {
                        wishEditando = null
                        estadoInicialForm = EstadoWish.DESEADA
                        mostrarForm = true
                    }
                )
            }
        }

        // Toast flotante (.toast)
        androidx.compose.animation.AnimatedVisibility(
            visible = state.mensajeToast != null,
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
                    text = state.mensajeToast.orEmpty(),
                    color = AccentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Bottom sheet de Acciones (#sa)
    wishAccion?.let { wish ->
        val esGranDeseo = wish.estado == EstadoWish.DESEADA && state.deseadas.firstOrNull()?.id == wish.id
        ModalBottomSheet(
            onDismissRequest = { wishAccion = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF13110D),
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = wish.nombre,
                    fontFamily = VitherSerif,
                    fontSize = 25.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = listOfNotNull(wish.franquicia, wish.linea).joinToString(" · ").uppercase(),
                    color = Color(0xFF7A7267),
                    fontSize = 9.5.sp,
                    letterSpacing = 2.4.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Botón primario dorado ("Ya llegó" o "Ya la compré")
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
                        .border(1.dp, Color(0xFF6B5832), RoundedCornerShape(13.dp))
                        .clickable {
                            val target = wish
                            wishAccion = null
                            if (target.estado == EstadoWish.POR_LLEGAR) {
                                val esHero = state.porLlegar.firstOrNull()?.id == target.id
                                if (esHero) {
                                    scope.launch {
                                        levantandoVeloId = target.id
                                        delay(1150)
                                        levantandoVeloId = null
                                        viewModel.marcarYaLlego(target, alIncorporarPieza)
                                    }
                                } else {
                                    viewModel.marcarYaLlego(target, alIncorporarPieza)
                                }
                            } else {
                                viewModel.marcarYaLaCompre(target)
                                tabSeleccionada = 0
                            }
                        }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (wish.estado == EstadoWish.POR_LLEGAR) "Ya llegó" else "Ya la compré",
                        color = AccentGold,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // "Hacer mi gran deseo" (si es Deseada y no está en #1)
                if (wish.estado == EstadoWish.DESEADA && !esGranDeseo) {
                    SheetActionButton(
                        texto = "Hacer mi gran deseo",
                        colorTexto = TextPrimary,
                        onClick = {
                            val target = wish
                            wishAccion = null
                            viewModel.hacerGranDeseo(target)
                        }
                    )
                }

                // "Editar"
                SheetActionButton(
                    texto = "Editar",
                    colorTexto = TextPrimary,
                    onClick = {
                        val target = wish
                        wishAccion = null
                        wishEditando = target
                        estadoInicialForm = target.estado
                        mostrarForm = true
                    }
                )

                // "Eliminar"
                SheetActionButton(
                    texto = "Eliminar",
                    colorTexto = CoralDelete,
                    onClick = {
                        val target = wish
                        wishAccion = null
                        viewModel.eliminarWish(target)
                    }
                )
            }
        }
    }

    // Bottom sheet de Formulario (#sf — "Reservar un lugar" / "Editar deseo")
    if (mostrarForm) {
        WishFormBottomSheet(
            wishInicial = wishEditando,
            estadoInicial = estadoInicialForm,
            resolverArchivo = viewModel::archivo,
            onImportarFoto = viewModel::importarFoto,
            onDismiss = {
                mostrarForm = false
                wishEditando = null
            },
            onGuardar = { id, nombre, franquicia, linea, estado, fechaCompra, fechaLlegada, metaPesos, fotoPath, creadoOriginal ->
                viewModel.guardarWish(
                    id = id,
                    nombre = nombre,
                    franquicia = franquicia,
                    linea = linea,
                    estado = estado,
                    metaPesos = metaPesos,
                    fechaCompraMillis = fechaCompra,
                    fechaLlegadaMillis = fechaLlegada,
                    fotoPath = fotoPath,
                    creadoEnOriginal = creadoOriginal
                )
                tabSeleccionada = if (estado == EstadoWish.POR_LLEGAR) 0 else 1
                mostrarForm = false
                wishEditando = null
            },
            onEliminar = { wish ->
                viewModel.eliminarWish(wish)
                mostrarForm = false
                wishEditando = null
            }
        )
    }
}

@Composable
private fun WishTabButton(
    titulo: String,
    conteo: Int,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 11.dp)
        ) {
            Text(
                text = titulo,
                color = if (activo) AccentGold else Color(0xFF7A7267),
                fontSize = 13.sp,
                fontWeight = if (activo) FontWeight.Medium else FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = conteo.toString(),
                color = if (activo) Color(0xFFB59A5B) else Color(0xFF6B6358),
                fontSize = 11.sp
            )
        }
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(1.5.dp)
                .background(if (activo) AccentGold else Color.Transparent)
        )
    }
}

@Composable
private fun SeccionPorLlegar(
    items: List<WishEntity>,
    levantandoVeloId: Long?,
    resolverArchivo: (String?) -> File?,
    onTapItem: (WishEntity) -> Unit,
    onReservarNuevo: () -> Unit
) {
    if (items.isEmpty()) {
        // Escenario iluminado vacío con marco punteado (.fm.empty)
        EscenarioIluminadoWish {
            MarcoPunteadoWish(
                ancho = 168.dp,
                onClick = onReservarNuevo
            )
        }
        Text(
            text = "Reserva un lugar para lo que viene",
            fontFamily = VitherSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 19.sp,
            color = Color(0xFF9A9184),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp)
        )
        return
    }

    val hero = items.first()
    val diasRestantes = hero.fechaEstimadaLlegada?.let { Fechas.diasHasta(it) } ?: 30L
    val textoNumero = if (diasRestantes <= 0L) "HOY" else diasRestantes.toString()
    val textoUnidad = when {
        diasRestantes <= 0L -> "DÍA DE LLEGADA"
        diasRestantes == 1L -> "DÍA"
        else -> "DÍAS"
    }

    // Escenario iluminado con la pieza próxima detrás del velo (.hs + .fm + .veil + .cloth)
    EscenarioIluminadoWish {
        MarcoMuseoWish(
            wish = hero,
            ancho = 168.dp,
            conVelo = true,
            veloLevantado = levantandoVeloId == hero.id,
            conSilueta = false,
            conCorazon = false,
            resolverArchivo = resolverArchivo,
            onClick = { onTapItem(hero) }
        )
    }

    // Información del Hero (.hi)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTapItem(hero) }
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = textoNumero,
            fontFamily = VitherSerif,
            fontSize = 56.sp,
            color = AccentGold,
            letterSpacing = (-1.5).sp,
            lineHeight = 56.sp
        )
        Text(
            text = textoUnidad,
            fontSize = 8.5.sp,
            letterSpacing = 3.2.sp,
            color = Color(0xFF8A7A58),
            modifier = Modifier.padding(top = 3.dp, bottom = 12.dp)
        )
        Text(
            text = hero.nombre,
            fontFamily = VitherSerif,
            fontSize = 29.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 31.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = listOfNotNull(hero.franquicia, hero.linea).joinToString(" · ").uppercase(),
            fontSize = 9.5.sp,
            letterSpacing = 2.4.sp,
            color = Color(0xFF7A7267),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        // Barra de progreso (.pb)
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF201C15))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((hero.progresoPorcentaje.coerceIn(8, 100)) / 100f)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF8A7340), AccentGold)
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        val fechaCorta = hero.fechaEstimadaLlegada?.let { Fechas.formatearCorto(it) } ?: "Próximamente"
        Text(
            text = "Llega el $fechaCorta",
            fontSize = 11.5.sp,
            color = Color(0xFF9A9184)
        )
    }

    val restantes = items.drop(1)
    if (restantes.isNotEmpty()) {
        SeparadorMuseo()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            restantes.forEach { wish ->
                FilaPorLlegar(
                    wish = wish,
                    resolverArchivo = resolverArchivo,
                    onClick = { onTapItem(wish) }
                )
            }
        }
    }
}

@Composable
private fun FilaPorLlegar(
    wish: WishEntity,
    resolverArchivo: (String?) -> File?,
    onClick: () -> Unit
) {
    val dias = wish.fechaEstimadaLlegada?.let { Fechas.diasHasta(it) } ?: 30L
    val fechaCorta = wish.fechaEstimadaLlegada?.let { Fechas.formatearCorto(it) } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF15120D), Color(0xFF0E0C09))
                )
            )
            .border(1.dp, Color(0xFF2A2419), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MarcoMuseoWish(
            wish = wish,
            ancho = 66.dp,
            conVelo = true,
            veloLevantado = false,
            conSilueta = false,
            conCorazon = false,
            resolverArchivo = resolverArchivo,
            onClick = onClick
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = wish.franquicia.uppercase(),
                color = Color(0xFFB59A5B),
                fontSize = 8.5.sp,
                letterSpacing = 2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = wish.nombre,
                fontFamily = VitherSerif,
                fontSize = 19.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF201C15))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((wish.progresoPorcentaje.coerceIn(8, 100)) / 100f)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF8A7340), AccentGold)
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val textoLlegada = when {
                    dias <= 0L -> "Llega hoy"
                    dias == 1L -> "Llega mañana"
                    else -> "Llega en $dias días"
                }
                Text(
                    text = textoLlegada,
                    color = TextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
                if (fechaCorta.isNotBlank()) {
                    Text(
                        text = " · $fechaCorta",
                        color = Color(0xFF9A9184),
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionDeseadas(
    items: List<WishEntity>,
    resolverArchivo: (String?) -> File?,
    onTapItem: (WishEntity) -> Unit,
    onAgregarDeseo: () -> Unit
) {
    if (items.isEmpty()) {
        EscenarioIluminadoWish {
            MarcoPunteadoWish(
                ancho = 168.dp,
                onClick = onAgregarDeseo
            )
        }
        Text(
            text = "¿Qué pieza sueñas tener?",
            fontFamily = VitherSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 19.sp,
            color = Color(0xFF9A9184),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp)
        )
        return
    }

    val granDeseo = items.first()
    EscenarioIluminadoWish {
        MarcoMuseoWish(
            wish = granDeseo,
            ancho = 168.dp,
            conVelo = false,
            veloLevantado = false,
            conSilueta = true,
            conCorazon = true,
            resolverArchivo = resolverArchivo,
            onClick = { onTapItem(granDeseo) }
        )
    }

    // Información de "EL GRAN DESEO" (.hi)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTapItem(granDeseo) }
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EL GRAN DESEO",
            color = AccentGold,
            fontSize = 9.sp,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = granDeseo.nombre,
            fontFamily = VitherSerif,
            fontSize = 29.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 31.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = listOfNotNull(granDeseo.franquicia, granDeseo.linea).joinToString(" · ").uppercase(),
            color = Color(0xFF7A7267),
            fontSize = 9.5.sp,
            letterSpacing = 2.4.sp,
            textAlign = TextAlign.Center
        )
        granDeseo.metaPesos?.takeIf { it > 0L }?.let { meta ->
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Meta ",
                    color = Color(0xFF9A9184),
                    fontSize = 12.sp
                )
                Text(
                    text = "${formatoPesosEnteros(meta)} MXN",
                    color = AccentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    SeparadorMuseo()

    // Cuadrícula de vitrinas 2 columnas (.grid / .vt) + vitrina punteada final (.vt.dsh)
    val restantes = items.drop(1)
    val romanos = listOf("II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")
    val totalCeldas = restantes.size + 1 // +1 para la vitrina punteada "+"
    val filas = (totalCeldas + 1) / 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (filaIdx in 0 until filas) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (colIdx in 0..1) {
                    val itemIdx = filaIdx * 2 + colIdx
                    if (itemIdx < restantes.size) {
                        val wish = restantes[itemIdx]
                        val romano = romanos.getOrElse(itemIdx) { "${itemIdx + 2}" }
                        VitrinaDeseadaCard(
                            wish = wish,
                            romano = romano,
                            resolverArchivo = resolverArchivo,
                            onClick = { onTapItem(wish) },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (itemIdx == restantes.size) {
                        VitrinaDeseadaNuevaCard(
                            onClick = onAgregarDeseo,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun VitrinaDeseadaCard(
    wish: WishEntity,
    romano: String,
    resolverArchivo: (String?) -> File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val archivo = remember(wish.fotoPath) { resolverArchivo(wish.fotoPath) }
    val matrizSilueta = remember {
        ColorMatrix().apply {
            setToSaturation(0.45f)
        }
    }

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vitrina de cristal (.cs)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF16130D), Color(0xFF0C0A07))
                    )
                )
                .border(
                    1.dp,
                    Color(0xFF2C261B),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                )
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0C0A07)),
                contentAlignment = Alignment.Center
            ) {
                if (archivo != null) {
                    SubcomposeAsyncImage(
                        model = archivo,
                        contentDescription = wish.nombre,
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(matrizSilueta),
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.75f)
                    )
                } else {
                    MonogramaDeseo(wish = wish)
                }

                // Viñeta oscura de misterio (.sil::after)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x28000000), Color(0xCC060504))
                            )
                        )
                )
            }

            // Número romano arriba a la izquierda (.rm)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xD90E0C08))
                    .border(1.dp, Color(0xFF2F281C), RoundedCornerShape(999.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = romano,
                    fontFamily = VitherSerif,
                    fontSize = 10.sp,
                    color = AccentGold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Pedestal (.ped)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2C2518), Color(0xFF17130C))
                    )
                )
                .border(
                    1.dp,
                    Color(0xFF3A3120),
                    RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                )
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = wish.nombre,
            fontFamily = VitherSerif,
            fontSize = 16.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        wish.metaPesos?.takeIf { it > 0L }?.let { meta ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Meta ${formatoPesosEnteros(meta)}",
                color = AccentGold,
                fontSize = 10.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VitrinaDeseadaNuevaCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .drawBehind {
                    drawRoundRect(
                        color = Color(0xFF302819),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        ),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF3A3120), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Añadir deseo",
                    tint = AccentGold,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun EscenarioIluminadoWish(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Riel de iluminación (.rail), lámparas (.lamp) y conos de luz (.cone)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Riel superior a 12.dp
            val railY = 12.dp.toPx()
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF2E271B),
                        Color(0xFF473C27),
                        Color(0xFF2E271B),
                        Color.Transparent
                    )
                ),
                start = Offset(w * 0.10f, railY),
                end = Offset(w * 0.90f, railY),
                strokeWidth = 2.dp.toPx()
            )

            // Dos lámparas al 27% y 73%
            val lampPositions = listOf(w * 0.27f, w * 0.73f)
            lampPositions.forEach { lx ->
                // Cono de luz
                val conePath = Path().apply {
                    moveTo(lx - 18.dp.toPx(), railY + 8.dp.toPx())
                    lineTo(lx + 18.dp.toPx(), railY + 8.dp.toPx())
                    lineTo(w * 0.5f + (lx - w * 0.5f) * 1.4f + 70.dp.toPx(), h)
                    lineTo(w * 0.5f + (lx - w * 0.5f) * 1.4f - 70.dp.toPx(), h)
                    close()
                }
                drawPath(
                    path = conePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x38FFE196),
                            Color(0x14FFE196),
                            Color.Transparent
                        ),
                        startY = railY,
                        endY = h
                    )
                )

                // Cuerpo de la lámpara
                drawRoundRect(
                    color = Color(0xFF4A3E28),
                    topLeft = Offset(lx - 7.dp.toPx(), railY - 2.dp.toPx()),
                    size = Size(14.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFFFE9A8),
                    radius = 4.dp.toPx(),
                    center = Offset(lx, railY + 8.dp.toPx())
                )
            }

            // Resplandor radial central sobre el marco
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2EFFDC8C), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = w * 0.48f
                ),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = w * 0.48f
            )
        }

        Box(modifier = Modifier.padding(bottom = 14.dp)) {
            content()
        }
    }
}

@Composable
private fun MarcoMuseoWish(
    wish: WishEntity,
    ancho: Dp,
    conVelo: Boolean,
    veloLevantado: Boolean,
    conSilueta: Boolean,
    conCorazon: Boolean,
    resolverArchivo: (String?) -> File?,
    onClick: () -> Unit
) {
    val archivo = remember(wish.fotoPath) { resolverArchivo(wish.fotoPath) }
    val paddingMarco = if (ancho > 100.dp) 8.dp else 4.dp
    val cornerMarco = if (ancho > 100.dp) 6.dp else 4.dp

    val liftProgress by animateFloatAsState(
        targetValue = if (veloLevantado) 1f else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "veilLift"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "starPulse")
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starScale"
    )
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starAlpha"
    )

    val matrizSilueta = remember {
        ColorMatrix().apply {
            setToSaturation(0.45f)
        }
    }

    Box(
        modifier = Modifier
            .width(ancho)
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(cornerMarco))
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
            .border(1.dp, Color(0xFF6B5832), RoundedCornerShape(cornerMarco))
            .clickable(onClick = onClick)
            .padding(paddingMarco)
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
                    contentDescription = wish.nombre,
                    contentScale = ContentScale.Crop,
                    colorFilter = if (conSilueta) ColorFilter.colorMatrix(matrizSilueta) else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (conSilueta) 0.78f else if (conVelo) 0.45f else 1f)
                )
            } else {
                MonogramaDeseo(wish = wish)
            }

            if (conSilueta) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x22000000), Color(0xCC060504))
                            )
                        )
                )
            }

            // Velo translúcido con pliegues y estrella dorada pulsante (.veil + .cloth)
            if (conVelo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-ancho * 1.4f) * liftProgress)
                        .alpha(1f - liftProgress)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xD91A160E),
                                    Color(0xEB0E0C08),
                                    Color(0xFA090806)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val starSize = if (ancho > 100.dp) 30.dp else 16.dp
                    Canvas(
                        modifier = Modifier
                            .size(starSize)
                            .scale(starScale)
                            .alpha(starAlpha)
                    ) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val rOut = size.width * 0.45f
                        val rIn = size.width * 0.12f
                        val starPath = Path().apply {
                            moveTo(cx, cy - rOut)
                            lineTo(cx + rIn, cy - rIn)
                            lineTo(cx + rOut, cy)
                            lineTo(cx + rIn, cy + rIn)
                            lineTo(cx, cy + rOut)
                            lineTo(cx - rIn, cy + rIn)
                            lineTo(cx - rOut, cy)
                            lineTo(cx - rIn, cy - rIn)
                            close()
                        }
                        drawPath(
                            path = starPath,
                            color = AccentGold,
                            style = Stroke(width = 1.4.dp.toPx())
                        )
                    }
                }
            }
        }

        // Insignia de corazón dorado arriba a la izquierda en "EL GRAN DESEO" (.hb)
        if (conCorazon) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xEB17140D))
                    .border(1.dp, Color(0xFF3A3120), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "El gran deseo",
                    tint = AccentGold,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun MarcoPunteadoWish(
    ancho: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(ancho)
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x9918150F), Color(0x990D0B08))
                )
            )
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFF3A3120),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    ),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFF3A3120), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Añadir",
                tint = AccentGold,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MonogramaDeseo(wish: WishEntity) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF231D12), Color(0xFF0A0806))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = wish.nombre.trim().take(1).uppercase().ifEmpty { "V" },
            fontFamily = VitherSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 36.sp,
            color = AccentGold.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun SeparadorMuseo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0xFF2B2418), Color.Transparent)
                )
            )
    )
}

@Composable
private fun SheetActionButton(
    texto: String,
    colorTexto: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF17140E))
            .border(1.dp, Color(0xFF262117), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishFormBottomSheet(
    wishInicial: WishEntity?,
    estadoInicial: EstadoWish,
    resolverArchivo: (String?) -> File?,
    onImportarFoto: (Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onGuardar: (
        id: Long,
        nombre: String,
        franquicia: String,
        linea: String?,
        estado: EstadoWish,
        fechaCompra: Long?,
        fechaLlegada: Long?,
        metaPesos: Long?,
        fotoPath: String?,
        creadoOriginal: Long?
    ) -> Unit,
    onEliminar: (WishEntity) -> Unit
) {
    var estado by rememberSaveable { mutableStateOf(wishInicial?.estado ?: estadoInicial) }
    var nombre by rememberSaveable { mutableStateOf(wishInicial?.nombre.orEmpty()) }
    var franquicia by rememberSaveable { mutableStateOf(wishInicial?.franquicia.orEmpty()) }
    var linea by rememberSaveable { mutableStateOf(wishInicial?.linea.orEmpty()) }
    var metaTexto by rememberSaveable { mutableStateOf(wishInicial?.metaPesos?.toString().orEmpty()) }
    var fotoPath by rememberSaveable { mutableStateOf(wishInicial?.fotoPath) }

    val hoyMillis = remember { Fechas.desdeFecha(LocalDate.now()) }
    var fechaCompraMillis by rememberSaveable { mutableStateOf(hoyMillis) }
    var fechaLlegadaMillis by rememberSaveable {
        mutableStateOf(
            wishInicial?.fechaEstimadaLlegada ?: Fechas.desdeFecha(LocalDate.now().plusDays(45))
        )
    }

    var mostrarPickerCompra by remember { mutableStateOf(false) }
    var mostrarPickerLlegada by remember { mutableStateOf(false) }

    val selectorImagen = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportarFoto(uri) { nuevoPath ->
                fotoPath = nuevoPath
            }
        }
    }

    val sugerenciasFranquicia = listOf(
        "Dragon Ball",
        "One Piece",
        "Evangelion",
        "Berserk",
        "Naruto",
        "Jujutsu Kaisen",
        "Saint Seiya"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF13110D),
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = when {
                    wishInicial != null -> "Editar deseo"
                    estado == EstadoWish.POR_LLEGAR -> "Reservar un lugar"
                    else -> "Nuevo deseo"
                },
                fontFamily = VitherSerif,
                fontSize = 25.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            )

            // Segmented control: Por llegar | Deseada (.seg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF0E0C09))
                    .border(1.dp, Color(0xFF242018), RoundedCornerShape(11.dp))
                    .padding(3.dp)
            ) {
                listOf(
                    EstadoWish.POR_LLEGAR to "Por llegar",
                    EstadoWish.DESEADA to "Deseada"
                ).forEach { (opcion, etiqueta) ->
                    val activo = estado == opcion
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activo) Color(0xFF211C13) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (activo) Color(0xFF4A3D24) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { estado = opcion }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = etiqueta,
                            color = if (activo) AccentGold else Color(0xFF7A7267),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = Color(0xFF252017),
                focusedContainerColor = Color(0xFF17140E),
                unfocusedContainerColor = Color(0xFF17140E),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = AccentGold,
                unfocusedLabelColor = TextSecondary
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la figura") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = franquicia,
                onValueChange = { franquicia = it },
                label = { Text("Franquicia") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Chips rápidos de franquicia (.chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sugerenciasFranquicia.forEach { sug ->
                    val activo = franquicia.equals(sug, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (activo) Color(0xFF211C13) else Color(0xFF17140E))
                            .border(
                                1.dp,
                                if (activo) AccentGold else Color(0xFF2A2419),
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { franquicia = sug }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sug,
                            color = if (activo) AccentGold else Color(0xFF9A9184),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = linea,
                onValueChange = { linea = it },
                label = { Text("Fabricante o línea (opcional)") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (estado == EstadoWish.DESEADA) {
                OutlinedTextField(
                    value = metaTexto,
                    onValueChange = { metaTexto = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Meta (MXN)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Fecha de compra y Llegada estimada (.r2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampoFechaWish(
                        etiqueta = "FECHA DE COMPRA",
                        fechaMillis = fechaCompraMillis,
                        onClick = { mostrarPickerCompra = true },
                        modifier = Modifier.weight(1f)
                    )
                    CampoFechaWish(
                        etiqueta = "LLEGADA ESTIMADA",
                        fechaMillis = fechaLlegadaMillis,
                        onClick = { mostrarPickerLlegada = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Foto de referencia (opcional) (.phb)
            Text(
                text = "FOTO DE REFERENCIA (OPCIONAL)",
                color = Color(0xFF7A7267),
                fontSize = 9.sp,
                letterSpacing = 1.8.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
            )
            val archivoFoto = remember(fotoPath) { resolverArchivo(fotoPath) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF15120D))
                    .drawBehind {
                        drawRoundRect(
                            color = Color(0xFF332B1D),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            ),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )
                    }
                    .clickable { selectorImagen.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (archivoFoto != null) {
                    SubcomposeAsyncImage(
                        model = archivoFoto,
                        contentDescription = "Foto de referencia",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "+ Añadir imagen",
                        color = Color(0xFF8A8073),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón primario de guardar
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
                    .border(1.dp, Color(0xFF6B5832), RoundedCornerShape(13.dp))
                    .clickable {
                        if (nombre.isNotBlank()) {
                            onGuardar(
                                wishInicial?.id ?: 0L,
                                nombre.trim(),
                                franquicia.trim().ifBlank { "Colección" },
                                linea.trim().ifBlank { null },
                                estado,
                                fechaCompraMillis,
                                fechaLlegadaMillis,
                                metaTexto.toLongOrNull(),
                                fotoPath,
                                wishInicial?.creadoEn
                            )
                        }
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        wishInicial != null -> "Guardar"
                        estado == EstadoWish.POR_LLEGAR -> "Reservar"
                        else -> "Guardar en Wishes"
                    },
                    color = AccentGold,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (wishInicial != null) {
                SheetActionButton(
                    texto = "Eliminar",
                    colorTexto = CoralDelete,
                    onClick = { onEliminar(wishInicial) }
                )
            }
        }
    }

    if (mostrarPickerCompra) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = fechaCompraMillis)
        DatePickerDialog(
            onDismissRequest = { mostrarPickerCompra = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val local = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        fechaCompraMillis = Fechas.desdeFecha(local)
                    }
                    mostrarPickerCompra = false
                }) {
                    Text("Aceptar", color = AccentGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPickerCompra = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (mostrarPickerLlegada) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = fechaLlegadaMillis)
        DatePickerDialog(
            onDismissRequest = { mostrarPickerLlegada = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val local = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        fechaLlegadaMillis = Fechas.desdeFecha(local)
                    }
                    mostrarPickerLlegada = false
                }) {
                    Text("Aceptar", color = AccentGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPickerLlegada = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun CampoFechaWish(
    etiqueta: String,
    fechaMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = etiqueta,
            color = Color(0xFF7A7267),
            fontSize = 9.sp,
            letterSpacing = 1.8.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF17140E))
                .border(1.dp, Color(0xFF252017), RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Text(
                text = Fechas.formatearCorto(fechaMillis),
                color = TextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
