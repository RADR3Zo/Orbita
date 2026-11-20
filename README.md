# Órbita 1.6.0

Cliente de Minecraft con estética espacial: un **launcher para Windows** (Java 21, Swing) más un **mod cliente de Fabric** para Minecraft **1.21.11**.

## Uso rápido

1. Abre `Orbita\Orbita.exe`. No necesitas instalar Java: va incluido.
2. Escribe tu nombre de jugador (modo offline) y pulsa **JUGAR**.
   - La primera vez descarga Minecraft 1.21.11, Fabric, Fabric API, el núcleo de Órbita y el pack de rendimiento.
   - Todo se guarda en `%APPDATA%\.orbita`, separado de `.minecraft`.
   - Si vienes de otra versión, tus mods se guardan en `mods-backup-<versión>` y se reinstalan para 1.21.11.
     Cualquier `.jar` de otra versión que aparezca en `mods` se aparta solo a `mods-incompatibles`.
3. Dentro del juego:
   - **Shift derecho**: menú de mods de Órbita.
   - **Z** (mantener): zoom. La rueda del ratón ajusta el aumento.
   - **Alt izquierdo** (mantener): Freelook, para mirar alrededor sin girar a tu personaje.
   - **Fullbright**: luz máxima en cuevas y de noche. Se enciende y apaga en su tarjeta del menú de mods (Mecánicas); también puedes asignarle una tecla.
   - **F7 / F8 / F9**: Spotify, canción anterior / pausar o reanudar / siguiente.
   - Todas las teclas se cambian desde el menú de mods (opciones de Zoom, Freelook y Spotify) o en Opciones → Controles → Órbita.

## Qué incluye

| Parte | Funciones |
|---|---|
| Menú principal | Entradas de texto a la izquierda, planeta con anillo inclinado, luna, cohete en órbita con estela, neblina, tarjeta de cuenta con tu cara y tipografía Raleway |
| Perfil | Tu jugador de pie sobre el planeta (arrastra para girarlo), datos de la cuenta y selector de capas |
| Menú de mods (estilo Lunar) | Buscador, categorías (Todos / HUD / Mecánicas / Otros), tarjetas con ACTIVADO/DESACTIVADO y opciones por módulo: fondo, corchetes, sombra, color (incluido arcoíris) y escala |
| HUD (21 módulos) | FPS, CPS, Ping, Coordenadas, Brújula, Keystrokes, **Spotify**, **Toggle Sprint / Toggle Sneak**, Pociones, Armadura, Reloj, Memoria, Servidor, Velocidad, Bioma, Día, Combo, Alcance, **Sesión** (tiempo jugando), **Luz** (luz de bloque y de cielo) y **Mirando** (bloque o criatura a la que apuntas). Cada uno con su icono (reloj, brújula, diamante…) |
| Spotify | Portada de la canción, título, artista, ecualizador y texto deslizante; teclas para pausar, siguiente y anterior. Lee la app de Spotify de Windows sin cuenta ni claves. La portada se busca por nombre en iTunes o Deezer (se puede desactivar) |
| Letras | Letra sincronizada de la canción de Spotify en 3D. Por defecto **Dispersas**: cada palabra aparece justo cuando se canta en un punto al azar de donde estás mirando, con tamaños e inclinaciones distintos, brillo y fundido a los pocos segundos; al empezar una canción flota una tarjeta «Sonando ahora». Otros modos: en el cielo (se queda donde aparece), delante de ti o en una caja; palabra a palabra, 8 estilos (Lavanda, Soñador, Fuego, Hielo, Real, Cómic, Órbita, Limpio), 7 fuentes, tamaño y desfase. Spotify también tiene estilo píldora con el tiempo transcurrido y la línea que suena. Tras un anuncio las letras siguen sincronizadas. Se sincronizan al instante con la posición real de Spotify que da Windows (aunque la canción ya vaya por la mitad o la adelantes), y cada línea aparece en un punto distinto del cielo a tu alrededor. Las letras se descargan de LRCLIB |
| Traído de Nova | Pestañas Cosméticos, Visual y Rendimiento en el menú de mods: alas, capa, halo, corona, sombrero, bandana, mascota, monedas, estela, anillos de aura, sello en el suelo y emotes (tecla B); cielos animados y video de fondo (MP4/GIF en .orbita\nova\videos, también en el menú principal); modo ahorro Medio/Extra, FPS Boost, FPS inteligentes, desbloquear FPS, modo ligero y sin partículas/nubes/lluvia; efectos de crítico, nombres y vida al apuntar, cambiar tu nombre solo en tu pantalla y contador de manzanas doradas (HUD) |
| Launcher | Página **Cuentas** (varias cuentas sin premium y de Microsoft, eliges con cuál jugar) y página **Rendimiento** (tus FPS por partida con gráfico, tu equipo, perfiles de gráficos y RAM recomendada) |
| Apariencia del launcher | Animación de inicio por el universo (se salta con clic, Esc, Espacio o Enter), 8 temas de color o uno personalizado, y fondo de universo, color, imagen (también GIF) o video MP4 (H.264) con o sin sonido, más música de fondo (MP3/WAV/M4A; los .aac sueltos no funcionan). El selector de archivos es el de Windows y se abre en tus carpetas de Videos, Música o Imágenes. Todo se pausa mientras juegas |
| Fullbright | Ves todo con luz máxima aunque esté oscuro. Solo en tu pantalla (visión nocturna local, sin icono ni partículas) y no aparece en el HUD de Pociones. Se activa y desactiva desde su tarjeta o con una tecla que elijas |
| Freelook | Cámara libre en tercera persona; modo mantener o alternar e invertir el eje vertical |
| Rendimiento | Recolector ZGC generacional (pausas de menos de 1 ms; G1 opcional en Ajustes), memoria de Java fija, texto del HUD en caché, arte del menú generado en segundo plano y el pack con Dynamic FPS y MoreCulling. El launcher oculto no gasta CPU y Spotify solo lee un título de ventana cada 1,5 s (5 s con el juego en segundo plano; nada si el módulo está apagado) |
| Editor de HUD | Arrastrar para mover, rueda para escalar y clic derecho para abrir las opciones del módulo |
| Zoom | Transición suave, cámara cinemática opcional y nivel ajustable |
| Launcher | Iconos reales de los mods de Modrinth (WebP incluido), gestor de mods con dependencias, actualizaciones y pack de rendimiento |

## Actualizaciones automáticas

Desde la 1.6.0 el launcher se actualiza solo desde **GitHub Releases**:

- Al abrirlo, mira si hay una release más nueva en el repositorio de `github_repo` (gradle.properties).
- Si la hay, enseña las novedades y, con **Actualizar ahora**, descarga el jar nuevo, lo comprueba con su SHA-256,
  cambia `app\Orbita.cfg` y reinicia Órbita. El mod del juego va dentro del jar, así que también se actualiza.
- Si en las notas de la release aparece `[completa]` (por ejemplo, porque cambió el Java incluido), pide descargar el `.zip` entero.

Para publicar una versión nueva: sube `orbita_version`, haz commit y sube una etiqueta `vX.Y.Z`.
GitHub Actions compila, prueba y publica la release solo. Los pasos detallados están en [GUIA-GITHUB.md](GUIA-GITHUB.md).

## Seguridad

- Los tokens de las cuentas de Microsoft se guardan **cifrados con DPAPI de Windows** en `launcher.json`:
  solo tu usuario, en tu PC, puede leerlos. Los archivos viejos sin cifrar se cifran solos al guardar.
- El archivo temporal con los argumentos del juego (que lleva el token de la sesión) se borra a los pocos segundos de arrancar.

## Limitaciones actuales

- **Inicio de sesión Microsoft**: el código está implementado, pero Mojang exige un *Client ID* de Azure aprobado. Pégalo en **Ajustes → Cuenta Microsoft** cuando lo tengas. Hasta entonces solo funciona el modo offline.
- **Cosméticos**: las capas solo las ves tú. Para que otros jugadores las vean hace falta un servidor de cosméticos propio. Alas y emotes: pronto.

## Compilar

```powershell
.\build.ps1 -JavaHome <ruta JDK 21> -GradleJavaHome <ruta JDK 25>
```

Genera en `dist\`: la carpeta `Orbita` con `Orbita.exe`, el `.zip` para repartir, y `orbita-launcher-<versión>.jar` + `.sha256` (lo que descarga el actualizador).

### Pruebas

```powershell
.\gradlew.bat :launcher:test
```

Las pruebas están en `launcher\src\test` (versiones, actualizador, rutas Maven, reglas de librerías, hashes y cifrado de tokens).
`build.ps1` las ejecuta siempre antes de empaquetar, y GitHub las ejecuta en cada push.

### Estructura del código

| Carpeta | Qué hay |
|---|---|
| `launcher/…/core/install` | Descarga e instalación de Minecraft, Fabric y librerías |
| `launcher/…/core/launch` | Línea de comandos y arranque del juego |
| `launcher/…/core/auth` | Cuentas, login de Microsoft y cifrado de tokens (`TokenVault`) |
| `launcher/…/core/update` | Actualizaciones desde GitHub (`Updater`, `Version`) |
| `launcher/…/ui` | Ventana del launcher (Swing) |
| `client/…/ui/screen` | Pantallas del juego. Las tarjetas del menú de mods están en `PanelEntries` |
| `client/…/hud` | Módulos del HUD de Órbita |
| `client/…/nova` | Funciones traídas de Nova (cosméticos, cielos, video, rendimiento) |

Loom 1.18 (Minecraft 1.21.11) necesita Java 25 para ejecutar Gradle. El mod se compila para Java 21 y `Orbita.exe` incluye un runtime Java 21.

Modo sin interfaz (para pruebas o scripts):

```powershell
java -jar orbita-launcher-1.6.0.jar --headless --install-only
java -jar orbita-launcher-1.6.0.jar --headless --username Steve
```

Con `-Dorbita.devshots=true` en *Argumentos JVM extra*, el mod recorre sus pantallas, crea un mundo de prueba y guarda capturas en `screenshots/orbita-*.png` (solo para desarrollo).

---

Órbita no está afiliado a Mojang Studios ni a Microsoft. Minecraft es una marca de Mojang Synergies AB. La fuente Raleway se distribuye bajo la SIL Open Font License (`assets/orbita/font/ofl_license.txt`).
