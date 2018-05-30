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

package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.dirtyable.ChangeWatcher;
import eu.fthevenet.binjr.data.dirtyable.Dirtyable;
import eu.fthevenet.binjr.data.dirtyable.IsDirtyable;
import eu.fthevenet.binjr.data.exceptions.DataAdapterException;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import eu.fthevenet.binjr.data.timeseries.transform.DecimationTransform;
import eu.fthevenet.binjr.data.timeseries.transform.TimeSeriesTransform;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.*;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * A class that represents and holds the current state of a single chart view
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "Chart")
public class Chart<T> implements Dirtyable, AutoCloseable {
    private static final Logger logger = LogManager.getLogger(Chart.class);
    private static final AtomicInteger globalCounter = new AtomicInteger(0);
    @IsDirtyable
    private ObservableList<TimeSeriesInfo<T>> series;
    @IsDirtyable
    private Property<String> name;
    @IsDirtyable
    private Property<String> unit;
    @IsDirtyable
    private Property<UnitPrefixes> unitPrefixes;
    @IsDirtyable
    private Property<ChartType> chartType;
    @IsDirtyable
    private DoubleProperty graphOpacity;
    @IsDirtyable
    private BooleanProperty showAreaOutline;
    @IsDirtyable
    private DoubleProperty strokeWidth;
    @IsDirtyable
    private BooleanProperty autoScaleYAxis;
    @IsDirtyable
    private DoubleProperty yAxisMinValue;
    @IsDirtyable
    private DoubleProperty yAxisMaxValue;

    private final ChangeWatcher status;

    /**
     * Initializes a new instance of the {@link Worksheet} class
     */
    public Chart() {
        this("New Chart (" + globalCounter.getAndIncrement() + ")",
                ChartType.STACKED,
                FXCollections.observableList(new LinkedList<>()),
                "-",
                UnitPrefixes.METRIC,
                GlobalPreferences.getInstance().getDefaultGraphOpacity(),
                GlobalPreferences.getInstance().isShowAreaOutline(),
                1.0,
                true,
                0.0,
                100.0);
    }

    /**
     * Initializes a new instance of the {@link Worksheet} class with the provided name, chart type and zoneid
     *
     * @param name      the name for the new {@link Worksheet} instance
     * @param chartType the {@link ChartType} for the new {@link Worksheet} instance

     */
    public Chart(String name, ChartType chartType, String unitName, UnitPrefixes prefix) {
        this(name,
                chartType,
                FXCollections.observableList(new LinkedList<>()),
                unitName,
                prefix,
                GlobalPreferences.getInstance().getDefaultGraphOpacity(),
                GlobalPreferences.getInstance().isShowAreaOutline(),
                1.0,
                true,
                0.0,
                100.0);
    }

    /**
     * Copy constructor to deep clone a {@link Chart} instance.
     *
     * @param initChart the {@link Chart} instance to clone.
     */
    public Chart(Chart<T> initChart) {
        this(initChart.getName(),
                initChart.getChartType(),
                initChart.getSeries().stream()
                        .map(TimeSeriesInfo::new)
                        .collect(Collectors.toCollection(() -> FXCollections.observableList(new LinkedList<>()))),
                initChart.getUnit(),
                initChart.getUnitPrefixes(),
                initChart.getGraphOpacity(),
                initChart.isShowAreaOutline(),
                initChart.getStrokeWidth(),
                initChart.isAutoScaleYAxis(),
                initChart.getyAxisMinValue(),
                initChart.getyAxisMaxValue()
        );
    }

    private Chart(String name,
                  ChartType chartType,
                  List<TimeSeriesInfo<T>> bindings,
                  String unitName,
                  UnitPrefixes base,
                  double graphOpacity,
                  boolean showAreaOutline,
                  double strokeWidth,
                  boolean autoScaleYAxis,
                  double yAxisMinValue,
                  double yAxisMaxValue) {
        this.name = new SimpleStringProperty(name);
        this.unit = new SimpleStringProperty(unitName);
        this.chartType = new SimpleObjectProperty<>(chartType);
        this.series = FXCollections.observableList(new LinkedList<>(bindings));
//        this.timeZone = new SimpleObjectProperty<>(timezone);
//        this.fromDateTime = new SimpleObjectProperty<>(fromDateTime);
//        this.toDateTime = new SimpleObjectProperty<>(toDateTime);
        this.unitPrefixes = new SimpleObjectProperty<>(base);
        this.graphOpacity = new SimpleDoubleProperty(graphOpacity);
        this.showAreaOutline = new SimpleBooleanProperty(showAreaOutline);
        this.strokeWidth = new SimpleDoubleProperty(strokeWidth);
        this.autoScaleYAxis = new SimpleBooleanProperty(autoScaleYAxis);
        this.yAxisMinValue = new SimpleDoubleProperty(yAxisMinValue);
        this.yAxisMaxValue = new SimpleDoubleProperty(yAxisMaxValue);

        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
    }

    /**
     * Fills up the backend for all {@link TimeSeriesInfo} in the worksheet with data from the adapter on the specified time interval
     *
     * @param startTime the start of the time interval
     * @param endTime   the end of the time interval
     * @throws DataAdapterException if an error occurs while retrieving data from the adapter
     */
    public void fetchDataFromSources(ZonedDateTime startTime, ZonedDateTime endTime, boolean bypassCache) throws DataAdapterException {
        // Define the reduction transform to apply
        TimeSeriesTransform<T> reducer = new DecimationTransform<>(GlobalPreferences.getInstance().getDownSamplingThreshold());
        // Group all bindings by common adapters
        Map<DataAdapter<T, ?>, List<TimeSeriesInfo<T>>> bindingsByAdapters = getSeries().stream().collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (Map.Entry<DataAdapter<T, ?>, List<TimeSeriesInfo<T>>> byAdapterEntry : bindingsByAdapters.entrySet()) {
            DataAdapter<T, ?> adapter = byAdapterEntry.getKey();
            // Group all bindings-by-adapters by path
            Map<String, List<TimeSeriesInfo<T>>> bindingsByPath = byAdapterEntry.getValue().stream().collect(groupingBy(o -> o.getBinding().getPath()));
            for (Map.Entry<String, List<TimeSeriesInfo<T>>> byPathEntry : bindingsByPath.entrySet()) {
                String path = byPathEntry.getKey();
                // Get data for source
                Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> data = adapter.fetchDecodedData(path, startTime.toInstant(), endTime.toInstant(), byPathEntry.getValue(), bypassCache);
                // Applying point reduction
                data = reducer.transform(data, GlobalPreferences.getInstance().getDownSamplingEnabled());
                //Update timeSeries data
                for (TimeSeriesInfo<T> seriesInfo : data.keySet()) {
                    seriesInfo.setProcessor(data.get(seriesInfo));
                }
            }
        }
    }

    /**
     * Adds a {@link TimeSeriesInfo} to the worksheet
     *
     * @param seriesInfo the {@link TimeSeriesInfo} to add
     */
    public void addSeries(TimeSeriesInfo<T> seriesInfo) {
        series.add(seriesInfo);
    }

    /**
     * Adds a collection of  {@link TimeSeriesInfo} to the worksheet
     *
     * @param seriesInfo the collection {@link TimeSeriesInfo} to add
     */
    public void addSeries(Collection<TimeSeriesInfo<T>> seriesInfo) {
        this.series.addAll(seriesInfo);
    }

    /**
     * Remove all the elements in the provided collection from the list of {@link Worksheet} instances
     *
     * @param seriesInfo the list of {@link Worksheet} instances to remove
     */
    public void removeSeries(Collection<TimeSeriesInfo> seriesInfo) {
        series.removeAll(seriesInfo);
    }

    /**
     * Clear the {@link Worksheet} list
     */
    public void clearSeries() {
        series.clear();
    }


    /**
     * The name of the {@link Worksheet}
     *
     * @return the name of the {@link Worksheet}
     */
    @XmlAttribute
    public String getName() {
        return name.getValue();
    }

    /**
     * The name of the {@link Worksheet}
     *
     * @return An instance of {@link Property} for the name of the {@link Worksheet}
     */
    public Property<String> nameProperty() {
        return name;
    }

    /**
     * The name of the {@link Worksheet}
     *
     * @param name the name of the {@link Worksheet}
     */
    public void setName(String name) {
        this.name.setValue(name);
    }

    /**
     * The time series of the {@link Worksheet}
     *
     * @return the time series of the {@link Worksheet}
     */

    //   @XmlTransient
    @XmlElementWrapper(name = "SeriesList")
    @XmlElements(@XmlElement(name = "Timeseries"))
    public ObservableList<TimeSeriesInfo<T>> getSeries() {
        return series;
    }

    /**
     * The type of chart hosted by the  {@link Worksheet}
     *
     * @return the type of chart hosted by the  {@link Worksheet}
     */
    @XmlAttribute
    public ChartType getChartType() {
        return chartType.getValue();
    }

    /**
     * The type of chart hosted by the  {@link Worksheet}
     *
     * @return An instance of {@link Property} for the type of chart hosted by the  {@link Worksheet}
     */
    public Property<ChartType> chartTypeProperty() {
        return chartType;
    }

    /**
     * The type of chart hosted by the  {@link Worksheet}
     *
     * @param chartType the type of chart hosted by the {@link Worksheet}
     */
    public void setChartType(ChartType chartType) {
        this.chartType.setValue(chartType);
    }

    /**
     * The unit for the {@link Worksheet}'s times series Y axis
     *
     * @return the unit for the {@link Worksheet}'s times series Y axis
     */
    @XmlAttribute
    public String getUnit() {
        return unit.getValue();
    }

    /**
     * The unit for the {@link Worksheet}'s times series Y axis
     *
     * @return An instance of {@link Property} for the unit for the {@link Worksheet}'s times series Y axis
     */
    public Property<String> unitProperty() {
        return unit;
    }

    /**
     * The unit for the {@link Worksheet}'s times series Y axis
     *
     * @param unit the unit for the {@link Worksheet}'s times series Y axis
     */
    public void setUnit(String unit) {
        this.unit.setValue(unit);
    }

    /**
     * The unit prefix for the {@link Worksheet}'s times series Y axis
     *
     * @return the unit prefix for the {@link Worksheet}'s times series Y axis
     */
    @XmlAttribute
    public UnitPrefixes getUnitPrefixes() {
        return unitPrefixes.getValue();
    }

    /**
     * The unit prefix for the {@link Worksheet}'s times series Y axis
     *
     * @return An instance of {@link Property} for the unit prefix for the {@link Worksheet}'s times series Y axis
     */
    public Property<UnitPrefixes> unitPrefixesProperty() {
        return unitPrefixes;
    }

    /**
     * The unit prefix for the {@link Worksheet}'s times series Y axis
     *
     * @param unitPrefixes the unit prefix for the {@link Worksheet}'s times series Y axis
     */
    public void setUnitPrefixes(UnitPrefixes unitPrefixes) {
        this.unitPrefixes.setValue(unitPrefixes);
    }

    /**
     * Gets the opacity factor to apply the the graph
     *
     * @return the opacity factor to apply the the graph
     */
    @XmlAttribute
    public double getGraphOpacity() {
        return graphOpacity.get();
    }

    /**
     * The graphOpacity property
     *
     * @return the graphOpacity property
     */
    public DoubleProperty graphOpacityProperty() {
        return graphOpacity;
    }

    /**
     * Sets the opacity factor to apply the the graph
     *
     * @param graphOpacity the opacity factor to apply the the graph
     */
    public void setGraphOpacity(double graphOpacity) {
        this.graphOpacity.set(graphOpacity);
    }

    /**
     * Returns true if area charts should display an outline stroke, false otherwise
     *
     * @return true if area charts should display an outline stroke, false otherwise
     */
    @XmlAttribute
    public boolean isShowAreaOutline() {
        return showAreaOutline.get();
    }

    /**
     * The showAreaOutline property
     *
     * @return The showAreaOutline property
     */
    public BooleanProperty showAreaOutlineProperty() {
        return showAreaOutline;
    }

    /**
     * Set to true if area charts should display an outline stroke, false otherwise
     *
     * @param showAreaOutline true if area charts should display an outline stroke, false otherwise
     */
    public void setShowAreaOutline(boolean showAreaOutline) {
        this.showAreaOutline.set(showAreaOutline);
    }

    /**
     * The strokeWidth property.
     *
     * @return The strokewidth property.
     */
    public DoubleProperty strokeWidthProperty() {
        return strokeWidth;
    }

    /**
     * Return the stroke width for line charts
     *
     * @return the stroke width for line charts
     */
    @XmlAttribute
    public double getStrokeWidth() {
        return strokeWidth.get();
    }

    /**
     * Sets the stroke width for line charts.
     *
     * @param value the stroke width for line charts.
     */
    public void setStrokeWidth(double value) {
        strokeWidth.setValue(value);
    }


    @Override
    public String toString() {
        return String.format("%s - %s",
                getName(),
                getChartType().toString()
        );
    }

    @XmlTransient
    @Override
    public Boolean isDirty() {
        return status.isDirty();
    }

    @Override
    public BooleanProperty dirtyProperty() {
        return status.dirtyProperty();
    }

    @Override
    public void cleanUp() {
        status.cleanUp();
    }

    @Override
    public void close() {
        series.clear();
    }

    @XmlAttribute
    public boolean isAutoScaleYAxis() {
        return autoScaleYAxis.get();
    }

    public BooleanProperty autoScaleYAxisProperty() {
        return autoScaleYAxis;
    }

    public void setAutoScaleYAxis(boolean autoScaleYAxis) {
        this.autoScaleYAxis.set(autoScaleYAxis);
    }

    @XmlAttribute
    public double getyAxisMinValue() {
        return yAxisMinValue.get();
    }

    public DoubleProperty yAxisMinValueProperty() {
        return yAxisMinValue;
    }

    public void setyAxisMinValue(double yAxisMinValue) {
        this.yAxisMinValue.set(yAxisMinValue);
    }

    @XmlAttribute
    public double getyAxisMaxValue() {
        return yAxisMaxValue.get();
    }

    public DoubleProperty yAxisMaxValueProperty() {
        return yAxisMaxValue;
    }

    public void setyAxisMaxValue(double yAxisMaxValue) {
        this.yAxisMaxValue.set(yAxisMaxValue);
    }
}

