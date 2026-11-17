package com.diego.vitther.data.repository

import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.data.remote.api.CatalogoExternoApi
import com.diego.vitther.data.remote.api.FirebaseMuseoApi
import com.diego.vitther.data.remote.api.VittherRemoteApiClient
import com.diego.vitther.data.remote.api.toCloudDto
import com.diego.vitther.data.remote.dto.ColeccionCompartidaDto
import com.diego.vitther.data.remote.dto.RespaldoFirebaseDto
import com.diego.vitther.domain.CapsulaVista

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
        capsulas: List<CapsulaVista> = emptyList(),
        wishes: List<WishEntity> = emptyList(),
        coleccionista: String = "Curador VITTHER",
        fotosPiezas: Map<Long, String?> = emptyMap(),
        fotosWishes: Map<Long, String?> = emptyMap(),
        uid: String? = null,
        nombreMuseo: String? = null
    ): ResultadoSincronizacionFirebase {
        val ahora = System.currentTimeMillis()
        val payload = RespaldoFirebaseDto(
            coleccionista = coleccionista,
            versionApp = "26.2.1",
            actualizadoEn = ahora,
            totalPiezas = piezas.size,
            totalCapsulas = capsulas.size,
            totalWishes = wishes.size,
            piezas = piezas.map { it.toCloudDto(ahora, fotosPiezas[it.id]) },
            capsulas = capsulas.map { it.toCloudDto() },
            wishes = wishes.map { it.toCloudDto(fotosWishes[it.id]) },
            nombreMuseo = nombreMuseo
        )
        val exitoRemoto = firebaseApi.sincronizarRespaldoUsuario(uid, payload)
        val mensaje = if (exitoRemoto) {
            "Sincronización completada con Firebase (${piezas.size} piezas, ${capsulas.size} cápsulas y ${wishes.size} wishes)."
        } else {
            "Instantánea lista (${piezas.size} piezas, ${capsulas.size} cápsulas, ${wishes.size} wishes). Revisa tu conexión a internet."
        }
        return ResultadoSincronizacionFirebase(
            sincronizadoRemoto = exitoRemoto,
            totalPiezasRespaldadas = piezas.size,
            timestamp = ahora,
            mensaje = mensaje
        )
    }

    suspend fun obtenerRespaldoDeFirebase(uid: String? = null): RespaldoFirebaseDto? =
        firebaseApi.obtenerRespaldoUsuario(uid)

    suspend fun publicarExposicionCompartida(expo: ColeccionCompartidaDto): Boolean =
        firebaseApi.publicarExposicionCompartida(expo)

    suspend fun obtenerExposicionCompartida(codigo: String): ColeccionCompartidaDto? =
        firebaseApi.obtenerExposicionCompartida(codigo)
}
