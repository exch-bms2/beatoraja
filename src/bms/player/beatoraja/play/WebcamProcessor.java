package bms.player.beatoraja.play;

import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.StretchType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.github.sarxos.webcam.Webcam;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

public class WebcamProcessor {
    private Webcam webcam;
    private Pixmap pixmap;
    private ByteBuffer pixels;
    private Texture texture;
    private TextureRegion textureRegion;
    private WebcamImageUpdater updateThread;
    private Rectangle tempRect;

    public void start(int deviceIndex, int resolutionIndex) {
        List<Webcam> cams = Webcam.getWebcams();
        if (cams.isEmpty() || deviceIndex >= cams.size()) {
            Logger.getGlobal().warning("invalid webcam setting - device no longer connected?");
            return;
        }

        webcam = cams.get(deviceIndex);

        Dimension[] sizes = webcam.getViewSizes();
        if (resolutionIndex >= sizes.length) {
            Logger.getGlobal().warning("invalid webcam resolution - defaulting to first");
            resolutionIndex = 0;
        }

        Dimension size = sizes[resolutionIndex];
        webcam.setViewSize(size);
        webcam.open();
        webcam.getLock().disable();

        pixmap = new Pixmap(size.width, size.height, Pixmap.Format.RGB888);
        pixels = pixmap.getPixels();
        texture = new Texture(pixmap);
        textureRegion = new TextureRegion(texture);
        tempRect = new Rectangle();

        updateThread = new WebcamImageUpdater();
        updateThread.start();
    }

    public void render(Skin.SkinObjectRenderer renderer, Rectangle destRect, StretchType stretchType) {
        tempRect.set(destRect);
        stretchType.stretchRect(tempRect, textureRegion, textureRegion);
        renderer.draw(textureRegion, tempRect.x, tempRect.y, tempRect.width, tempRect.height);
    }

    public void stop() {
        if (updateThread != null) {
            updateThread.stop();
        }
    }

    private class WebcamImageUpdater implements Runnable {
        private boolean running;

        public void start() {
            Thread thread = new Thread(this);
            thread.setName("beatoraja camera thread");
            thread.start();
        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            running = true;

            while (running) {
                webcam.getImageBytes(pixels);
                pixels.rewind();

                Gdx.app.postRunnable(() -> texture.draw(pixmap, 0, 0));

                try {
                    //noinspection BusyWait
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            webcam.close();
        }
    }
}
