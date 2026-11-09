package com.diego.vitther.ui.screens.coleccion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.*

@Composable
fun ColeccionScreen(
    piezas: List<PiezaEntity>,
    vm: ColeccionViewModel,
    agregar: () -> Unit,
    abrir: (Long) -> Unit,
    categoria: String? = null,
    volver: (() -> Unit)? = null
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

    val visibles = piezas.filter {
        (it.estado == EstadoPieza.BODEGA) == bodega &&
            (franquiciaSeleccionada == null || it.franquicia == franquiciaSeleccionada) &&
            (it.nombre.contains(busqueda, true) || it.franquicia.contains(busqueda, true))
    }

    val gruposPorFranquicia = remember(visibles) {
        visibles.groupBy { it.franquicia }.toSortedMap()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Surface(
                onClick = agregar,
                modifier = Modifier
                    .size(54.dp)
                    .semantics { contentDescription = "Agregar pieza" },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = Color(0xFF0A0A0A),
                shadowElevation = 10.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF0A0A0A)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        if (franquiciaSeleccionada != null) {
            VistaDetalleFranquicia(
                franquicia = franquiciaSeleccionada!!,
                piezas = piezas.filter { it.franquicia == franquiciaSeleccionada },
                vm = vm,
                abrir = abrir,
                agregar = agregar,
                volver = {
                    if (categoria != null && volver != null) {
                        volver()
                    } else {
                        franquiciaSeleccionada = null
                    }
                },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header: "Colección" + botón de lupa
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Colección",
                        fontSize = 34.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = {
                            mostrarBusqueda = !mostrarBusqueda
                            if (!mostrarBusqueda) busqueda = ""
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF141414), CircleShape)
                            .border(1.dp, Color(0xFF26231C), CircleShape)
                            .semantics { contentDescription = "Buscar pieza o franquicia" }
                    ) {
                        val tint = if (mostrarBusqueda || busqueda.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                        Canvas(Modifier.size(16.dp)) {
                            val r = size.minDimension * 0.36f
                            val centerCircle = Offset(size.width * 0.42f, size.height * 0.42f)
                            drawCircle(tint, radius = r, center = centerCircle, style = Stroke(1.6.dp.toPx()))
                            drawLine(
                                tint,
                                Offset(size.width * 0.68f, size.height * 0.68f),
                                Offset(size.width * 0.92f, size.height * 0.92f),
                                strokeWidth = 1.6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Barra de búsqueda desplegable
                AnimatedVisibility(visible = mostrarBusqueda || busqueda.isNotBlank()) {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        singleLine = true,
                        placeholder = { Text("Buscar pieza o franquicia…") },
                        shape = RoundedCornerShape(999.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141414),
                            unfocusedContainerColor = Color(0xFF141414),
                            focusedBorderColor = Color(0xFF5A4A22),
                            unfocusedBorderColor = Color(0xFF26231C)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                }

                // Segmented tabs: Museo | Bodega
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    TabRepisa(
                        texto = "Museo",
                        activo = !bodega,
                        onClick = { bodega = false },
                        modifier = Modifier.weight(1f)
                    )
                    TabRepisa(
                        texto = "Bodega",
                        activo = bodega,
                        onClick = { bodega = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (visibles.isEmpty()) {
                    val titulo = when {
                        busqueda.isNotBlank() -> "Sin coincidencias"
                        bodega -> "Tu bodega está vacía"
                        else -> "Tu museo espera su primera pieza"
                    }
                    val descripcion = when {
                        busqueda.isNotBlank() -> "Prueba otro nombre o franquicia."
                        else -> "Registra una pieza y decide dónde conservarla."
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EstadoVacio(titulo, descripcion, if (busqueda.isBlank()) "Agregar pieza" else null, agregar)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 18.dp, bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(26.dp)
                    ) {
                        items(gruposPorFranquicia.entries.toList(), key = { it.key }) { (franquicia, grupo) ->
                            EstanteFranquicia(
                                franquicia = franquicia,
                                piezas = grupo,
                                vm = vm,
                                onVerFranquicia = { franquiciaSeleccionada = franquicia },
                                onAbrirPieza = abrir
                            )
                        }
                        item {
                            Text(
                                text = "${gruposPorFranquicia.size} FRANQUICIAS · ${visibles.size} PIEZAS",
                                fontSize = 11.sp,
                                letterSpacing = 2.1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabRepisa(
    texto: String,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = texto,
            fontSize = 13.sp,
            letterSpacing = 0.8.sp,
            color = if (activo) gold else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 11.dp)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (activo) 2.dp else 1.dp)
                .background(if (activo) gold else Color(0xFF26231C))
        )
    }
}

@Composable
private fun EstanteFranquicia(
    franquicia: String,
    piezas: List<PiezaEntity>,
    vm: ColeccionViewModel,
    onVerFranquicia: () -> Unit,
    onAbrirPieza: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Cabecera de repisa (franquicia + conteo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onVerFranquicia)
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = franquicia,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${piezas.size} ${if (piezas.size == 1) "pieza" else "piezas"} ›",
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(10.dp))

        // Figuras sobre el estante
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(piezas, key = { it.id }) { pieza ->
                PiezaEnRepisaCard(
                    pieza = pieza,
                    vm = vm,
                    onClick = { onAbrirPieza(pieza.id) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Línea física de repisa (gradiente dorado a oscuro)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF5A4A22),
                            Color(0xFF26231C),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun PiezaEnRepisaCard(
    pieza: PiezaEntity,
    vm: ColeccionViewModel,
    onClick: () -> Unit
) {
    val archivoFoto = vm.archivo(pieza.fotoPath)
    val paleta = rememberPaletaPieza(pieza.nombre)

    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF26231C), RoundedCornerShape(14.dp))
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
                                colors = listOf(paleta.first, paleta.second, Color(0xFF0A0A0A)),
                                radius = 220f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val inicial = pieza.nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
                    Text(
                        text = inicial,
                        fontSize = 44.sp,
                        fontFamily = FontFamily.Serif,
                        color = Color(0x28F3EFE6)
                    )
                }
            }

            // Sombra inferior sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000))
                        )
                    )
            )

            // Estrella dorada si está destacada en vitrina
            if (pieza.destacada) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(18.dp)
                        .background(Color(0xBF0A0A0A), CircleShape)
                        .border(1.dp, Color(0xFF5A4A22), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "★",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            text = pieza.nombre,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun VistaDetalleFranquicia(
    franquicia: String,
    piezas: List<PiezaEntity>,
    vm: ColeccionViewModel,
    abrir: (Long) -> Unit,
    agregar: () -> Unit,
    volver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
            text = "‹ Volver",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = volver)
                .padding(vertical = 6.dp)
        )
        Text(
            text = franquicia,
            fontSize = 30.sp,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )
        if (piezas.isEmpty()) {
            EstadoVacio(
                titulo = "Sin piezas en $franquicia",
                descripcion = "Agrega una pieza a esta franquicia.",
                accion = "Agregar pieza",
                onAccion = agregar
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(piezas, key = { it.id }) { pieza ->
                    val archivoFoto = vm.archivo(pieza.fotoPath)
                    val paleta = rememberPaletaPieza(pieza.nombre)
                    Surface(
                        onClick = { abrir(pieza.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF141414),
                        border = BorderStroke(1.dp, Color(0xFF26231C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 58.dp, height = 74.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFF26231C), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (archivoFoto != null) {
                                    FotoPieza(archivoFoto, pieza.nombre, Modifier.fillMaxSize())
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(paleta.first, paleta.second, Color(0xFF0A0A0A))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pieza.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
                                            fontSize = 26.sp,
                                            fontFamily = FontFamily.Serif,
                                            color = Color(0x38F3EFE6)
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = pieza.nombre,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (pieza.estado == EstadoPieza.BODEGA) "En bodega" else "En exhibición",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "›",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
