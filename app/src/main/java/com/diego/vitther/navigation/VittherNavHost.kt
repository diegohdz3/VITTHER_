package com.diego.vitther.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.diego.vitther.domain.historiaDe
import com.diego.vitther.integration.CompartirExposicionSheet
import com.diego.vitther.integration.DatosExposicionCompartible
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.*
import com.diego.vitther.ui.screens.auth.LoginScreen
import com.diego.vitther.ui.screens.coleccion.*
import com.diego.vitther.ui.screens.historia.HistoriaScreen
import com.diego.vitther.ui.screens.historia.temaAccentHex
import com.diego.vitther.ui.screens.home.HomeRoute
import com.diego.vitther.ui.screens.perfil.ProfileIdentity
import com.diego.vitther.ui.screens.perfil.ProfileScreen
import com.diego.vitther.ui.screens.wishes.WishesRoute
import kotlinx.coroutines.launch

@Composable
fun VittherNavHost(
    vm: ColeccionViewModel = viewModel(factory = ColeccionViewModel.Factory),
    piezaPendiente: Long? = null,
    onPiezaPendienteConsumida: () -> Unit = {}
) {
    val context = LocalContext.current
    val usuarioActual by vm.usuarioActual.collectAsStateWithLifecycle()
    val cuentasRecordadas by vm.cuentasRecordadas.collectAsStateWithLifecycle()
    val exposicionInvitada by vm.exposicionInvitada.collectAsStateWithLifecycle()

    var mostrarLogin by remember { mutableStateOf(vm.usuarioActual.value == null) }
    LaunchedEffect(usuarioActual) {
        if (usuarioActual == null) mostrarLogin = true
    }

    val error by vm.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it); vm.limpiarError() } }

    AnimatedContent(
        targetState = mostrarLogin,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (!targetState) {
                (fadeIn(tween(700)) + scaleIn(initialScale = 0.88f, animationSpec = tween(700)))
                    .togetherWith(fadeOut(tween(550)) + scaleOut(targetScale = 1.14f, animationSpec = tween(550)))
            } else {
                fadeIn(tween(400)).togetherWith(fadeOut(tween(350)))
            }
        },
        label = "transicionLoginMuseo"
    ) { enLogin ->
        if (enLogin) {
            LoginScreen(
                cuentasRecordadas = cuentasRecordadas,
                onConfirmarCuentaGoogle = { nombre, correo -> vm.iniciarSesionGoogle(nombre, correo) },
                onSeleccionarCuentaRecordada = { vm.seleccionarCuentaGoogle(it) },
                onPortalAbiertoYListo = { cuenta ->
                    mostrarLogin = false
                    vm.sincronizarAlIniciarSesion(cuenta)
                }
            )
        } else {
            val nav = rememberNavController()
            val entrada by nav.currentBackStackEntryAsState()
            val destino = entrada?.destination
            val piezas by vm.piezas.collectAsStateWithLifecycle()
            val capsulas by vm.capsulas.collectAsStateWithLifecycle()
            val wishes by vm.wishes.collectAsStateWithLifecycle()
            val ahora by vm.ahora.collectAsStateWithLifecycle()
            val ocupado by vm.ocupado.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            LaunchedEffect(piezaPendiente) {
                piezaPendiente?.let {
                    nav.navigate(Cedula(it))
                    onPiezaPendienteConsumida()
                }
            }

            var datosParaCompartir by remember { mutableStateOf<DatosExposicionCompartible?>(null) }

            val resolverNombresMuseo: () -> Pair<String, String> = {
                val prefsPerfil = context.getSharedPreferences("vitther_perfil_prefs", Context.MODE_PRIVATE)
                val curador = usuarioActual?.nombre ?: "Curador"
                val nombreMuseo = prefsPerfil.getString("nombre_museo", null)
                    ?: "Museo de $curador"
                curador to nombreMuseo
            }

            val abrirSheetFranquicia: (String?) -> Unit = { franquicia ->
                val listaActual = piezas.orEmpty()
                val piezasFiltradas = if (franquicia == null) {
                    listaActual
                } else {
                    listaActual.filter { it.franquicia.equals(franquicia, ignoreCase = true) }
                }
                val (curador, nombreMuseo) = resolverNombresMuseo()
                if (piezasFiltradas.isEmpty()) {
                    scope.launch { snackbar.showSnackbar("Agrega piezas antes de compartir esta vitrina") }
                } else {
                    datosParaCompartir = DatosExposicionCompartible(
                        titulo = if (franquicia == null) nombreMuseo else "Sala $franquicia",
                        subtitulo = if (franquicia == null) {
                            "Catálogo completo del museo · ${piezasFiltradas.size} piezas"
                        } else {
                            "Selección curatorial de $franquicia · ${piezasFiltradas.size} piezas"
                        },
                        curador = curador,
                        piezas = piezasFiltradas,
                        colorAcentoHex = "#E8C97A",
                        franquiciaFiltro = franquicia
                    )
                }
            }

            val detalle = destino?.hasRoute<Cedula>() == true || destino?.hasRoute<Registro>() == true
            val seleccion = indiceRuta(destino).let { if (it >= 0) it else 0 }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    AnimatedVisibility(
                        visible = !detalle,
                        enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(240)),
                        exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(180))
                    ) {
                        VittherNavigationBar(seleccion) { indice ->
                            val ruta: Any = when (indice) {
                                0 -> Inicio
                                1 -> Wishes
                                2 -> Coleccion
                                3 -> Historia
                                else -> Perfil
                            }
                            nav.navigate(ruta) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = Inicio,
                    modifier = Modifier.padding(padding),
                    enterTransition = {
                        val from = indiceRuta(initialState.destination)
                        val to = indiceRuta(targetState.destination)
                        when {
                            targetState.destination.hasRoute<Cedula>() ->
                                scaleIn(initialScale = 0.92f, animationSpec = tween(360, easing = FastOutSlowInEasing)) +
                                    fadeIn(tween(320))
                            targetState.destination.hasRoute<Registro>() ->
                                slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 5 } +
                                    fadeIn(tween(320))
                            from >= 0 && to >= 0 && from != to -> {
                                val haciaDerecha = to > from
                                slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) {
                                    if (haciaDerecha) it / 4 else -it / 4
                                } + fadeIn(tween(300))
                            }
                            else -> fadeIn(tween(280))
                        }
                    },
                    exitTransition = {
                        val from = indiceRuta(initialState.destination)
                        val to = indiceRuta(targetState.destination)
                        when {
                            targetState.destination.hasRoute<Cedula>() ->
                                scaleOut(targetScale = 0.96f, animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                                    fadeOut(tween(260))
                            targetState.destination.hasRoute<Registro>() ->
                                fadeOut(tween(260))
                            from >= 0 && to >= 0 && from != to -> {
                                val haciaDerecha = to > from
                                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) {
                                    if (haciaDerecha) -it / 5 else it / 5
                                } + fadeOut(tween(240))
                            }
                            else -> fadeOut(tween(220))
                        }
                    },
                    popEnterTransition = {
                        val from = indiceRuta(initialState.destination)
                        val to = indiceRuta(targetState.destination)
                        when {
                            initialState.destination.hasRoute<Cedula>() ->
                                scaleIn(initialScale = 0.96f, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                    fadeIn(tween(260))
                            initialState.destination.hasRoute<Registro>() ->
                                fadeIn(tween(280))
                            from >= 0 && to >= 0 && from != to -> {
                                val haciaDerecha = to > from
                                slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) {
                                    if (haciaDerecha) it / 4 else -it / 4
                                } + fadeIn(tween(280))
                            }
                            else -> fadeIn(tween(260))
                        }
                    },
                    popExitTransition = {
                        when {
                            initialState.destination.hasRoute<Cedula>() ->
                                scaleOut(targetScale = 0.92f, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                    fadeOut(tween(240))
                            initialState.destination.hasRoute<Registro>() ->
                                slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it / 5 } +
                                    fadeOut(tween(240))
                            else -> fadeOut(tween(240))
                        }
                    }
                ) {
                    composable<Inicio> {
                        HomeRoute(
                            onPiezaClick = { nav.navigate(Cedula(it)) },
                            onAbrirColeccion = {
                                nav.navigate(Coleccion) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onAgregarPieza = { nav.navigate(Registro()) }
                        )
                    }
                    composable<Wishes> {
                        WishesRoute(abrirCedula = { nav.navigate(Cedula(it)) })
                    }
                    composable<Categorias> {
                        WishesRoute(abrirCedula = { nav.navigate(Cedula(it)) })
                    }
                    composable<Coleccion> {
                        if (piezas == null) CircularProgressIndicator() else ColeccionScreen(
                            piezas = piezas.orEmpty(),
                            vm = vm,
                            agregar = { nav.navigate(Registro()) },
                            abrir = { nav.navigate(Cedula(it)) },
                            agregarEnSala = { franq -> nav.navigate(Registro(franquicia = franq)) },
                            onCompartirSala = abrirSheetFranquicia,
                            onAbrirExposicion = { codigo -> vm.abrirExposicionPorCodigo(codigo) }
                        )
                    }
                    composable<ColeccionCategoria> { entry ->
                        val categoria = entry.toRoute<ColeccionCategoria>().franquicia
                        if (piezas == null) CircularProgressIndicator() else ColeccionScreen(
                            piezas = piezas.orEmpty(),
                            vm = vm,
                            agregar = { nav.navigate(Registro(franquicia = categoria)) },
                            abrir = { nav.navigate(Cedula(it)) },
                            categoria = categoria,
                            volver = { nav.popBackStack() },
                            agregarEnSala = { franq -> nav.navigate(Registro(franquicia = franq)) },
                            onCompartirSala = abrirSheetFranquicia,
                            onAbrirExposicion = { codigo -> vm.abrirExposicionPorCodigo(codigo) }
                        )
                    }
                    composable<Historia> {
                        if (piezas == null || capsulas == null) CircularProgressIndicator()
                        else HistoriaScreen(
                            hitos = remember(piezas, capsulas) { historiaDe(piezas.orEmpty(), capsulas.orEmpty()) },
                            piezas = piezas.orEmpty(),
                            archivo = vm::archivo,
                            volver = { nav.popBackStack() },
                            abrir = { nav.navigate(Cedula(it)) },
                            onCompartirRecorrido = { rec, piezasOrdenadas ->
                                val (curador, _) = resolverNombresMuseo()
                                datosParaCompartir = DatosExposicionCompartible(
                                    titulo = rec.nombre.ifBlank { "Recorrido VITTHER" },
                                    subtitulo = rec.subtitulo.ifBlank {
                                        "Recorrido guiado · ${piezasOrdenadas.size} paradas"
                                    },
                                    curador = curador,
                                    piezas = piezasOrdenadas,
                                    colorAcentoHex = temaAccentHex(rec.tema),
                                    franquiciaFiltro = null
                                )
                            },
                            capsulas = capsulas.orEmpty(),
                            wishes = wishes,
                            ahora = ahora
                        )
                    }
                    composable<Perfil> {
                        val estadoFirebase by vm.estadoFirebase.collectAsStateWithLifecycle()
                        ProfileScreen(
                            totalPiezas = piezas?.size,
                            totalCategorias = piezas?.map { it.franquicia }?.distinct()?.size,
                            ahora = ahora,
                            identity = ProfileIdentity(
                                nombre = usuarioActual?.nombre,
                                foto = usuarioActual?.fotoUrl,
                                correo = usuarioActual?.correo ?: "Curador · VITTHER Cloud"
                            ),
                            estadoFirebase = estadoFirebase,
                            sincronizando = ocupado,
                            onSincronizarFirebase = { url -> vm.sincronizarConFirebase(url) },
                            onRestaurarFirebase = { url -> vm.restaurarDesdeFirebase(url) },
                            onCompartirMuseo = { abrirSheetFranquicia(null) },
                            onAbrirExposicion = { codigo -> vm.abrirExposicionPorCodigo(codigo) },
                            onCerrarSesion = { vm.cerrarSesionGoogle() }
                        )
                    }
                    composable<Cedula> { entry ->
                        val id = entry.toRoute<Cedula>().id
                        if (piezas == null) CircularProgressIndicator() else CedulaScreen(
                            pieza = piezas?.find { it.id == id },
                            todasLasPiezas = piezas.orEmpty(),
                            vm = vm,
                            ocupado = ocupado,
                            capsulas = capsulas.orEmpty().filter { it.piezaId == id },
                            ahora = ahora,
                            volver = { nav.popBackStack() },
                            editar = { nav.navigate(Registro(it)) },
                            eliminado = {
                                nav.popBackStack()
                                scope.launch { snackbar.showSnackbar("Pieza eliminada") }
                            }
                        )
                    }
                    composable<Registro> { entry ->
                        val ruta = entry.toRoute<Registro>()
                        val id = ruta.id
                        val pieza = piezas?.find { it.id == id }
                        if (id != 0L && piezas == null) CircularProgressIndicator()
                        else if (id != 0L && pieza == null) EstadoVacio("Pieza no disponible", "Vuelve a tu colección.", "Volver", { nav.popBackStack() })
                        else RegistroScreen(
                            pieza = pieza,
                            todasLasPiezas = piezas.orEmpty(),
                            vm = vm,
                            ocupado = ocupado,
                            franquiciaInicial = ruta.franquicia,
                            volver = { nav.popBackStack() },
                            guardado = { nuevoId ->
                                nav.popBackStack()
                                if (id == 0L) nav.navigate(Cedula(nuevoId))
                                scope.launch { snackbar.showSnackbar(if (id == 0L) "Pieza incorporada a tu museo" else "Cédula actualizada") }
                            }
                        )
                    }
                }
            }

            datosParaCompartir?.let { datos ->
                CompartirExposicionSheet(
                    datos = datos,
                    resolverArchivo = vm::archivo,
                    onCompartirLinkVitther = {
                        datosParaCompartir = null
                        val (curador, nombreMuseo) = resolverNombresMuseo()
                        val piezasExplicitas = if (datos.franquiciaFiltro == null) datos.piezas else null
                        val tituloPersonalizado = if (datos.franquiciaFiltro == null && datos.titulo != nombreMuseo) {
                            datos.titulo
                        } else {
                            null
                        }
                        vm.compartirSalaOColeccion(
                            franquicia = datos.franquiciaFiltro,
                            uid = usuarioActual?.uid ?: "local",
                            curador = curador,
                            nombreMuseo = nombreMuseo,
                            piezasExplicitas = piezasExplicitas,
                            tituloPersonalizado = tituloPersonalizado
                        ) { _, mensajeShare ->
                            runCatching {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Invitación a mi museo VITTHER")
                                    putExtra(Intent.EXTRA_TEXT, mensajeShare)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Compartir colección (Solo lectura)")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    },
                    onDismiss = { datosParaCompartir = null }
                )
            }
        }
    }

    exposicionInvitada?.let { expo ->
        ExposicionCompartidaDialog(
            exposicion = expo,
            onCerrar = { vm.cerrarExposicionInvitada() }
        )
    }
}

private fun indiceRuta(destino: NavDestination?): Int = when {
    destino == null -> -1
    destino.hasRoute<Inicio>() -> 0
    destino.hasRoute<Wishes>() || destino.hasRoute<Categorias>() -> 1
    destino.hasRoute<Coleccion>() || destino.hasRoute<ColeccionCategoria>() -> 2
    destino.hasRoute<Historia>() -> 3
    destino.hasRoute<Perfil>() -> 4
    else -> -1
}
