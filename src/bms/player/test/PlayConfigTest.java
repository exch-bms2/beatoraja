package bms.player.test;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.junit.Test;

import bms.model.Mode;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayModeConfig;
import bms.player.beatoraja.PlayerConfig;

public class PlayConfigTest {

	
	public boolean deleteDirectory(File path) {
		if (!path.exists()) {
			return false;
		}
		File[] files = path.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				deleteDirectory(file);
			} else {
				file.delete();
			}
		}
		return path.delete();
	}

	String randomString() {
		// random 문자열 생성
		StringBuffer temp = new StringBuffer();
		Random rnd = new Random();
		for (int i = 0; i < 20; i++) {
			int rIndex = rnd.nextInt(3);
			switch (rIndex) {
			case 0:
				// a-z
				temp.append((char) ((int) (rnd.nextInt(26)) + 97));
				break;
			case 1:
				// A-Z
				temp.append((char) ((int) (rnd.nextInt(26)) + 65));
				break;
			case 2:
				// 0-9
				temp.append((rnd.nextInt(10)));
				break;
			}
		}
		return temp.toString();
	}
	
	
	@Test
	public void PlayerConfigSetGetNameTest() {
		String testNameOne = randomString();
		String testNameTwo = randomString();
		PlayerConfig Playerconfig = new PlayerConfig();
		Playerconfig.setName(testNameOne);
		assertEquals(testNameOne, Playerconfig.getName());
		Playerconfig.setName(testNameTwo);
		assertEquals(testNameTwo, Playerconfig.getName());
	}

	@Test
	public void PlayerConfigSetGetGauseTest() {
		int testGauseOne = 1;
		int testGauseTwo = 30000;
		PlayerConfig Playerconfig = new PlayerConfig();
		Playerconfig.setGauge(testGauseOne);
		assertEquals(testGauseOne, Playerconfig.getGauge());
		Playerconfig.setGauge(testGauseTwo);
		assertEquals(testGauseTwo, Playerconfig.getGauge());
	}

	// minus gauge가 나오면 안된다.
	@Test
	public void PlayerConfigSetGetGauseMinusTest() {
		int testGauseOne = -1;
		int testGauseTwo = -30000;
		PlayerConfig Playerconfig = new PlayerConfig();
		Playerconfig.setGauge(testGauseOne);
		assertEquals(testGauseOne, Playerconfig.getGauge());
		Playerconfig.setGauge(testGauseTwo);
		assertEquals(testGauseTwo, Playerconfig.getGauge());
	}

	@Test
	public void PlayerConfigInittest() {
		File file = new File("player");
		deleteDirectory(file);
		Config configure = new Config();
		PlayerConfig.init(configure);

	}

	@Test
	public void PlayerConfigCreateTest() {

		String randomName = randomString();
		PlayerConfig.create(randomName);
		Path p = Paths.get("player/" + randomName);
		assertEquals(true, Files.exists(p));
	}
	// 기존 directory가 존재할 때 Create Test
	// @Test
	// public void PlayerConfigCreateAtNoemptyTest() {
	//
	// String randomName = randomString();
	// PlayerConfig.create(randomName);
	// Path p = Paths.get("player/" + randomName);
	// assertEquals(true,Files.exists(p));
	// }

	@Test
	public void PlayerConfigWritetest() {
		String randomName = randomString();
		PlayerConfig playerconfig = new PlayerConfig();
		playerconfig.setId(randomName);
		PlayerConfig.create(randomName);
		PlayerConfig.write(playerconfig);
		Path p = Paths.get("player/" + randomName + "/config.json");
		assertEquals(true, Files.exists(p));
	}

	@Test
	public void PlayerConfigReadtest() {
		String randomName = randomString();
		PlayerConfig.create(randomName);
		PlayerConfig player = PlayerConfig.readPlayerConfig(randomName);
		assertEquals(player.getId(), randomName);

	}

	@Test
	public void PlayerConfigReadAlltest() {
		String randomName1 = randomString();
		String randomName2 = randomString();
		String randomName3 = randomString();
		boolean One_find = false;
		boolean Two_find = false;
		boolean Three_find = false;
		String[] before_StringArr = PlayerConfig.readAllPlayerID();
		int before_length = PlayerConfig.readAllPlayerID().length;
		PlayerConfig.create(randomName1);
		PlayerConfig.create(randomName2);
		PlayerConfig.create(randomName3);
		String[] after_StringArr = PlayerConfig.readAllPlayerID();
		int after_length = PlayerConfig.readAllPlayerID().length;
		assertEquals(before_length + 3, after_length);

		for (int i = 0; i < after_length; i++) {
			if (after_StringArr[i].equals(randomName1))
				One_find = true;
			if (after_StringArr[i].equals(randomName2))
				Two_find = true;
			if (after_StringArr[i].equals(randomName3))
				Three_find = true;
		}
		assertEquals(true, One_find);
		assertEquals(true, Two_find);
		assertEquals(true, Three_find);

	}
	
	/*
	 * isConstant function test
	 * initial constant variable is false
	 * intput : boolean
	 * output : boolean
	 */
	@Test
	public void isConstantTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(false,Playerconfig.isConstant());
		Playerconfig.setConstant(true);
		assertEquals(true,Playerconfig.isConstant());
		Playerconfig.setConstant(false);
		assertEquals(false,Playerconfig.isConstant());
	}
	
	/*
	 * isBpmguide function test
	 * initial bpmguide variable is false
	 * intput : boolean
	 * output : boolean
	 */
	@Test
	public void isBpmguideTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(false,Playerconfig.isBpmguide());
		Playerconfig.setBpmguide(true);
		assertEquals(true,Playerconfig.isBpmguide());
		Playerconfig.setBpmguide(false);
		assertEquals(false,Playerconfig.isBpmguide());
	}
	
	/*
	 * isContinueUntilEndOfSong function test
	 * initial ContinueUntilEndOfSong variable is false
	 * intput : boolean
	 * output : boolean
	 */
	@Test
	public void isContinueUntilEndOfSongTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(false,Playerconfig.isContinueUntilEndOfSong());
		Playerconfig.setContinueUntilEndOfSong(true);
		assertEquals(true,Playerconfig.isContinueUntilEndOfSong());
		Playerconfig.setContinueUntilEndOfSong(false);
		assertEquals(false,Playerconfig.isContinueUntilEndOfSong());
	}
	/*
	 * getRandom and getRandom2 test
	 * intput : integer
	 * output : integer
	 */
	@Test
	public void getRandomTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		Playerconfig.setRandom(1);
		Playerconfig.setRandom2(1);
		assertEquals(1,Playerconfig.getRandom());
		assertEquals(1,Playerconfig.getRandom2());
		Playerconfig.setRandom(-1);
		Playerconfig.setRandom2(-1);
		assertEquals(-1,Playerconfig.getRandom());
		assertEquals(-1,Playerconfig.getRandom2());
		Playerconfig.setRandom(100);
		Playerconfig.setRandom2(100);
		assertEquals(100,Playerconfig.getRandom());
		assertEquals(100,Playerconfig.getRandom2());
		Playerconfig.setRandom(-100);
		Playerconfig.setRandom2(-100);
		assertEquals(-100,Playerconfig.getRandom());
		assertEquals(-100,Playerconfig.getRandom2());
	}
	/*
	 * Judgetiming function test
	 * intput : integer
	 * output : integer
	 * timing is not negative
	 * but negative is enable this function..
	 */
	@Test
	public void JudgetimingTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		Playerconfig.setJudgetiming(1);
		assertEquals(1,Playerconfig.getJudgetiming());
		Playerconfig.setJudgetiming(-1);
		assertEquals(-1,Playerconfig.getJudgetiming());
		Playerconfig.setJudgetiming(100);
		assertEquals(100,Playerconfig.getJudgetiming());
		Playerconfig.setJudgetiming(-100);
		assertEquals(-100,Playerconfig.getJudgetiming());
	}
	
	/*
	 * getDoubleoption function test
	 * intput : integer
	 * output : integer
	 */
	@Test
	public void getDoubleoptionTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		Playerconfig.setDoubleoption(1);
		assertEquals(1,Playerconfig.getDoubleoption());
		Playerconfig.setDoubleoption(-1);
		assertEquals(-1,Playerconfig.getDoubleoption());
		Playerconfig.setDoubleoption(100);
		assertEquals(100,Playerconfig.getDoubleoption());
		Playerconfig.setDoubleoption(-100);
		assertEquals(-100,Playerconfig.getDoubleoption());
	}
	
	/*
	 * isNomine function test
	 * initial isNomine variable is false
	 * intput : boolean
	 * output : boolean
	 */
	@Test
	public void isNomineTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(false,Playerconfig.isNomine());
		Playerconfig.setNomine(true);
		assertEquals(true,Playerconfig.isNomine());
		Playerconfig.setNomine(false);
		assertEquals(false,Playerconfig.isNomine());
	}
	/*
	 * isLegacynote function test
	 * initial isLegacynote variable is false
	 * intput : boolean
	 * output : boolean
	 */
	@Test
	public void isLegacynoteTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(false,Playerconfig.isLegacynote());
		Playerconfig.setLegacynote(true);;
		assertEquals(true,Playerconfig.isLegacynote());
		Playerconfig.setLegacynote(false);
		assertEquals(false,Playerconfig.isLegacynote());
	}
	
	/*
	 * isShowjudgearea function test
	 * initial isShowjudgearea variable is false
	 * intput : boolean
	 * output : boolean
	 */
	@Test
	public void isShowjudgearea() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(false,Playerconfig.isShowjudgearea());
		Playerconfig.setShowjudgearea(true);;
		assertEquals(true,Playerconfig.isShowjudgearea());
		Playerconfig.setShowjudgearea(false);
		assertEquals(false,Playerconfig.isShowjudgearea());
	}
	
	/*
	 * isGuideSE function test
	 * initial isGuideSE variable is false
	 * intput : boolean
	 * output : boolean
	 */
	@Test
	public void isGuideSE() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(false,Playerconfig.isGuideSE());
		Playerconfig.setGuideSE(true);;
		assertEquals(true,Playerconfig.isGuideSE());
		Playerconfig.setGuideSE(false);
		assertEquals(false,Playerconfig.isGuideSE());
	}
	
	/*
	 * isWindowHold function test
	 * initial isWindowHold variable is false
	 * intput : boolean
	 * output : boolean
	 */
	@Test
	public void isWindowHold() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(false,Playerconfig.isWindowHold());
		Playerconfig.setWindowHold(true);;
		assertEquals(true,Playerconfig.isWindowHold());
		Playerconfig.setWindowHold(false);
		assertEquals(false,Playerconfig.isWindowHold());
	}

	/*
	 * isMarkprocessednote function test
	 * initial isShowjudgearea variable is false
	 * intput : boolean
	 * output : boolean
	 */
	@Test
	public void isMarkprocessednote() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(false,Playerconfig.isMarkprocessednote());
		Playerconfig.setMarkprocessednote(true);;
		assertEquals(true,Playerconfig.isMarkprocessednote());
		Playerconfig.setMarkprocessednote(false);
		assertEquals(false,Playerconfig.isMarkprocessednote());
	}
	/*
	 * Judgewindowrate function test
	 * intput : integer
	 * output : 25 >  intger or 400< integer is return 100
	 * output : else is  return each set integer
	 * timing is not negative
	 * but negative is enable this function..
	 */
	@Test
	public void JudgewindowrateTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		Playerconfig.setJudgewindowrate(24);
		assertEquals(100,Playerconfig.getJudgewindowrate());
		Playerconfig.setJudgewindowrate(25);
		assertEquals(25,Playerconfig.getJudgewindowrate());
		Playerconfig.setJudgewindowrate(401);
		assertEquals(100,Playerconfig.getJudgewindowrate());
		Playerconfig.setJudgewindowrate(400);
		assertEquals(400,Playerconfig.getJudgewindowrate());
	}
	
	/*
	 * MisslayerDuration function test
	 * intput : integer
	 * output : 0 >  intger oris return 0
	 * output : else is  return each set integer
	 * timing is not negative
	 * but negative is enable this function..
	 */
	@Test
	public void MisslayerDurationTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		Playerconfig.setMisslayerDuration(0);
		assertEquals(0,Playerconfig.getMisslayerDuration());
		Playerconfig.setMisslayerDuration(-1);
		assertEquals(0,Playerconfig.getMisslayerDuration());
		Playerconfig.setMisslayerDuration(1);
		assertEquals(1,Playerconfig.getMisslayerDuration());
		Playerconfig.setMisslayerDuration(55);
		assertEquals(55,Playerconfig.getMisslayerDuration());
	}
	
	/*
	 * getPlayConfig(mode) function test
	 */
	@Test
	public void getPlayConfigTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		Playerconfig.setMode(Mode.BEAT_5K);
		assertEquals(Playerconfig.getMode7(),Playerconfig.getPlayConfig(Playerconfig.getMode()));
		Playerconfig.setMode(Mode.POPN_5K);
		assertEquals(Playerconfig.getMode7(),Playerconfig.getPlayConfig(Mode.POPN_5K));
		Playerconfig.setMode(Mode.BEAT_7K);
		assertEquals(Playerconfig.getMode7(),Playerconfig.getPlayConfig(Mode.BEAT_7K));
		Playerconfig.setMode(Mode.BEAT_10K);
		assertEquals(Playerconfig.getMode14(),Playerconfig.getPlayConfig(Mode.BEAT_10K));
		Playerconfig.setMode(Mode.BEAT_14K);
		assertEquals(Playerconfig.getMode14(),Playerconfig.getPlayConfig(Mode.BEAT_14K));
		Playerconfig.setMode(Mode.POPN_9K);
		assertEquals(Playerconfig.getMode9(),Playerconfig.getPlayConfig(Mode.POPN_9K));
		Playerconfig.setMode(Mode.KEYBOARD_24K);
		assertEquals(Playerconfig.getMode24(),Playerconfig.getPlayConfig(Mode.KEYBOARD_24K));
		Playerconfig.setMode(Mode.KEYBOARD_24K_DOUBLE);
		assertEquals(Playerconfig.getMode24double(),Playerconfig.getPlayConfig(Mode.KEYBOARD_24K_DOUBLE));
	}
	
	/*
	 * getPlayConfig(int) function test
	 */
	@Test
	public void getPlayConfigIntTest() {
		PlayerConfig Playerconfig = new PlayerConfig();
		assertEquals(Playerconfig.getMode7(),Playerconfig.getPlayConfig(7));
		assertEquals(Playerconfig.getMode7(),Playerconfig.getPlayConfig(4));
		assertEquals(Playerconfig.getMode7(),Playerconfig.getPlayConfig(5));
		assertEquals(Playerconfig.getMode14(),Playerconfig.getPlayConfig(14));
		assertEquals(Playerconfig.getMode14(),Playerconfig.getPlayConfig(10));
		assertEquals(Playerconfig.getMode9(),Playerconfig.getPlayConfig(9));
		assertEquals(Playerconfig.getMode24(),Playerconfig.getPlayConfig(25));
		assertEquals(Playerconfig.getMode24double(),Playerconfig.getPlayConfig(50));
	}
	
	/*
	 * getSkin function test
	 */
	@Test
	public void getSkinTest() {
		//PlayerConfig Playerconfig = new PlayerConfig();
		//assertEquals(Playerconfig.getSkin(),Playerconfig.getPlayConfig(7));
		
	}
}
