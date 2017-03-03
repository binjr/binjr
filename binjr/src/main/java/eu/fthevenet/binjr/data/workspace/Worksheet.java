package eu.fthevenet.binjr.data.workspace;

import com.sun.javafx.collections.ObservableSetWrapper;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
 * Represents a single worksheet
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
    private final Property<Boolean> dirty;

    public Worksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() + ")",
                ChartType.STACKED,
                ZoneId.systemDefault(),
                new HashSet<>(),
                ZonedDateTime.now().minus(24, ChronoUnit.HOURS), ZonedDateTime.now(), "-", UnitPrefixes.METRIC);
    }

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

    public Worksheet(String name, ChartType chartType, ZoneId timezone, Set<TimeSeriesBinding<Number>> bindings, ZonedDateTime fromDateTime, ZonedDateTime toDateTime, String unitName, UnitPrefixes base) {
        this.name = new SimpleStringProperty(name);
        this.unit = new SimpleStringProperty(unitName);
        this.chartType = new SimpleObjectProperty<>(chartType);
        this.series = new ObservableSetWrapper<>(bindings);
        this.timeZone = new SimpleObjectProperty<>(timezone);
        this.fromDateTime = new SimpleObjectProperty<>(fromDateTime);
        this.toDateTime = new SimpleObjectProperty<>(toDateTime);
        this.unitPrefixes = new SimpleObjectProperty<>(base);
        this.dirty = new SimpleBooleanProperty(false);

        ChangeListener setDirty = (observable, oldValue, newValue) -> dirty.setValue(true);
        this.nameProperty().addListener(setDirty);
        this.unitProperty().addListener(setDirty);
        this.chartTypeProperty().addListener(setDirty);
        this.timeZoneProperty().addListener(setDirty);
        this.fromDateTimeProperty().addListener(setDirty);
        this.toDateTimeProperty().addListener(setDirty);
        this.unitPrefixesProperty().addListener(setDirty);
      //  this.series.addListener((InvalidationListener) observable -> dirty = true);
    }

    public String getName() {
        return name.getValue();
    }

    public Property<String> nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.setValue(name);
    }

    @XmlTransient
    public ObservableSetWrapper<TimeSeriesBinding<Number>> getSeries() {
        return series;
    }

    public void setSeries(ObservableSetWrapper<TimeSeriesBinding<Number>> series) {
        this.series = series;
    }

    public ZoneId getTimeZone() {
        return timeZone.getValue();
    }

    public Property<ZoneId> timeZoneProperty() {
        return timeZone;
    }

    public void setTimeZone(ZoneId timeZone) {
        this.timeZone.setValue(timeZone);
    }

    public ChartType getChartType() {
        return chartType.getValue();
    }

    public Property<ChartType> chartTypeProperty() {
        return chartType;
    }

    public void setChartType(ChartType chartType) {
        this.chartType.setValue(chartType);
    }

    public ZonedDateTime getFromDateTime() {
        return fromDateTime.getValue();
    }

    public Property<ZonedDateTime> fromDateTimeProperty() {
        return fromDateTime;
    }

    public void setFromDateTime(ZonedDateTime fromDateTime) {
        this.fromDateTime.setValue(fromDateTime);
    }

    public ZonedDateTime getToDateTime() {
        return toDateTime.getValue();
    }

    public Property<ZonedDateTime> toDateTimeProperty() {
        return toDateTime;
    }

    public void setToDateTime(ZonedDateTime toDateTime) {
        this.toDateTime.setValue(toDateTime);
    }

    public String getUnit() {
        return unit.getValue();
    }

    public Property<String> unitProperty() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit.setValue(unit);
    }

    public UnitPrefixes getUnitPrefixes() {
        return unitPrefixes.getValue();
    }

    public Property<UnitPrefixes> unitPrefixesProperty() {
        return unitPrefixes;
    }

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
    public boolean isDirty(){
        return dirty.getValue();
    }

    public Property<Boolean> dirtyProperty(){
        return dirty;
    }
}

