package dev.orbita.client.nova.video;

import java.awt.image.BufferedImage;

/** Origen de fotogramas (GIF o MP4). No depende de Minecraft. */
interface FrameSource extends AutoCloseable {
    /** Siguiente fotograma, o null al llegar al final. */
    BufferedImage next() throws Exception;
    /** Duracion del ultimo fotograma devuelto, en ms. */
    int delayMs();
    /** Vuelve al principio (para repetir en bucle). */
    void restart() throws Exception;
    @Override void close();
}
