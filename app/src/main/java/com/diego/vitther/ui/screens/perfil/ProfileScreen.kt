package com.diego.vitther.ui.screens.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.ui.components.VittherNavigationBar
import com.diego.vitther.ui.screens.coleccion.ToggleMuseo
import com.diego.vitther.ui.theme.VITTHERTheme
import java.time.Instant
import java.time.Period
import java.time.ZoneId

data class ProfileIdentity(
    val nombre: String? = null,
    val foto: String? = null,
    val registradoEn: Long? = null,
    val correo: String = "diego@correo.com"
)

@Composable
fun ProfileScreen(
    totalPiezas: Int?,
    totalCategorias: Int?,
    ahora: Long,
    identity: ProfileIdentity = ProfileIdentity(),
    estadoFirebase: String? = null,
    sincronizando: Boolean = false,
    onSincronizarFirebase: ((String?) -> Unit)? = null
) {
    var dialogoActivo by rememberSaveable { mutableStateOf<String?>(null) }
    var nombreMuseo by rememberSaveable { mutableStateOf(identity.nombre?.takeIf { it.isNotBlank() } ?: "Mi museo") }
    var correoCuenta by rememberSaveable { mutableStateOf(identity.correo) }
    var firebaseUrlInput by rememberSaveable {
        mutableStateOf("https://vitther-default-rtdb.firebaseio.com")
    }

    var avisosCapsulas by rememberSaveable { mutableStateOf(true) }
    var avisosWishes by rememberSaveable { mutableStateOf(true) }
    var ocultarValor by rememberSaveable { mutableStateOf(true) }

    val zona = ZoneId.systemDefault()
    val registro = identity.registradoEn?.let { Instant.ofEpochMilli(it).atZone(zona).toLocalDate() }
    val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
    val anios = (registro?.takeUnless { it.isAfter(hoy) }?.let { Period.between(it, hoy).years } ?: 1).coerceAtLeast(1)

    val gold = MaterialTheme.colorScheme.primary

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            // Avatar centrado (84.dp) + insignia de edición + título "Mi museo"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clickable { dialogoActivo = "Cuenta" },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF2A2314), Color(0xFF0D0B07))
                                )
                            )
                            .border(1.dp, gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (identity.foto == null) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Avatar de perfil",
                                tint = gold,
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            SubcomposeAsyncImage(
                                model = identity.foto,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    // Botón circular dorado de editar en la esquina inferior derecha
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(26.dp)
                            .background(gold, CircleShape)
                            .border(2.dp, Color(0xFF0A0A0A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar perfil",
                            tint = Color(0xFF1A1405),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = nombreMuseo,
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Serif,
                    color = gold,
                    textAlign = TextAlign.Center
                )
            }

            // GRUPO 1: TU COLECCIÓN
            SeccionGrupoPerfil("TU COLECCIÓN") {
                FilaPerfilNavegacion(
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.CloudUpload,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    titulo = "Respaldo en la nube",
                    insignia = "PLUS",
                    subtitulo = estadoFirebase ?: "Última copia: hoy, 11:20",
                    mostrarDivisor = true,
                    onClick = { dialogoActivo = "Respaldo en la nube" }
                )
                FilaPerfilNavegacion(
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    titulo = "Cuenta",
                    subtitulo = correoCuenta,
                    mostrarDivisor = false,
                    onClick = { dialogoActivo = "Cuenta" }
                )
            }

            // GRUPO 2: AVISOS
            SeccionGrupoPerfil("AVISOS") {
                FilaPerfilSwitch(
                    icono = { IconoRelojArenaPerfil(gold) },
                    titulo = "Cápsulas de tiempo",
                    subtitulo = "Cuando una cápsula esté por abrirse",
                    activo = avisosCapsulas,
                    mostrarDivisor = true,
                    onToggle = { avisosCapsulas = !avisosCapsulas }
                )
                FilaPerfilSwitch(
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    titulo = "Llegadas de Wishes",
                    subtitulo = "Cuando una pieza esté por llegar",
                    activo = avisosWishes,
                    mostrarDivisor = false,
                    onToggle = { avisosWishes = !avisosWishes }
                )
            }

            // GRUPO 3: PRIVACIDAD
            SeccionGrupoPerfil("PRIVACIDAD") {
                FilaPerfilSwitch(
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    titulo = "Ocultar valor estimado",
                    subtitulo = "Se muestra como •••••• hasta que lo pidas",
                    activo = ocultarValor,
                    mostrarDivisor = true,
                    onToggle = { ocultarValor = !ocultarValor }
                )
                FilaPerfilNavegacion(
                    icono = {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    titulo = "Ayuda y soporte",
                    subtitulo = null,
                    mostrarDivisor = false,
                    onClick = { dialogoActivo = "Ayuda y soporte" }
                )
            }

            // Pie de página con resumen del museo + enlaces legales
            val piezasCount = totalPiezas ?: 0
            val franqCount = totalCategorias ?: 0
            val etiquetaAnios = if (anios == 1) "1 AÑO" else "$anios AÑOS"

            Text(
                text = "$piezasCount PIEZAS · $franqCount FRANQUICIAS · $etiquetaAnios",
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                color = Color(0xFF8D8777),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Términos",
                    fontSize = 12.sp,
                    color = Color(0xFF8D8777),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { dialogoActivo = "Términos de uso" }
                        .padding(horizontal = 6.dp)
                )
                Text("·", fontSize = 12.sp, color = Color(0xFF8D8777))
                Text(
                    text = "Privacidad",
                    fontSize = 12.sp,
                    color = Color(0xFF8D8777),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { dialogoActivo = "Aviso de privacidad" }
                        .padding(horizontal = 6.dp)
                )
            }
        }
    }

    dialogoActivo?.let { titulo ->
        AlertDialog(
            onDismissRequest = { dialogoActivo = null },
            title = { Text(titulo, fontFamily = FontFamily.Serif) },
            text = {
                when (titulo) {
                    "Respaldo en la nube" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Sincroniza las cédulas, cápsulas y deseos de tu museo con Firebase Realtime Database.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = firebaseUrlInput,
                                onValueChange = { firebaseUrlInput = it },
                                label = { Text("URL de Firebase Realtime Database") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (sincronizando) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                            }
                            Button(
                                enabled = !sincronizando && onSincronizarFirebase != null,
                                onClick = { onSincronizarFirebase?.invoke(firebaseUrlInput) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = gold,
                                    contentColor = Color(0xFF1A1405)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (sincronizando) "Sincronizando..." else "Sincronizar museo con Firebase")
                            }
                            estadoFirebase?.let { msg ->
                                Text(msg, style = MaterialTheme.typography.bodySmall, color = gold)
                            }
                        }
                    }
                    "Cuenta" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = nombreMuseo,
                                onValueChange = { nombreMuseo = it },
                                label = { Text("Nombre de tu museo") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = correoCuenta,
                                onValueChange = { correoCuenta = it },
                                label = { Text("Correo del coleccionista") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    "Ayuda y soporte" -> {
                        Text("VITTHER conserva tus piezas, cápsulas de tiempo y lista de deseos localmente con Room y permite respaldo en la nube mediante Firebase Realtime Database.")
                    }
                    else -> {
                        Text("Tus fotografías y notas personales permanecen privadas dentro de tu museo VITTHER.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogoActivo = null }) {
                    Text("Listo", color = gold)
                }
            }
        )
    }
}

@Composable
private fun SeccionGrupoPerfil(
    etiqueta: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        Text(
            text = etiqueta,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            color = Color(0xFF8D8777),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        contenido()
    }
}

@Composable
private fun FilaPerfilNavegacion(
    icono: @Composable () -> Unit,
    titulo: String,
    insignia: String? = null,
    subtitulo: String? = null,
    mostrarDivisor: Boolean,
    onClick: () -> Unit
) {
    val gold = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            icono()
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = titulo,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (insignia != null) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color(0xFF5A4A22))
                        ) {
                            Text(
                                text = insignia,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp,
                                color = gold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (subtitulo != null) {
                    Text(
                        text = subtitulo,
                        fontSize = 12.sp,
                        color = Color(0xFF8D8777),
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF8D8777),
                modifier = Modifier.size(18.dp)
            )
        }
        if (mostrarDivisor) {
            HorizontalDivider(color = Color(0xFF26231C), thickness = 1.dp)
        }
    }
}

@Composable
private fun FilaPerfilSwitch(
    icono: @Composable () -> Unit,
    titulo: String,
    subtitulo: String,
    activo: Boolean,
    mostrarDivisor: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            icono()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitulo,
                    fontSize = 12.sp,
                    color = Color(0xFF8D8777),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            ToggleMuseo(activo = activo, onClick = onToggle)
        }
        if (mostrarDivisor) {
            HorizontalDivider(color = Color(0xFF26231C), thickness = 1.dp)
        }
    }
}

@Composable
private fun IconoRelojArenaPerfil(color: Color) {
    Canvas(Modifier.size(21.dp)) {
        val w = size.width
        val h = size.height
        val trazo = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color, Offset(w * 0.25f, h * 0.14f), Offset(w * 0.75f, h * 0.14f), trazo.width)
        drawLine(color, Offset(w * 0.25f, h * 0.86f), Offset(w * 0.75f, h * 0.86f), trazo.width)
        drawPath(
            Path().apply {
                moveTo(w * 0.30f, h * 0.14f)
                lineTo(w * 0.70f, h * 0.86f)
                moveTo(w * 0.70f, h * 0.14f)
                lineTo(w * 0.30f, h * 0.86f)
            },
            color = color,
            style = trazo
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilePreview() {
    VITTHERTheme {
        Scaffold(bottomBar = { VittherNavigationBar(4) {} }) { padding ->
            Box(Modifier.padding(padding)) {
                ProfileScreen(
                    totalPiezas = 11,
                    totalCategorias = 7,
                    ahora = Instant.parse("2026-11-08T12:00:00Z").toEpochMilli(),
                    identity = ProfileIdentity(
                        nombre = "Mi museo",
                        registradoEn = Instant.parse("2025-09-09T12:00:00Z").toEpochMilli()
                    )
                )
            }
        }
    }
}
