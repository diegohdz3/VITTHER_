package com.diego.vitther.navigation

/**
 * Definición central de rutas y grafo de navegación según la sección 5 de la guía MVVM
 * (navigation/Routes.kt y navigation/AppNavGraph.kt).
 */
object Routes {
    val inicio = Inicio
    val wishes = Wishes
    val categorias = Categorias
    val coleccion = Coleccion
    val historia = Historia
    val perfil = Perfil
    fun coleccionPorCategoria(franquicia: String) = ColeccionCategoria(franquicia)
    fun cedula(id: Long) = Cedula(id)
    fun registro(id: Long = 0, franquicia: String? = null) = Registro(id, franquicia)
}
