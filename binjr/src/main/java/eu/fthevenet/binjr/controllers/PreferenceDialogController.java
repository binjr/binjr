/*
 *    Copyright 2017 Frederic Thevenet
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

import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.dialogs.UserInterfaceThemes;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.binjr.preferences.UpdateManager;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.ToggleSwitch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The controller for the preference view.
 *
 * @author Frederic Thevenet
 */
public class PreferenceDialogController implements Initializable {
    private static final Logger logger = LogManager.getLogger(PreferenceDialogController.class);
    @FXML
    public TextField downSamplingThreshold;
    @FXML
    private ToggleSwitch enableDownSampling;
    @FXML
    private ToggleSwitch enableChartAnimation;
    @FXML
    private Label maxSampleLabel;
    @FXML
    private Accordion accordionPane;
    @FXML
    private ToggleSwitch loadAtStartupCheckbox;
    @FXML
    private ChoiceBox<UserInterfaceThemes> uiThemeChoiceBox;
    @FXML
    private TextFlow updateFlow;
    @FXML
    private ToggleSwitch updateCheckBox;

    @FXML
    private ToggleSwitch showOutline;

    @FXML
    private AnchorPane root;
    @FXML
    private Slider graphOpacitySlider = new Slider();
    @FXML
    private Label opacityText = new Label();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert downSamplingThreshold != null : "fx:id\"RDPEpsilon\" was not injected!";
        assert enableChartAnimation != null : "fx:id\"enableChartAnimation\" was not injected!";
        assert enableDownSampling != null : "fx:id\"enableDownSampling\" was not injected!";
        assert maxSampleLabel != null : "fx:id\"maxSampleLabel\" was not injected!";
        assert accordionPane != null : "fx:id\"accordionPane\" was not injected!";
        assert loadAtStartupCheckbox != null : "fx:id\"loadAtStartupCheckbox\" was not injected!";
        assert uiThemeChoiceBox != null : "fx:id\"uiThemeChoiceBox\" was not injected!";
        assert updateFlow != null : "fx:id\"updateFlow\" was not injected!";
        assert updateCheckBox != null : "fx:id\"updateCheckBox\" was not injected!";
        assert showOutline != null : "fx:id\"showOutline\" was not injected!";
        assert graphOpacitySlider != null : "fx:id\"graphOpacitySlider\" was not injected!";
        GlobalPreferences prefs = GlobalPreferences.getInstance();
        graphOpacitySlider.valueProperty().bindBidirectional(prefs.defaultGraphOpacityProperty());
        opacityText.textProperty().bind(Bindings.format("%.0f%%", graphOpacitySlider.valueProperty().multiply(100)));

        enableDownSampling.selectedProperty().addListener((observable, oldValue, newValue) -> {
            downSamplingThreshold.setDisable(!newValue);
            maxSampleLabel.setDisable(!newValue);
        });
        enableChartAnimation.selectedProperty().bindBidirectional(prefs.chartAnimationEnabledProperty());
        enableDownSampling.selectedProperty().bindBidirectional(prefs.downSamplingEnabledProperty());
        loadAtStartupCheckbox.selectedProperty().bindBidirectional(prefs.loadLastWorkspaceOnStartupProperty());
        final TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
        downSamplingThreshold.setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(prefs.downSamplingThresholdProperty());
        uiThemeChoiceBox.getItems().setAll(UserInterfaceThemes.values());
        uiThemeChoiceBox.getSelectionModel().select(prefs.getUserInterfaceTheme());
        prefs.userInterfaceThemeProperty().bind(uiThemeChoiceBox.getSelectionModel().selectedItemProperty());
        updateCheckBox.selectedProperty().bindBidirectional(prefs.checkForUpdateOnStartUpProperty());
        showOutline.selectedProperty().bindBidirectional(prefs.showAreaOutlineProperty());
    }

    public void handleCheckForUpdate(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        btn.setDisable(true);
        updateFlow.getChildren().clear();
        Label l = new Label("Checking for updates...");
        l.setTextFill(Color.DIMGRAY);
        l.setWrapText(true);
        updateFlow.getChildren().add(l);
        UpdateManager.getInstance().asyncForcedCheckForUpdate(githubRelease -> {
                    updateFlow.getChildren().clear();
                    Hyperlink latestReleaseLink = new Hyperlink("Version " + githubRelease.getVersion().toString() + " is available.");
                    latestReleaseLink.setTextFill(Color.valueOf("#4BACC6"));
                    latestReleaseLink.setOnAction(event -> {
                        try {
                            Dialogs.launchUrlInExternalBrowser(githubRelease.getHtmlUrl());
                        } catch (IOException | URISyntaxException e) {
                            logger.error(e);
                        }
                    });
                    updateFlow.getChildren().add(latestReleaseLink);
                    btn.setDisable(false);
                },
                version -> {
                    btn.setDisable(false);
                    l.setText("binjr is up to date (v" + version.toString() + ")");
                },
                () -> {
                    btn.setDisable(false);
                    l.setText("Could not check for update");
                });
    }

    public void handleHideSettings(ActionEvent actionEvent) {
        hide(Duration.millis(0));
    }

    private void hide(Duration delay) {
        Node n = root.getParent();
        TranslateTransition openNav = new TranslateTransition(new Duration(200), n);
        openNav.setDelay(delay);
        openNav.setToX(-MainViewController.SETTINGS_PANE_DISTANCE);
        openNav.play();
    }
}
