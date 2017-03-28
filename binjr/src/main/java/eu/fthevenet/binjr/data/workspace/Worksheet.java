package eu.fthevenet.binjr.data.workspace;

import com.sun.javafx.collections.ObservableListWrapper;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.dirtyable.ChangeWatcher;
import eu.fthevenet.binjr.data.dirtyable.Dirtyable;
import eu.fthevenet.binjr.data.dirtyable.IsDirtyable;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesProcessor;
import eu.fthevenet.binjr.data.timeseries.transform.DecimationTransform;
import eu.fthevenet.binjr.data.timeseries.transform.TimeSeriesTransform;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.beans.property.*;
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
public class Worksheet<T extends Number> implements Serializable, Dirtyable {
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

    private final ChangeWatcher<Worksheet> status;

    /**
     * Initializes a new instance of the {@link Worksheet} class
     */
    public Worksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() + ")",
                ChartType.STACKED,
                ZoneId.systemDefault(),
                new ObservableListWrapper<>(new LinkedList<>()),
                ZonedDateTime.now().minus(24, ChronoUnit.HOURS), ZonedDateTime.now(), "-",
                UnitPrefixes.METRIC,
                0.8);
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
                new ObservableListWrapper<>(new LinkedList<>()),
                fromDateTime,
                toDateTime,
                unitName,
                prefix,
                0.8);
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
                        .collect(Collectors.toCollection(() -> new ObservableListWrapper<>(new LinkedList<>()))),
                initWorksheet.getFromDateTime(),
                initWorksheet.getToDateTime(),
                initWorksheet.getUnit(),
                initWorksheet.getUnitPrefixes(),
                initWorksheet.getGraphOpacity());
    }

    private Worksheet(String name, ChartType chartType, ZoneId timezone, List<TimeSeriesInfo<T>> bindings, ZonedDateTime fromDateTime, ZonedDateTime toDateTime, String unitName, UnitPrefixes base, double graphOpacity) {
        this.name = new SimpleStringProperty(name);
        this.unit = new SimpleStringProperty(unitName);
        this.chartType = new SimpleObjectProperty<>(chartType);
        this.series = new ObservableListWrapper<>(new LinkedList<>(bindings));
        this.timeZone = new SimpleObjectProperty<>(timezone);
        this.fromDateTime = new SimpleObjectProperty<>(fromDateTime);
        this.toDateTime = new SimpleObjectProperty<>(toDateTime);
        this.unitPrefixes = new SimpleObjectProperty<>(base);
        this.graphOpacity = new SimpleDoubleProperty(graphOpacity);
        this.status = new ChangeWatcher<>(this);
    }

    public void fillData(ZonedDateTime startTime, ZonedDateTime endTime) throws DataAdapterException{
        // Group all bindings by common adapters
        //   TimeSeriesTransform<T> reducer = new LargestTriangleThreeBucketsTransform<>(GlobalPreferences.getInstance().getDownSamplingThreshold());
        TimeSeriesTransform<T> reducer = new DecimationTransform<>(GlobalPreferences.getInstance().getDownSamplingThreshold());

        Map<DataAdapter<T>, List<TimeSeriesInfo<T>>> bindingsByAdapters = getSeries().stream().collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (Map.Entry<DataAdapter<T>, List<TimeSeriesInfo<T>>> byAdapterEntry : bindingsByAdapters.entrySet()) {
            DataAdapter<T> adapter = byAdapterEntry.getKey();
            // Group all bindings-by-adapters by path
            Map<String, List<TimeSeriesInfo<T>>> bindingsByPath = byAdapterEntry.getValue().stream().collect(groupingBy(o -> o.getBinding().getPath()));
            for (Map.Entry<String, List<TimeSeriesInfo<T>>> byPathEntry : bindingsByPath.entrySet()) {
                String path = byPathEntry.getKey();
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    // Get raw data for source
                    try (InputStream in = adapter.getData(path, startTime.toInstant(), endTime.toInstant())) {
                        // Parse raw data obtained from adapter
                        Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> m = adapter.getParser().parse(in, byPathEntry.getValue());
                        // Applying point reduction
                        m = reducer.transform(m, GlobalPreferences.getInstance().getDownSamplingEnabled());
                        //Update timeSeries data
                        for (TimeSeriesInfo<T> info : m.keySet()){
                            info.setProcessor(m.get(info));
                        }
                    }
                } catch (IOException | ParseException e) {
                    throw new DataAdapterException("Error recovering data from source", e);
                }
            }
        }
    }

    public void addSeries(TimeSeriesInfo<T> seriesInfo) {
        series.add(seriesInfo);
    }

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

    public double getGraphOpacity() {
        return graphOpacity.get();
    }

    public DoubleProperty graphOpacityProperty() {
        return graphOpacity;
    }

    public void setGraphOpacity(double graphOpacity) {
        this.graphOpacity.set(graphOpacity);
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


}

