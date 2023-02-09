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

public class WebcamProcessor {
    private Pixmap pixmap;
    private ByteBuffer pixels;
    private Texture texture;
    private TextureRegion textureRegion;
    private WebcamImageUpdater updateThread;
    private Rectangle tempRect;

    public void start() {
        pixmap = new Pixmap(640, 480, Pixmap.Format.RGB888);
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
        renderer.draw(textureRegion, tempRect.x + tempRect.width, tempRect.y, -tempRect.width, tempRect.height);
    }

    public void stop() {
        updateThread.stop();
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
            Webcam webcam = Webcam.getDefault();
            webcam.setViewSize(new Dimension(640, 480));
            webcam.open();
            webcam.getLock().disable();

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
