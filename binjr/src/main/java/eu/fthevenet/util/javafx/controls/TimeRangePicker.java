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

import eu.fthevenet.util.javafx.bindings.BindingManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.textfield.TextFields;

import java.io.IOException;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

public class TimeRangePicker extends ToggleButton {
    private static final Logger logger = LogManager.getLogger(TimeRangePicker.class);
    private TimeRangePickerController timeRangePickerController;
    private final PopupControl popup;
    private final Property<ZoneId> zoneId = new SimpleObjectProperty<>(ZoneId.systemDefault());
    private final Property<TimeRange> timeRange = new SimpleObjectProperty<>(TimeRange.of(ZonedDateTime.now(), ZonedDateTime.now()));
    private final ObjectProperty<TimeRange> selectedRange = new SimpleObjectProperty<>(TimeRange.of(ZonedDateTime.now().minusHours(1), ZonedDateTime.now()));
    private final BindingManager bindingManager = new BindingManager();


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

        this.setOnAction(actionEvent -> {
            Node owner = (Node) actionEvent.getSource();
            Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            this.popup.show(owner.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY());
        });

        timeRangePickerController.applyNewTimeRange = (beginning, end) -> {
            this.selectedRange.setValue(TimeRange.of(beginning, end));
        };

        timeRangePickerController.startDate.setDateTimeValue(selectedRange.getValue().getBeginning());
        timeRangePickerController.endDate.setDateTimeValue(selectedRange.getValue().getEnd());

        bindingManager.bind(timeRangePickerController.startDate.zoneIdProperty(), zoneId);
        bindingManager.bind(timeRangePickerController.endDate.zoneIdProperty(), zoneId);
        bindingManager.bindBidirectionnal(timeRangePickerController.zoneIdProperty(), zoneId);
        bindingManager.attachListener(zoneId, (observable, oldValue, newValue) -> updateText());

        bindingManager.attachListener(selectedRange, (ChangeListener<TimeRange>) (observable, oldValue, newValue) -> {
            if (newValue != null) {
                bindingManager.suspend();
                try {
                    zoneId.setValue(newValue.zoneId);
                    timeRangePickerController.startDate.setDateTimeValue(newValue.getBeginning());
                    timeRangePickerController.endDate.setDateTimeValue(newValue.getEnd());
                } finally {
                    bindingManager.resume();
                    updateText();
                }
            }
        });
        bindingManager.attachListener(timeRangePickerController.startDate.dateTimeValueProperty(), (ChangeListener<ZonedDateTime>) (observable, oldValue, newValue) -> {
            if (newValue != null) {
                TimeRange newRange = TimeRange.of(newValue, timeRangePickerController.endDate.getDateTimeValue());
                if (newRange.isNegative()) {
                    TimeRange oldRange = TimeRange.of(oldValue, timeRangePickerController.endDate.getDateTimeValue());
                    this.selectedRange.setValue(TimeRange.of(newValue, newValue.plus(oldRange.getDuration())));
                }
                else {
                    this.selectedRange.setValue(newRange);
                }
            }
        });

        bindingManager.attachListener(timeRangePickerController.endDate.dateTimeValueProperty(), (ChangeListener<ZonedDateTime>) (observable, oldValue, newValue) -> {
            if (newValue != null) {
                TimeRange newRange = TimeRange.of(timeRangePickerController.startDate.getDateTimeValue(), newValue);
                if (newRange.isNegative()) {
                    TimeRange oldRange = TimeRange.of(timeRangePickerController.startDate.getDateTimeValue(), oldValue);
                    this.selectedRange.setValue(TimeRange.of(newValue.minus(oldRange.getDuration()), newValue));
                }
                else {
                    this.selectedRange.setValue(newRange);
                }
            }
        });
    }


    public ZoneId getZoneId() {
        return zoneId.getValue();
    }

    public Property<ZoneId> zoneIdProperty() {
        return zoneId;
    }

    public void closeBindingManager() {
        bindingManager.close();
    }

    private void updateText() {
        this.timeRange.setValue(TimeRange.of(timeRangePickerController.startDate.getDateTimeValue(), timeRangePickerController.endDate.getDateTimeValue()));
        setText(String.format("From %s to %s (%s)",
                timeRangePickerController.startDate.getDateTimeValue().format(timeRangePickerController.startDate.getFormatter()),
                timeRangePickerController.endDate.getDateTimeValue().format(timeRangePickerController.endDate.getFormatter()),
                getZoneId().toString()));
    }

    public TimeRange getSelectedRange() {
        return selectedRange.getValue();
    }

    public Property<TimeRange> selectedRangeProperty() {
        return selectedRange;
    }

    public void initSelectedRange(TimeRange selectedRange) {
        this.selectedRange.setValue(selectedRange);
    }

    public void updateSelectedRange(TimeRange newValue) {
        bindingManager.suspend();
        try {
            logger.trace(() -> "updateRangeBeginning -> " + newValue.toString());
            timeRangePickerController.startDate.setDateTimeValue(newValue.getBeginning());
            timeRangePickerController.endDate.setDateTimeValue(newValue.getEnd());
            this.selectedRange.setValue(TimeRange.of(newValue.getBeginning(), newValue.getEnd()));
            zoneId.setValue(newValue.getZoneId());
            updateText();
        } finally {
            bindingManager.resume();
        }
    }


    public TimeRange getTimeRange() {
        return timeRange.getValue();
    }

    public Property<TimeRange> timeRangeProperty() {
        return timeRange;
    }

    private void onZoneIdChanged(ObservableValue<? extends ZoneId> observable, ZoneId oldValue, ZoneId newValue) {
        updateText();
    }

    public void setOnSelectedRangeChanged(ChangeListener<TimeRange> timeRangeChangeListener) {
        bindingManager.attachListener(selectedRange, timeRangeChangeListener);
    }


    public Boolean isTimeRangeLinked() {
        return timeRangePickerController.linkTimeRangeButton.isSelected();
    }

    public Property<Boolean> timeRangeLinkedProperty() {
        return timeRangePickerController.linkTimeRangeButton.selectedProperty();
    }

    public void setTimeRangeLinked(Boolean timeRangeLinked) {
        timeRangePickerController.linkTimeRangeButton.setSelected(timeRangeLinked);
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
        @FXML
        private Button pasteTimeRangeButton;
        @FXML
        private Button copyTimeRangeButton;
        @FXML
        private ToggleButton linkTimeRangeButton;

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
                LocalDate today = LocalDate.now(zoneId.getValue());
                applyNewTimeRange.accept(
                        ZonedDateTime.of(today, LocalTime.MIDNIGHT, zoneId.getValue()),
                        ZonedDateTime.of(today.plusDays(1), LocalTime.MIDNIGHT, zoneId.getValue()));
            });
            yesterday.setOnAction(event -> {
                LocalDate today = LocalDate.now(zoneId.getValue());
                applyNewTimeRange.accept(
                        ZonedDateTime.of(today.minusDays(1), LocalTime.MIDNIGHT, zoneId.getValue()),
                        ZonedDateTime.of(today, LocalTime.MIDNIGHT, zoneId.getValue()));
            });
            thisWeek.setOnAction(event -> {
                LocalDate refDay = LocalDate.now(zoneId.getValue());
                int n = refDay.getDayOfWeek().getValue();
                applyNewTimeRange.accept(
                        ZonedDateTime.of(refDay.minusDays(n - 1), LocalTime.MIDNIGHT, zoneId.getValue()),
                        ZonedDateTime.of(refDay.plusDays(8 - n), LocalTime.MIDNIGHT, zoneId.getValue()));
            });
            lastWeek.setOnAction(event -> {
                LocalDate refDay = LocalDate.now(zoneId.getValue()).minusWeeks(1);
                int n = refDay.getDayOfWeek().getValue();
                applyNewTimeRange.accept(
                        ZonedDateTime.of(refDay.minusDays(n - 1), LocalTime.MIDNIGHT, zoneId.getValue()),
                        ZonedDateTime.of(refDay.plusDays(8 - n), LocalTime.MIDNIGHT, zoneId.getValue()));
            });
            copyTimeRangeButton.setOnAction(event -> {
                try {
                    final ClipboardContent content = new ClipboardContent();
                    content.put(TimeRange.TIME_RANGE_DATA_FORMAT, timeRange.getValue().serialize());
                    Clipboard.getSystemClipboard().setContent(content);
                } catch (Exception e) {
                    logger.error("Failed to copy time range to clipboard", e);
                }
            });
            pasteTimeRangeButton.setOnAction(event -> {
                try {
                    if (Clipboard.getSystemClipboard().hasContent(TimeRange.TIME_RANGE_DATA_FORMAT)) {
                        String content = (String) Clipboard.getSystemClipboard().getContent(TimeRange.TIME_RANGE_DATA_FORMAT);
                        TimeRange range = TimeRange.deSerialize(content);
                        zoneId.setValue(range.beginning.getZone());
                        selectedRange.setValue(range);
                    }
                } catch (Exception e) {
                    logger.error("Failed to paste range output from clipboard", e);
                }
            });
        }

        Property<ZoneId> zoneIdProperty() {
            return formatter.valueProperty();
        }
    }

    public static class TimeRange {
        public static final DataFormat TIME_RANGE_DATA_FORMAT = new DataFormat(TimeRange.class.getCanonicalName());
        private static final String DELIMITER = "\n";
        private final ZonedDateTime beginning;
        private final ZonedDateTime end;
        private final Duration duration;
        private final ZoneId zoneId;

        public static TimeRange of(ZonedDateTime beginning, ZonedDateTime end) {
            return new TimeRange(beginning, end);
        }

        TimeRange(ZonedDateTime beginning, ZonedDateTime end) {
            this.zoneId = beginning.getZone();
            this.beginning = beginning;
            this.end = end.withZoneSameInstant(zoneId);
            this.duration = Duration.between(beginning, end);
        }

        public ZonedDateTime getBeginning() {
            return beginning;
        }

        public ZonedDateTime getEnd() {
            return end;
        }

        public Duration getDuration() {
            return duration;
        }

        public ZoneId getZoneId() {
            return zoneId;
        }

        public boolean isNegative() {
            return duration.isNegative();
        }

        public String serialize() {
            return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(beginning) + DELIMITER + DateTimeFormatter.ISO_ZONED_DATE_TIME.format(end);
        }

        public static TimeRange deSerialize(String xmlString) {
            String[] s = xmlString.split(DELIMITER);
            if (s.length != 2) {
                throw new IllegalArgumentException("Could not parse provided string as a TimeRange");
            }
            return TimeRange.of(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(s[0], ZonedDateTime::from), DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(s[1], ZonedDateTime::from));
        }
    }


}
