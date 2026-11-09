package com.diego.vitther.ui.screens.coleccion

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

private val CapsuleBorder = Color(0xFF3A3322)
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

    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (capsulas.isEmpty() && !redactar) {
            // Tarjeta curatorial cuando aún no hay cápsulas selladas
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                color = CapsuleGold.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, CapsuleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(1.dp, CapsuleGhostBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconoRelojArenaCapsula(color = CapsuleGold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sin cápsulas selladas",
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF2EAD8)
                        )
                        Text(
                            text = "Guarda un mensaje para tu yo del futuro",
                            fontSize = 12.sp,
                            color = CapsuleMuted,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }

        capsulas.forEach { capsula ->
            val estado = estadoDeCapsula(capsula.abiertaEn, capsula.fechaApertura, ahora)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                color = CapsuleGold.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, CapsuleBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .border(1.dp, CapsuleGhostBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconoRelojArenaCapsula(color = CapsuleGold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            val tituloMostrado = when (estado) {
                                is EstadoCapsula.Sellada -> capsula.titulo?.takeIf { it.isNotBlank() } ?: "Cápsula sellada"
                                EstadoCapsula.Disponible -> capsula.titulo?.takeIf { it.isNotBlank() } ?: "Lista para revelar"
                                EstadoCapsula.Abierta -> capsula.titulo?.takeIf { it.isNotBlank() } ?: "Cápsula revelada"
                            }
                            Text(
                                text = tituloMostrado,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF2EAD8)
                            )
                            when (estado) {
                                is EstadoCapsula.Sellada -> {
                                    val dias = estado.diasRestantes
                                    val detalleDias = when {
                                        dias <= 0L -> "Se abre hoy"
                                        dias == 1L -> "Se abre en 1 día"
                                        else -> "Se abre en $dias días"
                                    }
                                    Text(
                                        text = "$detalleDias · ${Fechas.formatearCorto(capsula.fechaApertura)}",
                                        fontSize = 12.sp,
                                        color = CapsuleMuted,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                                EstadoCapsula.Disponible -> {
                                    Text(
                                        text = "Disponible desde ${Fechas.formatearCorto(capsula.fechaApertura)}",
                                        fontSize = 12.sp,
                                        color = CapsuleGold,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                                EstadoCapsula.Abierta -> {
                                    Text(
                                        text = "Abierta el ${Fechas.formatearCorto(capsula.abiertaEn ?: capsula.fechaApertura)}",
                                        fontSize = 12.sp,
                                        color = CapsuleMuted,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                            }
                        }
                    }

                    when (estado) {
                        is EstadoCapsula.Sellada -> Unit
                        EstadoCapsula.Disponible -> {
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
                                color = Color(0xFFF2EAD8)
                            )
                        }
                    }
                }
            }
        }

        if (!redactar) {
            Surface(
                onClick = { if (!ocupado) { redactar = true; mensaje = null } },
                shape = RoundedCornerShape(99.dp),
                color = CapsuleGold.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, CapsuleGhostBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Crear cápsula",
                        fontSize = 14.sp,
                        color = CapsuleGold
                    )
                }
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
    var avisosActivos by rememberSaveable { mutableStateOf(true) }
    var resultado by remember { mutableStateOf<String?>(null) }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        avisosActivos = concedido
        resultado = if (concedido) "Avisos activados." else "Puedes consultar tus cápsulas en Inicio aunque no actives los avisos."
        if (concedido) revisarAvisos(context)
    }

    val accionarToggle: () -> Unit = {
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
                avisosActivos = !avisosActivos
                if (avisosActivos) {
                    revisarAvisos(context)
                    resultado = "Avisos activados."
                } else {
                    resultado = "Avisos en pausa."
                }
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
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = accionarToggle)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Avisarme al abrirse",
            fontSize = 14.5.sp,
            color = Color(0xFFF2EAD8)
        )
        ToggleMuseo(
            activo = avisosActivos,
            onClick = accionarToggle
        )
    }
    resultado?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CapsuleMuted) }
}

@Composable
private fun IconoRelojArenaCapsula(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color, Offset(w * 0.24f, h * 0.14f), Offset(w * 0.76f, h * 0.14f), s.width)
        drawLine(color, Offset(w * 0.24f, h * 0.86f), Offset(w * 0.76f, h * 0.86f), s.width)
        val p = Path().apply {
            moveTo(w * 0.28f, h * 0.14f)
            cubicTo(w * 0.28f, h * 0.42f, w * 0.50f, h * 0.45f, w * 0.50f, h * 0.50f)
            cubicTo(w * 0.50f, h * 0.55f, w * 0.28f, h * 0.58f, w * 0.28f, h * 0.86f)
            moveTo(w * 0.72f, h * 0.14f)
            cubicTo(w * 0.72f, h * 0.42f, w * 0.50f, h * 0.45f, w * 0.50f, h * 0.50f)
            cubicTo(w * 0.50f, h * 0.55f, w * 0.72f, h * 0.58f, w * 0.72f, h * 0.86f)
        }
        drawPath(p, color, style = s)
    }
}
