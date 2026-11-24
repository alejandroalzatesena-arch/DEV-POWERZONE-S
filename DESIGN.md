---
name: PowerZone
description: Plataforma de gimnasio con identidad de sala de pesas: carbón cálido, rojo intenso y fotografía real.
colors:
  primary: "#e82343"
  primary-deep: "#c8102e"
  primary-soft: "rgba(200, 16, 46, 0.16)"
  link: "#ff6b7f"
  bg: "#12100d"
  surface: "#1b1714"
  surface-alt: "#241e19"
  border: "#3a2e26"
  text: "#f7f0e7"
  muted: "#b8a899"
  cream: "#f7efe6"
  danger: "#ff4d6d"
  danger-soft: "rgba(255, 77, 109, 0.14)"
  warning: "#ffb020"
  warning-soft: "rgba(255, 176, 32, 0.14)"
  success: "#3dd68c"
  success-soft: "rgba(61, 214, 140, 0.14)"
  info: "#7fa8ff"
  info-soft: "rgba(127, 168, 255, 0.14)"
typography:
  display:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "2rem"
    fontWeight: 800
    lineHeight: "1.12"
    letterSpacing: "-0.04em"
  headline:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "1.7rem"
    fontWeight: 800
    lineHeight: "1.1"
    letterSpacing: "-0.04em"
  title:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "1rem"
    fontWeight: 700
    lineHeight: "1.3"
    letterSpacing: "-0.02em"
  body:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "0.9rem"
    fontWeight: 400
    lineHeight: "1.5"
  label:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "0.68rem"
    fontWeight: 600
    lineHeight: "1.2"
    letterSpacing: "0.16em"
rounded:
  sm: "8px"
  md: "14px"
  lg: "22px"
  pill: "999px"
spacing:
  1: "0.25rem"
  2: "0.5rem"
  3: "0.75rem"
  4: "1rem"
  5: "1.5rem"
  6: "2rem"
  7: "3rem"
components:
  button-primary:
    backgroundColor: "{colors.primary-deep}"
    textColor: "#ffffff"
    rounded: "{rounded.sm}"
    padding: "0.65rem 1.15rem"
  button-primary-hover:
    backgroundColor: "#a50d25"
    textColor: "#ffffff"
    rounded: "{rounded.sm}"
    padding: "0.65rem 1.15rem"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.text}"
    hoverBorderColor: "{colors.primary}"
    hoverTextColor: "{colors.link}"
    rounded: "{rounded.sm}"
    padding: "0.65rem 1.15rem"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.md}"
    padding: "1rem 1.5rem 1.5rem"
  input:
    backgroundColor: "{colors.surface-alt}"
    textColor: "{colors.text}"
    rounded: "{rounded.sm}"
    padding: "0.6rem 0.75rem"
  badge:
    backgroundColor: "{colors.surface-alt}"
    textColor: "{colors.muted}"
    rounded: "{rounded.pill}"
    padding: "0.2rem 0.6rem"
---

# Design System: PowerZone

## Overview

**Creative North Star: "La antesala del gimnasio"**

PowerZone se construye como un gimnasio real visto desde dentro: carbón cálido, acero, fotografía de sala de pesas y un rojo que solo aparece cuando hay una acción, una decisión o una señal de progreso. No es una consola SaaS neutra ni un catálogo de tarjetas flotantes. La jerarquía es directa, los números se leen como datos de entrenamiento y el espacio sobra entre bloques para que la pantalla respire.

Hay dos registros conectados por el mismo acento. El acceso público es claro y fotográfico: panel de marca a sangre con el logo y el slogan, y tarjeta blanca de 410px al lado. El interior autenticado es oscuro y operativo: navegación lateral, cabeceras compactas, tarjetas sobre carbón y rojo para el foco, la selección y el estado. La fotografía real aparece solo donde aporta contexto —acceso y planes—; nunca se usa como textura decorativa en el resto de la app.

**Key Characteristics:**
- Carbón cálido (#12100d, #1b1714, #241e19) como campo por defecto.
- Rojo #c8102e como acento único de acción, selección y foco; #e82343 para realces sobre fondo oscuro.
- Inter con títulos 800 y tracking -0.04em; números en `tabular-nums` cuando representan cargas, importes o fechas.
- Tarjetas de 14px de radio, borde 1px y sombra suave con desplazamiento vertical.
- Fotografía licenciada local, tratada con veladura granate, nunca depender de un host externo en runtime.

**The Red Is a Signal Rule.** El rojo no rellena superficies grandes en el interior; marca foco, selección, eyebrow, CTA y estado. Su rareza es la jerarquía.

**The Contract Eyebrow Exception.** El floor de craft prohíbe un eyebrow sobre el encabezado, pero los contratos pinnados de Login y Planes fijan "BIENVENIDO DE NUEVO" y "TU PLAN" en el primer viewport. Se conservan solo en esas dos superficies; no se replican en el resto de la app.

## Colors

La paleta es cálida y gira alrededor del rojo de marca: el fondo es carbón con un matiz marrón, y los apoyos son crema y gris cálido, nunca azul.

### Primary
- **Rojo PowerZone** (#e82343): realce sobre carbón y foco. Se usa sobre superficies oscuras donde necesita luminancia.
- **Rojo Granate** (#c8102e): acento canónico de marca, botones primarios, borde de selección y veladura fotográfica.
- **Rojo de Lectura** (#ff6b7f): enlaces y texto de acento pequeño sobre carbón; eleva el contraste por encima de 4.5:1. En auth se sustituye por Granate sobre blanco.

### Neutral
- **Carbón Profundo** (#12100d): fondo global de la app autenticada.
- **Carbón de Superficie** (#1b1714): tarjetas y paneles.
- **Carbón Elevado** (#241e19): campos, chips y superficies alternas.
- **Borde Carbón** (#3a2e26): separadores de 1px.
- **Marfil PowerZone** (#f7f0e7): texto principal sobre carbón.
- **Gris Cálido** (#b8a899): texto secundario sobre carbón.
- **Crema de Papel** (#f7efe6): fondo del panel de formulario público.
- **Tinta Cercana** (#171717): texto sobre superficies claras de auth.

### States
- **Éxito** (#3dd68c), **Alerta** (#ffb020), **Peligro** (#ff4d6d) e **Info** (#7fa8ff): solo en badges, mensajes y estados de pago, con fondos translúcidos del mismo tono.

**The Warm Carbon Rule.** El interior nunca usa gris frío ni azul pizarra; si una superficie parece neutra, debe seguir siendo marrón-carbón.

## Typography

**Display Font:** Inter (con `system-ui`, `-apple-system`, `Segoe UI` y `Roboto` como respaldo)
**Body Font:** Inter (misma pila)
**Label/Mono Font:** JetBrains Mono / Cascadia Code para identificadores técnicos; no para texto de interfaz.

**Character:** Inter aporta una voz fitness contemporánea y directa. Los títulos grandes y compactos transmiten fuerza; el cuerpo abierto y el gris cálido evitan que la densidad se sienta agresiva.

### Hierarchy
- **Display** (800, 2rem, 1.12, -0.04em): títulos de pantallas públicas y cabeceras con photographía.
- **Headline** (800, 1.7rem, 1.1, -0.04em): títulos de página en el interior.
- **Title** (700, 1rem, 1.3, -0.02em): encabezados de tarjeta y grupos.
- **Body** (400, 0.9rem, 1.5): descripciones y celdas; máximo 62–75ch en párrafos.
- **Label** (600/700, 0.68rem, 0.16em, mayúsculas): rótulos de sección y etiquetas compactas.

**The Tabular Numbers Rule.** Importes, cargas, repeticiones y fechas usan `font-variant-numeric: tabular-nums` para que las columnas se lean como una tabla, no como prosa.

## Layout

El interior usa una cuadrícula fluida de página con `gap: 1.5rem` y cabeceras flexibles que envuelven a `space-between`. El grid de tarjetas es `repeat(auto-fit, minmax(220px, 1fr))` y las secciones de decisión —método de pago y resumen— se pliegan a dos columnas antes del corte móvil. La navegación lateral cede a una barra superior compacta por debajo de 900px; los formularios de dos columnas pasan a una sola a 620px.

El acceso público es una cuadrícula de dos columnas, ~1.15fr para la fotografía y 1fr para el panel claro; a 900px se apila, con la columna fotográfica reducida a 21rem de altura. La tarjeta de acceso conserva 410px de ancho en escritorio y se adapta al viewport en móvil. Hay un único momento de entrada por superficie: `.pz-page` y `.pz-auth-card` comparten el keyframe global `pz-enter` (0.42–0.5s, desplazamiento de 8px, opacidad mínima 0.92). No se escalona sección por sección, el contenido nunca queda invisible y `prefers-reduced-motion` está resuelto globalmente.

## Elevation & Depth

La profundidad es structural pero contenida: las tarjetas usan una sombra negra difusa con desplazamiento vertical, y las superficies claras de auth usan una sombra más larga y suave. No se apilan halos de colores; el borde hace el trabajo de separación.

### Shadow Vocabulary
- **Superficie interior** (`0 8px 24px rgba(0, 0, 0, 0.3)`): tarjetas, tablas y paneles sobre carbón.
- **Tarjeta de producto** (`0 18px 40px rgba(0, 0, 0, 0.45)`): tarjetas de plan resaltadas y estados elevados.
- **Tarjeta de acceso** (`0 18px 48px rgba(24, 12, 16, 0.1)`): única sombra sobre fondo claro.

**The Offset Rule.** Toda sombra lleva desplazamiento vertical y desenfoque suave; un halo de color sin desplazamiento es decoración, no profundidad.

## Shapes

Las esquinas son redondeadas pero sobrias: 8px en botones, campos y badges pequeños; 14px en tarjetas; 22px en contenedores amplios; 999px en pills. Los bordes son de 1px y el acento de selección también se mantiene en 1px con halo de 3px en foco. No hay pestañas laterales gruesas ni esquinas recortadas.

## Components

### Buttons
- **Shape:** 8px de radio, padding `0.65rem 1.15rem`, altura compacta.
- **Primary:** fondo `#c8102e`, texto blanco, peso 700; hover `#a50d25`, active `translateY(1px)`.
- **Ghost:** fondo transparente, borde `#3a2e26`, texto marfil; hover mantiene el borde en `--pz-primary` y lleva el texto a `--pz-link` (#ff6b7f).
- **Danger:** fondo `#ff4d6d`, texto `#2b0710`; se reserva para acciones destructivas.
- **Focus:** contorno global de 2px en rojo con `outline-offset: 2px`.

### Chips / Badges
- **Style:** pill con fondo `#241e19`, borde `#3a2e26`, texto `#b8a899`, 0.72rem y peso 700.
- **State:** las variantes success, warning, danger e info sustituyen fondo/borde/texto por su rampa translúcida; nunca se mezclan con el color del CTA primario.

### Cards / Containers
- **Corner Style:** 14px.
- **Background:** `#1b1714` sobre `#12100d`; borde `#3a2e26` de 1px.
- **Shadow Strategy:** sombra interior de superficie; la selección añade borde rojo y halo suave, no una franja lateral.
- **Internal Padding:** cabecera `1rem 1.5rem 0`, cuerpo `1rem 1.5rem 1.5rem`.

### Inputs / Fields
- **Style:** fondo `#241e19` en el interior, blanco en auth; borde 1px, radio 8px, altura mínima 2.7rem interior y 2.9rem en auth.
- **Focus:** borde rojo y halo `0 0 0 3px rgba(200, 16, 46, 0.16)`.
- **Error / Disabled:** borde `#ff4d6d` con `aria-invalid="true"`; botón deshabilitado con opacidad 0.5.

### Navigation
- **Style:** barra lateral oscura con logo y slogan, ítems con icono y texto, y acento rojo sobre la ruta activa. En móvil se convierte en una tira superior scrolleable.
- **Typography:** etiqueta 0.8rem, peso 600; el texto de apoyo del ítem activo sube a marfil.

### Auth Card
- **Style:** tarjeta blanca de 410px, radio 12px, padding `2.25rem 2rem`, título 2rem/800; variante ancha de 46rem para registro.
- **Rule:** es el único lugar donde la paleta local se invierte a tinta `#171717` sobre blanco; el rojo sigue siendo `#c8102e`.

### Membership Card
- **Style:** superficie `#1b1714`, borde 1px y acento rojo de 1px en el borde izquierdo; el estado de membresía usa badge de la rampa correspondiente.

## Do's and Don'ts

### Do:
- **Do** usar `#c8102e` como acento canónico y `#e82343` solo como realce sobre carbón.
- **Do** mantener `tabular-nums` en importes, cargas y fechas.
- **Do** reservar fotografías locales para el panel de acceso y la cabecera de planes, con veladura granate.
- **Do** conservar focus visible de 2px en rojo, caret y `::selection` tematizados, scrollbars sobre carbón y objetivos táctiles cómodos en móvil.

### Don't:
- **Don't** introducir azul pizarra, gris frío ni superficies de vidrio translúcido; el interior es carbón cálido.
- **Don't** usar bordes de acento de 3px ni franjas laterales gruesas: la selección es borde de 1px + halo.
- **Don't** usar #e82343 como texto pequeño sobre carbón; para lectura usa #ff6b7f.
- **Don't** inventar precios, métricas ni estados de demo; los datos vienen del backend.
- **Don't** extender los rótulos en mayúsculas con tracking a nuevas pantallas: son una concesión al copy del Figma, no una regla heredable, salvo la excepción documentada de Login y Planes.
