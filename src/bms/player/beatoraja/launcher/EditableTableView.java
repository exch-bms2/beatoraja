package bms.player.beatoraja.launcher;

import java.util.Arrays;
import javafx.scene.control.TableView;

/**
 * TableViewの拡張クラス
 * 
 * @author exch
 *
 * @param <T>
 */
public class EditableTableView<T> extends TableView<T>{

	public void addItem(T item) {
		getItems().add(item);
	}

	public void removeSelectedItems() {
		getItems().removeAll(getSelectionModel().getSelectedItems());
	}

	public void moveSelectedItemsUp() {
		int[] indices = getSelectionModel().getSelectedIndices().stream().mapToInt(i -> i).toArray();
		if (indices.length == 0) return;
		Arrays.sort(indices);
		int lastBlockIndex = 0;
		for (int i = 1; i <= indices.length; i++) {
			if (i == indices.length || indices[i] > indices[i-1] + 1) {
				if (indices[lastBlockIndex] > 0) {
					T item = getItems().get(indices[lastBlockIndex] - 1);
					getItems().remove(indices[lastBlockIndex] - 1);
					getItems().add(indices[i-1], item);
				}
				lastBlockIndex = i;
			}
		}

		for (int i = 0; i < indices.length; i++) {
			indices[i] -= 1;
		}
		if (indices[0] == -1) {
			indices[0] = 0;
			int j = 1;
			while (j < indices.length && indices[j] == indices[j-1]) {
				indices[j] += 1;
				j++;
			}
		}
		getSelectionModel().selectIndices(-1, indices);
	}

	public void moveSelectedItemsDown() {
		int[] indices = getSelectionModel().getSelectedIndices().stream().mapToInt(i -> i).toArray();
		if (indices.length == 0) return;
		Arrays.sort(indices);
		final int numItems = getItems().size();
		int lastBlockIndex = indices.length - 1;
		for (int i = indices.length - 2; i >= -1; i--) {
			if (i == -1 || indices[i] < indices[i+1] - 1) {
				if (indices[lastBlockIndex] < numItems - 1) {
					T item = getItems().get(indices[lastBlockIndex] + 1);
					getItems().remove(indices[lastBlockIndex] + 1);
					getItems().add(indices[i+1], item);
				}
				lastBlockIndex = i;
			}
		}

		for (int i = 0; i < indices.length; i++) {
			indices[i] += 1;
		}
		if (indices[indices.length - 1] == numItems) {
			indices[indices.length - 1] = numItems - 1;
			int j = indices.length - 2;
			while (j >= 0 && indices[j] == indices[j+1]) {
				indices[j] -= 1;
				j--;
			}
		}
		getSelectionModel().selectIndices(-1, indices);
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
