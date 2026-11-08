# VITTHER

> Tu museo. Tus cápsulas. Tu historia.

Aplicación Android para coleccionistas de figuras de acción, estatuas de anime y
piezas de edición limitada. No es un inventario: es un **museo personal** donde
cada pieza tiene su cédula, su lugar (exhibición o bodega) y, si el usuario
quiere, una **cápsula de tiempo** que solo puede abrirse en una fecha futura.

Proyecto individual · Diego Cortés Hernández (253420) · Desarrollo de
Aplicaciones Móviles.

---

## Estado

**Semana 4 en curso — carrusel 3D de Inicio implementado.**

- [x] Tema oscuro museo (negro + dorado), tipografía serif embebida, esquema
      Material 3 completo y splash de marca.
- [x] Modelo de dominio y reglas de negocio puras (cápsulas, validación, fechas).
- [x] Persistencia con Room: entidades, DAOs, repositorios.
- [x] Inyección de dependencias manual (`AppContainer`).
- [x] Datos de demostración solo en builds depurables.
- [x] 23 pruebas unitarias (22 de dominio + 1 de ejemplo).
- [x] Cinco destinos con navegación tipada y barra inferior con etiquetas.
- [x] Colección: Museo/Bodega, búsqueda y cuadrícula adaptable.
- [x] Registro con galería/cámara, copia privada de fotos, orientación EXIF y límite de 1280 px.
- [x] Cédula: editar, mover, destacar y eliminar con confirmación.
- [x] Cápsulas de tiempo: sellado, bloqueo de la nota antes de la fecha, revelación y aviso en Inicio.
- [x] Notificaciones con WorkManager, permiso de Android 13+, canal y aviso único por cápsula.
- [x] Historia derivada de adquisiciones, registros y cápsulas reveladas.
- [x] Categorías conectadas a la colección filtrada.
- [x] 8 pruebas instrumentadas: CRUD, persistencia, cápsulas, notificación y carrusel.
- [x] Home con HorizontalPager 3D, flechas, indicadores, aviso de cápsula futura y estados vacío/carga/error.
- [ ] Export/import y pulido final (semana 4).

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Room + KSP
- MVVM con `StateFlow` y repositorios
- AGP 9 · minSdk 26 · targetSdk 37
- Sin backend: todo vive en el dispositivo (offline-first)

## Arquitectura

```
com.diego.vitther
├── VittherApp.kt          Application + contenedor de dependencias
├── MainActivity.kt        Edge-to-edge + splash + entrada a Compose
├── di/                    AppContainer (DI manual)
├── domain/                Reglas puras: fechas, estado de cápsula, validación, hitos
├── data/
│   ├── local/             Entidades, DAOs, base de datos Room y seed
│   ├── media/             Importación y limpieza de fotos privadas
│   ├── notifications/     Worker y programación de avisos
│   └── repository/        PiezaRepository, CapsulaRepository (Flow)
├── navigation/            Rutas tipadas y NavHost
└── ui/
    ├── theme/             Color, Theme, Type
    ├── components/        Foto, emblemas y estados vacíos
    └── screens/           coleccion/ e historia/
```

### Decisiones que vale la pena conocer

- **La foto se copia al almacenamiento privado** y se guarda una ruta relativa,
  no la URI del selector de imágenes. Así no depende de permisos temporales
  ni de que el usuario conserve el archivo original en la galería.
- **Las fechas son `Long` en epoch millis**, normalizadas al inicio del día
  local para adquisiciones; el selector Material usa UTC y se convierte por día
  de calendario antes de guardar. Los instantes de registro conservan su hora.
- **Los enums se guardan como texto**, no como ordinal: agregar estados en el
  futuro no corrompe datos existentes.
- **La regla de la cápsula vive en el repositorio**, no en la pantalla. Ninguna
  vista futura (notificación, widget) puede saltarse el sellado.
- **La nota de una cápsula no se cifra**, solo se oculta hasta su apertura. Por
  eso el backup del sistema está desactivado: la base y las fotos no se envían
  a la nube. El cifrado y la biometría quedan para una etapa posterior.
- **Las notificaciones usan WorkManager horario**, no alarmas exactas: Android
  puede retrasarlas por ahorro de batería. Cada cápsula avisa una sola vez.
- **Al eliminar una pieza o reemplazar su foto se borra el archivo privado**;
  también se limpian los temporales de cámara.
- **Sin Hilt**: un `AppContainer` explícito es suficiente y mantiene el build
  simple para un proyecto individual.

## Compilar y probar

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
# Con un emulador o teléfono conectado:
./gradlew connectedDebugAndroidTest
```

Los datos de ejemplo se cargan una sola vez en builds depurables, dentro de una
transacción. Vaciar la colección no vuelve a crearlos al reiniciar.

## Probar el recorrido

1. Colección → Agregar pieza → elegir foto de Galería o Cámara.
2. Escribir nombre y franquicia, revisar la fecha y guardar.
3. Desde la cédula: editar nota/procedencia/ubicación, mover a bodega y destacar.
4. Reiniciar la app: pieza y foto deben seguir disponibles.
5. Cédula → Crear cápsula → escribir un mensaje y elegir una fecha futura.
6. Salir y volver a entrar: la nota debe seguir oculta y la cápsula sellada.
7. Cuando llegue la fecha, pulsar “Revelar cápsula”; la nota aparece y se añade
   a Historia. La notificación se puede activar desde la cédula o Perfil.
8. Categorías → abrir una franquicia; debe mostrar solo sus piezas.
9. Eliminar una pieza y confirmar; también se eliminan sus cápsulas y su foto.

Inicio muestra un carrusel 3D y el aviso de la próxima cápsula futura;
la exportación/importación y la búsqueda global siguen pendientes. La
captura con una cámara física necesita validación en teléfono; los tests cubren
importación y almacenamiento.

## Probar el carrusel

- Inicio: deslizar horizontalmente o usar las flechas. Tocar la tarjeta central abre su cédula.
- Se usa `observarVitrina()`: destacadas en exhibición; si no hay destacadas, hasta seis exhibidas recientes.
- Los puntos representan las piezas del carrusel, nunca las de Bodega.
- Los ejemplos de `SeedData` no incluyen fotos: muestran el placeholder dorado. Agregar una foto desde la cédula permite comprobar la viñeta.
- Mover todas las piezas a Bodega muestra “Tu vitrina está vacía”.
- `HomeScreen.kt` incluye previews con datos y vacío; `HomeCarouselTest` verifica flechas, gesto, callback y eliminación de páginas durante la sesión.

Navigation 2.9.0 y Coil 3.2.0 se mantienen compatibles con Kotlin 2.2.10.
Las versiones 2.10.2/3.6.3 probadas exigían metadatos Kotlin más recientes.
