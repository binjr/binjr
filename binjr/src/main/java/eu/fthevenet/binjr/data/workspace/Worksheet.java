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

package eu.fthevenet.binjr.data.workspace;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.exceptions.DataAdapterException;
import eu.fthevenet.binjr.data.dirtyable.ChangeWatcher;
import eu.fthevenet.binjr.data.dirtyable.Dirtyable;
import eu.fthevenet.binjr.data.dirtyable.IsDirtyable;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;


/**
 * A class that represents and holds the current state of a single worksheet
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "Worksheet")
public class Worksheet<T extends Number> implements Serializable, Dirtyable, AutoCloseable {
    private static final Logger logger = LogManager.getLogger(Worksheet.class);
    private static final AtomicInteger globalCounter = new AtomicInteger(0);
    @IsDirtyable
    private ObservableList<TimeSeriesInfo<T>> series;
    @IsDirtyable
    private Property<String> name;
    @IsDirtyable
    private Property<ZoneId> timeZone;
    @IsDirtyable
    private Property<String> unit;
    @IsDirtyable
    private Property<UnitPrefixes> unitPrefixes;
    @IsDirtyable
    private Property<ChartType> chartType;
    @IsDirtyable
    private Property<ZonedDateTime> fromDateTime;
    @IsDirtyable
    private Property<ZonedDateTime> toDateTime;
    @IsDirtyable
    private DoubleProperty graphOpacity;
    @IsDirtyable
    private BooleanProperty showAreaOutline;
    @IsDirtyable
    private BooleanProperty showChartSymbols;


    private final ChangeWatcher<Worksheet> status;

    /**
     * Initializes a new instance of the {@link Worksheet} class
     */
    public Worksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() + ")",
                ChartType.STACKED,
                ZoneId.systemDefault(),
                FXCollections.observableList(new LinkedList<>()),
                ZonedDateTime.now().minus(24, ChronoUnit.HOURS), ZonedDateTime.now(), "-",
                UnitPrefixes.METRIC,
                GlobalPreferences.getInstance().getDefaultGraphOpacity(),
                GlobalPreferences.getInstance().isShowAreaOutline(),
                GlobalPreferences.getInstance().getSampleSymbolsVisible());
    }

    /**
     * Initializes a new instance of the {@link Worksheet} class with the provided name, chart type and zoneid
     *
     * @param name      the name for the new {@link Worksheet} instance
     * @param chartType the {@link ChartType} for the new {@link Worksheet} instance
     * @param timezone  the {@link ZoneId} for the new {@link Worksheet} instance
     */
    public Worksheet(String name, ChartType chartType, ZonedDateTime fromDateTime, ZonedDateTime toDateTime, ZoneId timezone, String unitName, UnitPrefixes prefix) {
        this(name,
                chartType,
                timezone,
                FXCollections.observableList(new LinkedList<>()),
                fromDateTime,
                toDateTime,
                unitName,
                prefix,
                GlobalPreferences.getInstance().getDefaultGraphOpacity(),
                GlobalPreferences.getInstance().isShowAreaOutline(),
                GlobalPreferences.getInstance().getSampleSymbolsVisible());
    }

    /**
     * Copy constructor to deep clone a {@link Worksheet} instance.
     *
     * @param initWorksheet the {@link Worksheet} instance to clone.
     */
    public Worksheet(Worksheet<T> initWorksheet) {
        this(initWorksheet.getName(),
                initWorksheet.getChartType(),
                initWorksheet.getTimeZone(),
                initWorksheet.getSeries().stream()
                        .map(TimeSeriesInfo::new)
                        .collect(Collectors.toCollection(() -> FXCollections.observableList(new LinkedList<>()))),
                initWorksheet.getFromDateTime(),
                initWorksheet.getToDateTime(),
                initWorksheet.getUnit(),
                initWorksheet.getUnitPrefixes(),
                initWorksheet.getGraphOpacity(),
                initWorksheet.isShowAreaOutline(),
                initWorksheet.isShowChartSymbols());
    }

    private Worksheet(String name,
                      ChartType chartType,
                      ZoneId timezone,
                      List<TimeSeriesInfo<T>> bindings,
                      ZonedDateTime fromDateTime,
                      ZonedDateTime toDateTime,
                      String unitName,
                      UnitPrefixes base,
                      double graphOpacity,
                      boolean showAreaOutline,
                      boolean showChartSymbols) {
        this.name = new SimpleStringProperty(name);
        this.unit = new SimpleStringProperty(unitName);
        this.chartType = new SimpleObjectProperty<>(chartType);
        this.series = FXCollections.observableList(new LinkedList<>(bindings));
        this.timeZone = new SimpleObjectProperty<>(timezone);
        this.fromDateTime = new SimpleObjectProperty<>(fromDateTime);
        this.toDateTime = new SimpleObjectProperty<>(toDateTime);
        this.unitPrefixes = new SimpleObjectProperty<>(base);
        this.graphOpacity = new SimpleDoubleProperty(graphOpacity);
        this.showAreaOutline = new SimpleBooleanProperty(showAreaOutline);
        this.showChartSymbols = new SimpleBooleanProperty(showChartSymbols);
        this.status = new ChangeWatcher<>(this);
    }

    /**
     * Fills up the backend for all {@link TimeSeriesInfo} in the worksheet with data from the adapter on the specified time interval
     *
     * @param startTime the start of the time interval
     * @param endTime   the end of the time interval
     * @throws DataAdapterException if an error occurs while retrieving data from the adapter
     */
    public void fillData(ZonedDateTime startTime, ZonedDateTime endTime, boolean bypassCache) throws DataAdapterException {
        // Define the reduction transform to apply
        //   TimeSeriesTransform<T> reducer = new LargestTriangleThreeBucketsTransform<>(GlobalPreferences.getInstance().getDownSamplingThreshold());
        TimeSeriesTransform<T> reducer = new DecimationTransform<>(GlobalPreferences.getInstance().getDownSamplingThreshold());
        // Group all bindings by common adapters
        Map<DataAdapter<T>, List<TimeSeriesInfo<T>>> bindingsByAdapters = getSeries().stream().collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (Map.Entry<DataAdapter<T>, List<TimeSeriesInfo<T>>> byAdapterEntry : bindingsByAdapters.entrySet()) {
            DataAdapter<T> adapter = byAdapterEntry.getKey();
            // Group all bindings-by-adapters by path
            Map<String, List<TimeSeriesInfo<T>>> bindingsByPath = byAdapterEntry.getValue().stream().collect(groupingBy(o -> o.getBinding().getPath()));
            for (Map.Entry<String, List<TimeSeriesInfo<T>>> byPathEntry : bindingsByPath.entrySet()) {
                String path = byPathEntry.getKey();
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    // Get raw data for source
                    try (InputStream in = adapter.getData(path, startTime.toInstant(), endTime.toInstant(), bypassCache)) {
                        // Parse raw data obtained from adapter
                        Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> rawDataMap = adapter.getParser().parse(in, byPathEntry.getValue());
                        // Applying point reduction
                        rawDataMap = reducer.transform(rawDataMap, GlobalPreferences.getInstance().getDownSamplingEnabled());
                        //Update timeSeries data
                        for (TimeSeriesInfo<T> seriesInfo : rawDataMap.keySet()) {
                            seriesInfo.setProcessor(rawDataMap.get(seriesInfo));
                        }
                    }
                } catch (IOException | ParseException e) {
                    throw new DataAdapterException("Error recovering data from source", e);
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
     * The {@link ZoneId} used by the {@link Worksheet} time series
     *
     * @return the {@link ZoneId} used by the {@link Worksheet} time series
     */
    public ZoneId getTimeZone() {
        return timeZone.getValue();
    }

    /**
     * The {@link ZoneId} used by the {@link Worksheet} time series
     *
     * @return An instance of {@link Property} for the {@link ZoneId} used by the {@link Worksheet} time series
     */
    public Property<ZoneId> timeZoneProperty() {
        return timeZone;
    }

    /**
     * The {@link ZoneId} used by the {@link Worksheet} time series
     *
     * @param timeZone the {@link ZoneId} used by the {@link Worksheet} time series
     */
    public void setTimeZone(ZoneId timeZone) {
        this.timeZone.setValue(timeZone);
    }

    /**
     * The type of chart hosted by the  {@link Worksheet}
     *
     * @return the type of chart hosted by the  {@link Worksheet}
     */
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
     * The lower bound of the time interval of the {@link Worksheet}'s times series
     *
     * @return the lower bound of the time interval of the {@link Worksheet}'s times series
     */
    public ZonedDateTime getFromDateTime() {
        return fromDateTime.getValue();
    }

    /**
     * The lower bound of the time interval of the {@link Worksheet}'s times series
     *
     * @return An instance of {@link Property} for the lower bound of the time interval of the {@link Worksheet}'s times series
     */
    public Property<ZonedDateTime> fromDateTimeProperty() {
        return fromDateTime;
    }

    /**
     * The lower bound of the time interval of the {@link Worksheet}'s times series
     *
     * @param fromDateTime the lower bound of the time interval of the {@link Worksheet}'s times series
     */
    public void setFromDateTime(ZonedDateTime fromDateTime) {
        this.fromDateTime.setValue(fromDateTime);
    }

    /**
     * The upper bound of the time interval of the {@link Worksheet}'s times series
     *
     * @return the upper bound of the time interval of the {@link Worksheet}'s times series
     */
    public ZonedDateTime getToDateTime() {
        return toDateTime.getValue();
    }

    /**
     * The upper bound of the time interval of the {@link Worksheet}'s times series
     *
     * @return An instance of {@link Property} for the upper bound of the time interval of the {@link Worksheet}'s times series
     */
    public Property<ZonedDateTime> toDateTimeProperty() {
        return toDateTime;
    }

    /**
     * The upper bound of the time interval of the {@link Worksheet}'s times series
     *
     * @param toDateTime the upper bound of the time interval of the {@link Worksheet}'s times series
     */
    public void setToDateTime(ZonedDateTime toDateTime) {
        this.toDateTime.setValue(toDateTime);
    }

    /**
     * The unit for the {@link Worksheet}'s times series Y axis
     *
     * @return the unit for the {@link Worksheet}'s times series Y axis
     */
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
     * Returns true if charts should display sample symbols, false otherwise
     *
     * @return true if charts should display sample symbols, false otherwise
     */
    public boolean isShowChartSymbols() {
        return showChartSymbols.get();
    }

    /**
     * Returns the showChartSymbols property
     *
     * @return the showChartSymbols property
     */
    public BooleanProperty showChartSymbolsProperty() {
        return showChartSymbols;
    }

    /**
     * Set to true if charts should display sample symbols, false otherwise
     *
     * @param showChartSymbols true if charts should display sample symbols, false otherwise
     */
    public void setShowChartSymbols(boolean showChartSymbols) {
        this.showChartSymbols.set(showChartSymbols);
    }

    @Override
    public String toString() {
        return String.format("%s - %s - %s",
                getName(),
                getTimeZone().toString(),
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
    public void close() throws Exception {
        series.clear();
    }
}

