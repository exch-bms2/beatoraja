package bms.model;

import java.io.File;
import java.io.IOException;
import java.util.*;

import bms.model.bmson.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * bmsonデコーダー
 * 
 * @author exch
 */
public class BMSONDecoder {
	
	private int lntype;
	
	public BMSONDecoder(int lntype) {
		this.lntype = lntype;
	}
	
	public BMSModel decode(File f) {
		BMSModel model = new BMSModel();
		try {
			ObjectMapper mapper = new ObjectMapper();
			Bmson bmson = mapper.readValue(f, Bmson.class);
			model.setTitle(bmson.info.title);
			model.setArtist(bmson.info.artist);
			model.setGenre(bmson.info.genre);
			model.setJudgerank(bmson.info.judgeRank);
			model.setTotal(bmson.info.total);
			model.setBpm(bmson.info.initBPM);
			model.setPlaylevel(String.valueOf(bmson.info.level));
			model.setUseKeys(7);
			model.setLntype(lntype);
			double nowbpm = model.getBpm();
			// TODO bpmNotes処理
			// lines処理(小節線)
			for(BarLine bl : bmson.lines) {
				model.getTimeLine(bl.y / 960f, (int) ((1000.0 * 60 * 4 * bl.y) / (nowbpm * 960))).setSectionLine(true);
			}
			// TODO stopNotes処理
			List<String> wavmap = new ArrayList<String>();
			int id = 0;
			for(SoundChannel sc : bmson.soundChannel) {
				wavmap.add(sc.name);
				for(bms.model.bmson.Note n : sc.notes) {
					if(n.x == 0) {
						model.getTimeLine(n.y / 960f, (int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960))).addBackGroundNote(new NormalNote(id));						
					} else {
						if(n.l > 0) {
							// ロングノート
							TimeLine start = model.getTimeLine(n.y / 960f,(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)));
							LongNote ln = new LongNote(id, start);
							start.addNote(n.x - 1, ln);
							TimeLine end = model.getTimeLine(n.y / 960f,(int) ((1000.0 * 60 * 4 * (n.y + n.l)) / (nowbpm * 960)));
							ln.setEnd(end);
							end.addNote(n.x - 1, ln);
						} else {
							model.getTimeLine(n.y / 960f,(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960))).addNote(n.x - 1, new NormalNote(id));							
						}
					}
				}
				id++;
			}
			model.setWavList(wavmap.toArray(new String[0]));
			
		} catch (JsonParseException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return model;
	}
}
