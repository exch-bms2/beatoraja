package bms.player.beatoraja.launcher;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.util.StringConverter;

/**
 * JavaFX Spinnerの拡張クラス．
 * 
 * @author sack_magiclight
 *
 * @param <T>
 */
public class NumericSpinner<T> extends Spinner<T> {
	public NumericSpinner() {
		focusedProperty().addListener((s, ov, nv) -> {
			if (nv) {
				return;
			}
			commitEditorText(this);
		});
	}
	
	/**
	 * Spinnerの各種値を設定する
	 * 
	 * @param min Spinnerの最小値
	 * @param max Spinnerの最大値
	 * @param initialValue Spinnerの初期値
	 * @param amountToStepBy Spinnerの増減単位
	 */
	public void setValueFactoryValues(T min, T max, T initialValue, T amountToStepBy) {
		if(min instanceof Integer) {
			IntegerSpinnerValueFactory vf = (IntegerSpinnerValueFactory) this.getValueFactory();
			vf.setMin((Integer) min);
			vf.setMax((Integer) max);
			vf.setValue((Integer) initialValue);
			vf.setAmountToStepBy((Integer) amountToStepBy);
		}
		if(min instanceof Double) {
			DoubleSpinnerValueFactory vf = (DoubleSpinnerValueFactory) this.getValueFactory();
			vf.setMin((Double) min);
			vf.setMax((Double) max);
			vf.setValue((Double) initialValue);
			vf.setAmountToStepBy((Double) amountToStepBy);			
		}
	}

	private void commitEditorText(Spinner<T> spinner) {
		if (!spinner.isEditable()) {
			return;
		}
		String text = spinner.getEditor().getText();
		SpinnerValueFactory<T> valueFactory = spinner.getValueFactory();
		if (valueFactory != null) {
			StringConverter<T> converter = valueFactory.getConverter();
			if (converter != null) {
				try {
					T value = converter.fromString(text);
					setValue(valueFactory, value);
				} catch (Exception e) {
					spinner.getEditor().setText(valueFactory.getValue().toString());
				}
			}
		}
	}

	private void setValue(SpinnerValueFactory<T> valueFactory, T value) {
		if (valueFactory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory) {
			setValue((SpinnerValueFactory.IntegerSpinnerValueFactory) valueFactory, (Integer) value);
		} else if (valueFactory instanceof SpinnerValueFactory.DoubleSpinnerValueFactory) {
			setValue((SpinnerValueFactory.DoubleSpinnerValueFactory) valueFactory, (Double) value);
		}
		valueFactory.setValue(value);
	}

	private void setValue(SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory, Integer value) {
		valueFactory.setValue(Math.min(Math.max(value, valueFactory.getMin()), valueFactory.getMax()));
	}

	private void setValue(SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory, Double value) {
		valueFactory.setValue(Math.min(Math.max(value, valueFactory.getMin()), valueFactory.getMax()));
	}

}
