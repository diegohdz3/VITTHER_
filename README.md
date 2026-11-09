# VITTHER — Tu Museo Personal de Coleccionismo

**Materia:** Desarrollo de Aplicaciones Móviles  
**Autor:** Diego Cortés Hernández  
**Versión actual:** `26.1.1` (Esquema `AA.M.m`)  
**Tecnologías:** Kotlin · Jetpack Compose (Material 3) · Arquitectura MVVM · Room · Coroutines & StateFlow · Firebase Realtime Database · Jikan REST API v4 · WorkManager

---

## 1. Descripción del Proyecto

**VITTHER** es una aplicación nativa para Android diseñada como un **museo privado y curado de figuras de colección**. A diferencia de un inventario genérico, trata cada figura como una pieza de exhibición con su propia **Cédula**, **Estante por Franquicia**, **Cápsulas de Tiempo** (notas selladas que solo pueden revelarse cuando llega su fecha programada), **Línea de Tiempo Histórica** y lista de preventas/deseos (**Wishes**).

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
| `data/local` | Base de datos local **Room** (`VittherDatabase`), entidades (`PiezaEntity`, `CapsulaEntity`, `WishEntity`), DAOs (`PiezaDao`, `CapsulaDao`, `WishDao`), `Converters` y datos semilla (`SeedData`). |
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
- **Uso en la aplicación:** Al registrar o editar una pieza en **`RegistroScreen`**, el botón **`✦ Autocompletar desde catálogo online`** consulta en tiempo real la API externa y sugiere nombre oficial, franquicia, línea sugerida y reseña para autocompletar la cédula.

### B. Backend en la Nube (**Firebase Realtime Database REST API**)
- **Base de datos activa:** `https://vitther-default-rtdb.firebaseio.com/`
- **Nodo principal:** `/museos/default.json` (`PUT` y `GET`)
- **Uso en la aplicación:**
  - **Sincronización automática en segundo plano:** Cada alta, edición, movimiento a bodega o eliminación de piezas, cápsulas o wishes actualiza automáticamente el respaldo en Firebase Realtime Database.
  - **Sincronización manual:** Desde **Perfil → Respaldo en la nube (`PLUS`)**, el coleccionista puede verificar o ejecutar manualmente el respaldo de sus 3 colecciones (`piezas`, `capsulas` y `wishes`).

---

## 4. Recorrido Funcional de las Pantallas

1. **Inicio (`HomeScreen`):** Encabezado con insignia vectorial `VittherLogo` y lema *"Tu museo, hoy."*, carrusel 3D interactivo de piezas destacadas en exhibición, tira de métricas (`Piezas · Museo · Bodega · Franq.`), tarjeta de **Próxima cápsula** con anillo de progreso circular y sección **Un día como hoy**.
2. **Colección con Estantes (`ColeccionScreen`):** Fusión de Colección y Categorías mediante **repisas horizontales por franquicia**, pestañas `Museo` | `Bodega`, buscador desplegable y vista de detalle por franquicia.
3. **Wishes (`WishesScreen`):** Gestión de preventas **`Por llegar`** (con barra de progreso dorada y cuenta regresiva de arribo) y piezas **`Deseadas`** (con meta de ahorro en MXN). Incluye hoja de acciones para marcar **`"Ya llegó"`** (convirtiéndola automáticamente en pieza del Museo y abriendo su Cédula) o **`"Ya la compré"`**.
4. **Cédula de Figura (`CedulaScreen`):** Fotografía Hero con botones flotantes translúcidos (`Volver`, `Editar`, `Más opciones ⋯`), chips de estado/edición limitada y 3 pestañas (`Cédula` con valor estimado ocultable, `Historia` con cita personal y `Cápsulas` para sellar o revelar cápsulas de tiempo).
5. **Añadir / Editar Figura (`RegistroScreen`):** Flujo estructurado en 3 bloques (`1/3 ¿Qué pieza llegó?`, `2/3 Ubícala en tu museo`, `3/3 Cuenta su historia`) con captura de foto desde **Galería** o **Cámara**, autocompletado desde la API online y validaciones de dominio.
6. **Historia (`HistoriaScreen`):** Línea de tiempo cronológica con filtros (`Todo`, `Piezas`, `Cápsulas`), sección **`PRÓXIMAMENTE`** (cápsulas por abrirse y llegadas de Wishes) y agrupación por año.
7. **Perfil (`ProfileScreen`):** Identidad del museo, configuración de **Respaldo en la nube (`Firebase Realtime Database`)**, switches de avisos (`Cápsulas de tiempo`, `Llegadas de Wishes`) y privacidad (`Ocultar valor estimado`).

---

## 5. Estrategia de Ramas y Versionado en GitHub

El repositorio cumple estrictamente con la política institucional de control de versiones:
- **`main`:** Contiene **únicamente el cascarón del proyecto** (configuración Gradle, `AndroidManifest.xml`, recursos base y estructura de paquetes vacía con `.gitkeep`), sin código funcional `.kt`.
- **`develop`:** Rama principal de integración donde convergen todas las funcionalidades mediante Pull Requests.
- **`feature/*`:** Ramas modulares en minúsculas separadas por guiones desde las cuales se integran las funcionalidades hacia `develop`.
- **Esquema de versión:** `26.1.1` (`AA.M.m` — Año `26`, versión mayor `1`, versión menor `1`), con Releases publicados desde la rama `develop`.

---

## 6. Requisitos e Instrucciones de Ejecución

1. **Requisitos previos:**
   - Android Studio Ladybug / Meerkat o superior.
   - JDK 11 o superior.
   - Android SDK 36 (`minSdk = 26`, `targetSdk = 36`).
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
   El archivo APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.
