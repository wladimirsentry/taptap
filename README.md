# Tocamatik · Auto Tap

Auto clicker para Android 8+ con burbuja flotante, rutinas guardadas y modo natural.

- Hasta 20 puntos, de 1 a 20 toques por segundo (o un toque cada varios segundos/minutos).
- Modo natural: varía ritmo, posición y duración de cada toque manteniendo el promedio.
- Zona aleatoria: cada punto puede ser un círculo (movible y de tamaño ajustable) y cada toque cae en un lugar al azar dentro de él.
- Ráfagas: N toques seguidos y descanso hasta completar el ciclo (ej. 15 toques cada 5 s); con modo natural la cantidad y la pausa varían un poco.
- Rutinas con nombre, icono y detención automática por cantidad de toques o por tiempo.
- Aviso destacado y consentimiento antes de pedir el permiso de Accesibilidad (requisito de Google Play).
- Sin Internet, sin anuncios, sin cuentas, sin recopilar datos.

## Compilación

Cada push a `main` ejecuta `.github/workflows/build.yml` y publica en **Releases**:

| Archivo | Para qué |
|---|---|
| `Tocamatik-prueba.apk` | Instalar directo en un Android (paquete `app.tocamatik.debug`) |
| `Tocamatik-N.aab` | Subir a Google Play Console (solo si los secrets de la clave existen) |

Secrets necesarios para el AAB: `TOCAMATIK_KEYSTORE_BASE64`, `TOCAMATIK_KEYSTORE_PASSWORD`, `TOCAMATIK_KEY_ALIAS`.
La clave de subida **nunca** se guarda en el repositorio.

## Estructura

| Archivo | Qué hace |
|---|---|
| `TocamatikService.java` | Servicio de accesibilidad: burbuja, panel, puntos y motor de toques (`dispatchGesture`) |
| `MainActivity.java` | Bienvenida (pasos del permiso) e inicio (estado, estadísticas, rutinas) |
| `DisclosureActivity.java` | Aviso destacado con consentimiento |
| `EditorActivity.java` | Crear/editar rutinas |
| `SettingsActivity.java` | Permiso, privacidad, retirar consentimiento, licencias |
| `Store.java` / `Routine.java` | Datos locales (SharedPreferences + JSON) |
| `Ui.java` / `Views.java` | Estilos y vistas dibujadas a mano |
| `docs/privacidad.html` | Política de privacidad (GitHub Pages) |

Técnico: Java puro sin dependencias, `minSdk 26`, `targetSdk 36`, AGP 8.11.1, Gradle 8.14.3.

## Licencias de terceros

- Tipografía Plus Jakarta Sans — SIL Open Font License 1.1 (`FONT_LICENSE_OFL.txt`).
- Iconos Lucide — licencia ISC.
