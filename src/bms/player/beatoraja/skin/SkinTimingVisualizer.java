package bms.player.beatoraja.skin;

import java.util.Optional;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

/**
 * 判定タイミングの可視化
 *
 * @author keh
 */
public class SkinTimingVisualizer extends SkinObject {

	private TextureRegion backtex = null;
	private TextureRegion shapetex = null;
	private Pixmap shape = null;

	// 色はスキン側で設定できるように
	private Color[] JColor;
	private Color lineColor;
	private Color centerColor;

	private static final Color CLEAR = Color.valueOf("00000000");

	// 線の幅をスキン側で設定できるように
	private final int lineWidth;
	private final int width;
	private final int center;
	private final float judgeWidthRate;
	private final boolean drawDecay;

	/**
	 *
	 * @param width スキン描画幅
	 * @param judgeWidthMillis 判定描画幅
	 * @param lineWidth 入力線の幅
	 * @param Color RRGGBBAA形式
	 * @param transparent 1:POOR判定を透過する
	 * @param drawDecay 1:線を減衰させる
	 */
	public SkinTimingVisualizer(int width, int judgeWidthMillis, int lineWidth,
			String lineColor, String centerColor, String PGColor, String GRColor, String GDColor, String BDColor, String PRColor,
			int transparent, int drawDecay) {

		this.lineWidth = (lineWidth < 1 ? 1 : 10 < lineWidth ? 10 : lineWidth);
		this.width = width;
		this.center = judgeWidthMillis;
		this.judgeWidthRate = width / (float) (judgeWidthMillis * 2 + 1);
		this.lineColor = Color.valueOf(colorStringValidation(lineColor));
		this.centerColor = Color.valueOf(colorStringValidation(centerColor));
		JColor = new Color[] {
				Color.valueOf(colorStringValidation(PGColor)),
				Color.valueOf(colorStringValidation(GRColor)),
				Color.valueOf(colorStringValidation(GDColor)),
				Color.valueOf(colorStringValidation(BDColor)),
				transparent == 1 ? CLEAR : Color.valueOf(PRColor)
		};
		this.drawDecay = drawDecay == 1 ? true : false;
	}

	@Override
	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		if (state instanceof BMSPlayer) {
			draw(sprite, time, (BMSPlayer) state, getDestination(time, state));
		}
	}

	private void draw(SkinObjectRenderer sprite, long time, BMSPlayer state, Rectangle r) {
		if (r == null) {
			return;
		}

		PlayerResource resource = state.main.getPlayerResource();
		int[][] judgeArea = getJudgeArea(resource);

		// 背景テクスチャ生成
		if (backtex == null) {
			int pwidth = center * 2 + 1;
			shape = new Pixmap(pwidth, 1, Pixmap.Format.RGBA8888);

			int beforex1 = center;
			int beforex2 = center + 1;
			shape.setColor(centerColor);
			shape.fillRectangle(center, 0, 1, 1);
			for (int i = 0; i < JColor.length; i++) {
				shape.setColor(JColor[i]);
				int x1 = center + Math.max(-center, Math.min(judgeArea[i][0], center));
				int x2 = center + Math.max(-center, Math.min(judgeArea[i][1], center)) + 1;

				if (beforex1 > x1) {
				shape.fillRectangle(x1, 0, Math.abs(x1 - beforex1), 1);
				beforex1 = x1;
				}

				if (x2 > beforex2) {
				shape.fillRectangle(beforex2, 0, Math.abs(x2 - beforex2), 1);
				beforex2 = x2;
				}
			}

			backtex = new TextureRegion(new Texture(shape));
			shape.dispose();
			shape = null;
		}

		int index = state.getJudgeManager().getRecentJudgesIndex();
		long[] recent = state.getJudgeManager().getRecentJudges();

		if (shape == null) {
			shape = new Pixmap(width, recent.length * 2, Pixmap.Format.RGBA8888);
		}
		// 前景テクスチャ 透明色でフィルして初期化
		shape.setColor(CLEAR);
		shape.fill();

		for (int i = 0; i < recent.length; i++) {
			int j = i + index + 1;
			if (recent[j % recent.length] == Long.MIN_VALUE) {
				continue;
			}

			shape.setColor(Color.rgba8888(lineColor.r, lineColor.g, lineColor.b, (lineColor.a * i / (1.0f * recent.length))));
			int x = (width - lineWidth) / 2 + (int) (Math.max(-center, Math.min((int) recent[j % recent.length], center)) * judgeWidthRate);
			if (drawDecay) {
				shape.fillRectangle(x, recent.length - i, lineWidth, i * 2);
			} else {
				shape.fillRectangle(x, 0, lineWidth, recent.length * 2);
			}
		}

		if (shapetex == null) {
			shapetex = new TextureRegion(new Texture(shape));
		} else {
			shapetex.getTexture().draw(shape, 0, 0);
		}

		draw(sprite, backtex, r.x, r.y, r.width, r.height, state);
		draw(sprite, shapetex, r.x, r.y, r.width, r.height, state);
	}

	private int[][] getJudgeArea(PlayerResource resource) {
		BMSModel model = resource.getBMSModel();
		JudgeProperty rule = BMSPlayerRule.getBMSPlayerRule(model.getMode()).judge;

		final int judgerank = model.getJudgerank();
		final int judgeWindowRate = resource.getPlayerConfig().getJudgewindowrate();
		int constraint = 2;
		for (CourseData.CourseDataConstraint mode : resource.getConstraint()) {
			if (mode == CourseData.CourseDataConstraint.NO_GREAT) {
				constraint = 0;
			} else if (mode == CourseData.CourseDataConstraint.NO_GOOD) {
				constraint = 1;
			}
		}

		return rule.getNoteJudge(judgerank, judgeWindowRate, constraint,
				model.getMode() == Mode.POPN_9K && !BMSPlayerRule.isSevenToNine());
	}

	@Override
	public void dispose() {
		Optional.ofNullable(backtex.getTexture()).ifPresent(Texture::dispose);
		Optional.ofNullable(shapetex.getTexture()).ifPresent(Texture::dispose);
		Optional.ofNullable(shape).ifPresent(Pixmap::dispose);
	}

	private String colorStringValidation(String cs) {
		String s = cs.replaceAll("[^0-9a-fA-F]", "");
		return s.length() == 0 ? "FF0000FF" : s.substring(0, s.length() > 8 ? 8 : s.length());
	}
}
