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

import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.TextFields;

import java.net.URL;
import java.time.ZoneId;
import java.util.ResourceBundle;

public class TimeRangePickerController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private AnchorPane root;

    @FXML
    private Button previousIntervalBtn;

    @FXML
    public ZonedDateTimePicker startDate;

    @FXML
    public ZonedDateTimePicker endDate;

    @FXML
    private Button nextIntervalBtn;

    @FXML
    private Button interval8Hours;

    @FXML
    private TextField timezoneField;

    @FXML
    private Button interval1Hour;

    @FXML
    private Button interval24Hours;

    @FXML
    private Button interval7Days;

    @FXML
    private Button interval15Days;

    @FXML
    private Button interval30Days;

    @FXML
    private Button interval2Days;

    @FXML
    private Button interval90Days;

    private TextFormatter<ZoneId> formatter;

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
    }

    Property<ZoneId> zoneIdProperty() {
        return formatter.valueProperty();
    }
}
