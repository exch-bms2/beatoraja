package bms.player.beatoraja.skin.json;

import java.util.Set;

/** Which sources are prefetched: only those reachable from destinations. */
public final class JsonSkinSourceReferencesTest {

	private JsonSkinSourceReferencesTest() {
	}

	public static void main(String[] args) {
		directImageReference();
		imageSetAndGaugeNodes();
		judgeImagesNestedInDestinations();
		unreferencedSourceIsSkipped();
		singleObjectDefinitions();
		cycleTerminates();
	}

	private static JsonSkin.Skin skin(String... sourceIds) {
		JsonSkin.Skin sk = new JsonSkin.Skin();
		sk.source = new JsonSkin.Source[sourceIds.length];
		for (int i = 0; i < sourceIds.length; i++) {
			sk.source[i] = new JsonSkin.Source();
			sk.source[i].id = sourceIds[i];
			sk.source[i].path = "img" + i + ".png";
		}
		sk.destination = new JsonSkin.Destination[0];
		return sk;
	}

	private static JsonSkin.Destination dst(String id) {
		JsonSkin.Destination d = new JsonSkin.Destination();
		d.id = id;
		return d;
	}

	private static JsonSkin.Image image(String id, String src) {
		JsonSkin.Image img = new JsonSkin.Image();
		img.id = id;
		img.src = src;
		return img;
	}

	private static void directImageReference() {
		JsonSkin.Skin sk = skin("s1", "s2");
		sk.image = new JsonSkin.Image[] { image("bg", "s1") };
		sk.destination = new JsonSkin.Destination[] { dst("bg") };
		assertEquals(Set.of("s1"), JsonSkinSourceReferences.collect(sk));
	}

	private static void imageSetAndGaugeNodes() {
		JsonSkin.Skin sk = skin("a", "b", "c");
		sk.image = new JsonSkin.Image[] { image("i1", "a"), image("i2", "b"), image("n1", "c") };
		JsonSkin.ImageSet set = new JsonSkin.ImageSet();
		set.id = "set";
		set.images = new String[] { "i1", "i2" };
		sk.imageset = new JsonSkin.ImageSet[] { set };
		JsonSkin.Gauge gauge = new JsonSkin.Gauge();
		gauge.id = "gauge";
		gauge.nodes = new String[] { "n1" };
		sk.gauge = gauge;
		sk.destination = new JsonSkin.Destination[] { dst("set"), dst("gauge") };
		assertEquals(Set.of("a", "b", "c"), JsonSkinSourceReferences.collect(sk));
	}

	private static void judgeImagesNestedInDestinations() {
		JsonSkin.Skin sk = skin("judgesrc");
		sk.image = new JsonSkin.Image[] { image("pg", "judgesrc") };
		JsonSkin.Judge judge = new JsonSkin.Judge();
		judge.id = "judge";
		judge.images = new JsonSkin.Destination[] { dst("pg") };
		sk.judge = new JsonSkin.Judge[] { judge };
		sk.destination = new JsonSkin.Destination[] { dst("judge") };
		assertEquals(Set.of("judgesrc"), JsonSkinSourceReferences.collect(sk));
	}

	private static void unreferencedSourceIsSkipped() {
		JsonSkin.Skin sk = skin("used", "unused");
		sk.image = new JsonSkin.Image[] { image("a", "used"), image("b", "unused") };
		sk.destination = new JsonSkin.Destination[] { dst("a") };
		assertEquals(Set.of("used"), JsonSkinSourceReferences.collect(sk));
	}

	private static void singleObjectDefinitions() {
		JsonSkin.Skin sk = skin("lane");
		sk.image = new JsonSkin.Image[] { image("note-img", "lane") };
		JsonSkin.NoteSet notes = new JsonSkin.NoteSet();
		notes.id = "notes";
		notes.note = new String[] { "note-img" };
		sk.note = notes;
		sk.destination = new JsonSkin.Destination[] { dst("notes") };
		assertEquals(Set.of("lane"), JsonSkinSourceReferences.collect(sk));
	}

	private static void cycleTerminates() {
		JsonSkin.Skin sk = skin("x");
		JsonSkin.ImageSet a = new JsonSkin.ImageSet();
		a.id = "a";
		a.images = new String[] { "b", "img" };
		JsonSkin.ImageSet b = new JsonSkin.ImageSet();
		b.id = "b";
		b.images = new String[] { "a" };
		sk.imageset = new JsonSkin.ImageSet[] { a, b };
		sk.image = new JsonSkin.Image[] { image("img", "x") };
		sk.destination = new JsonSkin.Destination[] { dst("a") };
		assertEquals(Set.of("x"), JsonSkinSourceReferences.collect(sk));
	}

	private static void assertEquals(Set<String> expected, Set<String> actual) {
		if (!expected.equals(actual)) {
			throw new AssertionError("expected " + expected + " but was " + actual);
		}
	}
}
