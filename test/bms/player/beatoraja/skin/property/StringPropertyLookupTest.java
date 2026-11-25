package bms.player.beatoraja.skin.property;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Compare indexed lookup with the previous declaration-order search, including gaps and aliases. */
public final class StringPropertyLookupTest {
    public static void main(String[] args) throws Exception {
        Class<?> pattern = Class.forName(StringPropertyFactory.class.getName() + "$StringPropertyPattern");
        Object[] patterns = pattern.getEnumConstants();
        Field first = field(pattern, "firstId"), properties = field(pattern, "properties");
        Class<?> single = StringPropertyFactory.StringType.class;
        Object[] types = single.getEnumConstants();
        Field id = field(single, "id"), property = field(single, "property");
        for (int value = -1; value <= 65536; value++)
            check(StringPropertyFactory.getStringProperty(value) == reference(value, patterns, first, properties, types, id, property), "ID " + value);
        for (int value : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE})
            check(StringPropertyFactory.getStringProperty(value) == reference(value, patterns, first, properties, types, id, property), "extreme ID " + value);

        Method byName = pattern.getDeclaredMethod("getByName", String.class); byName.setAccessible(true);
        for (String name : new String[]{null, "", "missing", "key0", "key1", "key01", "key10", "key11", "key54", "key55",
                "rankingname1", "rankingname10", "rankingname11", "targetnamep1", "targetnamep10", "targetnamen1",
                "practice_item1", "practice_item16", "practice_item17", "practice_item1_label", "practice_item_label1",
                "practice_item1_value", "practice_item_value1", "practice_item2147483648", "title", "artist"}) {
            StringProperty expected = null;
            for (Object p : patterns) { expected = (StringProperty) byName.invoke(p, name); if (expected != null) break; }
            if (expected == null) for (Object t : types) if (((Enum<?>) t).name().equals(name)) { expected = (StringProperty) property.get(t); break; }
            check(StringPropertyFactory.getStringProperty(name) == expected, "name " + name);
        }
        System.out.println("StringPropertyLookupTest passed (65538 IDs, extremes, names and aliases)");
    }
    private static StringProperty reference(int value, Object[] patterns, Field first, Field properties,
            Object[] types, Field id, Field property) throws Exception {
        for (Object p : patterns) {
            StringProperty[] entries = (StringProperty[]) properties.get(p);
            int index = value - first.getInt(p);
            if (index >= 0 && index < entries.length) return entries[index];
        }
        for (Object t : types) if (id.getInt(t) == value) return (StringProperty) property.get(t);
        return null;
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
