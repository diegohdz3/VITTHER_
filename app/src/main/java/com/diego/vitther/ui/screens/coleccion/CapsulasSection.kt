package com.diego.vitther.ui.screens.coleccion

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.diego.vitther.data.notifications.CANAL_CAPSULAS
import com.diego.vitther.data.notifications.revisarAvisos
import com.diego.vitther.domain.*
import com.diego.vitther.ui.ColeccionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsulasSection(piezaId: Long, capsulas: List<CapsulaVista>, ahora: Long,
    vm: ColeccionViewModel, ocupado: Boolean) {
    var redactar by rememberSaveable(piezaId) { mutableStateOf(false) }
    // rememberSaveable: girar el teléfono mientras se escribe una nota larga
    // no debe borrar el borrador.
    var titulo by rememberSaveable(piezaId) { mutableStateOf("") }
    var nota by rememberSaveable(piezaId) { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf<Long?>(null) }
    var calendario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf(false) }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Text("Cápsulas de tiempo", style = MaterialTheme.typography.headlineSmall)
    Text("Un mensaje para tu yo del futuro. Sellarlo es opcional.", style = MaterialTheme.typography.bodyMedium)
    capsulas.forEach { capsula ->
        val estado = estadoDeCapsula(capsula.abiertaEn, capsula.fechaApertura, ahora)
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(capsula.titulo ?: "Un recuerdo para el futuro", style = MaterialTheme.typography.titleLarge)
                Text("Apertura: ${Fechas.formatearLargo(capsula.fechaApertura)}", style = MaterialTheme.typography.bodySmall)
                when (estado) {
                    is EstadoCapsula.Sellada -> {
                        val dias = estado.diasRestantes
                        val texto = when {
                            dias <= 0L -> "Sellada · se abre hoy"
                            dias == 1L -> "Sellada · 1 día restante"
                            else -> "Sellada · $dias días restantes"
                        }
                        Text(texto, color = MaterialTheme.colorScheme.tertiary)
                    }
                    EstadoCapsula.Disponible -> Button(enabled = !ocupado, onClick = { vm.abrirCapsula(capsula.id) }) { Text("Revelar cápsula") }
                    EstadoCapsula.Abierta -> {
                        Text("Revelada", color = MaterialTheme.colorScheme.primary)
                        Text(capsula.nota.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
    if (!redactar) OutlinedButton(enabled = !ocupado, onClick = { redactar = true; mensaje = null }) { Text("Crear cápsula") }
    else {
        OutlinedTextField(titulo, { titulo = it.take(80) }, label = { Text("Título de cápsula (opcional)") },
            enabled = !ocupado, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(nota, { nota = it.take(5000) }, label = { Text("Mensaje para tu yo del futuro") },
            minLines = 3, enabled = !ocupado, modifier = Modifier.fillMaxWidth())
        OutlinedButton(enabled = !ocupado, onClick = { calendario = true }) {
            Text(fecha?.let { "Abrir el ${Fechas.formatearCorto(it)}" } ?: "Elegir fecha de apertura")
        }
        Text("Se abrirá al comenzar el día elegido. Una vez sellada no podrás editar la nota ni su fecha.",
            style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !ocupado, onClick = {
            error = when {
                nota.isBlank() -> "Escribe un mensaje antes de sellarlo."
                fecha == null || fecha!! <= ahora -> "Elige una fecha futura."
                else -> null
            }
            if (error == null) confirmar = true
        }) { Text("Sellar cápsula") }
        TextButton(enabled = !ocupado, onClick = { redactar = false; nota = ""; titulo = ""; fecha = null }) { Text("Cancelar cápsula") }
    }
    mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    AvisosCapsulas()
    if (confirmar) AlertDialog(onDismissRequest = { if (!ocupado) confirmar = false },
        title = { Text("¿Sellar este recuerdo?") },
        text = { Text("El mensaje quedará oculto hasta ${Fechas.formatearLargo(fecha!!)}.") },
        confirmButton = { TextButton(enabled = !ocupado, onClick = {
            vm.sellar(piezaId, titulo, nota, fecha!!) {
                confirmar = false; redactar = false; nota = ""; titulo = ""; fecha = null
                mensaje = "Tu cápsula quedó sellada."
            }
        }) { Text("Confirmar sellado") } },
        dismissButton = { TextButton(enabled = !ocupado, onClick = { confirmar = false }) { Text("Seguir escribiendo") } })
    if (calendario) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.now())
        })
        DatePickerDialog(onDismissRequest = { calendario = false }, confirmButton = {
            TextButton(enabled = estado.selectedDateMillis != null, onClick = {
                fecha = estado.selectedDateMillis?.let { Fechas.desdeFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                calendario = false
            }) { Text("Elegir apertura") }
        }, dismissButton = { TextButton(onClick = { calendario = false }) { Text("Cancelar") } }) { DatePicker(estado) }
    }
}

@Composable
fun AvisosCapsulas() {
    val context = LocalContext.current
    var resultado by remember { mutableStateOf<String?>(null) }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        resultado = if (concedido) "Avisos activados." else "Puedes consultar tus cápsulas en Inicio aunque no actives los avisos."
        if (concedido) revisarAvisos(context)
    }
    TextButton(onClick = {
        val manager = NotificationManagerCompat.from(context)
        val canal = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CANAL_CAPSULAS)
        when {
            canal?.importance == NotificationManager.IMPORTANCE_NONE -> {
                context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, CANAL_CAPSULAS))
                resultado = "El canal de cápsulas está silenciado."
            }
            manager.areNotificationsEnabled() -> {
                revisarAvisos(context)
                resultado = "Avisos activados. Android puede retrasarlos por ahorro de batería."
            }
            Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED ->
                permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }
    }) { Text("Activar avisos de cápsulas") }
    TextButton(onClick = {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }) { Text("Ajustes de notificaciones") }
    resultado?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}
