package bms.player.beatoraja.external;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/** Windows/Linux fallback only. Never initialize AWT in the macOS GLFW JVM. */
final class AwtDesktopActions {
    private AwtDesktopActions() {}

    static void openPath(Path path) throws IOException {
        Desktop.getDesktop().open(path.toFile());
    }

    static void openUri(URI uri) throws IOException {
        Desktop.getDesktop().browse(uri);
    }

    static void copyImage(Path path) throws IOException {
        BufferedImage source = ImageIO.read(path.toFile());
        if (source == null) throw new IOException("Unsupported image: " + path);
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        var graphics = image.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
            source.flush();
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
            @Override public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] {DataFlavor.imageFlavor};
            }
            @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }
            @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                return image;
            }
        }, null);
    }
}
