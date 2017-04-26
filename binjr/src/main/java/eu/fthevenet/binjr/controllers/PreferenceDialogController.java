package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.dialogs.UserInterfaceThemes;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.util.converter.NumberStringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private CheckBox showChartSymbols;
    @FXML
    private CheckBox enableDownSampling;
    @FXML
    private CheckBox enableChartAnimation;
    @FXML
    private CheckBox useSourceColors;
    @FXML
    private Label maxSampleLabel;
    @FXML
    private Accordion accordionPane;
    @FXML
    private CheckBox loadAtStartupCheckbox;
    @FXML
    private ChoiceBox<UserInterfaceThemes> uiThemeChoiceBox;
    @FXML
    private TextFlow updateFlow;
    @FXML
    private CheckBox updateCheckBox;

    @FXML
    private RadioButton showCrosshairOnKeyPressedRadio;

    @FXML
    private CheckBox showOutline;
    @FXML
    private TextField defaultOpacityText;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert downSamplingThreshold != null : "fx:id\"RDPEpsilon\" was not injected!";
        assert showChartSymbols != null : "fx:id\"showChartSymbols\" was not injected!";
        assert enableChartAnimation != null : "fx:id\"enableChartAnimation\" was not injected!";
        assert enableDownSampling != null : "fx:id\"enableDownSampling\" was not injected!";
        assert maxSampleLabel != null : "fx:id\"maxSampleLabel\" was not injected!";
        assert accordionPane != null : "fx:id\"accordionPane\" was not injected!";
        assert useSourceColors != null : "fx:id\"useSourceColors\" was not injected!";
        assert loadAtStartupCheckbox != null : "fx:id\"loadAtStartupCheckbox\" was not injected!";
        assert uiThemeChoiceBox != null : "fx:id\"uiThemeChoiceBox\" was not injected!";
        assert updateFlow != null : "fx:id\"updateFlow\" was not injected!";
        assert updateCheckBox != null : "fx:id\"updateCheckBox\" was not injected!";
        assert showCrosshairOnKeyPressedRadio != null : "fx:id\"showCrosshairOnKeyPressedRadio\" was not injected!";
        assert showOutline != null : "fx:id\"showOutline\" was not injected!";
        assert defaultOpacityText != null : "fx:id\"defaultOpacityText\" was not injected!";

        enableDownSampling.selectedProperty().addListener((observable, oldValue, newValue) -> {
            downSamplingThreshold.setDisable(!newValue);
            maxSampleLabel.setDisable(!newValue);
        });

        GlobalPreferences prefs = GlobalPreferences.getInstance();
        enableChartAnimation.selectedProperty().bindBidirectional(prefs.chartAnimationEnabledProperty());
        showChartSymbols.selectedProperty().bindBidirectional(prefs.sampleSymbolsVisibleProperty());
        useSourceColors.selectedProperty().bindBidirectional(prefs.useSourceColorsProperty());
        enableDownSampling.selectedProperty().bindBidirectional(prefs.downSamplingEnabledProperty());
        loadAtStartupCheckbox.selectedProperty().bindBidirectional(prefs.loadLastWorkspaceOnStartupProperty());
        final TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
        downSamplingThreshold.setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(prefs.downSamplingThresholdProperty());
        uiThemeChoiceBox.getItems().setAll(UserInterfaceThemes.values());
        uiThemeChoiceBox.getSelectionModel().select(prefs.getUserInterfaceTheme());
        prefs.userInterfaceThemeProperty().bind(uiThemeChoiceBox.getSelectionModel().selectedItemProperty());
        updateCheckBox.selectedProperty().bindBidirectional(prefs.checkForUpdateOnStartUpProperty());
        showCrosshairOnKeyPressedRadio.selectedProperty().bindBidirectional(prefs.enableCrosshairOnKeyPressedProperty());
        final TextFormatter<Number> opacityFormatter = new TextFormatter<>(new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
        defaultOpacityText.setTextFormatter(opacityFormatter);
        opacityFormatter.valueProperty().bindBidirectional(prefs.defaultGraphOpacityProperty());
        showOutline.selectedProperty().bindBidirectional(prefs.showAreaOutlineProperty());

        Platform.runLater(() -> {
            accordionPane.getPanes().forEach(p -> p.expandedProperty().addListener((obs, oldValue, newValue) -> {
                p.requestLayout();
                p.getScene().getWindow().sizeToScene();
            }));

            if (accordionPane.getPanes() != null
                    && accordionPane.getPanes().size() > 0
                    && accordionPane.getPanes().get(0) != null) {
                accordionPane.getPanes().get(0).setExpanded(true);
            }
        });
    }

    public void handleCheckForUpdate(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        btn.setDisable(true);
        updateFlow.getChildren().clear();
        Label l = new Label("Checking for updates...");
        l.setTextFill(Color.DIMGRAY);
        updateFlow.getChildren().add(l);
        GlobalPreferences.getInstance().asyncCheckForUpdate(githubRelease -> {
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
}
