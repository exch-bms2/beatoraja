package bms.player.beatoraja;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

import bms.model.BMSModel;

/**
 * スコアグラフ描画用クラス
 * 
 * @author exch
 */
public class ScoreGraphRenderer {
	/**
	 * スコアグラフ最大値
	 */
	private int max;
	/**
	 * ベストスコア
	 */
	private int best;
	/**
	 * ライバルスコア
	 */
	private int rival;

	public ScoreGraphRenderer(BMSModel model, int best, int rival) {
		max = model.getTotalNotes()* 2;
		this.best = best;
		this.rival = rival;
	}

	public void drawGraph(PlaySkin skin, SpriteBatch sprite, BitmapFont font, ShapeRenderer shape, JudgeManager judge) {
		Rectangle graph = skin.getGraphregion();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(graph.x, graph.y, graph.width, graph.height);
		shape.end();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.WHITE);
		shape.rect(graph.x, graph.y, graph.width, graph.height);
		for(int i = 1;i < 20;i++) {
			float y = graph.y + graph.height * i / 20;
			if(i % 2 == 0){
				if(i >= 14) {
					shape.setColor(Color.LIGHT_GRAY);
					shape.line(graph.x, y, graph.x + graph.width, y);
				} else {
					shape.setColor(Color.GRAY);
					shape.line(graph.x, y, graph.x + graph.width / 6, y);					
				}
			} else {
				shape.setColor(Color.DARK_GRAY);
				shape.line(graph.x, y, graph.x + graph.width / 12, y);				
			}
		}
		shape.end();

		int now = (judge.getJudgeCount(0)) * 2 + (judge.getJudgeCount(1));
		int notes = judge.getJudgeCount() - judge.getJudgeCount(5);

		float rate = 0;
		if (notes != 0) {
			rate = (float) notes * 2 / max;
		}

		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLUE);
		shape.rect(graph.x + 3, graph.y, (graph.width - 9) / 3, graph.height
				* now / max);
		shape.end();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.GREEN);
		shape.rect(graph.x + graph.width / 3 + 3, graph.y,
				(graph.width - 9) / 3, graph.height * best / max);
		shape.end();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.GREEN);
		shape.rect(graph.x + graph.width / 3 + 3, graph.y,
				(graph.width - 9) / 3, graph.height * best * rate / max);
		shape.end();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.RED);
		shape.rect(graph.x + graph.width * 2 / 3 + 3, graph.y,
				(graph.width - 9) / 3, graph.height * rival / max);
		shape.end();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.RED);
		shape.rect(graph.x + graph.width * 2 / 3 + 3, graph.y,
				(graph.width - 9) / 3, graph.height * rival * rate / max);
		shape.end();
		
		sprite.begin();
		font.setColor(Color.valueOf("bbbbff"));
		font.setColor(Color.WHITE);
		font.draw(sprite, String.format("%5.1f", notes != 0 ? ((float)now * 100 / (notes * 2)) : 0f) + "%", graph.x + 20, graph.y - 5);
		font.draw(sprite, "P:" + String.format("%5d", now) , graph.x + 5, graph.y - 23);
		font.setColor(Color.valueOf("bbffbb"));
		font.draw(sprite, "B:" + String.format("%5d", best) , graph.x + 5, graph.y - 41);
		font.setColor(Color.valueOf("ffbbbb"));
		font.draw(sprite, "R:" + String.format("%5d", rival) , graph.x + 5, graph.y - 59);
		sprite.end();
	}

}
