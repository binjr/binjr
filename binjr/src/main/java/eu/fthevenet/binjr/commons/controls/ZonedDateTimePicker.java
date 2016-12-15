package eu.fthevenet.binjr.commons.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * A date picker with configurable datetime format where both date and time can be changed
 * via the text field and the date can additionally be changed via the JavaFX default date picker.
 */
@SuppressWarnings("unused")
public class ZonedDateTimePicker extends DatePicker {
  //  public static final String DefaultFormat = "yyyy/MM/dd HH:mm:ss";

    private DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
    private ObjectProperty<ZonedDateTime> dateTimeValue = new SimpleObjectProperty<>(ZonedDateTime.now());
//    private ObjectProperty<String> format = new SimpleObjectProperty<String>() {
//        public void set(String newValue) {
//            super.set(newValue);
//            formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
//        }
//    };

    public ZonedDateTimePicker(){
        this(ZoneId.systemDefault());
    }
    public ZonedDateTimePicker(ZoneId currentZoneId) {
        getStyleClass().add("datetime-picker");
     //   setFormat(DefaultFormat);
        setConverter(new InternalConverter());

        // Synchronize changes to the underlying date value back to the dateTimeValue
        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                dateTimeValue.set(null);
            } else {
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

//    public String getFormat() {
//        return format.get();
//    }
//
//    public ObjectProperty<String> formatProperty() {
//        return format;
//    }
//
//    public void setFormat(String format) {
//        this.format.set(format);
//    }

    class InternalConverter extends StringConverter<LocalDate> {
        public String toString(LocalDate object) {
            ZonedDateTime value = getDateTimeValue();
            return (value != null) ? value.format(formatter) : "";
        }

        public LocalDate fromString(String value) {
            if (value == null || value.isEmpty()) {
                dateTimeValue.set(null);
                return null;
            }

            dateTimeValue.set(ZonedDateTime.parse(value, formatter));
            return dateTimeValue.get().toLocalDate();
        }
    }
}