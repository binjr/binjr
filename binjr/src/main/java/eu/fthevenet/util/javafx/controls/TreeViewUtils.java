/*
 *    Copyright 2017 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package eu.fthevenet.util.javafx.controls;

import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Helper methods to walk JavaFX TreeViews
 *
 * @author Frederic Thevenet
 */
public class TreeViewUtils {
    /**
     * Finds the first tree item that matches the provided predicate.
     *
     * @param currentTreeItem the node in the tree where to start searching.
     * @param predicate       the predicate onto witch the search is based.
     * @param <T>             the type for the tree item
     * @return an {@link Optional} encapsulating the {@link TreeItem} instance matching the predicate.
     */
    public static <T> Optional<TreeItem<T>> findFirstInTree(TreeItem<T> currentTreeItem, Predicate<TreeItem<T>> predicate) {
        if (predicate.test(currentTreeItem)) {
            return Optional.of(currentTreeItem);
        }
        if (!currentTreeItem.isLeaf()) {
            for (TreeItem<T> item : currentTreeItem.getChildren()) {
                Optional<TreeItem<T>> res = findFirstInTree(item, predicate);
                if (res.isPresent()) {
                    return res;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds all tree items that match the provided predicate.
     *
     * @param currentTreeItem the node in the tree where to start searching.
     * @param predicate       the predicate onto witch the search is based.
     * @param <T>             the type for the tree item
     * @return a list of {@link Optional} encapsulating the {@link TreeItem} instances matching the predicate.
     */
    public static <T> List<TreeItem<T>> findAllInTree(TreeItem<T> currentTreeItem, Predicate<TreeItem<T>> predicate) {
        return findAllInTree(currentTreeItem, predicate, new ArrayList<>());
    }

    private static <T> List<TreeItem<T>> findAllInTree(TreeItem<T> currentTreeItem, Predicate<TreeItem<T>> predicate, List<TreeItem<T>> found) {
        if (predicate.test(currentTreeItem)) {
            found.add(currentTreeItem);
        }
        if (!currentTreeItem.isLeaf()) {
            for (TreeItem<T> item : currentTreeItem.getChildren()) {
                findAllInTree(item, predicate, found);
            }
        }
        return found;
    }
}
