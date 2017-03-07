package eu.fthevenet.binjr.data.workspace;

import com.sun.javafx.collections.ObservableSetWrapper;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A class that represents and holds the current state of a single worksheet
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "Worksheet")
public class Worksheet implements Serializable {
    private static final Logger logger = LogManager.getLogger(Worksheet.class);
    private static final AtomicInteger globalCounter = new AtomicInteger(0);
    private ObservableSetWrapper<TimeSeriesBinding<Number>> series;
    private Property<String> name;
    private Property<ZoneId> timeZone;
    private Property<String> unit;
    private Property<UnitPrefixes> unitPrefixes;
    private Property<ChartType> chartType;
    private Property<ZonedDateTime> fromDateTime;
    private Property<ZonedDateTime> toDateTime;
    @XmlTransient
    private final BooleanProperty dirty;

    /**
     * Initializes a new instance of the {@link Worksheet} class
     */
    public Worksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() + ")",
                ChartType.STACKED,
                ZoneId.systemDefault(),
                new HashSet<>(),
                ZonedDateTime.now().minus(24, ChronoUnit.HOURS), ZonedDateTime.now(), "-", UnitPrefixes.METRIC);
    }

    /**
     * Initializes a new instance of the {@link Worksheet} class with the provided name, chart type and zoneid
     *
     * @param name      the name for the new {@link Worksheet} instance
     * @param chartType the {@link ChartType} for the new {@link Worksheet} instance
     * @param timezone  the {@link ZoneId} for the new {@link Worksheet} instance
     */
    public Worksheet(String name, ChartType chartType, ZoneId timezone) {
        this(name, chartType, timezone,
                new HashSet<>(),
                ZonedDateTime.now().minus(24, ChronoUnit.HOURS), ZonedDateTime.now(), "-", UnitPrefixes.METRIC);
    }

    /**
     * Copy constructor to clone a {@link Worksheet} instance.
     *
     * @param initWorksheet the {@link Worksheet} instance to clone.
     */
    public Worksheet(Worksheet initWorksheet) {
        this(initWorksheet.getName(),
                initWorksheet.getChartType(),
                initWorksheet.getTimeZone(),
                initWorksheet.getSeries(),
                initWorksheet.getFromDateTime(),
                initWorksheet.getToDateTime(),
                initWorksheet.getUnit(),
                initWorksheet.getUnitPrefixes());
    }

    private Worksheet(String name, ChartType chartType, ZoneId timezone, Set<TimeSeriesBinding<Number>> bindings, ZonedDateTime fromDateTime, ZonedDateTime toDateTime, String unitName, UnitPrefixes base) {
        this.name = new SimpleStringProperty(name);
        this.unit = new SimpleStringProperty(unitName);
        this.chartType = new SimpleObjectProperty<>(chartType);
        this.series = new ObservableSetWrapper<>(bindings);
        this.timeZone = new SimpleObjectProperty<>(timezone);
        this.fromDateTime = new SimpleObjectProperty<>(fromDateTime);
        this.toDateTime = new SimpleObjectProperty<>(toDateTime);
        this.unitPrefixes = new SimpleObjectProperty<>(base);
        this.dirty = new SimpleBooleanProperty(false);

        ChangeListener<Object> setDirty = (observable, oldValue, newValue) -> dirty.setValue(true);
        this.nameProperty().addListener(setDirty);
        this.unitProperty().addListener(setDirty);
        this.chartTypeProperty().addListener(setDirty);
        this.timeZoneProperty().addListener(setDirty);
        this.fromDateTimeProperty().addListener(setDirty);
        this.toDateTimeProperty().addListener(setDirty);
        this.unitPrefixesProperty().addListener(setDirty);
        //  this.series.addListener((InvalidationListener) observable -> dirty = true);
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
    @XmlTransient
    public ObservableSetWrapper<TimeSeriesBinding<Number>> getSeries() {
        return series;
    }

    /**
     * The time series of the {@link Worksheet}
     *
     * @param series the time series of the {@link Worksheet}
     */
    public void setSeries(ObservableSetWrapper<TimeSeriesBinding<Number>> series) {
        this.series = series;
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

    @Override
    public String toString() {
        return String.format("%s - %s - %s",
                getName(),
                getTimeZone().toString(),
                getChartType().toString()
        );
    }

    @XmlTransient
    boolean isDirty() {
        return dirty.getValue();
    }

    BooleanProperty dirtyProperty() {
        return dirty;
    }

    void setSaved() {
        dirty.setValue(false);
    }
}

