package bms.player.beatoraja.skin;

import java.util.Optional;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.BMSPlayerRule;
import bms.player.beatoraja.play.JudgeProperty;
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

	private static final Color[] JColor = new Color[] {
			Color.valueOf("000088"),
			Color.valueOf("008800"),
			Color.valueOf("666600"),
			Color.valueOf("880000"),
			Color.valueOf("000000") };

	private static final Color CLEAR = Color.valueOf("00000000");

	private final int center;
	private final int range;
	private final boolean drawCenter;
	private final boolean drawDecay;

	public SkinTimingVisualizer() {
		this(401, 1, 1);
	}

	public SkinTimingVisualizer(int width, int drawCenter, int drawDecay) {
		if (width > 0) {
			this.center = width / 2;
			this.range = width;
		} else {
			this.center = 200;
			this.range = 401;
		}
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

		if (backtex == null) {
			shape = new Pixmap(range, 1, Pixmap.Format.RGBA8888);
			shape.setColor(Color.BLACK);
			shape.fill();
			for (int i = JColor.length - 1; i >= 0; i--) {
				shape.setColor(JColor[i]);
				int x = center + Math.max(-center, Math.min(judgeArea[i][0], center));
				int width = Math.min(range - x, Math.abs(judgeArea[i][0]) + Math.abs(judgeArea[i][1]) + 1);
				shape.fillRectangle(x, 0, width, 1);
			}
			if (drawCenter) {
				shape.setColor(Color.WHITE);
				shape.fillRectangle(center, 0, Math.min(range, 2), 1);
			}

			backtex = new TextureRegion(new Texture(shape));
			shape.dispose();
			shape = null;
		}

		int index = state.getJudgeManager().getRecentJudgesIndex();
		long[] recent = state.getJudgeManager().getRecentJudges();

		if (shape == null) {
			shape = new Pixmap(range, recent.length * 2, Pixmap.Format.RGBA8888);
		}
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

}
