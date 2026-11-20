package dev.orbita.launcher.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hashing {
    private Hashing() {
    }

    public static String hash(Path file, String algorithm) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return hash(in, algorithm);
        }
    }

    public static String hash(InputStream in, String algorithm) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = in.read(buf)) > 0) {
            md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    public static String sha1(Path file) throws IOException {
        return hash(file, "SHA-1");
    }

    public static String sha512(Path file) throws IOException {
        return hash(file, "SHA-512");
    }
}
