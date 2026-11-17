package com.diego.vitther.integration

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.withSave
import com.diego.vitther.data.local.PiezaEntity
import com.diego.vitther.domain.Fechas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class DatosExposicionCompartible(
    val titulo: String,
    val subtitulo: String,
    val curador: String,
    val piezas: List<PiezaEntity>,
    val colorAcentoHex: String = "#E8C97A",
    val franquiciaFiltro: String? = null
)

/**
 * Exportador universal de exposiciones, recorridos y salas de VITTHER
 * hacia Póster HD (.jpg) o Catálogo Multi-Página (.pdf) sin mostrar precios privados.
 */
object ColeccionShareExporter {

    private const val PDF_W = 595
    private const val PDF_H = 842
    private const val POSTER_W = 1080
    private const val POSTER_H = 1620

    fun generarArchivoImagenPoster(
        context: Context,
        datos: DatosExposicionCompartible,
        resolverArchivo: (String?) -> File?
    ): File {
        val bitmap = Bitmap.createBitmap(POSTER_W, POSTER_H, Bitmap.Config.ARGB_8888)
        try {
            val canvas = android.graphics.Canvas(bitmap)
            val accentColor = parseColorSeguro(datos.colorAcentoHex, Color.parseColor("#E8C97A"))
            val gold = Color.parseColor("#E8C97A")
            val cream = Color.parseColor("#F2EAD8")
            val muted = Color.parseColor("#8D8777")

            // Fondo nocturno degradado + halo radial del color temático
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, POSTER_H.toFloat(),
                    intArrayOf(Color.parseColor("#0B1224"), Color.parseColor("#0A0D17"), Color.parseColor("#08080A")),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, POSTER_W.toFloat(), POSTER_H.toFloat(), bgPaint)

            val glowPaint = Paint().apply {
                shader = RadialGradient(
                    POSTER_W * 0.5f,
                    POSTER_H * 0.22f,
                    POSTER_W * 0.68f,
                    Color.argb(90, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, POSTER_W.toFloat(), POSTER_H.toFloat(), glowPaint)

            // Doble marco de museo
            val borderOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5A4A22")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRect(32f, 32f, POSTER_W - 32f, POSTER_H - 32f, borderOuter)

            val borderInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(110, 232, 201, 122)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRect(46f, 46f, POSTER_W - 46f, POSTER_H - 46f, borderInner)

            // Cabecera estilo boleto oficial
            val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gold
                textSize = 22f
                letterSpacing = 0.28f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("VITTHER · EXPOSICIÓN VIRTUAL", POSTER_W / 2f, 114f, tagPaint)

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cream
                textSize = 54f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            val titleLayout = StaticLayout.Builder.obtain(
                datos.titulo,
                0,
                datos.titulo.length,
                titlePaint,
                POSTER_W - 140
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(2)
                .build()
            canvas.withSave {
                translate(70f, 138f)
                titleLayout.draw(this)
            }

            var headerBottomY = 148f + titleLayout.height
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                textSize = 24f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            val metaLine = "${datos.subtitulo} · Por ${datos.curador}".trim()
            canvas.drawText(metaLine.take(64), POSTER_W / 2f, headerBottomY + 26f, subPaint)
            headerBottomY += 56f

            val dividerPaint = Paint().apply {
                color = Color.parseColor("#2E281B")
                strokeWidth = 2f
            }
            canvas.drawLine(84f, headerBottomY, POSTER_W - 84f, headerBottomY, dividerPaint)

            // Cuadrícula de hasta 6 piezas (2 columnas × 3 filas)
            val mostradas = datos.piezas.take(6)
            val gridTop = headerBottomY + 34f
            val colGap = 36f
            val rowGap = 28f
            val cardW = (POSTER_W - 168f - colGap) / 2f
            val cardH = 342f

            mostradas.forEachIndexed { idx, pieza ->
                val fila = idx / 2
                val col = idx % 2
                val left = 84f + col * (cardW + colGap)
                val top = gridTop + fila * (cardH + rowGap)
                val cardRect = RectF(left, top, left + cardW, top + cardH)

                val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#11141D")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(cardRect, 18f, 18f, cardBg)

                val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#473B20")
                    style = Paint.Style.STROKE
                    strokeWidth = 1.8f
                }
                canvas.drawRoundRect(cardRect, 18f, 18f, cardBorder)

                // Marco de foto arqueado superior
                val frameRect = RectF(left + 22f, top + 20f, left + cardW - 22f, top + 226f)
                val frameFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0A0B0F")
                    style = Paint.Style.FILL
                }
                val radiusArch = frameRect.width() / 2f
                val archPath = Path().apply {
                    moveTo(frameRect.left, frameRect.bottom)
                    lineTo(frameRect.left, frameRect.top + radiusArch)
                    arcTo(
                        RectF(frameRect.left, frameRect.top, frameRect.right, frameRect.top + frameRect.width()),
                        180f,
                        180f,
                        false
                    )
                    lineTo(frameRect.right, frameRect.bottom)
                    close()
                }
                canvas.drawPath(archPath, frameFill)

                val archivo = resolverArchivo(pieza.fotoPath)
                val bmp = archivo?.takeIf { it.exists() }?.let { decodificarEscalado(it, 480) }
                if (bmp != null) {
                    try {
                        canvas.withSave {
                            clipPath(archPath)
                            val scale = minOf(
                                (frameRect.width() - 12f) / bmp.width.toFloat(),
                                (frameRect.height() - 12f) / bmp.height.toFloat()
                            )
                            val dw = bmp.width * scale
                            val dh = bmp.height * scale
                            val dl = frameRect.centerX() - dw / 2f
                            val dt = frameRect.centerY() - dh / 2f
                            drawBitmap(bmp, null, RectF(dl, dt, dl + dw, dt + dh), Paint(Paint.FILTER_BITMAP_FLAG))
                        }
                    } finally {
                        bmp.recycle()
                    }
                } else {
                    val inicial = pieza.nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
                    val initPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(140, Color.red(gold), Color.green(gold), Color.blue(gold))
                        textSize = 78f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    }
                    canvas.drawText(inicial, frameRect.centerX(), frameRect.centerY() + 26f, initPaint)
                }

                val archStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(115, 232, 201, 122)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.6f
                }
                canvas.drawPath(archPath, archStroke)

                // Etiqueta de franquicia y nombre de pieza
                val franqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = 15f
                    letterSpacing = 0.16f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(pieza.franquicia.uppercase(Locale.US).take(26), cardRect.centerX(), top + 252f, franqPaint)

                val pNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = cream
                    textSize = 23f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                val pLayout = StaticLayout.Builder.obtain(
                    pieza.nombre,
                    0,
                    pieza.nombre.length,
                    pNamePaint,
                    (cardW - 36f).toInt()
                )
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(2)
                    .build()
                canvas.withSave {
                    translate(left + 18f, top + 264f)
                    pLayout.draw(this)
                }
            }

            if (datos.piezas.size > 6) {
                val extraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = gold
                    textSize = 21f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                }
                canvas.drawText(
                    "+ ${datos.piezas.size - 6} piezas más en esta colección",
                    POSTER_W / 2f,
                    POSTER_H - 134f,
                    extraPaint
                )
            }

            // Pie de página oficial
            canvas.drawLine(84f, POSTER_H - 104f, POSTER_W - 84f, POSTER_H - 104f, dividerPaint)
            val footerLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = muted
                textSize = 18f
                letterSpacing = 0.16f
            }
            val footerRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gold
                textSize = 18f
                letterSpacing = 0.14f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(
                "VITTHER · ${Fechas.formatearCorto(System.currentTimeMillis()).uppercase()}",
                84f,
                POSTER_H - 66f,
                footerLeftPaint
            )
            canvas.drawText("MODO VIRTUAL · SOLO LECTURA", POSTER_W - 84f, POSTER_H - 66f, footerRightPaint)

            val carpeta = File(context.cacheDir, "camara").apply { mkdirs() }
            val imgFile = File(carpeta, "Exposicion_VITTHER_${System.currentTimeMillis()}.jpg")
            imgFile.outputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out))
            }
            return imgFile
        } finally {
            bitmap.recycle()
        }
    }

    fun generarArchivoCatalogoPdf(
        context: Context,
        datos: DatosExposicionCompartible,
        resolverArchivo: (String?) -> File?
    ): File {
        val doc = PdfDocument()
        try {
            val paginas = datos.piezas.chunked(3).ifEmpty { listOf(emptyList()) }
            val totalPaginas = paginas.size
            val gold = Color.parseColor("#E8C97A")
            val accentColor = parseColorSeguro(datos.colorAcentoHex, gold)
            val cream = Color.parseColor("#F2EAD8")
            val muted = Color.parseColor("#8D8777")

            val bgPaint = Paint().apply { color = Color.parseColor("#0C0E14") }
            val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5A4A22")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(95, 232, 201, 122)
                style = Paint.Style.STROKE
                strokeWidth = 0.7f
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#2A2418")
                strokeWidth = 1f
            }

            paginas.forEachIndexed { pageIdx, lote ->
                val pageInfo = PdfDocument.PageInfo.Builder(PDF_W, PDF_H, pageIdx + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawRect(0f, 0f, PDF_W.toFloat(), PDF_H.toFloat(), bgPaint)
                canvas.drawRect(22f, 22f, PDF_W - 22f, PDF_H - 22f, outerBorder)
                canvas.drawRect(28f, 28f, PDF_W - 28f, PDF_H - 28f, innerBorder)

                var cursorY = 56f
                if (pageIdx == 0) {
                    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = gold
                        textSize = 9.5f
                        letterSpacing = 0.24f
                    }
                    canvas.drawText("VITTHER · CATÁLOGO OFICIAL DE EXPOSICIÓN", 46f, cursorY, tagPaint)

                    val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = muted
                        textSize = 9.5f
                        letterSpacing = 0.16f
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(
                        "${datos.piezas.size} ${if (datos.piezas.size == 1) "PIEZA" else "PIEZAS"}",
                        PDF_W - 46f,
                        cursorY,
                        countPaint
                    )
                    cursorY += 14f

                    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = cream
                        textSize = 24f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    }
                    val tLayout = StaticLayout.Builder.obtain(
                        datos.titulo,
                        0,
                        datos.titulo.length,
                        titlePaint,
                        PDF_W - 92
                    ).setMaxLines(2).build()
                    canvas.withSave {
                        translate(46f, cursorY)
                        tLayout.draw(this)
                    }
                    cursorY += tLayout.height + 6f

                    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = accentColor
                        textSize = 11f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    }
                    canvas.drawText(
                        "${datos.subtitulo} · Curador: ${datos.curador}".take(78),
                        46f,
                        cursorY + 10f,
                        subPaint
                    )
                    cursorY += 24f
                    canvas.drawLine(46f, cursorY, PDF_W - 46f, cursorY, dividerPaint)
                    cursorY += 16f
                } else {
                    val runPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = gold
                        textSize = 9.5f
                        letterSpacing = 0.2f
                    }
                    canvas.drawText(datos.titulo.uppercase(Locale.US).take(44), 46f, cursorY, runPaint)
                    cursorY += 14f
                    canvas.drawLine(46f, cursorY, PDF_W - 46f, cursorY, dividerPaint)
                    cursorY += 18f
                }

                // Tarjetas de piezas en esta página
                val cardHeight = if (pageIdx == 0) 186f else 210f
                lote.forEachIndexed { localIdx, pieza ->
                    val globalStop = pageIdx * 3 + localIdx + 1
                    val cardRect = RectF(46f, cursorY, PDF_W - 46f, cursorY + cardHeight)

                    val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#11141C") }
                    canvas.drawRoundRect(cardRect, 8f, 8f, cardBg)
                    canvas.drawRoundRect(cardRect, 8f, 8f, outerBorder)

                    // Marco izquierdo de foto (106 x 142)
                    val imgLeft = cardRect.left + 14f
                    val imgTop = cardRect.top + (cardHeight - 142f) / 2f
                    val imgRect = RectF(imgLeft, imgTop, imgLeft + 106f, imgTop + 142f)
                    val imgBg = Paint().apply { color = Color.parseColor("#0A0B0F") }
                    canvas.drawRect(imgRect, imgBg)

                    val archivo = resolverArchivo(pieza.fotoPath)
                    val bmp = archivo?.takeIf { it.exists() }?.let { decodificarEscalado(it, 420) }
                    if (bmp != null) {
                        try {
                            val scale = minOf(
                                (imgRect.width() - 8f) / bmp.width.toFloat(),
                                (imgRect.height() - 8f) / bmp.height.toFloat()
                            )
                            val dw = bmp.width * scale
                            val dh = bmp.height * scale
                            val dl = imgRect.centerX() - dw / 2f
                            val dt = imgRect.centerY() - dh / 2f
                            canvas.drawBitmap(bmp, null, RectF(dl, dt, dl + dw, dt + dh), Paint(Paint.FILTER_BITMAP_FLAG))
                        } finally {
                            bmp.recycle()
                        }
                    } else {
                        val initPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.argb(145, 232, 201, 122)
                            textSize = 44f
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                        }
                        val inicial = pieza.nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
                        canvas.drawText(inicial, imgRect.centerX(), imgRect.centerY() + 15f, initPaint)
                    }
                    canvas.drawRect(imgRect, innerBorder)

                    // Textos de la pieza a la derecha
                    val txLeft = imgRect.right + 16f
                    val txWidth = (cardRect.right - 14f - txLeft).toInt().coerceAtLeast(180)
                    var ty = cardRect.top + 24f

                    val stopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = accentColor
                        textSize = 8.5f
                        letterSpacing = 0.18f
                    }
                    canvas.drawText(
                        String.format(Locale.US, "PARADA %02d · %s", globalStop, pieza.franquicia.uppercase(Locale.US).take(26)),
                        txLeft,
                        ty,
                        stopPaint
                    )
                    ty += 8f

                    val pTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = cream
                        textSize = 15.5f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    }
                    val pTitleLayout = StaticLayout.Builder.obtain(
                        pieza.nombre,
                        0,
                        pieza.nombre.length,
                        pTitlePaint,
                        txWidth
                    ).setMaxLines(2).build()
                    canvas.withSave {
                        translate(txLeft, ty)
                        pTitleLayout.draw(this)
                    }
                    ty += pTitleLayout.height + 6f

                    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = muted
                        textSize = 9.5f
                    }
                    val lineaText = listOfNotNull(
                        pieza.linea?.takeIf { it.isNotBlank() },
                        Fechas.formatearCorto(pieza.fechaAdquisicion),
                        pieza.ubicacion?.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    canvas.drawText(lineaText.take(54), txLeft, ty + 8f, metaPaint)
                    ty += 18f

                    val historia = pieza.notaPersonal?.takeIf { it.isNotBlank() }
                        ?: "Pieza catalogada en el archivo privado de ${datos.curador}."
                    val qPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#D8D0C2")
                        textSize = 10.5f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    }
                    val qText = "“$historia”"
                    val qLayout = StaticLayout.Builder.obtain(qText, 0, qText.length, qPaint, txWidth)
                        .setMaxLines(4)
                        .build()
                    canvas.withSave {
                        translate(txLeft, ty)
                        qLayout.draw(this)
                    }

                    cursorY += cardHeight + 14f
                }

                // Pie de página
                val footerY = PDF_H - 44f
                canvas.drawLine(46f, footerY - 14f, PDF_W - 46f, footerY - 14f, dividerPaint)
                val footLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = muted
                    textSize = 8.5f
                    letterSpacing = 0.14f
                }
                val footRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = gold
                    textSize = 8.5f
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("ARCHIVO VITTHER · VISTA SOLO LECTURA", 46f, footerY, footLeft)
                canvas.drawText("Página ${pageIdx + 1} de $totalPaginas", PDF_W - 46f, footerY, footRight)

                doc.finishPage(page)
            }

            val carpeta = File(context.cacheDir, "camara").apply { mkdirs() }
            val pdfFile = File(carpeta, "Recorrido_VITTHER_${System.currentTimeMillis()}.pdf")
            pdfFile.outputStream().use { out -> doc.writeTo(out) }
            return pdfFile
        } finally {
            doc.close()
        }
    }

    fun compartirComoImagenPoster(
        context: Context,
        datos: DatosExposicionCompartible,
        resolverArchivo: (String?) -> File?
    ) {
        val archivo = generarArchivoImagenPoster(context, datos, resolverArchivo)
        lanzarShareArchivo(context, archivo, "image/jpeg", "Compartir póster de VITTHER", datos.titulo)
    }

    fun compartirComoCatalogoPdf(
        context: Context,
        datos: DatosExposicionCompartible,
        resolverArchivo: (String?) -> File?
    ) {
        val archivo = generarArchivoCatalogoPdf(context, datos, resolverArchivo)
        lanzarShareArchivo(context, archivo, "application/pdf", "Exportar catálogo en PDF", datos.titulo)
    }

    fun lanzarShareArchivo(
        context: Context,
        archivo: File,
        mimeType: String,
        chooserTitle: String,
        subjectTitle: String
    ) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "VITTHER · $subjectTitle")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun decodificarEscalado(file: File, maxLado: Int): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return@runCatching null
        var sample = 1
        while (maxOf(opts.outWidth, opts.outHeight) / sample > maxLado) sample *= 2
        BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    private fun parseColorSeguro(hex: String, fallback: Int): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(fallback)
}

/**
 * Hoja modal universal para compartir cualquier Sala, Recorrido o Museo completo
 * en el formato que prefiera el usuario: Imagen HD, Catálogo PDF o Link interactivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompartirExposicionSheet(
    datos: DatosExposicionCompartible,
    resolverArchivo: (String?) -> File?,
    onCompartirLinkVitther: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var generando by remember { mutableStateOf(false) }
    val gold = androidx.compose.ui.graphics.Color(0xFFE8C97A)
    val cream = androidx.compose.ui.graphics.Color(0xFFF2EAD8)
    val muted = androidx.compose.ui.graphics.Color(0xFF8D8777)

    ModalBottomSheet(
        onDismissRequest = { if (!generando) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = androidx.compose.ui.graphics.Color(0xFF13110D),
        contentColor = cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "COMPARTIR EXPOSICIÓN",
                color = gold,
                fontSize = 9.5.sp,
                letterSpacing = 2.4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = datos.titulo,
                color = cream,
                fontSize = 21.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${datos.piezas.size} ${if (datos.piezas.size == 1) "pieza" else "piezas"} · Elige cómo enviarla a tus amigos",
                color = muted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            if (generando) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = gold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Preparando exposición…", color = gold, fontSize = 13.sp)
                }
            } else {
                OpcionFormatoCompartir(
                    icono = Icons.Outlined.Image,
                    titulo = "Compartir como Imagen / Póster HD",
                    subtitulo = "Ideal para WhatsApp, Instagram o guardar en galería",
                    gold = gold,
                    cream = cream,
                    muted = muted,
                    onClick = {
                        scope.launch {
                            generando = true
                            val archivo = withContext(Dispatchers.IO) {
                                runCatching {
                                    ColeccionShareExporter.generarArchivoImagenPoster(context, datos, resolverArchivo)
                                }.getOrNull()
                            }
                            generando = false
                            if (archivo != null) {
                                onDismiss()
                                runCatching {
                                    ColeccionShareExporter.lanzarShareArchivo(
                                        context,
                                        archivo,
                                        "image/jpeg",
                                        "Compartir póster HD",
                                        datos.titulo
                                    )
                                }
                            }
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))

                OpcionFormatoCompartir(
                    icono = Icons.Outlined.PictureAsPdf,
                    titulo = "Exportar Catálogo en PDF",
                    subtitulo = "Folleto completo con todas las piezas y sus historias",
                    gold = gold,
                    cream = cream,
                    muted = muted,
                    onClick = {
                        scope.launch {
                            generando = true
                            val archivo = withContext(Dispatchers.IO) {
                                runCatching {
                                    ColeccionShareExporter.generarArchivoCatalogoPdf(context, datos, resolverArchivo)
                                }.getOrNull()
                            }
                            generando = false
                            if (archivo != null) {
                                onDismiss()
                                runCatching {
                                    ColeccionShareExporter.lanzarShareArchivo(
                                        context,
                                        archivo,
                                        "application/pdf",
                                        "Exportar catálogo en PDF",
                                        datos.titulo
                                    )
                                }
                            }
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))

                OpcionFormatoCompartir(
                    icono = Icons.Outlined.Share,
                    titulo = "Enviar Link / Código Interactivo VITTHER",
                    subtitulo = "Para recorrerla en vivo dentro de la app en solo lectura",
                    gold = gold,
                    cream = cream,
                    muted = muted,
                    onClick = {
                        onDismiss()
                        onCompartirLinkVitther()
                    }
                )
            }
        }
    }
}

@Composable
private fun OpcionFormatoCompartir(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    gold: androidx.compose.ui.graphics.Color,
    cream: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = androidx.compose.ui.graphics.Color(0xFF191610),
        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF332B1C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color(0xFF261F12),
                                androidx.compose.ui.graphics.Color(0xFF13100A)
                            )
                        )
                    )
                    .border(1.dp, androidx.compose.ui.graphics.Color(0xFF5A4A28), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = gold,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    color = cream,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitulo,
                    color = muted,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
