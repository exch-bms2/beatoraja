package bms.player.beatoraja.skin;

import java.util.ArrayList;
import java.util.List;

/** Headless regression test for per-frame custom timer/event updates (#540). */
public final class SkinCustomObjectsTest {
    public static void main(String[] args) {
        updatesInIdOrder();
        timersAddedDuringUpdateDoNotBreakIteration();
        System.out.println("SkinCustomObjectsTest passed");
    }

    private static void updatesInIdOrder() {
        var skin = newSkin();
        List<Integer> order = new ArrayList<>();
        // IntMap iteration order differs from ID order for these keys.
        for (int id : new int[] {5000, 0, 17, 1, 4096, 33, 2}) {
            skin.addCustomTimer(new CustomTimer(id, state -> { order.add(id); return id; }));
        }
        skin.updateCustomObjects(null);
        check(order.equals(List.of(0, 1, 2, 17, 33, 4096, 5000)), "Timers update in ascending ID order: " + order);
        order.clear();
        skin.updateCustomObjects(null);
        check(order.size() == 7, "Every timer updates once per frame");
    }

    private static void timersAddedDuringUpdateDoNotBreakIteration() {
        var skin = newSkin();
        int[] calls = {0};
        // A timer function that registers many passive timers, forcing the IntMap to grow mid-frame.
        skin.addCustomTimer(new CustomTimer(10, state -> {
            int frame = calls[0]++;
            for (int n = 0; n < 200; n++) skin.setMicroCustomTimer(10_000 + frame * 200 + n, n);
            return frame;
        }));
        for (int id = 1; id < 40; id++) {
            skin.addCustomTimer(new CustomTimer(id * 37, state -> 0L));
        }
        for (int frame = 0; frame < 5; frame++) skin.updateCustomObjects(null);
        check(calls[0] == 5, "Function timer runs exactly once per frame");
        check(skin.getMicroCustomTimer(10) == 4, "Function timer keeps its latest value");
        check(skin.getMicroCustomTimer(10_000 + 4 * 200 + 199) == 199, "Timers added mid-frame are kept");
    }

    private static Skin newSkin() {
        var header = new SkinHeader();
        header.setSourceResolution(bms.player.beatoraja.Resolution.HD);
        header.setDestinationResolution(bms.player.beatoraja.Resolution.HD);
        return new Skin(header);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
