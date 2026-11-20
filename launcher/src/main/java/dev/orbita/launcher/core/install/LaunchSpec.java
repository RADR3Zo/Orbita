package dev.orbita.launcher.core.install;

import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.List;

/** Resultado de la instalación: todo lo necesario para construir la línea de comandos. */
public record LaunchSpec(String versionId, String mainClass, List<Path> classpath,
                         JsonObject vanilla, JsonObject fabric, String assetIndex) {
}
