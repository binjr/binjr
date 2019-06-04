/*
 *    Copyright 2017-2019 Frederic Thevenet
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

package eu.binjr.core.controllers;

import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.adapters.DataAdapterInfo;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.dialogs.UserInterfaceThemes;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.GlobalPreferences;
import eu.binjr.core.preferences.NotificationDurationChoices;
import eu.binjr.core.preferences.UpdateManager;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.ToggleSwitch;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;

/**
 * The controller for the preference view.
 *
 * @author Frederic Thevenet
 */
public class PreferenceDialogController implements Initializable {
    private static final Logger logger = LogManager.getLogger(PreferenceDialogController.class);
    @FXML
    public TextField downSamplingThreshold;
    public TextField pluginLocTextfield;
    public Button browsePluginLocButton;
    public TableView<DataAdapterInfo> availableAdapterTable;
    public TableColumn<DataAdapterInfo, Boolean> enabledColumn;
    public ChoiceBox<NotificationDurationChoices> notifcationDurationChoiceBox;
    @FXML
    public TitledPane updatePreferences;
    @FXML
    public ToggleSwitch fullHeightCrosshair;
    @FXML
    private ToggleSwitch loadExternalToggle;
    @FXML
    private ToggleSwitch enableDownSampling;
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
        enableDownSampling.selectedProperty().bindBidirectional(prefs.downSamplingEnabledProperty());

        fullHeightCrosshair.selectedProperty().bindBidirectional(prefs.fullHeightCrosshairMarkerProperty());

        final TextFormatter<Path> pathFormatter = new TextFormatter<>(new StringConverter<>() {
            @Override
            public String toString(Path object) {
                return object.toString();
            }

            @Override
            public Path fromString(String string) {
                return Paths.get(string);
            }
        });
        pathFormatter.valueProperty().bindBidirectional(prefs.pluginsLocationProperty());
        pluginLocTextfield.setTextFormatter(pathFormatter);
        prefs.pluginsLocationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !Files.exists(newValue)) {
                Dialogs.notifyError("Invalid Plugins Folder Location",
                        "The selected path for the plugins folder location does not exists",
                        Pos.BOTTOM_RIGHT,
                        root);
                Platform.runLater(() -> prefs.setPluginsLocation(oldValue));
            } else {
                Dialogs.notifyInfo(
                        "Plugins Folder Location Changed",
                        "Changes to the plugins folder location will take effect the next time binjr is started",
                        Pos.BOTTOM_RIGHT,
                        root);
            }
        });
        loadExternalToggle.selectedProperty().bindBidirectional(prefs.loadPluginsFromExternalLocationProperty());
        browsePluginLocButton.disableProperty().bind(prefs.loadPluginsFromExternalLocationProperty().not());
        pluginLocTextfield.disableProperty().bind(prefs.loadPluginsFromExternalLocationProperty().not());
        enabledColumn.setCellFactory( CheckBoxTableCell.forTableColumn(enabledColumn));
        availableAdapterTable.getItems().setAll(DataAdapterFactory.getInstance().getAllAdapters());
        loadAtStartupCheckbox.selectedProperty().bindBidirectional(prefs.loadLastWorkspaceOnStartupProperty());
        final TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
        downSamplingThreshold.setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(prefs.downSamplingThresholdProperty());
        uiThemeChoiceBox.getItems().setAll(UserInterfaceThemes.values());
        uiThemeChoiceBox.getSelectionModel().select(prefs.getUserInterfaceTheme());
        prefs.userInterfaceThemeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                uiThemeChoiceBox.getSelectionModel().select(newValue);
            }
        });
        uiThemeChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                prefs.setUserInterfaceTheme(newValue);
            }
        });
        notifcationDurationChoiceBox.getItems().setAll(NotificationDurationChoices.values());
        notifcationDurationChoiceBox.getSelectionModel().select(NotificationDurationChoices.valueOf(prefs.getNotificationPopupDuration()));
        prefs.notificationPopupDurationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                notifcationDurationChoiceBox.getSelectionModel().select(NotificationDurationChoices.valueOf(newValue));
            }
        });
        notifcationDurationChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                prefs.setNotificationPopupDuration(newValue.getDuration());
            }
        });
        updateCheckBox.selectedProperty().bindBidirectional(prefs.checkForUpdateOnStartUpProperty());
        showOutline.selectedProperty().bindBidirectional(prefs.showAreaOutlineProperty());
        updatePreferences.visibleProperty().bind(Bindings.not(AppEnvironment.getInstance().updateCheckDisabledProperty()));
    }

    public void handleCheckForUpdate(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        btn.setDisable(true);
        printToTextFlow("Checking for updates...", "#C2C2C2");

        UpdateManager.getInstance().asyncForcedCheckForUpdate(githubRelease -> {
                    updateFlow.getChildren().clear();
                    Hyperlink latestReleaseLink = new Hyperlink("Version " + githubRelease.getVersion().toString() + " is available.");
                    latestReleaseLink.setOnAction(event -> {
                        try {
                            Dialogs.launchUrlInExternalBrowser(githubRelease.getHtmlUrl());
                        } catch (IOException | URISyntaxException e) {
                            logger.error(e);
                        }
                    });
                    updateFlow.getChildren().add(latestReleaseLink);
                    btn.setDisable(false);
                    UpdateManager.getInstance().showUpdateAvailableNotification(githubRelease, root);
                },
                version -> {
                    btn.setDisable(false);
                    printToTextFlow("binjr is up to date (v" + version.toString() + ")");
                },
                () -> {
                    btn.setDisable(false);
                    printToTextFlow("Could not check for update!", "#E81123");
                });
    }

    private void printToTextFlow(String text) {
        printToTextFlow(text, null);
    }

    private void printToTextFlow(String text, String color) {
        updateFlow.getChildren().clear();
        Label l = new Label(text);
        if (color != null) {
            l.setStyle("-fx-text-fill:" + color + ";");
        }
        l.setWrapText(true);
        updateFlow.getChildren().add(l);
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

    public void handleResetSettings(ActionEvent actionEvent) {
        try {
            if (Dialogs.confirmDialog(root, "Restore all settings to their default value.", "Are you sure?") == ButtonType.YES) {
                GlobalPreferences.getInstance().reset();
            }
        } catch (BackingStoreException e) {
            Dialogs.notifyException("Could not restore settings to default", e, root);
        }
    }

    public void handleBrowsePluginsFolder(ActionEvent actionEvent) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Select binjr plugins location");
        try {
            Path pluginPath = Paths.get(pluginLocTextfield.getText()).toRealPath();
            if (Files.isDirectory(pluginPath)) {
                fileChooser.setInitialDirectory(pluginPath.toFile());
            }
        } catch (Exception e) {
            logger.debug("Could not initialize working dir for DirectoryChooser", e);
        }
        File newPluginLocation = fileChooser.showDialog(Dialogs.getStage(root));
        if (newPluginLocation != null) {
            pluginLocTextfield.setText(newPluginLocation.getPath());
        }
    }
}
