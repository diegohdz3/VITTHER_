package com.diego.vitther.data.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.Locale

@Serializable
data class VittherUserAccount(
    val uid: String,
    val nombre: String,
    val correo: String,
    val fotoUrl: String? = null
)

/**
 * Gestor de autenticación de curadores mediante cuenta de Google.
 * Persiste la sesión activa y cuentas recordadas en SharedPreferences privadas,
 * generando un UID determinístico por correo para vincular cada respaldo en Firebase
 * (`/museos/usuarios/{uid}.json`).
 */
class VittherAuthManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _usuarioActual = MutableStateFlow(leerCuentaActiva())
    val usuarioActual: StateFlow<VittherUserAccount?> = _usuarioActual.asStateFlow()

    private val _cuentasRecordadas = MutableStateFlow(leerCuentasRecordadas())
    val cuentasRecordadas: StateFlow<List<VittherUserAccount>> = _cuentasRecordadas.asStateFlow()

    fun iniciarSesionConGoogle(
        nombre: String,
        correo: String,
        fotoUrl: String? = null
    ): VittherUserAccount {
        val correoLimpio = correo.trim().lowercase(Locale.ROOT)
        require(correoLimpio.contains("@") && correoLimpio.substringAfter("@").contains(".")) {
            "Ingresa un correo de Google válido."
        }
        val nombreLimpio = nombre.trim().ifBlank {
            correoLimpio.substringBefore("@")
                .replace('.', ' ')
                .replace('_', ' ')
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { palabra ->
                    palabra.replaceFirstChar { c -> c.uppercaseChar() }
                }
                .ifBlank { "Curador" }
        }
        val cuenta = VittherUserAccount(
            uid = generarUidDesdeCorreo(correoLimpio),
            nombre = nombreLimpio,
            correo = correoLimpio,
            fotoUrl = fotoUrl
        )
        guardarCuentaActiva(cuenta)
        registrarEnHistorial(cuenta)
        return cuenta
    }

    fun seleccionarCuentaRecordada(cuenta: VittherUserAccount): VittherUserAccount {
        guardarCuentaActiva(cuenta)
        registrarEnHistorial(cuenta)
        return cuenta
    }

    fun cerrarSesion() {
        prefs.edit().remove(KEY_CUENTA_ACTIVA).apply()
        _usuarioActual.value = null
    }

    private fun guardarCuentaActiva(cuenta: VittherUserAccount) {
        prefs.edit().putString(KEY_CUENTA_ACTIVA, json.encodeToString(cuenta)).apply()
        _usuarioActual.value = cuenta
    }

    private fun registrarEnHistorial(cuenta: VittherUserAccount) {
        val actuales = leerCuentasRecordadas()
            .filterNot { it.uid == cuenta.uid || it.correo.equals(cuenta.correo, ignoreCase = true) }
        val actualizadas = listOf(cuenta) + actuales
        prefs.edit().putString(KEY_CUENTAS_RECORDADAS, json.encodeToString(actualizadas)).apply()
        _cuentasRecordadas.value = actualizadas
    }

    private fun leerCuentaActiva(): VittherUserAccount? {
        val raw = prefs.getString(KEY_CUENTA_ACTIVA, null) ?: return null
        return runCatching { json.decodeFromString<VittherUserAccount>(raw) }.getOrNull()
    }

    private fun leerCuentasRecordadas(): List<VittherUserAccount> {
        val raw = prefs.getString(KEY_CUENTAS_RECORDADAS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<VittherUserAccount>>(raw) }
            .getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_AUTH = "vitther_auth_prefs"
        private const val KEY_CUENTA_ACTIVA = "cuenta_activa_json"
        private const val KEY_CUENTAS_RECORDADAS = "cuentas_recordadas_json"

        fun generarUidDesdeCorreo(correoNormalizado: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(correoNormalizado.trim().lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }.take(20)
        }
    }
}
