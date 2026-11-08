package com.diego.vitther.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.ui.screens.perfil.ProfileScreen
import com.diego.vitther.ui.screens.home.HomeRoute
import com.diego.vitther.domain.historiaDe
import com.diego.vitther.ui.screens.historia.HistoriaScreen
import com.diego.vitther.ui.ColeccionViewModel
import com.diego.vitther.ui.components.*
import com.diego.vitther.ui.screens.coleccion.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
        destino?.hasRoute<Categorias>() == true -> 1
        destino?.hasRoute<Coleccion>() == true || destino?.hasRoute<ColeccionCategoria>() == true -> 2
        destino?.hasRoute<Historia>() == true -> 3
        destino?.hasRoute<Perfil>() == true -> 4
        else -> 0
    }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it); vm.limpiarError() } }
    Scaffold(
        topBar = { if (!detalle && seleccion != 0 && seleccion != 3 && seleccion != 4) TopAppBar(title = {
            Text(if (seleccion == 0) "VITTHER" else listOf("Inicio", "Categorías", "Colección", "Historia", "Perfil")[seleccion],
                style = if (seleccion == 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
        }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (!detalle) VittherNavigationBar(seleccion) { indice ->
            val ruta: Any = when (indice) { 0 -> Inicio; 1 -> Categorias; 2 -> Coleccion; 3 -> Historia; else -> Perfil }
            nav.navigate(ruta) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } }
    ) { padding ->
        NavHost(nav, startDestination = Inicio, modifier = Modifier.padding(padding)) {
            composable<Inicio> {
                HomeRoute(onPiezaClick = { nav.navigate(Cedula(it)) })
            }
            composable<Coleccion> {
                if (piezas == null) CircularProgressIndicator() else ColeccionScreen(piezas.orEmpty(), vm,
                    agregar = { nav.navigate(Registro()) }, abrir = { nav.navigate(Cedula(it)) })
            }
            composable<Categorias> {
                if (piezas == null) CircularProgressIndicator()
                else ListaCategorias(piezas.orEmpty(), vm) { nav.navigate(ColeccionCategoria(it)) }
            }
            composable<ColeccionCategoria> { entry ->
                val categoria = entry.toRoute<ColeccionCategoria>().franquicia
                if (piezas == null) CircularProgressIndicator() else ColeccionScreen(piezas.orEmpty(), vm,
                    agregar = { nav.navigate(Registro(franquicia = categoria)) }, abrir = { nav.navigate(Cedula(it)) },
                    categoria = categoria, volver = { nav.popBackStack() })
            }
            composable<Historia> {
                if (piezas == null || capsulas == null) CircularProgressIndicator()
                else HistoriaScreen(
                    hitos = remember(piezas, capsulas) { historiaDe(piezas.orEmpty(), capsulas.orEmpty()) },
                    piezas = piezas.orEmpty(), archivo = vm::archivo,
                    volver = { nav.popBackStack() }, abrir = { nav.navigate(Cedula(it)) })
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
                if (piezas == null) CircularProgressIndicator() else CedulaScreen(piezas?.find { it.id == id }, vm, ocupado,
                    capsulas = capsulas.orEmpty().filter { it.piezaId == id }, ahora = ahora,
                    volver = { nav.popBackStack() }, editar = { nav.navigate(Registro(it)) }, eliminado = {
                        nav.popBackStack()
                        scope.launch { snackbar.showSnackbar("Pieza eliminada") }
                    })
            }
            composable<Registro> { entry ->
                val ruta = entry.toRoute<Registro>()
                val id = ruta.id
                val pieza = piezas?.find { it.id == id }
                if (id != 0L && piezas == null) CircularProgressIndicator()
                else if (id != 0L && pieza == null) EstadoVacio("Pieza no disponible", "Vuelve a tu colección.", "Volver", { nav.popBackStack() })
                else RegistroScreen(pieza, vm, ocupado, franquiciaInicial = ruta.franquicia,
                    volver = { nav.popBackStack() }, guardado = { nuevoId ->
                        nav.popBackStack()
                        if (id == 0L) nav.navigate(Cedula(nuevoId))
                        scope.launch { snackbar.showSnackbar(if (id == 0L) "Pieza incorporada a tu museo" else "Cédula actualizada") }
                    })
            }
        }
    }
}

@Composable
private fun ListaCategorias(piezas: List<PiezaEntity>, vm: ColeccionViewModel, abrir: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (piezas.isEmpty()) item { EstadoVacio("Sin categorías todavía", "Aparecerán al registrar tu primera pieza.") }
        items(piezas.groupBy { it.franquicia }.toSortedMap().entries.toList(), key = { it.key }) { (nombre, grupo) ->
            OutlinedCard(onClick = { abrir(nombre) }, modifier = Modifier.fillMaxWidth()) {
                Text(nombre, Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                Text("${grupo.size} piezas", Modifier.padding(start = 16.dp, bottom = 16.dp))
                Row(Modifier.padding(start = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    grupo.take(3).forEach { pieza ->
                        FotoPieza(vm.archivo(pieza.fotoPath), pieza.nombre, Modifier.size(48.dp).clip(CircleShape))
                    }
                }
            }
        }
    }
}
