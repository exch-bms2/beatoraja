package bms.player.beatoraja.skin.json;

import java.io.File;
import java.nio.file.Path;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinLoader;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinSource;
import bms.player.beatoraja.skin.SkinSourceImage;
import bms.player.beatoraja.skin.SkinText;
import bms.player.beatoraja.skin.SkinTextBitmap;
import bms.player.beatoraja.skin.SkinTextFont;
import bms.player.beatoraja.skin.json.JSONSkinLoader.SourceData;
import bms.player.beatoraja.skin.property.StringProperty;
import bms.player.beatoraja.skin.property.StringPropertyFactory;

/**
 * JSONスキンオブジェクトローダー
 * 
 * @author exch
 *
 * @param <S>
 */
public abstract class JsonSkinObjectLoader<S extends Skin> {
	
	private final JSONSkinLoader loader;
	
	public JsonSkinObjectLoader(JSONSkinLoader loader) {
		this.loader = loader;
	}
	
	public abstract S getSkin(Resolution src, Resolution dst);
	
	public abstract SkinObject loadSkinObject(Skin skin, JsonSkin.Skin sk, String dstid, Path p);

	protected Texture getTexture(String srcid, Path p) {
		if(srcid == null) {
			return null;
		}
		
		final SourceData data = loader.sourceMap.get(srcid);
		if(data == null) {
			return null;
		}

		if(data.loaded) {
			return (data.data instanceof Texture) ? (Texture)data.data : null;
		}
		final File imagefile = SkinLoader.getPath(p.getParent().toString() + "/" + data.path, loader.filemap);
		if (imagefile.exists()) {
			data.data = getTexture(imagefile.getPath());
		}
		data.loaded = true;
		
		return (Texture) data.data;
	}

	protected SkinSource[] getNoteTexture(String[] images, Path p) {
		SkinSource[] noteimages = new SkinSource[images.length];
		for(int i = 0;i < images.length;i++) {
			for (JsonSkin.Image img : loader.sk.image) {
				if (images[i].equals(img.id)) {
					JsonSkin.Image note = img;
					Texture tex = getTexture(note.src, p);
					noteimages[i] = new SkinSourceImage(getSourceImage(tex,  note.x, note.y, note.w,
							note.h, note.divx, note.divy), note.timer, note.cycle);
					break;
				}
			}

		}
		return noteimages;
	}

	protected TextureRegion[] getSourceImage(Texture image, int x, int y, int w, int h, int divx, int divy) {
		if (w == -1) {
			w = image.getWidth();
		}
		if (h == -1) {
			h = image.getHeight();
		}
		if (divx <= 0) {
			divx = 1;
		}
		if (divy <= 0) {
			divy = 1;
		}
		TextureRegion[] images = new TextureRegion[divx * divy];
		for (int i = 0; i < divx; i++) {
			for (int j = 0; j < divy; j++) {
				images[divx * j + i] = new TextureRegion(image, x + w / divx * i, y + h / divy * j, w / divx, h / divy);
			}
		}
		return images;
	}

	protected Texture getTexture(String path) {
		return SkinLoader.getTexture(path, loader.usecim);
	}

	protected SkinText createText(JsonSkin.Text text, Path skinPath) {
		for (JsonSkin.Font font : loader.sk.font) {
			if (font.id.equals(text.font)) {
				Path path = skinPath.getParent().resolve(font.path);
				SkinText skinText;
				StringProperty property = text.value;
				if (property == null) {
					property = StringPropertyFactory.getStringProperty(text.ref);
				}
				if (path.toString().toLowerCase().endsWith(".fnt")) {
					if (!loader.bitmapSourceMap.containsKey(font.id)) {
						SkinTextBitmap.SkinTextBitmapSource source = new SkinTextBitmap.SkinTextBitmapSource(path, loader.usecim);
						source.setType(font.type);
						loader.bitmapSourceMap.put(font.id, source);
					}
					skinText = new SkinTextBitmap(loader.bitmapSourceMap.get(font.id), text.size * ((float)loader.dstr.width / loader.sk.w), property);
				} else {
					skinText = new SkinTextFont(path.toString(), 0, text.size, 0, property);
				}
				skinText.setConstantText(text.constantText);
				skinText.setAlign(text.align);
				skinText.setWrapping(text.wrapping);
				skinText.setOverflow(text.overflow);
				skinText.setOutlineColor(parseHexColor(text.outlineColor, Color.WHITE));
				skinText.setOutlineWidth(text.outlineWidth);
				skinText.setShadowColor(parseHexColor(text.shadowColor, Color.WHITE));
				skinText.setShadowOffset(new Vector2(text.shadowOffsetX, text.shadowOffsetY));
				skinText.setShadowSmoothness(text.shadowSmoothness);
				return skinText;
			}
		}
		return null;
	}

	protected Color parseHexColor(String hex, Color fallbackColor) {
		try {
			return Color.valueOf(hex);
		} catch (Exception e) {
			return fallbackColor;
		}
	}

	protected File getSrcIdPath(String srcid, Path p) {
		if(srcid == null) {
			return null;
		}
		
		final SourceData data = loader.sourceMap.get(srcid);
		if(data == null) {
			return null;
		}
		
		return SkinLoader.getPath(p.getParent().toString() + "/" + data.path, loader.filemap);
	}

	protected void setDestination(Skin skin, SkinObject obj, JsonSkin.Destination dst) {
		JsonSkin.Animation prev = null;
		for (JsonSkin.Animation a : dst.dst) {
			if (prev == null) {
				a.time = (a.time == Integer.MIN_VALUE ? 0 : a.time);
				a.x = (a.x == Integer.MIN_VALUE ? 0 : a.x);
				a.y = (a.y == Integer.MIN_VALUE ? 0 : a.y);
				a.w = (a.w == Integer.MIN_VALUE ? 0 : a.w);
				a.h = (a.h == Integer.MIN_VALUE ? 0 : a.h);
				a.acc = (a.acc == Integer.MIN_VALUE ? 0 : a.acc);
				a.angle = (a.angle == Integer.MIN_VALUE ? 0 : a.angle);
				a.a = (a.a == Integer.MIN_VALUE ? 255 : a.a);
				a.r = (a.r == Integer.MIN_VALUE ? 255 : a.r);
				a.g = (a.g == Integer.MIN_VALUE ? 255 : a.g);
				a.b = (a.b == Integer.MIN_VALUE ? 255 : a.b);
			} else {
				a.time = (a.time == Integer.MIN_VALUE ? prev.time : a.time);
				a.x = (a.x == Integer.MIN_VALUE ? prev.x : a.x);
				a.y = (a.y == Integer.MIN_VALUE ? prev.y : a.y);
				a.w = (a.w == Integer.MIN_VALUE ? prev.w : a.w);
				a.h = (a.h == Integer.MIN_VALUE ? prev.h : a.h);
				a.acc = (a.acc == Integer.MIN_VALUE ? prev.acc : a.acc);
				a.angle = (a.angle == Integer.MIN_VALUE ? prev.angle : a.angle);
				a.a = (a.a == Integer.MIN_VALUE ? prev.a : a.a);
				a.r = (a.r == Integer.MIN_VALUE ? prev.r : a.r);
				a.g = (a.g == Integer.MIN_VALUE ? prev.g : a.g);
				a.b = (a.b == Integer.MIN_VALUE ? prev.b : a.b);
			}
			if(dst.draw != null) {
				skin.setDestination(obj, a.time, a.x, a.y, a.w, a.h, a.acc, a.a, a.r, a.g, a.b, dst.blend, dst.filter,
						a.angle, dst.center, dst.loop, dst.timer, dst.draw);
			} else {
				skin.setDestination(obj, a.time, a.x, a.y, a.w, a.h, a.acc, a.a, a.r, a.g, a.b, dst.blend, dst.filter,
						a.angle, dst.center, dst.loop, dst.timer, dst.op);
			}
			if (dst.mouseRect != null) {
				skin.setMouseRect(obj, dst.mouseRect.x, dst.mouseRect.y, dst.mouseRect.w, dst.mouseRect.h);
			}
			prev = a;
		}

		int[] offsets = new int[dst.offsets.length + 1];
		for(int i = 0; i < dst.offsets.length; i++) {
			offsets[i] = dst.offsets[i];
		}
		offsets[dst.offsets.length] = dst.offset;
		obj.setOffsetID(offsets);
		if (dst.stretch >= 0) {
			obj.setStretch(dst.stretch);
		}
	}

}
