package com.diego.vitther.data.repository

import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.remote.api.CatalogoExternoApi
import com.diego.vitther.data.remote.api.FirebaseMuseoApi
import com.diego.vitther.data.remote.api.VittherRemoteApiClient
import com.diego.vitther.data.remote.api.toCloudDto
import com.diego.vitther.data.remote.dto.RespaldoFirebaseDto

/**
 * Ficha sugerida desde la API externa (Jikan REST API) o catálogo curado de respaldo
 * para autocompletar datos al registrar una pieza en el museo.
 */
data class FichaCatalogoRemoto(
    val nombre: String,
    val franquicia: String,
    val lineaSugerida: String,
    val notaSugerida: String,
    val popularidad: Int
)

data class ResultadoSincronizacionFirebase(
    val sincronizadoRemoto: Boolean,
    val totalPiezasRespaldadas: Int,
    val timestamp: Long,
    val mensaje: String
)

class CatalogoRemotoRepository(
    private val catalogoApi: CatalogoExternoApi = VittherRemoteApiClient(),
    private val firebaseApi: FirebaseMuseoApi = VittherRemoteApiClient()
) {

    suspend fun buscarEnCatalogoOnline(consulta: String): Result<List<FichaCatalogoRemoto>> =
        runCatching {
            val remotos = catalogoApi.buscarPersonajesColeccionables(consulta)
            remotos.map { dto ->
                val resumen = dto.about
                    ?.lineSequence()
                    ?.firstOrNull { it.isNotBlank() }
                    ?.take(160)
                    ?: "Pieza verificada en el catálogo internacional de coleccionistas."
                val franquiciaInferida = dto.nicknames.firstOrNull() ?: "Edición de Colección"
                FichaCatalogoRemoto(
                    nombre = dto.name,
                    franquicia = franquiciaInferida,
                    lineaSugerida = "S.H. Figuarts / Escala 1/7",
                    notaSugerida = resumen,
                    popularidad = dto.favorites
                )
            }
        }

    suspend fun respaldarEnFirebase(
        piezas: List<PiezaEntity>,
        coleccionista: String = "Diego Cortés Hernández"
    ): ResultadoSincronizacionFirebase {
        val ahora = System.currentTimeMillis()
        val payload = RespaldoFirebaseDto(
            coleccionista = coleccionista,
            versionApp = "26.1.1",
            actualizadoEn = ahora,
            totalPiezas = piezas.size,
            piezas = piezas.map { it.toCloudDto(ahora) }
        )
        val exitoRemoto = firebaseApi.sincronizarRespaldo(payload)
        val mensaje = if (exitoRemoto) {
            "Sincronización completada con Firebase Realtime Database (${piezas.size} piezas respaldadas)."
        } else {
            "Instantánea de respaldo preparada (${piezas.size} piezas). Configura las reglas públicas de tu nodo Firebase o verifica tu conexión."
        }
        return ResultadoSincronizacionFirebase(
            sincronizadoRemoto = exitoRemoto,
            totalPiezasRespaldadas = piezas.size,
            timestamp = ahora,
            mensaje = mensaje
        )
    }
}
