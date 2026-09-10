# App Test PER

Aplicación Android offline para practicar el examen teórico del Patrón de Embarcaciones de Recreo (PER).

## Funciones

- Banco inicial de **1.000 preguntas** integrado en la APK.
- Tests por los 11 temas del PER.
- Selección de todas las preguntas del tema, sin filtro de dificultad.
- Una pregunta por etiqueta en cada test, incluido el simulacro; se priorizan conceptos y variantes no recientes. Si se supera el máximo de conceptos del tema, se solicita reducir la cantidad.
- Selección de respuesta + botón **Confirmar**.
- Respuesta correcta en verde y errónea en rojo tras confirmar.
- Navegación **Anterior / Confirmar / Siguiente**.
- Las preguntas no confirmadas no cuentan como fallo hasta terminar el test.
- En la última pregunta, **Siguiente** cambia a **Terminar**.
- Simulacro directo de **45 preguntas / 90 minutos** con distribución oficial.
- Criterio de aprobado: mínimo 32 aciertos y máximos de error: RIPA 5, Balizamiento 2, Carta 2.
- Resultado final con nota, tiempo y temas a repasar.
- Estadísticas locales desglosadas por materia y opción para reiniciarlas.
- Evita en lo posible repetir las preguntas usadas recientemente.
- Funciona sin Internet y sin API de pago.
- Ajustes en un panel lateral derecho, accesible mediante el engranaje de la cabecera, con interruptor **Modo noche**, aplicado a todas las pantallas y guardado localmente.
- Cabecera del test con más espacio superior y controles fuera de las barras del sistema.

## Distribución del banco

| Tema | Preguntas |
|---|---:|
| Nomenclatura náutica | 70 |
| Elementos de amarre y fondeo | 60 |
| Seguridad | 90 |
| Legislación | 70 |
| Balizamiento | 120 |
| RIPA | 180 |
| Maniobra | 80 |
| Emergencias en la mar | 70 |
| Meteorología | 80 |
| Teoría de navegación | 90 |
| Carta de navegación | 90 |
| **Total** | **1.000** |

## APK

### Animaciones de resultados

Todas las modalidades actuales comparten `showResults()`. Su cabecera usa
`ResultScoreView(context, new TestResult(aciertos, total), color)`; un futuro modo
puede reutilizarla con sus datos sin repetir el cálculo ni los umbrales.
`TestResult` calcula `(aciertos / total) * 100` con decimales: menos de 50 usa
`result_sad.webp`, desde 50 hasta menos de 80 usa `result_good.webp`, y desde 80
usa `result_celebration.webp`. No se redondea antes de seleccionar el archivo.

Los tres archivos están en `app/src/main/assets/`. Se pueden sustituir manteniendo
exactamente sus nombres y recompilando. Se incluyen animaciones geométricas
originales de ejemplo; el generador opcional está en `tools/generate_result_animations.py`
(Pillow, no necesario para compilar Android).

El reproductor usa el WebView del sistema, compatible con WebP animado también
en Android 8, sin librerías adicionales ni JavaScript ni acceso a red. Conserva
la proporción con `object-fit: contain`, fuerza la repetición en memoria y libera
el reproductor al salir. La nota conserva su estilo y la imagen ocupa hasta 88 dp;
la cabecera pasa a vertical si falta espacio, incluido el texto ampliado.

Pruebas: límites 0/49/50/79/80/100, porcentajes fraccionarios, varias longitudes,
resultados por temas y simulacro (incluido tiempo agotado), tamaños de cabecera,
reproducción/pausa y conservación de estadísticas.

El proyecto Android está en la raíz del repositorio (`app/`, `build.gradle` y
`settings.gradle`), como archivos normales. El workflow no descomprime ningún ZIP.

Con Java 17, Gradle 8.7 y Android SDK 35 instalados, ejecutar desde la raíz:

```sh
gradle testDebugUnitTest --no-daemon
gradle assembleDebug --no-daemon
```

La APK local queda en `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions ejecuta las pruebas y compila con cada push a `main`, o manualmente
mediante `workflow_dispatch`. Descargar el artefacto **Test-PER-APK** de la ejecución
del workflow **Compilar APK Test PER**.

Las pruebas de regresión ejecutan la actividad en Android 8 (API 26) y Android 15
(API 35): creación del test, disposición de las vistas, navegación, confirmación,
finalización, nuevo test y simulacro. También verifican los modos día/noche,
su persistencia y el espacio reservado para las barras del sistema.

Es una APK **debug firmada automáticamente por Android**, válida para instalar manualmente en un dispositivo Android.

## Base normativa

La estructura del simulacro sigue el Real Decreto 875/2014 y la convocatoria estatal 2026. La parte de Balizamiento se ha preparado para IALA-MBS 2022 Región A, indicada para las convocatorias estatales de 2026.

> Esta app es una herramienta personal de estudio y no sustituye el material oficial. Las preguntas son originales e inspiradas en el estilo y temario del examen, no copias literales de exámenes oficiales.
