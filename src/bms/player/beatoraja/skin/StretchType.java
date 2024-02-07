package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * 画像の伸縮方法
 *
 * @author excln
 */
public enum StretchType {
	
	/**
	 * 描画先の範囲に合わせて伸縮する
	 */
    STRETCH(0, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);    	
    }),
    
    /**
     * アスペクト比を保ちつつ描画先の範囲に収まるように伸縮する
     */
    KEEP_ASPECT_RATIO_FIT_INNER(1, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);
        final float scaleX = rectangle.width / image.getRegionWidth();
        final float scaleY = rectangle.height / image.getRegionHeight();
        if (scaleX <= scaleY) {
            fitHeight(rectangle, image.getRegionHeight() * scaleX);
        } else {
            fitWidth(rectangle, image.getRegionWidth() * scaleY);
        }
    }),
    
    /**
     * アスペクト比を保ちつつ描画先の範囲全体を覆うように伸縮する
     */
    KEEP_ASPECT_RATIO_FIT_OUTER(2, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);    	
        final float scaleX = rectangle.width / image.getRegionWidth();
        final float scaleY = rectangle.height / image.getRegionHeight();
        if (scaleX >= scaleY) {
            fitHeight(rectangle, image.getRegionHeight() * scaleX);
        } else {
            fitWidth(rectangle, image.getRegionWidth() * scaleY);
        }
    }),
    KEEP_ASPECT_RATIO_FIT_OUTER_TRIMMED(3, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);    	
        final float scaleX = rectangle.width / image.getRegionWidth();
        final float scaleY = rectangle.height / image.getRegionHeight();
        if (scaleX >= scaleY) {
            fitHeightTrimmed(rectangle, scaleX, trimmedImage);
        } else {
            fitWidthTrimmed(rectangle, scaleY, trimmedImage);
        }
    }),
    
    /**
     * アスペクト比を保ちつつ描画先の横幅に合わせて伸縮する
     */
    KEEP_ASPECT_RATIO_FIT_WIDTH(4, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);    	
        fitHeight(rectangle, image.getRegionHeight() * rectangle.width / image.getRegionWidth());
    }),
    KEEP_ASPECT_RATIO_FIT_WIDTH_TRIMMED(5, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);
        fitHeightTrimmed(rectangle, rectangle.width / image.getRegionWidth(), trimmedImage);
    }),
    
    /**
     * アスペクト比を保ちつつ描画先の縦幅に合わせて伸縮する
     */
    KEEP_ASPECT_RATIO_FIT_HEIGHT(6, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);    	
        fitWidth(rectangle, image.getRegionWidth() * rectangle.height / image.getRegionHeight());
    }),
    KEEP_ASPECT_RATIO_FIT_HEIGHT_TRIMMED(7, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);    	
        fitWidthTrimmed(rectangle, rectangle.height / image.getRegionHeight(), trimmedImage);
    }),
    
    /**
     * 描画先に収まらない場合にはアスペクト比を保ちつつ縮小する
     */
    KEEP_ASPECT_RATIO_NO_EXPANDING(8, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);    	
        final float scale = Math.min(1f, Math.min(rectangle.width / image.getRegionWidth(), rectangle.height / image.getRegionHeight()));
        fitWidth(rectangle, image.getRegionWidth() * scale);
        fitHeight(rectangle, image.getRegionHeight() * scale);
    }),
    
    /**
     * 伸縮しない（中央に合わせる）
     */
    NO_RESIZE(9, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);    	
        fitWidth(rectangle, image.getRegionWidth());
        fitHeight(rectangle, image.getRegionHeight());
    }),
    NO_RESIZE_TRIMMED(10, (rectangle, trimmedImage, image) -> {
        trimmedImage.setRegion(image);    	
        fitWidthTrimmed(rectangle, 1.0f, trimmedImage);
        fitHeightTrimmed(rectangle, 1.0f, trimmedImage);
    }),
    ;
	
	public final Stretcher stretcher;
	
    StretchType(int id, Stretcher stretcher) {
        this.id = id;
        this.stretcher = stretcher;
    }
    public final int id;

    public void stretchRect(Rectangle rectangle, TextureRegion trimmedImage, TextureRegion image) {
    	stretcher.stretch(rectangle, trimmedImage, image);
    }

    private static void fitWidth(Rectangle rectangle, float width) {
        float cx = rectangle.x + rectangle.width * 0.5f;
        rectangle.width = width;
        rectangle.x = cx - rectangle.width * 0.5f;
    }

    private static void fitHeight(Rectangle rectangle, float height) {
        float cy = rectangle.y + rectangle.height * 0.5f;
        rectangle.height = height;
        rectangle.y = cy - rectangle.height * 0.5f;
    }

    private static void fitWidthTrimmed(Rectangle rectangle, float scale, TextureRegion image) {
        float width = scale * image.getRegionWidth();
        if (rectangle.width < width) {
            float cx = image.getRegionX() + image.getRegionWidth() * 0.5f;
            float w = rectangle.width / scale;
            image.setRegionX((int)(cx - w * 0.5f));
            image.setRegionWidth((int)w);
        } else {
            fitWidth(rectangle, width);
        }
    }

    private static void fitHeightTrimmed(Rectangle rectangle, float scale, TextureRegion image) {
        float height = scale * image.getRegionHeight();
        if (rectangle.height < height) {
            float cy = image.getRegionY() + image.getRegionHeight() * 0.5f;
            float h = rectangle.height / scale;
            image.setRegionY((int)(cy - h * 0.5f));
            image.setRegionHeight((int)h);
        } else {
            fitHeight(rectangle, height);
        }
    }

    public interface Stretcher {
    	
    	public void stretch(Rectangle rectangle, TextureRegion trimmedImage, TextureRegion image);
    }
}
