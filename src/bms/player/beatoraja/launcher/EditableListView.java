package bms.player.beatoraja.launcher;

import javafx.scene.control.ListView;

/**
 * ListViewの拡張クラス
 * 
 * @author exch
 *
 * @param <T>
 */
public class EditableListView<T> extends ListView<T>{

	public void addItem(T item) {
		getItems().add(item);
	}

	public void removeSelectedItems() {
		getItems().removeAll(getSelectionModel().getSelectedItems());
	}

	public void moveSelectedItemUp() {
		final int index = getSelectionModel().getSelectedIndex();
		if(index > 0) {
			T item = getSelectionModel().getSelectedItem();
			getItems().remove(index);
			getItems().add(index - 1, item);
			getSelectionModel().select(index - 1);
		}
	}

	public void moveSelectedItemDown() {
		final int index = getSelectionModel().getSelectedIndex();
		if(index >= 0 && index < getItems().size() - 1) {
			T item = getSelectionModel().getSelectedItem();
			getItems().remove(index);
			getItems().add(index + 1, item);
			getSelectionModel().select(index + 1);
		}
	}

	public void moveSelectedItemTop() {
		final int index = getSelectionModel().getSelectedIndex();
		if(index > 0) {
			T item = getSelectionModel().getSelectedItem();
			getItems().remove(index);
			getItems().add(0, item);
			getSelectionModel().select(0);
		}
	}

	public void moveSelectedItemBottom() {
		final int index = getSelectionModel().getSelectedIndex();
		if(index >= 0 && index < getItems().size() - 1) {
			T item = getSelectionModel().getSelectedItem();
			getItems().remove(index);
			getItems().add(item);
			getSelectionModel().select(getItems().size() - 1);
		}
	}
}
