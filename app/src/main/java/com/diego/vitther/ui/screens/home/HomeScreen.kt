package com.diego.vitther.ui.screens.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diego.vitther.R
import com.diego.vitther.domain.model.ResumenMuseoModel
import com.diego.vitther.ui.components.*
import com.diego.vitther.ui.theme.VITTHERTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private val formatoFechaHome = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-MX"))

@Composable
fun HomeRoute(onPiezaClick: (Long) -> Unit, vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    HomeScreen(estado, onPiezaClick)
}

/** Pantalla independiente de Navigation; la integración decide qué hacer al tocar una pieza. */
@Composable
fun HomeScreen(estado: HomeUiState, onPiezaClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val fechaHoy = remember {
        LocalDate.now().format(formatoFechaHome).uppercase(Locale.forLanguageTag("es-MX"))
    }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val ancho = maxWidth
            val anchoTarjeta = (ancho * .68f).coerceAtMost(360.dp)
            val altoTarjeta = (maxHeight * .50f).coerceIn(220.dp, 440.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(fechaHoy, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.width(96.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                }
                val dias = when (estado) {
                    is HomeUiState.Contenido -> estado.diasHastaCapsula
                    is HomeUiState.Vacia -> estado.diasHastaCapsula
                    else -> null
                }
                if (dias != null) AvisoCapsula(dias)
                when (estado) {
                    HomeUiState.Cargando -> CircularProgressIndicator(Modifier.padding(48.dp))
                    HomeUiState.Error -> Text(stringResource(R.string.home_error), Modifier.padding(24.dp),
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    is HomeUiState.Vacia -> Column(Modifier.padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.home_vacia), style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center)
                        Text(stringResource(R.string.home_vacia_descripcion), textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        if (estado.resumen.totalPiezas > 0) {
                            TiraEstadoMuseo(estado.resumen)
                        }
                    }
                    is HomeUiState.Contenido -> {
                        val piezas = estado.piezas
                        if (piezas.isNotEmpty()) {
                            val pager = rememberPagerState(pageCount = { piezas.size })
                            val scope = rememberCoroutineScope()
                            val actual by remember { derivedStateOf { pager.currentPage } }
                            val moviendo by remember { derivedStateOf { pager.isScrollInProgress } }
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                HorizontalPager(
                                    state = pager, pageSize = PageSize.Fixed(anchoTarjeta),
                                    contentPadding = PaddingValues(horizontal = (ancho - anchoTarjeta) / 2),
                                    pageSpacing = 12.dp, beyondViewportPageCount = 2,
                                    key = { piezas[it].id }, modifier = Modifier.fillMaxWidth()
                                ) { pagina ->
                                    val pieza = piezas[pagina]
                                    PiezaCarouselCard(pieza.foto, pieza.nombre,
                                        enabled = pagina == actual && !moviendo,
                                        onClick = { onPiezaClick(pieza.id) },
                                        modifier = Modifier.fillMaxWidth().height(altoTarjeta).graphicsLayer {
                                            // Se lee en la fase de dibujo: el gesto no recompone la tarjeta.
                                            val offset = (pager.currentPage - pagina) + pager.currentPageOffsetFraction
                                            val distancia = offset.absoluteValue.coerceIn(0f, 2f)
                                            rotationY = offset.coerceIn(-1.5f, 1.5f) * -32f
                                            scaleX = 1f - .15f * distancia
                                            scaleY = 1f - .15f * distancia
                                            alpha = (1f - .42f * distancia).coerceAtLeast(.12f)
                                            cameraDistance = 18f * density
                                        })
                                }
                                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    FlechaCarrusel(true, actual > 0 && !moviendo, {
                                        scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
                                    })
                                    FlechaCarrusel(false, actual < piezas.lastIndex && !moviendo, {
                                        scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(piezas.lastIndex)) }
                                    })
                                }
                            }
                            val central = piezas[actual.coerceIn(0, piezas.lastIndex)]
                            Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(central.nombre, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                                Text(central.franquicia.uppercase(Locale.forLanguageTag("es-MX")),
                                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center)
                                central.linea?.takeIf { it.isNotBlank() }?.let { lineaTexto ->
                                    Text(lineaTexto, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                                }
                                if (central.edicionLimitada) {
                                    val etiquetaEdicion = central.numeroEdicion?.takeIf { it.isNotBlank() }
                                        ?.let { "Edición limitada · $it" } ?: "Edición limitada"
                                    Text(etiquetaEdicion, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary, textAlign = TextAlign.Center)
                                }
                                EstadoExhibidoPill()
                            }
                            IndicadoresCarrusel(piezas.size, actual.coerceIn(0, piezas.lastIndex), Modifier.widthIn(max = ancho - 48.dp))
                            if (estado.resumen.totalPiezas > 0) {
                                TiraEstadoMuseo(estado.resumen)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TiraEstadoMuseo(resumen: ResumenMuseoModel) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 14.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricaMuseo("${resumen.totalPiezas}", "PIEZAS", Modifier.weight(1f))
            VerticalDivider(Modifier.height(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MetricaMuseo("${resumen.enExhibicion}", "EXHIBIDAS", Modifier.weight(1f))
            VerticalDivider(Modifier.height(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MetricaMuseo("${resumen.enBodega}", "EN BODEGA", Modifier.weight(1f))
            VerticalDivider(Modifier.height(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MetricaMuseo("${resumen.totalFranquicias}", "FRANQUICIAS", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricaMuseo(valor: String, etiqueta: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(valor, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AvisoCapsula(dias: Long) {
    val texto = if (dias == 0L) stringResource(R.string.home_capsula_hoy)
        else pluralStringResource(R.plurals.home_capsula_dias, dias.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), dias)
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(Modifier.width(24.dp), color = MaterialTheme.colorScheme.outline)
        Text(texto, Modifier.weight(1f), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.width(24.dp), color = MaterialTheme.colorScheme.outline)
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 760)
@Composable
private fun HomePreview() {
    VITTHERTheme {
        HomeScreen(HomeUiState.Contenido(listOf(
            PiezaHome(1, "Goku Ultra Instinct", "Dragon Ball", linea = "S.H. Figuarts", edicionLimitada = true, numeroEdicion = "0142/2000"),
            PiezaHome(2, "Spider-Man Integrated Suit", "Marvel"),
            PiezaHome(3, "Guts Berserker Armor", "Berserk")
        ), 12, ResumenMuseoModel(12, 8, 4, 5, 4500000L)), {})
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 760)
@Composable
private fun HomeVaciaPreview() {
    VITTHERTheme { HomeScreen(HomeUiState.Vacia(), {}) }
}
