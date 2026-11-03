package bms.player.beatoraja.input;

import bms.player.beatoraja.GlfwWindowSupport;
import org.lwjgl.glfw.GLFW;

/** Backend policy/math tests only; never claim a compositor or physical device was tested. */
public final class WaylandCompatibilityTest {
    public static void main(String[] args) {
        check(!GlfwWindowSupport.hasWindowPosition(GLFW.GLFW_PLATFORM_WAYLAND),"Actual Wayland backend has no global position");
        for (int platform : new int[]{GLFW.GLFW_PLATFORM_X11,GLFW.GLFW_PLATFORM_COCOA,GLFW.GLFW_PLATFORM_WIN32})
            check(GlfwWindowSupport.hasWindowPosition(platform),"XWayland/X11 and desktop backends keep legacy position support");
        var p = new MouseScratchPosition();
        p.sample(true,false,7,9,640,480);
        check(!p.delivered && p.dx==0 && p.recenter,"Initial centered sample only primes");
        p.sample(true,true,332,237,640,480);
        check(p.delivered && p.dx==12 && p.dy==-3 && p.recenter,"Captured Wayland uses exact legacy logical deltas");
        p.sample(false,true,100,90,640,480);
        check(!p.delivered && !p.recenter,"Unlock does not turn absolute cursor position into motion");
        p.sample(false,true,112,87,640,480);
        check(p.delivered && p.dx==12 && p.dy==-3 && !p.recenter,"Uncaptured Wayland uses bounded endpoint deltas, never warp");
        p.sample(false,true,112,87,640,480);
        check(p.dx==0 && p.dy==0,"Stationary uncaptured cursor cannot generate repeated false scratches");
        p.sample(true,true,900,700,640,480);
        check(!p.delivered && p.recenter,"Lock transition re-primes without a jump");
        p.sample(true,true,308,243,640,480);
        check(p.dx==-12 && p.dy==3,"First stable reverse sample retains units");
        var queue = new MouseMotionQueue(); queue.add(6,2);
        var geometry = new MouseScratchGeometry();
        for (float scale : new float[]{1,1.25f,1.5f,2}) {
            boolean deliver = geometry.sample(1,0,640,480,(int)(640*scale),(int)(480*scale),0,0,scale,scale,true,false);
            p.sample(true,deliver,320,240,640,480);
            check(!p.delivered,"DPI transition primes without multiplying sensitivity");
            deliver = geometry.sample(1,0,640,480,(int)(640*scale),(int)(480*scale),0,0,scale,scale,true,false);
            p.sample(true,deliver,332,237,640,480);
            check(p.dx==12 && p.dy==-3,"Same logical movement at every buffer/content scale");
            queue.add(p.dx,p.dy);
        }
        var batch = new MouseMotionQueue.Batch(); queue.drain(batch);
        check(batch.size==5 && batch.dx[0]==6 && batch.dy[0]==2,"Geometry changes preserve pending FIFO");
        for (int i=1;i<batch.size;i++) check(batch.dx[i]==12 && batch.dy[i]==-3,"FIFO has unchanged deltas in order");
        p.invalidate(); p.sample(false,true,300,200,640,480);
        check(!p.delivered,"Focus/reset requires a new uncaptured baseline");
        diagnostics();
        System.out.println("WaylandCompatibilityTest passed (policy, capture/absolute endpoints, HiDPI, pending FIFO; NO native Wayland)");
    }
    private static void diagnostics() {
        var d = new MouseScratchDiagnostics();
        d.sample(true,true,100,90,640,480,640,480,1280,960,1,false,0,0);
        d.sample(true,true,112,87,640,480,640,480,1280,960,2,true,12,-3);
        d.consumed(12,-3,3);
        var s = d.snapshot();
        check(s.counts().get(MouseScratchDiagnostics.Counter.CONSUME_MISMATCHES)==0,"Diagnostics compare delivered endpoint delta, not distance from center");
        check(s.counts().get(MouseScratchDiagnostics.Counter.WARP_REQUESTS)==0,"Uncaptured observations do not fabricate warp requests");
    }
    private static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
}
