package bms.player.beatoraja.launcher;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Skin;
import javafx.scene.control.Spinner;
import javafx.scene.control.skin.SpinnerSkin;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

/**
 * JavaFX Spinnerの拡張クラス．
 * 
 * @author sack_magiclight
 *
 * @param <T>
 */
public class NumericSpinner<T> extends Spinner<T> {
	private static final double PRESET_BUTTON_WIDTH = 38;
	private final ContextMenu presetMenu = new ContextMenu();
	private final List<String> presetValues = new ArrayList<>();
	private StackPane presetArrowButton;

	public NumericSpinner() {
		presetMenu.setOnShown(event -> presetMenu.getScene().setFill(Color.TRANSPARENT));

		focusedProperty().addListener((s, ov, nv) -> {
			if (nv) {
				return;
			}
			commitEditorText(this);
		});

		addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (!presetValues.isEmpty() && event.getX() >= getWidth() - PRESET_BUTTON_WIDTH) {
				commitEditorText(this);
				event.consume();
			}
		});
		addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
			if (!presetValues.isEmpty() && event.getX() >= getWidth() - PRESET_BUTTON_WIDTH) {
				showPresetMenu();
				event.consume();
			}
		});
	}

	/**
	 * Adds a compact preset picker while keeping the editor available for custom values.
	 * Values are supplied as a comma-separated FXML attribute.
	 */
	public void setPresetValues(String values) {
		presetValues.clear();
		if (values != null) {
			for (String value : values.split(",")) {
				String trimmed = value.trim();
				if (!trimmed.isEmpty()) {
					presetValues.add(trimmed);
				}
			}
		}
		if (!presetValues.isEmpty() && !getStyleClass().contains("preset-spinner")) {
			getStyleClass().add("preset-spinner");
		}
		if (presetArrowButton != null) {
			presetArrowButton.setVisible(!presetValues.isEmpty());
		}
	}

	public String getPresetValues() {
		return String.join(",", presetValues);
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new PresetSpinnerSkin(this);
	}

	private final class PresetSpinnerSkin extends SpinnerSkin<T> {
		private PresetSpinnerSkin(NumericSpinner<T> spinner) {
			super(spinner);
			Region arrow = new Region();
			arrow.getStyleClass().add("preset-spinner-arrow");
			presetArrowButton = new StackPane(arrow);
			presetArrowButton.getStyleClass().add("preset-spinner-arrow-button");
			presetArrowButton.setMouseTransparent(true);
			presetArrowButton.setVisible(!presetValues.isEmpty());
			getChildren().add(presetArrowButton);
		}

		@Override
		protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
			super.layoutChildren(contentX, contentY, contentWidth, contentHeight);
			presetArrowButton.resizeRelocate(
					contentX + contentWidth - PRESET_BUTTON_WIDTH,
					contentY,
					PRESET_BUTTON_WIDTH,
					contentHeight);
		}
	}

	private void showPresetMenu() {
		presetMenu.getItems().clear();
		presetMenu.setPrefWidth(getWidth());
		SpinnerValueFactory<T> valueFactory = getValueFactory();
		if (valueFactory == null || valueFactory.getConverter() == null) {
			return;
		}

		for (String preset : presetValues) {
			T value;
			try {
				value = valueFactory.getConverter().fromString(preset);
			} catch (RuntimeException exception) {
				continue;
			}
			MenuItem item = new MenuItem();
			Label presetLabel = new Label(preset);
			presetLabel.setMinWidth(Math.max(120, getWidth() - 58));
			item.setGraphic(presetLabel);
			item.getStyleClass().add("preset-menu-item");
			if (value.equals(valueFactory.getValue())) {
				item.getStyleClass().add("selected-preset");
			}
			item.setOnAction(event -> {
				setValue(valueFactory, value);
				getEditor().setText(valueFactory.getConverter().toString(valueFactory.getValue()));
			});
			presetMenu.getItems().add(item);
		}
		presetMenu.show(this, Side.BOTTOM, 0, 4);
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
