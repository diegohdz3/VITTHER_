package com.diego.vitther.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.domain.EstadoWish
import com.diego.vitther.domain.Fechas
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Datos de demostración locales para generar evidencias visuales y capturas de pantalla
 * para el reporte técnico (versión de evaluación).
 *
 * NOTA: Estos cambios son locales y no se suben a Git ni modifican la lógica de producción.
 */
object SeedData {

    suspend fun sembrarSiEstaVacio(
        piezaDao: PiezaDao,
        capsulaDao: CapsulaDao,
        wishDao: WishDao? = null,
        context: Context
    ) {
        val zona = ZoneId.systemDefault()
        val hoy = LocalDate.now(zona)

        fun haceDias(dias: Long): Long = Fechas.desdeFecha(hoy.minusDays(dias), zona)
        fun enDias(dias: Long): Long = Fechas.desdeFecha(hoy.plusDays(dias), zona)

        // Generador de imágenes reales en el almacenamiento privado de la app (filesDir/fotos)
        fun generarFoto(nombreArchivo: String, colorAcento: Int, inicial: String): String {
            val dir = File(context.filesDir, "fotos").apply { mkdirs() }
            val file = File(dir, nombreArchivo)
            if (!file.exists()) {
                val w = 600
                val h = 800
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)

                // Fondo degradado profundo estilo museo
                val paint = Paint().apply { isAntiAlias = true }
                val shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    intArrayOf(0xFF14120E.toInt(), 0xFF0B0A08.toInt(), 0xFF1C1813.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                paint.shader = null

                // Luz cenital radial dorada
                val radial = RadialGradient(
                    w / 2f, 180f, 280f,
                    0x40E8C97A.toInt(), 0x00000000,
                    Shader.TileMode.CLAMP
                )
                paint.shader = radial
                canvas.drawCircle(w / 2f, 180f, 280f, paint)
                paint.shader = null

                // Marco dorado fino
                paint.color = 0x33E8C97A.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawRoundRect(24f, 24f, w - 24f, h - 24f, 24f, 24f, paint)

                // Silueta de nicho/pedestal central
                paint.style = Paint.Style.FILL
                paint.color = colorAcento
                canvas.drawRoundRect(w / 2f - 130f, h / 2f - 150f, w / 2f + 130f, h / 2f + 150f, 32f, 32f, paint)

                // Borde del pedestal
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = 0xFFE8C97A.toInt()
                canvas.drawRoundRect(w / 2f - 130f, h / 2f - 150f, w / 2f + 130f, h / 2f + 150f, 32f, 32f, paint)

                // Inicial / monograma de la figura
                paint.style = Paint.Style.FILL
                paint.color = 0xFFFBF6EA.toInt()
                paint.textSize = 72f
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                canvas.drawText(inicial, w / 2f, h / 2f + 25f, paint)

                // Leyenda inferior
                paint.textSize = 20f
                paint.color = 0xFFA0937D.toInt()
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                canvas.drawText("MUSEO VITTHER", w / 2f, h - 50f, paint)

                file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            }
            return "fotos/$nombreArchivo"
        }

        // Sembrado de piezas si no existen
        if (piezaDao.contar() == 0) {
            val fotoGoku = generarFoto("goku_ui.jpg", 0xFF2A3B5C.toInt(), "GK")
            val fotoSpidey = generarFoto("spidey_mms.jpg", 0xFF7A2020.toInt(), "SM")
            val fotoZelda = generarFoto("link_totk.jpg", 0xFF1E4D38.toInt(), "LK")
            val fotoVader = generarFoto("darth_vader.jpg", 0xFF3D1B1B.toInt(), "DV")
            val fotoEva = generarFoto("eva_01.jpg", 0xFF43255C.toInt(), "EV")
            val fotoGuts = generarFoto("guts_berserk.jpg", 0xFF36322C.toInt(), "GT")
            val fotoVegeta = generarFoto("vegeta_ssb.jpg", 0xFF1A385C.toInt(), "VG")
            val fotoMiles = generarFoto("miles_morales.jpg", 0xFF631826.toInt(), "MM")
            val fotoNaruto = generarFoto("naruto_sage.jpg", 0xFF7A4A1E.toInt(), "NT")
            val fotoIronMan = generarFoto("ironman_mk85.jpg", 0xFF7A2A1A.toInt(), "IM")

            val piezas = listOf(
                PiezaEntity(
                    nombre = "Goku Ultra Instinct",
                    franquicia = "Dragon Ball",
                    linea = "S.H.Figuarts",
                    fechaAdquisicion = haceDias(420),
                    fechaRegistro = haceDias(420),
                    fotoPath = fotoGoku,
                    estado = EstadoPieza.EXHIBICION,
                    destacada = true,
                    ubicacion = "Sala 01 · Vitrina central",
                    procedencia = "Anime Expo, Ciudad de México",
                    notaPersonal = "La busqué durante dos años. Llegó justo el día de mi cumpleaños.",
                    edicionLimitada = true,
                    numeroEdicion = "0142/2000",
                    valorEstimadoCentavos = 350_000
                ),
                PiezaEntity(
                    nombre = "Spider-Man Integrated Suit",
                    franquicia = "Marvel",
                    linea = "Hot Toys MMS",
                    fechaAdquisicion = haceDias(300),
                    fechaRegistro = haceDias(300),
                    fotoPath = fotoSpidey,
                    estado = EstadoPieza.EXHIBICION,
                    destacada = true,
                    ubicacion = "Sala 02 · Pedestal de honor",
                    procedencia = "Sideshow Collectibles",
                    notaPersonal = "El primer Hot Toys de la colección. El nivel de detalle en la tela es una obra maestra.",
                    edicionLimitada = true,
                    numeroEdicion = "0520/1500",
                    valorEstimadoCentavos = 890_000
                ),
                PiezaEntity(
                    nombre = "Link Tears of the Kingdom",
                    franquicia = "The Legend of Zelda",
                    linea = "Figma",
                    fechaAdquisicion = haceDias(120),
                    fechaRegistro = haceDias(120),
                    fotoPath = fotoZelda,
                    estado = EstadoPieza.EXHIBICION,
                    destacada = true,
                    ubicacion = "Sala 03 · Nicho arqueado",
                    procedencia = "Good Smile Company Japón",
                    notaPersonal = "Captura perfectamente el brazo corrompido de Rauru y la espada maestra rota.",
                    edicionLimitada = false,
                    valorEstimadoCentavos = 280_000
                ),
                PiezaEntity(
                    nombre = "Darth Vader",
                    franquicia = "Star Wars",
                    linea = "Black Series 40th",
                    fechaAdquisicion = haceDias(260),
                    fechaRegistro = haceDias(260),
                    fotoPath = fotoVader,
                    estado = EstadoPieza.EXHIBICION,
                    destacada = true,
                    ubicacion = "Sala 04 · Iluminación carmesí",
                    procedencia = "Celebration Anaheim",
                    notaPersonal = "La presencia imponente del lord Sith. La capa con caída real luce en la sala.",
                    edicionLimitada = false,
                    valorEstimadoCentavos = 120_000
                ),
                PiezaEntity(
                    nombre = "Guts Berserker Armor",
                    franquicia = "Berserk",
                    linea = "Threezero",
                    fechaAdquisicion = haceDias(45),
                    fechaRegistro = haceDias(45),
                    fotoPath = fotoGuts,
                    estado = EstadoPieza.EXHIBICION,
                    destacada = true,
                    ubicacion = "Sala 05 · Centro",
                    procedencia = "Preventa Japón",
                    notaPersonal = "Un monumento al arte de Kentaro Miura. Pesa más de dos kilos y la pintura oxidada impresiona.",
                    edicionLimitada = true,
                    numeroEdicion = "0088/1000",
                    valorEstimadoCentavos = 1_150_000
                ),
                PiezaEntity(
                    nombre = "Iron Man Mark LXXXV",
                    franquicia = "Marvel",
                    linea = "Hot Toys Diecast",
                    fechaAdquisicion = haceDias(190),
                    fechaRegistro = haceDias(190),
                    fotoPath = fotoIronMan,
                    estado = EstadoPieza.EXHIBICION,
                    destacada = false,
                    ubicacion = "Sala 02 · Vitrina lateral",
                    procedencia = "Sideshow Collectibles",
                    notaPersonal = "Pesada, con luces LED operativas en el reactor Arc y el guantelete nano.",
                    edicionLimitada = true,
                    valorEstimadoCentavos = 1_050_000
                ),
                // Piezas en la Bodega (Cava de vinos)
                PiezaEntity(
                    nombre = "Vegeta Super Saiyan Blue",
                    franquicia = "Dragon Ball",
                    linea = "S.H.Figuarts",
                    fechaAdquisicion = haceDias(390),
                    fechaRegistro = haceDias(390),
                    fotoPath = fotoVegeta,
                    estado = EstadoPieza.BODEGA,
                    destacada = false,
                    ubicacion = "Rack 01 · Columna A",
                    procedencia = "Mercado Libre",
                    notaPersonal = "Guardada en su empaque original esperando una nueva vitrina acrílica.",
                    valorEstimadoCentavos = 145_000
                ),
                PiezaEntity(
                    nombre = "Evangelion Unit-01",
                    franquicia = "Evangelion",
                    linea = "Real Action Heroes Neo",
                    fechaAdquisicion = haceDias(210),
                    fechaRegistro = haceDias(14),
                    fotoPath = fotoEva,
                    estado = EstadoPieza.BODEGA,
                    destacada = false,
                    ubicacion = "Rack 02 · Cava principal",
                    procedencia = "Mandarake Osaka, Japón",
                    notaPersonal = "Importada directamente. La caja venía sellada con precinto dorado.",
                    valorEstimadoCentavos = 520_000
                ),
                PiezaEntity(
                    nombre = "Miles Morales",
                    franquicia = "Marvel",
                    linea = "Sentinel SV-Action",
                    fechaAdquisicion = haceDias(150),
                    fechaRegistro = haceDias(150),
                    fotoPath = fotoMiles,
                    estado = EstadoPieza.BODEGA,
                    destacada = false,
                    ubicacion = "Rack 01 · Nivel 2",
                    procedencia = "AmiAmi",
                    valorEstimadoCentavos = 210_000
                ),
                PiezaEntity(
                    nombre = "Naruto Sage Mode",
                    franquicia = "Naruto",
                    linea = "Banpresto Grandista",
                    fechaAdquisicion = haceDias(20),
                    fechaRegistro = haceDias(20),
                    fotoPath = fotoNaruto,
                    estado = EstadoPieza.BODEGA,
                    destacada = false,
                    ubicacion = "Rack 03 · Entrada de bodega",
                    procedencia = "Tienda local",
                    valorEstimadoCentavos = 85_000
                )
            )

            val ids = piezaDao.insertarTodas(piezas)
            if (ids.size == piezas.size) {
                // Sembrar cápsulas de tiempo
                val capsulas = listOf(
                    CapsulaEntity(
                        piezaId = ids[4], // Guts
                        titulo = "Para el Diego del futuro",
                        nota = "Si sigues teniendo esta pieza en tu museo, recuerda el esfuerzo que costó conseguirla y cómo inspiró este proyecto.",
                        fechaApertura = enDias(30)
                    ),
                    CapsulaEntity(
                        piezaId = ids[1], // Spidey
                        titulo = "Primer Hot Toys",
                        nota = "El día que llegó sentí que mi colección daba el salto a museo formal.",
                        fechaApertura = enDias(90)
                    ),
                    CapsulaEntity(
                        piezaId = ids[0], // Goku
                        titulo = "La pieza fundadora",
                        nota = "Con esta figura empezó la pasión de exhibir y conservar cada historia.",
                        fechaApertura = haceDias(15),
                        abiertaEn = haceDias(10)
                    )
                )
                capsulaDao.insertarTodas(capsulas)
            }
        }

        // Sembrar Wishes si no existen
        if (wishDao != null && wishDao.contar() == 0) {
            val fotoEvaWish = generarFoto("wish_eva.jpg", 0xFF35204C.toInt(), "EV")
            val fotoGriffith = generarFoto("wish_griffith.jpg", 0xFF525048.toInt(), "GF")
            val fotoKratos = generarFoto("wish_kratos.jpg", 0xFF4D2020.toInt(), "KR")
            val fotoGojo = generarFoto("wish_gojo.jpg", 0xFF1C2C4D.toInt(), "GJ")
            val fotoZeldaWish = generarFoto("wish_zelda.jpg", 0xFF4D401C.toInt(), "ZD")

            val wishes = listOf(
                // Por llegar (con velo translúcido y cuenta regresiva)
                WishEntity(
                    nombre = "Evangelion Unit-01 Test Type",
                    franquicia = "Evangelion",
                    linea = "Threezero Robo-Dou",
                    estado = EstadoWish.POR_LLEGAR,
                    fechaEstimadaLlegada = enDias(7),
                    progresoPorcentaje = 88,
                    metaPesos = 6_200,
                    fotoPath = fotoEvaWish
                ),
                WishEntity(
                    nombre = "Griffith Hawk of Light",
                    franquicia = "Berserk",
                    linea = "Figma",
                    estado = EstadoWish.POR_LLEGAR,
                    fechaEstimadaLlegada = enDias(35),
                    progresoPorcentaje = 45,
                    metaPesos = 4_800,
                    fotoPath = fotoGriffith
                ),
                WishEntity(
                    nombre = "Kratos Ragnarök",
                    franquicia = "God of War",
                    linea = "Spin Master Deluxe",
                    estado = EstadoWish.POR_LLEGAR,
                    fechaEstimadaLlegada = enDias(90),
                    progresoPorcentaje = 15,
                    metaPesos = 3_900,
                    fotoPath = fotoKratos
                ),
                // Deseadas: El Gran Deseo
                WishEntity(
                    nombre = "Gojo Satoru Limitless",
                    franquicia = "Jujutsu Kaisen",
                    linea = "S.H.Figuarts",
                    estado = EstadoWish.DESEADA,
                    metaPesos = 4_500,
                    fotoPath = fotoGojo
                ),
                // Vitrina de cristal II
                WishEntity(
                    nombre = "Princesa Zelda Tears of the Kingdom",
                    franquicia = "The Legend of Zelda",
                    linea = "Figma",
                    estado = EstadoWish.DESEADA,
                    metaPesos = 3_400,
                    fotoPath = fotoZeldaWish
                )
            )
            wishDao.insertarTodos(wishes)
        }

        // Sembrar Recorridos guiados (Historia) en SharedPreferences si no existen
        val prefsRecorridos = context.getSharedPreferences("vitther_recorridos_prefs", Context.MODE_PRIVATE)
        if (!prefsRecorridos.contains("recorridos_json")) {
            val jsonRecorridos = """
                [
                  {
                    "id": "rec-joyas",
                    "nombre": "Joyas de la Corona",
                    "emblema": "crown",
                    "tema": "oro",
                    "subtitulo": "Las piezas maestras más valoradas y detalladas de la colección.",
                    "orden": "manual",
                    "piezaIds": [1, 2, 3, 5]
                  },
                  {
                    "id": "rec-anime",
                    "nombre": "Santuario Anime",
                    "emblema": "sword",
                    "tema": "ambar",
                    "subtitulo": "Obras del manga y animación japonesa reunidas en una misma sala.",
                    "orden": "sala",
                    "piezaIds": [1, 5, 7, 8, 10]
                  },
                  {
                    "id": "rec-comics",
                    "nombre": "Héroes del Multiverso",
                    "emblema": "star",
                    "tema": "carmesi",
                    "subtitulo": "Un recorrido cronológico desde las primeras adquisiciones hasta hoy.",
                    "orden": "fecha",
                    "piezaIds": [2, 4, 6]
                  }
                ]
            """.trimIndent()
            prefsRecorridos.edit().putString("recorridos_json", jsonRecorridos).apply()
        }
    }
}
