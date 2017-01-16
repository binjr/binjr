package eu.fthevenet.binjr.preferences;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.beans.BeanInfo;
import java.io.Serializable;
import java.util.prefs.Preferences;

/**
 * Created by FTT2 on 16/01/2017.
 */
public class GlobalPreferences {
    private static final String CHART_ANIMATION_ENABLED = "chartAnimationEnabled";
    private static final String DOWN_SAMPLING_THRESHOLD = "downSamplingThreshold";
    private static final String SAMPLE_SYMBOLS_VISIBLE = "sampleSymbolsVisible";
    private static final String DOWN_SAMPLING_ENABLED = "downSamplingEnabled";
    private Property<Boolean> downSamplingEnabled;
    private SimpleIntegerProperty downSamplingThreshold;
    private Property<Boolean> sampleSymbolsVisible;
    private Property<Boolean> chartAnimationEnabled;
    private Preferences prefs;

    private GlobalPreferences() {
        prefs = Preferences.userRoot().node("binjr/global");

        downSamplingEnabled = new SimpleBooleanProperty(prefs.getBoolean(DOWN_SAMPLING_ENABLED, true));
        downSamplingEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(DOWN_SAMPLING_ENABLED, newValue));
        downSamplingThreshold = new SimpleIntegerProperty(prefs.getInt(DOWN_SAMPLING_THRESHOLD, 1000));
        downSamplingThreshold.addListener((observable, oldValue, newValue) -> prefs.putInt(DOWN_SAMPLING_THRESHOLD, newValue.intValue()));
        sampleSymbolsVisible = new SimpleBooleanProperty(prefs.getBoolean(SAMPLE_SYMBOLS_VISIBLE, false));
        sampleSymbolsVisible.addListener((observable, oldValue, newValue) -> prefs.putBoolean(SAMPLE_SYMBOLS_VISIBLE, newValue));
        chartAnimationEnabled = new SimpleBooleanProperty(prefs.getBoolean(CHART_ANIMATION_ENABLED, false));
        chartAnimationEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(CHART_ANIMATION_ENABLED, newValue));
    }


    public Boolean getChartAnimationEnabled() {
        return downSamplingEnabled.getValue();
    }

    public Property<Boolean> chartAnimationEnabledProperty() {
        return chartAnimationEnabled;
    }

    public void setChartAnimationEnabled(Boolean chartAnimationEnabled) {
        this.chartAnimationEnabled.setValue(chartAnimationEnabled);
    }

    public Boolean getSampleSymbolsVisible() {
        return sampleSymbolsVisible.getValue();
    }

    public Property<Boolean> sampleSymbolsVisibleProperty() {
        return sampleSymbolsVisible;
    }

    public void setSampleSymbolsVisible(Boolean sampleSymbolsVisible) {
        this.sampleSymbolsVisible.setValue(sampleSymbolsVisible);
    }

    public Boolean getDownSamplingEnabled() {
        return downSamplingEnabled.getValue();
    }

    public Property<Boolean> downSamplingEnabledProperty() {
        return downSamplingEnabled;
    }

    public void setDownSamplingEnabled(Boolean downSamplingEnabled) {
        this.downSamplingEnabled.setValue(downSamplingEnabled);
    }

    public int getDownSamplingThreshold() {
        return downSamplingThreshold.getValue();
    }

    public Property<Number> downSamplingThresholdProperty() {
        return downSamplingThreshold;
    }

    public void setDownSamplingThreshold(int downSamplingThreshold) {
        this.downSamplingThreshold.setValue(downSamplingThreshold);
    }

    private static class GlobalPreferencesHolder {
        private final static GlobalPreferences instance = new GlobalPreferences();
    }

    public static GlobalPreferences getInstance() {
        return GlobalPreferencesHolder.instance;
    }
}
