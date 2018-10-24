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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.TextFields;

import java.io.IOException;
import java.net.URL;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

public class TimeRangePicker extends ToggleButton {
    private final TimeRangePickerController timeRangePickerController;
    private final PopupControl popup;
    private final Property<ZoneId> zoneId = new SimpleObjectProperty<>(ZoneId.systemDefault());

    public TimeRangePicker() throws IOException {
        super();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/TimeRangePickerView.fxml"));
        this.timeRangePickerController = new TimeRangePickerController();
        loader.setController(timeRangePickerController);
        Pane timeRangePickerPane = loader.load();
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
        this.setOnAction(actionEvent -> {
            Node owner = (Node) actionEvent.getSource();
            Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            this.popup.show(owner.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY());
        });
    }

    public void setApplyNewTimeRange(BiConsumer<ZonedDateTime, ZonedDateTime> applyNewTimeRange) {
        timeRangePickerController.applyNewTimeRange = applyNewTimeRange;
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

    private class TimeRangePickerController {

        @FXML
        private ResourceBundle resources;

        @FXML
        private URL location;

        @FXML
        private AnchorPane root;

        @FXML
        private Button previousIntervalBtn;

        @FXML
        private ZonedDateTimePicker startDate;

        @FXML
        private ZonedDateTimePicker endDate;

        @FXML
        private Button nextIntervalBtn;

        @FXML
        private TextField timezoneField;

        @FXML
        private Button last6Hours;
        @FXML
        private Button last3Hours;
        @FXML
        private Button last24Hours;
        @FXML
        private Button last7Days;
        @FXML
        private Button last15Days;
        @FXML
        private Button last30Days;
        @FXML
        private Button last90Days;
        @FXML
        private Button last15Minutes;
        @FXML
        private Button last30Minutes;
        @FXML
        private Button last60Minutes;
        @FXML
        private Button last12Hours;
        @FXML
        private Button last90Minutes;

        @FXML
        private Button today;
        @FXML
        private Button yesterday;
        @FXML
        private Button thisWeek;
        @FXML
        private Button lastWeek;


        private TextFormatter<ZoneId> formatter;

        private BiConsumer<ZonedDateTime, ZonedDateTime> applyNewTimeRange = (start, end) -> {
            startDate.dateTimeValueProperty().setValue(start);
            endDate.dateTimeValueProperty().setValue(end);
        };

        private void stepBy(Duration intervalDuration) {
            applyNewTimeRange.accept(startDate.getDateTimeValue().plus(intervalDuration), endDate.getDateTimeValue().plus(intervalDuration));
        }

        private void last(Duration duration) {
            ZonedDateTime end = ZonedDateTime.now(zoneId.getValue());
            applyNewTimeRange.accept(end.minus(duration), end);
        }

        @FXML
        void initialize() {
            formatter = new TextFormatter<ZoneId>(new StringConverter<ZoneId>() {
                @Override
                public String toString(ZoneId object) {
                    if (object == null) {
                        return "null";
                    }
                    return object.toString();
                }

                @Override
                public ZoneId fromString(String string) {
                    return ZoneId.of(string);
                }
            });
            TextFields.bindAutoCompletion(timezoneField, ZoneId.getAvailableZoneIds());
            timezoneField.setTextFormatter(formatter);
            nextIntervalBtn.setOnAction(event -> {
                stepBy(Duration.between(startDate.getDateTimeValue(), endDate.getDateTimeValue()));
            });
            previousIntervalBtn.setOnAction(event -> {
                stepBy(Duration.between(endDate.getDateTimeValue(), startDate.getDateTimeValue()));
            });
            last3Hours.setOnAction(event -> last(Duration.of(3, ChronoUnit.HOURS)));
            last6Hours.setOnAction(event -> last(Duration.of(6, ChronoUnit.HOURS)));
            last12Hours.setOnAction(event -> last(Duration.of(12, ChronoUnit.HOURS)));
            last24Hours.setOnAction(event -> last(Duration.of(24, ChronoUnit.HOURS)));
            last7Days.setOnAction(event -> last(Duration.of(7, ChronoUnit.DAYS)));
            last15Days.setOnAction(event -> last(Duration.of(15, ChronoUnit.DAYS)));
            last30Days.setOnAction(event -> last(Duration.of(30, ChronoUnit.DAYS)));
            last90Days.setOnAction(event -> last(Duration.of(90, ChronoUnit.DAYS)));
            last15Minutes.setOnAction(event -> last(Duration.of(15, ChronoUnit.MINUTES)));
            last30Minutes.setOnAction(event -> last(Duration.of(30, ChronoUnit.MINUTES)));
            last60Minutes.setOnAction(event -> last(Duration.of(60, ChronoUnit.MINUTES)));
            last90Minutes.setOnAction(event -> last(Duration.of(90, ChronoUnit.MINUTES)));

            today.setOnAction(event -> {
                LocalDate today = ZonedDateTime.now(zoneId.getValue()).toLocalDate();
                applyNewTimeRange.accept(ZonedDateTime.of(today, LocalTime.MIDNIGHT, zoneId.getValue()), ZonedDateTime.of(today.plusDays(1), LocalTime.MIDNIGHT, zoneId.getValue()));
            });
            yesterday.setOnAction(event -> {
                LocalDate today = ZonedDateTime.now(zoneId.getValue()).toLocalDate();
                applyNewTimeRange.accept(ZonedDateTime.of(today.minusDays(1), LocalTime.MIDNIGHT, zoneId.getValue()), ZonedDateTime.of(today, LocalTime.MIDNIGHT, zoneId.getValue()));
            });

            thisWeek.setOnAction(event -> {
                LocalDate refDay = ZonedDateTime.now(zoneId.getValue()).toLocalDate();
                int n = refDay.getDayOfWeek().getValue();
                applyNewTimeRange.accept(
                        ZonedDateTime.of(refDay.minusDays(n - 1), LocalTime.MIDNIGHT, zoneId.getValue()),
                        ZonedDateTime.of(refDay.plusDays(8 - n), LocalTime.MIDNIGHT, zoneId.getValue()));
            });
            lastWeek.setOnAction(event -> {
                LocalDate refDay = ZonedDateTime.now(zoneId.getValue()).minusWeeks(1).toLocalDate();
                int n = refDay.getDayOfWeek().getValue();
                applyNewTimeRange.accept(
                        ZonedDateTime.of(refDay.minusDays(n - 1), LocalTime.MIDNIGHT, zoneId.getValue()),
                        ZonedDateTime.of(refDay.plusDays(8 - n), LocalTime.MIDNIGHT, zoneId.getValue()));
            });
        }

        Property<ZoneId> zoneIdProperty() {
            return formatter.valueProperty();
        }
    }
}
