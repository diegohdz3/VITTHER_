# VITTHER — *Tu museo, hoy.*

**Desarrollado por:** Diego Cortés Hernández  
**Materia:** Desarrollo de Aplicaciones Móviles  
**Versión actual:** `26.1.1`

---

## ¿Qué es VITTHER?

**VITTHER** es una aplicación móvil concebida como un **museo privado y personal para coleccionistas de figuras**.

Mientras la mayoría de las aplicaciones tratan una colección como una simple lista de compras o un inventario frío lleno de tablas, **VITTHER** nace de una idea distinta: cada figura tiene una historia, un lugar en tu vitrina y un significado personal. En lugar de registrar productos, en VITTHER **curas tu propio museo**.

---

## ¿Cómo se desarrolló la experiencia?

El desarrollo de **VITTHER** se centró en trasladar la atmósfera de una galería de exhibición nocturna a la pantalla del teléfono:

- **Estética de vitrina:** Fondo negro profundo, acentos en oro cálido (`#E8C97A`) y tipografía editorial serif que le da a cada figura el protagonismo de una pieza de museo.
- **De la espera a la vitrina:** Acompaña al coleccionista en todo el ciclo de vida de una pieza: desde que es un deseo o una preventa en camino (**Wishes**), hasta que llega a casa, se fotografía, recibe su **Cédula de exhibición**, toma su lugar en los **estantes por franquicia** y guarda recuerdos hacia el futuro mediante **Cápsulas de Tiempo**.
- **Privacidad y pertenencia:** El museo comienza completamente limpio de fábrica para que cada coleccionista construya su propio acervo desde cero, con la tranquilidad de poder ocultar los valores estimados de sus piezas cuando muestra su colección a otras personas.

---

## Secciones Principales de la Aplicación

### 1. Inicio — *Tu museo, hoy.*
La entrada principal a tu galería. Presenta el emblema de **VITTHER** seguido de un **carrusel 3D interactivo** con las piezas destacadas que tienes actualmente en exhibición.Debajo del carrusel encontrarás:
- **Resumen en vivo de tu museo:** Total de piezas, cuántas están en exhibición (*Museo*), cuántas en *Bodega* y cuántas *Franquicias* conforman tu colección.
- **Próxima cápsula:** Un recordatorio visual con anillo de progreso que indica cuántos días faltan para que pueda abrirse tu siguiente cápsula de tiempo.
- **Un día como hoy:** Destaca la historia personal y los días transcurridos desde que adquiriste una de tus piezas.

### 2. Colección con Estantes (Repisas)
El corazón del museo, accesible desde el emblema central de la barra inferior. Fusiona la vista de colección y categorías en **repisas horizontales organizadas por franquicia**:
- **Pestañas Museo y Bodega:** Separa las figuras que tienes actualmente exhibidas en vitrina de aquellas que conservas resguardadas en caja o bodega.
- **Repisas por franquicia:** Cada franquicia tiene su propio estante; al tocar el encabezado de una franquicia puedes ver el listado completo de figuras que pertenecen a ella.
- **Buscador rápido:** Permite localizar cualquier pieza o franquicia al instante.

### 3. Wishes — *Por llegar y Deseadas*
El espacio dedicado a las próximas incorporaciones de tu museo:
- **Por llegar:** Para figuras compradas en preventa o en tránsito, mostrando una barra de progreso dorada y los días restantes para su llegada. Cuando la figura llega a tus manos, basta con tocar **`"Ya llegó"`** para que pase automáticamente a formar parte de tu Museo.
- **Deseadas:** Tu lista de deseos con la meta estimada en MXN para planear tus próximas adquisiciones y pasarlas a *Por llegar* con el botón **`"Ya la compré"`**.

### 4. Cédula de la Pieza y Cápsulas de Tiempo
Al tocar cualquier figura del museo se abre su **Cédula de exhibición**, con un vitral amplio para apreciar la fotografía completa de la pieza y tres pestañas:
- **Cédula:** Fecha de adquisición, procedencia, ubicación física, serie/edición limitada y valor estimado (oculto por defecto tras `•••••• MXN` hasta que decidas mostrarlo).
- **Historia:** La nota personal de lo que esa pieza significa para ti.
- **Cápsulas:** Te permite escribir y **sellar una cápsula de tiempo** asociada a la figura eligiendo una fecha futura. La nota permanece bloqueada e ilegible hasta que llega el día programado, momento en que la aplicación te avisa que ya puedes revelarla.

### 5. Añadir Figura
Un recorrido guiado en **3 pasos sencillos** (`1/3 ¿Qué pieza llegó?`, `2/3 Ubícala en tu museo` y `3/3 Cuenta su historia`) que permite tomar una fotografía con la cámara o elegirla de la galería, autocompletar información desde el catálogo en línea y decidir si la pieza va directo a exhibición o a bodega.

### 6. Historia
Una línea de tiempo cronológica que narra la evolución de tu colección año por año: desde cuál fue **tu primera pieza**, pasando por cada adquisición y cápsula revelada, hasta la sección **Próximamente** con las preventas por llegar y las cápsulas próximas a abrirse.

### 7. Perfil — *Mi museo*
El panel personal del coleccionista donde puedes personalizar el nombre de tu museo, respaldar tu colección en la nube y configurar tus preferencias de avisos y privacidad.

---

## Créditos y Autoría

- **Autor y Desarrollador:** Diego Cortés Hernández
- **Proyecto Integrador:** Desarrollo de Aplicaciones Móviles

> **Nota sobre la documentación técnica:**  
> Esta rama (`main`) contiene la presentación general del proyecto y la estructura base del repositorio. Toda la **documentación técnica de ingeniería, arquitectura MVVM, consumo de APIs REST, base de datos en Firebase/Room e instrucciones de compilación** se encuentra detallada en el `README.md` de la rama **[`develop`](https://github.com/diegohdz3/VITTHER_/tree/develop)**.
