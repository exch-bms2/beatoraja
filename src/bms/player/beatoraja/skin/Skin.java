package bms.player.beatoraja.skin;

public class Skin {

	private SkinObject[] skinparts = new SkinObject[0];

	private SkinNumber[] numbers = new SkinNumber[0];

	public SkinObject[] getSkinPart() {
		return skinparts;
	}

	public void setSkinPart(SkinObject[] parts) {
		skinparts = parts;
	}

	public SkinNumber[] getSkinNumbers() {
		return numbers;
	}

	public void setSkinNumbers(SkinNumber[] numbers) {
		this.numbers = numbers;
	}
}
