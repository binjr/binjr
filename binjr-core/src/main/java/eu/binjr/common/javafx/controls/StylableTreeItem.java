/*
 *    Copyright 2021 Frederic Thevenet
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

import javafx.css.PseudoClass;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StylableTreeItem {
    private final String label;
    private final List<PseudoClass> pseudoClasses;

    public StylableTreeItem(String label, String... pseudoClasses) {
        this.label = label;
        this.pseudoClasses = Stream.of(pseudoClasses).map(PseudoClass::getPseudoClass).collect(Collectors.toList());
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public List<PseudoClass> getPseudoClasses() {
        return pseudoClasses;
    }

    public static <T extends StylableTreeItem>  void setCellFactory(TreeView<T> treeView){
        treeView.setCellFactory(tv -> new TreeCell<T>() {
            private final Set<PseudoClass> pseudoClassesSet = new HashSet<>();
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                textProperty().unbind();

                pseudoClassesSet.forEach(pc -> pseudoClassStateChanged(pc, false));
                if (empty) {
                    setText("");
                    setGraphic(null);
                } else {
                    setText(item.getLabel());
                    setGraphic(getTreeItem().getGraphic());
                    List<PseudoClass> itemPC = item.getPseudoClasses();
                    if (itemPC != null) {
                        itemPC.forEach(pc->{
                            pseudoClassStateChanged(pc, true);
                            pseudoClassesSet.add(pc);
                        });
                    }
                }
            }
        });
    }
}
