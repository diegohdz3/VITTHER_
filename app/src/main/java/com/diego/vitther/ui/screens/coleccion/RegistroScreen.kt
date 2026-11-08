package com.diego.vitther.ui.screens.coleccion

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.*
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.FotoPieza
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(pieza: PiezaEntity?, vm: ColeccionViewModel, ocupado: Boolean,
    franquiciaInicial: String? = null, volver: () -> Unit, guardado: (Long) -> Unit) {
    var nombre by rememberSaveable { mutableStateOf(pieza?.nombre ?: "") }
    var franquicia by rememberSaveable { mutableStateOf(pieza?.franquicia ?: franquiciaInicial.orEmpty()) }
    var linea by rememberSaveable { mutableStateOf(pieza?.linea ?: "") }
    var edicionLimitada by rememberSaveable { mutableStateOf(pieza?.edicionLimitada ?: false) }
    var numeroEdicion by rememberSaveable { mutableStateOf(pieza?.numeroEdicion ?: "") }
    var valorPesos by rememberSaveable {
        mutableStateOf(pieza?.valorEstimadoCentavos?.let { (it / 100).toString() } ?: "")
    }
    var fecha by rememberSaveable { mutableStateOf(pieza?.fechaAdquisicion ?: Fechas.desdeFecha(LocalDate.now())) }
    val fechaInicial = rememberSaveable { pieza?.fechaAdquisicion ?: Fechas.desdeFecha(LocalDate.now()) }
    var foto by rememberSaveable { mutableStateOf(pieza?.fotoPath) }
    var procedencia by rememberSaveable { mutableStateOf(pieza?.procedencia ?: "") }
    var ubicacion by rememberSaveable { mutableStateOf(pieza?.ubicacion ?: "") }
    var nota by rememberSaveable { mutableStateOf(pieza?.notaPersonal ?: "") }
    var detalles by rememberSaveable { mutableStateOf(pieza != null) }
    var validar by rememberSaveable { mutableStateOf(false) }
    var calendario by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    var uriCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var archivoCamara by remember { mutableStateOf<File?>(null) }
    var errorCamara by remember { mutableStateOf<String?>(null) }

    val sugerenciasApi by vm.sugerenciasRemotas.collectAsStateWithLifecycle()
    val buscandoApi by vm.buscandoApi.collectAsStateWithLifecycle()

    val contexto = LocalContext.current
    val errores = validarPieza(nombre, franquicia, fecha)
    val modificado = nombre != (pieza?.nombre ?: "") || franquicia != (pieza?.franquicia ?: "") ||
        linea != (pieza?.linea ?: "") || edicionLimitada != (pieza?.edicionLimitada ?: false) ||
        numeroEdicion != (pieza?.numeroEdicion ?: "") ||
        foto != pieza?.fotoPath || nota != (pieza?.notaPersonal ?: "") ||
        procedencia != (pieza?.procedencia ?: "") || ubicacion != (pieza?.ubicacion ?: "") ||
        fecha != fechaInicial
    val salir: () -> Unit = {
        if (!ocupado) {
            vm.limpiarSugerenciasRemotas()
            if (modificado) confirmarSalida = true else volver()
        }
    }
    BackHandler { salir() }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.importar(it) { ruta -> foto = ruta } }
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) uriCamara?.let { uri ->
            vm.importar(Uri.parse(uri)) { ruta ->
                foto = ruta
                archivoCamara?.delete()
                archivoCamara = null
            }
        } else {
            archivoCamara?.delete()
            archivoCamara = null
        }
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = salir, enabled = !ocupado) { Text("Volver") }
        Text(if (pieza == null) "Una nueva historia" else "Editar cédula", style = MaterialTheme.typography.headlineMedium)
        FotoPieza(vm.archivo(foto), nombre.ifBlank { "tu pieza" }, Modifier.fillMaxWidth().height(220.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(enabled = !ocupado, onClick = {
                selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Galería") }
            OutlinedButton(enabled = !ocupado, onClick = {
                try {
                    val carpeta = File(contexto.cacheDir, "camara").apply { mkdirs() }
                    val archivo = File.createTempFile("captura_", ".jpg", carpeta)
                    archivoCamara = archivo
                    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.files", archivo)
                    uriCamara = uri.toString()
                    camara.launch(uri)
                } catch (_: Exception) {
                    archivoCamara?.delete()
                    archivoCamara = null
                    errorCamara = "No se pudo abrir la cámara. Prueba con Galería."
                }
            }) { Text("Cámara") }
            if (foto != null) TextButton(enabled = !ocupado, onClick = { foto = null }) { Text("Quitar") }
        }
        errorCamara?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (ocupado || buscandoApi) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre *") }, singleLine = true,
            enabled = !ocupado, isError = validar && errores.nombre != null,
            supportingText = { if (validar) errores.nombre?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())

        // Integración con Servicio Externo (API REST Jikan v4) para autocompletar desde catálogo global
        OutlinedButton(
            enabled = !ocupado && !buscandoApi && nombre.isNotBlank(),
            onClick = { vm.buscarEnApiCatalogo(nombre) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (buscandoApi) "Consultando API de catálogo..." else "Autocompletar desde API de catálogo online")
        }

        if (sugerenciasApi.isNotEmpty()) {
            Card(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sugerencias de la API externa", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { vm.limpiarSugerenciasRemotas() }) { Text("Cerrar") }
                    }
                    sugerenciasApi.forEach { item ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().clickable {
                                nombre = item.nombre
                                if (franquicia.isBlank()) franquicia = item.franquicia
                                if (linea.isBlank()) linea = item.lineaSugerida
                                if (nota.isBlank()) nota = item.notaSugerida
                                detalles = true
                                vm.limpiarSugerenciasRemotas()
                            }
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.nombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("${item.franquicia} · ${item.lineaSugerida}", style = MaterialTheme.typography.bodySmall)
                                Text(item.notaSugerida, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(franquicia, { franquicia = it }, label = { Text("Franquicia *") }, singleLine = true,
            enabled = !ocupado, isError = validar && errores.franquicia != null,
            supportingText = { if (validar) errores.franquicia?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(enabled = !ocupado, onClick = { calendario = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Adquisición: ${Fechas.formatearCorto(fecha)}")
        }
        if (validar) errores.fechaAdquisicion?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = { detalles = !detalles }) { Text(if (detalles) "Ocultar detalles de coleccionista" else "Añadir detalles de coleccionista") }
        if (detalles) {
            OutlinedTextField(linea, { linea = it }, label = { Text("Línea o fabricante (ej. S.H. Figuarts, Kotobukiya)") },
                singleLine = true, enabled = !ocupado, modifier = Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Edición limitada / numerada", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = edicionLimitada, onCheckedChange = { edicionLimitada = it }, enabled = !ocupado)
            }
            if (edicionLimitada) {
                OutlinedTextField(numeroEdicion, { numeroEdicion = it }, label = { Text("Número de serie o tiraje (ej. 0142/2000)") },
                    singleLine = true, enabled = !ocupado, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = valorPesos,
                onValueChange = { nuevo -> valorPesos = nuevo.filter { it.isDigit() }.take(9) },
                label = { Text("Valor estimado (MXN)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(procedencia, { procedencia = it }, label = { Text("Procedencia") }, enabled = !ocupado, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ubicacion, { ubicacion = it }, label = { Text("Ubicación física") }, enabled = !ocupado, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nota, { nota = it }, label = { Text("¿Qué significa para ti?") }, enabled = !ocupado,
                minLines = 3, modifier = Modifier.fillMaxWidth())
        }
        Button(enabled = !ocupado, onClick = {
            validar = true
            if (!errores.hayErrores) {
                val centavos = valorPesos.toLongOrNull()?.times(100L)
                val base = pieza ?: PiezaEntity(nombre = nombre, franquicia = franquicia, fechaAdquisicion = fecha)
                vm.guardar(
                    base.copy(
                        nombre = nombre,
                        franquicia = franquicia,
                        linea = linea.ifBlank { null },
                        edicionLimitada = edicionLimitada,
                        numeroEdicion = if (edicionLimitada) numeroEdicion.ifBlank { null } else null,
                        valorEstimadoCentavos = centavos,
                        fechaAdquisicion = fecha,
                        fotoPath = foto,
                        procedencia = procedencia,
                        ubicacion = ubicacion,
                        notaPersonal = nota
                    ),
                    guardado
                )
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (pieza == null) "Incorporar al museo" else "Guardar cambios") }
        Text("* Datos esenciales. La foto y los detalles son opcionales.", style = MaterialTheme.typography.bodySmall)
    }
    if (confirmarSalida) AlertDialog(onDismissRequest = { confirmarSalida = false },
        title = { Text("¿Descartar cambios?") }, text = { Text("Los cambios de esta cédula no se han guardado.") },
        confirmButton = { TextButton(onClick = volver) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Seguir editando") } })
    if (calendario) {
        // Material DatePicker trabaja en UTC; la adquisición representa un día local.
        val dia = Instant.ofEpochMilli(fecha).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val estado = rememberDatePickerState(initialSelectedDateMillis = dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { calendario = false }, confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let {
                    fecha = Fechas.desdeFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                calendario = false
            }) { Text("Elegir") }
        }, dismissButton = { TextButton(onClick = { calendario = false }) { Text("Cancelar") } }) {
            DatePicker(state = estado)
        }
    }
}
