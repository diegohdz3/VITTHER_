# VITTHER — Tu Museo Personal de Coleccionismo

> *"Tu museo, hoy."*

**Autor:** Diego Cortés Hernández  
**Versión actual:** `26.2.0`  
**Proyecto:** Desarrollo de Aplicaciones Móviles

---

## ¿En qué consiste VITTHER?

**VITTHER** nace de una idea sencilla pero profunda: **una colección de figuras no es una lista de compras ni una hoja de inventario; es un museo privado donde cada pieza guarda un momento de tu vida.**

Muchas veces el coleccionista adquiere una figura que esperó durante meses o años, la coloca en su vitrina y, con el paso del tiempo, olvida la historia detrás de cómo llegó, cuánto significó en ese instante o qué pensaba el día que la abrió. **VITTHER** transforma el teléfono en la entrada a tu propio museo personal: un espacio íntimo, oscuro y curado con iluminación de galería donde cada figura recibe el trato de una obra de exhibición.

---

## ¿Cómo se desarrolló la experiencia?

El desarrollo de **VITTHER** se concibió desde la mirada del coleccionista y la museografía contemporánea:

1. **Atmósfera inspirada en los grandes museos (Louvre y Cava Privada):**  
   Se diseñó una interfaz oscura con acentos dorados (`#E8C97A`) que elimina el ruido visual para que las fotografías de tus figuras sean las verdaderas protagonistas. Al cambiar entre **Museo** y **Bodega**, la aplicación transforma su atmósfera de una galería nocturna iluminada a una cálida **cava subterránea de madera y piedra**.
2. **Organización espacial auténtica (Salas y Racks):**  
   En lugar de tablas frías, la colección se recorre a través de **Salas de Museo numeradas por franquicia (`SALA 01`, `SALA 02`...)** donde tus piezas cuelgan enmarcadas sobre paredes iluminadas con piso de duela de madera, mientras que las piezas en resguardo descansan en **Racks de Cava** con etiquetas colgantes de pergamino (`COSECHA <año>`).
3. **Recorridos Guiados y Cápsulas de Tiempo:**  
   Además de la **Placa de Bronce y Cédula** de cada pieza y sus **Cápsulas de Tiempo**, el coleccionista puede diseñar sus propios **Recorridos Guiados** con boletos personalizados, puertas dobles de museo y **Audioguía**.
4. **Ciclo completo del coleccionista (de fábrica):**  
   La aplicación inicia **completamente limpia de fábrica**, lista para que cada coleccionista o amigo registre sus deseos, preventas, figuras y recorridos desde cero.

---

## Recorrido por la Aplicación (`v26.2.0`)

- **Inicio (Inspiración Louvre):**  
  Te recibe con el emblema de VITTHER, el lema *"Tu museo, hoy."* y la **Pirámide de Cristal reticulada** detrás de un carrusel de **nichos arqueados iluminados** que exhiben tus piezas destacadas junto a su cartela de museo, incluyendo un nicho especial (*"Un nicho te espera"*) listo para tu próxima adquisición.
- **Colección (Museo Digital y Bodega tipo Cava):**  
  - **Museo:** Agrupa tus figuras en **Salas por franquicia**, con cuadros enmarcados en oro y bisel oscuro, selector de distribución (*Variadas*, *Verticales*, *Horizontales*) y tamaño de marcos.
  - **Bodega:** Presenta un **Arco Principal de Cava** con la *"Última en entrar"* y estanterías de madera (**Racks**) con etiquetas de cosecha.
- **Wishes (Escenario de Velo y Vitrinas de Deseo):**  
  - **Por llegar:** Exhibe la pieza más próxima a llegar sobre un escenario iluminado detrás de un **velo translúcido con estrella dorada pulsante** y una cuenta regresiva monumental en días. Al tocar *"Ya llegó"*, el velo se levanta suavemente para revelar la obra e incorporarla al museo.
  - **Deseadas:** Consagra tu pieza más anhelada como **`EL GRAN DESEO`** bajo reflectores cenitales y organiza el resto en **vitrinas de cristal numeradas en números romanos (`II`, `III`, `IV`...)** sobre pedestales de museo.
- **Añadir y Editar Pieza (El Nicho Central):**  
  Un flujo limpio centrado en el **Nicho de Museo**: coloca la fotografía desde tu cámara o galería, escribe su nombre en la cartela, elige o crea una **`+ Nueva sala`** y, al guardarla, contempla cómo tu pieza toma su lugar en la pared de la sala.
- **Detalle de Figura y Modo Contemplar:**  
  Exhibe tu pieza enmarcada bajo un **riel con dos lámparas doradas, haces de luz cenitales y partículas flotantes**. Incluye el botón **Contemplar** para admirar la obra a pantalla completa sin distracciones, una **Placa de Bronce** con su sala y número de catálogo, su **Cédula**, su **Historia** con tipografía editorial y sus **Cápsulas de Tiempo**.
- **Historia (Mis Recorridos Guiados):**  
  Una boletería donde puedes crear y vivir **Recorridos Guiados** por tu colección: elige el nombre, el emblema (`estrella`, `corazón`, `espada`, `corona`, `llama`, `luna`), el tema de iluminación (`oro`, `ámbar`, `carmesí`, `esmeralda`, `noche`), el orden (*Mi orden*, *Por años*, *Por salas*) y las piezas. Al iniciar el recorrido, **las puertas dobles del museo se abren** hacia cada sala o época con **Audioguía** integrada.
- **Perfil (Retrato del Curador):**  
  Presenta el **Retrato Enmarcado del Curador** bajo iluminación cenital (con marco elegible entre *Línea dorada* y *Vitrina*), el título de *"Mi museo"* y 3 vitrinas con placas de bronce (`TU COLECCIÓN`, `AVISOS`, `PRIVACIDAD`) para respaldar en la nube (`PLUS`) y configurar tu privacidad.

---

## Autoría y Créditos

- **Concepción, Diseño de Experiencia y Desarrollo:** Diego Cortés Hernández (`@diegohdz3`)
- **Filosofía del proyecto:** Hecho para coleccionistas que ven en cada figura una historia digna de ser exhibida y recordada.

---

> 📌 **Nota sobre la estructura del repositorio:**  
> Esta rama (`main`) contiene la presentación general del proyecto y la estructura base del repositorio. Toda la documentación técnica de arquitectura, paquetes, base de datos, consumo de APIs e instrucciones de compilación se encuentra en el `README.md` de la rama **[`develop`](../../tree/develop)**.
