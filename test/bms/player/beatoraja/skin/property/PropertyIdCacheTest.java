package bms.player.beatoraja.skin.property;

import java.security.MessageDigest;

/** Cold/warm and lookup-order equivalence for all IDs; prints a mask for cross-version comparison. */
public final class PropertyIdCacheTest {
    public static void main(String[] args) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int id = 0; id < 65536; id++) {
            IntegerProperty number = IntegerPropertyFactory.getIntegerProperty(id);
            IntegerProperty image = IntegerPropertyFactory.getImageIndexProperty(id);
            // Negative first exercises the shared miss cache independently of the positive cache.
            BooleanProperty negative = BooleanPropertyFactory.getBooleanProperty(-id);
            BooleanProperty positive = BooleanPropertyFactory.getBooleanProperty(id);
            check(number == IntegerPropertyFactory.getIntegerProperty(id), "number " + id);
            check(image == IntegerPropertyFactory.getImageIndexProperty(id), "image " + id);
            check(positive == BooleanPropertyFactory.getBooleanProperty(id), "positive " + id);
            check(negative == BooleanPropertyFactory.getBooleanProperty(-id), "negative " + id);
            check((positive == null) == (negative == null), "negation presence " + id);
            digest.update((byte)((number != null ? 1 : 0) | (image != null ? 2 : 0)
                    | (positive != null ? 4 : 0) | (negative != null ? 8 : 0)));
        }
        for (int id : new int[]{-1, -65536, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            check(IntegerPropertyFactory.getIntegerProperty(id) == null, "invalid number ID");
            check(IntegerPropertyFactory.getImageIndexProperty(id) == null, "invalid image ID");
        }
        for (int id : new int[]{65536, -65536, Integer.MAX_VALUE, -Integer.MAX_VALUE})
            check(BooleanPropertyFactory.getBooleanProperty(id) == null, "invalid boolean ID");
        StringBuilder mask = new StringBuilder();
        for (byte b : digest.digest()) mask.append(String.format("%02x", b & 255));
        System.out.println("PropertyIdCacheTest passed; ID mask SHA-256=" + mask);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
