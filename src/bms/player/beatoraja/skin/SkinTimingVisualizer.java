package bms.player.beatoraja.skin;

import java.util.Optional;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;

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

	private Color[] JColor;
	/**
	 * 判定履歴表示用ラインの色
	 */
	private final Color lineColor;
	/**
	 * センターラインの色
	 */
	private final Color centerColor;

	/**
	 * 判定履歴表示用ラインの幅
	 */
	private final int lineWidth;
	private final int width;
	private final int center;
	private final float judgeWidthRate;
	private final boolean drawDecay;
	
	private BMSModel model;
	private int[][] judgeArea;

	private int currentindex = -1;
	
	private int index;
	private long[] recent;
	/**
	 *
	 * @param width スキン描画幅
	 * @param judgeWidthMillis 判定描画幅
	 * @param lineWidth 入力線の幅
	 * @param transparent 1:POOR判定を透過する
	 * @param drawDecay 1:線を減衰させる
	 */
	public SkinTimingVisualizer(int width, int judgeWidthMillis, int lineWidth,
			String lineColor, String centerColor, String PGColor, String GRColor, String GDColor, String BDColor,
			String PRColor,
			int transparent, int drawDecay) {

		this.lineWidth = MathUtils.clamp(lineWidth, 1, 4);
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
				transparent == 1 ? Color.CLEAR : Color.valueOf(PRColor)
		};
		this.drawDecay = drawDecay == 1 ? true : false;
	}

	public void prepare(long time, MainState state) {
		if(!(state instanceof BMSPlayer)) {
			draw = false;
			return;
		}
		super.prepare(time, state);
		final PlayerResource resource = state.main.getPlayerResource();
		if(resource.getBMSModel() != model) {
			model = resource.getBMSModel();
			judgeArea = getJudgeArea(resource);			
		}
		
		index = ((BMSPlayer)state).getJudgeManager().getRecentJudgesIndex();
		recent = ((BMSPlayer)state).getJudgeManager().getRecentJudges();
	}
	
	public void draw(SkinObjectRenderer sprite) {
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
				int x1 = center + MathUtils.clamp(judgeArea[i][0], -center, center);
				int x2 = center + MathUtils.clamp(judgeArea[i][1], -center, center) + 1;

				if (beforex1 > x1) {
					shape.fillRectangle(x1, 0, Math.abs(x1 - beforex1), 1);
					beforex1 = x1;
				}

				if (x2 > beforex2) {
					shape.fillRectangle(beforex2, 0, Math.abs(x2 - beforex2), 1);
					beforex2 = x2;
				}
			}
			
			shape.setColor(0f, 0f, 0f, 0.25f);
			for(int x = center % 10;x < pwidth;x += 10) {
				shape.drawLine(x, 0, x, 1);
			}

			backtex = new TextureRegion(new Texture(shape));
			shape.dispose();
			shape = null;
		}

		if (shape == null) {
			shape = new Pixmap(width, recent.length * 2, Pixmap.Format.RGBA8888);
		}

		if(currentindex != index) {
			currentindex = index;
			// 前景テクスチャ 透明色でフィルして初期化
			shape.setColor(Color.CLEAR);
			shape.fill();

			for (int i = 0; i < recent.length; i++) {
				int j = i + index + 1;
				if (recent[j % recent.length] == Long.MIN_VALUE) {
					continue;
				}

				shape.setColor(
						Color.rgba8888(lineColor.r, lineColor.g, lineColor.b, (lineColor.a * i / (1.0f * recent.length))));
				int x = (width - lineWidth) / 2
						+ (int) (MathUtils.clamp(recent[j % recent.length], -center, center) * judgeWidthRate);
				if (drawDecay) {
					shape.fillRectangle(x, recent.length - i, lineWidth, i * 2);
				} else {
					shape.fillRectangle(x, 0, lineWidth, recent.length * 2);
				}
			}
		}

		if (shapetex == null) {
			shapetex = new TextureRegion(new Texture(shape));
		} else {
			shapetex.getTexture().draw(shape, 0, 0);
		}

		draw(sprite, backtex);
		draw(sprite, shapetex);
	}

	static int[][] getJudgeArea(PlayerResource resource) {
		BMSModel model = resource.getBMSModel();
		JudgeProperty rule = BMSPlayerRule.getBMSPlayerRule(resource.getOriginalMode()).judge;

		final int judgerank = model.getJudgerank();
		final PlayerConfig config = resource.getPlayerConfig();
		final int[] judgeWindowRate = config.isCustomJudge()
				? new int[]{config.getKeyJudgeWindowRatePerfectGreat(), config.getKeyJudgeWindowRateGreat(), config.getKeyJudgeWindowRateGood()}
				: new int[]{100, 100, 100};
				
		for (CourseData.CourseDataConstraint mode : resource.getConstraint()) {
			if (mode == CourseData.CourseDataConstraint.NO_GREAT) {
				judgeWindowRate[1] = judgeWindowRate[2] = 0;
			} else if (mode == CourseData.CourseDataConstraint.NO_GOOD) {
				judgeWindowRate[2] = 0;
			}
		}

		return rule.getNoteJudge(judgerank, judgeWindowRate);
	}

	@Override
	public void dispose() {
		Optional.ofNullable(backtex).ifPresent(t -> t.getTexture().dispose());
		Optional.ofNullable(shapetex).ifPresent(t -> t.getTexture().dispose());
		Optional.ofNullable(shape).ifPresent(Pixmap::dispose);
	}

	/**
	 * @return 文字列が16進以外の情報を持つか、長さ6未満の場合 異常を示す不透明赤
	 */
	static String colorStringValidation(String cs) {
		if (cs.replaceAll("[^0-9a-fA-F]", "").length() != cs.length() || cs.length() < 6) {
			return "FF0000FF";
		} else {
			return cs;
		}
	}
}
