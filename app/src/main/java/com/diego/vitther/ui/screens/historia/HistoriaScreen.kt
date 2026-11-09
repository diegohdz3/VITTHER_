package com.diego.vitther.ui.screens.historia

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.R
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.domain.*
import com.diego.vitther.ui.components.rememberPaletaPieza
import com.diego.vitther.ui.theme.VITTHERTheme
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

private val formatoDiaMes = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX"))

private data class EventoTimeline(
    val idUnico: String,
    val seccion: String, // "Próximamente" o el año ("2026", "2025"...)
    val textoFecha: String,
    val nombrePieza: String,
    val etiqueta: String,
    val cita: String? = null,
    val esCapsula: Boolean = false,
    val esFuturo: Boolean = false,
    val esPrimeraPieza: Boolean = false,
    val piezaId: Long? = null,
    val fotoPath: String? = null,
    val ordenMillis: Long
)

@Composable
fun HistoriaScreen(
    hitos: List<Hito>,
    piezas: List<PiezaEntity>,
    archivo: (String?) -> File?,
    volver: () -> Unit,
    abrir: (Long) -> Unit,
    capsulas: List<CapsulaVista> = emptyList(),
    wishes: List<WishEntity> = emptyList(),
    ahora: Long = System.currentTimeMillis()
) {
    var filtro by rememberSaveable { mutableStateOf("todo") } // "todo" | "pieza" | "capsula"

    val porId = remember(piezas) { piezas.associateBy { it.id } }
    val idPrimeraPieza = remember(piezas) { piezas.minByOrNull { it.fechaAdquisicion }?.id }

    val todosLosEventos = remember(hitos, piezas, capsulas, wishes, ahora) {
        val lista = mutableListOf<EventoTimeline>()
        val zona = ZoneId.systemDefault()

        // 1. Eventos futuros ("Próximamente"): Cápsulas selladas por abrirse
        capsulas.filter { it.abiertaEn == null }.forEach { cap ->
            val pieza = porId[cap.piezaId]
            if (pieza != null) {
                val dias = TimeUnit.MILLISECONDS.toDays((cap.fechaApertura - ahora).coerceAtLeast(0L))
                val fechaTxt = Instant.ofEpochMilli(cap.fechaApertura).atZone(zona).format(formatoDiaMes)
                val prefijo = when {
                    dias == 0L -> "Hoy · $fechaTxt"
                    dias == 1L -> "En 1 día · $fechaTxt"
                    else -> "En $dias días · $fechaTxt"
                }
                lista.add(
                    EventoTimeline(
                        idUnico = "fut-cap-${cap.id}",
                        seccion = "Próximamente",
                        textoFecha = prefijo,
                        nombrePieza = pieza.nombre,
                        etiqueta = "Se abre una cápsula",
                        esCapsula = true,
                        esFuturo = true,
                        piezaId = pieza.id,
                        fotoPath = pieza.fotoPath,
                        ordenMillis = cap.fechaApertura
                    )
                )
            }
        }

        // 2. Eventos futuros ("Próximamente"): Wishes en camino (POR_LLEGAR)
        wishes.filter { it.estado == EstadoWish.POR_LLEGAR }.forEach { wish ->
            val llegada = wish.fechaEstimadaLlegada ?: (ahora + TimeUnit.DAYS.toMillis(7))
            val dias = TimeUnit.MILLISECONDS.toDays((llegada - ahora).coerceAtLeast(0L))
            val fechaTxt = Instant.ofEpochMilli(llegada).atZone(zona).format(formatoDiaMes)
            val prefijo = when {
                dias == 0L -> "Hoy · $fechaTxt"
                dias == 1L -> "En 1 día · $fechaTxt"
                else -> "En $dias días · $fechaTxt"
            }
            lista.add(
                EventoTimeline(
                    idUnico = "fut-wish-${wish.id}",
                    seccion = "Próximamente",
                    textoFecha = prefijo,
                    nombrePieza = wish.nombre,
                    etiqueta = "Llega desde Wishes",
                    esCapsula = false,
                    esFuturo = true,
                    piezaId = null,
                    fotoPath = null,
                    ordenMillis = llegada
                )
            )
        }

        // 3. Eventos pasados derivados de hitos y cápsulas
        val capsulasPorId = capsulas.associateBy { "capsula-${it.id}" }
        hitos.forEach { hito ->
            val pieza = porId[hito.piezaId]
            val fechaZoned = Instant.ofEpochMilli(hito.fecha).atZone(zona)
            val anio = fechaZoned.year.toString()
            val diaMes = fechaZoned.format(formatoDiaMes)
            val esPrimera = hito.tipo == TipoHito.ADQUISICION && hito.piezaId == idPrimeraPieza
            val capVista = capsulasPorId[hito.clave]

            val etiqueta = when (hito.tipo) {
                TipoHito.ADQUISICION -> {
                    if (esPrimera) "Tu primera pieza"
                    else if (pieza?.procedencia?.contains("preventa", ignoreCase = true) == true) "Adquirida · preventa"
                    else "Adquirida"
                }
                TipoHito.REGISTRO -> "Registrada en el museo"
                TipoHito.CAPSULA -> "Cápsula revelada"
            }

            lista.add(
                EventoTimeline(
                    idUnico = hito.clave,
                    seccion = anio,
                    textoFecha = diaMes,
                    nombrePieza = hito.nombrePieza,
                    etiqueta = etiqueta,
                    cita = capVista?.nota ?: (if (esPrimera) pieza?.notaPersonal else null),
                    esCapsula = hito.tipo == TipoHito.CAPSULA,
                    esFuturo = false,
                    esPrimeraPieza = esPrimera,
                    piezaId = hito.piezaId,
                    fotoPath = pieza?.fotoPath,
                    ordenMillis = hito.fecha
                )
            )
        }

        // También mostrar cápsulas selladas en su año de actividad si aún no se abren
        capsulas.filter { it.abiertaEn == null }.forEach { cap ->
            val pieza = porId[cap.piezaId]
            if (pieza != null) {
                val fechaSello = pieza.fechaRegistro
                val fechaZoned = Instant.ofEpochMilli(fechaSello).atZone(zona)
                lista.add(
                    EventoTimeline(
                        idUnico = "sellada-${cap.id}",
                        seccion = fechaZoned.year.toString(),
                        textoFecha = fechaZoned.format(formatoDiaMes),
                        nombrePieza = pieza.nombre,
                        etiqueta = "Cápsula sellada",
                        esCapsula = true,
                        esFuturo = false,
                        piezaId = pieza.id,
                        fotoPath = pieza.fotoPath,
                        ordenMillis = fechaSello + 1L
                    )
                )
            }
        }

        val futuros = lista.filter { it.esFuturo }.sortedBy { it.ordenMillis }
        val pasados = lista.filter { !it.esFuturo }.sortedByDescending { it.ordenMillis }
        futuros + pasados
    }

    val filtrados = remember(todosLosEventos, filtro) {
        todosLosEventos.filter { ev ->
            when (filtro) {
                "pieza" -> !ev.esCapsula
                "capsula" -> ev.esCapsula
                else -> true
            }
        }
    }

    val gruposPorSeccion = remember(filtrados) {
        filtrados.groupBy { it.seccion }
    }

    val primeraFecha = piezas.minOfOrNull { it.fechaAdquisicion }
    val inicio = primeraFecha?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
    val hoy = LocalDate.now()
    val aniosRecorrido = inicio?.let { ChronoUnit.YEARS.between(it, hoy).coerceAtLeast(1).toInt() } ?: 1
    val textoRecorrido = pluralStringResource(R.plurals.historia_anios, aniosRecorrido, aniosRecorrido)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            // Cabecera "Historia"
            Text(
                text = "Historia",
                fontSize = 34.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp)
            )

            // Chips de filtro: Todo | Piezas | Cápsulas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipFiltroHistoria("Todo", filtro == "todo") { filtro = "todo" }
                ChipFiltroHistoria("Piezas", filtro == "pieza") { filtro = "pieza" }
                ChipFiltroHistoria("Cápsulas", filtro == "capsula") { filtro = "capsula" }
            }

            if (filtrados.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp)
                ) {
                    Text(stringResource(R.string.historia_vacia), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.historia_vacia_detalle),
                        Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    gruposPorSeccion.forEach { (seccion, eventos) ->
                        item(key = "sec-$seccion") {
                            EncabezadoAnioHistoria(seccion)
                        }
                        itemsIndexed(eventos, key = { _, ev -> ev.idUnico }) { idx, ev ->
                            FilaEventoHistoria(
                                evento = ev,
                                archivoFoto = ev.fotoPath?.let(archivo),
                                esPrimeroSeccion = idx == 0,
                                esUltimoSeccion = idx == eventos.lastIndex,
                                onClick = { ev.piezaId?.let(abrir) }
                            )
                        }
                    }

                    item(key = "footer") {
                        val totalFranquicias = piezas.map { it.franquicia }.distinct().size
                        Text(
                            text = "${piezas.size} PIEZAS · $totalFranquicias FRANQUICIAS · ${textoRecorrido.uppercase(Locale.forLanguageTag("es-MX"))} DE RECORRIDO",
                            fontSize = 11.sp,
                            letterSpacing = 1.4.sp,
                            color = Color(0xFF8D8777),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipFiltroHistoria(
    texto: String,
    activo: Boolean,
    onClick: () -> Unit
) {
    val gold = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (activo) Color(0xFF5A4A22) else Color(0xFF26231C))
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            color = if (activo) gold else Color(0xFF8D8777),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun EncabezadoAnioHistoria(titulo: String) {
    val gold = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 34.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = titulo.uppercase(Locale.forLanguageTag("es-MX")),
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            letterSpacing = 2.8.sp,
            color = gold
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF5A4A22), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun FilaEventoHistoria(
    evento: EventoTimeline,
    archivoFoto: File?,
    esPrimeroSeccion: Boolean,
    esUltimoSeccion: Boolean,
    onClick: () -> Unit
) {
    val gold = MaterialTheme.colorScheme.primary
    val paleta = rememberPaletaPieza(evento.nombrePieza)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(enabled = evento.piezaId != null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna de línea de tiempo + nodo circular (23.dp)
        Box(
            modifier = Modifier
                .width(34.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(Modifier.matchParentSize()) {
                val x = 11.5.dp.toPx()
                val topY = if (esPrimeroSeccion) size.height * 0.5f else 0f
                val bottomY = if (esUltimoSeccion) size.height * 0.5f else size.height
                drawLine(
                    brush = Brush.verticalGradient(listOf(gold, Color(0xFF4A3D1C))),
                    start = Offset(x, topY),
                    end = Offset(x, bottomY),
                    strokeWidth = 1.dp.toPx()
                )
            }
            NodoCirculoHistoria(
                esCapsula = evento.esCapsula,
                esFuturo = evento.esFuturo,
                esPrimera = evento.esPrimeraPieza
            )
        }

        // Cuerpo de texto central
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = evento.textoFecha,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = Color(0xFF8D8777)
            )
            Text(
                text = evento.nombrePieza,
                fontSize = 19.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = evento.etiqueta,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = if (evento.esFuturo) Color(0xFF8D8777) else gold
            )
            evento.cita?.takeIf { it.isNotBlank() }?.let { textoCita ->
                Text(
                    text = "“$textoCita”",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF8D8777),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Miniatura derecha (52x64.dp)
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 64.dp)
                .alpha(if (evento.esFuturo) 0.55f else 1f)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF26231C), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (archivoFoto != null) {
                SubcomposeAsyncImage(
                    model = archivoFoto,
                    contentDescription = stringResource(R.string.home_foto, evento.nombrePieza),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(paleta.first, paleta.second, Color(0xFF17140E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = evento.nombrePieza.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Serif,
                        color = Color(0x55F2EAD8)
                    )
                }
            }
        }
    }
}

@Composable
private fun NodoCirculoHistoria(
    esCapsula: Boolean,
    esFuturo: Boolean,
    esPrimera: Boolean
) {
    val gold = MaterialTheme.colorScheme.primary
    val bg = when {
        esPrimera -> gold
        esCapsula && !esFuturo -> Color(0xFF1C1708)
        else -> Color(0xFF0A0A0A)
    }
    val iconColor = when {
        esPrimera -> Color(0xFF1A1405)
        esFuturo -> Color(0xFF8D8777)
        else -> gold
    }

    Box(
        modifier = Modifier
            .size(23.dp)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = if (esFuturo) {
                Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                )
            } else {
                Stroke(width = 1.dp.toPx())
            }
            drawCircle(
                color = if (esFuturo) Color(0xFF5A4A22) else gold,
                style = stroke
            )
        }
        Canvas(Modifier.size(12.dp)) {
            val w = size.width
            val h = size.height
            val trazo = Stroke(1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (esCapsula) {
                // Reloj de arena (cápsula)
                drawLine(iconColor, Offset(w * 0.25f, h * 0.12f), Offset(w * 0.75f, h * 0.12f), trazo.width)
                drawLine(iconColor, Offset(w * 0.25f, h * 0.88f), Offset(w * 0.75f, h * 0.88f), trazo.width)
                drawPath(
                    Path().apply {
                        moveTo(w * 0.30f, h * 0.12f)
                        lineTo(w * 0.70f, h * 0.88f)
                        moveTo(w * 0.70f, h * 0.12f)
                        lineTo(w * 0.30f, h * 0.88f)
                    },
                    color = iconColor,
                    style = trazo
                )
            } else {
                // Cubo isométrico (pieza)
                drawPath(
                    Path().apply {
                        moveTo(w * 0.50f, h * 0.10f)
                        lineTo(w * 0.86f, h * 0.30f)
                        lineTo(w * 0.86f, h * 0.70f)
                        lineTo(w * 0.50f, h * 0.90f)
                        lineTo(w * 0.14f, h * 0.70f)
                        lineTo(w * 0.14f, h * 0.30f)
                        close()
                    },
                    color = iconColor,
                    style = trazo
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 800)
@Composable
private fun HistoriaPreview() {
    val fecha = Fechas.desdeFecha(LocalDate.of(2025, 9, 9))
    val piezas = listOf(
        PiezaEntity(id = 1, nombre = "Goku Ultra Instinct", franquicia = "Dragon Ball", fechaAdquisicion = fecha, fechaRegistro = fecha),
        PiezaEntity(id = 2, nombre = "Spider-Man Integrated Suit", franquicia = "Marvel", fechaAdquisicion = fecha + 86_400_000L * 120, fechaRegistro = fecha + 86_400_000L * 120)
    )
    VITTHERTheme { HistoriaScreen(historiaDe(piezas, emptyList()), piezas, { null }, {}, {}) }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 800)
@Composable
private fun HistoriaVaciaPreview() {
    VITTHERTheme { HistoriaScreen(emptyList(), emptyList(), { null }, {}, {}) }
}
