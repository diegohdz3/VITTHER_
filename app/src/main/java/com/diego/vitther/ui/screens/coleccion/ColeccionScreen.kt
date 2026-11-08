package com.diego.vitther.ui.screens.coleccion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.*

@Composable
fun ColeccionScreen(piezas: List<PiezaEntity>, vm: ColeccionViewModel,
    agregar: () -> Unit, abrir: (Long) -> Unit, categoria: String? = null, volver: (() -> Unit)? = null) {
    var bodega by rememberSaveable(categoria) {
        val deCategoria = piezas.filter { categoria == null || it.franquicia == categoria }
        mutableStateOf(categoria != null && deCategoria.isNotEmpty() &&
            deCategoria.all { it.estado == EstadoPieza.BODEGA })
    }
    var busqueda by rememberSaveable { mutableStateOf("") }
    val visibles = piezas.filter {
        (it.estado == EstadoPieza.BODEGA) == bodega &&
            (categoria == null || it.franquicia == categoria) &&
            (it.nombre.contains(busqueda, true) || it.franquicia.contains(busqueda, true))
    }
    Scaffold(floatingActionButton = {
        ExtendedFloatingActionButton(onClick = agregar,
            modifier = Modifier.semantics { contentDescription = "Agregar pieza" },
            text = { Text("Agregar pieza") },
            icon = { Text("+", style = MaterialTheme.typography.headlineSmall) })
    }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (categoria != null) {
                TextButton(onClick = { volver?.invoke() }) { Text("Volver a categorías") }
                Text(categoria, style = MaterialTheme.typography.headlineSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = !bodega, onClick = { bodega = false }, label = { Text("Museo") })
                FilterChip(selected = bodega, onClick = { bodega = true }, label = { Text("Bodega") })
            }
            OutlinedTextField(value = busqueda, onValueChange = { busqueda = it }, singleLine = true,
                label = { Text("Buscar pieza o franquicia") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            if (visibles.isEmpty()) {
                val titulo = when {
                    busqueda.isNotBlank() -> "Sin coincidencias"
                    categoria != null -> "Sin piezas en ${if (bodega) "bodega" else "exhibición"}"
                    bodega -> "Tu bodega está vacía"
                    else -> "Tu museo espera su primera pieza"
                }
                val descripcion = when {
                    busqueda.isNotBlank() -> "Prueba otro nombre o franquicia."
                    categoria != null -> "Cambia de pestaña o agrega una pieza a $categoria."
                    else -> "Registra una pieza y decide dónde conservarla."
                }
                EstadoVacio(titulo, descripcion, if (busqueda.isBlank()) "Agregar pieza" else null, agregar)
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visibles, key = { it.id }) { pieza ->
                    Card(modifier = Modifier.clickable { abrir(pieza.id) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = if (bodega)
                            MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surface)) {
                        FotoPieza(vm.archivo(pieza.fotoPath), pieza.nombre, Modifier.fillMaxWidth().aspectRatio(.85f))
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(pieza.nombre, style = MaterialTheme.typography.titleLarge)
                            Text(pieza.franquicia, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pieza.estado.etiqueta, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
