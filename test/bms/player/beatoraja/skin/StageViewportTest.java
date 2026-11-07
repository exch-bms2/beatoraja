package bms.player.beatoraja.skin;

import bms.player.beatoraja.input.mouse.MouseCoordinates;

import java.lang.reflect.Proxy;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.viewport.StretchViewport;

/** Logical skin/Stage inverse mapping and physical GL viewport, without a window. */
public final class StageViewportTest {
    public static void main(String[] args) {
        GdxNativesLoader.load();
        var oldGraphics = Gdx.graphics; var oldGl = Gdx.gl; var oldGl20 = Gdx.gl20;
        int[] dimensions = new int[4], viewport = new int[4];
        Gdx.graphics = (Graphics) Proxy.newProxyInstance(StageViewportTest.class.getClassLoader(), new Class<?>[] {Graphics.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getWidth" -> dimensions[0]; case "getHeight" -> dimensions[1];
                    case "getBackBufferWidth" -> dimensions[2]; case "getBackBufferHeight" -> dimensions[3];
                    default -> throw new AssertionError(m.getName());
                });
        Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance(StageViewportTest.class.getClassLoader(), new Class<?>[] {GL20.class},
                (p, m, a) -> { if (m.getName().equals("glViewport")) for (int i = 0; i < 4; i++) viewport[i] = (int)a[i]; return null; });
        try {
            var stage = new StretchViewport(1280, 720);
            for (int[] size : new int[][] {{480,270}, {400,300}, {1440,900}, {1920,1080}})
                for (double dpi : new double[] {1, 1.25, 1.5, 2}) {
                    dimensions[0] = size[0]; dimensions[1] = size[1];
                    dimensions[2] = (int)(size[0] * dpi); dimensions[3] = (int)(size[1] * dpi);
                    stage.update(size[0], size[1], true);
                    check(viewport[0] == 0 && viewport[1] == 0 && viewport[2] == dimensions[2] && viewport[3] == dimensions[3], "Physical viewport fills backbuffer once");
                    for (float fraction : new float[] {0, .25f, .5f, .75f, 1}) {
                        int x = Math.round(size[0] * fraction), y = Math.round(size[1] * fraction);
                        var point = stage.unproject(new Vector2(x, y));
                        check(Math.abs(point.x - MouseCoordinates.skinX(x, 1280, size[0])) <= 1.1f, "Stage/skin click X agreement");
                        check(Math.abs(point.y - MouseCoordinates.skinY(y, 720, size[1])) <= 1.1f, "Stage/skin click Y agreement");
                    }
                }
            System.out.println("StageViewportTest passed (logical coordinates, nonuniform resizing, fractional/2x DPI)");
        } finally { Gdx.graphics = oldGraphics; Gdx.gl = oldGl; Gdx.gl20 = oldGl20; }
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
