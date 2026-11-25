package bms.player.beatoraja.skin.json;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * destination から辿れる source の id を集める。画像の先読み対象を絞るためのもので、取りこぼしても
 * その画像は従来どおり描画スレッドで読まれるだけ(正しさには影響しない)。
 *
 * Collects the source ids reachable from the destinations, to limit image prefetching. A missed source
 * is only loaded serially on the render thread as before, so this need not be exact.
 * Definitions are matched by id: any String or String[] field of a reachable definition that names another
 * definition (image set -> images, gauge -> nodes, judge -> images/values, ...) or a source is followed.
 */
final class JsonSkinSourceReferences {

	private JsonSkinSourceReferences() {
	}

	static Set<String> collect(JsonSkin.Skin sk) {
		final Set<String> sourceIds = new HashSet<>();
		if (sk.source != null) {
			for (JsonSkin.Source source : sk.source) {
				if (source != null && source.id != null) {
					sourceIds.add(source.id);
				}
			}
		}
		final Map<String, List<Object>> definitions = indexDefinitions(sk);

		final Set<String> result = new LinkedHashSet<>();
		final Set<String> seen = new HashSet<>();
		final ArrayDeque<String> queue = new ArrayDeque<>();
		if (sk.destination != null) {
			for (JsonSkin.Destination dst : sk.destination) {
				if (dst != null && dst.id != null && seen.add(dst.id)) {
					queue.add(dst.id);
				}
			}
		}
		final List<String> strings = new ArrayList<>();
		while (!queue.isEmpty()) {
			final String id = queue.poll();
			if (sourceIds.contains(id)) {
				result.add(id);
			}
			final List<Object> defs = definitions.get(id);
			if (defs == null) {
				continue;
			}
			for (Object def : defs) {
				strings.clear();
				collectStrings(def, strings, 0);
				for (String s : strings) {
					if (seen.add(s) && (sourceIds.contains(s) || definitions.containsKey(s))) {
						queue.add(s);
					}
				}
			}
		}
		return result;
	}

	/** id -> definitions with that id, for every JsonSkin object held by sk (except sources and destinations). */
	private static Map<String, List<Object>> indexDefinitions(JsonSkin.Skin sk) {
		final Map<String, List<Object>> definitions = new HashMap<>();
		for (Field field : JsonSkin.Skin.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("source")
					|| field.getName().equals("destination")) {
				continue;
			}
			final Object value;
			try {
				value = field.get(sk);
			} catch (IllegalAccessException e) {
				continue;
			}
			if (value instanceof Object[] array) {
				for (Object element : array) {
					index(definitions, element);
				}
			} else {
				index(definitions, value);
			}
		}
		return definitions;
	}

	private static void index(Map<String, List<Object>> definitions, Object def) {
		if (!isJsonSkinObject(def)) {
			return;
		}
		try {
			final Field idField = def.getClass().getField("id");
			if (idField.get(def) instanceof String id) {
				definitions.computeIfAbsent(id, ignored -> new ArrayList<>(1)).add(def);
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			// definitions without an id cannot be referenced
		}
	}

	private static void collectStrings(Object obj, List<String> out, int depth) {
		if (obj == null || depth > 4) {
			return;
		}
		for (Field field : obj.getClass().getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			final Object value;
			try {
				value = field.get(obj);
			} catch (IllegalAccessException e) {
				continue;
			}
			if (value instanceof String s) {
				out.add(s);
			} else if (value instanceof String[] array) {
				for (String s : array) {
					if (s != null) {
						out.add(s);
					}
				}
			} else if (value instanceof Object[] array) {
				for (Object element : array) {
					if (isJsonSkinObject(element)) {
						collectStrings(element, out, depth + 1);
					}
				}
			} else if (isJsonSkinObject(value)) {
				collectStrings(value, out, depth + 1);
			}
		}
	}

	private static boolean isJsonSkinObject(Object value) {
		return value != null && value.getClass().getEnclosingClass() == JsonSkin.class;
	}
}
