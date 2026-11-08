package com.diego.vitther.ui.screens.historia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.R
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.*
import com.diego.vitther.ui.theme.VITTHERTheme
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch

private val fechaHito = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-MX"))

@Composable
fun HistoriaScreen(
    hitos: List<Hito>, piezas: List<PiezaEntity>, archivo: (String?) -> File?,
    volver: () -> Unit, abrir: (Long) -> Unit
) {
    val ordenados = remember(hitos) { hitos.sortedWith(compareBy<Hito> { it.fecha }.thenBy { it.clave }) }
    val porId = remember(piezas) { piezas.associateBy { it.id } }
    val lista = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var calendario by remember { mutableStateOf(false) }
    val anios = remember(ordenados) { ordenados.map { Fechas.anioDe(it.fecha) }.distinct() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(state = lista, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
            item(key = "encabezado") { HistoryHeader(volver, { calendario = true }, anios.isNotEmpty()) }
            item(key = "estadisticas") {
                StatsCard(piezas)
                Spacer(Modifier.height(32.dp))
            }
            if (ordenados.isEmpty()) item(key = "vacio") {
                Text(stringResource(R.string.historia_vacia), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.historia_vacia_detalle), Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            itemsIndexed(ordenados, key = { _, hito -> hito.clave }) { indice, hito ->
                val pieza = porId[hito.piezaId]
                TimelineItem(hito, pieza?.fotoPath?.let(archivo), indice == 0,
                    indice == ordenados.lastIndex, { abrir(hito.piezaId) })
            }
        }
    }
    if (calendario) AlertDialog(onDismissRequest = { calendario = false },
        title = { Text(stringResource(R.string.historia_elegir_anio)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(anios.size) { indice ->
                    val anio = anios[indice]
                    TextButton(onClick = {
                        calendario = false
                        scope.launch { lista.animateScrollToItem(2 + ordenados.indexOfFirst { Fechas.anioDe(it.fecha) == anio }) }
                    }, modifier = Modifier.fillMaxWidth()) { Text(anio.toString()) }
                }
            }
        }, confirmButton = { TextButton(onClick = { calendario = false }) { Text(stringResource(R.string.historia_cerrar)) } })
}

@Composable
fun HistoryHeader(volver: () -> Unit, calendario: () -> Unit, calendarioDisponible: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HistoryIconButton("volver", stringResource(R.string.historia_volver), true, volver)
            HistoryIconButton("calendario", stringResource(R.string.historia_calendario), calendarioDisponible, calendario)
        }
        Text(stringResource(R.string.historia_titulo), Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.historia_subtitulo), Modifier.padding(top = 10.dp, bottom = 28.dp),
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatsCard(piezas: List<PiezaEntity>) {
    val primera = piezas.minOfOrNull { it.fechaAdquisicion }
    val inicio = primera?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
    val hoy = LocalDate.now()
    val anios = inicio?.let { ChronoUnit.YEARS.between(it, hoy).coerceAtLeast(0).toInt() } ?: 0
    val meses = inicio?.let { ChronoUnit.MONTHS.between(it, hoy).coerceAtLeast(0).toInt() } ?: 0
    val dias = inicio?.let { ChronoUnit.DAYS.between(it, hoy).coerceAtLeast(0).toInt() } ?: 0
    val tiempo = when {
        inicio == null -> stringResource(R.string.historia_sin_tiempo)
        anios > 0 -> pluralStringResource(R.plurals.historia_anios, anios, anios)
        meses > 0 -> pluralStringResource(R.plurals.historia_meses, meses, meses)
        else -> pluralStringResource(R.plurals.historia_dias, dias, dias)
    }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 20.dp)) {
            StatColumn("cubo", stringResource(R.string.historia_piezas), piezas.size.toString(), Modifier.weight(1f))
            VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
            StatColumn("capas", stringResource(R.string.historia_colecciones), piezas.map { it.franquicia }.distinct().size.toString(), Modifier.weight(1f))
            VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
            StatColumn("reloj", stringResource(R.string.historia_recorrido), tiempo, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatColumn(icono: String, etiqueta: String, valor: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HistoryGlyph(icono, Modifier.size(24.dp))
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    }
}

@Composable
fun TimelineItem(hito: Hito, foto: File?, primero: Boolean, ultimo: Boolean, abrir: () -> Unit) {
    val titulo = stringResource(when (hito.tipo) {
        TipoHito.ADQUISICION -> if (primero) R.string.historia_primera else R.string.historia_adquisicion
        TipoHito.REGISTRO -> R.string.historia_registro
        TipoHito.CAPSULA -> R.string.historia_capsula
    })
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(role = Role.Button, onClick = abrir)) {
        TimelineNode(primero, ultimo, Modifier.width(28.dp).fillMaxHeight())
        Row(Modifier.weight(1f).padding(start = 12.dp, bottom = 24.dp).heightIn(min = 92.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(Instant.ofEpochMilli(hito.fecha).atZone(ZoneId.systemDefault()).format(fechaHito),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                Text(hito.nombrePieza, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val shape = RoundedCornerShape(10.dp)
            SubcomposeAsyncImage(model = foto, contentDescription = stringResource(R.string.home_foto, hito.nombrePieza),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp, 60.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
                loading = { TimelinePlaceholder() }, error = { TimelinePlaceholder() })
            HistoryGlyph("siguiente", Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TimelinePlaceholder() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        HistoryGlyph("cubo", Modifier.size(22.dp))
    }
}

@Composable
fun TimelineNode(primero: Boolean, ultimo: Boolean, modifier: Modifier = Modifier) {
    val oro = MaterialTheme.colorScheme.outline
    val fondo = MaterialTheme.colorScheme.background
    Canvas(modifier) {
        val y = 12.dp.toPx()
        val centro = Offset(size.width / 2, y)
        if (!primero) drawLine(oro, Offset(centro.x, 0f), centro, 1.dp.toPx())
        if (!ultimo) drawLine(oro, centro, Offset(centro.x, size.height), 1.dp.toPx())
        drawCircle(fondo, 9.dp.toPx(), centro)
        if (primero) {
            drawCircle(oro, 10.dp.toPx(), centro, style = Stroke(1.dp.toPx()))
            drawCircle(oro, 5.dp.toPx(), centro)
        } else drawCircle(oro, 5.dp.toPx(), centro, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun HistoryIconButton(icono: String, descripcion: String, enabled: Boolean, accion: () -> Unit) {
    IconButton(onClick = accion, enabled = enabled, modifier = Modifier.size(48.dp).semantics { contentDescription = descripcion }) {
        HistoryGlyph(icono, Modifier.size(24.dp))
    }
}

@Composable
private fun HistoryGlyph(tipo: String, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val trazo = Stroke(1.3.dp.toPx())
        fun linea(x: Float, y: Float, xx: Float, yy: Float) = drawLine(color, Offset(w*x,h*y), Offset(w*xx,h*yy), trazo.width)
        when (tipo) {
            "volver", "siguiente" -> {
                val a = if (tipo == "volver") .65f else .35f
                val b = 1f-a
                linea(a,.2f,b,.5f); linea(b,.5f,a,.8f)
            }
            "reloj" -> {
                drawCircle(color, w*.4f, style = trazo)
                linea(.5f,.25f,.5f,.5f); linea(.5f,.5f,.7f,.6f)
            }
            "calendario" -> {
                drawRect(color, Offset(w*.15f,h*.2f), androidx.compose.ui.geometry.Size(w*.7f,h*.65f), style = trazo)
                linea(.15f,.4f,.85f,.4f); linea(.3f,.1f,.3f,.3f); linea(.7f,.1f,.7f,.3f)
            }
            else -> {
                drawPath(Path().apply {
                    moveTo(w*.1f,h*.3f); lineTo(w*.5f,h*.1f); lineTo(w*.9f,h*.3f)
                    lineTo(w*.5f,h*.5f); close()
                }, color, style = trazo)
                if (tipo == "capas") {
                    linea(.1f,.5f,.5f,.7f); linea(.5f,.7f,.9f,.5f)
                    linea(.1f,.7f,.5f,.9f); linea(.5f,.9f,.9f,.7f)
                } else {
                    linea(.1f,.3f,.1f,.7f); linea(.1f,.7f,.5f,.9f)
                    linea(.5f,.9f,.9f,.7f); linea(.9f,.7f,.9f,.3f); linea(.5f,.5f,.5f,.9f)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 800)
@Composable
private fun HistoriaPreview() {
    val fecha = Fechas.desdeFecha(LocalDate.of(2024, 3, 12))
    val piezas = listOf(PiezaEntity(id = 1, nombre = "Goku Ultra Instinct", franquicia = "Dragon Ball", fechaAdquisicion = fecha, fechaRegistro = fecha),
        PiezaEntity(id = 2, nombre = "Spider-Man Integrated Suit", franquicia = "Marvel", fechaAdquisicion = fecha + 86_400_000L, fechaRegistro = fecha + 86_400_000L))
    VITTHERTheme { HistoriaScreen(historiaDe(piezas, emptyList()), piezas, { null }, {}, {}) }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 800)
@Composable
private fun HistoriaVaciaPreview() {
    VITTHERTheme { HistoriaScreen(emptyList(), emptyList(), { null }, {}, {}) }
}
