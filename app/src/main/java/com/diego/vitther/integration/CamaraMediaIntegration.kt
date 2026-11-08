package com.diego.vitther.integration

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.diego.vitther.data.media.FotoStore
import java.io.File

/**
 * Módulo de integración con recursos nativos de Android (Cámara y Almacenamiento privado)
 * ubicado en integration/ conforme a la sección 5 y 8 de la guía MVVM.
 */
class CamaraMediaIntegration(
    private val context: Context,
    private val fotoStore: FotoStore
) {
    fun crearArchivoTemporalCaptura(): Pair<File, Uri> {
        val carpeta = File(context.cacheDir, "camara").apply { mkdirs() }
        val archivo = File.createTempFile("captura_", ".jpg", carpeta)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", archivo)
        return archivo to uri
    }

    suspend fun importarImagenPrivada(uri: Uri): String =
        fotoStore.importar(uri)

    suspend fun eliminarImagenPrivada(rutaRelativa: String?) =
        fotoStore.eliminar(rutaRelativa)
}
