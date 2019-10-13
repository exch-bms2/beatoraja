package bms.player.beatoraja.skin;

import bms.model.*;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongInformation;

import java.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * BPM推移のグラフ
 *
 * @author niente1899
 */
public class SkinBPMGraph extends SkinObject {

	/**
	 * グラフテクスチャ
	 */
	private TextureRegion shapetex;
	private long time;
	private MainState state;
	private BMSModel model;
	private SongData current;

	/**
	 * ゲージ描画を完了するまでの時間(ms)
	 */
	private int delay = 0;
	/**
	 * グラフ線の太さ
	 */
	private int lineWidth = 2;

	private Color mainLineColor = Color.valueOf("00ff00");			//緑
	private Color minLineColor = Color.valueOf("0000ff");			//青
	private Color maxLineColor = Color.valueOf("ff0000");			//赤
	private Color otherLineColor = Color.valueOf("ffff00");			//黄
	private Color stopLineColor = Color.valueOf("ff00ff");			//紫
	private Color transitionLineColor = Color.valueOf("7f7f7f");	//灰

	private double[][] data = new double[0][2];
	private double mainbpm;
	private double minbpm;
	private double maxbpm;

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

	public void prepare(long time, MainState state) {
		this.time = time;
		this.state = state;
		super.prepare(time, state);
	}

	public void draw(SkinObjectRenderer sprite) {
		final SongData song = state.main.getPlayerResource().getSongdata();
		final BMSModel model = song != null ? song.getBMSModel() : null;
		
		if(current == null || song != current || (this.model == null && model != null) || shapetex == null) {
			current = song;
			this.model = model;
			if(song != null && song.getInformation() != null) {
				updateGraph(song.getInformation());
			} else {
				updateGraph(model);
			}
		}

		final float render = time >= delay ? 1.0f : (float) time / delay;
		shapetex.setRegionWidth((int) (shapetex.getTexture().getWidth() * render));
		draw(sprite, shapetex, region.x, region.y + region.height, (int)(region.width * render), -region.height);
	}

	private void updateGraph(SongInformation info) {
		data = info.getSpeedchangeValues();
		minbpm = Double.MAX_VALUE;
		maxbpm = Double.MIN_VALUE;
		for(double[] d : data) {
			if(d[0] > 0) {
				minbpm = Math.min(d[0], minbpm);				
			}
			maxbpm = Math.min(d[0], maxbpm);
		}
		this.mainbpm = info.getMainbpm();
		
		updateTexture();
	}

	private void updateGraph(BMSModel model) {
		if (model == null) {
			data = new double[0][2];
		} else {
			List<double[]> speedList = new ArrayList<double[]>();
			Map<Double, Integer> bpmNoteCountMap = new HashMap<Double, Integer>();
			double nowSpeed = model.getBpm();
			speedList.add(new double[] {nowSpeed, 0.0});
			final TimeLine[] tls = model.getAllTimeLines();
			for (TimeLine tl : tls) {
				int notecount = bpmNoteCountMap.containsKey(tl.getBPM()) ? bpmNoteCountMap.get(tl.getBPM()) : 0;
				bpmNoteCountMap.put(tl.getBPM(), notecount + tl.getTotalNotes());

				if(tl.getStop() > 0) {
					if(nowSpeed != 0) {
						nowSpeed = 0;					
						speedList.add(new double[] {nowSpeed, tl.getTime()});
					}
				} else if(nowSpeed != tl.getBPM() * tl.getScroll()) {
					nowSpeed = tl.getBPM() * tl.getScroll();
					speedList.add(new double[] {nowSpeed, tl.getTime()});
				}
			}
			
			int maxcount = 0;
			for (double bpm : bpmNoteCountMap.keySet()) {
				if (bpmNoteCountMap.get(bpm) > maxcount) {
					maxcount = bpmNoteCountMap.get(bpm);
					mainbpm = bpm;
				}
			}
			if(speedList.get(speedList.size() - 1)[1] != tls[tls.length - 1].getTime()) {
				speedList.add(new double[] {nowSpeed, tls[tls.length - 1].getTime()});			
			}
			
			data = speedList.toArray(new double[speedList.size()][]);
			minbpm = model.getMinBPM();
			maxbpm = model.getMaxBPM();	
		}
		updateTexture();
	}
	
	private void updateTexture() {
		Pixmap shape;
		if (data.length < 2) {
			shape = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
			shapetex = new TextureRegion(new Texture(shape));
		} else {
			final int width = (int) Math.abs(region.width);
			final int height = (int) Math.abs(region.height);
			shape = new Pixmap(width, height, Pixmap.Format.RGBA8888);

			int lastTime = (int) (data[data.length - 1][1] + 1000);

			// グラフ描画
			int x1,x2,y1,y2;
			for (int i = 1; i < data.length; i++) {
				//縦線
				x1 = (int) (width * data[i][1] / lastTime);
				y1 = (int) ((Math.log10(Math.min(Math.max((data[i - 1][0] / mainbpm),minValue),maxValue)) - minValueLog) / (maxValueLog-minValueLog) * (height - lineWidth));
				x2 = x1;
				y2 = (int) ((Math.log10(Math.min(Math.max((data[i][0] / mainbpm),minValue),maxValue)) - minValueLog) / (maxValueLog-minValueLog) * (height - lineWidth));
				if(Math.abs(y2 - y1) - lineWidth > 0) {
					shape.setColor(transitionLineColor);
					shape.fillRectangle(x1, Math.min(y1, y2) + lineWidth, lineWidth, Math.abs(y2 - y1) - lineWidth);
				}
				//横線
				x1 = (int) (width * data[i - 1][1] / lastTime);
				y1 = (int) ((Math.log10(Math.min(Math.max((data[i - 1][0] / mainbpm),minValue),maxValue)) - minValueLog) / (maxValueLog-minValueLog) * (height - lineWidth));
				x2 = (int) (width * data[i][1] / lastTime);
				y2 = y1;
				Color lineColor = otherLineColor;
				if(data[i - 1][0] == mainbpm) lineColor = mainLineColor;
				else if(data[i - 1][0] == minbpm) lineColor = minLineColor;
				else if(data[i - 1][0] == maxbpm) lineColor = maxLineColor;
				else if(data[i - 1][0] <= 0) lineColor = stopLineColor;
				shape.setColor(lineColor);
				shape.fillRectangle(x1, y2, x2 - x1 + lineWidth, lineWidth);
			}
			//横線
			x1 = (int) (width * data[data.length - 1][1] / lastTime);
			y1 = (int) ((Math.log10(Math.min(Math.max((data[data.length - 1][0] / mainbpm),minValue),maxValue)) - minValueLog) / (maxValueLog-minValueLog) * (height - lineWidth));
			x2 = (int) width;
			y2 = y1;
			Color lineColor = otherLineColor;
			if(data[data.length - 1][0] == mainbpm) lineColor = mainLineColor;
			else if(data[data.length - 1][0] == minbpm) lineColor = minLineColor;
			else if(data[data.length - 1][0] == maxbpm) lineColor = maxLineColor;
			else if(data[data.length - 1][0] <= 0) lineColor = stopLineColor;
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
