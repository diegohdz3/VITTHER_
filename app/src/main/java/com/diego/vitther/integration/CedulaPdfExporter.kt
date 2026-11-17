package com.diego.vitther.integration

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.withSave
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.Fechas
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * Generador nativo de Cédula Oficial de Museo en formato PDF A4 con marco dorado,
 * fotografía preservando proporción, metadatos de curaduría y sello de verificación.
 */
object CedulaPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun exportarYCompartirCedula(
        context: Context,
        pieza: PiezaEntity,
        archivoFoto: File?,
        numeroGlobal: Int,
        mostrarValor: Boolean
    ) {
        val doc = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            val bgPaint = Paint().apply {
                color = Color.parseColor("#0C0E14")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            val outerFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5A4A22")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRect(22f, 22f, PAGE_WIDTH - 22f, PAGE_HEIGHT - 22f, outerFramePaint)

            val innerFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E8C97A")
                style = Paint.Style.STROKE
                strokeWidth = 0.7f
                alpha = 110
            }
            canvas.drawRect(28f, 28f, PAGE_WIDTH - 28f, PAGE_HEIGHT - 28f, innerFramePaint)

            // Encabezado oficial
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E8C97A")
                textSize = 9.5f
                letterSpacing = 0.24f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
            canvas.drawText("VITTHER · CÉDULA OFICIAL DE MUSEO", 48f, 62f, headerPaint)

            val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8D8777")
                textSize = 9.5f
                letterSpacing = 0.18f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.MONOSPACE
            }
            canvas.drawText(
                String.format(Locale.US, "PIEZA N.º %02d", numeroGlobal.coerceAtLeast(1)),
                PAGE_WIDTH - 48f,
                62f,
                codePaint
            )

            val dividerPaint = Paint().apply {
                color = Color.parseColor("#2A2418")
                strokeWidth = 1f
            }
            canvas.drawLine(48f, 76f, PAGE_WIDTH - 48f, 76f, dividerPaint)

            // Marco de fotografía (190 x 240 centrado)
            val frameW = 190f
            val frameH = 240f
            val frameLeft = (PAGE_WIDTH - frameW) / 2f
            val frameTop = 98f
            val frameRect = RectF(frameLeft, frameTop, frameLeft + frameW, frameTop + frameH)

            val frameBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#13151C")
                style = Paint.Style.FILL
            }
            canvas.drawRect(frameRect, frameBgPaint)

            val photoArea = RectF(
                frameLeft + 10f,
                frameTop + 10f,
                frameLeft + frameW - 10f,
                frameTop + frameH - 10f
            )

            val bitmap = archivoFoto?.takeIf { it.exists() }?.let { decodificarEscalado(it, 600) }
            if (bitmap != null) {
                try {
                    val scale = minOf(
                        photoArea.width() / bitmap.width.toFloat(),
                        photoArea.height() / bitmap.height.toFloat()
                    )
                    val drawW = bitmap.width * scale
                    val drawH = bitmap.height * scale
                    val drawLeft = photoArea.left + (photoArea.width() - drawW) / 2f
                    val drawTop = photoArea.top + (photoArea.height() - drawH) / 2f
                    val destRect = RectF(drawLeft, drawTop, drawLeft + drawW, drawTop + drawH)
                    canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                } finally {
                    bitmap.recycle()
                }
            } else {
                val initial = pieza.nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
                val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E8C97A")
                    alpha = 95
                    textSize = 72f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                }
                canvas.drawText(
                    initial,
                    frameRect.centerX(),
                    frameRect.centerY() + 24f,
                    initialPaint
                )
            }

            val photoBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#7A6733")
                style = Paint.Style.STROKE
                strokeWidth = 1.1f
            }
            canvas.drawRect(frameRect, photoBorderPaint)
            canvas.drawRect(photoArea, innerFramePaint)

            // Título y subtítulo
            var cursorY = frameTop + frameH + 34f
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F2EAD8")
                textSize = 24f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val titleLayout = StaticLayout.Builder.obtain(
                pieza.nombre,
                0,
                pieza.nombre.length,
                titlePaint,
                PAGE_WIDTH - 96
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(2f, 1.05f)
                .setMaxLines(2)
                .build()
            canvas.withSave {
                translate(48f, cursorY - 18f)
                titleLayout.draw(this)
            }
            cursorY += titleLayout.height.toFloat() + 2f

            val subtitulo = buildString {
                append(pieza.franquicia)
                pieza.linea?.takeIf { it.isNotBlank() }?.let {
                    append(" · ")
                    append(it)
                }
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#9C9485")
                textSize = 11.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(subtitulo, PAGE_WIDTH / 2f, cursorY, subPaint)
            cursorY += 18f

            // Chips de estado y edición limitada
            val chips = buildList {
                add(pieza.estado.etiqueta.uppercase(Locale.forLanguageTag("es-MX")))
                if (pieza.edicionLimitada) {
                    val ed = pieza.numeroEdicion?.takeIf { it.isNotBlank() }?.let { "ED. LIMITADA $it" } ?: "EDICIÓN LIMITADA"
                    add(ed)
                }
                if (pieza.destacada) add("DESTACADA")
            }
            val chipLine = chips.joinToString("   ·   ")
            val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E8C97A")
                textSize = 8.8f
                letterSpacing = 0.14f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(chipLine, PAGE_WIDTH / 2f, cursorY + 4f, chipPaint)
            cursorY += 24f

            // Tabla de metadatos
            val formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))
            val valorTexto = if (!mostrarValor) {
                "•••••• MXN (Privado)"
            } else {
                pieza.valorEstimadoCentavos?.let { formatoMoneda.format(it / 100.0) + " MXN" } ?: "No registrado"
            }

            val filas = listOf(
                "ADQUIRIDA" to Fechas.formatearCorto(pieza.fechaAdquisicion),
                "PROCEDENCIA" to (pieza.procedencia?.takeIf { it.isNotBlank() } ?: "No registrada"),
                "UBICACIÓN" to (pieza.ubicacion?.takeIf { it.isNotBlank() } ?: "No registrada"),
                "VALOR ESTIMADO" to valorTexto
            )

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8D8777")
                textSize = 8.5f
                letterSpacing = 0.16f
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F2EAD8")
                textSize = 11f
                textAlign = Paint.Align.RIGHT
            }

            canvas.drawLine(48f, cursorY, PAGE_WIDTH - 48f, cursorY, dividerPaint)
            filas.forEach { (etiqueta, valor) ->
                cursorY += 22f
                canvas.drawText(etiqueta, 48f, cursorY, labelPaint)
                canvas.drawText(valor.take(46), PAGE_WIDTH - 48f, cursorY, valuePaint)
                cursorY += 10f
                canvas.drawLine(48f, cursorY, PAGE_WIDTH - 48f, cursorY, dividerPaint)
            }

            // Cita de historia personal
            val historia = pieza.notaPersonal?.takeIf { it.isNotBlank() }
                ?: "Pieza catalogada en el archivo personal de VITTHER."
            cursorY += 24f
            val historiaTituloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E8C97A")
                textSize = 8.5f
                letterSpacing = 0.2f
            }
            canvas.drawText("HISTORIA DE CURADURÍA", 48f, cursorY, historiaTituloPaint)
            cursorY += 12f

            val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#DFD7C8")
                textSize = 11.5f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            val quoteText = "“$historia”"
            val maxQuoteHeight = (PAGE_HEIGHT - 115f - cursorY).coerceAtLeast(48f)
            val quoteLayout = StaticLayout.Builder.obtain(
                quoteText,
                0,
                quoteText.length,
                quotePaint,
                PAGE_WIDTH - 96
            )
                .setLineSpacing(2f, 1.15f)
                .setMaxLines((maxQuoteHeight / 16f).toInt().coerceIn(2, 8))
                .build()
            canvas.withSave {
                translate(48f, cursorY)
                quoteLayout.draw(this)
            }

            // Sello inferior de verificación + matriz geométrica de catálogo
            val footerTop = PAGE_HEIGHT - 86f
            canvas.drawLine(48f, footerTop, PAGE_WIDTH - 48f, footerTop, dividerPaint)

            val sealStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E8C97A")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val matrixLeft = 48f
            val matrixTop = footerTop + 14f
            val cellSize = 4.5f
            val matrixFill = Paint().apply {
                color = Color.parseColor("#E8C97A")
                style = Paint.Style.FILL
            }
            val seed = (pieza.id * 31L + pieza.nombre.hashCode().toLong()).let { kotlin.math.abs(it) }
            for (r in 0 until 6) {
                for (c in 0 until 6) {
                    val isCorner = (r < 2 && c < 2) || (r < 2 && c > 3) || (r > 3 && c < 2)
                    val bit = ((seed shr ((r * 6 + c) % 24)) and 1L) == 1L
                    if (isCorner || bit) {
                        canvas.drawRect(
                            matrixLeft + c * cellSize,
                            matrixTop + r * cellSize,
                            matrixLeft + (c + 0.8f) * cellSize,
                            matrixTop + (r + 0.8f) * cellSize,
                            matrixFill
                        )
                    }
                }
            }
            canvas.drawRect(matrixLeft - 3f, matrixTop - 3f, matrixLeft + 6 * cellSize + 2f, matrixTop + 6 * cellSize + 2f, sealStrokePaint)

            val footerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E8C97A")
                textSize = 9f
                letterSpacing = 0.16f
            }
            val footerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8D8777")
                textSize = 8.5f
            }
            canvas.drawText("ARCHIVO CERTIFICADO VITTHER · v26.2.1", matrixLeft + 38f, matrixTop + 11f, footerTitlePaint)
            canvas.drawText(
                "REG-${pieza.id.toString().padStart(4, '0')} · ${Fechas.formatearCorto(System.currentTimeMillis())}",
                matrixLeft + 38f,
                matrixTop + 24f,
                footerSubPaint
            )

            doc.finishPage(page)

            val carpeta = File(context.cacheDir, "camara").apply { mkdirs() }
            val pdfFile = File(carpeta, "Cedula_VITTHER_${pieza.id}.pdf")
            pdfFile.outputStream().use { out -> doc.writeTo(out) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", pdfFile)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cédula VITTHER · ${pieza.nombre}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Exportar cédula en PDF")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } finally {
            doc.close()
        }
    }

    private fun decodificarEscalado(file: File, maxLado: Int): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return@runCatching null
        var sample = 1
        while (maxOf(opts.outWidth, opts.outHeight) / sample > maxLado) sample *= 2
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }.getOrNull()
}
