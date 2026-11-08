package com.diego.vitther.ui.screens.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.ui.components.VittherNavigationBar
import com.diego.vitther.ui.screens.coleccion.AvisosCapsulas
import com.diego.vitther.ui.theme.VITTHERTheme
import java.time.Instant
import java.time.Period
import java.time.ZoneId

data class ProfileMenuEntry(val icon: ImageVector, val title: String, val onClick: () -> Unit)

// Campos opcionales hasta que exista una fuente de datos de cuenta.
data class ProfileIdentity(
    val nombre: String? = null,
    val foto: String? = null,
    val registradoEn: Long? = null
)

@Composable
fun ProfileScreen(
    totalPiezas: Int?,
    totalCategorias: Int?,
    ahora: Long,
    identity: ProfileIdentity = ProfileIdentity()
) {
    var opcion by rememberSaveable { mutableStateOf<String?>(null) }
    val items = listOf(
        ProfileMenuEntry(Icons.Outlined.Person, "Cuenta") { opcion = "Cuenta" },
        ProfileMenuEntry(Icons.Outlined.Notifications, "Notificaciones") { opcion = "Notificaciones" },
        ProfileMenuEntry(Icons.Outlined.LightMode, "Apariencia") { opcion = "Apariencia" },
        ProfileMenuEntry(Icons.Outlined.Lock, "Preferencias de cápsula del tiempo") { opcion = "Preferencias de cápsula del tiempo" },
        ProfileMenuEntry(Icons.Outlined.HelpOutline, "Ayuda y soporte") { opcion = "Ayuda y soporte" },
        ProfileMenuEntry(Icons.Outlined.Description, "Términos y privacidad") { opcion = "Términos y privacidad" }
    )
    val zona = ZoneId.systemDefault()
    val registro = identity.registradoEn?.let { Instant.ofEpochMilli(it).atZone(zona).toLocalDate() }
    val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
    val anios = registro?.takeUnless { it.isAfter(hoy) }?.let { Period.between(it, hoy).years }
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(colors.primaryContainer.copy(alpha = .25f), colors.background)))
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileHeader(identity, registro?.year)
        Spacer(Modifier.height(30.dp))
        ProfileStatsRow(totalPiezas, totalCategorias, anios)
        Spacer(Modifier.height(26.dp))
        ProfileMenuList(items)
        Spacer(Modifier.height(20.dp))
        Text("Tu colección se guarda en este dispositivo.", style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
    opcion?.let { titulo ->
        AlertDialog(onDismissRequest = { opcion = null }, title = { Text(titulo) },
            text = {
                if (titulo == "Notificaciones" || titulo == "Preferencias de cápsula del tiempo") {
                    AvisosCapsulas()
                } else {
                    Text("Esta sección estará disponible próximamente.")
                }
            },
            confirmButton = { TextButton(onClick = { opcion = null }) { Text("Cerrar") } })
    }
}

@Composable
fun ProfileHeader(identity: ProfileIdentity, anioRegistro: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(116.dp).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .padding(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center) {
            if (identity.foto == null) ProfileAvatarPlaceholder()
            else SubcomposeAsyncImage(model = identity.foto, contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                loading = { ProfileAvatarPlaceholder() }, error = { ProfileAvatarPlaceholder() })
        }
        Spacer(Modifier.height(20.dp))
        Text(identity.nombre?.takeIf { it.isNotBlank() } ?: "Mi museo",
            style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(anioRegistro?.let { "COLECCIONISTA DESDE $it" } ?: "TU MUSEO PERSONAL",
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileAvatarPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Person, "Sin foto de perfil", Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun ProfileStatsRow(totalPiezas: Int?, totalCategorias: Int?, anios: Int?) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        ProfileStat(totalPiezas?.toString() ?: "—", "PIEZAS TOTALES", Modifier.weight(1f))
        VerticalDivider(Modifier.height(40.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .25f))
        ProfileStat(totalCategorias?.toString() ?: "—", "CATEGORÍAS", Modifier.weight(1f))
        VerticalDivider(Modifier.height(40.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .25f))
        ProfileStat(anios?.toString() ?: "—", "DESDE QUE EMPEZASTE", Modifier.weight(1f),
            anios?.let { if (it == 1) "AÑO" else "AÑOS" })
    }
}

@Composable
private fun ProfileStat(valor: String, etiqueta: String, modifier: Modifier, unidad: String? = null) {
    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            unidad?.let { Text(it, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(6.dp))
        Text(etiqueta, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProfileMenuList(items: List<ProfileMenuEntry>) {
    Column(Modifier.fillMaxWidth()) {
        ProfileDivider()
        items.forEach { item ->
            ProfileMenuItem(item)
            ProfileDivider()
        }
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f), thickness = .5.dp)
}

@Composable
fun ProfileMenuItem(item: ProfileMenuEntry) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = item.onClick)
        .heightIn(min = 56.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(item.icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilePreview() {
    VITTHERTheme {
        Scaffold(bottomBar = { VittherNavigationBar(4) {} }) { padding ->
            Box(Modifier.padding(padding)) {
                ProfileScreen(48, 8, Instant.parse("2025-10-27T12:00:00Z").toEpochMilli(),
                    ProfileIdentity("Diego Cortés Hernández", registradoEn = Instant.parse("2024-01-01T12:00:00Z").toEpochMilli()))
            }
        }
    }
}
