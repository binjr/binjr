/*
 *    Copyright 2020 Frederic Thevenet
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

/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.core.controllers;

import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.core.data.timeseries.FacetEntry;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

public class FacetPillsContainer extends HBox implements Closeable {
    private final ObservableList<FacetPill> facetPills = FXCollections.observableArrayList();
    private final BindingManager bindingManager = new BindingManager();
    private final ObservableSet<FacetPill> selectedPills = FXCollections.observableSet();

    public FacetPillsContainer() {
        bindingManager.attachListener(facetPills, (ListChangeListener<FacetPill>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(pill -> {
                        bindingManager.attachListener(pill.selectedProperty(),
                                (ChangeListener<Boolean>) (o, oldVal, newVal) -> {
                                    if (newVal) {
                                        selectedPills.add(pill);
                                    } else {
                                        selectedPills.remove(pill);
                                    }
                                });
                        this.getChildren().add(pill);
                        if (pill.isSelected()) {
                            selectedPills.add(pill);
                        }
                    });
                }
                if (c.wasRemoved()) {
                    c.getRemoved().forEach(pill -> {
                        this.getChildren().remove(pill);
                        selectedPills.remove(pill);
                    });
                }
            }
        });
    }

    public FacetPill set(int index, FacetPill pill) {
        return facetPills.set(index, pill);
    }


    public boolean setAllEntries(Collection<FacetEntry> entries) {
        return setAll(entries.stream().map(FacetPill::new).collect(Collectors.toList()));
    }

    public boolean setAll(FacetPill... pills) {
        return facetPills.setAll(pills);
    }

    public boolean setAll(Collection<FacetPill> pills) {
        return facetPills.setAll(pills);
    }

    public boolean add(FacetEntry entry) {
        return add(new FacetPill(entry));
    }

    public boolean add(FacetPill pill) {
        return facetPills.add(pill);
    }

    public boolean addAll(FacetPill... pills) {
        return facetPills.addAll(pills);
    }

    public boolean addAll(Collection<FacetPill> pills) {
        return facetPills.addAll(pills);
    }

    public void remove(FacetPill pill) {
        facetPills.remove(pill);
    }

    public void removeAll(FacetPill... pills) {
        facetPills.removeAll(pills);
    }

    public void removeAll(Collection<FacetPill> pills) {
        facetPills.removeAll(pills);
    }

    public ObservableList<FacetPill> getFacetPills() {
        return FXCollections.unmodifiableObservableList(facetPills);
    }

    public ObservableSet<FacetPill> getSelectedPills() {
        return selectedPills;
    }

    @Override
    public void close() throws IOException {
        bindingManager.close();
    }


    public static class FacetPill extends CheckBox {
        private final FacetEntry facet;

        public FacetPill(FacetEntry facet) {
            this.getStyleClass().add("facet-pill");
            this.facet = facet;
            this.setText(facet.toString());
        }

        public FacetEntry getFacet() {
            return facet;
        }
    }
}
