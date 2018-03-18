package bms.player.beatoraja.skin;

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

	private TextureRegion backtex;
	private TextureRegion shapetex;

	private static final Color[] JColor = new Color[] {
			Color.valueOf("000088"),
			Color.valueOf("008800"),
			Color.valueOf("666600"),
			Color.valueOf("880000"),
			Color.valueOf("000000") };

	private final int center = 200;
	private final int range = center * 2 + 1;

	@Override
	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		if (state instanceof BMSPlayer) {
			draw(sprite, time, (BMSPlayer) state, getDestination(time, state));
		}

	}

	private void draw(SkinObjectRenderer sprite, long time, BMSPlayer state, Rectangle r) {
		if (backtex != null) {
			backtex.getTexture().dispose();
		}
		if (shapetex != null) {
			shapetex.getTexture().dispose();
		}

		if (r == null) {
			return;
		}

		PlayerResource resource = state.main.getPlayerResource();
		int index = state.getJudgeManager().getRecentJudgesIndex();
		long[] recent = state.getJudgeManager().getRecentJudges();
		int[][] judgeArea = getJudgeArea(resource);

		Pixmap shape = new Pixmap(range, 200, Pixmap.Format.RGBA8888);
		for (int i = JColor.length - 1; i >= 0; i--) {
			shape.setColor(JColor[i]);
			int x = center + Math.max(-center, Math.min(judgeArea[i][0], center));
			int width = Math.min(range - x, Math.abs(judgeArea[i][0]) + Math.abs(judgeArea[i][1]) + 1);
			shape.fillRectangle(x, 0, width, 200);
		}
		shape.setColor(Color.valueOf("FFFFFF"));
		shape.fillRectangle(0, 100, range, 1);
		shape.fillRectangle(center, 0, 2, 200);

		backtex = new TextureRegion(new Texture(shape));
		shape.dispose();

		shape = new Pixmap(range, 200, Pixmap.Format.RGBA8888);
		for (int i = 0; i < recent.length; i++) {
			int j = index + 1;
			if (recent[(i + j) % recent.length] == Long.MIN_VALUE) {
				continue;
			}
			shape.setColor(Color.rgba8888(0.0f, 1.0f, 0, (i / (1.0f * recent.length))));
			int x = center + Math.max(-center, Math.min((int) recent[(i + j) % recent.length], center));
			shape.fillRectangle(x, recent.length - i, 1, i * 2);
		}

		shapetex = new TextureRegion(new Texture(shape));
		shape.dispose();

		draw(sprite, backtex, r.x, r.y, r.width, r.height);
		draw(sprite, shapetex, r.x, r.y, r.width, r.height);
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
		if (shapetex != null) {
			shapetex.getTexture().dispose();
			backtex.getTexture().dispose();
			shapetex = null;
		}
	}

}
