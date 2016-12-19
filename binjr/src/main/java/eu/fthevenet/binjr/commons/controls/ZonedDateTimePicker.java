package eu.fthevenet.binjr.commons.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * A date picker with configurable datetime format where both date and time can be changed
 * via the text field and the date can additionally be changed via the JavaFX default date picker.
 */
@SuppressWarnings("unused")
public class ZonedDateTimePicker extends DatePicker {
    private static final Logger logger = LogManager.getLogger(ZonedDateTimePicker.class);
    private DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
    private ObjectProperty<ZonedDateTime> dateTimeValue = new SimpleObjectProperty<>(ZonedDateTime.now());


    public ZonedDateTimePicker(){
        this(ZoneId.systemDefault());
    }
    public ZonedDateTimePicker(ZoneId currentZoneId) {
        getStyleClass().add("datetime-picker");

        setConverter(new InternalConverter());

        // Synchronize changes to the underlying date value back to the dateTimeValue
        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (dateTimeValue.get() == null) {
                    dateTimeValue.set(ZonedDateTime.of(newValue, LocalTime.now(),currentZoneId));
                } else {
                    LocalTime time = dateTimeValue.get().toLocalTime();
                    dateTimeValue.set(ZonedDateTime.of(newValue, time,currentZoneId));
                }
            }
        });

        // Synchronize changes to dateTimeValue back to the underlying date value
        dateTimeValue.addListener((observable, oldValue, newValue) -> {
            logger.debug(() -> "observable = " + observable);
            logger.debug(() -> "oldValue = " + oldValue);
            logger.debug(() -> "newValue = " + newValue);
            //Force valueProperty to be invalidated, even is the LocalDate part of the ZonedDateTime has not changed
            setValue(null);
            setValue(newValue == null ? null : newValue.toLocalDate());

        });

        // Persist changes onblur
        getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue)
                simulateEnterPressed();
        });
    }

    private void simulateEnterPressed() {
        getEditor().commitValue();
    }

    public ZonedDateTime getDateTimeValue() {
        return dateTimeValue.get();
    }

    public void setDateTimeValue(ZonedDateTime dateTimeValue) {
        this.dateTimeValue.set(dateTimeValue);
    }

    public ObjectProperty<ZonedDateTime> dateTimeValueProperty() {
        return dateTimeValue;
    }

    class InternalConverter extends StringConverter<LocalDate> {
        public String toString(LocalDate object) {
            ZonedDateTime value = getDateTimeValue();
            return (value != null) ? value.format(formatter) : "";
        }

        public LocalDate fromString(String stringValue) {
            ZonedDateTime zdt = ZonedDateTime.parse(stringValue, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM));
            if (stringValue == null || stringValue.isEmpty()) {
                dateTimeValue.set(null);
                return null;
            }

            dateTimeValue.set(zdt);
            return dateTimeValue.get().toLocalDate();
        }
    }
}