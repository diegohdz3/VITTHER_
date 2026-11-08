package com.diego.vitther.data.local

import com.diego.vitther.domain.EstadoPieza
import com.diego.vitther.domain.Fechas
import java.time.LocalDate
import java.time.ZoneId

/**
 * Datos de arranque SOLO para builds depurables. Sirven para que el museo
 * nunca se vea vacío en una demo y para ejercitar Categorías, Historia y el
 * aviso de cápsulas desde el primer minuto.
 */
object SeedData {

    suspend fun sembrarSiEstaVacio(
        piezaDao: PiezaDao,
        capsulaDao: CapsulaDao,
        ahora: Long = System.currentTimeMillis()
    ) {
        if (piezaDao.contar() > 0) return

        val zona = ZoneId.systemDefault()
        val hoy = LocalDate.now(zona)

        fun haceDias(dias: Long): Long = Fechas.desdeFecha(hoy.minusDays(dias), zona)
        fun enDias(dias: Long): Long = Fechas.desdeFecha(hoy.plusDays(dias), zona)

        val piezas = listOf(
            PiezaEntity(
                nombre = "Goku Ultra Instinct",
                franquicia = "Dragon Ball",
                linea = "S.H.Figuarts",
                fechaAdquisicion = haceDias(420),
                fechaRegistro = haceDias(420),
                estado = EstadoPieza.EXHIBICION,
                destacada = true,
                ubicacion = "Estante 1 · vitrina A",
                procedencia = "Anime Expo, Ciudad de México",
                notaPersonal = "La busqué durante dos años. Llegó justo el día de mi cumpleaños.",
                edicionLimitada = true,
                numeroEdicion = "0142/2000",
                valorEstimadoCentavos = 320_000
            ),
            PiezaEntity(
                nombre = "Vegeta Super Saiyan Blue",
                franquicia = "Dragon Ball",
                linea = "S.H.Figuarts",
                fechaAdquisicion = haceDias(390),
                fechaRegistro = haceDias(390),
                estado = EstadoPieza.BODEGA,
                ubicacion = "Caja 2 · burbuja",
                procedencia = "Mercado Libre",
                valorEstimadoCentavos = 145_000
            ),
            PiezaEntity(
                nombre = "Spider-Man Integrated Suit",
                franquicia = "Marvel",
                linea = "Hot Toys MMS",
                fechaAdquisicion = haceDias(300),
                fechaRegistro = haceDias(300),
                estado = EstadoPieza.EXHIBICION,
                destacada = true,
                ubicacion = "Estante 2 · centro",
                procedencia = "Sideshow Collectibles",
                notaPersonal = "El primer Hot Toys. Me tardé meses en decidirme y no me arrepiento.",
                edicionLimitada = true,
                valorEstimadoCentavos = 890_000
            ),
            PiezaEntity(
                nombre = "Darth Vader",
                franquicia = "Star Wars",
                linea = "Black Series",
                fechaAdquisicion = haceDias(260),
                fechaRegistro = haceDias(260),
                estado = EstadoPieza.EXHIBICION,
                ubicacion = "Estante 2 · lateral",
                procedencia = "Amazon",
                valorEstimadoCentavos = 95_000
            ),
            PiezaEntity(
                nombre = "Evangelion Unit-01",
                franquicia = "Evangelion",
                linea = "Real Action Heroes",
                fechaAdquisicion = haceDias(210),
                // Registrada mucho después: genera un hito de "agregada al museo".
                fechaRegistro = haceDias(14),
                estado = EstadoPieza.BODEGA,
                ubicacion = "Caja 1 · acrílico",
                procedencia = "Mandarake, Japón",
                notaPersonal = "Venía de Osaka. La caja llegó golpeada, la figura intacta.",
                valorEstimadoCentavos = 520_000
            ),
            PiezaEntity(
                nombre = "Miles Morales",
                franquicia = "Marvel",
                linea = "Sentinel",
                fechaAdquisicion = haceDias(150),
                fechaRegistro = haceDias(150),
                estado = EstadoPieza.BODEGA,
                ubicacion = "Caja 2 · burbuja",
                valorEstimadoCentavos = 210_000
            ),
            PiezaEntity(
                nombre = "Link Tears of the Kingdom",
                franquicia = "The Legend of Zelda",
                linea = "Figma",
                fechaAdquisicion = haceDias(90),
                fechaRegistro = haceDias(90),
                estado = EstadoPieza.EXHIBICION,
                destacada = true,
                ubicacion = "Estante 1 · vitrina B",
                procedencia = "Preventa oficial",
                valorEstimadoCentavos = 780_000
            ),
            PiezaEntity(
                nombre = "Asuka Langley",
                franquicia = "Evangelion",
                linea = "Kotobukiya",
                fechaAdquisicion = haceDias(60),
                fechaRegistro = haceDias(60),
                estado = EstadoPieza.BODEGA,
                ubicacion = "Caja 1 · acrílico",
                valorEstimadoCentavos = 260_000
            ),
            PiezaEntity(
                nombre = "Iron Man Mark III",
                franquicia = "Marvel",
                linea = "Hot Toys MMS",
                fechaAdquisicion = haceDias(30),
                fechaRegistro = haceDias(30),
                estado = EstadoPieza.EXHIBICION,
                ubicacion = "Estante 3 · centro",
                procedencia = "Sideshow Collectibles",
                valorEstimadoCentavos = 950_000
            ),
            PiezaEntity(
                nombre = "Naruto Sage Mode",
                franquicia = "Naruto",
                linea = "Banpresto",
                fechaAdquisicion = haceDias(7),
                fechaRegistro = haceDias(7),
                estado = EstadoPieza.BODEGA,
                ubicacion = "Caja 3 · pendiente de vitrina"
            ),
            PiezaEntity(
                nombre = "Guts Berserker Armor",
                franquicia = "Berserk",
                linea = "Threezero",
                fechaAdquisicion = haceDias(3),
                fechaRegistro = haceDias(1),
                estado = EstadoPieza.EXHIBICION,
                destacada = true,
                ubicacion = "Estante 3 · lateral",
                procedencia = "Preventa, edición limitada",
                notaPersonal = "Llegó antes de lo que esperaba. Ya tiene su lugar.",
                edicionLimitada = true,
                valorEstimadoCentavos = 1_150_000
            )
        )

        val ids = piezaDao.insertarTodas(piezas)
        if (ids.size != piezas.size) return

        val capsulas = listOf(
            CapsulaEntity(
                piezaId = ids[0],
                titulo = "Para el Diego que ya no colecciona igual",
                nota = "Si sigues teniendo esta figura, espero que todavía te acuerdes de por qué la querías.",
                fechaApertura = enDias(12)
            ),
            CapsulaEntity(
                piezaId = ids[2],
                titulo = "Cuando tenga mi primer trabajo",
                nota = "Quería confirmar que valió la pena esperar tanto para comprarte esto.",
                fechaApertura = enDias(45)
            ),
            CapsulaEntity(
                piezaId = ids[3],
                titulo = "Lo que sentí al abrir la caja",
                nota = "Nunca lo escribí antes. Lo escribo ahora, todavía con el olor a plástico nuevo.",
                fechaApertura = haceDias(2)
            ),
            CapsulaEntity(
                piezaId = ids[6],
                titulo = "Carta a mí mismo después de la preventa",
                nota = "El correo que no me animé a enviar. Lo dejo aquí para dentro de un año.",
                fechaApertura = haceDias(20),
                abiertaEn = haceDias(18)
            )
        )
        capsulaDao.insertarTodas(capsulas)
    }
}
