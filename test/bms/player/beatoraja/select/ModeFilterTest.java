package bms.player.beatoraja.select;

import bms.player.beatoraja.PlayerConfig;
import com.badlogic.gdx.utils.Json;

/** Regression tests for optional composite mode filters. */
public final class ModeFilterTest {

	private ModeFilterTest() {
	}

	public static void main(String[] args) {
		testDefaultAndNavigation();
		testPlayerConfigSerialization();
	}

	private static void testDefaultAndNavigation() {
		PlayerConfig config = new PlayerConfig();
		assertFalse(ModeFilter.BEAT_5K_7K.isEnabled(config));
		assertFalse(ModeFilter.BEAT_10K_14K.isEnabled(config));
		assertEquals(ModeFilter.ALL, ModeFilter.nextEnabled(ModeFilter.KEYBOARD_24K_DOUBLE, config, 1));

		config.setEnabledModeFilters(new ModeFilter[] { ModeFilter.BEAT_5K_7K });
		assertEquals(ModeFilter.BEAT_5K_7K,
				ModeFilter.nextEnabled(ModeFilter.BEAT_5K_7K, config, 1));

		config.setEnabledModeFilters(new ModeFilter[] { ModeFilter.BEAT_5K_7K, ModeFilter.BEAT_10K_14K });
		assertEquals(ModeFilter.BEAT_10K_14K,
				ModeFilter.nextEnabled(ModeFilter.BEAT_5K_7K, config, 1));
	}

	private static void testPlayerConfigSerialization() {
		PlayerConfig config = new PlayerConfig();
		config.setEnabledModeFilters(new ModeFilter[] { ModeFilter.ALL, ModeFilter.BEAT_5K_7K, ModeFilter.BEAT_10K_14K });

		PlayerConfig restored = new Json().fromJson(PlayerConfig.class, new Json().toJson(config));
		restored.validate();
		assertTrue(restored.isModeFilterEnabled(ModeFilter.BEAT_5K_7K));
		assertTrue(restored.isModeFilterEnabled(ModeFilter.BEAT_10K_14K));
	}

	private static void assertFalse(boolean value) {
		if (value) {
			throw new AssertionError("Expected false");
		}
	}

	private static void assertTrue(boolean value) {
		if (!value) {
			throw new AssertionError("Expected true");
		}
	}

	private static void assertEquals(Object expected, Object actual) {
		if (!expected.equals(actual)) {
			throw new AssertionError("Expected " + expected + " but was " + actual);
		}
	}
}
