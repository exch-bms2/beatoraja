package bms.player.beatoraja;

import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderManager {

	private static HashMap<String, ShaderProgram> shaders = new HashMap();

	public static ShaderProgram getShader(String name) {
		if (!shaders.containsKey(name)) {
			ShaderProgram shader = new ShaderProgram(Gdx.files.classpath("glsl/" + name + ".vert"),
					Gdx.files.classpath("glsl/" + name + ".frag"));
			if(shader.isCompiled()) {
				shaders.put(name, shader);
				return shader;				
			}
		}
		return shaders.get(name);
	}
	
	public static void dispose() {
		for(Entry<String, ShaderProgram> e : shaders.entrySet()) {
			if(e.getValue() != null) {
				e.getValue().dispose();
			}
		}
	}
}
