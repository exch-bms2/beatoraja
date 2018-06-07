package bms.player.beatoraja;


public abstract class SuperPointCalculatorFactory {
	abstract public IPointCalculator createFacotry(CalcuatorType type);

	public enum CalcuatorType{
		ORIGIN
	}
}
