package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.converter.NumberStringConverter;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The controller for the preference view.
 *
 * @author Frederic Thevenet
 */
public class PreferenceDialogController implements Initializable {
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert downSamplingThreshold != null : "fx:id\"RDPEpsilon\" was not injected!";
        assert showChartSymbols != null : "fx:id\"showChartSymbols\" was not injected!";
        assert enableChartAnimation != null : "fx:id\"enableChartAnimation\" was not injected!";
        assert enableDownSampling != null : "fx:id\"enableDownSampling\" was not injected!";
        assert maxSampleLabel != null : "fx:id\"maxSampleLabel\" was not injected!";
        assert accordionPane != null : "fx:id\"accordionPane\" was not injected!";
        assert useSourceColors != null : "fx:id\"useSourceColors\" was not injected!";

        enableDownSampling.selectedProperty().addListener((observable, oldValue, newValue) -> {
            downSamplingThreshold.setDisable(!newValue);
            maxSampleLabel.setDisable(!newValue);
        });

        enableChartAnimation.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().chartAnimationEnabledProperty());
        showChartSymbols.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().sampleSymbolsVisibleProperty());
        useSourceColors.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().useSourceColorsProperty());
        enableDownSampling.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().downSamplingEnabledProperty());
        final TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
        downSamplingThreshold.setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(GlobalPreferences.getInstance().downSamplingThresholdProperty());

        Platform.runLater(()-> {
            if (accordionPane.getPanes() != null
                    && accordionPane.getPanes().size() > 0
                    && accordionPane.getPanes().get(0) != null) {
                accordionPane.getPanes().get(0).setExpanded(true);
            }
        });

    }
}
