package dev.orbita.client.nova.video;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

/** GIF animado con ImageIO (viene con Java, no necesita librerias). */
final class GifSource implements FrameSource {
    private final File file;
    private ImageReader reader;
    private ImageInputStream in;
    private BufferedImage canvas, backup;
    private int index, count, delay = 80, lastDisposal;
    private int lastX, lastY, lastW, lastH;

    GifSource(File f) throws Exception { file = f; open(); }

    private void open() throws Exception {
        close();
        in = ImageIO.createImageInputStream(file);
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
        reader = it.next();
        reader.setInput(in, false);
        count = reader.getNumImages(true);
        index = 0; canvas = null; backup = null; lastDisposal = 0;
    }

    @Override public BufferedImage next() throws Exception {
        if (index >= count) return null;
        BufferedImage frame = reader.read(index);
        IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");
        int x = 0, y = 0, disposal = 0, dl = 8;
        IIOMetadataNode desc = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
        if (desc != null) { x = Integer.parseInt(desc.getAttribute("imageLeftPosition")); y = Integer.parseInt(desc.getAttribute("imageTopPosition")); }
        IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
        if (gce != null) {
            dl = Integer.parseInt(gce.getAttribute("delayTime"));
            String d = gce.getAttribute("disposalMethod");
            disposal = d.startsWith("restoreToBackground") ? 2 : d.startsWith("restoreToPrevious") ? 3 : 0;
        }
        delay = Math.max(20, dl * 10);
        if (canvas == null) {
            int w = frame.getWidth() + x, h = frame.getHeight() + y;
            try { w = Math.max(w, Integer.parseInt(((IIOMetadataNode) reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0")
                    .getFirstChild()).getAttribute("logicalScreenWidth"))); } catch (Exception ignored) { }
            canvas = new BufferedImage(Math.max(w, 1), Math.max(h, 1), BufferedImage.TYPE_INT_ARGB);
        }
        // elimina el fotograma anterior segun su metodo de descarte
        Graphics2D g = canvas.createGraphics();
        if (lastDisposal == 2) { g.setComposite(java.awt.AlphaComposite.Clear); g.fillRect(lastX, lastY, lastW, lastH); g.setComposite(java.awt.AlphaComposite.SrcOver); }
        else if (lastDisposal == 3 && backup != null) { g.setComposite(java.awt.AlphaComposite.Src); g.drawImage(backup, 0, 0, null); g.setComposite(java.awt.AlphaComposite.SrcOver); }
        if (disposal == 3) { backup = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB); Graphics2D bg = backup.createGraphics(); bg.drawImage(canvas, 0, 0, null); bg.dispose(); }
        g.drawImage(frame, x, y, null);
        g.dispose();
        lastDisposal = disposal; lastX = x; lastY = y; lastW = frame.getWidth(); lastH = frame.getHeight();
        index++;
        BufferedImage out = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D og = out.createGraphics(); og.drawImage(canvas, 0, 0, null); og.dispose();
        return out;
    }

    @Override public int delayMs() { return delay; }
    @Override public void restart() throws Exception { open(); }

    @Override public void close() {
        try { if (reader != null) reader.dispose(); } catch (Exception ignored) { }
        try { if (in != null) in.close(); } catch (Exception ignored) { }
        reader = null; in = null;
    }
}
