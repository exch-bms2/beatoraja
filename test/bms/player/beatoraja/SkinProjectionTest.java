package bms.player.beatoraja;

import bms.player.beatoraja.input.mouse.MouseCoordinates;

import bms.player.beatoraja.input.WindowDiagnostics;
import com.badlogic.gdx.math.Matrix4;

/** No window or GL context: verify the actual production projection against skin/input coordinates. */
public final class SkinProjectionTest {
    public static void main(String[] args) {
        for (Resolution resolution : Resolution.values()) {
            Matrix4 projection = MainController.skinProjection(resolution.width, resolution.height);
            float[] m = projection.val;
            check(WindowDiagnostics.matchesSkinProjection(m[Matrix4.M00], m[Matrix4.M11],
                    resolution.width, resolution.height), "Configured skin projection " + resolution);
            for (int[] window : new int[][] {{resolution.width, resolution.height},
                    {320, 240}, {480, 360}, {1280, 720}, {1440, 900}, {2560, 1440}}) {
                for (double dpi : new double[] {1, 1.25, 1.5, 2}) {
                    for (double fraction : new double[] {0, 0.25, 0.5, 0.75, 1}) {
                        double skinX = resolution.width * fraction, skinY = resolution.height * fraction;
                        double logicalX = ((m[Matrix4.M00] * skinX + m[Matrix4.M03]) + 1) * window[0] / 2;
                        double logicalY = ((m[Matrix4.M11] * skinY + m[Matrix4.M13]) + 1) * window[1] / 2;
                        near(logicalX, window[0] * fraction, 0.001, "Draw X fills logical viewport");
                        near(logicalY, window[1] * fraction, 0.001, "Draw Y fills logical viewport");
                        near(logicalX * dpi, window[0] * dpi * fraction, 0.002, "Framebuffer X");
                        near(logicalY * dpi, window[1] * dpi * fraction, 0.002, "Framebuffer Y");
                        double toleranceX = Math.ceil((double) resolution.width / window[0]) + 1;
                        double toleranceY = Math.ceil((double) resolution.height / window[1]) + 1;
                        near(MouseCoordinates.skinX((int) Math.round(logicalX), resolution.width, window[0]),
                                skinX, toleranceX, "Click inverse X");
                        near(MouseCoordinates.skinY((int) Math.round(window[1] - logicalY), resolution.height, window[1]),
                                skinY, toleranceY, "Click inverse Y");
                    }
                }
            }
        }
        float[] p = MainController.skinProjection(1280, 720).val;
        check(p[Matrix4.M00] != 2f / 640 && p[Matrix4.M11] != 2f / 360,
                "Do not derive projection from a smaller initial logical window");
        System.out.println("SkinProjectionTest passed (all configured resolutions, mismatched windows and DPI scales)");
    }

    private static void near(double value, double expected, double tolerance, String message) {
        check(Math.abs(value - expected) <= tolerance, message + ": " + value + " != " + expected);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
