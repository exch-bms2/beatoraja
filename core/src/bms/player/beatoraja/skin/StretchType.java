package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * 画像の伸縮方法
 *
 * @author excln
 */
public enum StretchType {
    // 描画先の範囲に合わせて伸縮する
    STRETCH(0),
    // アスペクト比を保ちつつ描画先の範囲に収まるように伸縮する
    KEEP_ASPECT_RATIO_FIT_INNER(1),
    // アスペクト比を保ちつつ描画先の範囲全体を覆うように伸縮する
    KEEP_ASPECT_RATIO_FIT_OUTER(2),
    KEEP_ASPECT_RATIO_FIT_OUTER_TRIMMED(3),
    // アスペクト比を保ちつつ描画先の横幅に合わせて伸縮する
    KEEP_ASPECT_RATIO_FIT_WIDTH(4),
    KEEP_ASPECT_RATIO_FIT_WIDTH_TRIMMED(5),
    // アスペクト比を保ちつつ描画先の縦幅に合わせて伸縮する
    KEEP_ASPECT_RATIO_FIT_HEIGHT(6),
    KEEP_ASPECT_RATIO_FIT_HEIGHT_TRIMMED(7),
    // 描画先に収まらない場合にはアスペクト比を保ちつつ縮小する
    KEEP_ASPECT_RATIO_NO_EXPANDING(8),
    // 伸縮しない（中央に合わせる）
    NO_RESIZE(9),
    NO_RESIZE_TRIMMED(10),
    ;
    StretchType(int id) {
        this.id = id;
    }
    public final int id;

    public void stretchRect(Rectangle rectangle, TextureRegion trimmedImage, TextureRegion image) {
        trimmedImage.setRegion(image);
        if (this == StretchType.STRETCH) {
            return;
        }
        float scaleX = rectangle.width / image.getRegionWidth();
        float scaleY = rectangle.height / image.getRegionHeight();
        switch (this) {
            case KEEP_ASPECT_RATIO_FIT_INNER:
                if (scaleX <= scaleY) {
                    fitHeight(rectangle, image.getRegionHeight() * scaleX);
                } else {
                    fitWidth(rectangle, image.getRegionWidth() * scaleY);
                }
                break;
            case KEEP_ASPECT_RATIO_FIT_OUTER:
                if (scaleX >= scaleY) {
                    fitHeight(rectangle, image.getRegionHeight() * scaleX);
                } else {
                    fitWidth(rectangle, image.getRegionWidth() * scaleY);
                }
                break;
            case KEEP_ASPECT_RATIO_FIT_OUTER_TRIMMED:
                if (scaleX >= scaleY) {
                    fitHeightTrimmed(rectangle, scaleX, trimmedImage);
                } else {
                    fitWidthTrimmed(rectangle, scaleY, trimmedImage);
                }
                break;
            case KEEP_ASPECT_RATIO_FIT_WIDTH:
                fitHeight(rectangle, image.getRegionHeight() * scaleX);
                break;
            case KEEP_ASPECT_RATIO_FIT_WIDTH_TRIMMED:
                fitHeightTrimmed(rectangle, scaleX, trimmedImage);
                break;
            case KEEP_ASPECT_RATIO_FIT_HEIGHT:
                fitWidth(rectangle, image.getRegionWidth() * scaleY);
                break;
            case KEEP_ASPECT_RATIO_FIT_HEIGHT_TRIMMED:
                fitWidthTrimmed(rectangle, scaleY, trimmedImage);
                break;
            case KEEP_ASPECT_RATIO_NO_EXPANDING: {
                float scale = Math.min(1f, Math.min(scaleX, scaleY));
                fitWidth(rectangle, image.getRegionWidth() * scale);
                fitHeight(rectangle, image.getRegionHeight() * scale);
                break;
            }
            case NO_RESIZE:
                fitWidth(rectangle, image.getRegionWidth());
                fitHeight(rectangle, image.getRegionHeight());
                break;
            case NO_RESIZE_TRIMMED:
                fitWidthTrimmed(rectangle, 1.0f, trimmedImage);
                fitHeightTrimmed(rectangle, 1.0f, trimmedImage);
                break;
        }
    }

    private void fitWidth(Rectangle rectangle, float width) {
        float cx = rectangle.x + rectangle.width * 0.5f;
        rectangle.width = width;
        rectangle.x = cx - rectangle.width * 0.5f;
    }

    private void fitHeight(Rectangle rectangle, float height) {
        float cy = rectangle.y + rectangle.height * 0.5f;
        rectangle.height = height;
        rectangle.y = cy - rectangle.height * 0.5f;
    }

    private void fitWidthTrimmed(Rectangle rectangle, float scale, TextureRegion image) {
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

    private void fitHeightTrimmed(Rectangle rectangle, float scale, TextureRegion image) {
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


}
