package com.diego.vitther.ui.screens.coleccion

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.*
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.*
import java.text.NumberFormat
import java.util.Locale

private val GoldAccent = Color(0xFFE8C97A)
private val GoldBorderActive = Color(0xFF5A4A22)
private val LineDivider = Color(0xFF26231C)
private val MutedText = Color(0xFF8D8777)
private val DangerRed = Color(0xFFE0707A)
private val DarkBg = Color(0xFF0A0A0A)
private val SheetSurface = Color(0xFF141414)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CedulaScreen(
    pieza: PiezaEntity?,
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
    var confirmarEliminar by remember { mutableStateOf(false) }
    val formatoMoneda = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")) }
    val contexto = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. HERO CON FOTO Y GRADIENTE INFERIOR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3A3222),
                                Color(0xFF15120C),
                                DarkBg
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                FotoPieza(
                    archivo = vm.archivo(pieza.fotoPath),
                    nombre = pieza.nombre,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradiente inferior de 120.dp hacia el fondo oscuro (#0A0A0A)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, DarkBg)
                            )
                        )
                )
            }

            // 2. CABECERA SUPERPUESTA (margin-top: -34dp simulado con offset)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp)
                    .padding(horizontal = 20.dp)
            ) {
                val subtituloFranquicia = buildString {
                    append(pieza.franquicia)
                    pieza.linea?.takeIf { it.isNotBlank() }?.let {
                        append(" · ")
                        append(it)
                    }
                }
                Text(
                    text = subtituloFranquicia,
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pieza.nombre,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        lineHeight = 33.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(10.dp))

                // Chips de estado y edición limitada
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChipDetalle(
                        texto = pieza.estado.etiqueta,
                        activo = pieza.estado == EstadoPieza.EXHIBICION
                    )
                    if (pieza.edicionLimitada) {
                        val etiquetaEdicion = pieza.numeroEdicion?.takeIf { it.isNotBlank() }
                            ?.let { "Edición limitada · $it" } ?: "Edición limitada"
                        ChipDetalle(texto = etiquetaEdicion, activo = false)
                    }
                    if (pieza.destacada) {
                        ChipDetalle(texto = "Destacada", activo = true)
                    }
                }
            }

            // 3. PESTAÑAS (Cédula | Historia | Cápsulas)
            val pestanas = listOf("Cédula", "Historia", "Cápsulas")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-12).dp)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = LineDivider,
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
                                color = if (seleccionada) GoldAccent else MutedText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (seleccionada) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 12.dp)
                                        .fillMaxWidth(0.5f)
                                        .height(2.dp)
                                        .background(GoldAccent, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }

            // 4. CONTENIDO DE LA PESTAÑA ACTIVA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 40.dp)
            ) {
                when (tabSeleccionada) {
                    0 -> {
                        // PESTAÑA 1: CÉDULA
                        FilaCedula(
                            etiqueta = "ADQUIRIDA",
                            valor = Fechas.formatearLargo(pieza.fechaAdquisicion)
                        )
                        FilaCedula(
                            etiqueta = "PROCEDENCIA",
                            valor = pieza.procedencia ?: "Aún no registrada"
                        )
                        FilaCedula(
                            etiqueta = "UBICACIÓN",
                            valor = pieza.ubicacion ?: "Aún no registrada"
                        )
                        val valorFormateado = pieza.valorEstimadoCentavos?.let {
                            formatoMoneda.format(it / 100.0) + " MXN"
                        } ?: "No registrado"
                        FilaCedula(
                            etiqueta = "VALOR ESTIMADO",
                            valor = if (mostrarValor) valorFormateado else "•••••• MXN",
                            ultimo = true,
                            accionDerecha = {
                                TextButton(onClick = { mostrarValor = !mostrarValor }) {
                                    Text(
                                        text = if (mostrarValor) "Ocultar" else "Mostrar",
                                        color = GoldAccent,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        )
                    }

                    1 -> {
                        // PESTAÑA 2: HISTORIA (Cita con barra lateral dorada)
                        Spacer(Modifier.height(8.dp))
                        val textoHistoria = pieza.notaPersonal?.takeIf { it.isNotBlank() }
                            ?: "Cada pieza tiene una historia. Puedes escribir la suya al editar la cédula."
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawLine(
                                        color = GoldAccent,
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                                .padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = textoHistoria,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 22.sp,
                                    lineHeight = 30.sp,
                                    fontStyle = FontStyle.Normal
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Tu nota personal · ${Fechas.formatearCorto(pieza.fechaAdquisicion)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }

                    2 -> {
                        // PESTAÑA 3: CÁPSULAS
                        CapsulasSection(pieza.id, capsulas, ahora, vm, ocupado)
                    }
                }
            }
        }

        // 5. BARRA SUPERIOR FLOTANTE CON BOTONES CIRCULARES TRANSLÚCIDOS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonFlotanteCircular(
                descripcion = "Volver",
                onClick = volver
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonFlotanteCircular(
                    descripcion = "Editar cédula",
                    enabled = !ocupado,
                    onClick = { editar(pieza.id) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
                BotonFlotanteCircular(
                    descripcion = "Más opciones",
                    enabled = !ocupado,
                    onClick = { mostrarSheet = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // 6. BOTTOM SHEET DE OPCIONES (Mover a bodega, Destacar, Notificaciones, Eliminar)
    if (mostrarSheet) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSheet = false },
            containerColor = SheetSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 10.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LineDivider)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                AccionSheetItem(
                    texto = if (pieza.estado == EstadoPieza.EXHIBICION) "Mover a bodega" else "Poner en exhibición",
                    enabled = !ocupado,
                    onClick = {
                        mostrarSheet = false
                        vm.mover(pieza)
                    }
                )
                AccionSheetItem(
                    texto = if (pieza.destacada) "Quitar de destacadas" else "Destacar en Inicio",
                    enabled = !ocupado,
                    onClick = {
                        mostrarSheet = false
                        vm.destacar(pieza)
                    }
                )
                AccionSheetItem(
                    texto = "Editar cédula",
                    enabled = !ocupado,
                    onClick = {
                        mostrarSheet = false
                        editar(pieza.id)
                    }
                )
                AccionSheetItem(
                    texto = "Ajustes de notificaciones",
                    enabled = !ocupado,
                    onClick = {
                        mostrarSheet = false
                        contexto.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                        )
                    }
                )
                AccionSheetItem(
                    texto = "Eliminar pieza",
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
private fun BotonFlotanteCircular(
    descripcion: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    contenido: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color(0x990A0A0A),
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = descripcion }
    ) {
        Box(contentAlignment = Alignment.Center) {
            contenido()
        }
    }
}

@Composable
private fun ChipDetalle(texto: String, activo: Boolean) {
    val colorBorde = if (activo) GoldBorderActive else LineDivider
    val colorTexto = if (activo) GoldAccent else MutedText
    Box(
        modifier = Modifier
            .border(1.dp, colorBorde, RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = texto,
            color = colorTexto,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun FilaCedula(
    etiqueta: String,
    valor: String,
    ultimo: Boolean = false,
    accionDerecha: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = etiqueta,
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = valor,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            accionDerecha?.invoke()
        }
        if (!ultimo) {
            HorizontalDivider(color = LineDivider, thickness = 1.dp)
        }
    }
}

@Composable
private fun AccionSheetItem(
    texto: String,
    esPeligro: Boolean = false,
    ultimo: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 15.dp)
        ) {
            Text(
                text = texto,
                color = if (esPeligro) DangerRed else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
            )
        }
        if (!ultimo) {
            HorizontalDivider(color = LineDivider, thickness = 1.dp)
        }
    }
}
