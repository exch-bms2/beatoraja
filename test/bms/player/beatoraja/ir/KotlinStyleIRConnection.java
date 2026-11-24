package bms.player.beatoraja.ir;

/**
 * Mirrors what kotlinc emits for {@code companion object { val NAME = ...; val HOME = ... }}:
 * the values are private static fields, reachable only through the public Companion getters (#800).
 */
public final class KotlinStyleIRConnection extends TestIRConnectionBase {

	@SuppressWarnings("unused")
	private static final String NAME = "Kotlin Companion IR";
	@SuppressWarnings("unused")
	private static final String HOME = "https://kotlin.example/";

	public static final Companion Companion = new Companion();

	public static final class Companion {
		public String getNAME() { return NAME; }
		public String getHOME() { return HOME; }
	}
}
