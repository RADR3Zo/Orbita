# Compila Órbita y genera dist\Orbita\Orbita.exe (con Java 21 incluido) + un .zip para distribuir.
# Uso:  .\build.ps1 -JavaHome <JDK 21> -GradleJavaHome <JDK 25> [-GithubRepo usuario/repo]
#   - Gradle/Loom 1.18 (Minecraft 1.21.11) necesita Java 25 para ejecutarse.
#   - El runtime que se incluye en Orbita.exe es Java 21 (el que pide Minecraft 1.21.11).
#   - -GithubRepo: repositorio del que el launcher busca actualizaciones (si no se pasa, el de gradle.properties).
#     GitHub Actions lo pasa solo (.github\workflows\release.yml).
param([string]$JavaHome = $env:JAVA_HOME, [string]$GradleJavaHome = $env:JAVA_HOME, [string]$GithubRepo = '')

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not $JavaHome) { $JavaHome = Split-Path (Split-Path (Get-Command javac).Source) }
$env:JAVA_HOME = $GradleJavaHome
$bin = Join-Path $JavaHome 'bin'
$version = (Select-String -Path gradle.properties -Pattern '^orbita_version=(.+)$').Matches[0].Groups[1].Value

Write-Host "==> Compilando mod cliente y launcher (Órbita $version)" -ForegroundColor Magenta
$gradleArgs = @(':launcher:test', ':launcher:fatJar', '--console=plain')
if ($GithubRepo) { $gradleArgs += "-Pgithub_repo=$GithubRepo" }
& .\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) { throw 'La compilación o las pruebas fallaron' }

$build = Join-Path $PSScriptRoot 'build\package'
$dist = Join-Path $PSScriptRoot 'dist'
Remove-Item -Recurse -Force $build, $dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$build\input", $dist | Out-Null
Copy-Item "launcher\build\dist\orbita-launcher-$version.jar" "$build\input\"

# Runtime mínimo con los módulos que necesitan el launcher y Minecraft + Fabric (incl. Sodium/Iris).
$modules = 'java.se,jdk.unsupported,jdk.unsupported.desktop,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.zipfs,jdk.management,jdk.management.agent,jdk.naming.dns,jdk.localedata,jdk.charsets,jdk.httpserver,jdk.net,jdk.jfr,jdk.accessibility,jdk.attach,jdk.jdi,jdk.jsobject,jdk.xml.dom'
Write-Host '==> Creando runtime Java con jlink' -ForegroundColor Magenta
& "$bin\jlink.exe" --add-modules $modules --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output "$build\runtime"
if ($LASTEXITCODE -ne 0) { throw 'jlink falló' }

Write-Host '==> Empaquetando Orbita.exe con jpackage' -ForegroundColor Magenta
& "$bin\jpackage.exe" --type app-image --name Orbita --app-version $version `
    --vendor 'Órbita' --description 'Launcher y cliente de Minecraft Órbita' `
    --input "$build\input" --main-jar "orbita-launcher-$version.jar" `
    --main-class dev.orbita.launcher.OrbitaLauncher `
    --runtime-image "$build\runtime" --dest $dist `
    --java-options '-Dsun.java2d.d3d=true' --java-options '-Dfile.encoding=UTF-8' `
    --java-options '-Xmx256m' --java-options '-XX:+UseSerialGC' --java-options '-XX:TieredStopAtLevel=1' `
    --java-options '-XX:CICompilerCount=2' --java-options '-Xss512k'
if ($LASTEXITCODE -ne 0) { throw 'jpackage falló' }

$zip = Join-Path $dist "Orbita-$version-windows-x64.zip"
Compress-Archive -Path "$dist\Orbita" -DestinationPath $zip -Force
# Archivos para publicar una actualización en GitHub (el launcher descarga el .jar y lo comprueba con el .sha256)
$jarName = "orbita-launcher-$version.jar"
Copy-Item "launcher\build\dist\$jarName" $dist
$hash = (Get-FileHash "$dist\$jarName" -Algorithm SHA256).Hash.ToLower()
Set-Content -Path "$dist\$jarName.sha256" -Value "$hash  $jarName" -NoNewline -Encoding ascii

Write-Host "==> Listo: $dist\Orbita\Orbita.exe" -ForegroundColor Green
Write-Host "    Zip:   $zip" -ForegroundColor Green
Write-Host "    Jar:   $dist\$jarName (+ .sha256)" -ForegroundColor Green
