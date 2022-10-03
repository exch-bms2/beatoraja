package bms.player.beatoraja.launcher;

import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaFXUtils {
    /**
     * 自身または再帰的に探索した親ノードのクラスの単純名が {@code className} と同じなら、
     * そのノードを {@code Optional} にラップして返します。
     * 見つからなかった場合は空の {@code Optional} を返します。
     *
     * @param node 探索するノード
     */
    public static <T extends Node> Optional<T> findParentByClassSimpleName(final Node node, final String className) {
        // 無限ループ対策
        final List<Node> parentList = new ArrayList<>();
        Node targetNode = node;

        while (targetNode != null && !parentList.contains(targetNode)) {
            if (targetNode.getClass().getSimpleName().equals(className)) {
                return Optional.of((T) targetNode);
            }
            parentList.add(targetNode);
            targetNode = targetNode.getParent();
        }
        return Optional.empty();
    }
}
