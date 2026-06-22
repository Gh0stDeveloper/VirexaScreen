# Virexa Screen

Starter project Android en Kotlin + Jetpack Compose para grabación de pantalla.

## Incluye

- Navegación Compose
- Pantallas de splash, onboarding, home, biblioteca, ajustes y detalle
- Preferencias persistentes con DataStore
- Grabación básica con MediaProjection + MediaRecorder
- Servicio de burbuja flotante
- Reproducción local con Media3 ExoPlayer
- Compartición mediante FileProvider

## Notas técnicas

- El audio interno y el audio de llamadas dependen de la versión de Android y de restricciones del sistema. Esta base deja la arquitectura lista y muestra la limitación en UI.
- La grabación se guarda en `Android/data/com.virexa.screen/files/Movies/<carpeta>/`.
- La burbuja flotante requiere permiso de superposición.

## Siguiente paso recomendado

- Abrir el proyecto en Android Studio.
- Sincronizar Gradle.
- Ajustar versiones de dependencias si el entorno local lo requiere.
- Probar captura real en dispositivo físico.
