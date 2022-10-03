package bms.player.beatoraja.play;

import bms.model.Mode;

public class LaneProperty {

	/**
	 * 入力キーからレーンへの対応
	 */
	private final int[] keyToLane;

	/**
	 * レーンから入力キー（複数）への対応
	 */
	private final int[][] laneToKey;

	/**
	 * レーンが何個目のスクラッチか
	 */
	private final int[] laneToScratch;

	/**
	 * レーンからスキンに使用する番号への対応
	 */
	private final int[] laneToSkinOffset;

	/**
	 * レーンからプレイヤー番号への対応
	 */
	private final int[] laneToPlayer;

	/**
	 * 各スクラッチを処理する入力キー（2個ずつ）
	 */
	private final int[][] scratchToKey;

	public LaneProperty(Mode mode) {
		switch (mode) {
		case BEAT_5K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 5 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5,6} };
			laneToScratch = new int[] { -1, -1, -1, -1, -1, 0 };
			laneToSkinOffset = new int[] { 1, 2, 3, 4, 5, 0 };
			scratchToKey = new int[][] { {5,6} };
			break;
		case BEAT_7K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7,8} };
			laneToScratch = new int[] { -1, -1, -1, -1, -1, -1, -1, 0 };
			laneToSkinOffset = new int[] { 1, 2, 3, 4, 5, 6, 7, 0 };
			scratchToKey = new int[][] { {7,8} };
			break;
		case BEAT_10K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 5, 6, 7, 8, 9, 10, 11, 11 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5,6}, {7}, {8}, {9}, {10}, {11}, {12,13} };
			laneToScratch = new int[] { -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, 1 };
			laneToSkinOffset = new int[] { 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0 };
			scratchToKey = new int[][] { {5,6}, {12,13} };
			break;
		case BEAT_14K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9, 10, 11, 12, 13, 14, 15, 15 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7,8}, {9}, {10}, {11}, {12}, {13}, {14}, {15}, {16,17} };
			laneToScratch = new int[] { -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, -1, -1, 1 };
			laneToSkinOffset = new int[] { 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7, 0 };
			scratchToKey = new int[][] { {7,8}, {16,17} };
			break;
		case POPN_9K:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8} };
			laneToScratch = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1 };
			laneToSkinOffset = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
			scratchToKey = new int[][] { };
			break;
		case KEYBOARD_24K:
			keyToLane = new int[26];
			laneToKey = new int[26][1];
			laneToScratch = new int[26];
			laneToSkinOffset = new int[26];
			for (int i=0; i<26; i++) {
				keyToLane[i] = i;
				laneToKey[i][0] = i;
				laneToScratch[i] = -1;
				laneToSkinOffset[i] = i + 1;
			}
			scratchToKey = new int[][] { };
			break;
		case KEYBOARD_24K_DOUBLE:
			keyToLane = new int[52];
			laneToKey = new int[52][1];
			laneToScratch = new int[52];
			laneToSkinOffset = new int[52];
			for (int i=0; i<52; i++) {
				keyToLane[i] = i;
				laneToKey[i][0] = i;
				laneToScratch[i] = -1;
				laneToSkinOffset[i] = i % 26 + 1;
			}
			scratchToKey = new int[][] { };
			break;
		default:
			keyToLane = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7 };
			laneToKey = new int[][] { {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7,8} };
			laneToScratch = new int[] { -1, -1, -1, -1, -1, -1, -1, 0 };
			laneToSkinOffset = new int[] { 1, 2, 3, 4, 5, 6, 7, 0 };
			scratchToKey = new int[][] { {7,8} };
			break;
		}
		laneToPlayer = new int[mode.key];
		for(int i = 0; i < mode.key; i++) {
			laneToPlayer[i] = i / (mode.key / mode.player);
		}
	}

	public int[] getKeyLaneAssign() {
		return keyToLane;
	}

	public int[][] getLaneKeyAssign() {
		return laneToKey;
	}

	public int[] getLaneScratchAssign() {
		return laneToScratch;
	}

	public int[] getLaneSkinOffset() {
		return laneToSkinOffset;
	}

	public int[] getLanePlayer() {
		return laneToPlayer;
	}

	public int[][] getScratchKeyAssign() {
		return scratchToKey;
	}
}
