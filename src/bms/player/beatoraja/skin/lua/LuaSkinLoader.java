package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.*;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.logging.Logger;

public class LuaSkinLoader extends JSONSkinLoader {

	public LuaSkinLoader() {
		super(new SkinLuaAccessor());
	}

	public LuaSkinLoader(MainState state, Config c) {
		super(state, c);
	}

	@Override
	public SkinHeader loadHeader(Path p) {
		SkinHeader header = null;
		try {
			lua.setDirectory(p.getParent());
			LuaValue value = lua.execFile(p);
			sk = fromLuaValue(JsonSkin.class, value);
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
		try {
			initFileMap(header, property);
			lua.setSkinProperty(property, (String path) -> {
				return getPath(p.getParent().toString() + "/" + path, filemap).getPath();
			});
			LuaValue value = lua.execFile(p);
			sk = fromLuaValue(JsonSkin.class, value);
			skin = loadJsonSkin(header, sk, type, property, p);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return skin;
	}

	<T> T fromLuaValue(Class<T> cls, LuaValue lv) {
		if (cls.isArray()) {
			Class componentClass = cls.getComponentType();
			if (lv.istable()) {
				LuaTable table = (LuaTable)lv;
				LuaValue[] keys = table.keys();
				Object array = Array.newInstance(componentClass, keys.length);
				for (int i=0; i<keys.length; i++) {
					Array.set(array, i, fromLuaValue(componentClass, table.get(keys[i])));
				}
				return (T)array;
			} else {
				return (T)Array.newInstance(componentClass, 0);
			}
		} else if (cls == boolean.class || cls == Boolean.class) {
			return (T)(Boolean)lv.toboolean();
		} else if (cls == int.class || cls == Integer.class) {
			return (T)(Integer)lv.toint();
		} else if (cls == float.class || cls == Float.class) {
			return (T)(Float)lv.tofloat();
		} else if (cls == String.class) {
			return (T)lv.tojstring();
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
