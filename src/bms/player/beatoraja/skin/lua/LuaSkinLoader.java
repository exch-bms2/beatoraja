package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.json.JSONSkinLoader;
import bms.player.beatoraja.skin.json.JsonSkin;
import bms.player.beatoraja.skin.property.*;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Luaスキンローダー
 * 
 * @author excln
 */
public class LuaSkinLoader extends JSONSkinLoader {

	public LuaSkinLoader() {
		super(new SkinLuaAccessor(false));
	}

	public LuaSkinLoader(MainState state, Config c) {
		super(state, c, new SkinLuaAccessor(false));
	}

	@Override
	public SkinHeader loadHeader(Path p) {
		SkinHeader header = null;
		try {
			lua.setDirectory(p.getParent());
			LuaValue value = lua.execFile(p);
			sk = fromLuaValue(JsonSkin.Skin.class, value);
			header = loadJsonSkinHeader(sk, p);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return header;
	}

	@Override
	public Skin loadSkin(Path p, SkinType type, SkinConfig.Property property) {
		return load(p, type, property);
	}

	@Override
	public Skin load(Path p, SkinType type, SkinConfig.Property property) {
		Skin skin = null;
		SkinHeader header = loadHeader(p);
		if(header == null) {
			return null;
		}
		header.setSkinConfigProperty(property);
		
		try {			
			filemap = new ObjectMap<>();
			for(SkinHeader.CustomFile customFile : header.getCustomFiles()) {
				if(customFile.getSelectedFilename() != null) {
					filemap.put(customFile.path, customFile.getSelectedFilename());
				}
			}

			lua.exportSkinProperty(header, property, (String path) -> {
				return getPath(p.getParent().toString() + "/" + path, filemap).getPath();
			});
			LuaValue value = lua.execFile(p);
			sk = fromLuaValue(JsonSkin.Skin.class, value);
			skin = loadJsonSkin(header, sk, type, property, p);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return skin;
	}

	private Map<Class, Function<LuaValue, Object>> serializerMap = new HashMap<Class, Function<LuaValue, Object>>() {
		{
			put(boolean.class, LuaValue::toboolean);
			put(Boolean.class, LuaValue::toboolean);
			put(int.class, LuaValue::toint);
			put(Integer.class, LuaValue::toint);
			put(float.class, LuaValue::tofloat);
			put(Float.class, LuaValue::tofloat);
			put(String.class, LuaValue::tojstring);
			put(BooleanProperty.class, lv ->
					serializeLuaScript(lv, lua::loadBooleanProperty, lua::loadBooleanProperty, BooleanPropertyFactory::getBooleanProperty));
			put(IntegerProperty.class, lv ->
					serializeLuaScript(lv, lua::loadIntegerProperty, lua::loadIntegerProperty, IntegerPropertyFactory::getIntegerProperty));
			put(FloatProperty.class, lv ->
					serializeLuaScript(lv, lua::loadFloatProperty, lua::loadFloatProperty, FloatPropertyFactory::getRateProperty));
			put(StringProperty.class, lv ->
					serializeLuaScript(lv, lua::loadStringProperty, lua::loadStringProperty, StringPropertyFactory::getStringProperty));
			put(TimerProperty.class, lv ->
					serializeLuaScript(lv, lua::loadTimerProperty, lua::loadTimerProperty, TimerPropertyFactory::getTimerProperty));
			put(FloatWriter.class, lv ->
					serializeLuaScript(lv, lua::loadFloatWriter, lua::loadFloatWriter, FloatPropertyFactory::getRateWriter));
			put(Event.class, lv ->
					serializeLuaScript(lv, lua::loadEvent, lua::loadEvent, EventFactory::getEvent));
		}
	};

	private static <T> T serializeLuaScript(LuaValue lv, Function<LuaFunction, T> asFunction, Function<String, T> asScript, Function<Integer, T> byId) {
		if (lv.isfunction()) {
			return asFunction.apply(lv.checkfunction());
		} else if (lv.isnumber() && byId != null) {
			return byId.apply(lv.toint());
		} else if (lv.isstring()) {
			return asScript.apply(lv.tojstring());
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	<T> T fromLuaValue(Class<T> cls, LuaValue lv) {
		if (serializerMap.containsKey(cls)) {
			return (T) serializerMap.get(cls).apply(lv);
		} else if (cls.isArray()) {
			Class componentClass = cls.getComponentType();
			if (lv.istable()) {
				LuaTable table = (LuaTable) lv;
				LuaValue[] keys = table.keys();
				Object array = Array.newInstance(componentClass, keys.length);
				for (int i = 0; i < keys.length; i++) {
					Array.set(array, i, fromLuaValue(componentClass, table.get(keys[i])));
				}
				return (T) array;
			} else {
				return (T) Array.newInstance(componentClass, 0);
			}
		} else {
			try {
				T instance = (T) ClassReflection.newInstance(cls);
				Field[] fields = ClassReflection.getFields(cls);
				if (lv.istable()) {
					LuaTable table = (LuaTable)lv;
					for (LuaValue key : table.keys()) {
						String keyName = key.tojstring();
						for (Field field : fields) {
							if (field.getName().equals(keyName)) {
								Object value = fromLuaValue(field.getType(), table.get(key));
								field.set(instance, value);
								break;
							}
						}
					}
				} else if (lv.isuserdata()) {
				}
				return instance;
			} catch (ReflectionException e) {
				return null;
			}
		}
	}
}
