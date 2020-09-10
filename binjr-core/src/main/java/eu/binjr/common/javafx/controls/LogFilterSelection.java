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

package eu.binjr.common.javafx.controls;

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.LogFilter;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.controlsfx.control.CheckComboBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LogFilterSelection extends ToggleButton {
    private static final Logger logger = Logger.create(LogFilterSelection.class);
    private final Label timeRangeText;
    private LogFilterSelectionController controller;
    private final PopupControl popup;

    private final Property<LogFilter> filter = new SimpleObjectProperty<>(LogFilter.empty());

    private final Property<String> filteringQuery = new SimpleStringProperty("");

    public LogFilterSelection() throws IOException {
        super();
        timeRangeText = new Label("Click here to filter log events");
        HBox graphic = new HBox();
        graphic.setPadding(new Insets(0, 10, 0, 10));
        graphic.setAlignment(Pos.CENTER);
        graphic.getStyleClass().addAll("icon-container");
        graphic.setSpacing(15.0);
        graphic.getChildren().addAll(
                ToolButtonBuilder.makeIconNode(Pos.CENTER, "filter-icon", "medium-icon"),
                timeRangeText,
                ToolButtonBuilder.makeIconNode(Pos.CENTER, "combo-box-arrow-icon")
        );
        this.setGraphic(graphic);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/eu/binjr/views/LogFilterSelection.fxml"));
        this.controller = new LogFilterSelectionController();
        loader.setController(controller);
        Pane timeRangePickerPane = loader.load();
        popup = new PopupControl();
        popup.setAutoHide(true);
        popup.getScene().setRoot(timeRangePickerPane);
        popup.showingProperty().addListener((observable, oldValue, newValue) -> {
            setSelected(newValue);
        });
        this.setOnAction(actionEvent -> {
            Node owner = (Node) actionEvent.getSource();
            Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            this.popup.show(owner.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY());
        });

        controller.severityComboBox
                .getCheckModel()
                .getCheckedItems()
                .addListener((InvalidationListener)l-> filter.setValue(
                        new LogFilter(controller.filterTextField.getText(),
                                controller.severityComboBox.getCheckModel().getCheckedItems())));
//                .addListener((ListChangeListener<String>) c -> {
//                    while (c.next()) {
//                        if (c.wasAdded()) {
//                            c.getAddedSubList().forEach(tab -> {
//
//                            });
//                        }
//                        if (c.wasRemoved()) {
//                            c.getRemoved().forEach((t -> {
//
//                            }));
//                        }
//                    }
//                });
    }

    public void setSeverityLabels(String... labels){
        controller.severityComboBox.getItems().setAll(labels);
    }

    public ObservableList<String> getSelectedSeverities() {
        return controller.severityComboBox.getCheckModel().getCheckedItems();
    }

    public LogFilter getFilter() {
        return filter.getValue();
    }

    public Property<LogFilter> filterProperty() {
        return filter;
    }

    private class LogFilterSelectionController implements Initializable {
        @FXML
        CheckComboBox<String> severityComboBox;
        @FXML
        private TextField filterTextField;

        @FXML
        private Button  clearFilterButton;

        @FXML
        private Button filterHistoryButton;

        public ObservableList<String> logLevelList() {
            return severityComboBox.getItems();
        }

        @Override
        public void initialize(URL location, ResourceBundle resources) {

        }
    }
}
