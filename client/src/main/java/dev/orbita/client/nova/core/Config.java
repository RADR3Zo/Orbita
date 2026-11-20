package dev.orbita.client.nova.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Guarda/lee los ajustes en un archivo .properties simple. */
public final class Config {
    private final Path file;
    private final Properties props = new Properties();

    public Config(Path file) {
        this.file = file;
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) { props.load(r); } catch (IOException ignored) { }
        }
    }

    public String get(String key) { return props.getProperty(key); }
    public void put(String key, String value) { props.setProperty(key, value); }

    public void write() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) { props.store(w, "Nova Client"); }
        } catch (IOException ignored) { }
    }
}
