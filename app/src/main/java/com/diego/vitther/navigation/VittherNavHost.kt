package com.diego.vitther.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.diego.vitther.domain.historiaDe
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.*
import com.diego.vitther.ui.screens.coleccion.*
import com.diego.vitther.ui.screens.historia.HistoriaScreen
import com.diego.vitther.ui.screens.home.HomeRoute
import com.diego.vitther.ui.screens.perfil.ProfileScreen
import com.diego.vitther.ui.screens.wishes.WishesRoute
import kotlinx.coroutines.launch

@Composable
fun VittherNavHost(
    vm: ColeccionViewModel = viewModel(factory = ColeccionViewModel.Factory),
    piezaPendiente: Long? = null,
    onPiezaPendienteConsumida: () -> Unit = {}
) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val piezas by vm.piezas.collectAsStateWithLifecycle()
    val capsulas by vm.capsulas.collectAsStateWithLifecycle()
    val ahora by vm.ahora.collectAsStateWithLifecycle()
    val ocupado by vm.ocupado.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(piezaPendiente) {
        piezaPendiente?.let {
            nav.navigate(Cedula(it))
            onPiezaPendienteConsumida()
        }
    }
    val detalle = destino?.hasRoute<Cedula>() == true || destino?.hasRoute<Registro>() == true
    val seleccion = when {
        destino?.hasRoute<Wishes>() == true || destino?.hasRoute<Categorias>() == true -> 1
        destino?.hasRoute<Coleccion>() == true || destino?.hasRoute<ColeccionCategoria>() == true -> 2
        destino?.hasRoute<Historia>() == true -> 3
        destino?.hasRoute<Perfil>() == true -> 4
        else -> 0
    }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it); vm.limpiarError() } }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!detalle) VittherNavigationBar(seleccion) { indice ->
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
    ) { padding ->
        NavHost(nav, startDestination = Inicio, modifier = Modifier.padding(padding)) {
            composable<Inicio> {
                HomeRoute(
                    onPiezaClick = { nav.navigate(Cedula(it)) },
                    onAbrirColeccion = {
                        nav.navigate(Coleccion) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
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
                    abrir = { nav.navigate(Cedula(it)) }
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
                    volver = { nav.popBackStack() }
                )
            }
            composable<Historia> {
                if (piezas == null || capsulas == null) CircularProgressIndicator()
                else HistoriaScreen(
                    hitos = remember(piezas, capsulas) { historiaDe(piezas.orEmpty(), capsulas.orEmpty()) },
                    piezas = piezas.orEmpty(),
                    archivo = vm::archivo,
                    volver = { nav.popBackStack() },
                    abrir = { nav.navigate(Cedula(it)) }
                )
            }
            composable<Perfil> {
                val estadoFirebase by vm.estadoFirebase.collectAsStateWithLifecycle()
                ProfileScreen(
                    totalPiezas = piezas?.size,
                    totalCategorias = piezas?.map { it.franquicia }?.distinct()?.size,
                    ahora = ahora,
                    estadoFirebase = estadoFirebase,
                    sincronizando = ocupado,
                    onSincronizarFirebase = { url -> vm.sincronizarConFirebase(url) }
                )
            }
            composable<Cedula> { entry ->
                val id = entry.toRoute<Cedula>().id
                if (piezas == null) CircularProgressIndicator() else CedulaScreen(
                    pieza = piezas?.find { it.id == id },
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
}
