package com.diego.vitther.data.local

import android.content.Context
import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.domain.EstadoWish
import com.diego.vitther.domain.Fechas
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Sembrado automático inicial y generador de lotes aleatorios de prueba
 * preservando los monogramas, velos y marcos nativos de VITTHER (fotoPath = null).
 */
object SeedData {

    private const val PREFS_SEED = "vitther_seed_prefs"
    private const val KEY_LOTE_INICIAL = "demo_lote_inicial_v1"
    private const val KEY_LIMPIEZA_POSTERS = "seed_fotos_limpiadas_v1"
    private const val DAY_MS = 86_400_000L

    private data class PiezaSeedSpec(
        val nombre: String,
        val franquicia: String,
        val linea: String,
        val diasAtras: Long,
        val estado: EstadoPieza,
        val destacada: Boolean,
        val ubicacion: String,
        val procedencia: String,
        val historia: String,
        val edicionLimitada: Boolean,
        val numeroEdicion: String?,
        val valorCentavos: Long,
        val colorHex: String
    )

    private val loteInicial = listOf(
        PiezaSeedSpec(
            nombre = "Naruto Uzumaki Sage Mode",
            franquicia = "Naruto",
            linea = "S.H. Figuarts",
            diasAtras = 180,
            estado = EstadoPieza.EXHIBICION,
            destacada = true,
            ubicacion = "Vitrina Central · Nivel 1",
            procedencia = "Tamashii Nations Tokyo",
            historia = "La capa carmesí y los pergaminos representan el momento exacto en que Naruto domina la energía natural del Monte Myoboku para proteger Konoha.",
            edicionLimitada = true,
            numeroEdicion = "#042/500",
            valorCentavos = 245000L,
            colorHex = "#D97A2B"
        ),
        PiezaSeedSpec(
            nombre = "Itachi Uchiha Akatsuki",
            franquicia = "Naruto",
            linea = "Megahouse G.E.M.",
            diasAtras = 140,
            estado = EstadoPieza.EXHIBICION,
            destacada = true,
            ubicacion = "Vitrina Central · Nivel 2",
            procedencia = "AmiAmi Japón",
            historia = "Esculpida sobre un pedestal con cuervos en vuelo, captura la serenidad melancólica de quien sacrificó su propio nombre por la paz.",
            edicionLimitada = false,
            numeroEdicion = null,
            valorCentavos = 398000L,
            colorHex = "#B83B48"
        ),
        PiezaSeedSpec(
            nombre = "Darth Vader Empire Strikes Back",
            franquicia = "Star Wars",
            linea = "Hot Toys MMS",
            diasAtras = 260,
            estado = EstadoPieza.EXHIBICION,
            destacada = true,
            ubicacion = "Ala Imperial · Nicho Principal",
            procedencia = "Sideshow Collectibles",
            historia = "Armadura en escala 1/6 con cámara de meditación iluminada LED y acabado en cuero real que evoca el duelo en Ciudad Nube.",
            edicionLimitada = true,
            numeroEdicion = "#118/1000",
            valorCentavos = 785000L,
            colorHex = "#C23636"
        ),
        PiezaSeedSpec(
            nombre = "The Mandalorian & Grogu Beskar",
            franquicia = "Star Wars",
            linea = "Hot Toys Deluxe",
            diasAtras = 95,
            estado = EstadoPieza.EXHIBICION,
            destacada = false,
            ubicacion = "Ala Imperial · Nivel 2",
            procedencia = "Distribución Oficial MX",
            historia = "Acero beskar cromado a mano acompañado de la cuna flotante y el pequeño Grogu en una escena icónica del Borde Exterior.",
            edicionLimitada = false,
            numeroEdicion = null,
            valorCentavos = 690000L,
            colorHex = "#5E7B9C"
        ),
        PiezaSeedSpec(
            nombre = "Guts Berserker Armor",
            franquicia = "Berserk",
            linea = "Threezero 1/6",
            diasAtras = 310,
            estado = EstadoPieza.EXHIBICION,
            destacada = true,
            ubicacion = "Santuario Oscuro · Centro",
            procedencia = "Threezero Store",
            historia = "La primera gran pieza que inauguró el museo: acero desgastado en batalla, capa articulada y la emblemática Dragonslayer de hierro macizo.",
            edicionLimitada = true,
            numeroEdicion = "#019/300",
            valorCentavos = 640000L,
            colorHex = "#7A2D38"
        ),
        PiezaSeedSpec(
            nombre = "Link Tears of the Kingdom",
            franquicia = "The Legend of Zelda",
            linea = "Figma DX Edition",
            diasAtras = 45,
            estado = EstadoPieza.EXHIBICION,
            destacada = true,
            ubicacion = "Sala Hyrule · Pedestal A",
            procedencia = "Good Smile Company",
            historia = "Brazo derecho Zonai, Espada Maestra y escudo hyliano en edición DX lista para exhibirse sobre los cielos de Hyrule.",
            edicionLimitada = false,
            numeroEdicion = null,
            valorCentavos = 285000L,
            colorHex = "#3E9B78"
        ),
        PiezaSeedSpec(
            nombre = "Son Goku Ultra Instinct",
            franquicia = "Dragon Ball",
            linea = "S.H. Figuarts",
            diasAtras = 210,
            estado = EstadoPieza.BODEGA,
            destacada = false,
            ubicacion = "Cava Privada · Estante 2",
            procedencia = "Bandai Collector",
            historia = "Pintura nacarada plateada con aura translúcida del Torneo del Poder, resguardada en su empaque original.",
            edicionLimitada = false,
            numeroEdicion = null,
            valorCentavos = 185000L,
            colorHex = "#7C9EE8"
        ),
        PiezaSeedSpec(
            nombre = "Roronoa Zoro King of Hell",
            franquicia = "One Piece",
            linea = "Portrait.Of.Pirates",
            diasAtras = 60,
            estado = EstadoPieza.BODEGA,
            destacada = false,
            ubicacion = "Cava Privada · Caja B",
            procedencia = "MegaHouse Exclusiva",
            historia = "Las tres espadas envueltas en llamas esmeralda del clímax en Onigashima, guardada en bodega esperando vitrina nueva.",
            edicionLimitada = true,
            numeroEdicion = "#207/800",
            valorCentavos = 520000L,
            colorHex = "#2E8A5E"
        )
    )

    private val catalogoAleatorioExtra = listOf(
        PiezaSeedSpec(
            nombre = "EVA-01 Test Type",
            franquicia = "Evangelion",
            linea = "Metal Build Bandai",
            diasAtras = 15,
            estado = EstadoPieza.EXHIBICION,
            destacada = true,
            ubicacion = "Estante Tokio-3 · Nivel 1",
            procedencia = "Tamashii Web Exclusive",
            historia = "Chasis die-cast con acabado metálico violeta y verde neón, inspirado en la obra de Ikuto Yamashita.",
            edicionLimitada = true,
            numeroEdicion = "#088/600",
            valorCentavos = 720000L,
            colorHex = "#6F42C1"
        ),
        PiezaSeedSpec(
            nombre = "Denji & Pochita",
            franquicia = "Chainsaw Man",
            linea = "Shibuya Scramble 1/7",
            diasAtras = 22,
            estado = EstadoPieza.EXHIBICION,
            destacada = true,
            ubicacion = "Vitrina Moderna · Ala B",
            procedencia = "eStream Japón",
            historia = "Escultura dinámica en plena transformación con acabados metálicos y base de escombros.",
            edicionLimitada = false,
            numeroEdicion = null,
            valorCentavos = 610000L,
            colorHex = "#D65A31"
        ),
        PiezaSeedSpec(
            nombre = "Satoru Gojo Shibuya",
            franquicia = "Jujutsu Kaisen",
            linea = "F:NEX FuRyu 1/7",
            diasAtras = 10,
            estado = EstadoPieza.EXHIBICION,
            destacada = false,
            ubicacion = "Vitrina Moderna · Nivel 2",
            procedencia = "F:NEX Japón",
            historia = "Los Seis Ojos brillando tras la venda con el efecto Vacío Púrpura alrededor del pedestal.",
            edicionLimitada = true,
            numeroEdicion = "#154/500",
            valorCentavos = 475000L,
            colorHex = "#4C6EF5"
        ),
        PiezaSeedSpec(
            nombre = "Spider-Man Integrated Suit",
            franquicia = "Marvel",
            linea = "Hot Toys MMS Deluxe",
            diasAtras = 75,
            estado = EstadoPieza.EXHIBICION,
            destacada = true,
            ubicacion = "Torre Marvel · Nivel 1",
            procedencia = "Sideshow Collectibles",
            historia = "Traje híbrido con detalles de nanotecnología dorada y base diorama de la Estatua de la Libertad.",
            edicionLimitada = false,
            numeroEdicion = null,
            valorCentavos = 740000L,
            colorHex = "#C92A2A"
        ),
        PiezaSeedSpec(
            nombre = "Batman Dark Knight Armory",
            franquicia = "DC Comics",
            linea = "Queen Studios / InArt",
            diasAtras = 130,
            estado = EstadoPieza.EXHIBICION,
            destacada = false,
            ubicacion = "Bóveda Gotham · Central",
            procedencia = "Spec Fiction Shop",
            historia = "Armadura táctica de Wayne Enterprises acompañada de vitrina iluminada y arsenal modular.",
            edicionLimitada = true,
            numeroEdicion = "#063/400",
            valorCentavos = 960000L,
            colorHex = "#495057"
        ),
        PiezaSeedSpec(
            nombre = "Tanjiro Kamado Hinokami",
            franquicia = "Demon Slayer",
            linea = "Aniplex+ 1/8",
            diasAtras = 35,
            estado = EstadoPieza.BODEGA,
            destacada = false,
            ubicacion = "Cava Privada · Estante 4",
            procedencia = "Aniplex Tokyo",
            historia = "Danza del Dios del Fuego esculpida en resina translúcida naranja y carmesí.",
            edicionLimitada = false,
            numeroEdicion = null,
            valorCentavos = 355000L,
            colorHex = "#E67700"
        ),
        PiezaSeedSpec(
            nombre = "Malenia Blade of Miquella",
            franquicia = "Elden Ring",
            linea = "Bandai Collector Edition",
            diasAtras = 55,
            estado = EstadoPieza.EXHIBICION,
            destacada = true,
            ubicacion = "Sala Souls · Pedestal B",
            procedencia = "Bandai Namco Store",
            historia = "Casco valquiria dorado, prótesis mecánica y katana colosal de las Tierras Intermedias.",
            edicionLimitada = true,
            numeroEdicion = "#312/1500",
            valorCentavos = 440000L,
            colorHex = "#B08938"
        ),
        PiezaSeedSpec(
            nombre = "Sasuke Uchiha Rinnegan",
            franquicia = "Naruto",
            linea = "S.H. Figuarts",
            diasAtras = 115,
            estado = EstadoPieza.EXHIBICION,
            destacada = false,
            ubicacion = "Vitrina Central · Nivel 3",
            procedencia = "Tamashii Nations",
            historia = "Espada Kusanagi, efecto Chidori y manto de viajero vigilando Konoha desde las sombras.",
            edicionLimitada = false,
            numeroEdicion = null,
            valorCentavos = 195000L,
            colorHex = "#5F3DC4"
        )
    )

    suspend fun sembrarSiEstaVacio(
        piezaDao: PiezaDao,
        capsulaDao: CapsulaDao,
        wishDao: WishDao? = null,
        context: Context? = null
    ) {
        if (context == null) return
        // No interferir si la app se ejecuta bajo pruebas automatizadas de AndroidJUnit.
        val enTestInstrumentado = runCatching { Class.forName("androidx.test.espresso.Espresso") }.isSuccess
        if (enTestInstrumentado) return

        val prefs = context.applicationContext.getSharedPreferences(PREFS_SEED, Context.MODE_PRIVATE)

        // Limpiar pósters seed_*.jpg previos para mostrar los emblemas y velos nativos del museo.
        if (!prefs.getBoolean(KEY_LIMPIEZA_POSTERS, false)) {
            piezaDao.observarTodas().first().forEach { p ->
                val path = p.fotoPath
                if (path != null && path.startsWith("fotos/seed_")) {
                    runCatching { File(context.filesDir, path).delete() }
                    piezaDao.actualizar(p.copy(fotoPath = null))
                }
            }
            wishDao?.observarTodos()?.first()?.forEach { w ->
                val path = w.fotoPath
                if (path != null && path.startsWith("fotos/seed_")) {
                    runCatching { File(context.filesDir, path).delete() }
                    wishDao.actualizar(w.copy(fotoPath = null))
                }
            }
            prefs.edit().putBoolean(KEY_LIMPIEZA_POSTERS, true).apply()
        }

        if (prefs.getBoolean(KEY_LOTE_INICIAL, false)) return

        val ahora = Fechas.inicioDelDia(System.currentTimeMillis())
        val existentes = piezaDao.observarTodas().first()
        val idsPiezas = mutableListOf<Long>()

        for (spec in loteInicial) {
            val yaExiste = existentes.find { it.nombre.equals(spec.nombre, ignoreCase = true) }
            if (yaExiste != null) {
                idsPiezas.add(yaExiste.id)
                continue
            }
            val id = piezaDao.insertar(
                PiezaEntity(
                    nombre = spec.nombre,
                    franquicia = spec.franquicia,
                    linea = spec.linea,
                    fechaAdquisicion = ahora - spec.diasAtras * DAY_MS,
                    fechaRegistro = System.currentTimeMillis() - spec.diasAtras * DAY_MS,
                    fotoPath = null,
                    estado = spec.estado,
                    destacada = spec.destacada,
                    ubicacion = spec.ubicacion,
                    procedencia = spec.procedencia,
                    notaPersonal = spec.historia,
                    edicionLimitada = spec.edicionLimitada,
                    numeroEdicion = spec.numeroEdicion,
                    valorEstimadoCentavos = spec.valorCentavos
                )
            )
            idsPiezas.add(id)
        }

        // Sembrar 2 cápsulas de tiempo si aún no hay cápsulas
        if (capsulaDao.observarTodas().first().isEmpty() && idsPiezas.isNotEmpty()) {
            capsulaDao.insertar(
                CapsulaEntity(
                    piezaId = idsPiezas.first(),
                    titulo = "Recuerdo del primer día",
                    nota = "Prometí que esta figura marcaría el inicio de un museo personal digno de exhibirse. ¡Hora de celebrarlo!",
                    fechaApertura = ahora - DAY_MS,
                    creadaEn = ahora - 30L * DAY_MS
                )
            )
            val segundaPiezaId = idsPiezas.getOrElse(2) { idsPiezas.first() }
            capsulaDao.insertar(
                CapsulaEntity(
                    piezaId = segundaPiezaId,
                    titulo = "Carta al curador del futuro",
                    nota = "Recuerda limpiar el polvo de la vitrina y apreciar cuánto ha crecido la colección desde este momento.",
                    fechaApertura = ahora + 14L * DAY_MS,
                    creadaEn = ahora
                )
            )
        }

        // Sembrar 3 Wishes de prueba si la lista está vacía
        if (wishDao != null && wishDao.contar() == 0) {
            val wishesSeed = listOf(
                WishEntity(
                    nombre = "Gojo Satoru Domain Expansion",
                    franquicia = "Jujutsu Kaisen",
                    linea = "SHIBUYA SCRAMBLE",
                    estado = EstadoWish.POR_LLEGAR,
                    fechaEstimadaLlegada = ahora + 10L * DAY_MS,
                    progresoPorcentaje = 72,
                    metaPesos = 5600L,
                    fotoPath = null,
                    creadoEn = System.currentTimeMillis() - 18L * DAY_MS
                ),
                WishEntity(
                    nombre = "Samus Aran Metroid Dread",
                    franquicia = "Metroid",
                    linea = "Figma Good Smile",
                    estado = EstadoWish.POR_LLEGAR,
                    fechaEstimadaLlegada = ahora + 24L * DAY_MS,
                    progresoPorcentaje = 35,
                    metaPesos = 2750L,
                    fotoPath = null,
                    creadoEn = System.currentTimeMillis() - 8L * DAY_MS
                ),
                WishEntity(
                    nombre = "Asuka Langley Radio EVA",
                    franquicia = "Evangelion",
                    linea = "Hobby Max 1/7",
                    estado = EstadoWish.DESEADA,
                    fechaEstimadaLlegada = null,
                    progresoPorcentaje = 0,
                    metaPesos = 4200L,
                    fotoPath = null,
                    creadoEn = System.currentTimeMillis() - 3L * DAY_MS
                )
            )
            wishesSeed.forEach { wishDao.insertar(it) }
        }

        prefs.edit().putBoolean(KEY_LOTE_INICIAL, true).apply()
    }

    /**
     * Agrega 3 figuras aleatorias del catálogo extendido preservando los marcos
     * y monogramas nativos de VITTHER para probar filtros, audioguía, PDF y salas compartibles.
     */
    suspend fun generarFigurasAleatoriasExtra(piezaDao: PiezaDao, context: Context): Int {
        val existentes = piezaDao.observarTodas().first()
        val disponibles = catalogoAleatorioExtra.filter { spec ->
            existentes.none { it.nombre.equals(spec.nombre, ignoreCase = true) }
        }
        val elegidas = if (disponibles.size >= 3) {
            disponibles.shuffled().take(3)
        } else {
            val sufijos = listOf("Ed. Especial", "Ver. Museo", "Reserva Privada", "Exhibición Deluxe")
            (disponibles + catalogoAleatorioExtra.shuffled().take(3 - disponibles.size)).mapIndexed { idx, s ->
                val sufijo = sufijos.random()
                val nombreUnico = if (disponibles.contains(s)) s.nombre else "${s.nombre} ($sufijo #${(10..99).random()})"
                s.copy(nombre = nombreUnico, diasAtras = (1L..90L).random() + idx)
            }
        }

        val ahora = Fechas.inicioDelDia(System.currentTimeMillis())
        var agregadas = 0
        for (spec in elegidas) {
            piezaDao.insertar(
                PiezaEntity(
                    nombre = spec.nombre,
                    franquicia = spec.franquicia,
                    linea = spec.linea,
                    fechaAdquisicion = ahora - spec.diasAtras * DAY_MS,
                    fechaRegistro = System.currentTimeMillis(),
                    fotoPath = null,
                    estado = spec.estado,
                    destacada = spec.destacada,
                    ubicacion = spec.ubicacion,
                    procedencia = spec.procedencia,
                    notaPersonal = spec.historia,
                    edicionLimitada = spec.edicionLimitada,
                    numeroEdicion = spec.numeroEdicion,
                    valorEstimadoCentavos = spec.valorCentavos
                )
            )
            agregadas++
        }
        return agregadas
    }
}
