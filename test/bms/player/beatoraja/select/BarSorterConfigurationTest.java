package bms.player.beatoraja.select;

import java.util.Arrays;

import bms.player.beatoraja.PlayerConfig;
import com.badlogic.gdx.utils.Json;

/** Regression tests for configurable sort choices and legacy numeric IDs. */
public final class BarSorterConfigurationTest {
	public static void main(String[] args) {
		PlayerConfig config = new PlayerConfig();
		assertSorters(BarSorter.defaultSorter, config.getEnabledBarSorters());
		config.validate();
		for (int i = 1; i <= 8; i++) {
			config.cycleBarSorter(1);
			assertEquals(BarSorter.defaultSorter[i % 8].name(), config.getSortid());
			assertEquals(i % 8, config.getSort());
		}
		config.cycleBarSorter(-1);
		assertEquals("MISSCOUNT", config.getSortid());

		BarSorter extra = Arrays.stream(BarSorter.values())
				.filter(sorter -> !Arrays.asList(BarSorter.defaultSorter).contains(sorter)).findFirst().orElseThrow();
		BarSorter[] choices = {BarSorter.SCORE, extra, BarSorter.TITLE};
		config.setEnabledBarSorters(choices);
		choices[0] = BarSorter.BPM;
		config.getEnabledBarSorters()[0] = BarSorter.BPM;
		config.setSortid("TITLE");
		config.cycleBarSorter(1);
		assertEquals("SCORE", config.getSortid());
		assertEquals(6, config.getSort());
		config.cycleBarSorter(1);
		assertEquals(extra.name(), config.getSortid());
		assertEquals(6, config.getSort());
		config.cycleBarSorter(-1);
		assertEquals("SCORE", config.getSortid());

		Json json = new Json();
		PlayerConfig restored = json.fromJson(PlayerConfig.class, json.toJson(config));
		restored.validate();
		assertSorters(new BarSorter[] {BarSorter.SCORE, extra, BarSorter.TITLE}, restored.getEnabledBarSorters());
		assertEquals("SCORE", restored.getSortid());

		config.setSortid("ARTIST");
		config.cycleBarSorter(1);
		assertEquals("SCORE", config.getSortid());
		config.setSortid("INVALID");
		config.cycleBarSorter(-1);
		assertEquals("TITLE", config.getSortid());
		config.setEnabledBarSorters(new BarSorter[] {BarSorter.SCORE, null, BarSorter.TITLE, BarSorter.SCORE});
		assertSorters(new BarSorter[] {BarSorter.SCORE, BarSorter.TITLE}, config.getEnabledBarSorters());
		config.setEnabledBarSorters(new BarSorter[] {extra});
		config.cycleBarSorter(1);
		config.cycleBarSorter(-1);
		assertEquals(extra.name(), config.getSortid());
		for (BarSorter[] invalid : new BarSorter[][] {null, {}, {null}}) {
			config.setEnabledBarSorters(invalid);
			assertSorters(BarSorter.defaultSorter, config.getEnabledBarSorters());
		}
		PlayerConfig legacy = json.fromJson(PlayerConfig.class, "{\"sort\":4,\"sortid\":\"LEVEL\"}");
		legacy.validate();
		assertEquals("LEVEL", legacy.getSortid());
		assertSorters(BarSorter.defaultSorter, legacy.getEnabledBarSorters());
		PlayerConfig numericOnly = json.fromJson(PlayerConfig.class, "{\"sort\":4,\"sortid\":null}");
		numericOnly.validate();
		assertEquals("LEVEL", numericOnly.getSortid());
		PlayerConfig malformed = json.fromJson(PlayerConfig.class,
				"{\"enabledBarSorters\":[\"SCORE\",null,\"TITLE\",\"SCORE\"]}");
		malformed.validate();
		assertSorters(new BarSorter[] {BarSorter.SCORE, BarSorter.TITLE}, malformed.getEnabledBarSorters());
		PlayerConfig nullChoices = json.fromJson(PlayerConfig.class, "{\"enabledBarSorters\":null}");
		nullChoices.validate();
		assertSorters(BarSorter.defaultSorter, nullChoices.getEnabledBarSorters());
	}

	private static void assertSorters(BarSorter[] expected, BarSorter[] actual) {
		if (!Arrays.equals(expected, actual)) {
			throw new AssertionError("Expected " + Arrays.toString(expected) + " but was " + Arrays.toString(actual));
		}
	}

	private static void assertEquals(Object expected, Object actual) {
		if (!expected.equals(actual)) {
			throw new AssertionError("Expected " + expected + " but was " + actual);
		}
	}
}
