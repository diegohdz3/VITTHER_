# VITTHER — Tu Museo Personal de Coleccionismo

**Materia:** Desarrollo de Aplicaciones Móviles  
**Autor:** Diego Cortés Hernández  
**Versión actual:** `26.2.1` (Esquema `AA.M.m`)  
**Tecnologías:** Kotlin · Jetpack Compose (Material 3) · Arquitectura MVVM · Room · Coroutines & StateFlow · Firebase Realtime Database · Jikan REST API v4 · WorkManager

---

## 1. Descripción del Proyecto

**VITTHER** es una aplicación nativa para Android diseñada como un **museo privado y curado de figuras de colección**. A diferencia de un inventario genérico, trata cada figura como una obra de exhibición con su propio **Nicho Arqueado**, **Salas de Museo Digitales por Franquicia**, **Bodega Subterránea tipo Cava de Vinos** con etiquetas de cosecha, **Escenario de Exhibición con Iluminación Cenital y Modo Contemplar**, **Wishes con Escenario de Velo y Vitrinas Numeradas**, **Recorridos Guiados con Audioguía**, **Cápsulas de Tiempo** y **Retrato del Curador**.

La aplicación se entrega **100 % limpia de fábrica** (sin datos precargados), lista para que cada coleccionista inaugure su propio museo desde cero.

---

## 2. Arquitectura MVVM y Estructura de Paquetes

El proyecto implementa el patrón arquitectónico **Model–View–ViewModel (MVVM)** con flujo unidireccional de datos (**UDF**):

```text
UI (Jetpack Compose)  ── Eventos / Acciones ──▶  ViewModel (StateFlow / UiState)
       ▲                                                  │
       │                                                  ▼
 Estado Reactivo                               Domain UseCases / Contratos
       │                                                  │
       └──────────────  Repositories  ◀───────────────────┘
                        ├── Local: Room (PiezaDao, CapsulaDao, WishDao) + FotoStore
                        └── Remote: VittherRemoteApiClient (Jikan API + Firebase RTDB)
```

### Distribución de directorios (`com.diego.vitther`)

| Paquete | Responsabilidad |
| :--- | :--- |
| `core/common` | Utilidades transversales y contenedor de estados de operación (`ResourceState`). |
| `core/ui/theme` | Tokens de diseño y puente del sistema visual oscuro de museo (`VittherDesignSystem`). |
| `data/local` | Base de datos local **Room** (`VittherDatabase`), entidades (`PiezaEntity`, `CapsulaEntity`, `WishEntity`), DAOs (`PiezaDao`, `CapsulaDao`, `WishDao`), `Converters` y `SeedData` vacío de fábrica. |
| `data/media` | Almacenamiento privado de fotografías (`FotoStore`) dentro de `filesDir/fotos` con compresión JPEG y corrección EXIF. |
| `data/notifications` | Programación y disparo de notificaciones de cápsulas de tiempo mediante **WorkManager** (`CapsulaWorker`, `CapsulaNotifier`). |
| `data/remote/api` | Cliente HTTP y contratos de red (`VittherRemoteApiClient`, `CatalogoExternoApi`, `FirebaseMuseoApi`). |
| `data/remote/dto` | Objetos de transferencia de datos serializables (`JikanCharacterDto`, `RespaldoFirebaseDto`, `PiezaCloudDto`, `CapsulaCloudDto`, `WishCloudDto`). |
| `data/repository` | Implementación de repositorios (`PiezaRepository`, `CapsulaRepository`, `WishRepository`, `CatalogoRemotoRepository`). |
| `di` | Contenedor de inyección manual de dependencias (`AppContainer`). |
| `domain` | Reglas de negocio puras, validadores (`ValidacionPieza`), cálculo de fechas (`Fechas`), estados (`EstadoPieza`, `EstadoCapsula`, `EstadoWish`) e hitos (`Hito`). |
| `domain/model` | Modelos de dominio (`ResumenMuseoModel`). |
| `domain/repository` | Contratos de repositorios (`PiezaRepositoryContract`, `CapsulaRepositoryContract`, `WishRepositoryContract`). |
| `domain/usecase` | Casos de uso (`ObtenerResumenMuseoUseCase`, `SincronizarMuseoUseCase`). |
| `integration` | Integraciones nativas del dispositivo (`CamaraMediaIntegration`: Cámara con `FileProvider` y Galería con `PickVisualMedia`). |
| `navigation` | Grafo de navegación tipado (`VittherNavHost`, `AppNavGraph`, `Routes`, `Destino`). |
| `presentation/*` | ViewModels y contratos `UiState` por módulo (`inicio`, `coleccion`, `wishes`, `capsulas`, `historia`, `perfil`). |
| `ui/screens/*` | Pantallas en Jetpack Compose (`HomeScreen`, `ColeccionScreen`, `WishesScreen`, `CedulaScreen`, `RegistroScreen`, `HistoriaScreen`, `ProfileScreen`) y componentes vectoriales (`VittherLogo`, `PiezaCarouselCard`, `VittherNavigationBar`). |

---

## 3. Consumo de API Externa y Backend en Firebase

### A. API Externa de Catálogo de Figuras/Personajes (**Jikan REST API v4**)
- **Endpoint:** `GET https://api.jikan.moe/v4/characters?q={consulta}&limit=5&order_by=favorites&sort=desc`
- **Uso en la aplicación:** Al registrar o editar una pieza en **`RegistroScreen`**, el enlace **`✦ Autocompletar desde catálogo online`** consulta en tiempo real la API externa y sugiere nombre oficial, franquicia, línea sugerida y reseña para autocompletar la pieza.

### B. Backend en la Nube (**Firebase Realtime Database REST API**)
- **Base de datos activa:** `https://vitther-default-rtdb.firebaseio.com/`
- **Nodo principal:** `/museos/default.json` (`PUT` y `GET`)
- **Uso en la aplicación:**
  - **Sincronización automática en segundo plano:** Cada alta, edición, movimiento a bodega o eliminación de piezas, cápsulas o wishes actualiza automáticamente el respaldo en Firebase Realtime Database.
  - **Sincronización manual:** Desde **Perfil → Respaldo en la nube (`PLUS`)**, el coleccionista puede ejecutar manualmente el respaldo de sus 3 colecciones (`piezas`, `capsulas` y `wishes`).

---

## 4. Recorrido Funcional de las Pantallas (`v26.2.1`)

1. **Inicio · Inspiración Louvre (`HomeScreen`):** Encabezado con insignia vectorial `VittherLogo` y lema *"Tu museo, hoy."* sobre cielo nocturno parisino, **Pirámide de Cristal del Louvre** reticulada en segundo plano, carrusel de **nichos arqueados iluminados** (incluyendo el nicho punteado *"Un nicho te espera"*), **Cartela de Museo** con filetes dorados y puntos indicadores en forma de mini-nicho.
2. **Colección: Salas de Museo y Bodega Cava de Vinos (`ColeccionScreen`):**
   - **Modo Museo:** Cada franquicia conforma una **Sala numerada (`SALA 01`, `SALA 02`...)** con iluminación cenital doble, cuadros con marcos dorados y biselados sobre la pared, piso de duela de madera y selector modal **"Cómo ver tus figuras"** (Distribución `Variadas` / `Verticales` / `Horizontales` y Tamaño `Pequeño` / `Mediano` / `Grande`).
   - **Modo Bodega (Cava de Vinos):** Atmósfera cálida subterránea con **Arco Principal de Cava** que exhibe la *"Última en entrar"* y **Racks de Madera (`RACK 01`, `RACK 02`...)** de 3 columnas con etiquetas colgantes de pergamino (`COSECHA <año>`).
3. **Wishes: Escenario de Velo y Vitrinas de Deseo (`WishesScreen`):**
   - **Por llegar:** Escenario iluminado con la próxima figura cubierta por un **velo translúcido con estrella dorada pulsante** (que se levanta con animación al marcar **`"Ya llegó"`**), cuenta regresiva monumental en `DÍAS`, barra de progreso dorada y filas veladas para preventas adicionales.
   - **Deseadas:** Destaca **`EL GRAN DESEO`** en el escenario iluminado con insignia de corazón dorado y silueta misteriosa, seguido por una cuadrícula de **vitrinas de cristal numeradas en números romanos (`II`, `III`, `IV`...)** sobre pedestales de museo y promoción de cualquier pieza a *"Hacer mi gran deseo"*.
4. **Añadir / Editar Pieza (`RegistroScreen`):** Experiencia curatorial con **Nicho Central de Museo**, hoja modal para elegir **Galería** o **Cámara**, cartela de nombre con metadato dinámico (`SALA XX · N.º YY`), chips horizontales de salas con opción **`+ Nueva sala`**, hoja modal **`Su historia`** y pantalla final de celebración mostrando la pieza recién colocada en la pared de su sala.
5. **Detalle de Figura y Modo Contemplar (`CedulaScreen`):** Escenario de museo con riel superior, **dos lámparas doradas con haces de luz cenitales**, partículas de polvo dorado flotando en la luz, marco central con modo inmersivo **`Contemplar`**, **Placa de Bronce superpuesta** (`SALA XX · N.º YY`), pestaña **Cédula**, pestaña **Historia** con comilla monumental `“` y pestaña **Cápsulas**.
6. **Historia: Mis Recorridos Guiados (`HistoriaScreen`):**
   - **Boletería de Entrada:** Lista de boletos de museo con talón punteado, emblema vectorial (`star`, `heart`, `sword`, `crown`, `flame`, `moon`), mini-marcos superpuestos y boleto **`+ Nuevo recorrido`**.
   - **Recorrido Guiado Interactivo:** 5 temas de iluminación (`oro`, `ambar`, `carmesi`, `esmeralda`, `noche`), **puertas dobles de museo animadas** por época o sala, **Audioguía** con efecto máquina de escribir y avance automático, y plano interactivo de salas con paradas.
   - **Diseñador de Recorridos:** Vista previa en vivo del boleto, selector de emblema y tema, palabras del curador, ordenamiento (`Mi orden`, `Por años`, `Por salas`) y cuadrícula numerada de selección de piezas.
7. **Perfil: Retrato del Curador (`ProfileScreen`):** Escenario iluminado con celosía del Louvre, **Retrato Enmarcado del Curador** (con marco elegible entre `Línea dorada` y `Vitrina` con pedestal, editable con foto de Galería o Cámara), nombre personalizable del museo, y 3 vitrinas con placas de bronce superpuestas (`TU COLECCIÓN`, `AVISOS`, `PRIVACIDAD`) sobre piso de parquet de museo.

---

## 5. Estrategia de Ramas y Versionado en GitHub

El repositorio cumple estrictamente con la política institucional de control de versiones:
- **`main`:** Contiene **únicamente el cascarón del proyecto** (configuración Gradle, `AndroidManifest.xml`, recursos base y estructura de paquetes vacía con `.gitkeep`), sin código funcional `.kt`, junto con el `README.md` de presentación general del producto.
- **`develop`:** Rama principal de integración donde convergen todas las funcionalidades y la documentación técnica.
- **`feature/*`:** Ramas modulares en minúsculas separadas por guiones desde las cuales se integran las funcionalidades hacia `develop`.
- **Esquema de versión:** `26.2.1` (`AA.M.m` — Año `26`, versión mayor `2`, versión menor `1`), con Releases publicados desde la rama `develop`.

---

## 6. Requisitos e Instrucciones de Ejecución

1. **Requisitos previos:**
   - Android Studio Ladybug / Meerkat o superior.
   - JDK 11 o superior.
   - Android SDK 36/37 (`minSdk = 26`).
2. **Clonar y abrir el proyecto:**
   ```bash
   git clone https://github.com/diegohdz3/VITTHER_.git
   cd VITTHER_
   git checkout develop
   ```
3. **Compilar y ejecutar pruebas unitarias por consola:**
   ```bash
   ./gradlew compileDebugKotlin testDebugUnitTest
   ```
4. **Generar archivo APK instalable:**
   ```bash
   ./gradlew assembleDebug
   ```
   El archivo APK se genera en `app/build/outputs/apk/debug/app-debug.apk` (y copia lista como `VITTHER-v26.2.1.apk`).
