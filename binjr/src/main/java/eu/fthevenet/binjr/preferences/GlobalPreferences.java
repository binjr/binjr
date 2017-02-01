package eu.fthevenet.binjr.preferences;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.prefs.Preferences;

/**
 * Stores the global user preferences for the application.
 *
 * @author Frederic Thevenet
 */
public class GlobalPreferences {
    private static final String CHART_ANIMATION_ENABLED = "chartAnimationEnabled";
    private static final String DOWN_SAMPLING_THRESHOLD = "downSamplingThreshold";
    private static final String SAMPLE_SYMBOLS_VISIBLE = "sampleSymbolsVisible";
    private static final String DOWN_SAMPLING_ENABLED = "downSamplingEnabled";
    private static final String BINJR_GLOBAL = "binjr/global";
    private Property<Boolean> downSamplingEnabled;
    private SimpleIntegerProperty downSamplingThreshold;
    private Property<Boolean> sampleSymbolsVisible;
    private Property<Boolean> chartAnimationEnabled;
    private Preferences prefs;

    private static class GlobalPreferencesHolder {
        private final static GlobalPreferences instance = new GlobalPreferences();
    }

    private GlobalPreferences() {
        prefs = Preferences.userRoot().node(BINJR_GLOBAL);
        downSamplingEnabled = new SimpleBooleanProperty(prefs.getBoolean(DOWN_SAMPLING_ENABLED, true));
        downSamplingEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(DOWN_SAMPLING_ENABLED, newValue));
        downSamplingThreshold = new SimpleIntegerProperty(prefs.getInt(DOWN_SAMPLING_THRESHOLD, 1500));
        downSamplingThreshold.addListener((observable, oldValue, newValue) -> prefs.putInt(DOWN_SAMPLING_THRESHOLD, newValue.intValue()));
        sampleSymbolsVisible = new SimpleBooleanProperty(prefs.getBoolean(SAMPLE_SYMBOLS_VISIBLE, false));
        sampleSymbolsVisible.addListener((observable, oldValue, newValue) -> prefs.putBoolean(SAMPLE_SYMBOLS_VISIBLE, newValue));
        chartAnimationEnabled = new SimpleBooleanProperty(prefs.getBoolean(CHART_ANIMATION_ENABLED, false));
        chartAnimationEnabled.addListener((observable, oldValue, newValue) -> prefs.putBoolean(CHART_ANIMATION_ENABLED, newValue));
    }

    /**
     * Returns the singleton instance of {@link GlobalPreferences}
     *
     * @return the singleton instance of {@link GlobalPreferences}
     */
    public static GlobalPreferences getInstance() {
        return GlobalPreferencesHolder.instance;
    }

    /**
     * Returns true if the chart animation is enabled, false otherwise.
     *
     * @return true if the chart animation is enabled, false otherwise.
     */
    public Boolean getChartAnimationEnabled() {
        return downSamplingEnabled.getValue();
    }

    /**
     * Returns the chart animation property
     *
     * @return the chart animation property
     */
    public Property<Boolean> chartAnimationEnabledProperty() {
        return chartAnimationEnabled;
    }

    /**
     * Enables or disables the chart animation
     *
     * @param chartAnimationEnabled true to enable the chart animation, false otherwise.
     */
    public void setChartAnimationEnabled(Boolean chartAnimationEnabled) {
        this.chartAnimationEnabled.setValue(chartAnimationEnabled);
    }

    /**
     * Returns true if the chart symbols are visible, false otherwise.
     *
     * @return true if the chart symbols are visible, false otherwise.
     */
    public Boolean getSampleSymbolsVisible() {
        return sampleSymbolsVisible.getValue();
    }

    /**
     * Return the chart symbols visibility property
     *
     * @return the chart symbols visibility property
     */
    public Property<Boolean> sampleSymbolsVisibleProperty() {
        return sampleSymbolsVisible;
    }

    /**
     * Sets the visibility of chart symbols
     *
     * @param sampleSymbolsVisible the visibility of chart symbols
     */
    public void setSampleSymbolsVisible(Boolean sampleSymbolsVisible) {
        this.sampleSymbolsVisible.setValue(sampleSymbolsVisible);
    }

    /**
     * Returns true if series down-sampling is enabled, false otherwise.
     *
     * @return true if series down-sampling is enabled, false otherwise.
     */
    public Boolean getDownSamplingEnabled() {
        return downSamplingEnabled.getValue();
    }

    /**
     * Returns the down-sampling property
     *
     * @return the down-sampling property
     */
    public Property<Boolean> downSamplingEnabledProperty() {
        return downSamplingEnabled;
    }

    /**
     * Enables or disables series down-sampling
     *
     * @param downSamplingEnabled true to enable series down-sampling, false otherwise.
     */
    public void setDownSamplingEnabled(Boolean downSamplingEnabled) {
        this.downSamplingEnabled.setValue(downSamplingEnabled);
    }

    /**
     * Returns the series down-sampling threshold value
     *
     * @return the series down-sampling threshold value
     */
    public int getDownSamplingThreshold() {
        return downSamplingThreshold.getValue();
    }

    /**
     * Returns the property for the series down-sampling threshold value
     *
     * @return the property for the series down-sampling threshold value
     */
    public Property<Number> downSamplingThresholdProperty() {
        return downSamplingThreshold;
    }

    /**
     * Sets the series down-sampling threshold value
     *
     * @param downSamplingThreshold the series down-sampling threshold value
     */
    public void setDownSamplingThreshold(int downSamplingThreshold) {
        this.downSamplingThreshold.setValue(downSamplingThreshold);
    }
}
