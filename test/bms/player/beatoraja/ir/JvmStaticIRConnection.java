package bms.player.beatoraja.ir;

/** Mirrors a Kotlin companion {@code @JvmStatic val NAME}: a public static getter, no public field (#800). */
public final class JvmStaticIRConnection extends TestIRConnectionBase {

	public static String getNAME() { return "Kotlin JvmStatic IR"; }
}
