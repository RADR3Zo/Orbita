package dev.orbita.client.nova.video;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;

/** MP4/MOV (H.264) con JCodec, incluido dentro del mod. Solo video, sin sonido. */
final class Mp4Source implements FrameSource {
    private final File file;
    private SeekableByteChannel channel;
    private FrameGrab grab;
    private int delay = 40;

    Mp4Source(File f) throws Exception { file = f; open(); }

    private void open() throws Exception {
        close();
        channel = NIOUtils.readableChannel(file);
        grab = FrameGrab.createFrameGrab(channel);
        var meta = grab.getVideoTrack().getMeta();
        double dur = meta.getTotalDuration();
        int frames = meta.getTotalFrames();
        if (dur > 0 && frames > 0) delay = (int) Math.max(10, Math.round(dur * 1000.0 / frames));
    }

    @Override public BufferedImage next() throws Exception {
        Picture p = grab.getNativeFrame();
        return p == null ? null : AWTUtil.toBufferedImage(p);
    }

    @Override public int delayMs() { return delay; }
    @Override public void restart() throws Exception { open(); }

    @Override public void close() {
        if (channel != null) NIOUtils.closeQuietly(channel);
        channel = null; grab = null;
    }
}
