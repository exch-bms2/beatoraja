package bms.player.beatoraja.launcher;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

public class NumericSpinner<T> extends Spinner<T> {
	public NumericSpinner() {
		focusedProperty().addListener((s, ov, nv) -> {
			if (nv) {
				return;
			}
			commitEditorText(this);
		});
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
