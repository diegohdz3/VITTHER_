package com.diego.vitther.ui.screens.perfil

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.SubcomposeAsyncImage
import com.diego.vitther.VittherApp
import com.diego.vitther.data.notifications.revisarAvisos
import com.diego.vitther.ui.components.NotaCuratorialPrimerIngreso
import com.diego.vitther.ui.components.reiniciarOnboardingModulos
import com.diego.vitther.ui.screens.coleccion.ToggleMuseo
import com.diego.vitther.ui.screens.coleccion.VisitarExposicionSheet
import com.diego.vitther.ui.theme.AccentGold
import com.diego.vitther.ui.theme.TextPrimary
import com.diego.vitther.ui.theme.VitherSerif
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ProfileIdentity(
    val nombre: String? = null,
    val foto: String? = null,
    val registradoEn: Long? = null,
    val correo: String = "Curador · VITTHER Cloud"
)

private const val PREFS_PERFIL = "vitther_perfil_prefs"
private const val KEY_NOMBRE_MUSEO = "nombre_museo"
private const val KEY_FOTO_CURADOR = "foto_curador_path"
private const val KEY_MARCO_CURADOR = "marco_curador" // "a" = Línea dorada, "b" = Vitrina
private const val KEY_AVISO_CAPSULAS = "aviso_capsulas"
private const val KEY_AVISO_WISHES = "aviso_wishes"
private const val KEY_OCULTAR_VALOR = "ocultar_valor"
private const val KEY_URL_NUBE = "url_nube_firebase"
private const val URL_NUBE_DEFAULT = "https://vitther-default-rtdb.firebaseio.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    totalPiezas: Int?,
    totalCategorias: Int?,
    ahora: Long = System.currentTimeMillis(),
    identity: ProfileIdentity = ProfileIdentity(),
    estadoFirebase: String? = null,
    sincronizando: Boolean = false,
    onSincronizarFirebase: ((String?) -> Unit)? = null,
    onRestaurarFirebase: ((String?) -> Unit)? = null,
    onCompartirMuseo: (() -> Unit)? = null,
    onAbrirExposicion: ((String) -> Unit)? = null,
    onCerrarSesion: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fotoStore = remember(context) {
        (context.applicationContext as? VittherApp)?.container?.fotos
    }
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_PERFIL, Context.MODE_PRIVATE)
    }

    var nombreMuseo by rememberSaveable {
        mutableStateOf(
            prefs.getString(KEY_NOMBRE_MUSEO, null)
                ?: identity.nombre?.takeIf { it.isNotBlank() }?.let { "Museo de $it" }
                ?: "Mi museo"
        )
    }
    var fotoCuradorPath by rememberSaveable {
        mutableStateOf(prefs.getString(KEY_FOTO_CURADOR, null) ?: identity.foto)
    }
    var marcoCurador by rememberSaveable {
        mutableStateOf(prefs.getString(KEY_MARCO_CURADOR, "a") ?: "a")
    }
    var avisosCapsulas by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_AVISO_CAPSULAS, true))
    }
    var avisosWishes by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_AVISO_WISHES, true))
    }
    var ocultarValor by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_OCULTAR_VALOR, false))
    }
    var urlNube by rememberSaveable {
        mutableStateOf(prefs.getString(KEY_URL_NUBE, URL_NUBE_DEFAULT) ?: URL_NUBE_DEFAULT)
    }

    var mostrarSheetCurador by rememberSaveable { mutableStateOf(false) }
    var mostrarSheetNube by rememberSaveable { mutableStateOf(false) }
    var mostrarSheetGuia by rememberSaveable { mutableStateOf(false) }
    var mostrarSheetPrivacidad by rememberSaveable { mutableStateOf(false) }
    var mostrarVisitarSheet by rememberSaveable { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(estadoFirebase) {
        if (!estadoFirebase.isNullOrBlank()) {
            toastMsg = estadoFirebase
        }
    }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2200)
            toastMsg = null
        }
    }

    val archivoRetrato = remember(fotoCuradorPath) {
        fotoStore?.archivo(fotoCuradorPath)
    }

    val piezasCount = totalPiezas ?: 0
    val salasCount = totalCategorias ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0A08))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp)
        ) {
            // Hero Stage del Curador (.hero)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Celosía Louvre de fondo + riel + 2 lámparas + 2 conos de luz + línea de reflejo (.pond)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Resplandor radial central
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2EFFDC8C), Color.Transparent),
                            center = Offset(w * 0.5f, h * 0.45f),
                            radius = w * 0.55f
                        ),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.55f
                    )

                    // Celosía geométrica de diamantes sutiles
                    val step = 28.dp.toPx()
                    val strokeLattice = Color(0x09E8C97A)
                    var x = -h
                    while (x < w + h) {
                        drawLine(
                            color = strokeLattice,
                            start = Offset(x, 0f),
                            end = Offset(x + h, h),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = strokeLattice,
                            start = Offset(x + h, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.dp.toPx()
                        )
                        x += step
                    }

                    // Riel superior (.rail)
                    val railY = 8.dp.toPx()
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF2E271B),
                                Color(0xFF473C27),
                                Color(0xFF2E271B),
                                Color.Transparent
                            )
                        ),
                        start = Offset(w * 0.14f, railY),
                        end = Offset(w * 0.86f, railY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Dos lámparas (.lamp al 29% y 71%) y conos de luz (.cone)
                    listOf(w * 0.29f, w * 0.71f).forEach { lx ->
                        val cone = Path().apply {
                            moveTo(lx - 15.dp.toPx(), railY + 7.dp.toPx())
                            lineTo(lx + 15.dp.toPx(), railY + 7.dp.toPx())
                            lineTo(w * 0.5f + (lx - w * 0.5f) * 1.3f + 60.dp.toPx(), h)
                            lineTo(w * 0.5f + (lx - w * 0.5f) * 1.3f - 60.dp.toPx(), h)
                            close()
                        }
                        drawPath(
                            path = cone,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x36FFE196),
                                    Color(0x12FFE196),
                                    Color.Transparent
                                ),
                                startY = railY,
                                endY = h
                            )
                        )
                        drawRoundRect(
                            color = Color(0xFF4A3E28),
                            topLeft = Offset(lx - 6.dp.toPx(), railY - 2.dp.toPx()),
                            size = Size(12.dp.toPx(), 7.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFFFFE9A8),
                            radius = 3.5.dp.toPx(),
                            center = Offset(lx, railY + 7.dp.toPx())
                        )
                    }

                    // Línea dorada de horizonte (.pond)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFF5A4A28), Color.Transparent)
                        ),
                        start = Offset(w * 0.5f - 75.dp.toPx(), h - 4.dp.toPx()),
                        end = Offset(w * 0.5f + 75.dp.toPx(), h - 4.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Retrato Enmarcado del Curador (.fr.a o .fr.b) + botón flotante de edición (.pb)
                Box(
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .clickable { mostrarSheetCurador = true }
                ) {
                    val esVitrina = marcoCurador == "b"
                    val shapeMarco = if (esVitrina) {
                        RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                    } else {
                        RoundedCornerShape(6.dp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(122.dp)
                                .aspectRatio(3f / 4f)
                                .clip(shapeMarco)
                                .background(
                                    if (esVitrina) {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1B160D), Color(0xFF0E0B07))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF4A3D22),
                                                Color(0xFF241D11),
                                                Color(0xFF56472A),
                                                Color(0xFF1F190E)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (esVitrina) Color(0xFF3A3120) else Color(0xFF6B5832),
                                    shapeMarco
                                )
                                .padding(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF0C0A07))
                                    .then(
                                        if (!esVitrina) {
                                            Modifier.border(
                                                1.dp,
                                                AccentGold.copy(alpha = 0.28f),
                                                RoundedCornerShape(2.dp)
                                            )
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (archivoRetrato != null) {
                                    SubcomposeAsyncImage(
                                        model = archivoRetrato,
                                        contentDescription = "Retrato del curador",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    SiluetaCuradorDefault()
                                }
                            }
                        }

                        if (esVitrina) {
                            // Pedestal inferior del marco tipo Vitrina (.fr.b::after)
                            Box(
                                modifier = Modifier
                                    .width(126.dp)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF3A3120), Color(0xFF1A150D))
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Color(0xFF5A4A2C),
                                        RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                                    )
                            )
                        }
                    }

                    // Botón flotante de edición (.pb)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF282112), Color(0xFF17130A))
                                )
                            )
                            .border(1.dp, Color(0xFF6B5832), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar retrato y museo",
                            tint = AccentGold,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Título del Museo (.tt)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mostrarSheetCurador = true }
                    .padding(top = 8.dp, bottom = 18.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = nombreMuseo,
                    fontFamily = VitherSerif,
                    fontSize = 36.sp,
                    color = AccentGold,
                    letterSpacing = (-0.6).sp,
                    lineHeight = 38.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "FUNDADO EN 2025",
                    color = Color(0xFF7A7267),
                    fontSize = 8.5.sp,
                    letterSpacing = 2.8.sp
                )
            }

            NotaCuratorialPrimerIngreso(
                moduloKey = "perfil",
                titulo = "Tu cuenta y nube personal",
                descripcion = "Administra tu sesión de Google, respaldos y vitrinas compartidas:",
                puntos = listOf(
                    "Respaldo en la nube guarda tus figuras, deseos y fotos en tu cuenta de Google.",
                    "Comparte tu museo virtual o visita el de un amigo en modo solo lectura.",
                    "En Ayuda y soporte puedes reiniciar estas notas guía cuando quieras."
                )
            )

            // Sección 1: TU COLECCIÓN (.sec + .plq + .vt)
            VitrinaSeccionPerfil(tituloPlaca = "TU COLECCIÓN") {
                val progresoBarra by animateFloatAsState(
                    targetValue = if (sincronizando) 0.85f else 0f,
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    label = "backupProgress"
                )

                // Fila 1: Respaldo en la nube [PLUS]
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !sincronizando) {
                                mostrarSheetNube = true
                            }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        IconoCircularPerfil(icono = Icons.Outlined.CloudQueue)

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Respaldo en la nube",
                                    color = TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF241E11))
                                        .border(1.dp, Color(0xFF5A4A28), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PLUS",
                                        color = AccentGold,
                                        fontSize = 8.sp,
                                        letterSpacing = 1.6.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    sincronizando -> "Sincronizando tu colección…"
                                    !estadoFirebase.isNullOrBlank() -> estadoFirebase
                                    else -> "Toca para respaldar o restaurar · $piezasCount ${if (piezasCount == 1) "pieza" else "piezas"}"
                                },
                                color = Color(0xFF7A7267),
                                fontSize = 11.sp
                            )
                        }

                        // Botón circular dorado de subida rápida (.go)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF282112), Color(0xFF17130A))
                                    )
                                )
                                .border(1.dp, Color(0xFF5A4A28), CircleShape)
                                .clickable(enabled = !sincronizando) {
                                    onSincronizarFirebase?.invoke(urlNube.trim().ifBlank { URL_NUBE_DEFAULT })
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = "Respaldar ahora",
                                tint = AccentGold,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Barra inferior de progreso (.bar)
                    if (sincronizando || progresoBarra > 0.01f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(progresoBarra)
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF8A7340), AccentGold)
                                    )
                                )
                        )
                    }
                }

                DivisorVitrinaPerfil()

                // Fila 2: Cuenta
                FilaVitrinaPerfil(
                    icono = Icons.Outlined.PersonOutline,
                    titulo = "Cuenta de Google",
                    subtitulo = "${identity.nombre ?: "Curador"} · ${identity.correo}",
                    onClick = { mostrarSheetCurador = true },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF6B6358),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                if (onCompartirMuseo != null) {
                    DivisorVitrinaPerfil()
                    FilaVitrinaPerfil(
                        icono = Icons.Outlined.Share,
                        titulo = "Compartir museo virtual",
                        subtitulo = "Generar link o código en solo lectura",
                        onClick = { onCompartirMuseo.invoke() },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF6B6358),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                if (onAbrirExposicion != null) {
                    DivisorVitrinaPerfil()
                    FilaVitrinaPerfil(
                        icono = Icons.Outlined.TravelExplore,
                        titulo = "Visitar colección de un amigo",
                        subtitulo = "Ingresar link o código compartido",
                        onClick = { mostrarVisitarSheet = true },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF6B6358),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // Sección 2: AVISOS (.sec + .plq + .vt)
            val alternarAvisosCapsulas = {
                avisosCapsulas = !avisosCapsulas
                prefs.edit().putBoolean(KEY_AVISO_CAPSULAS, avisosCapsulas).apply()
                context.getSharedPreferences("vitther", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("avisos", avisosCapsulas)
                    .apply()
                if (avisosCapsulas) {
                    revisarAvisos(context)
                    toastMsg = "Avisos de cápsulas activados"
                } else {
                    toastMsg = "Avisos de cápsulas en pausa"
                }
            }
            val alternarAvisosWishes = {
                avisosWishes = !avisosWishes
                prefs.edit().putBoolean(KEY_AVISO_WISHES, avisosWishes).apply()
                toastMsg = if (avisosWishes) "Recordatorios de wishes activos" else "Recordatorios de wishes en pausa"
            }
            val alternarOcultarValor = {
                ocultarValor = !ocultarValor
                prefs.edit().putBoolean(KEY_OCULTAR_VALOR, ocultarValor).apply()
                toastMsg = if (ocultarValor) "Valores ocultos por defecto en fichas" else "Valores visibles en fichas"
            }

            VitrinaSeccionPerfil(tituloPlaca = "AVISOS") {
                FilaVitrinaPerfil(
                    icono = Icons.Outlined.AccessTime,
                    titulo = "Cápsulas de tiempo",
                    subtitulo = "Avisarme cuando se abran",
                    onClick = alternarAvisosCapsulas,
                    trailing = {
                        ToggleMuseo(
                            activo = avisosCapsulas,
                            onClick = alternarAvisosCapsulas
                        )
                    }
                )

                DivisorVitrinaPerfil()

                FilaVitrinaPerfil(
                    icono = Icons.Outlined.AutoAwesome,
                    titulo = "Llegadas de Wishes",
                    subtitulo = "Recordar fechas estimadas",
                    onClick = alternarAvisosWishes,
                    trailing = {
                        ToggleMuseo(
                            activo = avisosWishes,
                            onClick = alternarAvisosWishes
                        )
                    }
                )
            }

            // Sección 3: PRIVACIDAD (.sec + .plq + .vt)
            VitrinaSeccionPerfil(tituloPlaca = "PRIVACIDAD") {
                FilaVitrinaPerfil(
                    icono = Icons.Outlined.VisibilityOff,
                    titulo = "Ocultar valor estimado",
                    subtitulo = "Esconder montos en las fichas",
                    onClick = alternarOcultarValor,
                    trailing = {
                        ToggleMuseo(
                            activo = ocultarValor,
                            onClick = alternarOcultarValor
                        )
                    }
                )

                DivisorVitrinaPerfil()

                FilaVitrinaPerfil(
                    icono = Icons.AutoMirrored.Outlined.HelpOutline,
                    titulo = "Ayuda y soporte",
                    subtitulo = "Guía del curador · v26.2.1",
                    onClick = { mostrarSheetGuia = true },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF6B6358),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            // Pie de página (.foot) + Piso de madera de museo (.floor)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$piezasCount ${if (piezasCount == 1) "PIEZA" else "PIEZAS"} · $salasCount ${if (salasCount == 1) "SALA" else "SALAS"} · 1 AÑO",
                    color = Color(0xFF6B6358),
                    fontSize = 9.5.sp,
                    letterSpacing = 2.4.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Términos · Privacidad",
                    color = Color(0xFF5A5348),
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { mostrarSheetPrivacidad = true }
                )
            }

            // Piso de parquet de museo (.floor)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF1A140C), Color(0xFF0E0B07))
                    )
                )
                drawLine(
                    color = Color(0xFF2D2418),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                val plankW = 36.dp.toPx()
                var x = plankW
                while (x < size.width) {
                    drawLine(
                        color = Color(0x14E8C97A),
                        start = Offset(x, 0f),
                        end = Offset(x - 10.dp.toPx(), size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += plankW
                }
            }
        }

        // Toast flotante
        androidx.compose.animation.AnimatedVisibility(
            visible = toastMsg != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xF51A160E))
                    .border(1.dp, Color(0xFF4A3D24), RoundedCornerShape(999.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = toastMsg.orEmpty(),
                    color = AccentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Bottom sheet "Retrato del curador" (#sh)
    if (mostrarSheetCurador) {
        var nombreTemp by rememberSaveable { mutableStateOf(nombreMuseo) }
        var marcoTemp by rememberSaveable { mutableStateOf(marcoCurador) }

        val galeriaLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null && fotoStore != null) {
                scope.launch {
                    runCatching { fotoStore.importar(uri) }
                        .onSuccess { nuevoPath ->
                            fotoCuradorPath = nuevoPath
                            prefs.edit().putString(KEY_FOTO_CURADOR, nuevoPath).apply()
                            toastMsg = "Retrato actualizado"
                        }
                }
            }
        }

        val camaraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bmp: Bitmap? ->
            if (bmp != null && fotoStore != null) {
                scope.launch {
                    runCatching { fotoStore.guardarBitmap(bmp) }
                        .onSuccess { nuevoPath ->
                            fotoCuradorPath = nuevoPath
                            prefs.edit().putString(KEY_FOTO_CURADOR, nuevoPath).apply()
                            toastMsg = "Retrato capturado"
                        }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { mostrarSheetCurador = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF13110D),
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Retrato del curador",
                    fontFamily = VitherSerif,
                    fontSize = 25.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                )

                // Input Nombre del museo (.in)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF17140E))
                        .border(1.dp, Color(0xFF252017), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (nombreTemp.isEmpty()) {
                        Text(
                            text = "Nombre del museo",
                            fontFamily = VitherSerif,
                            fontSize = 20.sp,
                            color = Color(0xFF6B6358)
                        )
                    }
                    BasicTextField(
                        value = nombreTemp,
                        onValueChange = { nombreTemp = it },
                        singleLine = true,
                        cursorBrush = SolidColor(AccentGold),
                        textStyle = TextStyle(
                            fontFamily = VitherSerif,
                            fontSize = 20.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botones Galería y Cámara (.pbr / .pbt)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF17140E))
                            .border(1.dp, Color(0xFF262117), RoundedCornerShape(12.dp))
                            .clickable { galeriaLauncher.launch("image/*") }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "Galería",
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Galería",
                            color = Color(0xFFD8D0C2),
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF17140E))
                            .border(1.dp, Color(0xFF262117), RoundedCornerShape(12.dp))
                            .clickable { camaraLauncher.launch(null) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "Cámara",
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cámara",
                            color = Color(0xFFD8D0C2),
                            fontSize = 12.sp
                        )
                    }
                }

                // Selector de MARCO (.frs / .fro)
                Text(
                    text = "MARCO",
                    color = Color(0xFF7A7267),
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 14.dp, bottom = 7.dp, start = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Línea dorada ("a")
                    val activoA = marcoTemp == "a"
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (activoA) Color(0xFF221C11) else Color(0xFF15120D))
                            .border(
                                1.dp,
                                if (activoA) AccentGold else Color(0xFF252017),
                                RoundedCornerShape(11.dp)
                            )
                            .clickable { marcoTemp = "a" }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 28.dp, height = 36.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF0E0C08))
                                .border(2.dp, Color(0xFF6B5832), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Línea dorada",
                            color = if (activoA) AccentGold else Color(0xFF9A9184),
                            fontSize = 10.5.sp
                        )
                    }

                    // Vitrina ("b")
                    val activoB = marcoTemp == "b"
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (activoB) Color(0xFF221C11) else Color(0xFF15120D))
                            .border(
                                1.dp,
                                if (activoB) AccentGold else Color(0xFF252017),
                                RoundedCornerShape(11.dp)
                            )
                            .clickable { marcoTemp = "b" }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(width = 28.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFF0E0C08))
                                    .border(
                                        1.dp,
                                        Color(0xFF3A3120),
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 28.dp, height = 4.dp)
                                    .background(Color(0xFF4A3D22))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Vitrina",
                            color = if (activoB) AccentGold else Color(0xFF9A9184),
                            fontSize = 10.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón primario Guardar (.sbtn.pri)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF282112), Color(0xFF1B160C))
                            )
                        )
                        .border(1.dp, Color(0xFF6B5832), RoundedCornerShape(13.dp))
                        .clickable {
                            val limpio = nombreTemp.trim().ifBlank { "Mi museo" }
                            nombreMuseo = limpio
                            marcoCurador = marcoTemp
                            prefs
                                .edit()
                                .putString(KEY_NOMBRE_MUSEO, limpio)
                                .putString(KEY_MARCO_CURADOR, marcoTemp)
                                .apply()
                            mostrarSheetCurador = false
                            toastMsg = "Guardado"
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Guardar",
                        color = AccentGold,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (onCerrarSesion != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFF171311))
                            .border(1.dp, Color(0xFF4A2B2B), RoundedCornerShape(13.dp))
                            .clickable {
                                mostrarSheetCurador = false
                                onCerrarSesion.invoke()
                            }
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = Color(0xFFE07A6E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cambiar cuenta de Google / Cerrar sesión",
                            color = Color(0xFFE07A6E),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet "VITTHER Cloud"
    if (mostrarSheetNube) {
        var urlTemp by rememberSaveable { mutableStateOf(urlNube) }
        val piezasCount = totalPiezas ?: 0

        ModalBottomSheet(
            onDismissRequest = { mostrarSheetNube = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF13110D),
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "VITTHER CLOUD",
                    color = AccentGold,
                    fontSize = 10.sp,
                    letterSpacing = 2.6.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Respaldo y restauración en la nube",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontFamily = VitherSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = estadoFirebase?.takeIf { it.isNotBlank() }
                        ?: "Colección actual: $piezasCount ${if (piezasCount == 1) "pieza" else "piezas"} listas para sincronizar con fotografías.",
                    color = Color(0xFF8D8477),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "URL DE FIREBASE REALTIME DATABASE",
                    color = Color(0xFF8D8477),
                    fontSize = 9.5.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlTemp,
                    onValueChange = { urlTemp = it },
                    singleLine = true,
                    placeholder = {
                        Text(URL_NUBE_DEFAULT, color = Color(0xFF5A5348), fontSize = 12.5.sp)
                    },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = Color(0xFF3A3120),
                        cursorColor = AccentGold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                val resolverUrlLista = {
                    val limpia = urlTemp.trim().ifBlank { URL_NUBE_DEFAULT }
                    urlNube = limpia
                    prefs.edit().putString(KEY_URL_NUBE, limpia).apply()
                    limpia
                }

                // Botón primario: Respaldar colección y fotos ahora
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF282112), Color(0xFF1B160C)))
                        )
                        .border(1.dp, Color(0xFF6B5832), RoundedCornerShape(13.dp))
                        .clickable(enabled = !sincronizando) {
                            val urlLista = resolverUrlLista()
                            mostrarSheetNube = false
                            onSincronizarFirebase?.invoke(urlLista)
                        }
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileUpload,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Respaldar colección y fotos ahora",
                        color = AccentGold,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón secundario: Restaurar colección desde la nube
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF17140F))
                        .border(1.dp, Color(0xFF2E271A), RoundedCornerShape(13.dp))
                        .clickable(enabled = !sincronizando) {
                            val urlLista = resolverUrlLista()
                            mostrarSheetNube = false
                            onRestaurarFirebase?.invoke(urlLista)
                        }
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Restaurar colección desde la nube",
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Bottom sheet "Guía del curador"
    if (mostrarSheetGuia) {
        SheetInformativoPerfil(
            etiqueta = "GUÍA DEL CURADOR · v26.2.1",
            titulo = "Los 5 pabellones de VITTHER",
            bloques = listOf(
                "Inicio Louvre 3D" to "Carrusel de piezas destacadas con iluminación nocturna. Desliza las vitrinas o toca cualquier nicho para abrir su Cédula.",
                "Cédulas, Zoom y PDF" to "Visualiza cada figura en alta resolución con zoom táctil (Modo Contemplar), escucha su historia narrada o expórtala en PDF oficial.",
                "Salas Compartibles" to "Comparte tu colección completa o una sala específica (Naruto, Star Wars, etc.) mediante código o link de solo lectura.",
                "Cápsulas del Tiempo" to "Sella notas para el futuro en cada pieza. Al cumplirse la fecha recibirás un aviso automático mediante WorkManager.",
                "Historia y Audioguía" to "Recorre tus piezas por épocas o franquicias con reproductor automático de voz (Text-to-Speech).",
                "Cuenta Google y VITTHER Cloud" to "Respalda tu colección completa con fotografías en la nube vinculada a tu cuenta y restáurala en cualquier dispositivo."
            ),
            accionExtra = {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF282112), Color(0xFF1B160C)))
                        )
                        .border(1.dp, Color(0xFF6B5832), RoundedCornerShape(12.dp))
                        .clickable {
                            reiniciarOnboardingModulos(context)
                            mostrarSheetGuia = false
                            toastMsg = "Guías de primer ingreso reactivadas en todos los módulos"
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Volver a ver notas guía en los módulos",
                        color = AccentGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            onDismiss = { mostrarSheetGuia = false }
        )
    }

    // Bottom sheet "Términos · Privacidad"
    if (mostrarSheetPrivacidad) {
        SheetInformativoPerfil(
            etiqueta = "ARQUITECTURA DE PRIVACIDAD",
            titulo = "Términos y resguardo local",
            bloques = listOf(
                "Almacenamiento privado" to "Todas tus fotografías y base de datos SQLite/Room residen exclusivamente en el directorio interno privado de la app (filesDir), inaccesible para otras aplicaciones.",
                "Cámara sin permisos invasivos" to "La captura de fotos delega en la cámara nativa vía FileProvider con URIs temporales seguros y utiliza Android Photo Picker sin solicitar acceso general a tu galería.",
                "Visitas en solo lectura" to "Cuando compartes un link o código de colección, los valores monetarios estimados se omiten y solo tú como dueño puedes editar o eliminar piezas."
            ),
            onDismiss = { mostrarSheetPrivacidad = false }
        )
    }

    if (mostrarVisitarSheet && onAbrirExposicion != null) {
        VisitarExposicionSheet(
            onDismiss = { mostrarVisitarSheet = false },
            onVisitarCodigo = onAbrirExposicion
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetInformativoPerfil(
    etiqueta: String,
    titulo: String,
    bloques: List<Pair<String, String>>,
    accionExtra: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF13110D),
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = etiqueta,
                color = AccentGold,
                fontSize = 9.5.sp,
                letterSpacing = 2.4.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = titulo,
                color = TextPrimary,
                fontSize = 19.sp,
                fontFamily = VitherSerif
            )
            Spacer(modifier = Modifier.height(14.dp))
            bloques.forEachIndexed { idx, (subtitulo, cuerpo) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = subtitulo,
                        color = AccentGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = cuerpo,
                        color = Color(0xFFACA395),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                if (idx < bloques.lastIndex) {
                    DivisorVitrinaPerfil()
                }
            }
            accionExtra?.invoke()
        }
    }
}

@Composable
private fun VitrinaSeccionPerfil(
    tituloPlaca: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 10.dp)
    ) {
        // Placa de bronce superpuesta (.plq)
        Box(
            modifier = Modifier
                .padding(start = 14.dp)
                .offset(y = 8.dp)
                .zIndex(2f)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A160D), Color(0xFF100D08))
                    )
                )
                .border(1.dp, Color(0xFF3A3322), RoundedCornerShape(999.dp))
                .padding(horizontal = 11.dp, vertical = 3.dp)
        ) {
            Text(
                text = tituloPlaca,
                color = AccentGold,
                fontSize = 8.5.sp,
                letterSpacing = 2.6.sp
            )
        }

        // Contenedor de vitrina (.vt)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF14110B), Color(0xFF0D0B07))
                    )
                )
                .border(1.dp, Color(0xFF2A2318), RoundedCornerShape(16.dp))
                .padding(top = 6.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun FilaVitrinaPerfil(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        IconoCircularPerfil(icono = icono)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitulo,
                color = Color(0xFF7A7267),
                fontSize = 11.sp
            )
        }

        trailing()
    }
}

@Composable
private fun IconoCircularPerfil(icono: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF211B10), Color(0xFF120F09))
                )
            )
            .border(1.dp, Color(0xFF332B1C), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = AccentGold,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DivisorVitrinaPerfil() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color(0xFF1E1911))
    )
}

@Composable
private fun SiluetaCuradorDefault() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2A2113), Color(0xFF0E0B07)),
                center = Offset(w * 0.5f, h * 0.35f),
                radius = w * 0.75f
            )
        )

        // Cabeza del curador
        drawCircle(
            color = Color(0x38E8C97A),
            radius = w * 0.16f,
            center = Offset(w * 0.5f, h * 0.38f)
        )
        drawCircle(
            color = Color(0x70E8C97A),
            radius = w * 0.16f,
            center = Offset(w * 0.5f, h * 0.38f),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Hombros del curador
        val bustPath = Path().apply {
            moveTo(w * 0.20f, h * 0.90f)
            cubicTo(w * 0.22f, h * 0.64f, w * 0.78f, h * 0.64f, w * 0.80f, h * 0.90f)
            close()
        }
        drawPath(path = bustPath, color = Color(0x28E8C97A))
        drawPath(
            path = bustPath,
            color = Color(0x66E8C97A),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
