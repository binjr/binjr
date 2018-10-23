/*
 *    Copyright 2018 Frederic Thevenet
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

import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeRangePicker extends ToggleButton {
    private final Pane timeRangePickerPane;
    private final TimeRangePickerController timeRangePickerController;
    private final PopupControl popup;

    private final Property<ZoneId> zoneId = new SimpleObjectProperty<>(ZoneId.systemDefault());

    public TimeRangePicker() throws IOException {
        super();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/TimeRangePickerView.fxml"));
        this.timeRangePickerController = new TimeRangePickerController();
        loader.setController(timeRangePickerController);
        this.timeRangePickerPane = loader.load();
        popup = new PopupControl();
        popup.setAutoHide(true);
        popup.getScene().setRoot(timeRangePickerPane);
        popup.showingProperty().addListener((observable, oldValue, newValue) -> {
            setSelected(newValue);
        });

        timeRangePickerController.startDate.zoneIdProperty().bind(zoneId);
        timeRangePickerController.endDate.zoneIdProperty().bind(zoneId);

        timeRangePickerController.startDate.dateTimeValueProperty().addListener(this::intervalDescriptionChanged);
        timeRangePickerController.endDate.dateTimeValueProperty().addListener(this::intervalDescriptionChanged);
        zoneId.addListener(this::intervalDescriptionChanged);

        timeRangePickerController.zoneIdProperty().bindBidirectional(zoneId);

//        zoneId.addListener((observable, oldValue, newValue) -> {
//            if (newValue !=null){
//                timeRangePickerController.zoneIdProperty().setValue(newValue);
//            }
//        });
//
//        timeRangePickerController.zoneIdProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                zoneId.setValue(newValue);
//            }
//        });
//        timeRangePickerController.zoneIdProperty().setValue(zoneId.getValue());
//
//        startDate.addListener( (observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                timeRangePickerController.startDate.setDateTimeValue(newValue);
//            }
//        });
//        timeRangePickerController.startDate.dateTimeValueProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                startDate.setValue(newValue);
//            }
//        });
//
//        endDate.addListener( (observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                timeRangePickerController.endDate.setDateTimeValue(newValue);
//            }
//        });
//        timeRangePickerController.endDate.dateTimeValueProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                endDate.setValue(newValue);
//            }
//        });
//        timeRangePickerController.startDate.setDateTimeValue(startDate.getValue());
//        timeRangePickerController.endDate.setDateTimeValue(endDate.getValue());
//
//

        this.setOnAction(actionEvent -> {
            Node owner = (Node) actionEvent.getSource();
            Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            this.popup.show(owner.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY());
        });
    }

    public ZonedDateTime getStartDate() {
        return timeRangePickerController.startDate.getDateTimeValue();
    }

    public void setStartDate(ZonedDateTime value) {
        timeRangePickerController.startDate.setDateTimeValue(value);
    }

    public Property<ZonedDateTime> startDateProperty() {
        return timeRangePickerController.startDate.dateTimeValueProperty();
    }

    public ZonedDateTime getEndDate() {
        return timeRangePickerController.startDate.getDateTimeValue();
    }

    public void setEndDate(ZonedDateTime value) {
        timeRangePickerController.endDate.setDateTimeValue(value);
    }

    public Property<ZonedDateTime> endDateProperty() {
        return timeRangePickerController.endDate.dateTimeValueProperty();
    }

    public ZoneId getZoneId() {
        return zoneId.getValue();
    }

    public Property<ZoneId> zoneIdProperty() {
        return zoneId;
    }

    private void intervalDescriptionChanged(Observable observable) {
        setText(String.format("From %s to %s (%s)",
                timeRangePickerController.startDate.getDateTimeValue().format(timeRangePickerController.startDate.getFormatter()),
                timeRangePickerController.endDate.getDateTimeValue().format(timeRangePickerController.endDate.getFormatter()),
                getZoneId().toString()));
    }
}
