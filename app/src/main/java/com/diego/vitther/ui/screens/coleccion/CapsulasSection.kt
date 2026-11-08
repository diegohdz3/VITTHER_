package com.diego.vitther.ui.screens.coleccion

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.diego.vitther.data.notifications.CANAL_CAPSULAS
import com.diego.vitther.data.notifications.revisarAvisos
import com.diego.vitther.domain.*
import com.diego.vitther.ui.ColeccionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val CapsuleSurface = Color(0xFF141414)
private val CapsuleBorder = Color(0xFF26231C)
private val CapsuleGold = Color(0xFFE8C97A)
private val CapsuleGoldDarkText = Color(0xFF1A1405)
private val CapsuleGhostBorder = Color(0xFF5A4A22)
private val CapsuleMuted = Color(0xFF8D8777)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsulasSection(
    piezaId: Long,
    capsulas: List<CapsulaVista>,
    ahora: Long,
    vm: ColeccionViewModel,
    ocupado: Boolean
) {
    var redactar by rememberSaveable(piezaId) { mutableStateOf(false) }
    var titulo by rememberSaveable(piezaId) { mutableStateOf("") }
    var nota by rememberSaveable(piezaId) { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf<Long?>(null) }
    var calendario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Un mensaje para tu yo del futuro. Sellarlo es opcional.",
            style = MaterialTheme.typography.bodyMedium,
            color = CapsuleMuted
        )

        capsulas.forEach { capsula ->
            val estado = estadoDeCapsula(capsula.abiertaEn, capsula.fechaApertura, ahora)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CapsuleSurface,
                border = BorderStroke(1.dp, CapsuleBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val etiquetaEstado = when (estado) {
                        is EstadoCapsula.Sellada -> "SELLADA"
                        EstadoCapsula.Disponible -> "DISPONIBLE PARA REVELAR"
                        EstadoCapsula.Abierta -> "REVELADA"
                    }
                    Text(
                        text = etiquetaEstado,
                        color = CapsuleGold,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp)
                    )
                    Text(
                        text = capsula.titulo ?: "Un recuerdo para el futuro",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    when (estado) {
                        is EstadoCapsula.Sellada -> {
                            val dias = estado.diasRestantes
                            val detalleDias = when {
                                dias <= 0L -> "se abre hoy"
                                dias == 1L -> "falta 1 día"
                                else -> "faltan $dias días"
                            }
                            Text(
                                text = "Se revela el ${Fechas.formatearCorto(capsula.fechaApertura)} · $detalleDias",
                                style = MaterialTheme.typography.bodySmall,
                                color = CapsuleMuted
                            )
                        }
                        EstadoCapsula.Disponible -> {
                            Text(
                                text = "Apertura disponible desde ${Fechas.formatearCorto(capsula.fechaApertura)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CapsuleMuted
                            )
                            Spacer(Modifier.height(6.dp))
                            Button(
                                enabled = !ocupado,
                                onClick = { vm.abrirCapsula(capsula.id) },
                                shape = RoundedCornerShape(99.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CapsuleGold,
                                    contentColor = CapsuleGoldDarkText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Revelar cápsula")
                            }
                        }
                        EstadoCapsula.Abierta -> {
                            Text(
                                text = capsula.nota.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        if (!redactar) {
            Button(
                enabled = !ocupado,
                onClick = { redactar = true; mensaje = null },
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CapsuleGold,
                    contentColor = CapsuleGoldDarkText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Crear cápsula")
            }
        } else {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it.take(80) },
                label = { Text("Título de cápsula (opcional)") },
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it.take(5000) },
                label = { Text("Mensaje para tu yo del futuro") },
                minLines = 3,
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                enabled = !ocupado,
                onClick = { calendario = true },
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.dp, CapsuleGhostBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = fecha?.let { "Abrir el ${Fechas.formatearCorto(it)}" } ?: "Elegir fecha de apertura",
                    color = CapsuleGold
                )
            }
            Text(
                text = "Se abrirá al comenzar el día elegido. Una vez sellada no podrás editar la nota ni su fecha.",
                style = MaterialTheme.typography.bodySmall,
                color = CapsuleMuted
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !ocupado,
                onClick = {
                    error = when {
                        nota.isBlank() -> "Escribe un mensaje antes de sellarlo."
                        fecha == null || fecha!! <= ahora -> "Elige una fecha futura."
                        else -> null
                    }
                    if (error == null) confirmar = true
                },
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CapsuleGold,
                    contentColor = CapsuleGoldDarkText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Sellar cápsula")
            }
            TextButton(
                enabled = !ocupado,
                onClick = { redactar = false; nota = ""; titulo = ""; fecha = null },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar cápsula", color = CapsuleMuted)
            }
        }

        mensaje?.let { Text(it, color = CapsuleGold) }
        AvisosCapsulas()
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { if (!ocupado) confirmar = false },
            title = { Text("¿Sellar este recuerdo?") },
            text = { Text("El mensaje quedará oculto hasta ${Fechas.formatearLargo(fecha!!)}.") },
            confirmButton = {
                TextButton(
                    enabled = !ocupado,
                    onClick = {
                        vm.sellar(piezaId, titulo, nota, fecha!!) {
                            confirmar = false; redactar = false; nota = ""; titulo = ""; fecha = null
                            mensaje = "Tu cápsula quedó sellada."
                        }
                    }
                ) { Text("Confirmar sellado") }
            },
            dismissButton = {
                TextButton(
                    enabled = !ocupado,
                    onClick = { confirmar = false }
                ) { Text("Seguir escribiendo") }
            }
        )
    }

    if (calendario) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.now())
            }
        )
        DatePickerDialog(
            onDismissRequest = { calendario = false },
            confirmButton = {
                TextButton(
                    enabled = estado.selectedDateMillis != null,
                    onClick = {
                        fecha = estado.selectedDateMillis?.let {
                            Fechas.desdeFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        calendario = false
                    }
                ) { Text("Elegir apertura") }
            },
            dismissButton = {
                TextButton(onClick = { calendario = false }) { Text("Cancelar") }
            }
        ) { DatePicker(estado) }
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
    OutlinedButton(
        onClick = {
            val manager = NotificationManagerCompat.from(context)
            val canal = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CANAL_CAPSULAS)
            when {
                canal?.importance == NotificationManager.IMPORTANCE_NONE -> {
                    context.startActivity(
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, CANAL_CAPSULAS)
                    )
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
                else -> context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        },
        shape = RoundedCornerShape(99.dp),
        border = BorderStroke(1.dp, CapsuleGhostBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text("Activar avisos de cápsulas", color = CapsuleGold)
    }
    resultado?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CapsuleMuted) }
}
