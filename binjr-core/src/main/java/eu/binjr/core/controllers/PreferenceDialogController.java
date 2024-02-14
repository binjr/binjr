/*
 *    Copyright 2017-2024 Frederic Thevenet
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

import eu.binjr.common.javafx.controls.LabelWithInlineHelp;
import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.common.javafx.controls.TextFieldValidator;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.preferences.ObfuscatedString;
import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.appearance.BuiltInChartColorPalettes;
import eu.binjr.core.appearance.BuiltInUserInterfaceThemes;
import eu.binjr.core.appearance.UserInterfaceThemes;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.adapters.DataAdapterInfo;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.*;
import eu.binjr.core.update.UpdateManager;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.prefs.BackingStoreException;

/**
 * The controller for the preference view.
 *
 * @author Frederic Thevenet
 */
public class PreferenceDialogController implements Initializable {
    private static final Logger logger = Logger.create(PreferenceDialogController.class);
    @FXML
    private LabelWithInlineHelp overrideScaleLabel;
    @FXML
    private ToggleSwitch overrideScalingToggle;
    @FXML
    private Slider customScalingSlider;
    @FXML
    private Label customScalingText;
    @FXML
    private ChoiceBox<HardwareAccelerationSupport> hwAccelerationChoiceBox;
    @FXML
    private ToggleSwitch forceNaNtoZeroSwitch;
    @FXML
    private ChoiceBox<ChartType> defaultChartTypeChoiceBox;
    @FXML
    private ChoiceBox<UnitPrefixes> defaultUnitPrefixChoiceBox;
    @FXML
    private ChoiceBox<IndexingTokenizer> indexingModeChoiceBox;
    @FXML
    private ToggleSwitch alwaysIncludeOriginInAutoScale;
    @FXML
    private ChoiceBox<DateTimeAnchor> dateTimeAnchorChoiceBox;
    @FXML
    private TextField defaultTextSizeField;
    @FXML
    private ToggleSwitch enableProxyToggle;
    @FXML
    private TextField proxyHostnameTextfield;
    @FXML
    private TextField proxyPortTextfield;
    @FXML
    private ToggleSwitch useProxyAuthToggle;
    @FXML
    private TextField proxyLoginTextfield;
    @FXML
    private PasswordField proxyPasswordTextfield;
    @FXML
    private ToggleSwitch dontAskBeforeClosingTabCheckbox;
    @FXML
    private ToggleSwitch dontAskBeforeRemovingChartCheckbox;
    @FXML
    private ToggleSwitch filterBarVisibleToggle;
    @FXML
    private ToggleSwitch findBarVisibleToggle;
    @FXML
    private ToggleSwitch heatmapVisibleToggle;
    @FXML
    private Label minChartHeightText;
    @FXML
    private TextField downSamplingThreshold;
    @FXML
    private TextField pluginLocTextfield;
    @FXML
    private Button browsePluginLocButton;
    @FXML
    private TableView<DataAdapterInfo> availableAdapterTable;
    @FXML
    private TableColumn<DataAdapterInfo, Boolean> enabledColumn;
    @FXML
    private ChoiceBox<NotificationDurationChoices> notifcationDurationChoiceBox;
    @FXML
    private ChoiceBox<SnapshotOutputScale> snapshotScaleChoiceBox;
    @FXML
    private TitledPane updatePreferences;
    @FXML
    private ToggleSwitch fullHeightCrosshair;
    @FXML
    private ChoiceBox<BuiltInChartColorPalettes> chartPaletteChoiceBox;
    @FXML
    private ToggleSwitch showOutlineStackedAreaCharts;
    @FXML
    private Slider stackedAreaChartOpacitySlider;
    @FXML
    private Label stackedAreaChartsOpacityText;
    @FXML
    private ChoiceBox<BuiltInChartColorPalettes> logsPaletteChoiceBox;
    @FXML
    private Slider minChartHeightSlider;
    @FXML
    private ToggleSwitch loadExternalToggle;
    @FXML
    private ToggleSwitch enableDownSampling;
    @FXML
    private LabelWithInlineHelp maxSampleLabel;
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
    private ToggleSwitch showOutlineAreaCharts;
    @FXML
    private AnchorPane root;
    @FXML
    private Slider areaChartOpacitySlider = new Slider();
    @FXML
    private Label areaChartsOpacityText = new Label();

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
        assert showOutlineAreaCharts != null : "fx:id\"showOutline\" was not injected!";
        assert areaChartOpacitySlider != null : "fx:id\"graphOpacitySlider\" was not injected!";
        UserPreferences userPrefs = UserPreferences.getInstance();
        areaChartOpacitySlider.valueProperty()
                .bindBidirectional(userPrefs.defaultOpacityAreaCharts.property());
        areaChartsOpacityText.textProperty()
                .bind(Bindings.format("%.0f%%", areaChartOpacitySlider.valueProperty().multiply(100)));
        minChartHeightSlider.minProperty()
                .bind(userPrefs.lowerStackedChartHeight.property());
        minChartHeightSlider.maxProperty()
                .bind(userPrefs.upperChartHeight.property());
        minChartHeightSlider.valueProperty()
                .bindBidirectional(userPrefs.minChartHeight.property());
        minChartHeightText.textProperty()
                .bind(Bindings.format("%.0f", minChartHeightSlider.valueProperty()));
        stackedAreaChartOpacitySlider.valueProperty()
                .bindBidirectional(userPrefs.defaultOpacityStackedAreaCharts.property());
        stackedAreaChartsOpacityText.textProperty()
                .bind(Bindings.format("%.0f%%", stackedAreaChartOpacitySlider.valueProperty().multiply(100)));
        enableDownSampling.selectedProperty().addListener((observable, oldValue, newValue) -> {
            downSamplingThreshold.setDisable(!newValue);
            maxSampleLabel.setDisable(!newValue);
        });
        enableDownSampling.selectedProperty()
                .bindBidirectional(UserPreferences.getInstance().downSamplingEnabled.property());
        alwaysIncludeOriginInAutoScale.selectedProperty()
                .bindBidirectional(UserPreferences.getInstance().defaultForceZeroInYAxisAutoRange.property());
        forceNaNtoZeroSwitch.selectedProperty()
                .bindBidirectional(UserPreferences.getInstance().forceNanToZero.property());
        fullHeightCrosshair.selectedProperty()
                .bindBidirectional(userPrefs.fullHeightCrosshairMarker.property());
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
        pathFormatter.valueProperty().bindBidirectional(userPrefs.userPluginsLocation.property());
        pluginLocTextfield.setTextFormatter(pathFormatter);
        userPrefs.userPluginsLocation.property().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !Files.exists(newValue)) {
                Dialogs.notifyError("Invalid Plugins Folder Location",
                        "The selected path for the plugins folder location does not exists",
                        Pos.BOTTOM_RIGHT,
                        root);
                Platform.runLater(() -> userPrefs.userPluginsLocation.set(oldValue));
            } else {
                Dialogs.notifyRestartNeeded(
                        "Plugins Folder Location Changed",
                        "Changes to the plugins folder location will take effect the next time binjr is started",
                        root);
            }
        });
        loadExternalToggle.selectedProperty()
                .bindBidirectional(userPrefs.loadPluginsFromExternalLocation.property());
        browsePluginLocButton.disableProperty()
                .bind(BooleanBinding.booleanExpression(userPrefs.loadPluginsFromExternalLocation.property()).not());
        pluginLocTextfield.disableProperty()
                .bind(BooleanBinding.booleanExpression(userPrefs.loadPluginsFromExternalLocation.property()).not());
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));
        availableAdapterTable.getItems().setAll(DataAdapterFactory.getInstance().getAllAdapters());
        loadAtStartupCheckbox.selectedProperty().bindBidirectional(userPrefs.loadLastWorkspaceOnStartup.property());
        final TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
        downSamplingThreshold.setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(userPrefs.downSamplingThreshold.property());

        final TextFormatter<Number> fontSizeformatter = new TextFormatter<>(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                if (object == null) {
                    return "";
                }
                return object.toString();
            }

            @Override
            public Number fromString(String string) {
                int val = Integer.parseInt(string);
                if (val < AppEnvironment.MIN_FONT_SIZE || val > AppEnvironment.MAX_FONT_SIZE) {
                    TextFieldValidator.fail(proxyPortTextfield, "Font size must between " +
                            AppEnvironment.MIN_FONT_SIZE + " and " + AppEnvironment.MAX_FONT_SIZE, true);
                }
                return val;
            }
        });

        customScalingSlider.valueProperty().bindBidirectional(userPrefs.customUIScale.property());
        customScalingText.textProperty().bind(Bindings.format("%d%%", userPrefs.customUIScale.property()));
        overrideScalingToggle.selectedProperty().bindBidirectional(userPrefs.forceUIScaling.property());
        customScalingSlider.disableProperty().bind(overrideScalingToggle.selectedProperty().not());
        customScalingSlider.disableProperty().bind(overrideScalingToggle.selectedProperty().not());
        customScalingText.disableProperty().bind(overrideScalingToggle.selectedProperty().not());

        // Delay the search until at least the following amount of time elapsed
        var delay = new PauseTransition(Duration.millis(300));
        var lastTrigger = new AtomicInteger(userPrefs.customUIScale.get().intValue());
        delay.setOnFinished(event -> Dialogs.notifyRestartNeeded("Override UI scale", root));
        customScalingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(lastTrigger.get() - newVal.intValue()) >= 20) {
                lastTrigger.set(newVal.intValue());
                delay.playFromStart();
            }
        });
        UserPreferences.getInstance().forceUIScaling.property().addListener(i -> Dialogs.notifyRestartNeeded("Override UI scale", root));
        UserPreferences.getInstance().hardwareAcceleration.property().addListener(i -> Dialogs.notifyRestartNeeded("Hardware acceleration support changed", root));

        // Overriding the UI scaling factor is only possible on Windows and Linux; don't show option if unavailable.
        OsFamily osFamily = AppEnvironment.getInstance().getOsFamily();
        setNodesVisibility(osFamily == OsFamily.LINUX || osFamily == OsFamily.WINDOWS,
                overrideScaleLabel,
                overrideScalingToggle,
                customScalingSlider,
                customScalingText);

        defaultTextSizeField.setTextFormatter(fontSizeformatter);
        fontSizeformatter.valueProperty().bindBidirectional(userPrefs.defaultTextViewFontSize.property());
        bindEnumToChoiceBox(userPrefs.defaultDateTimeAnchor, dateTimeAnchorChoiceBox, DateTimeAnchor.values());
        bindEnumToChoiceBox(userPrefs.defaultChartType, defaultChartTypeChoiceBox, ChartType.definedValues());
        bindEnumToChoiceBox(userPrefs.defaultUnitPrefix, defaultUnitPrefixChoiceBox, UnitPrefixes.definedValues());
        bindEnumToChoiceBox(userPrefs.userInterfaceTheme,
                s -> UserInterfaceThemes.valueOf(s, BuiltInUserInterfaceThemes.LIGHT),
                UserInterfaceThemes::name,
                uiThemeChoiceBox,
                UserInterfaceThemes.values());
        bindEnumToChoiceBox(userPrefs.hardwareAcceleration, hwAccelerationChoiceBox, HardwareAccelerationSupport.values());
        bindEnumToChoiceBox(userPrefs.chartColorPalette, chartPaletteChoiceBox, BuiltInChartColorPalettes.values());
        bindEnumToChoiceBox(userPrefs.logFilesColorPalette, logsPaletteChoiceBox, BuiltInChartColorPalettes.values());
        bindEnumToChoiceBox(userPrefs.notificationPopupDuration, notifcationDurationChoiceBox, NotificationDurationChoices.values());
        bindEnumToChoiceBox(userPrefs.snapshotOutputScale, snapshotScaleChoiceBox, SnapshotOutputScale.values());
        bindEnumToChoiceBox(userPrefs.indexingTokenizer, indexingModeChoiceBox, IndexingTokenizer.values());
        updateCheckBox.selectedProperty().bindBidirectional(userPrefs.checkForUpdateOnStartUp.property());
        showOutlineAreaCharts.selectedProperty().bindBidirectional(userPrefs.showOutlineOnAreaCharts.property());
        showOutlineStackedAreaCharts.selectedProperty().bindBidirectional(userPrefs.showOutlineOnStackedAreaCharts.property());
        filterBarVisibleToggle.selectedProperty().bindBidirectional(UserPreferences.getInstance().logFilterBarVisible.property());
        findBarVisibleToggle.selectedProperty().bindBidirectional(UserPreferences.getInstance().logFindBarVisible.property());
        heatmapVisibleToggle.selectedProperty().bindBidirectional(UserPreferences.getInstance().logHeatmapVisible.property());
        updatePreferences.visibleProperty().bind(Bindings.not(AppEnvironment.getInstance().updateCheckDisabledProperty()));
        dontAskBeforeClosingTabCheckbox.selectedProperty().bindBidirectional(UserPreferences.getInstance().doNotWarnOnTabClose.property());
        dontAskBeforeRemovingChartCheckbox.selectedProperty().bindBidirectional(UserPreferences.getInstance().doNotWarnOnChartClose.property());
        proxyHostnameTextfield.disableProperty().bind(enableProxyToggle.selectedProperty().not());
        proxyPortTextfield.disableProperty().bind(enableProxyToggle.selectedProperty().not());
        useProxyAuthToggle.disableProperty().bind(enableProxyToggle.selectedProperty().not());
        proxyLoginTextfield.disableProperty().bind(
                Bindings.or(enableProxyToggle.selectedProperty().not(),
                        useProxyAuthToggle.selectedProperty().not()));
        proxyPasswordTextfield.disableProperty().bind(
                Bindings.or(enableProxyToggle.selectedProperty().not(),
                        useProxyAuthToggle.selectedProperty().not()));

        enableProxyToggle.selectedProperty().bindBidirectional(userPrefs.enableHttpProxy.property());
        proxyHostnameTextfield.textProperty().bindBidirectional(userPrefs.httpProxyHost.property());
        final TextFormatter<Number> portFormatter =
                new TextFormatter<>(new StringConverter<>() {
                    @Override
                    public String toString(Number object) {
                        if (object == null) {
                            return "";
                        }
                        return object.toString();
                    }

                    @Override
                    public Number fromString(String string) {
                        int ushort = Integer.parseInt(string);
                        if (ushort < 0) {
                            TextFieldValidator.fail(proxyPortTextfield, "Port number cannot be less than 0", true);
                        } else if (ushort > 65535) {
                            TextFieldValidator.fail(proxyPortTextfield, "Port number cannot be greater than 65535", true);
                        }
                        return ushort;
                    }
                });
        proxyPortTextfield.setTextFormatter(portFormatter);
        portFormatter.valueProperty().bindBidirectional(userPrefs.httpProxyPort.property());
        useProxyAuthToggle.selectedProperty().bindBidirectional(userPrefs.useHttpProxyAuth.property());
        proxyLoginTextfield.textProperty().bindBidirectional(userPrefs.httpProxyLogin.property());
        final TextFormatter<ObfuscatedString> pwdFormatter = new TextFormatter<>(new StringConverter<>() {
            @Override
            public String toString(ObfuscatedString object) {
                if (object == null) {
                    return "";
                }
                return object.toPlainText();
            }

            @Override
            public ObfuscatedString fromString(String string) {
                return userPrefs.getObfuscator().fromPlainText(string);
            }
        });
        proxyPasswordTextfield.setTextFormatter(pwdFormatter);
        pwdFormatter.valueProperty().bindBidirectional(userPrefs.httpProxyPassword.property());

    }

    private void setNodesVisibility(boolean isVisible, Node... nodes) {
        for (var n : nodes) {
            n.setVisible(isVisible);
            n.setManaged(isVisible);
        }
    }

    private <T> void bindEnumToChoiceBox(ObservablePreference<T> observablePreference, ChoiceBox<T> choiceBox, T... initValues) {
        bindEnumToChoiceBox(observablePreference, t -> t, t -> t, choiceBox, initValues);
    }

    private <A, B> void bindEnumToChoiceBox(ObservablePreference<A> observablePreference,
                                            Function<A, B> fromAtoB, Function<B, A> fromBtoA,
                                            ChoiceBox<B> choiceBox, B... initValues) {
        choiceBox.getItems().setAll(initValues);
        choiceBox.getSelectionModel().select(fromAtoB.apply(observablePreference.get()));
        observablePreference.property().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                choiceBox.getSelectionModel().select(fromAtoB.apply(newValue));
            }
        });
        choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                observablePreference.set(fromBtoA.apply(newValue));
            }
        });
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
        TranslateTransition openNav = new TranslateTransition(new Duration(250), n);
        openNav.setDelay(delay);
        openNav.setToX(-MainViewController.SETTINGS_PANE_WIDTH);
        openNav.play();
    }

    public void handleResetSettings(ActionEvent actionEvent) {
        try {
            if (Dialogs.confirmDialog(root, "Restore all settings to their default value.", "Are you sure?") == ButtonType.YES) {
                UserPreferences.getInstance().reset();
                logger.info("User settings successfully reset to default");
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
            if (!Files.isDirectory(pluginPath)) {
                pluginPath = pluginPath.getParent();
            }
            if (pluginPath != null) {
                fileChooser.setInitialDirectory(pluginPath.toFile());
            }
        } catch (Exception e) {
            logger.debug("Could not initialize working dir for DirectoryChooser", e);
        }
        File newPluginLocation = fileChooser.showDialog(NodeUtils.getStage(root));
        if (newPluginLocation != null) {
            pluginLocTextfield.setText(newPluginLocation.getPath());
        }
    }

    public void handleExportSettings(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Settings");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("binjr settings", "*.xml"));
        fileChooser.setInitialFileName("binjr_settings.xml");
        Dialogs.getInitialDir(UserHistory.getInstance().mostRecentSaveFolders).ifPresent(fileChooser::setInitialDirectory);
        var exportPath = fileChooser.showSaveDialog(NodeUtils.getStage(root));
        if (exportPath != null) {
            try {
                Files.deleteIfExists(exportPath.toPath());
                UserHistory.getInstance().mostRecentSaveFolders.push(exportPath.toPath().getParent());
                UserPreferences.getInstance().exportToFile(exportPath.toPath());
                logger.info("User settings successfully exported to " + exportPath);
            } catch (Exception e) {
                Dialogs.notifyException("An error occurred while exporting settings: " + e.getMessage(), e, root);
            }
        }
    }

    public void handleImportSettings(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Settings");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("binjr settings", "*.xml"));
        Dialogs.getInitialDir(UserHistory.getInstance().mostRecentSaveFolders).ifPresent(fileChooser::setInitialDirectory);
        File importPath = fileChooser.showOpenDialog(NodeUtils.getStage(root));
        if (importPath != null) {
            try {
                UserPreferences.getInstance().importFromFile(importPath.toPath());
                logger.info("User settings successfully imported from " + importPath);
            } catch (Exception e) {
                Dialogs.notifyException("An error occurred while importing settings: " + e.getMessage(), e, root);
            }
        }
    }

    public void handleClearHistory(ActionEvent actionEvent) {
        try {
            if (Dialogs.confirmDialog(root, "Clear all saved history (recently opened workspace, sources, etc...)", "Are you sure?") == ButtonType.YES) {
                UserHistory.getInstance().reset();
                logger.info("Saved history successfully reset");
            }
        } catch (BackingStoreException e) {
            Dialogs.notifyException("Could not clear all saved history", e, root);
        }
    }
}
