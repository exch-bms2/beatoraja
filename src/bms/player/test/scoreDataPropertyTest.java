package bms.player.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bms.model.Mode;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.ScoreDataProperty;

public class scoreDataPropertyTest {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test_Beat5k() {
		IRScoreData irs = new IRScoreData(Mode.BEAT_5K);
		int TOTAL_NOTES = 120;
		int NOW_NOTES = 12;
		
		irs.setNotes(TOTAL_NOTES);
		irs.setEpg(10);
		irs.setLpg(10);
		
		irs.setEgr(10);
		irs.setLgr(10);
		
		irs.setEgd(10);
		irs.setLgd(10);
		
		irs.setEbd(10);
		irs.setLbd(10);
		irs.setEpr(10);
		irs.setLpr(10);
		irs.setEms(10);
		irs.setLms(10);
		
		
		ScoreDataProperty scoreData = new ScoreDataProperty();
		scoreData.update(irs, NOW_NOTES);
		
		assertEquals( 41666, scoreData.getNowScore());
		assertTrue(((float) 60 / (NOW_NOTES*2)) == scoreData.getNowRate());
		assertTrue((float) (scoreData.getNowRate() * 100) == scoreData.getNowRateInt());
		assertTrue(((float) 60 / (TOTAL_NOTES * 2)) == scoreData.getRate());
		assertTrue((float) (scoreData.getRate() * 100) == scoreData.getRateInt());
	}

	@Test
	public void test_Beat7k() {
		IRScoreData irs = new IRScoreData(Mode.BEAT_7K);
		int TOTAL_NOTES = 120;
		int NOW_NOTES = 12;
		
		irs.setNotes(TOTAL_NOTES);
		irs.setEpg(10);
		irs.setLpg(10);
		
		irs.setEgr(10);
		irs.setLgr(10);
		
		irs.setEgd(10);
		irs.setLgd(10);
		
		irs.setEbd(10);
		irs.setLbd(10);
		irs.setEpr(10);
		irs.setLpr(10);
		irs.setEms(10);
		irs.setLms(10);
		
		
		
		ScoreDataProperty scoreData = new ScoreDataProperty();
		scoreData.update(irs, NOW_NOTES);
		
		assertEquals(45000, scoreData.getNowScore());
		assertTrue(((float) 60 / (NOW_NOTES*2)) == scoreData.getNowRate());
		assertTrue((float) (scoreData.getNowRate() * 100) == scoreData.getNowRateInt());
		assertTrue(((float) 60 / (TOTAL_NOTES * 2)) == scoreData.getRate());
		assertTrue((float) (scoreData.getRate() * 100) == scoreData.getRateInt());
	}
	
	@Test
	public void test_POPN5K() {
		IRScoreData irs = new IRScoreData(Mode.POPN_5K);
		int TOTAL_NOTES = 120;
		int NOW_NOTES = 12;
		
		irs.setNotes(TOTAL_NOTES);
		irs.setEpg(10);
		irs.setLpg(10);
		
		irs.setEgr(10);
		irs.setLgr(10);
		
		irs.setEgd(10);
		irs.setLgd(10);
		
		irs.setEbd(10);
		irs.setLbd(10);
		irs.setEpr(10);
		irs.setLpr(10);
		irs.setEms(10);
		irs.setLms(10);
		
		
		
		ScoreDataProperty scoreData = new ScoreDataProperty();
		scoreData.update(irs, NOW_NOTES);
		
		assertEquals(35000, scoreData.getNowScore());		
		assertTrue(((float) 60 / (NOW_NOTES*2)) == scoreData.getNowRate());
		assertTrue((float) (scoreData.getNowRate() * 100) == scoreData.getNowRateInt());
		assertTrue(((float) 60 / (TOTAL_NOTES * 2)) == scoreData.getRate());
		assertTrue((float) (scoreData.getRate() * 100) == scoreData.getRateInt());
	}
	
	@Test
	public void test_Beat5kSET() {
		IRScoreData scoreData1 = new IRScoreData(Mode.BEAT_5K);
		int TOTAL_NOTES = 120;
		int NOW_NOTES = 12;
		
		scoreData1.setNotes(TOTAL_NOTES);
		scoreData1.setEpg(10);
		scoreData1.setLpg(10);
		
		scoreData1.setEgr(10);
		scoreData1.setLgr(10);
		
		scoreData1.setEgd(10);
		scoreData1.setLgd(10);
		
		scoreData1.setEbd(10);
		scoreData1.setLbd(10);
		scoreData1.setEpr(10);
		scoreData1.setLpr(10);
		scoreData1.setEms(10);
		scoreData1.setLms(10);
		
		
		
		ScoreDataProperty scoreData = new ScoreDataProperty();
		scoreData.setTargetScore(50000, 45000, TOTAL_NOTES);
		scoreData.update(scoreData1, NOW_NOTES);
		
		assertEquals(41666, scoreData.getNowScore());
		assertEquals(50000, scoreData.getBestScore());
		assertTrue(((float)50000 / (TOTAL_NOTES * 2)) == scoreData.getBestScoreRate());
		assertEquals(50000 * NOW_NOTES / TOTAL_NOTES, scoreData.getNowBestScore());
		assertTrue((float) 50000 * NOW_NOTES / (TOTAL_NOTES * TOTAL_NOTES * 2)
				== scoreData.getNowBestScoreRate());
		
		
		assertTrue(((float) 60 / (NOW_NOTES*2)) == scoreData.getNowRate());
		assertTrue((float) (scoreData.getNowRate() * 100) == scoreData.getNowRateInt());
		assertTrue(((float) 60 / (TOTAL_NOTES * 2)) == scoreData.getRate());
		assertTrue((float) (scoreData.getRate() * 100) == scoreData.getRateInt());
		assertTrue( (float) 50000 / (TOTAL_NOTES * 2) == scoreData.getBestScoreRate());
		assertTrue((int) (scoreData.getBestScoreRate() * 100) == scoreData.getBestRateInt());
		
		assertTrue(45000 == scoreData.getRivalScore());
		assertTrue((float) 45000 / (TOTAL_NOTES * 2) == scoreData.getRivalScoreRate());
		assertTrue(scoreData.getRivalScore() * NOW_NOTES / TOTAL_NOTES == scoreData.getNowRivalScore());
		assertTrue((float) scoreData.getRivalScore() * NOW_NOTES / (TOTAL_NOTES * TOTAL_NOTES * 2)
				== scoreData.getNowRivalScoreRate());
		assertTrue((int) (scoreData.getRivalScoreRate() * 100) == scoreData.getRivalRateInt());
	}
	
	@Test
	public void test_Beat7kSET() {
		IRScoreData irs = new IRScoreData(Mode.BEAT_7K);
		int TOTAL_NOTES = 120;
		int NOW_NOTES = 12;
		
		irs.setNotes(TOTAL_NOTES);
		irs.setEpg(10);
		irs.setLpg(10);
		
		irs.setEgr(10);
		irs.setLgr(10);
		
		irs.setEgd(10);
		irs.setLgd(10);
		
		irs.setEbd(10);
		irs.setLbd(10);
		irs.setEpr(10);
		irs.setLpr(10);
		irs.setEms(10);
		irs.setLms(10);
		
		
		
		ScoreDataProperty scoreData = new ScoreDataProperty();
		scoreData.setTargetScore(50000, 45000, TOTAL_NOTES);
		scoreData.update(irs, NOW_NOTES);
		
		assertEquals(45000, scoreData.getNowScore());
		assertEquals(50000, scoreData.getBestScore());
		assertTrue(((float)50000 / (TOTAL_NOTES * 2)) == scoreData.getBestScoreRate());
		assertEquals(50000 * NOW_NOTES / TOTAL_NOTES, scoreData.getNowBestScore());
		assertTrue((float) 50000 * NOW_NOTES / (TOTAL_NOTES * TOTAL_NOTES * 2)
				== scoreData.getNowBestScoreRate());
		
		
		assertTrue(((float) 60 / (NOW_NOTES*2)) == scoreData.getNowRate());
		assertTrue((float) (scoreData.getNowRate() * 100) == scoreData.getNowRateInt());
		assertTrue(((float) 60 / (TOTAL_NOTES * 2)) == scoreData.getRate());
		assertTrue((float) (scoreData.getRate() * 100) == scoreData.getRateInt());
		assertTrue( (float) 50000 / (TOTAL_NOTES * 2) == scoreData.getBestScoreRate());
		assertTrue((int) (scoreData.getBestScoreRate() * 100) == scoreData.getBestRateInt());
		
		assertTrue(45000 == scoreData.getRivalScore());
		assertTrue((float) 45000 / (TOTAL_NOTES * 2) == scoreData.getRivalScoreRate());
		assertTrue(scoreData.getRivalScore() * NOW_NOTES / TOTAL_NOTES == scoreData.getNowRivalScore());
		assertTrue((float) scoreData.getRivalScore() * NOW_NOTES / (TOTAL_NOTES * TOTAL_NOTES * 2)
				== scoreData.getNowRivalScoreRate());
		assertTrue((int) (scoreData.getRivalScoreRate() * 100) == scoreData.getRivalRateInt());
	}
	
	@Test
	public void test_POPN5KSET() {
		IRScoreData irs = new IRScoreData(Mode.POPN_5K);
		int TOTAL_NOTES = 120;
		int NOW_NOTES = 12;
		
		irs.setNotes(TOTAL_NOTES);
		irs.setEpg(10);
		irs.setLpg(10);
		
		irs.setEgr(10);
		irs.setLgr(10);
		
		irs.setEgd(10);
		irs.setLgd(10);
		
		irs.setEbd(10);
		irs.setLbd(10);
		irs.setEpr(10);
		irs.setLpr(10);
		irs.setEms(10);
		irs.setLms(10);
		
		
		
		ScoreDataProperty scoreData = new ScoreDataProperty();
		scoreData.setTargetScore(50000, 45000, TOTAL_NOTES);
		scoreData.update(irs, NOW_NOTES);
		
		assertEquals(35000, scoreData.getNowScore());
		assertEquals(50000, scoreData.getBestScore());
		assertTrue(((float)50000 / (TOTAL_NOTES * 2)) == scoreData.getBestScoreRate());
		assertEquals(50000 * NOW_NOTES / TOTAL_NOTES, scoreData.getNowBestScore());
		assertTrue((float) 50000 * NOW_NOTES / (TOTAL_NOTES * TOTAL_NOTES * 2)
				== scoreData.getNowBestScoreRate());
		
		
		assertTrue(((float) 60 / (NOW_NOTES*2)) == scoreData.getNowRate());
		assertTrue((float) (scoreData.getNowRate() * 100) == scoreData.getNowRateInt());
		assertTrue(((float) 60 / (TOTAL_NOTES * 2)) == scoreData.getRate());
		assertTrue((float) (scoreData.getRate() * 100) == scoreData.getRateInt());
		assertTrue( (float) 50000 / (TOTAL_NOTES * 2) == scoreData.getBestScoreRate());
		assertTrue((int) (scoreData.getBestScoreRate() * 100) == scoreData.getBestRateInt());
		
		assertTrue(45000 == scoreData.getRivalScore());
		assertTrue((float) 45000 / (TOTAL_NOTES * 2) == scoreData.getRivalScoreRate());
		assertTrue(scoreData.getRivalScore() * NOW_NOTES / TOTAL_NOTES == scoreData.getNowRivalScore());
		assertTrue((float) scoreData.getRivalScore() * NOW_NOTES / (TOTAL_NOTES * TOTAL_NOTES * 2)
				== scoreData.getNowRivalScoreRate());
		assertTrue((int) (scoreData.getRivalScoreRate() * 100) == scoreData.getRivalRateInt());
	}
}
