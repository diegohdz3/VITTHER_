package com.diego.vitther.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.withTransaction
import com.diego.vitther.data.local.SeedData
import com.diego.vitther.data.local.VittherDatabase
import com.diego.vitther.data.repository.CapsulaRepository
import com.diego.vitther.data.repository.PiezaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Inyección de dependencias manual. Para un proyecto individual esta es la
 * decisión correcta: una sola clase, sin KSP extra ni curva de Hilt.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val baseDatos = VittherDatabase.crear(appContext)
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val piezaRepository = PiezaRepository(baseDatos.piezaDao())
    val capsulaRepository = CapsulaRepository(baseDatos.capsulaDao())
    val fotos = com.diego.vitther.data.media.FotoStore(appContext)

    init {
        val esDepurable = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (esDepurable) {
            alcance.launch {
                val preferencias = appContext.getSharedPreferences("demo", Context.MODE_PRIVATE)
                if (!preferencias.getBoolean("sembrado", false)) {
                    baseDatos.withTransaction {
                        SeedData.sembrarSiEstaVacio(baseDatos.piezaDao(), baseDatos.capsulaDao())
                    }
                    preferencias.edit().putBoolean("sembrado", true).apply()
                }
            }
        }
    }
}
