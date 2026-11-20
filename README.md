# Órbita 1.4.2

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
   - **F7 / F8 / F9**: Spotify, canción anterior / pausar o reanudar / siguiente.
   - Todas las teclas se cambian desde el menú de mods (opciones de Zoom, Freelook y Spotify) o en Opciones → Controles → Órbita.

## Qué incluye

| Parte | Funciones |
|---|---|
| Menú principal | Entradas de texto a la izquierda, planeta con anillo inclinado, luna, cohete en órbita con estela, neblina, tarjeta de cuenta con tu cara y tipografía Raleway |
| Perfil | Tu jugador de pie sobre el planeta (arrastra para girarlo), datos de la cuenta y selector de capas |
| Menú de mods (estilo Lunar) | Buscador, categorías (Todos / HUD / Mecánicas / Otros), tarjetas con ACTIVADO/DESACTIVADO y opciones por módulo: fondo, corchetes, sombra, color (incluido arcoíris) y escala |
| HUD (18 módulos) | FPS, CPS, Ping, Coordenadas, Brújula, Keystrokes, **Spotify**, **Toggle Sprint / Toggle Sneak**, Pociones, Armadura, Reloj, Memoria, Servidor, Velocidad, Bioma, Día, Combo y Alcance. Cada uno con su icono (reloj, brújula, diamante…) |
| Spotify | Portada de la canción, título, artista, ecualizador y texto deslizante; teclas para pausar, siguiente y anterior. Lee la app de Spotify de Windows sin cuenta ni claves. La portada se busca por nombre en iTunes o Deezer (se puede desactivar) |
| Letras | Letra sincronizada de la canción de Spotify en 3D: en el cielo (se queda donde aparece), delante de ti o en una caja; palabra a palabra, 8 estilos (Lavanda, Soñador, Fuego, Hielo, Real, Cómic, Órbita, Limpio), 7 fuentes, tamaño y desfase. Spotify también tiene estilo píldora con la línea que suena. Se sincronizan al instante con la posición real de Spotify que da Windows (aunque la canción ya vaya por la mitad o la adelantes), y cada línea aparece en un punto distinto del cielo a tu alrededor. Las letras se descargan de LRCLIB |
| Apariencia del launcher | Animación de inicio por el universo (se salta con clic, Esc, Espacio o Enter), 8 temas de color o uno personalizado, y fondo de universo, color, imagen (también GIF) o video MP4 (H.264) con o sin sonido, más música de fondo (MP3/WAV/M4A; los .aac sueltos no funcionan). El selector de archivos es el de Windows y se abre en tus carpetas de Videos, Música o Imágenes. Todo se pausa mientras juegas |
| Freelook | Cámara libre en tercera persona; modo mantener o alternar e invertir el eje vertical |
| Rendimiento | Recolector ZGC generacional (pausas de menos de 1 ms; G1 opcional en Ajustes), memoria de Java fija, texto del HUD en caché, arte del menú generado en segundo plano y el pack con Dynamic FPS y MoreCulling. El launcher oculto no gasta CPU y Spotify solo lee un título de ventana cada 1,5 s (5 s con el juego en segundo plano; nada si el módulo está apagado) |
| Editor de HUD | Arrastrar para mover, rueda para escalar y clic derecho para abrir las opciones del módulo |
| Zoom | Transición suave, cámara cinemática opcional y nivel ajustable |
| Launcher | Iconos reales de los mods de Modrinth (WebP incluido), gestor de mods con dependencias, actualizaciones y pack de rendimiento |

## Limitaciones actuales

- **Inicio de sesión Microsoft (premium)**: funciona sin configurar nada. En **Cuentas → Iniciar sesión con Microsoft** se abre la página oficial de Microsoft dentro de Órbita: pones tu correo y tu contraseña y listo (como en el launcher oficial). Usa el login oficial de Microsoft, así que no hace falta app de Azure; el *Client ID* de **Ajustes → Cuenta Microsoft** es opcional.
- **Cosméticos**: las capas solo las ves tú. Para que otros jugadores las vean hace falta un servidor de cosméticos propio. Alas y emotes: pronto.

## Compilar

```powershell
.\build.ps1 -JavaHome <ruta JDK 21> -GradleJavaHome <ruta JDK 25>
```

Loom 1.18 (Minecraft 1.21.11) necesita Java 25 para ejecutar Gradle. El mod se compila para Java 21 y `Orbita.exe` incluye un runtime Java 21.

Modo sin interfaz (para pruebas o scripts):

```powershell
java -jar orbita-launcher-1.1.0.jar --headless --install-only
java -jar orbita-launcher-1.1.0.jar --headless --username Steve
```

Con `-Dorbita.devshots=true` en *Argumentos JVM extra*, el mod recorre sus pantallas, crea un mundo de prueba y guarda capturas en `screenshots/orbita-*.png` (solo para desarrollo).

---

Órbita no está afiliado a Mojang Studios ni a Microsoft. Minecraft es una marca de Mojang Synergies AB. La fuente Raleway se distribuye bajo la SIL Open Font License (`assets/orbita/font/ofl_license.txt`).
