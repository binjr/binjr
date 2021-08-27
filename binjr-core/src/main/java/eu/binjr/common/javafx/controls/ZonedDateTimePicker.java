/*
 *    Copyright 2016-2021 Frederic Thevenet
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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.DatePicker;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * A date and time picker control that works with {@link java.time.ZonedDateTime}
 *
 * @author Frederic Thevenet
 */
public class ZonedDateTimePicker extends DatePicker {
    private static final Logger logger = Logger.create(ZonedDateTimePicker.class);
    private final Property<ZoneId> zoneId;
    private final ObjectProperty<ZonedDateTime> dateTimeValue;//

    /**
     * Initializes a new instance of the {@link ZonedDateTimePicker} class with the system's default timezone
     */
    public ZonedDateTimePicker() {
        this(ZoneId.systemDefault());
    }

    public ZonedDateTimePicker(ZoneId zoneId) {
        this(new SimpleObjectProperty<>(zoneId));
    }

    /**
     * Initializes a new instance of the {@link ZonedDateTimePicker} class with the provided timezone
     *
     * @param zoneIdProperty the timezone id to use in the control
     */
    public ZonedDateTimePicker(SimpleObjectProperty<ZoneId> zoneIdProperty) {
        this.zoneId = zoneIdProperty;
        dateTimeValue = new SimpleObjectProperty<>(ZonedDateTime.now(zoneIdProperty.get()));
        getStyleClass().add("datetime-picker");
        setConverter(new StringConverter<LocalDate>() {
            public String toString(LocalDate object) {
                ZonedDateTime value = getDateTimeValue();
                return (value != null) ? value.format(getFormatter()) : "";
            }

            public LocalDate fromString(String stringValue) {
                if (stringValue == null || stringValue.isEmpty()) {
                    dateTimeValue.set(null);
                    return null;
                }
                try {
                    dateTimeValue.set(ZonedDateTime.parse(stringValue, getFormatter()));
                    logger.trace(() -> "zonedId for dateTimeValue=" + dateTimeValue.get().getZone());
                } catch (Exception ex) {
                    logger.debug("Error parsing date", ex);
                    throw ex;
                }
                return dateTimeValue.get().toLocalDate();
            }
        });

        ChangeListener<LocalDate> localDateChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (dateTimeValue.get() == null) {
                    dateTimeValue.set(ZonedDateTime.of(newValue, LocalTime.now(), getZoneId()));
                } else {
                    LocalTime time = dateTimeValue.get().toLocalTime();
                    dateTimeValue.set(ZonedDateTime.of(newValue, time, getZoneId()));
                }
            }
        };

        valueProperty().addListener(localDateChangeListener);

        ChangeListener<ZoneId> zoneIdChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                dateTimeValue.setValue(dateTimeValue.get().withZoneSameInstant(newValue));
            }
        };

        dateTimeValue.addListener((observable, oldValue, newValue) -> {
            valueProperty().removeListener(localDateChangeListener);
            setValue(null);
            setValue(newValue == null ? null : newValue.toLocalDate());
            valueProperty().addListener(localDateChangeListener);
        });

        getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                KeyEvent ke = new KeyEvent(KeyEvent.KEY_RELEASED,
                        KeyCode.ENTER.toString(), KeyCode.ENTER.toString(),
                        KeyCode.ENTER, false, false, false, false);
                getEditor().getParent().fireEvent(ke);
            }
        });

        this.zoneId.addListener(zoneIdChangeListener);
    }

    DateTimeFormatter getFormatter() {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM).withZone(getZoneId());
    }

    //region [Properties]

    /**
     * Gets the selected date/time value of the control.
     *
     * @return the selected date/time value of the control.
     */
    public ZonedDateTime getDateTimeValue() {
        return dateTimeValue.get();
    }

    /**
     * Sets the selected date/time value of the control.
     *
     * @param dateTimeValue the selected date/time value of the control.
     */
    public void setDateTimeValue(ZonedDateTime dateTimeValue) {
        this.dateTimeValue.set(dateTimeValue);
    }

    /**
     * Returns the value property for the selected date/time
     *
     * @return the value property for the selected date/time
     */
    public ObjectProperty<ZonedDateTime> dateTimeValueProperty() {
        return dateTimeValue;
    }

    /**
     * Gets the {@link ZoneId} of the date picker
     *
     * @return the {@link ZoneId} of the date picker
     */
    public ZoneId getZoneId() {
        return zoneId.getValue();
    }

    /**
     * The zoneId property
     *
     * @return the  zoneId property
     */
    public Property<ZoneId> zoneIdProperty() {
        return zoneId;
    }

    /**
     * Sets  the {@link ZoneId} of the date picker
     *
     * @param zoneId the {@link ZoneId} of the date picker
     */
    public void setZoneId(ZoneId zoneId) {
        this.zoneId.setValue(zoneId);
    }

    //endregion
}