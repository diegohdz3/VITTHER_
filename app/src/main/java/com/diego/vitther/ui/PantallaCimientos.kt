package com.diego.vitther.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diego.vitther.ui.theme.VITTHERTheme

/**
 * Pantalla provisional de la Semana 1: valida de punta a punta el tema, la
 * fuente serif, Room y el repositorio. En la Semana 2 la sustituye el Home
 * con la barra de navegación real.
 */
@Composable
fun PantallaCimientos(viewModel: MuseoViewModel = viewModel(factory = MuseoViewModel.Factory)) {
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    PantallaCimientosContenido(resumen)
}

@Composable
private fun PantallaCimientosContenido(resumen: ResumenMuseo) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "VITTHER",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                modifier = Modifier.width(96.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Tu museo. Tus cápsulas. Tu historia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(56.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DatoMuseo(resumen.totalPiezas.toString(), "piezas")
                Separador()
                DatoMuseo(resumen.totalFranquicias.toString(), "categorías")
                Separador()
                DatoMuseo(resumen.enBodega.toString(), "en bodega")
            }
            Spacer(Modifier.height(56.dp))
            Text(
                text = "Semana 1 · cimientos listos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DatoMuseo(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Separador() {
    VerticalDivider(
        modifier = Modifier.size(width = 1.dp, height = 28.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0C0C)
@Composable
private fun PreviewCimientos() {
    VITTHERTheme {
        PantallaCimientosContenido(
            ResumenMuseo(totalPiezas = 11, totalFranquicias = 7, enBodega = 5)
        )
    }
}
