package bms.player.beatoraja.launcher;

import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.beans.value.WritableValue;

/**
 * TableCell用 NumericSpinner
 */
public final class SpinnerCell extends TableCell<ControllerConfigViewModel, Integer> {
    private final NumericSpinner<Integer> spinner;

    SpinnerCell(int min, int max, int initial, int step) {
        spinner = new NumericSpinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial, step));
        spinner.setEditable(true);
        spinner.valueProperty().addListener((o, oldValue, newValue) -> {
            WritableValue<Integer> cellProperty = (WritableValue<Integer>)getTableColumn().getCellObservableValue((ControllerConfigViewModel)getTableRow().getItem());
            cellProperty.setValue(newValue);
        });
    }

    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            spinner.getValueFactory().setValue(item);
            setGraphic(spinner);
        }
    }
}