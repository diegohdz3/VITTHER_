package com.diego.vitther.data.remote.api

import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.data.local.WishEntity
import com.diego.vitther.data.remote.dto.CapsulaCloudDto
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
}

class VittherRemoteApiClient(
    private val firebaseBaseUrl: String = "https://vitther-museo-default-rtdb.firebaseio.com",
    private val jikanBaseUrl: String = "https://api.jikan.moe/v4"
) : CatalogoExternoApi, FirebaseMuseoApi {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val baseLimpia: String
        get() = firebaseBaseUrl.trim().removeSuffix("/").removeSuffix("/museos/default.json").removeSuffix(".json")

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
        withContext(Dispatchers.IO) {
            val endpoint = URL("$baseLimpia/museos/default.json")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                val payload = json.encodeToString(respaldo)
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload) }
                conn.responseCode in 200..299
            } catch (_: Exception) {
                false
            } finally {
                conn.disconnect()
            }
        }

    override suspend fun obtenerRespaldo(): RespaldoFirebaseDto? =
        withContext(Dispatchers.IO) {
            val endpoint = URL("$baseLimpia/museos/default.json")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode in 200..299) {
                    val cuerpo = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    if (cuerpo.isBlank() || cuerpo.trim() == "null") null
                    else json.decodeFromString<RespaldoFirebaseDto>(cuerpo)
                } else null
            } catch (_: Exception) {
                null
            } finally {
                conn.disconnect()
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

fun PiezaEntity.toCloudDto(ahora: Long = System.currentTimeMillis()): PiezaCloudDto =
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
        sincronizadoEn = ahora
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

fun WishEntity.toCloudDto(): WishCloudDto =
    WishCloudDto(
        idLocal = id,
        nombre = nombre,
        franquicia = franquicia,
        linea = linea,
        estado = estado.name,
        metaPesos = metaPesos,
        progresoPorcentaje = progresoPorcentaje,
        fechaEstimadaLlegada = fechaEstimadaLlegada,
        creadoEn = creadoEn
    )
