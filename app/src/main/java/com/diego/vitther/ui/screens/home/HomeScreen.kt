package com.diego.vitther.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

@Composable
fun HomeRoute(
    onPiezaClick: (Long) -> Unit,
    onAbrirColeccion: () -> Unit = {},
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    HomeScreen(estado, onPiezaClick, onAbrirColeccion = onAbrirColeccion)
}

/** Pantalla independiente de Navigation; la integración decide qué hacer al tocar una pieza. */
@Composable
fun HomeScreen(
    estado: HomeUiState,
    onPiezaClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onAbrirColeccion: () -> Unit = {}
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val ancho = maxWidth
            val anchoTarjeta = (ancho * .64f).coerceIn(220.dp, 260.dp)
            val altoTarjeta = 330.dp
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 18.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Logo centrado arriba y debajo la frase "Tu museo, hoy." centrada
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VittherLogo(size = 54.dp)
                    Text(
                        text = buildAnnotatedString {
                            append("Tu museo, ")
                            withStyle(
                                SpanStyle(
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                append("hoy.")
                            }
                        },
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(14.dp))

                when (estado) {
                    HomeUiState.Cargando -> CircularProgressIndicator(Modifier.padding(48.dp))
                    HomeUiState.Error -> Text(
                        stringResource(R.string.home_error),
                        Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    is HomeUiState.Vacia -> {
                        Column(
                            Modifier.padding(horizontal = 32.dp, vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.home_vacia),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                stringResource(R.string.home_vacia_descripcion),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (estado.resumen.totalPiezas > 0) {
                            TiraEstadoMuseo(estado.resumen, onAbrirColeccion)
                        }
                        if (estado.diasHastaCapsula != null || estado.proximaCapsula != null) {
                            Spacer(Modifier.height(22.dp))
                            SeccionProximaCapsula(
                                dias = estado.diasHastaCapsula ?: estado.proximaCapsula?.diasRestantes ?: 0L,
                                info = estado.proximaCapsula,
                                onPiezaClick = onPiezaClick
                            )
                        }
                        estado.recuerdoHoy?.let { rec ->
                            Spacer(Modifier.height(22.dp))
                            SeccionUnDiaComoHoy(rec, onPiezaClick)
                        }
                    }
                    is HomeUiState.Contenido -> {
                        val piezas = estado.piezas
                        if (piezas.isNotEmpty()) {
                            val pager = rememberPagerState(pageCount = { piezas.size })
                            val scope = rememberCoroutineScope()
                            val actual by remember { derivedStateOf { pager.currentPage } }
                            val moviendo by remember { derivedStateOf { pager.isScrollInProgress } }

                            Box(Modifier.fillMaxWidth().height(348.dp), contentAlignment = Alignment.Center) {
                                HorizontalPager(
                                    state = pager,
                                    pageSize = PageSize.Fixed(anchoTarjeta),
                                    contentPadding = PaddingValues(horizontal = (ancho - anchoTarjeta) / 2),
                                    pageSpacing = 12.dp,
                                    beyondViewportPageCount = 2,
                                    key = { piezas[it].id },
                                    modifier = Modifier.fillMaxWidth()
                                ) { pagina ->
                                    val pieza = piezas[pagina]
                                    PiezaCarouselCard(
                                        foto = pieza.foto,
                                        nombre = pieza.nombre,
                                        enabled = pagina == actual && !moviendo,
                                        onClick = { onPiezaClick(pieza.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(altoTarjeta)
                                            .graphicsLayer {
                                                val offset = (pager.currentPage - pagina) + pager.currentPageOffsetFraction
                                                val distancia = offset.absoluteValue.coerceIn(0f, 2f)
                                                rotationY = offset.coerceIn(-1.5f, 1.5f) * -28f
                                                scaleX = 1f - .16f * distancia
                                                scaleY = 1f - .16f * distancia
                                                alpha = (1f - .45f * distancia).coerceAtLeast(.12f)
                                                cameraDistance = 18f * density
                                            }
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FlechaCarrusel(true, actual > 0 && !moviendo, {
                                        scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
                                    })
                                    FlechaCarrusel(false, actual < piezas.lastIndex && !moviendo, {
                                        scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(piezas.lastIndex)) }
                                    })
                                }
                            }

                            val central = piezas[actual.coerceIn(0, piezas.lastIndex)]
                            Column(
                                Modifier
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                                    .clickable { onPiezaClick(central.id) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = central.nombre,
                                    fontSize = 26.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = central.franquicia.uppercase(Locale.forLanguageTag("es-MX")),
                                    fontSize = 11.sp,
                                    letterSpacing = 2.4.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                central.linea?.takeIf { it.isNotBlank() }?.let { lineaTexto ->
                                    Text(
                                        text = lineaTexto,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (central.edicionLimitada) {
                                    val etiquetaEdicion = central.numeroEdicion?.takeIf { it.isNotBlank() }
                                        ?.let { "Edición limitada · $it" } ?: "Edición limitada"
                                    Text(
                                        etiquetaEdicion,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            IndicadoresCarrusel(
                                piezas.size,
                                actual.coerceIn(0, piezas.lastIndex),
                                Modifier.widthIn(max = ancho - 48.dp)
                            )

                            Spacer(Modifier.height(14.dp))
                            TiraEstadoMuseo(estado.resumen, onAbrirColeccion)

                            if (estado.diasHastaCapsula != null || estado.proximaCapsula != null) {
                                Spacer(Modifier.height(22.dp))
                                SeccionProximaCapsula(
                                    dias = estado.diasHastaCapsula ?: estado.proximaCapsula?.diasRestantes ?: 0L,
                                    info = estado.proximaCapsula,
                                    onPiezaClick = onPiezaClick
                                )
                            }

                            estado.recuerdoHoy?.let { rec ->
                                Spacer(Modifier.height(22.dp))
                                SeccionUnDiaComoHoy(rec, onPiezaClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TiraEstadoMuseo(resumen: ResumenMuseoModel, onAbrirColeccion: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF26231C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable { onAbrirColeccion() }
                .padding(vertical = 16.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricaMuseo("${resumen.totalPiezas}", "PIEZAS", Modifier.weight(1f))
            VerticalDivider(Modifier.height(34.dp), color = Color(0xFF26231C))
            MetricaMuseo("${resumen.enExhibicion}", "MUSEO", Modifier.weight(1f))
            VerticalDivider(Modifier.height(34.dp), color = Color(0xFF26231C))
            MetricaMuseo("${resumen.enBodega}", "BODEGA", Modifier.weight(1f))
            VerticalDivider(Modifier.height(34.dp), color = Color(0xFF26231C))
            MetricaMuseo("${resumen.totalFranquicias}", "FRANQ.", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricaMuseo(valor: String, etiqueta: String, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = valor,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = etiqueta,
            fontSize = 9.sp,
            letterSpacing = 1.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SeccionProximaCapsula(
    dias: Long,
    info: ProximaCapsulaHome?,
    onPiezaClick: (Long) -> Unit
) {
    val gold = MaterialTheme.colorScheme.primary
    val textoFallback = if (dias == 0L) stringResource(R.string.home_capsula_hoy)
    else pluralStringResource(R.plurals.home_capsula_dias, dias.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), dias)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "PRÓXIMA CÁPSULA",
            fontSize = 11.sp,
            letterSpacing = 2.4.sp,
            color = gold
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = info != null) { info?.let { onPiezaClick(it.piezaId) } },
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF141414),
            border = BorderStroke(1.dp, Color(0xFF26231C))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    val progreso = info?.progresoAnillo ?: 0.78f
                    Canvas(Modifier.fillMaxSize()) {
                        val grosor = 2.5.dp.toPx()
                        drawCircle(
                            color = Color(0xFF26231C),
                            style = Stroke(width = grosor)
                        )
                        drawArc(
                            color = gold,
                            startAngle = -90f,
                            sweepAngle = 360f * progreso,
                            useCenter = false,
                            style = Stroke(width = grosor, cap = StrokeCap.Round)
                        )
                    }
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF0A0A0A)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$dias",
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Serif,
                                color = gold
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val titulo = when {
                        dias == 0L -> "Lista para abrir hoy"
                        dias == 1L -> "Se abre en 1 día"
                        else -> "Se abre en $dias días"
                    }
                    Text(
                        text = titulo,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (info != null) "Sellada para ${info.nombrePieza}" else textoFallback,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "›",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SeccionUnDiaComoHoy(
    recuerdo: RecuerdoHoyHome,
    onPiezaClick: (Long) -> Unit
) {
    val gold = MaterialTheme.colorScheme.primary
    val borderGold = Color(0xFF5A4A22)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable { onPiezaClick(recuerdo.piezaId) },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "UN DÍA COMO HOY",
            fontSize = 11.sp,
            letterSpacing = 2.4.sp,
            color = gold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = borderGold,
                        startStart = Offset(0f, 4.dp.toPx()),
                        end = Offset(0f, size.height - 4.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "“${recuerdo.nota}”",
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 23.sp
            )
            Text(
                text = "${recuerdo.nombrePieza} · hace ${recuerdo.haceDias} días",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLine(
    color: Color,
    startStart: Offset,
    end: Offset,
    strokeWidth: Float
) {
    drawLine(color = color, start = startStart, end = end, strokeWidth = strokeWidth)
}

@Preview(showBackground = true, widthDp = 393, heightDp = 760)
@Composable
private fun HomePreview() {
    VITTHERTheme {
        HomeScreen(
            HomeUiState.Contenido(
                listOf(
                    PiezaHome(1, "Goku Ultra Instinct", "Dragon Ball", linea = "S.H. Figuarts", edicionLimitada = true, numeroEdicion = "0142/2000"),
                    PiezaHome(2, "Spider-Man Integrated Suit", "Marvel"),
                    PiezaHome(3, "Guts Berserker Armor", "Berserk")
                ),
                7,
                ResumenMuseoModel(6, 4, 2, 5, 4500000L),
                ProximaCapsulaHome(3, "Guts Berserker Armor", 7, 0.78f),
                RecuerdoHoyHome(1, "Goku Ultra Instinct", "Llegó antes de lo que esperaba. Ya tiene su lugar.", 28)
            ),
            {}
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 760)
@Composable
private fun HomeVaciaPreview() {
    VITTHERTheme { HomeScreen(HomeUiState.Vacia(), {}) }
}
