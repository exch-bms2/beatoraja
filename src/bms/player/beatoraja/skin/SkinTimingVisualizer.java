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

	// 判定色はスキン側で設定できるように
	private Color[] JColor;

	private static final Color CLEAR = Color.valueOf("00000000");

	// 線の幅をスキン側で設定できるように
	private final int lineWidth;
	private final int width;
	private final int judgeWidthMillis;
	private final int center;
	private final boolean drawCenter;
	private final boolean drawDecay;

	public SkinTimingVisualizer(int width) {
		this(width, 150, 1, "00FF00FF", "000088FF", "008800FF", "666600FF", "880000FF", "000000FF", 0, 1, 1);
	}

	/**
	 *
	 * @param lineWidth 入力線の幅
	 * @param width スキン描画幅(
	 * @param judgeWidth 判定描画幅
	 * @param Color AARRGGBB形式
	 * @param transparent 1:POOR判定を透過する
	 * @param drawCenter 1:判定の中央を描画する
	 * @param drawDecay 1:線を減衰させる
	 */
	public SkinTimingVisualizer(int width, int judgeWidthMillis, int lineWidth,
			String LineColor, String PGColor, String GRColor, String GDColor, String BDColor, String PRColor,
			int transparent, int drawCenter, int drawDecay) {


		this.lineWidth = lineWidth;
		this.width = width;
		this.center = judgeWidthMillis;
		this.judgeWidthMillis = judgeWidthMillis;

		JColor = new Color[] {
				Color.valueOf(colorStringValidation(PGColor)),
				Color.valueOf(colorStringValidation(GRColor)),
				Color.valueOf(colorStringValidation(GDColor)),
				Color.valueOf(colorStringValidation(BDColor)),
				transparent == 1 ? CLEAR : Color.valueOf(PRColor)
		};
		this.drawCenter = drawCenter == 1 ? true : false;
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
			int pwidth = judgeWidthMillis * 2 + 1;
			shape = new Pixmap(pwidth, 1, Pixmap.Format.RGBA8888);
			shape.setColor(CLEAR);
			shape.fill();
			for (int i = JColor.length - 1; i >= 0; i--) {
				shape.setColor(JColor[i]);
				int x = center + Math.max(-center, Math.min(judgeArea[i][0], center));
				int jwidth = Math.min(pwidth - x, Math.abs(judgeArea[i][0]) + Math.abs(judgeArea[i][1]) + 1);
				shape.fillRectangle(x, 0, jwidth, 1);
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

			shape.setColor(Color.rgba8888(0.0f, 1.0f, 0, (i / (1.0f * recent.length))));
			int x = center + Math.max(-center, Math.min((int) recent[j % recent.length], center));
			if (drawDecay) {
				shape.drawLine(x, recent.length - i, x, recent.length + i);
			} else {
				shape.drawLine(x, 0, x, recent.length * 2);
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
