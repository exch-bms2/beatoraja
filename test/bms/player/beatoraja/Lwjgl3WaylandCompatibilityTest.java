package bms.player.beatoraja;

import bms.player.beatoraja.input.MouseMotionQueue;
import bms.player.beatoraja.input.PollingLwjgl3Input;
import bms.player.beatoraja.input.WaylandInputTestSupport;
import bms.player.beatoraja.input.WindowDiagnostics;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

/** Synthetic cursor samples on real GL; native mode must really use Wayland, never XWayland. */
public final class Lwjgl3WaylandCompatibilityTest {
    public static void main(String[] args) {
        boolean nativeWayland = Boolean.getBoolean("beatoraja.test.nativeWayland");
        if (nativeWayland && !System.getProperty("os.name","").startsWith("Linux")) {
            System.out.println("SKIP native Wayland: requires Linux and a compositor"); return;
        }
        if (nativeWayland) GLFW.glfwInitHint(GLFW.GLFW_PLATFORM,GLFW.GLFW_PLATFORM_WAYLAND);
        var cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("beatoraja Wayland compatibility smoke"); cfg.setWindowedMode(320,240);
        cfg.setHdpiMode(HdpiMode.Logical); cfg.disableAudio(true); cfg.setForegroundFPS(60);
        new Lwjgl3Application(new ApplicationAdapter() {
            int frames, phase, since;
            boolean checking, captureSupported;
            final MouseMotionQueue.Batch batch = new MouseMotionQueue.Batch();
            final int[] featureErrors = {0};
            GLFWErrorCallback observer, previous;
            WindowModeController modes;
            int savedWidth, savedHeight;

            @Override public void create() {
                if (nativeWayland) check(!GlfwWindowSupport.hasWindowPosition(),"Native task must not silently run on X11/XWayland");
                observer = GLFWErrorCallback.create((error,message) -> {
                    if (error==GLFW.GLFW_FEATURE_UNAVAILABLE || error==GLFW.GLFW_FEATURE_UNIMPLEMENTED) featureErrors[0]++;
                    if (previous!=null) previous.invoke(error,message);
                });
                previous = GLFW.glfwSetErrorCallback(observer);
            }
            @Override public void render() {
                if (checking) return;
                checking = true;
                try {
                    frames++;
                    var graphics=(Lwjgl3Graphics)Gdx.graphics;
                    var input=(PollingLwjgl3Input)Gdx.input;
                    long handle=graphics.getWindow().getWindowHandle();
                    if (!nativeWayland && frames==2) {
                        var focus=GLFW.glfwSetWindowFocusCallback(handle,null); GLFW.glfwSetWindowFocusCallback(handle,focus); focus.invoke(handle,true);
                    }
                    if (frames>600) throw new AssertionError("Focused window/compositor configure timeout, phase="+phase);
                    if (phase==0 && frames>=10 && graphics.getWindow().isFocused()) {
                        input.setMouseScratchEnabled(true); input.setCursorCatched(false);
                        cursor(handle,100,90); input.update(); expect(input,0,0,0,"Initial absolute baseline");
                        cursor(handle,112,87); input.update(); expect(input,1,12,-3,"Absolute endpoint movement, no warp");
                        check(input.getX()==112 && input.getY()==87,"Noncaptured UI coordinates remain actual positions");
                        cursor(handle,112,87); input.update(); expect(input,0,0,0,"Stationary absolute cursor has no repeated motion");
                        check(featureErrors[0]==0,"No unsupported position/warp API during normal-cursor sampling");
                        input.setCursorCatched(true); captureSupported=input.isCursorCatched();
                        if (captureSupported) {
                            cursor(handle,7,9); input.update(); expect(input,0,0,0,"Capture change primes virtual center");
                            cursor(handle,graphics.getWidth()/2+12,graphics.getHeight()/2-3); input.update();
                            expect(input,1,12,-3,"Captured virtual deltas equal legacy logical units");
                        } else System.out.println("SKIP continuous capture: compositor refused pointer locking; bounded fallback only");
                        input.setCursorCatched(false);
                        cursor(handle,200,150); input.update(); expect(input,0,0,0,"Capture release primes absolute origin");
                        cursor(handle,209,150); input.update(); // Genuine pending movement survives resize/rebase.
                        featureErrors[0]=0;
                        savedWidth=graphics.getWidth(); savedHeight=graphics.getHeight();
                        GLFW.glfwSetWindowSize(handle,480,360); phase=1;
                    } else if (phase==1 && (graphics.getWidth()!=savedWidth || graphics.getHeight()!=savedHeight)) {
                        cursor(handle,209,150); input.update();
                        input.consumeMouseScratchDeltas(batch);
                        check(batch.size>=1 && batch.dx[0]==9 && batch.dy[0]==0,"Valid pre-resize FIFO is retained");
                        input.invalidateMouseScratchCenter(); cursor(handle,100,90); input.update(); expect(input,0,0,0,"Resize/boundary does not synthesize movement");
                        cursor(handle,112,87); input.update(); expect(input,1,12,-3,"Logical sensitivity retained after resize");
                        var diagnostics=WindowDiagnostics.createIfEnabled(); if (diagnostics!=null) diagnostics.sample();
                        if (nativeWayland) check(GlfwWindowSupport.refreshRate(graphics)==0,"Unknown windowed output refresh is not fabricated");
                        check(featureErrors[0]==0,"No unsupported API during resize/diagnostics");
                        input.setMouseScratchEnabled(false);
                        if (Boolean.getBoolean("beatoraja.test.waylandFullscreen")) {
                            modes=WindowModeController.create(graphics,Resolution.SD);
                            savedWidth=graphics.getLogicalWidth(); savedHeight=graphics.getLogicalHeight();
                            check(modes.toggle(),"Fullscreen request"); since=frames; phase=2;
                        } else finish(graphics);
                    } else if (phase==2 && frames-since>30) {
                        check(graphics.isFullscreen(),"Fullscreen monitor attached");
                        check(modes.toggle(),"Windowed restore request"); phase=3;
                    } else if (phase==3 && !graphics.isFullscreen() && graphics.getLogicalWidth()==savedWidth && graphics.getLogicalHeight()==savedHeight) {
                        check(featureErrors[0]==0,"Fullscreen round trip avoids unsupported desktop-position APIs");
                        finish(graphics);
                    }
                } finally { checking=false; }
            }
            void expect(PollingLwjgl3Input input,int size,int dx,int dy,String message) {
                input.consumeMouseScratchDeltas(batch);
                check(batch.size==size && (size==0 || batch.dx[0]==dx && batch.dy[0]==dy),message);
            }
            void finish(Lwjgl3Graphics graphics) {
                System.out.println("Wayland compatibility smoke: nativeWayland="+nativeWayland+", GLFWplatform="+GLFW.glfwGetPlatform()
                        +", captureSupported="+captureSupported+", logical="+graphics.getWidth()+"x"+graphics.getHeight()
                        +", framebuffer="+graphics.getBackBufferWidth()+"x"+graphics.getBackBufferHeight());
                Gdx.app.exit(); phase=4;
            }
            @Override public void dispose() {
                if (observer!=null) { GLFW.glfwSetErrorCallback(previous); observer.free(); }
            }
        },cfg) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return WaylandInputTestSupport.input(window); }
        };
        System.out.println("Lwjgl3WaylandCompatibilityTest passed (nativeWayland="+nativeWayland+"; synthetic movement, NOT physical sensitivity/latency)");
    }
    private static void cursor(long handle,double x,double y) {
        var callback=GLFW.glfwSetCursorPosCallback(handle,null); GLFW.glfwSetCursorPosCallback(handle,callback); callback.invoke(handle,x,y);
    }
    private static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
}
