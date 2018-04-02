package bms.player.test.GrooveGauge;

import static bms.player.beatoraja.skin.SkinProperty.OPTION_AUTOPLAYOFF;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_AUTOPLAYON;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_BACKBMP;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_BGAEXTEND;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_BGANORMAL;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_BGAOFF;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_BGAON;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_BPMCHANGE;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_COURSE_STAGE1;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_COURSE_STAGE2;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_COURSE_STAGE3;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_COURSE_STAGE4;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_COURSE_STAGE_FINAL;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_DIFFICULTY0;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_DIFFICULTY1;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_DIFFICULTY2;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_DIFFICULTY3;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_DIFFICULTY4;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_DIFFICULTY5;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_GHOST_A;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_GHOST_B;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_GHOST_C;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_GHOST_OFF;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_MODE_COURSE;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_MODE_EXPERT;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_MODE_GRADE;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_MODE_NONSTOP;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_NO_BACKBMP;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_NO_BPMCHANGE;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_NO_STAGEFILE;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_OFFLINE;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_ONLINE;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_SCOREGRAPHOFF;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_SCOREGRAPHON;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_STAGEFILE;
import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.badlogic.gdx.graphics.Pixmap;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.PlaySkin;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.skin.Skin;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

public class SkinTest extends MainLoader{
	Skin skin = new Skin(Resolution.HD,Resolution.HD,fixop);
	boolean usecim=false;
	 File imagefile = new File("C:/Users/rnwhd/Desktop/programing/git/beatoraja/skin/default/POMYU Chara/Off/dummy.chp");
	int type=0, color=0,side=0,dsttimer=0;
	int dstOp1=0,dstOp2=0,dstOp3=0,dstOffset=0;
	float dstx=0.0f,dsty=0.0f,dsth=0.0f,dstw=0.0f;
	
	private static final int[] fixop = {OPTION_STAGEFILE, OPTION_NO_STAGEFILE, OPTION_BACKBMP, OPTION_NO_BACKBMP,
			OPTION_AUTOPLAYON, OPTION_AUTOPLAYOFF, OPTION_BGAON, OPTION_BGAOFF,
			OPTION_BGANORMAL, OPTION_BGAEXTEND, OPTION_GHOST_OFF, OPTION_GHOST_A, OPTION_GHOST_B,
			OPTION_GHOST_C, OPTION_OFFLINE, OPTION_ONLINE,
			OPTION_SCOREGRAPHOFF, OPTION_SCOREGRAPHON,OPTION_DIFFICULTY0,OPTION_DIFFICULTY1
			,OPTION_DIFFICULTY2,OPTION_DIFFICULTY3,OPTION_DIFFICULTY4,OPTION_DIFFICULTY5,
			OPTION_NO_BPMCHANGE,OPTION_BPMCHANGE,
			OPTION_COURSE_STAGE1,OPTION_COURSE_STAGE2,OPTION_COURSE_STAGE3,OPTION_COURSE_STAGE4,OPTION_COURSE_STAGE_FINAL,
			OPTION_MODE_COURSE,OPTION_MODE_NONSTOP,OPTION_MODE_EXPERT,OPTION_MODE_GRADE};
	@Before
	public void setSkin(){
		skin = new Skin(Resolution.HD,Resolution.HD,fixop);
		usecim=false;
		
		type=0;
		color=0;
		side=0;
		dsttimer=0;
		dstOp1=0;
		dstOp2=0;
		dstOp3=0;
		dstOffset=0;
		dstx=0.0f;
		dsty=0.0f;
		dsth=0.0f;
		dstw=0.0f;
	}
	@Test
	public void typeBoundaryTest(){
		imagefile = new File("./skin/default/POMYU Chara/Off/dummy.chp");
		type = 16;
		assertTrue(skin.PMcharaLoader(usecim, imagefile, type, color, dstx, dsty, dstw, dsth, side, dsttimer, dstOp1, dstOp2, dstOp3, dstOffset)==null);
		type = -1;
		assertTrue(skin.PMcharaLoader(usecim, imagefile, type, color, dstx, dsty, dstw, dsth, side, dsttimer, dstOp1, dstOp2, dstOp3, dstOffset)==null);
		type = 3;
		assertTrue(skin.PMcharaLoader(usecim, imagefile, type, color, dstx, dsty, dstw, dsth, side, dsttimer, dstOp1, dstOp2, dstOp3, dstOffset)==null);
	}
	@Test
	public void fileExistTest(){
		imagefile = new File("./skin/default/POMYU Chara/Off/dummy.chp");
		assertTrue(skin.PMcharaLoader(usecim, imagefile, type, color, dstx, dsty, dstw, dsth, side, dsttimer, dstOp1, dstOp2, dstOp3, dstOffset)==null);
		
		imagefile = new File("./skin/default/POMYU Chara/Off/dummy2.chp");
		assertTrue(skin.PMcharaLoader(usecim, imagefile, type, color, dstx, dsty, dstw, dsth, side, dsttimer, dstOp1, dstOp2, dstOp3, dstOffset)==null);
		
		imagefile = new File("./skin/default/POMYU Chara/Off/");
		assertTrue(skin.PMcharaLoader(usecim, imagefile, type, color, dstx, dsty, dstw, dsth, side, dsttimer, dstOp1, dstOp2, dstOp3, dstOffset)==null);
		
		imagefile = new File("./skin/default/POMYU Chara/Off");
		assertTrue(skin.PMcharaLoader(usecim, imagefile, type, color, dstx, dsty, dstw, dsth, side, dsttimer, dstOp1, dstOp2, dstOp3, dstOffset)==null);
	}
}
