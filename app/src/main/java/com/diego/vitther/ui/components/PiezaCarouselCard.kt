package com.diego.vitther.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.R
import java.io.File

/** Coil resuelve el tamaño desde las restricciones de la tarjeta, no desde el original. */
@Composable
fun PiezaCarouselCard(
    foto: File?, nombre: String, enabled: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondo = MaterialTheme.colorScheme.background
    val descripcion = stringResource(R.string.home_foto, nombre)
    Card(
        onClick = onClick, enabled = enabled, modifier = modifier.semantics { contentDescription = descripcion },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = foto,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                loading = { PlaceholderCarrusel() }, error = { PlaceholderCarrusel() }
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to fondo.copy(alpha = .35f), .3f to fondo.copy(alpha = 0f),
                .7f to fondo.copy(alpha = 0f), 1f to fondo.copy(alpha = .8f)
            )))
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                0f to fondo.copy(alpha = .3f), .2f to fondo.copy(alpha = 0f),
                .8f to fondo.copy(alpha = 0f), 1f to fondo.copy(alpha = .3f)
            )))
        }
    }
}

@Composable
private fun PlaceholderCarrusel() {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
            Emblema(2, Modifier.size(52.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.home_sin_foto), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EstadoExhibidoPill(modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Text(stringResource(R.string.home_exhibido), Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
