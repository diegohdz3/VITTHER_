package com.diego.vitther.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import java.io.File

@Composable
fun Emblema(indice: Int, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val trazo = Stroke(1.6.dp.toPx())
        when (indice) {
            0 -> drawPath(Path().apply {
                moveTo(w * .12f, h * .45f); lineTo(w * .5f, h * .12f)
                lineTo(w * .88f, h * .45f); lineTo(w * .88f, h * .88f)
                lineTo(w * .12f, h * .88f); close()
            }, color, style = trazo)
            1 -> for (x in listOf(.12f, .57f)) for (y in listOf(.12f, .57f)) {
                drawRect(color, Offset(w*x, h*y), androidx.compose.ui.geometry.Size(w*.3f,h*.3f), style = trazo)
            }
            2 -> {
                drawPath(Path().apply {
                    moveTo(w*.2f,h*.15f); lineTo(w*.5f,h*.72f); lineTo(w*.8f,h*.15f)
                },color,style=trazo)
                drawLine(color,Offset(w*.15f,h*.9f),Offset(w*.85f,h*.9f),trazo.width)
            }
            3 -> {
                drawCircle(color,w*.38f,style=trazo)
                drawPath(Path().apply {
                    moveTo(w*.5f,h*.25f); lineTo(w*.5f,h*.5f); lineTo(w*.7f,h*.6f)
                },color,style=trazo)
            }
            else -> {
                drawCircle(color,w*.18f,Offset(w*.5f,h*.3f),style=trazo)
                drawArc(color,180f,180f,false,Offset(w*.16f,h*.55f),
                    androidx.compose.ui.geometry.Size(w*.68f,h*.55f),style=trazo)
            }
        }
    }
}

@Composable
fun FotoPieza(archivo: File?, nombre: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = archivo, contentDescription = "Foto de $nombre",
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop,
        loading = { PlaceholderFoto() }, error = { PlaceholderFoto() }
    )
}

@Composable
private fun PlaceholderFoto() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Emblema(2, Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Sin fotografía", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EstadoVacio(titulo: String, descripcion: String, accion: String? = null, onAccion: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Emblema(2)
        Text(titulo, style = MaterialTheme.typography.headlineSmall)
        Text(descripcion, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (accion != null) Button(onClick = onAccion) { Text(accion) }
    }
}
