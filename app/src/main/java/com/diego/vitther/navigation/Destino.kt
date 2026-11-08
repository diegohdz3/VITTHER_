package com.diego.vitther.navigation

import kotlinx.serialization.Serializable

@Serializable data object Inicio
@Serializable data object Categorias
@Serializable data object Coleccion
@Serializable data class ColeccionCategoria(val franquicia: String)
@Serializable data object Historia
@Serializable data object Perfil
@Serializable data class Cedula(val id: Long)
@Serializable data class Registro(val id: Long = 0, val franquicia: String? = null)
