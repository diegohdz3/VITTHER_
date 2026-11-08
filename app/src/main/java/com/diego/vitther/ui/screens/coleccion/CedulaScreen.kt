package com.diego.vitther.ui.screens.coleccion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.*
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.*

@Composable
fun CedulaScreen(pieza: PiezaEntity?, vm: ColeccionViewModel, ocupado: Boolean,
    capsulas: List<CapsulaVista>, ahora: Long,
    volver: () -> Unit, editar: (Long) -> Unit, eliminado: () -> Unit) {
    if (pieza == null) {
        EstadoVacio("Pieza no disponible", "Esta pieza ya no pertenece a tu colección.", "Volver", volver)
        return
    }
    var confirmar by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        TextButton(onClick = volver) { Text("Volver") }
        FotoPieza(vm.archivo(pieza.fotoPath), pieza.nombre, Modifier.fillMaxWidth().height(320.dp))
        Text(pieza.nombre, style = MaterialTheme.typography.headlineMedium)
        Text(pieza.franquicia, color = MaterialTheme.colorScheme.primary)
        Text(pieza.estado.etiqueta, style = MaterialTheme.typography.labelLarge)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Dato("Adquirida", Fechas.formatearLargo(pieza.fechaAdquisicion))
        Dato("Procedencia", pieza.procedencia ?: "Aún no registrada")
        Dato("Ubicación", pieza.ubicacion ?: "Aún no registrada")
        Dato("Su historia", pieza.notaPersonal ?: "Cada pieza tiene una historia. Puedes escribir la suya al editar la cédula.")
        OutlinedButton(enabled = !ocupado, onClick = { vm.mover(pieza) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (pieza.estado == EstadoPieza.EXHIBICION) "Mover a bodega" else "Poner en exhibición")
        }
        OutlinedButton(enabled = !ocupado, onClick = { vm.destacar(pieza) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (pieza.destacada) "Quitar de destacadas" else "Destacar en Inicio")
        }
        if (pieza.destacada && pieza.estado == EstadoPieza.BODEGA) Text(
            "Conserva su marca de destacada; aparecerá en Inicio al volver a exhibición.", style = MaterialTheme.typography.bodySmall)
        Button(enabled = !ocupado, onClick = { editar(pieza.id) }, modifier = Modifier.fillMaxWidth()) { Text("Editar cédula") }
        CapsulasSection(pieza.id, capsulas, ahora, vm, ocupado)
        TextButton(enabled = !ocupado, onClick = { confirmar = true }) {
            Text("Eliminar pieza", color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmar) AlertDialog(onDismissRequest = { if (!ocupado) confirmar = false },
        title = { Text("¿Eliminar ${pieza.nombre}?") },
        text = { Text("Se eliminarán la pieza y todas sus cápsulas de tiempo. Esta acción no se puede deshacer.") },
        confirmButton = { TextButton(enabled = !ocupado, onClick = { vm.eliminar(pieza, eliminado) }) { Text("Eliminar") } },
        dismissButton = { TextButton(enabled = !ocupado, onClick = { confirmar = false }) { Text("Conservar") } })
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
