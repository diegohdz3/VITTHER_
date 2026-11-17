package com.diego.vitther.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diego.vitther.data.auth.VittherUserAccount
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val GoldLogin = Color(0xFFE8C97A)
private val MutedLogin = Color(0xFF8D8777)
private val TextLogin = Color(0xFFF2EAD8)
private val PortalBorder = Color(0xFF6B5A2C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    cuentasRecordadas: List<VittherUserAccount>,
    onConfirmarCuentaGoogle: (nombre: String, correo: String) -> VittherUserAccount,
    onSeleccionarCuentaRecordada: (VittherUserAccount) -> VittherUserAccount,
    onPortalAbiertoYListo: (VittherUserAccount) -> Unit
) {
    var cargando by remember { mutableStateOf(false) }
    var portalAbierto by remember { mutableStateOf(false) }
    var mostrarBienvenida by remember { mutableStateOf(false) }
    var ocultarBotones by remember { mutableStateOf(false) }
    var usuarioAutenticado by remember { mutableStateOf<VittherUserAccount?>(null) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    var mostrarSelectorGoogle by rememberSaveable { mutableStateOf(false) }
    var mostrarLegal by rememberSaveable { mutableStateOf<String?>(null) }

    // Secuencia cinematográfica del portal al autenticarse con Google
    LaunchedEffect(usuarioAutenticado) {
        val cuenta = usuarioAutenticado ?: return@LaunchedEffect
        cargando = true
        delay(650)
        portalAbierto = true
        delay(650)
        mostrarBienvenida = true
        ocultarBotones = true
        delay(950)
        onPortalAbiertoYListo(cuenta)
    }

    val puertaEase = remember { CubicBezierEasing(0.65f, 0f, 0.25f, 1f) }
    val desplazamientoPuerta by animateFloatAsState(
        targetValue = if (portalAbierto) 1.02f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = puertaEase),
        label = "puertaPortal"
    )
    val opacidadLuz by animateFloatAsState(
        targetValue = if (portalAbierto) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "luzPortal"
    )
    val escalaEstrella by animateFloatAsState(
        targetValue = if (portalAbierto) 1.35f else 1f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "estrellaPortal"
    )
    val opacidadControles by animateFloatAsState(
        targetValue = if (ocultarBotones) 0f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "opacidadBotones"
    )

    val fondo = Brush.verticalGradient(
        0.0f to Color(0xFF0B1224),
        0.38f to Color(0xFF0A0D17),
        0.72f to Color(0xFF0A0A0A),
        1.0f to Color(0xFF0A0A0A)
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val anchoTotal = maxWidth
        val escalaEspacial = if (maxHeight < 700.dp) 0.84f else 1f
        val anchoPortal = 236.dp * escalaEspacial
        val altoPortal = 316.dp * escalaEspacial
        val topPiramide = 36.dp * escalaEspacial
        val topPortal = 60.dp * escalaEspacial

        // 1. Cielo estrellado parpadeante (.sky)
        CieloEstrelladoLogin(modifier = Modifier.fillMaxSize())

        // 2. Pirámide del Louvre reticulada (.pyr)
        PiramideLoginCanvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPiramide)
                .width(minOf(anchoTotal, 390.dp))
                .height(360.dp * escalaEspacial)
        )

        // 3. Portal Arqueado con Luz Radial, Puertas de Celosía y Estrella de 4 puntas (.portal)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(topPortal))

            Box(
                modifier = Modifier.size(anchoPortal, altoPortal),
                contentAlignment = Alignment.Center
            ) {
                val archShape = RoundedCornerShape(topStart = anchoPortal / 2, topEnd = anchoPortal / 2)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(archShape)
                        .background(Color(0xFF0A0D17))
                ) {
                    // Luz radial interior del museo (.light)
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                0.0f to Color(0xFFF6E2B6).copy(alpha = 0.70f * opacidadLuz),
                                0.42f to Color(0xFFC79A58).copy(alpha = 0.34f * opacidadLuz),
                                0.82f to Color(0xFF0A0D17).copy(alpha = 0.96f),
                                center = Offset(size.width * 0.5f, size.height * 0.62f),
                                radius = size.height * 0.72f
                            )
                        )
                    }

                    // Puertas gemelas con celosía diagonal dorada (.door.dl / .door.dr)
                    Canvas(Modifier.fillMaxSize()) {
                        val halfW = size.width * 0.505f
                        val h = size.height
                        val stepPx = 22.dp.toPx()

                        // Puerta izquierda
                        val shiftL = -size.width * 0.505f * desplazamientoPuerta
                        clipRect(left = shiftL, top = 0f, right = shiftL + halfW, bottom = h) {
                            drawRect(
                                color = Color(0xFF0C1122),
                                topLeft = Offset(shiftL, 0f),
                                size = androidx.compose.ui.geometry.Size(halfW, h)
                            )
                            dibujarCelosiaPuerta(shiftL, halfW, h, stepPx)
                            drawLine(
                                color = PortalBorder,
                                start = Offset(shiftL + halfW - 0.5.dp.toPx(), 0f),
                                end = Offset(shiftL + halfW - 0.5.dp.toPx(), h),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Puerta derecha
                        val baseR = size.width - halfW
                        val shiftR = size.width * 0.505f * desplazamientoPuerta
                        val leftR = baseR + shiftR
                        clipRect(left = leftR, top = 0f, right = leftR + halfW, bottom = h) {
                            drawRect(
                                color = Color(0xFF0C1122),
                                topLeft = Offset(leftR, 0f),
                                size = androidx.compose.ui.geometry.Size(halfW, h)
                            )
                            dibujarCelosiaPuerta(leftR, halfW, h, stepPx)
                            drawLine(
                                color = PortalBorder,
                                start = Offset(leftR + 0.5.dp.toPx(), 0f),
                                end = Offset(leftR + 0.5.dp.toPx(), h),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // Borde interior y exterior del arco (sin línea inferior)
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeOuter = 1.dp.toPx()
                        val strokeInner = 1.dp.toPx()
                        val archPathOuter = Path().apply {
                            val r = size.width / 2f
                            moveTo(strokeOuter / 2f, size.height)
                            lineTo(strokeOuter / 2f, r)
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(
                                    strokeOuter / 2f,
                                    strokeOuter / 2f,
                                    size.width - strokeOuter / 2f,
                                    size.width - strokeOuter / 2f
                                ),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 180f,
                                forceMoveTo = false
                            )
                            lineTo(size.width - strokeOuter / 2f, size.height)
                        }
                        drawPath(archPathOuter, color = PortalBorder, style = Stroke(width = strokeOuter))

                        val inset = 7.dp.toPx()
                        val archPathInner = Path().apply {
                            val r = (size.width - inset * 2f) / 2f
                            moveTo(inset, size.height)
                            lineTo(inset, inset + r)
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(
                                    inset,
                                    inset,
                                    size.width - inset,
                                    inset + r * 2f
                                ),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 180f,
                                forceMoveTo = false
                            )
                            lineTo(size.width - inset, size.height)
                        }
                        drawPath(
                            archPathInner,
                            color = GoldLogin.copy(alpha = 0.24f),
                            style = Stroke(width = strokeInner)
                        )
                    }

                    // Estrella de 4 puntas centrada en el portal (.star)
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(58.dp * escalaEspacial)
                            .graphicsLayer {
                                scaleX = escalaEstrella
                                scaleY = escalaEstrella
                            }
                    ) {
                        val haloAlpha = if (portalAbierto) 0.55f else 0.26f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(GoldLogin.copy(alpha = haloAlpha), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.72f
                            )
                        )
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val starPath = Path().apply {
                            moveTo(12f * sx, 2f * sy)
                            lineTo(14.2f * sx, 9.8f * sy)
                            lineTo(22f * sx, 12f * sy)
                            lineTo(14.2f * sx, 14.2f * sy)
                            lineTo(12f * sx, 22f * sy)
                            lineTo(9.8f * sx, 14.2f * sy)
                            lineTo(2f * sx, 12f * sy)
                            lineTo(9.8f * sx, 9.8f * sy)
                            close()
                        }
                        drawPath(starPath, color = GoldLogin)
                    }
                }
            }

            // .base y .pool debajo del portal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            0.0f to Color.Transparent,
                            0.25f to PortalBorder,
                            0.75f to PortalBorder,
                            1.0f to Color.Transparent
                        )
                    )
            )
            Canvas(
                modifier = Modifier
                    .width(230.dp)
                    .height(26.dp)
            ) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldLogin.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.50f
                    )
                )
            }

            Spacer(Modifier.height(18.dp * escalaEspacial))

            // 4. Branding (.brand): VITTHER + "Tu museo personal" / "Bienvenido, Diego"
            Text(
                text = "VITTHER",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 32.sp,
                letterSpacing = 11.sp,
                color = GoldLogin,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            val primerNombre = usuarioAutenticado?.nombre?.substringBefore(" ")?.takeIf { it.isNotBlank() }
            if (mostrarBienvenida && primerNombre != null) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MutedLogin, fontStyle = FontStyle.Italic)) {
                            append("Bienvenido, ")
                        }
                        withStyle(SpanStyle(color = TextLogin, fontWeight = FontWeight.Medium)) {
                            append(primerNombre)
                        }
                    },
                    fontFamily = FontFamily.Serif,
                    fontSize = 21.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Tu museo personal",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 21.sp,
                    color = MutedLogin,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            // 5. Botón Google + Estado + Legal (.bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp)
                    .alpha(opacidadControles),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    onClick = {
                        if (!cargando && !ocultarBotones) {
                            mensajeError = null
                            mostrarSelectorGoogle = true
                        }
                    },
                    enabled = !cargando && !ocultarBotones,
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF131314),
                    border = BorderStroke(1.dp, Color(0xFF8E918F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = GoldLogin,
                                trackColor = Color(0xFF3A3A3C),
                                strokeWidth = 2.dp
                            )
                        } else {
                            GoogleMultiColorLogo(modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when {
                                cargando -> "Abriendo tu museo…"
                                mensajeError != null -> "Reintentar con Google"
                                else -> "Continuar con Google"
                            },
                            color = Color(0xFFE3E3E3),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(top = 10.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (!mensajeError.isNullOrBlank()) {
                        Text(
                            text = mensajeError.orEmpty(),
                            color = Color(0xFFE0707A),
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Al continuar aceptas los ",
                        color = MutedLogin,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Términos",
                        color = MutedLogin,
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { mostrarLegal = "terminos" }
                    )
                    Text(
                        text = " y el ",
                        color = MutedLogin,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Aviso de privacidad",
                        color = MutedLogin,
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { mostrarLegal = "privacidad" }
                    )
                }
            }
        }
    }

    // Hoja modal de Selección de Cuenta de Google
    if (mostrarSelectorGoogle) {
        var modoNuevaCuenta by rememberSaveable { mutableStateOf(cuentasRecordadas.isEmpty()) }
        var nombreInput by rememberSaveable { mutableStateOf("") }
        var correoInput by rememberSaveable { mutableStateOf("") }
        var errorModal by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { mostrarSelectorGoogle = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF14151A),
            contentColor = TextLogin
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoogleMultiColorLogo(modifier = Modifier.size(22.dp))
                    Text(
                        text = "Acceder con Google",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        color = TextLogin,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tu colección, fotografías y cápsulas quedarán vinculadas a tu cuenta para respaldarlas en la nube.",
                    fontSize = 12.sp,
                    color = MutedLogin,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(16.dp))

                if (!modoNuevaCuenta && cuentasRecordadas.isNotEmpty()) {
                    cuentasRecordadas.forEach { cuenta ->
                        Surface(
                            onClick = {
                                val seleccionada = onSeleccionarCuentaRecordada(cuenta)
                                mostrarSelectorGoogle = false
                                usuarioAutenticado = seleccionada
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1A1B22),
                            border = BorderStroke(1.dp, Color(0xFF2B2820)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF282112))
                                        .border(1.dp, PortalBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cuenta.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                                        color = GoldLogin,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cuenta.nombre,
                                        color = TextLogin,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = cuenta.correo,
                                        color = MutedLogin,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFF26231C),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { modoNuevaCuenta = true }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = GoldLogin,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Usar otra cuenta de Google",
                            color = GoldLogin,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = correoInput,
                        onValueChange = { correoInput = it; errorModal = null },
                        singleLine = true,
                        label = { Text("Correo de Google", color = MutedLogin) },
                        placeholder = { Text("curador@gmail.com", color = Color(0xFF5A5348)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldLogin,
                            unfocusedBorderColor = Color(0xFF332D22),
                            focusedTextColor = TextLogin,
                            unfocusedTextColor = TextLogin,
                            cursorColor = GoldLogin
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nombreInput,
                        onValueChange = { nombreInput = it },
                        singleLine = true,
                        label = { Text("Tu nombre de curador", color = MutedLogin) },
                        placeholder = { Text("Ej. Diego Cortés", color = Color(0xFF5A5348)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PersonOutline,
                                contentDescription = null,
                                tint = MutedLogin
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldLogin,
                            unfocusedBorderColor = Color(0xFF332D22),
                            focusedTextColor = TextLogin,
                            unfocusedTextColor = TextLogin,
                            cursorColor = GoldLogin
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!errorModal.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorModal.orEmpty(),
                            color = Color(0xFFE0707A),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            runCatching { onConfirmarCuentaGoogle(nombreInput, correoInput) }
                                .onSuccess { cuenta ->
                                    mostrarSelectorGoogle = false
                                    usuarioAutenticado = cuenta
                                }
                                .onFailure { err ->
                                    errorModal = err.message ?: "Verifica el correo ingresado."
                                }
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldLogin,
                            contentColor = Color(0xFF1A1405)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Continuar e iniciar mi museo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (cuentasRecordadas.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Volver a cuentas guardadas",
                            color = MutedLogin,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { modoNuevaCuenta = false }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Hoja modal legal de Términos / Privacidad
    if (mostrarLegal != null) {
        val esTerminos = mostrarLegal == "terminos"
        ModalBottomSheet(
            onDismissRequest = { mostrarLegal = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF13110D),
            contentColor = TextLogin
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = if (esTerminos) "TÉRMINOS DE USO" else "AVISO DE PRIVACIDAD",
                    color = GoldLogin,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (esTerminos) "Tu museo personal en VITTHER" else "Protección de tu colección",
                    color = TextLogin,
                    fontSize = 19.sp,
                    fontFamily = FontFamily.Serif
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (esTerminos) {
                        "VITTHER es un espacio personal para catalogar, fotografiar y narrar tu colección. Cuando compartes una sala mediante link o código, los visitantes acceden únicamente en modo de solo lectura y nunca pueden alterar tus piezas."
                    } else {
                        "Tu cuenta de Google vincula el identificador de tu respaldo en Firebase para que nunca pierdas tus figuras ni tus cápsulas al cambiar de teléfono. Cuando compartes una colección con amigos, los valores estimados se ocultan automáticamente."
                    },
                    color = Color(0xFFACA395),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.dibujarCelosiaPuerta(
    left: Float,
    width: Float,
    height: Float,
    step: Float
) {
    val lineColor = GoldLogin.copy(alpha = 0.20f)
    val strokeW = 1.dp.toPx()
    var d = -height
    while (d < width + height) {
        drawLine(
            color = lineColor,
            start = Offset(left + d, 0f),
            end = Offset(left + d + height, height),
            strokeWidth = strokeW
        )
        drawLine(
            color = lineColor,
            start = Offset(left + d, height),
            end = Offset(left + d + height, 0f),
            strokeWidth = strokeW
        )
        d += step
    }
}

@Composable
private fun CieloEstrelladoLogin(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "estrellas")
    val fase by transicion.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400),
            repeatMode = RepeatMode.Restart
        ),
        label = "faseEstrellas"
    )
    val estrellas = remember {
        listOf(
            0.10f to 0.05f, 0.25f to 0.09f, 0.82f to 0.06f,
            0.90f to 0.12f, 0.18f to 0.18f, 0.77f to 0.19f,
            0.06f to 0.27f, 0.94f to 0.26f, 0.38f to 0.04f
        )
    }
    Canvas(modifier = modifier) {
        val radius = 1.3.dp.toPx()
        estrellas.forEachIndexed { i, (rx, ry) ->
            val wave = (sin(fase + i * 0.85f) + 1f) * 0.5f
            val alpha = 0.08f + 0.44f * wave
            drawCircle(
                color = GoldLogin.copy(alpha = alpha),
                radius = radius,
                center = Offset(size.width * rx, size.height * ry)
            )
        }
    }
}

@Composable
private fun PiramideLoginCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sx = w / 390f
        val sy = h / 360f
        val strokeColor = GoldLogin.copy(alpha = 0.14f)
        val strokeWidth = 0.8.dp.toPx()

        val outer = Path().apply {
            moveTo(195f * sx, 10f * sy)
            lineTo(20f * sx, 330f * sy)
            lineTo(370f * sx, 330f * sy)
            close()
        }
        drawPath(outer, color = strokeColor, style = Stroke(width = strokeWidth))

        val n = 8
        for (i in 1 until n) {
            val t = i.toFloat() / n.toFloat()
            val baseX = (20f + 350f * t) * sx
            val baseY = 330f * sy
            drawLine(
                color = strokeColor,
                start = Offset(baseX, baseY),
                end = Offset((195f + 175f * t) * sx, (10f + 320f * t) * sy),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = strokeColor,
                start = Offset(baseX, baseY),
                end = Offset((20f + 175f * t) * sx, (330f - 320f * t) * sy),
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
private fun GoogleMultiColorLogo(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        val stroke = with(density) { (size.minDimension * 0.21f) }
        val inset = stroke / 2f
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        // Rojo (arriba)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -140f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )
        // Amarillo (izquierda)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 145f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )
        // Verde (abajo)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 40f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )
        // Azul (derecha + barra horizontal)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -5f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )
        val midY = size.height * 0.5f
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.50f, midY),
            end = Offset(size.width * 0.96f, midY),
            strokeWidth = stroke
        )
    }
}
