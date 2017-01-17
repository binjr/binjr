package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.NumberStringConverter;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by FTT2 on 16/01/2017.
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
    private Label maxSampleLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert downSamplingThreshold != null : "fx:id\"RDPEpsilon\" was not injected!";
        assert showChartSymbols != null : "fx:id\"showChartSymbols\" was not injected!";
        assert enableChartAnimation != null : "fx:id\"enableChartAnimation\" was not injected!";
        assert enableDownSampling != null : "fx:id\"enableDownSampling\" was not injected!";
        assert maxSampleLabel != null : "fx:id\"maxSampleLabel\" was not injected!";

        enableDownSampling.selectedProperty().addListener((observable, oldValue, newValue) -> {
            downSamplingThreshold.setDisable(!newValue);
            maxSampleLabel.setDisable(!newValue);
        });


        enableChartAnimation.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().chartAnimationEnabledProperty());
        showChartSymbols.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().sampleSymbolsVisibleProperty());
        enableDownSampling.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().downSamplingEnabledProperty());
        final TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
        downSamplingThreshold.setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(GlobalPreferences.getInstance().downSamplingThresholdProperty());

    }
}
