package bms.player.beatoraja;

public class PointCalculatorFactory extends SuperPointCalculatorFactory{

	@Override
	public IPointCalculator createFacotry(CalcuatorType type) {
		IPointCalculator calculator = null;
		switch(type) {
		case ORIGIN:
			calculator = new OriginPointCalculator();
			break;
		default:
			break;
		}
		return calculator;
	}
}
