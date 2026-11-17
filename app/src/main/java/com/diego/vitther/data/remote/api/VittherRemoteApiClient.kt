package com.diego.vitther.data.remote.api

import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.data.remote.dto.CapsulaCloudDto
import com.diego.vitther.data.remote.dto.ColeccionCompartidaDto
import com.diego.vitther.data.remote.dto.JikanCharacterDto
import com.diego.vitther.data.remote.dto.JikanCharacterSearchResponseDto
import com.diego.vitther.data.remote.dto.PiezaCloudDto
import com.diego.vitther.data.remote.dto.RespaldoFirebaseDto
import com.diego.vitther.data.remote.dto.WishCloudDto
import com.diego.vitther.domain.CapsulaVista
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Contratos de cliente HTTP para consumo de API externa de figuras/personajes (Jikan REST API v4)
 * y backend en la nube con Firebase Realtime Database REST API.
 */
interface CatalogoExternoApi {
    suspend fun buscarPersonajesColeccionables(consulta: String): List<JikanCharacterDto>
}

interface FirebaseMuseoApi {
    suspend fun sincronizarRespaldo(respaldo: RespaldoFirebaseDto): Boolean
    suspend fun obtenerRespaldo(): RespaldoFirebaseDto?
    suspend fun consultarEstadoConexion(): Boolean

    suspend fun sincronizarRespaldoUsuario(uid: String?, respaldo: RespaldoFirebaseDto): Boolean =
        sincronizarRespaldo(respaldo)

    suspend fun obtenerRespaldoUsuario(uid: String?): RespaldoFirebaseDto? =
        obtenerRespaldo()

    suspend fun publicarExposicionCompartida(expo: ColeccionCompartidaDto): Boolean = false

    suspend fun obtenerExposicionCompartida(codigo: String): ColeccionCompartidaDto? = null
}

class VittherRemoteApiClient(
    private val firebaseBaseUrl: String = "https://vitther-default-rtdb.firebaseio.com",
    private val jikanBaseUrl: String = "https://api.jikan.moe/v4"
) : CatalogoExternoApi, FirebaseMuseoApi {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val baseLimpia: String
        get() = firebaseBaseUrl.trim().removeSuffix("/").removeSuffix("/museos/default.json").removeSuffix(".json")

    private fun rutaMuseo(uid: String?): String {
        val limpiaUid = uid?.trim()?.replace(Regex("[^a-zA-Z0-9_-]"), "")
        return if (!limpiaUid.isNullOrBlank()) "museos/usuarios/$limpiaUid.json" else "museos/default.json"
    }

    private fun sanitizarCodigo(codigo: String): String =
        codigo.trim().uppercase().replace(Regex("[^A-Z0-9_-]"), "")

    private suspend fun putJson(rutaRelativa: String, payloadJson: String): Boolean =
        withContext(Dispatchers.IO) {
            val endpoint = URL("$baseLimpia/$rutaRelativa")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payloadJson) }
                conn.responseCode in 200..299
            } catch (_: Exception) {
                false
            } finally {
                conn.disconnect()
            }
        }

    private suspend fun getJson(rutaRelativa: String): String? =
        withContext(Dispatchers.IO) {
            val endpoint = URL("$baseLimpia/$rutaRelativa")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode in 200..299) {
                    val cuerpo = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    cuerpo.takeUnless { it.isBlank() || it.trim() == "null" }
                } else null
            } catch (_: Exception) {
                null
            } finally {
                conn.disconnect()
            }
        }

    override suspend fun buscarPersonajesColeccionables(consulta: String): List<JikanCharacterDto> =
        withContext(Dispatchers.IO) {
            val limpia = consulta.trim()
            if (limpia.isBlank()) return@withContext emptyList()
            val encoded = URLEncoder.encode(limpia, "UTF-8")
            val endpoint = URL("$jikanBaseUrl/characters?q=$encoded&limit=5&order_by=favorites&sort=desc")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode in 200..299) {
                    val cuerpo = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    json.decodeFromString<JikanCharacterSearchResponseDto>(cuerpo).data
                } else {
                    throw IllegalStateException("El servicio de catálogo respondió con código ${conn.responseCode}.")
                }
            } finally {
                conn.disconnect()
            }
        }

    override suspend fun sincronizarRespaldo(respaldo: RespaldoFirebaseDto): Boolean =
        sincronizarRespaldoUsuario(null, respaldo)

    override suspend fun obtenerRespaldo(): RespaldoFirebaseDto? =
        obtenerRespaldoUsuario(null)

    override suspend fun sincronizarRespaldoUsuario(
        uid: String?,
        respaldo: RespaldoFirebaseDto
    ): Boolean = putJson(rutaMuseo(uid), json.encodeToString(respaldo))

    override suspend fun obtenerRespaldoUsuario(uid: String?): RespaldoFirebaseDto? =
        getJson(rutaMuseo(uid))?.let { cuerpo ->
            runCatching { json.decodeFromString<RespaldoFirebaseDto>(cuerpo) }.getOrNull()
        }

    override suspend fun publicarExposicionCompartida(expo: ColeccionCompartidaDto): Boolean {
        val code = sanitizarCodigo(expo.codigo)
        if (code.isBlank()) return false
        return putJson("compartidas/$code.json", json.encodeToString(expo.copy(codigo = code)))
    }

    override suspend fun obtenerExposicionCompartida(codigo: String): ColeccionCompartidaDto? {
        val code = sanitizarCodigo(codigo)
        if (code.isBlank()) return null
        return getJson("compartidas/$code.json")?.let { cuerpo ->
            runCatching { json.decodeFromString<ColeccionCompartidaDto>(cuerpo) }.getOrNull()
        }
    }

    override suspend fun consultarEstadoConexion(): Boolean =
        withContext(Dispatchers.IO) {
            val endpoint = URL("$jikanBaseUrl/characters?limit=1")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4_000
                readTimeout = 4_000
            }
            try {
                conn.responseCode in 200..299
            } catch (_: Exception) {
                false
            } finally {
                conn.disconnect()
            }
        }
}

fun PiezaEntity.toCloudDto(
    ahora: Long = System.currentTimeMillis(),
    fotoBase64: String? = null
): PiezaCloudDto =
    PiezaCloudDto(
        idLocal = id,
        nombre = nombre,
        franquicia = franquicia,
        linea = linea,
        fechaAdquisicion = fechaAdquisicion,
        estado = estado.name,
        ubicacion = ubicacion,
        procedencia = procedencia,
        edicionLimitada = edicionLimitada,
        numeroEdicion = numeroEdicion,
        valorEstimadoCentavos = valorEstimadoCentavos,
        notaPersonal = notaPersonal,
        destacada = destacada,
        sincronizadoEn = ahora,
        fotoBase64 = fotoBase64
    )

fun CapsulaVista.toCloudDto(): CapsulaCloudDto =
    CapsulaCloudDto(
        idLocal = id,
        piezaId = piezaId,
        titulo = titulo,
        fechaApertura = fechaApertura,
        abiertaEn = abiertaEn,
        notaRevelada = nota
    )

fun WishEntity.toCloudDto(fotoBase64: String? = null): WishCloudDto =
    WishCloudDto(
        idLocal = id,
        nombre = nombre,
        franquicia = franquicia,
        linea = linea,
        estado = estado.name,
        metaPesos = metaPesos,
        progresoPorcentaje = progresoPorcentaje,
        fechaEstimadaLlegada = fechaEstimadaLlegada,
        creadoEn = creadoEn,
        fotoBase64 = fotoBase64
    )
