package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.skin.SkinLoader;
import bms.player.beatoraja.skin.lua.SkinLuaAccessor;
import bms.player.beatoraja.skin.property.*;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JsonSkinSerializer {

	private SkinLuaAccessor lua;
	private Function<String, File> pathGetter;

	public JsonSkinSerializer(SkinLuaAccessor lua, Function<String, File> pathGetter) {
		this.lua = lua;
		this.pathGetter = pathGetter;
	}

	public void setSerializers(Json json, HashSet<Integer> enabledOptions, Path path) {
		Class[] classes = {
				JsonSkin.Property.class,
				JsonSkin.Filepath.class,
				JsonSkin.Offset.class,
				JsonSkin.Source.class,
				JsonSkin.Font.class,
				JsonSkin.Image.class,
				JsonSkin.ImageSet.class,
				JsonSkin.Value.class,
				JsonSkin.FloatValue.class,
				JsonSkin.Text.class,
				JsonSkin.Slider.class,
				JsonSkin.Graph.class,
				JsonSkin.GaugeGraph.class,
				JsonSkin.JudgeGraph.class,
				JsonSkin.HitErrorVisualizer.class,
				JsonSkin.TimingVisualizer.class,
				JsonSkin.TimingDistributionGraph.class,
				JsonSkin.NoteSet.class,
				JsonSkin.Gauge.class,
				JsonSkin.BGA.class,
				JsonSkin.Judge.class,
				JsonSkin.SongList.class,
				JsonSkin.Destination.class,
				JsonSkin.Animation.class,
				JsonSkin.SkinConfigurationProperty.class,
				JsonSkin.CustomEvent.class,
				JsonSkin.CustomTimer.class,
		};
		for (Class c : classes) {
			json.setSerializer(c, new ObjectSerializer<>(enabledOptions, path));
		}

		Class[] array_classes = {
				JsonSkin.Property[].class,
				JsonSkin.Filepath[].class,
				JsonSkin.Offset[].class,
				JsonSkin.Source[].class,
				JsonSkin.Font[].class,
				JsonSkin.Image[].class,
				JsonSkin.ImageSet[].class,
				JsonSkin.Value[].class,
				JsonSkin.FloatValue[].class,
				JsonSkin.Text[].class,
				JsonSkin.Slider[].class,
				JsonSkin.Graph[].class,
				JsonSkin.GaugeGraph[].class,
				JsonSkin.JudgeGraph[].class,
				JsonSkin.HitErrorVisualizer[].class,
				JsonSkin.TimingVisualizer[].class,
				JsonSkin.TimingDistributionGraph[].class,
				JsonSkin.Judge[].class,
				JsonSkin.Destination[].class,
				JsonSkin.Animation[].class,
				JsonSkin.CustomEvent[].class,
				JsonSkin.CustomTimer[].class,
		};
		for (Class c : array_classes) {
			json.setSerializer(c, new ArraySerializer<>(enabledOptions, path));
		}

		json.setSerializer(BooleanProperty.class, new LuaScriptSerializer<>(SkinLuaAccessor::loadBooleanProperty, BooleanPropertyFactory::getBooleanProperty));
		json.setSerializer(IntegerProperty.class, new LuaScriptSerializer<>(SkinLuaAccessor::loadIntegerProperty, IntegerPropertyFactory::getIntegerProperty));
		json.setSerializer(FloatProperty.class, new LuaScriptSerializer<>(SkinLuaAccessor::loadFloatProperty, FloatPropertyFactory::getRateProperty));
		json.setSerializer(StringProperty.class, new LuaScriptSerializer<>(SkinLuaAccessor::loadStringProperty, StringPropertyFactory::getStringProperty));
		json.setSerializer(TimerProperty.class, new LuaScriptSerializer<>(SkinLuaAccessor::loadTimerProperty, TimerPropertyFactory::getTimerProperty));
		json.setSerializer(FloatWriter.class, new LuaScriptSerializer<>(SkinLuaAccessor::loadFloatWriter, FloatPropertyFactory::getRateWriter));
		json.setSerializer(Event.class, new LuaScriptSerializer<>(SkinLuaAccessor::loadEvent, EventFactory::getEvent));
	}

	private abstract class Serializer<T> extends Json.ReadOnlySerializer<T> {

		HashSet<Integer> options;
		Path path;

		public Serializer(HashSet<Integer> op, Path path) {
			this.options = op != null ? op : new HashSet<>();
			this.path = path;
		}

		// test "if" as follows:
		// 901 -> 901 enabled
		// [901, 911] -> 901 enabled && 911 enabled
		// [[901, 902], 911] -> (901 || 902) && 911
		// -901 -> 901 disabled
		protected boolean testOption(JsonValue ops) {
			if (ops == null) {
				return true;
			} else if (ops.isNumber()) {
				return testNumber(ops.asInt());
			} else if (ops.isArray()) {
				boolean enabled = true;
				for (int j = 0; j < ops.size; j++) {
					JsonValue ops2 = ops.get(j);
					if (ops2.isNumber()) {
						enabled = testNumber(ops2.asInt());
					} else if (ops2.isArray()) {
						boolean enabled_sub = false;
						for (int k = 0; k < ops2.size; k++) {
							JsonValue ops3 = ops2.get(k);
							if (ops3.isNumber() && testNumber(ops3.asInt())) {
								enabled_sub = true;
								break;
							}
						}
						enabled = enabled_sub;
					} else {
						enabled = false;
					}
					if (!enabled)
						break;
				}
				return enabled;
			} else {
				return false;
			}
		}

		private boolean testNumber(int op) {
			return op >= 0 ? options.contains(op) : !options.contains(-op);
		}
	}

	private class ObjectSerializer<T> extends Serializer<T> {

		public ObjectSerializer(HashSet<Integer> op, Path path) {
			super(op, path);
		}

		public T read(Json json, JsonValue jsonValue, Class cls) {
			if (jsonValue.isArray()) {
				// conditional branch
				// take first clause satisfying its conditions
				JsonValue val = null;
				for (int i = 0; i < jsonValue.size; i++) {
					JsonValue branch = jsonValue.get(i);
					if (testOption(branch.get("if"))) {
						val = branch.get("value");
						break;
					}
				}
				return (T)json.readValue(cls, val);
			} else if (jsonValue.isObject() && jsonValue.has("include")) {
				Json subJson = new Json();
				subJson.setIgnoreUnknownFields(true);
				File file = pathGetter.apply(path.getParent().toString() + "/" + jsonValue.get("include").asString());
				if (file.exists()) {
					setSerializers(subJson, this.options, file.toPath());
					try {
						return (T)subJson.fromJson(cls, new FileReader(file));
					} catch (FileNotFoundException e) {
					}
				}
				return null;
			} else {
				// literal
				T instance = null;
				try {
					instance = (T)ClassReflection.newInstance(cls);
				} catch (ReflectionException e) {
					e.printStackTrace();
					return null;
				}
				try {
					Field[] fields = ClassReflection.getFields(cls);
					for (JsonValue child = jsonValue.child; child != null; child = child.next) {
						for (Field field : fields) {
							if (field.getName().equals(child.name)) {
								field.set(instance, json.readValue(field.getType(), child));
								break;
							}
						}
					}
				} catch (ReflectionException e) {
				} catch (NullPointerException e) {
				}
				return instance;
			}
		}
	}

	private class ArraySerializer<T> extends Serializer<T[]> {

		public ArraySerializer(HashSet<Integer> op, Path path) {
			super(op, path);
		}

		public T[] read(Json json, JsonValue jsonValue, Class cls) {
			Class componentClass = cls.getComponentType();
			ArrayList<T> items = new ArrayList<T>();
			try {
				if (jsonValue.isArray()) {
					for (int i = 0; i < jsonValue.size; i++) {
						JsonValue item = jsonValue.get(i);
						if (item.isObject() && item.has("if") && (item.has("value") || item.has("values"))) {
							// conditional item(s)
							// add item(s) to array if conditions are satisfied
							JsonValue value = item.get("value");
							JsonValue values = item.get("values");
							if (testOption(item.get("if"))) {
								if (value != null) {
									T obj = (T) json.readValue(componentClass, value);
									items.add(obj);
								}
								if (values != null) {
									T[] objs = (T[]) json.readValue(cls, values);
									Collections.addAll(items, objs);
								}
							}
						} else if (item.isObject() && item.has("include")) {
							// array include (inside)
							includeArray(json, item, cls, items);
						} else {
							// single item
							T obj = (T)json.readValue(componentClass, item);
							items.add(obj);
						}
					}
				} else if (jsonValue.isObject() && jsonValue.has("include")) {
					// array include (outside)
					includeArray(json, jsonValue, cls, items);
				} else if (jsonValue.isObject()) {
					// regard as a single item
					T obj = (T)json.readValue(componentClass, jsonValue);
					items.add(obj);
				}
			} catch (NullPointerException e) {
			}
			Object array = Array.newInstance(componentClass, items.size());
			for (int i=0; i<items.size(); i++) {
				Array.set(array, i, items.get(i));
			}
			return (T[])array;
		}

		private void includeArray(Json json, JsonValue jsonValue, Class cls, ArrayList<T> items) {
			Json subJson = new Json();
			subJson.setIgnoreUnknownFields(true);
			File file = pathGetter.apply(path.getParent().toString() + "/" + jsonValue.get("include").asString());
			if (file.exists()) {
				setSerializers(subJson, this.options, file.toPath());
				try {
					T[] array = (T[])subJson.fromJson(cls, new FileReader(file));
					Collections.addAll(items, array);
				} catch (FileNotFoundException e) {
				}
			}
		}
	}

	private class LuaScriptSerializer<T> extends Json.ReadOnlySerializer<T> {
		BiFunction<SkinLuaAccessor, String, T> luaPropertyLoader;
		Function<Integer, T> idPropertyLoader;

		LuaScriptSerializer(BiFunction<SkinLuaAccessor, String, T> loader, Function<Integer, T> byId) {
			luaPropertyLoader = loader;
			idPropertyLoader = byId;
		}

		public T read(Json json, JsonValue jsonValue, Class cls) {
			if (jsonValue.isString() && luaPropertyLoader != null) {
				return luaPropertyLoader.apply(lua, jsonValue.asString());
			} else if (jsonValue.isNumber() && idPropertyLoader != null) {
				return idPropertyLoader.apply(jsonValue.asInt());
			} else {
				return null;
			}
		}
	}
}
