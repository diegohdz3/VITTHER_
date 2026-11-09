package com.diego.vitther.di

import android.content.Context
import com.diego.vitther.data.local.VittherDatabase
import com.diego.vitther.data.remote.api.VittherRemoteApiClient
import com.diego.vitther.data.repository.CapsulaRepository
import com.diego.vitther.data.repository.CatalogoRemotoRepository
import com.diego.vitther.data.repository.PiezaRepository
import com.diego.vitther.data.repository.WishRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Inyección de dependencias manual.
 * La aplicación inicia completamente limpia (de fábrica) sin datos precargados,
 * lista para que el usuario registre su propia colección desde cero.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val baseDatos = VittherDatabase.crear(appContext)
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val remoteClient = VittherRemoteApiClient()

    val piezaRepository = PiezaRepository(baseDatos.piezaDao())
    val capsulaRepository = CapsulaRepository(baseDatos.capsulaDao())
    val wishRepository = WishRepository(baseDatos.wishDao())
    val catalogoRemotoRepository = CatalogoRemotoRepository(remoteClient, remoteClient)
    val fotos = com.diego.vitther.data.media.FotoStore(appContext)

    fun sincronizarEnSegundoPlano() {
        alcance.launch {
            runCatching {
                val piezasActuales = piezaRepository.observarTodas().first()
                val capsulasActuales = capsulaRepository.observarTodas().first()
                val wishesActuales = wishRepository.observarTodos().first()
                catalogoRemotoRepository.respaldarEnFirebase(
                    piezas = piezasActuales,
                    capsulas = capsulasActuales,
                    wishes = wishesActuales
                )
            }
        }
    }
}
