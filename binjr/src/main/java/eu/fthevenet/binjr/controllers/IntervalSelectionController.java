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

package eu.fthevenet.binjr.controllers;

import eu.fthevenet.util.javafx.controls.ZonedDateTimePicker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

public class IntervalSelectionController {

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
    private ToggleButton interval8Hours;

    @FXML
    private ToggleButton interval1Hour;

    @FXML
    private ToggleButton interval24Hours;

    @FXML
    private ToggleButton interval7Days;

    @FXML
    private ToggleButton interval15Days;

    @FXML
    private ToggleButton interval30Days;

    @FXML
    private ToggleButton interval2Days;

    @FXML
    private ToggleButton interval90Days;

    @FXML
    void initialize() {
        assert root != null : "fx:id=\"root\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert previousIntervalBtn != null : "fx:id=\"previousIntervalBtn\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert startDate != null : "fx:id=\"startDate\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert endDate != null : "fx:id=\"endDate\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert nextIntervalBtn != null : "fx:id=\"nextIntervalBtn\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert interval8Hours != null : "fx:id=\"interval8Hours\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert interval1Hour != null : "fx:id=\"interval1Hour\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert interval24Hours != null : "fx:id=\"interval24Hours\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert interval7Days != null : "fx:id=\"interval7Days\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert interval15Days != null : "fx:id=\"interval15Days\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert interval30Days != null : "fx:id=\"interval30Days\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert interval2Days != null : "fx:id=\"interval2Days\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";
        assert interval90Days != null : "fx:id=\"interval90Days\" was not injected: check your FXML file 'IntervalSelectionView.fxml'.";

    }
}
