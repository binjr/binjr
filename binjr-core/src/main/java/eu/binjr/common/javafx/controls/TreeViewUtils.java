/*
 *    Copyright 2017-2018 Frederic Thevenet
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
 */

package eu.binjr.common.javafx.controls;

import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.EnumSet.of;

/**
 * Helper methods to walk JavaFX TreeViews
 *
 * @author Frederic Thevenet
 */
public class TreeViewUtils {
    public enum ExpandDirection {
        UP,
        DOWN,
        BOTH
    }

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

    public static <T> List<T> flattenLeaves(TreeItem<T> branch) {
        return flattenLeaves(branch, false);
    }

    public static <T> List<T> flattenLeaves(TreeItem<T> branch, boolean expand) {
        List<T> leaves = new ArrayList<>();
        flattenLeaves(branch, leaves, expand);
        return leaves;
    }

    private static <T> void flattenLeaves(TreeItem<T> branch, List<T> leaves, boolean expand) {
        if (expand) {
            branch.setExpanded(true);
        }
        if (!branch.isLeaf()) {
            for (TreeItem<T> t : branch.getChildren()) {
                flattenLeaves(t, leaves, expand);
            }
        } else {
            leaves.add(branch.getValue());
        }
    }

    public static <T> List<TreeItem<T>> splitAboveLeaves(TreeItem<T> branch) {
        return splitAboveLeaves(branch, false);
    }

    public static <T> List<TreeItem<T>> splitAboveLeaves(TreeItem<T> branch, boolean expand) {
        List<TreeItem<T>> level1Items = new ArrayList<>();
        if (branch.isLeaf()) {
            level1Items.add(branch);
        } else {
            splitAboveLeaves(branch, level1Items, expand);
        }
        return level1Items;
    }

    //FIXME: The following implementation assumes that a leaf node cannot have a sibling which isn't also a leaf.
    private static <T> void splitAboveLeaves(TreeItem<T> branch, List<TreeItem<T>> level1Items, boolean expand) {
        if (expand) {
            branch.setExpanded(true);
        }
        if (!branch.isLeaf() && branch.getChildren().get(0).isLeaf()) {
            level1Items.add(branch);
        } else {
            branch.setExpanded(true);
            for (TreeItem<T> t : branch.getChildren()) {
                splitAboveLeaves(t, level1Items, expand);
            }
        }
    }

    public static <T> void expandBranch(TreeItem<T> branch, ExpandDirection direction) {
        if (EnumSet.of(ExpandDirection.BOTH, ExpandDirection.UP).contains(direction)) {
            climbUpFromBranch(branch, true);
        }
        if (EnumSet.of(ExpandDirection.BOTH, ExpandDirection.DOWN).contains(direction)) {
            climbDownFromBranch(branch, true);
        }
    }

    public static <T> void collapseBranch(TreeItem<T> branch, ExpandDirection direction) {
        if (EnumSet.of(ExpandDirection.BOTH, ExpandDirection.UP).contains(direction)) {
            climbUpFromBranch(branch, false);
        }
        if (EnumSet.of(ExpandDirection.BOTH, ExpandDirection.DOWN).contains(direction)) {
            climbDownFromBranch(branch, false);
        }
    }

    private static <T> void climbUpFromBranch(TreeItem<T> branch, boolean expanded) {
        if (branch == null) {
            return;
        }
        branch.setExpanded(expanded);
        climbUpFromBranch(branch.getParent(), expanded);
    }

    private static <T> void climbDownFromBranch(TreeItem<T> branch, boolean expanded) {
        if (branch == null) {
            return;
        }
        branch.setExpanded(expanded);
        if (branch.getChildren() != null) {
            for (TreeItem<?> item : branch.getChildren()) {
                climbDownFromBranch(item, expanded);
            }
        }
    }


}
