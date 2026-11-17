package com.diego.vitther.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Importa inmediatamente: la colección no depende del permiso temporal del picker. */
class FotoStore(private val context: Context) {
    fun archivo(ruta: String?): File? = ruta?.let {
        val base = File(context.filesDir, "fotos").canonicalFile
        File(context.filesDir, it).canonicalFile.takeIf { file -> file.parentFile == base && file.exists() }
    }

    suspend fun eliminar(ruta: String?) = withContext(Dispatchers.IO) {
        archivo(ruta)?.delete()
        Unit
    }

    suspend fun guardarBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
        val destino = File(carpeta, "${UUID.randomUUID()}.jpg")
        destino.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
        "fotos/${destino.name}"
    }

    suspend fun codificarBase64(ruta: String?): String? = withContext(Dispatchers.IO) {
        val file = archivo(ruta) ?: return@withContext null
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 960) sample *= 2
            val decoded = BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return@runCatching null
            var scaled: Bitmap? = null
            try {
                val factor = minOf(1f, 960f / maxOf(decoded.width, decoded.height))
                val target = if (factor < 1f) {
                    scaled = Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * factor).toInt().coerceAtLeast(1),
                        (decoded.height * factor).toInt().coerceAtLeast(1),
                        true
                    )
                    scaled!!
                } else {
                    decoded
                }
                val stream = ByteArrayOutputStream()
                if (!target.compress(Bitmap.CompressFormat.JPEG, 75, stream)) return@runCatching null
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            } finally {
                scaled?.takeIf { it !== decoded }?.recycle()
                decoded.recycle()
            }
        }.getOrNull()
    }

    suspend fun decodificarBase64(base64: String?): String? = withContext(Dispatchers.IO) {
        if (base64.isNullOrBlank()) return@withContext null
        runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            if (bytes.isEmpty()) return@runCatching null
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
            try {
                guardarBitmap(bmp)
            } finally {
                bmp.recycle()
            }
        }.getOrNull()
    }

    suspend fun importar(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        require(opciones.outWidth > 0 && opciones.outHeight > 0) { "No se pudo leer esa imagen." }
        var muestra = 1
        while (maxOf(opciones.outWidth, opciones.outHeight) / muestra > 2560) muestra *= 2
        val original = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
        } ?: error("No se pudo abrir la foto.")
        var orientada: Bitmap? = null
        var reducida: Bitmap? = null
        val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
        val destino = File(carpeta, "${UUID.randomUUID()}.jpg")
        try {
            val exif = resolver.openInputStream(uri)?.use { ExifInterface(it) }
            val matriz = Matrix().apply {
                if (exif?.isFlipped == true) postScale(-1f, 1f)
                postRotate((exif?.rotationDegrees ?: 0).toFloat())
            }
            orientada = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
            val factor = minOf(1f, 1280f / maxOf(orientada.width, orientada.height))
            reducida = Bitmap.createScaledBitmap(orientada,
                (orientada.width * factor).toInt().coerceAtLeast(1),
                (orientada.height * factor).toInt().coerceAtLeast(1), true)
            destino.outputStream().use { check(reducida.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
            "fotos/${destino.name}"
        } catch (e: Exception) {
            destino.delete()
            throw e
        } finally {
            reducida?.takeIf { it !== orientada && it !== original }?.recycle()
            orientada?.takeIf { it !== original }?.recycle()
            original.recycle()
        }
    }
}
