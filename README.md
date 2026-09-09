# App Test PER

Aplicación Android offline para practicar el examen teórico del Patrón de Embarcaciones de Recreo (PER).

## Funciones

- Banco inicial de **1.000 preguntas** integrado en la APK.
- Tests por los 11 temas del PER.
- Dificultad fácil, media, difícil o mixta.
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
finalización, nuevo test y simulacro.

Es una APK **debug firmada automáticamente por Android**, válida para instalar manualmente en un dispositivo Android.

## Base normativa

La estructura del simulacro sigue el Real Decreto 875/2014 y la convocatoria estatal 2026. La parte de Balizamiento se ha preparado para IALA-MBS 2022 Región A, indicada para las convocatorias estatales de 2026.

> Esta app es una herramienta personal de estudio y no sustituye el material oficial. Las preguntas son originales e inspiradas en el estilo y temario del examen, no copias literales de exámenes oficiales.
