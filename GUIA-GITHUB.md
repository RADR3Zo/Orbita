# Guía: subir Órbita a GitHub y publicar actualizaciones

Esta guía se sigue **una sola vez** para dejar todo listo (pasos 1 a 5).
Después, publicar una versión nueva son solo 3 comandos (paso 6).

---

## 1. Instala lo necesario

1. **Git para Windows**: https://git-scm.com/download/win (deja las opciones por defecto).
2. **Cuenta de GitHub**: https://github.com/signup si aún no tienes.
3. Abre **PowerShell** y dile a Git quién eres (usa el mismo correo de tu cuenta de GitHub):

   ```powershell
   git config --global user.name "Jhon Henry Mendez Arroyo"
   git config --global user.email "TU_CORREO_DE_GITHUB"
   ```

## 2. Crea el repositorio en GitHub

1. Entra a https://github.com/new
2. **Repository name**: `orbita`
3. Elige **Public**. Con un repo privado, los launchers de otras PCs no pueden ver las releases.
4. **No** marques "Add a README", ".gitignore" ni "license": ya vienen en el proyecto.
5. Pulsa **Create repository**.

## 3. Pon tu usuario en el proyecto

Abre `gradle.properties` y cambia esta línea por tu usuario de GitHub:

```properties
github_repo=RADR3Zo/Orbita
```

Por ejemplo, si tu usuario es `jhonmendez`, queda `github_repo=jhonmendez/orbita`.

> Las releases que crea GitHub Actions ya ponen el repositorio correcto por su cuenta.
> Esta línea sirve para cuando compilas en tu PC con `build.ps1`.

## 4. Sube el código

En PowerShell, dentro de la carpeta del proyecto (donde está `build.ps1`):

```powershell
git add -A
git commit -m "Configura mi repositorio de GitHub"
git remote add origin https://github.com/RADR3Zo/Orbita.git
git push -u origin main
```

La primera vez se abre una ventana para iniciar sesión en GitHub. Inicia sesión y listo.

Abre la pestaña **Actions** de tu repositorio. Vas a ver el flujo **Pruebas** compilando.
Cuando sale ✅ significa que el proyecto compila en una PC limpia y que todas las pruebas pasan.

## 5. Publica la primera versión con actualizaciones (1.6.0)

```powershell
git tag v1.6.0
git push origin v1.6.0
```

En **Actions** se ejecuta **Publicar release**, que tarda unos 5-10 minutos. Al terminar, en **Releases** aparece
"Órbita 1.6.0" con tres archivos:

| Archivo | Para qué |
|---|---|
| `Orbita-1.6.0-windows-x64.zip` | Instalación completa, para gente nueva |
| `orbita-launcher-1.6.0.jar` | Lo que descarga el actualizador |
| `orbita-launcher-1.6.0.jar.sha256` | Huella para comprobar que la descarga no se corrompió |

**Importante:** las PCs que todavía tienen la 1.5.4 no traen el actualizador, así que **esta vez** hay que
pasarles el `.zip` de la 1.6.0 a mano. A partir de ahí ya se actualizan solas.

> **Otra forma de publicar:** en vez de la etiqueta, sube a la rama `release` con `git push origin main:release`.
> GitHub crea la etiqueta `vX.Y.Z` solo, con la versión de `gradle.properties`.

## 6. Publicar una actualización (cada vez que quieras)

1. Haz tus cambios y pruébalos.
2. Sube la versión en `gradle.properties`, por ejemplo `orbita_version=1.6.1`.
3. Escribe las novedades en `novedades/1.6.1.md` (es lo que verán los jugadores al actualizar).
4. Ejecuta:

   ```powershell
   git add -A
   git commit -m "Órbita 1.6.1: lo que cambiaste"
   git push
   git tag v1.6.1
   git push origin v1.6.1
   ```

Cuando termine **Publicar release**, cualquier PC con Órbita verá al abrirlo:
*"Hay una versión nueva de Órbita: 1.6.1"*. Con **Actualizar ahora** se descarga, se instala y se reinicia sola.

### Escribir las novedades

Las novedades que ve el usuario salen de `novedades/<versión>.md`. También puedes editarlas después en
**Releases → (tu versión) → Edit**.

### Cuándo hace falta el .zip completo

El actualizador solo cambia el `.jar`. Si cambias algo de fuera del jar, como la versión de Java incluida o los
módulos de `jlink` en `build.ps1`, escribe `[completa]` en las notas de la release. El launcher pedirá descargar
el `.zip` en vez de actualizarse solo.

## Si algo sale mal

| Problema | Solución |
|---|---|
| ❌ "La etiqueta es v1.6.1 pero gradle.properties dice 1.6.0" | Olvidaste subir `orbita_version`. Bórrala con `git tag -d v1.6.1` y `git push origin :refs/tags/v1.6.1`, corrige la versión y repite |
| ❌ en **Pruebas** | Abre el error en Actions: dice qué prueba falló o qué archivo no compila |
| El launcher no avisa | El repo tiene que ser **público** y la release no puede ser borrador. Revisa también `github_repo` |
| "No se pudo actualizar" | Órbita no puede escribir en su carpeta (por ejemplo, si está en Archivos de programa). Muévelo a otra carpeta o descarga el .zip |

## Comandos de Git que más vas a usar

```powershell
git status                 # qué archivos cambiaste
git diff                   # qué cambiaste exactamente
git add -A                 # preparar todos los cambios
git commit -m "mensaje"    # guardar un punto en el historial
git push                   # subirlo a GitHub
git log --oneline          # ver el historial
git restore Archivo.java   # deshacer cambios de un archivo (todavía sin commit)
```
