/*
 *    Copyright 2016-2020 Frederic Thevenet
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

package eu.binjr.core.data.workspace;

import eu.binjr.common.io.IOUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.Dirtyable;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.SourceCommunicationException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.timeseries.transform.AlignBoundariesTransform;
import eu.binjr.core.data.timeseries.transform.NanToZeroTransform;
import eu.binjr.core.data.timeseries.transform.SortTransform;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserPreferences;
import jakarta.xml.bind.annotation.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
public class Chart implements Dirtyable, AutoCloseable, Rangeable<Double> {
    private static final Logger logger = Logger.create(Chart.class);
    private static final AtomicInteger globalCounter = new AtomicInteger(0);

    @IsDirtyable
    private final transient ObservableList<TimeSeriesInfo<Double>> series;
    @IsDirtyable
    private final Property<String> name;
    @IsDirtyable
    private final Property<String> unit;
    @IsDirtyable
    private final Property<UnitPrefixes> unitPrefixes;
    @IsDirtyable
    private final Property<ChartType> chartType;
    @IsDirtyable
    private final DoubleProperty graphOpacity;
    @IsDirtyable
    private final BooleanProperty showAreaOutline;
    @IsDirtyable
    private final DoubleProperty strokeWidth;
    @IsDirtyable
    private final BooleanProperty autoScaleYAxis;
    @IsDirtyable
    private final DoubleProperty yAxisMinValue;
    @IsDirtyable
    private final DoubleProperty yAxisMaxValue;

    private final transient ChangeWatcher status;
    private final transient BooleanProperty showProperties;
    private final transient UserPreferences userPref;

    /**
     * Initializes a new instance of the {@link Chart} class
     */
    public Chart() {
        this("New Chart (" + globalCounter.getAndIncrement() + ")",
                ChartType.STACKED,
                FXCollections.observableList(new LinkedList<>()),
                "-",
                UnitPrefixes.METRIC,
                UserPreferences.getInstance().defaultOpacityStackedAreaCharts.get().doubleValue(),
                UserPreferences.getInstance().showOutlineOnStackedAreaCharts.get(),
                1.0,
                true,
                0.0,
                100.0);
    }

    /**
     * Initializes a new instance of the {@link Chart} class with the provided name, chart type and zoneid
     *
     * @param name      the name for the new {@link Chart} instance
     * @param chartType the {@link ChartType} for the new {@link Chart} instance
     * @param unitName  the label for the unit of Y axis.
     * @param prefix    the unit prefix to use for the unit of Y axis.
     */
    public Chart(String name, ChartType chartType, String unitName, UnitPrefixes prefix) {
        this(name,
                chartType,
                FXCollections.observableList(new LinkedList<>()),
                unitName,
                prefix,
                chartType == ChartType.STACKED ?
                        UserPreferences.getInstance().defaultOpacityStackedAreaCharts.get().doubleValue() :
                        UserPreferences.getInstance().defaultOpacityAreaCharts.get().doubleValue(),
                chartType == ChartType.STACKED ?
                        UserPreferences.getInstance().showOutlineOnStackedAreaCharts.get() :
                        UserPreferences.getInstance().showOutlineOnAreaCharts.get(),
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
    public Chart(Chart initChart) {
        this(initChart.getName(),
                initChart.getChartType(),
                initChart.getSeries().stream()
                        .map(TimeSeriesInfo<Double>::new)
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
                  List<TimeSeriesInfo<Double>> bindings,
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
        this.unitPrefixes = new SimpleObjectProperty<>(base);
        this.graphOpacity = new SimpleDoubleProperty(graphOpacity);
        this.showAreaOutline = new SimpleBooleanProperty(showAreaOutline);
        this.strokeWidth = new SimpleDoubleProperty(strokeWidth);
        this.autoScaleYAxis = new SimpleBooleanProperty(autoScaleYAxis);
        this.yAxisMinValue = new SimpleDoubleProperty(yAxisMinValue);
        this.yAxisMaxValue = new SimpleDoubleProperty(yAxisMaxValue);
        this.showProperties = new SimpleBooleanProperty(false);

        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
        userPref = UserPreferences.getInstance();
    }

    /**
     * Fills up the backend for all {@link TimeSeriesInfo} in the chart by querying the relevant data adapters
     * for the specified time interval.
     *
     * @param startTime   the start of the time interval
     * @param endTime     the end of the time interval
     * @param bypassCache set to true to forcefully bypass any cache on the adapter.
     * @throws DataAdapterException if an error occurs while retrieving data from the adapter
     */
    public void fetchDataFromSources(ZonedDateTime startTime, ZonedDateTime endTime, boolean bypassCache)
            throws DataAdapterException {
        // prune series from closed adapters
        series.removeIf(seriesInfo -> {
            if (seriesInfo.getBinding().getAdapter().isClosed()) {
                logger.debug(() -> seriesInfo.getDisplayName() + " will be pruned because attached adapter " +
                        seriesInfo.getBinding().getAdapter().getId() + " is closed.");
                return true;
            }
            return false;
        });

        var align = new AlignBoundariesTransform(startTime, endTime, this.chartType.getValue() != ChartType.STACKED);
        var clean = new NanToZeroTransform();
        clean.setEnabled(userPref.forceNanToZero.get());
        // Group all bindings by common adapters
        var bindingsByAdapters = getSeries().stream()
                .collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (var byAdapterEntry : bindingsByAdapters.entrySet()) {
            // Define the transforms to apply
            var reduce = userPref.downSamplingAlgorithm.get().instantiateTransform(getChartType(),
                    userPref.downSamplingThreshold.get().intValue());
            reduce.setEnabled(userPref.downSamplingEnabled.get());
            DataAdapter<Double> adapter = (DataAdapter<Double>) byAdapterEntry.getKey();
            SortTransform<Double> sort = new SortTransform<>();
            sort.setEnabled(adapter.isSortingRequired());
            // Group all queries with the same adapter and path
            var bindingsByPath = byAdapterEntry.getValue().stream()
                    .collect(groupingBy(o -> o.getBinding().getPath()));
            var latch = new CountDownLatch(bindingsByPath.entrySet().size());
            var errors = new ArrayList<Throwable>();
            for (var byPathEntry : bindingsByPath.entrySet()) {
                AsyncTaskManager.getInstance().submitSubTask(
                        () -> {
                            try {
                                String path = byPathEntry.getKey();
                                logger.trace("Fetch sub-task '" + path + "' started");
                                // Get data from the adapter
                                Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> data;
                                try {
                                    data = adapter.fetchData(
                                            path,
                                            startTime.toInstant(),
                                            endTime.toInstant(),
                                            byPathEntry.getValue(),
                                            bypassCache);
                                } catch (DataAdapterException e) {
                                    Dialogs.notifyException("An error occurred while fetching data from source", e);
                                    data = new HashMap<>();
                                }
                                if (data.isEmpty()) {
                                    // initialize processors with at least boundaries samples in it
                                    for (var info : byPathEntry.getValue()) {
                                        var proc = new DoubleTimeSeriesProcessor();
                                        proc.addSample(startTime, Double.NaN);
                                        proc.addSample(endTime, Double.NaN);
                                        data.put(info, proc);
                                    }
                                }
                                data.entrySet().parallelStream().forEach(entry -> {
                                    var info = entry.getKey();
                                    var proc = entry.getValue();
                                    //bind proc to timeSeries info
                                    info.setProcessor(proc);
                                    // Applying sample transforms
                                    proc.applyTransforms(clean, sort, reduce);
                                });
                                // Run second pass transforms and time frame alignment
                                data.entrySet().parallelStream().forEach(entry -> {
                                    entry.getValue().applyTransforms(reduce.getNextPassTransform(), align);
                                });
                            } catch (Throwable t) {
                                logger.error(t);
                                errors.add(t);
                            } finally {
                                logger.trace("Fetch sub-task 'for path'" + byPathEntry.getKey() + "' done");
                                latch.countDown();
                            }
                        });
            }
            try {
                if (!latch.await(userPref.asyncTasksTimeOutMs.get().longValue(), TimeUnit.MILLISECONDS)) {
                    throw new DataAdapterException("Waiting for fetch sub-tasks to complete aborted");
                }
                if (!errors.isEmpty()) {
                    for (var t : errors) {
                        //FIXME only first exception is rethrown
                        if (t instanceof DataAdapterException) {
                            throw (DataAdapterException) t;
                        } else {
                            throw new DataAdapterException("Unexpected error while retrieving data from adapter: " + t.getMessage(), t);
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new DataAdapterException("Async fetch task interrupted", e);
            }
        }
    }

    /**
     * Adds a {@link TimeSeriesInfo} to the Chart
     *
     * @param seriesInfo the {@link TimeSeriesInfo} to add
     */
    public void addSeries(TimeSeriesInfo<Double> seriesInfo) {
        series.add(seriesInfo);
    }

    /**
     * Adds a collection of  {@link TimeSeriesInfo} to the chart
     *
     * @param seriesInfo the collection {@link TimeSeriesInfo} to add
     */
    public void addSeries(Collection<TimeSeriesInfo<Double>> seriesInfo) {
        this.series.addAll(seriesInfo);
    }

    /**
     * Remove all the elements in the provided collection from the list of {@link Chart} instances
     *
     * @param seriesInfo the list of {@link Chart} instances to remove
     */
    public void removeSeries(Collection<TimeSeriesInfo<Double>> seriesInfo) {
        series.removeAll(seriesInfo);
    }

    /**
     * Clear the series list
     */
    public void clearSeries() {
        series.clear();
    }


    /**
     * The name of the {@link Chart}
     *
     * @return the name of the {@link Chart}
     */
    @XmlAttribute
    public String getName() {
        return name.getValue();
    }

    /**
     * The name of the {@link Chart}
     *
     * @param name the name of the {@link Chart}
     */
    public void setName(String name) {
        this.name.setValue(name);
    }

    /**
     * The name of the {@link Chart}
     *
     * @return An instance of {@link Property} for the name of the {@link Chart}
     */
    public Property<String> nameProperty() {
        return name;
    }

    /**
     * The time series of the {@link Chart}
     *
     * @return the time series of the {@link Chart}
     */

    //   @XmlTransient
    @XmlElementWrapper(name = "SeriesList")
    @XmlElements(@XmlElement(name = "Timeseries"))
    public ObservableList<TimeSeriesInfo<Double>> getSeries() {
        return series;
    }

    /**
     * The type of chart hosted by the  {@link Chart}
     *
     * @return the type of chart hosted by the  {@link Chart}
     */
    @XmlAttribute
    public ChartType getChartType() {
        return chartType.getValue();
    }

    /**
     * The type of chart hosted by the  {@link Chart}
     *
     * @param chartType the type of chart hosted by the {@link Chart}
     */
    public void setChartType(ChartType chartType) {
        this.chartType.setValue(chartType);
    }

    /**
     * The type of chart hosted by the  {@link Chart}
     *
     * @return An instance of {@link Property} for the type of chart hosted by the  {@link Chart}
     */
    public Property<ChartType> chartTypeProperty() {
        return chartType;
    }

    /**
     * The unit for the {@link Chart}'s times series Y axis
     *
     * @return the unit for the {@link Chart}'s times series Y axis
     */
    @XmlAttribute
    public String getUnit() {
        return unit.getValue();
    }

    /**
     * The unit for the {@link Chart}'s times series Y axis
     *
     * @param unit the unit for the {@link Chart}'s times series Y axis
     */
    public void setUnit(String unit) {
        this.unit.setValue(unit);
    }

    /**
     * The unit for the {@link Chart}'s times series Y axis
     *
     * @return An instance of {@link Property} for the unit for the {@link Chart}'s times series Y axis
     */
    public Property<String> unitProperty() {
        return unit;
    }

    /**
     * The unit prefix for the {@link Chart}'s times series Y axis
     *
     * @return the unit prefix for the {@link Chart}'s times series Y axis
     */
    @XmlAttribute
    public UnitPrefixes getUnitPrefixes() {
        return unitPrefixes.getValue();
    }

    /**
     * The unit prefix for the {@link Chart}'s times series Y axis
     *
     * @param unitPrefixes the unit prefix for the {@link Chart}'s times series Y axis
     */
    public void setUnitPrefixes(UnitPrefixes unitPrefixes) {
        this.unitPrefixes.setValue(unitPrefixes);
    }

    /**
     * The unit prefix for the {@link Chart}'s times series Y axis
     *
     * @return An instance of {@link Property} for the unit prefix for the {@link Chart}'s times series Y axis
     */
    public Property<UnitPrefixes> unitPrefixesProperty() {
        return unitPrefixes;
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
     * Sets the opacity factor to apply the the graph
     *
     * @param graphOpacity the opacity factor to apply the the graph
     */
    public void setGraphOpacity(double graphOpacity) {
        this.graphOpacity.set(graphOpacity);
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
     * Returns true if area charts should display an outline stroke, false otherwise
     *
     * @return true if area charts should display an outline stroke, false otherwise
     */
    @XmlAttribute
    public boolean isShowAreaOutline() {
        return showAreaOutline.get();
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
     * The showAreaOutline property
     *
     * @return The showAreaOutline property
     */
    public BooleanProperty showAreaOutlineProperty() {
        return showAreaOutline;
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
        IOUtils.closeAll(series);
        this.status.close();
    }

    @XmlAttribute
    public boolean isAutoScaleYAxis() {
        return autoScaleYAxis.get();
    }

    public void setAutoScaleYAxis(boolean autoScaleYAxis) {
        this.autoScaleYAxis.set(autoScaleYAxis);
    }

    public BooleanProperty autoScaleYAxisProperty() {
        return autoScaleYAxis;
    }

    @XmlAttribute
    public double getyAxisMinValue() {
        return yAxisMinValue.getValue();
    }

    public void setyAxisMinValue(double yAxisMinValue) {
        this.yAxisMinValue.setValue(yAxisMinValue);
    }

    public DoubleProperty yAxisMinValueProperty() {
        return yAxisMinValue;
    }

    @XmlAttribute
    public double getyAxisMaxValue() {
        return yAxisMaxValue.getValue();
    }

    public void setyAxisMaxValue(double yAxisMaxValue) {
        this.yAxisMaxValue.setValue(yAxisMaxValue);
    }

    public DoubleProperty yAxisMaxValueProperty() {
        return yAxisMaxValue;
    }

    @XmlTransient
    public boolean isShowProperties() {
        return showProperties.get();
    }

    public void setShowProperties(boolean showProperties) {
        this.showProperties.set(showProperties);
    }

    public BooleanProperty showPropertiesProperty() {
        return showProperties;
    }
}

