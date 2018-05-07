package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
import bms.model.NormalNote;
import bms.model.Note;
import bms.model.TimeLine;

	public static final int MIRROR = 0;
	/**
	 * �꺆�꺖�깇�꺖�깉
	 */
	public static final int R_RANDOM = 1;
	/**
	 * �꺀�꺍���깲
	 */
	public static final int RANDOM = 2;
	/**
	 * �궚�꺆�궧
	 */
	public static final int CROSS = 3;
	/**
	 * �궧�궚�꺀�긿�긽�꺃�꺖�꺍�굮�맜���꺀�꺍���깲
	 */
	public static final int RANDOM_EX = 4;
	/**
	 * 1P-2P�굮�뀯�굦�쎘�걟�굥
	 */
	public static final int FLIP = 5;
	/**
	 * 1P�겗鈺쒒씊�굮2P�겓�궠�깞�꺖�걲�굥
	 */
	public static final int BATTLE = 6;

	public LaneShuffleModifier(int type) {
		super(type == RANDOM_EX ? 1 : 0);
		this.type = type;
	}

	private void makeRandom(BMSModel model) {
		Mode mode = model.getMode();
		int[] keys;
		switch (type) {
		case MIRROR:
			keys = getKeys(mode, false);
			random = keys.length > 0 ? rotate(keys, keys.length - 1, false) : keys;
			break;
		case R_RANDOM:
			keys = getKeys(mode, false);
			random = keys.length > 0 ? rotate(keys) : keys;
			break;
		case RANDOM:
			keys = getKeys(mode, false);
			random = keys.length > 0 ? shuffle(keys) : keys;
			break;
		case CROSS:
			keys = getKeys(mode, false);
			random = new int[keys.length];
			for (int i = 0; i < keys.length / 2 - 1; i += 2) {
				random[i] = keys[i + 1];
				random[i + 1] = keys[i];
				random[keys.length - i - 1] = keys[keys.length - i - 2];
				random[keys.length - i - 2] = keys[keys.length - i - 1];
			}
			break;
		case RANDOM_EX:
			keys = getKeys(mode, true);
			if(mode == Mode.POPN_9K) random = keys.length > 0 ? noMurioshiLaneShuffle(model) : keys;
			else random = keys.length > 0 ? shuffle(keys) : keys;
			break;
		case FLIP:
			if (mode.player == 2) {
				random = new int[mode.key];
				for (int i = 0; i < random.length; i++) {
					random[i] = (i + (mode.key / mode.player)) % mode.key;
				}
			} else {
				random = new int[0];
			}
			break;
		case BATTLE:
			if (mode.player == 1) {
				random = new int[0];
			} else {
				keys = getKeys(mode, true);
				random = new int[keys.length * 2];
				System.arraycopy(keys, 0, random, 0, keys.length);
				System.arraycopy(keys, 0, random, keys.length, keys.length);
			}
			break;

		}
	}

				List<Integer> noteLane = new ArrayList<Integer>(keys.length);
				for (int i = 0; i < lanes; i++) {
					Note n = tl.getNote(i);
					if((n != null && n instanceof NormalNote ) || (ln != null && ln[i] != -1)) {
							noteLane.add((Integer) i);
					}
				}

				if(noteLane.size() >= 7) {
					isImpossible = true;
					break;
				} else if(noteLane.size() >= 3) {
					int pattern=0;
					for(int i=0;i<noteLane.size();i++) {
						pattern += (int) Math.pow(2, noteLane.get(i));
					}
					originalPatternList.add((Integer) pattern);
				}
			}
		}

			for(int i = 0 ; i < originalPatternList.size()-1 ; i++ ) {
				for(int j = originalPatternList.size()-1 ; j > i; j-- ) {
					if (originalPatternList.get(i).equals(originalPatternList.get(j))) {
						originalPatternList.remove(j);
					}
				}
			}

			int[] searchLane = new int[9];
			boolean[] searchLaneFlag = new boolean[9];
			Arrays.fill(searchLaneFlag, false);
			List<Integer> tempPattern = new ArrayList<Integer>(keys.length);
			for(searchLane[0]=0;searchLane[0]<9;searchLane[0]++) {
				searchLaneFlag[searchLane[0]] = true;
				for(searchLane[1]=0;searchLane[1]<9;searchLane[1]++) {
					if(searchLaneFlag[searchLane[1]]) continue;
					searchLaneFlag[searchLane[1]] = true;
					for(searchLane[2]=0;searchLane[2]<9;searchLane[2]++) {
						if(searchLaneFlag[searchLane[2]]) continue;
						searchLaneFlag[searchLane[2]] = true;
						for(searchLane[3]=0;searchLane[3]<9;searchLane[3]++) {
							if(searchLaneFlag[searchLane[3]]) continue;
							searchLaneFlag[searchLane[3]] = true;
							for(searchLane[4]=0;searchLane[4]<9;searchLane[4]++) {
								if(searchLaneFlag[searchLane[4]]) continue;
								searchLaneFlag[searchLane[4]] = true;
								for(searchLane[5]=0;searchLane[5]<9;searchLane[5]++) {
									if(searchLaneFlag[searchLane[5]]) continue;
									searchLaneFlag[searchLane[5]] = true;
									for(searchLane[6]=0;searchLane[6]<9;searchLane[6]++) {
										if(searchLaneFlag[searchLane[6]]) continue;
										searchLaneFlag[searchLane[6]] = true;
										for(searchLane[7]=0;searchLane[7]<9;searchLane[7]++) {
											if(searchLaneFlag[searchLane[7]]) continue;
											searchLaneFlag[searchLane[7]] = true;
											for(searchLane[8]=0;searchLane[8]<9;searchLane[8]++) {
												for(int i=0;i<originalPatternList.size();i++) {
													tempPattern.clear();
													for(int j=0;j<9;j++) {
														if(((int)(originalPatternList.get(i)/Math.pow(2,j))%2)==1) tempPattern.add((Integer) searchLane[j]+1);
													}
													if(
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)4)!=-1&&tempPattern.indexOf((Integer)7)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)4)!=-1&&tempPattern.indexOf((Integer)8)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)4)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)5)!=-1&&tempPattern.indexOf((Integer)8)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)5)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)1)!=-1&&tempPattern.indexOf((Integer)6)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)2)!=-1&&tempPattern.indexOf((Integer)5)!=-1&&tempPattern.indexOf((Integer)8)!=-1)||
															(tempPattern.indexOf((Integer)2)!=-1&&tempPattern.indexOf((Integer)5)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)2)!=-1&&tempPattern.indexOf((Integer)6)!=-1&&tempPattern.indexOf((Integer)9)!=-1)||
															(tempPattern.indexOf((Integer)3)!=-1&&tempPattern.indexOf((Integer)6)!=-1&&tempPattern.indexOf((Integer)9)!=-1)
															) {
														murioshiFlag=true;
														break;
													}
												}

											}
											searchLaneFlag[searchLane[7]] = false;
										}
										searchLaneFlag[searchLane[6]] = false;
									}
									searchLaneFlag[searchLane[5]] = false;
								}
								searchLaneFlag[searchLane[4]] = false;
							}
							searchLaneFlag[searchLane[3]] = false;
						}
						searchLaneFlag[searchLane[2]] = false;
					}
					searchLaneFlag[searchLane[1]] = false;
				}
				searchLaneFlag[searchLane[0]] = false;
			}
		}
		
		if(kouhoPatternList.size() > 0) {
			int r = (int) (Math.random() * kouhoPatternList.size());
			for (int i = 0; i < 9; i++) {
				result[(int) (kouhoPatternList.get(r)).get(i)] = i;
			}
		} else {
			int mirror = (int) (Math.random() * 2);
			for (int i = 0; i < 9; i++) {
				result[i] = mirror == 0 ? i : 8 - i;
			}
		}
