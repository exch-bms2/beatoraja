package bms.player.beatoraja.skin;

import bms.model.BMSModel;
import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.BMSPlayerRule;
import bms.player.beatoraja.play.JudgeProperty;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import java.util.Optional;

/**
 * Early/Late HitError Visualization with EMA
 *
 * @author hadronyche
 */
public class SkinHitErrorVisualizer extends SkinObject {

	private TextureRegion shapetex = null;
	private Pixmap shape = null;

	private Color[] JColor;

	private final Color lineColor;
	private final Color centerColor;
	private final Color emaColor;

	private final int lineWidth;
	private final int width;
	private final int center;
	private final int windowLength;
	private final int emaMode;
	private final float judgeWidthRate;
	private final boolean hiterrorMode;
	private final boolean colorMode;
	private final boolean drawDecay;

	private BMSModel model;
	private int[][] judgeArea;

	private int currentindex = -1;

	private int index;
	private long[] recent;
	private Long ema = null;
	private float alpha;

	public SkinHitErrorVisualizer(int width, int judgeWidthMillis, int lineWidth, int colorMode,
								  int hiterrorMode, int emaMode, String lineColor, String centerColor,
								  String PGColor, String GRColor, String GDColor, String BDColor, String PRColor,
								  String emaColor, float alpha, int windowLength, int transparent, int drawDecay) {

		this.lineWidth = MathUtils.clamp(lineWidth, 1, 4);
		this.width = width;
		this.center = judgeWidthMillis;
		this.alpha = alpha;
		this.ema = 0L;
		this.emaMode = emaMode;
		this.windowLength = windowLength < 100 ? windowLength : 100;
		this.judgeWidthRate = width / (float) (judgeWidthMillis * 2 + 1);
		this.lineColor = Color.valueOf(colorStringValidation(lineColor));
		this.centerColor = Color.valueOf(colorStringValidation(centerColor));
		this.emaColor = Color.valueOf(colorStringValidation(emaColor));
		JColor = new Color[] {
				Color.valueOf(colorStringValidation(PGColor)),
				Color.valueOf(colorStringValidation(GRColor)),
				Color.valueOf(colorStringValidation(GDColor)),
				Color.valueOf(colorStringValidation(BDColor)),
				transparent == 1 ? Color.CLEAR : Color.valueOf(PRColor)
		};
		this.hiterrorMode = hiterrorMode == 1 ? true : false;
		this.colorMode    = colorMode == 1 ? true : false;
		this.drawDecay    = drawDecay == 1 ? true : false;
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

	private void updateEMA (long value) {
		ema = ema + (long) (alpha * (value - ema));
	}
	
	public void draw(SkinObjectRenderer sprite) {
		if (shape == null) {
			shape = new Pixmap(width, windowLength * 2, Pixmap.Format.RGBA8888);
		}

		// Clear canvas
		shape.setColor(Color.CLEAR);
		shape.fill();

		// Individual note hiterror
		if (hiterrorMode) {
			for (int i = windowLength; i > 0; i--) {
				int j = -windowLength + i + index;
				int cycle = ((j % recent.length + recent.length) % recent.length);

				if (recent[cycle] == Long.MIN_VALUE) {
					continue;
				}
				// Judge color or single color
				if (colorMode) {
					long judge = recent[cycle];
					if (judge > judgeArea[0][0] && judge < judgeArea[0][1]) {
						shape.setColor(JColor[0]);
					} else if (judge > judgeArea[1][0] && judge < judgeArea[1][1]) {
						shape.setColor(JColor[1]);
					} else if (judge > judgeArea[2][0] && judge < judgeArea[2][1]) {
						shape.setColor(JColor[2]);
					} else if (judge > judgeArea[3][0] && judge < judgeArea[3][1]) {
						shape.setColor(JColor[3]);
					} else {//(judge > judgeArea[4][0] && judge < judgeArea[4][1]) {
						shape.setColor(JColor[4]);
					}
				} else {
					shape.setColor(
							Color.rgba8888(lineColor.r, lineColor.g, lineColor.b, (lineColor.a * i / (1.0f * windowLength / 2))));
				}
				int x = (width - lineWidth) / 2
						+ (int) (MathUtils.clamp(recent[cycle], -center, center) * -judgeWidthRate);
				// Draw decay shortens older hiterror lines
				if (drawDecay) {
					shape.fillRectangle(x, windowLength - i, lineWidth, i * 2);
				} else {
					shape.fillRectangle(x, 0, lineWidth, recent.length * 2);
				}
			}
		}

		// Centre line
		shape.setColor(centerColor);
		shape.fillRectangle((width - lineWidth) / 2, 0, lineWidth, windowLength * 2);

		if (emaMode != 0) {
			long last = recent[index];
			// Ignore misses
			if (last != Long.MIN_VALUE && (last > judgeArea[3][0] && last < judgeArea[3][1])) {
				updateEMA(last);
			}
			int x = (width - lineWidth) / 2
					+ (int) (MathUtils.clamp(ema.intValue(), -center, center) * -judgeWidthRate);
			int w = (int) (width * 0.01);

			// Line and/or Triangle style
			shape.setColor(emaColor);
			if (emaMode == 1 || emaMode == 3) {
				shape.fillRectangle(x, 0, lineWidth, windowLength * 2);
			}
			if (emaMode == 2 || emaMode == 3) {
				x = x+(lineWidth/2);
				if(w%2!=0) {
					w++;
				}
				shape.fillTriangle(x, (windowLength * 2) / 3, x + w, 0, x - w, 0);
			}
		}

		if (shapetex == null) {
			shapetex = new TextureRegion(new Texture(shape));
		} else {
			shapetex.getTexture().draw(shape, 0, 0);
		}

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
