package bms.player.beatoraja.launcher;

import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JavaFXUtils {
    /**
     * 自身または再帰的に探索した親ノードが {@code clazz} のインスタンスなら、
     * そのノードを {@code Optional} にラップして返します。
     * 見つからなかった場合は空の {@code Optional} を返します。
     *
     * @param node 探索するノード
     * @param clazz クラス
     * @param <T> クラスの型
     * @return 見つかったノードが入った、または空のOptional
     */
    public static <T extends Node> Optional<T> findParentByClass(final Node node, final Class<T> clazz) {
        // 無限ループ対策
        final List<Node> parentList = new ArrayList<>();
        Node targetNode = node;

        while (targetNode != null && !parentList.contains(targetNode)) {
            if (clazz.isInstance(targetNode)) {
                return Optional.ofNullable((T) targetNode);
            }
            parentList.add(targetNode);
            targetNode = targetNode.getParent();
        }
        return Optional.empty();
    }
}
