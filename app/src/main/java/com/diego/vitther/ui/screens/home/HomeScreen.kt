package com.diego.vitther.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diego.vitther.R
import com.diego.vitther.domain.model.ResumenMuseoModel
import com.diego.vitther.ui.components.*
import com.diego.vitther.ui.theme.VITTHERTheme
import java.util.Locale
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private val GoldLouvre = Color(0xFFE8C97A)
private val MutedLouvre = Color(0xFF8D8777)
private val PlaqueBorderGold = Color(0xFF4A3D1C)
private val VioletLimited = Color(0xFFB9A3E6)

@Composable
fun HomeRoute(
    onPiezaClick: (Long) -> Unit,
    onAbrirColeccion: () -> Unit = {},
    onAgregarPieza: () -> Unit = {},
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    HomeScreen(
        estado = estado,
        onPiezaClick = onPiezaClick,
        onAbrirColeccion = onAbrirColeccion,
        onAgregarPieza = onAgregarPieza,
        mostrarNichoAgregar = true
    )
}

/**
 * Pantalla de Inicio con inspiración en el Museo del Louvre:
 * Cielo nocturno parisino, Pirámide de Cristal reticulada detrás del carrusel,
 * nichos arqueados iluminados, cartela de museo con filetes dorados e indicadores mini-nicho.
 */
@Composable
fun HomeScreen(
    estado: HomeUiState,
    onPiezaClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onAbrirColeccion: () -> Unit = {},
    onAgregarPieza: () -> Unit = {},
    mostrarNichoAgregar: Boolean = false
) {
    val louvreBackground = Brush.verticalGradient(
        0.0f to Color(0xFF0B1224),
        0.28f to Color(0xFF0A0D17),
        0.58f to Color(0xFF0A0A0A),
        1.0f to Color(0xFF0A0A0A)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(louvreBackground)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val ancho = maxWidth
            val anchoNicho = 260.dp
            val altoNicho = 390.dp

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 18.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. HEADER: Emblema VITTHER + "Tu museo, hoy."
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VittherLogo(size = 46.dp)
                    Text(
                        text = buildAnnotatedString {
                            append("Tu museo, ")
                            withStyle(
                                SpanStyle(
                                    fontStyle = FontStyle.Italic,
                                    color = GoldLouvre
                                )
                            ) {
                                append("hoy.")
                            }
                        },
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFFF2EAD8),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(6.dp))

                when (estado) {
                    HomeUiState.Cargando -> CircularProgressIndicator(
                        modifier = Modifier.padding(48.dp),
                        color = GoldLouvre
                    )

                    HomeUiState.Error -> Text(
                        text = stringResource(R.string.home_error),
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MutedLouvre
                    )

                    is HomeUiState.Vacia -> {
                        if (mostrarNichoAgregar) {
                            // Escenario Louvre con el nicho arqueado esperando la primera pieza
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(430.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                PiramideLouvreCanvas(
                                    modifier = Modifier
                                        .width(390.dp)
                                        .height(360.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 14.dp)
                                        .width(anchoNicho)
                                        .height(altoNicho)
                                ) {
                                    NichoAgregarCard(
                                        onClick = onAgregarPieza,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                BaseLuzNichoLouvre(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                )
                            }

                            PlacaMuseoLouvre(
                                franquicia = "TU PRÓXIMA PIEZA",
                                fabricante = null,
                                titulo = "Un nicho te espera",
                                ubicacion = "Toca para añadir",
                                edicionLimitada = false,
                                numeroEdicion = null,
                                onClick = onAgregarPieza
                            )

                            Spacer(Modifier.height(16.dp))
                            IndicadoresCarrusel(total = 1, actual = 0)
                        }

                        // Mensaje curatorial discreto (y compatible con pruebas de estado vacío)
                        Column(
                            Modifier.padding(horizontal = 32.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_vacia),
                                fontSize = if (mostrarNichoAgregar) 13.sp else 20.sp,
                                fontFamily = FontFamily.Serif,
                                color = if (mostrarNichoAgregar) MutedLouvre else Color(0xFFF2EAD8),
                                textAlign = TextAlign.Center
                            )
                            if (!mostrarNichoAgregar) {
                                Text(
                                    text = stringResource(R.string.home_vacia_descripcion),
                                    textAlign = TextAlign.Center,
                                    color = MutedLouvre,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        estado.diasHastaCapsula?.let { dias ->
                            AvisoCapsulaLouvre(dias)
                        }
                    }

                    is HomeUiState.Contenido -> {
                        val piezas = estado.piezas
                        val totalPaginas = piezas.size + if (mostrarNichoAgregar) 1 else 0
                        if (totalPaginas > 0) {
                            val pager = rememberPagerState(pageCount = { totalPaginas })
                            val scope = rememberCoroutineScope()
                            val actual by remember { derivedStateOf { pager.currentPage } }
                            val moviendo by remember { derivedStateOf { pager.isScrollInProgress } }

                            // 2. ESCENARIO LOUVRE: Pirámide de cristal + Carrusel de nichos arqueados + Reflejo de luz
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(430.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                PiramideLouvreCanvas(
                                    modifier = Modifier
                                        .width(390.dp)
                                        .height(360.dp)
                                )

                                HorizontalPager(
                                    state = pager,
                                    pageSize = PageSize.Fixed(anchoNicho),
                                    contentPadding = PaddingValues(
                                        start = (ancho - anchoNicho) / 2,
                                        end = (ancho - anchoNicho) / 2,
                                        top = 14.dp,
                                        bottom = 26.dp
                                    ),
                                    pageSpacing = 16.dp,
                                    beyondViewportPageCount = 2,
                                    key = { idx ->
                                        if (idx < piezas.size) "pieza_${piezas[idx].id}" else "nicho_agregar"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { pagina ->
                                    val modifierEscala = Modifier
                                        .width(anchoNicho)
                                        .height(altoNicho)
                                        .graphicsLayer {
                                            val offset = (pager.currentPage - pagina) + pager.currentPageOffsetFraction
                                            val distancia = offset.absoluteValue.coerceIn(0f, 1.5f)
                                            val escala = 1f - 0.10f * distancia.coerceAtMost(1f)
                                            scaleX = escala
                                            scaleY = escala
                                            translationY = if (distancia < 0.25f) (-4).dp.toPx() else 0f
                                            alpha = (1f - 0.62f * distancia.coerceAtMost(1f)).coerceAtLeast(0.38f)
                                        }

                                    if (pagina < piezas.size) {
                                        val pieza = piezas[pagina]
                                        PiezaCarouselCard(
                                            foto = pieza.foto,
                                            nombre = pieza.nombre,
                                            enabled = pagina == actual && !moviendo,
                                            onClick = { onPiezaClick(pieza.id) },
                                            modifier = modifierEscala
                                        )
                                    } else {
                                        NichoAgregarCard(
                                            enabled = pagina == actual && !moviendo,
                                            onClick = onAgregarPieza,
                                            modifier = modifierEscala
                                        )
                                    }
                                }

                                // Flechas laterales circulares (.arr)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 195.dp, start = 10.dp, end = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FlechaCarrusel(
                                        anterior = true,
                                        enabled = actual > 0 && !moviendo,
                                        onClick = {
                                            scope.launch {
                                                pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0))
                                            }
                                        }
                                    )
                                    FlechaCarrusel(
                                        anterior = false,
                                        enabled = actual < totalPaginas - 1 && !moviendo,
                                        onClick = {
                                            scope.launch {
                                                pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(totalPaginas - 1))
                                            }
                                        }
                                    )
                                }

                                // Línea base dorada (.base) y charco de luz reflejada (.pool)
                                BaseLuzNichoLouvre(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                )
                            }

                            // 3. PLACA / CARTELA DE MUSEO (.plq)
                            val indiceSeguro = actual.coerceIn(0, totalPaginas - 1)
                            if (indiceSeguro < piezas.size) {
                                val central = piezas[indiceSeguro]
                                PlacaMuseoLouvre(
                                    franquicia = central.franquicia,
                                    fabricante = central.linea,
                                    titulo = central.nombre,
                                    ubicacion = central.ubicacion?.takeIf { it.isNotBlank() } ?: "En exhibición",
                                    edicionLimitada = central.edicionLimitada,
                                    numeroEdicion = central.numeroEdicion,
                                    onClick = { onPiezaClick(central.id) }
                                )
                            } else {
                                PlacaMuseoLouvre(
                                    franquicia = "TU PRÓXIMA PIEZA",
                                    fabricante = null,
                                    titulo = "Un nicho te espera",
                                    ubicacion = "Toca para añadir",
                                    edicionLimitada = false,
                                    numeroEdicion = null,
                                    onClick = onAgregarPieza
                                )
                            }

                            // 4. INDICADORES MINI-NICHO (.dots)
                            Spacer(Modifier.height(16.dp))
                            IndicadoresCarrusel(
                                total = totalPaginas,
                                actual = indiceSeguro,
                                modifier = Modifier.widthIn(max = ancho - 48.dp)
                            )

                            estado.diasHastaCapsula?.let { dias ->
                                Spacer(Modifier.height(14.dp))
                                AvisoCapsulaLouvre(dias)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Pirámide de cristal del Louvre dibujada con retícula de rombos dorados (idéntica al SVG #pyr del HTML). */
@Composable
private fun PiramideLouvreCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sx = w / 390f
        val sy = h / 360f
        val strokeColor = GoldLouvre.copy(alpha = 0.16f)
        val strokeWidth = 0.85.dp.toPx()

        // Triángulo exterior: M195 10 L20 330 H370 Z
        val outerPath = Path().apply {
            moveTo(195f * sx, 10f * sy)
            lineTo(20f * sx, 330f * sy)
            lineTo(370f * sx, 330f * sy)
            close()
        }
        drawPath(outerPath, color = strokeColor, style = Stroke(width = strokeWidth))

        // Retícula interior de 8 divisiones diagonales cruzadas
        val n = 8
        for (i in 1 until n) {
            val t = i.toFloat() / n.toFloat()
            val baseX = (20f + 350f * t) * sx
            val baseY = 330f * sy

            val rightEdgeX = (195f + 175f * t) * sx
            val rightEdgeY = (10f + 320f * t) * sy
            drawLine(
                color = strokeColor,
                start = Offset(baseX, baseY),
                end = Offset(rightEdgeX, rightEdgeY),
                strokeWidth = strokeWidth
            )

            val leftEdgeX = (20f + 175f * t) * sx
            val leftEdgeY = (330f - 320f * t) * sy
            drawLine(
                color = strokeColor,
                start = Offset(baseX, baseY),
                end = Offset(leftEdgeX, leftEdgeY),
                strokeWidth = strokeWidth
            )
        }
    }
}

/** Línea de horizonte dorada (.base) y reflejo elíptico en el piso (.pool) debajo del nicho activo. */
@Composable
private fun BaseLuzNichoLouvre(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(34.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // .base (bottom: 24px, height: 1px)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color.Transparent,
                        0.25f to Color(0xFF6B5A2C),
                        0.75f to Color(0xFF6B5A2C),
                        1.0f to Color.Transparent
                    )
                )
        )
        // .pool (width: 230dp, height: 22dp, radial glow)
        Canvas(
            modifier = Modifier
                .width(230.dp)
                .height(22.dp)
                .align(Alignment.BottomCenter)
        ) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldLouvre.copy(alpha = 0.32f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, 0f),
                    radius = size.width * 0.52f
                )
            )
        }
    }
}

/** Cartela / Placa de museo (.plq) con filetes horizontales dorados arriba y abajo. */
@Composable
private fun PlacaMuseoLouvre(
    franquicia: String,
    fabricante: String?,
    titulo: String,
    ubicacion: String,
    edicionLimitada: Boolean,
    numeroEdicion: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = PlaqueBorderGold, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // .mk: Franquicia (y fabricante si existe)
            val textoFranquicia = franquicia.uppercase(Locale.forLanguageTag("es-MX"))
            Text(
                text = buildAnnotatedString {
                    append(textoFranquicia)
                    fabricante?.takeIf { it.isNotBlank() }?.let { fab ->
                        append(" · ")
                        withStyle(SpanStyle(color = Color(0xFFF2EAD8), fontWeight = FontWeight.Normal)) {
                            append(fab.uppercase(Locale.forLanguageTag("es-MX")))
                        }
                    }
                },
                fontSize = 11.sp,
                letterSpacing = 1.8.sp,
                color = MutedLouvre,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // h2: Nombre de la pieza
            Text(
                text = titulo,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF2EAD8),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            // .meta: Ubicación + etiqueta de edición limitada si aplica
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ubicacion,
                    fontSize = 12.sp,
                    color = MutedLouvre,
                    fontWeight = FontWeight.Light
                )
                if (edicionLimitada) {
                    val etiqueta = numeroEdicion?.takeIf { it.isNotBlank() }
                        ?.let { "Edición limitada · $it" } ?: "Edición limitada"
                    Box(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = VioletLimited.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(99.dp)
                            )
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = etiqueta,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = VioletLimited
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = PlaqueBorderGold, thickness = 1.dp)
    }
}

@Composable
private fun AvisoCapsulaLouvre(dias: Long) {
    val texto = if (dias == 0L) {
        stringResource(R.string.home_capsula_hoy)
    } else {
        pluralStringResource(
            R.plurals.home_capsula_dias,
            dias.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            dias
        )
    }
    Text(
        text = texto,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        color = MutedLouvre,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 780)
@Composable
private fun HomePreview() {
    VITTHERTheme {
        HomeScreen(
            estado = HomeUiState.Contenido(
                piezas = listOf(
                    PiezaHome(
                        id = 1,
                        nombre = "Guts Berserker Armor",
                        franquicia = "Berserk",
                        linea = "Threezero",
                        edicionLimitada = true,
                        ubicacion = "Estante 3 · lateral"
                    ),
                    PiezaHome(
                        id = 2,
                        nombre = "Link Tears of the Kingdom",
                        franquicia = "The Legend of Zelda",
                        linea = "Figma",
                        ubicacion = "Estante 1"
                    )
                ),
                diasHastaCapsula = 7,
                resumen = ResumenMuseoModel(6, 4, 2, 5, 4500000L)
            ),
            onPiezaClick = {},
            mostrarNichoAgregar = true
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 780)
@Composable
private fun HomeVaciaPreview() {
    VITTHERTheme {
        HomeScreen(
            estado = HomeUiState.Vacia(),
            onPiezaClick = {},
            mostrarNichoAgregar = true
        )
    }
}
