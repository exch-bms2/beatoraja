package bms.player.beatoraja.skin;

import bms.model.*;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.skin.SkinObject;

import java.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * BPM�렓燁삠겗�궛�꺀�깢
 *
 * @author exch
 */
public class SkinBPMGraph extends SkinObject implements SkinObserver{

	/**
	 * �궛�꺀�깢�깇�궚�궧�긽�깵
	 */
	private TextureRegion shapetex;
	private long time;
	private MainState state;
	private BMSModel model;
	private SongData current;

	/**
	 * �궟�꺖�궦�룒�뵽�굮若뚥틙�걲�굥�겲�겎�겗�셽�뼋(ms)
	 */
	private int delay = 0;
	/**
	 * �궛�꺀�깢渶싥겗鸚ゃ걬
	 */
	private int lineWidth = 2;

	private Color mainLineColor = Color.valueOf("00ff00");			//渶�
	private Color minLineColor = Color.valueOf("0000ff");			//�쓵
	private Color maxLineColor = Color.valueOf("ff0000");			//壅�
	private Color otherLineColor = Color.valueOf("ffff00");			//容�
	private Color stopLineColor = Color.valueOf("ff00ff");			//榮�
	private Color transitionLineColor = Color.valueOf("7f7f7f");	//�겙

	private double minValue = 1d/8;
	private double maxValue = 8;
	private double minValueLog = Math.log10(minValue);
	private double maxValueLog = Math.log10(maxValue);

	public SkinBPMGraph(int delay, int lineWidth, String mainBPMColor, String minBPMColor, String maxBPMColor, String otherBPMColor, String stopLineColor, String transitionLineColor) {
		if(delay > 0) this.delay = delay;
		if(lineWidth > 0) this.lineWidth = lineWidth;
		String mainBPMColorString = mainBPMColor.replaceAll("[^0-9a-fA-F]", "").substring(0, mainBPMColor.replaceAll("[^0-9a-fA-F]", "").length() > 6 ? 6 : mainBPMColor.replaceAll("[^0-9a-fA-F]", "").length());
		String minBPMColorString = minBPMColor.replaceAll("[^0-9a-fA-F]", "").substring(0, minBPMColor.replaceAll("[^0-9a-fA-F]", "").length() > 6 ? 6 : minBPMColor.replaceAll("[^0-9a-fA-F]", "").length());
		String maxBPMColorString = maxBPMColor.replaceAll("[^0-9a-fA-F]", "").substring(0, maxBPMColor.replaceAll("[^0-9a-fA-F]", "").length() > 6 ? 6 : maxBPMColor.replaceAll("[^0-9a-fA-F]", "").length());
		String otherBPMColorString = otherBPMColor.replaceAll("[^0-9a-fA-F]", "").substring(0, otherBPMColor.replaceAll("[^0-9a-fA-F]", "").length() > 6 ? 6 : otherBPMColor.replaceAll("[^0-9a-fA-F]", "").length());
		String stopLineColorString = stopLineColor.replaceAll("[^0-9a-fA-F]", "").substring(0, stopLineColor.replaceAll("[^0-9a-fA-F]", "").length() > 6 ? 6 : stopLineColor.replaceAll("[^0-9a-fA-F]", "").length());
		String transitionLineColorString = transitionLineColor.replaceAll("[^0-9a-fA-F]", "").substring(0, transitionLineColor.replaceAll("[^0-9a-fA-F]", "").length() > 6 ? 6 : transitionLineColor.replaceAll("[^0-9a-fA-F]", "").length());
		if(mainBPMColorString.length() > 0) {
			mainLineColor = Color.valueOf(mainBPMColorString);
		}
		if(minBPMColorString.length() > 0) {
			minLineColor = Color.valueOf(minBPMColorString);
		}
		if(maxBPMColorString.length() > 0) {
			maxLineColor = Color.valueOf(maxBPMColorString);
		}
		if(otherBPMColorString.length() > 0) {
			otherLineColor = Color.valueOf(otherBPMColorString);
		}
		if(stopLineColorString.length() > 0) {
			this.stopLineColor = Color.valueOf(stopLineColorString);
		}
		if(transitionLineColorString.length() > 0) {
			this.transitionLineColor = Color.valueOf(transitionLineColorString);
		}
	}

	@Override
	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		final SongData song = state.main.getPlayerResource().getSongdata();
		final BMSModel model = song != null ? song.getBMSModel() : null;

		this.time = time;
		this.state = state;

		if(current == null || song != current || (this.model == null && model != null) || shapetex == null) {
			current = song;
			this.model = model;
			updateGraph(model);
		}

		Rectangle graph = getDestination(time, state);
		if (graph == null) {
			return;
		}
		final float render = time >= delay ? 1.0f : (float) time / delay;
		shapetex.setRegionWidth((int) (shapetex.getTexture().getWidth() * render));
		draw(sprite, shapetex, graph.x, graph.y + graph.height, (int)(graph.width * render), -graph.height, state);
	}

	private void updateGraph(BMSModel model) {
		Rectangle graph = getDestination(time, state);
		Pixmap shape;
		if (model == null || graph == null) {
			shape = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
			shapetex = new TextureRegion(new Texture(shape));
		} else {
			int width = (int) Math.abs(graph.width);
			int height = (int) Math.abs(graph.height);
			shape = new Pixmap((int) width, (int) height, Pixmap.Format.RGBA8888);

			double mainBPM = model.getBpm();
			Map<Double, Integer> bpmNoteCountMap = new HashMap<Double, Integer>();
			List<Double> BPMList = new ArrayList<Double>();
			List<Integer> BPMTimeList = new ArrayList<Integer>();
			double nowBPM = model.getBpm();
			BPMList.add(model.getBpm());
			BPMTimeList.add(0);
			for (TimeLine tl : model.getAllTimeLines()) {
				Integer count = bpmNoteCountMap.get(tl.getBPM());
				if (count == null) {
					count = 0;
				}
				bpmNoteCountMap.put(tl.getBPM(), count + tl.getTotalNotes());
				if(tl.getStop() > 0) {
					BPMList.add(0d);
					BPMTimeList.add(tl.getTime());
					nowBPM = 0;
				}
				if(nowBPM != tl.getBPM()) {
					BPMList.add(tl.getBPM());
					BPMTimeList.add(tl.getTime() + tl.getStop());
					nowBPM = tl.getBPM();
				}
			}
			int maxcount = 0;
			for (double bpm : bpmNoteCountMap.keySet()) {
				if (bpmNoteCountMap.get(bpm) > maxcount) {
					maxcount = bpmNoteCountMap.get(bpm);
					mainBPM = bpm;
				}
			}
			double minBPM = model.getMinBPM();
			double maxBPM = model.getMaxBPM();
			int lastTime = model.getLastTime() + 1000;

			// �궛�꺀�깢�룒�뵽
			int x1,x2,y1,y2;
			for (int i = 1; i < BPMList.size(); i++) {
				//潁�渶�
				x1 = (int) (width * BPMTimeList.get(i) / lastTime);
				y1 = (int) ((Math.log10(Math.min(Math.max((BPMList.get(i-1) / mainBPM),minValue),maxValue)) - minValueLog) / (maxValueLog-minValueLog) * (height - lineWidth));
				x2 = x1;
				y2 = (int) ((Math.log10(Math.min(Math.max((BPMList.get(i) / mainBPM),minValue),maxValue)) - minValueLog) / (maxValueLog-minValueLog) * (height - lineWidth));
				if(Math.abs(y2 - y1) - lineWidth > 0) {
					shape.setColor(transitionLineColor);
					shape.fillRectangle(x1, Math.min(y1, y2) + lineWidth, lineWidth, Math.abs(y2 - y1) - lineWidth);
				}
				//與ょ퇉
				x1 = (int) (width * BPMTimeList.get(i-1) / lastTime);
				y1 = (int) ((Math.log10(Math.min(Math.max((BPMList.get(i-1) / mainBPM),minValue),maxValue)) - minValueLog) / (maxValueLog-minValueLog) * (height - lineWidth));
				x2 = (int) (width * BPMTimeList.get(i) / lastTime);
				y2 = y1;
				Color lineColor = otherLineColor;
				if(BPMList.get(i-1) == mainBPM) lineColor = mainLineColor;
				else if(BPMList.get(i-1) == minBPM) lineColor = minLineColor;
				else if(BPMList.get(i-1) == maxBPM) lineColor = maxLineColor;
				else if(BPMList.get(i-1) == 0) lineColor = stopLineColor;
				shape.setColor(lineColor);
				shape.fillRectangle(x1, y2, x2 - x1 + lineWidth, lineWidth);
			}
			//與ょ퇉
			x1 = (int) (width * BPMTimeList.get(BPMTimeList.size()-1) / lastTime);
			y1 = (int) ((Math.log10(Math.min(Math.max((BPMList.get(BPMTimeList.size()-1) / mainBPM),minValue),maxValue)) - minValueLog) / (maxValueLog-minValueLog) * (height - lineWidth));
			x2 = (int) width;
			y2 = y1;
			Color lineColor = otherLineColor;
			if(BPMList.get(BPMList.size()-1) == mainBPM) lineColor = mainLineColor;
			else if(BPMList.get(BPMList.size()-1) == minBPM) lineColor = minLineColor;
			else if(BPMList.get(BPMList.size()-1) == maxBPM) lineColor = maxLineColor;
			else if(BPMList.get(BPMList.size()-1) == 0) lineColor = stopLineColor;
			shape.setColor(lineColor);
			shape.fillRectangle(x1, y2, x2 - x1 + lineWidth, lineWidth);
			if (shapetex != null) {
				shapetex.getTexture().dispose();
				shapetex = null;
			}
			shapetex = new TextureRegion(new Texture(shape));
			shape.dispose();
		}

	}

	@Override
	public void dispose() {
		if (shapetex != null) {
			shapetex.getTexture().dispose();
			shapetex = null;
		}
	}

}
