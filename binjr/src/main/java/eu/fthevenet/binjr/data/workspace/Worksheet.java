package eu.fthevenet.binjr.data.workspace;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSetWrapper;
import eu.fthevenet.binjr.charts.XYChartSelection;
import eu.fthevenet.binjr.controllers.TimeSeriesController;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.TimeSeries;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Represents a single worksheet
 *
 * @author Frederic Thevenet
 */
public class Worksheet implements Serializable {
    private static final Logger logger = LogManager.getLogger(Worksheet.class);
    private static final AtomicInteger globalCounter = new AtomicInteger(0);
    private ObservableSetWrapper<TimeSeriesBinding<Number>> series;
    private Property<String> name;
    private Property<ZoneId> timeZone;
    private Property<ChartType> chartType;
    private Property<ZonedDateTime> fromDateTime;
    private Property<ZonedDateTime> toDateTime;

    public Worksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() +")",
                ChartType.STACKED,
                ZoneId.systemDefault(),
                new HashSet<TimeSeriesBinding<Number>>(),
               ZonedDateTime.now().minus(24, ChronoUnit.HOURS), ZonedDateTime.now());
    }

    public Worksheet(String name, ChartType chartType, ZoneId timezone){
        this(name, chartType, timezone,
                new HashSet<TimeSeriesBinding<Number>>(),
               ZonedDateTime.now().minus(24, ChronoUnit.HOURS), ZonedDateTime.now());
    }

    public Worksheet(String name, ChartType chartType, ZoneId timezone, Set<TimeSeriesBinding<Number>> bindings, ZonedDateTime fromDateTime, ZonedDateTime toDateTime) {
        this.name = new SimpleStringProperty(name);
        this.chartType = new SimpleObjectProperty<>(chartType);
        this.series = new ObservableSetWrapper<TimeSeriesBinding<Number>>(bindings);
        this.timeZone = new SimpleObjectProperty<>(timezone);
        this.fromDateTime = new SimpleObjectProperty<>(fromDateTime);
        this.toDateTime = new SimpleObjectProperty<>(toDateTime);
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

    @Override
    public String toString() {
        return String.format("Name: %s Type: %s Timezone: %s From: %s To: %s",
                getName(),
                getChartType().toString(),
                getTimeZone().toString(),
                getFromDateTime().toLocalDateTime(),
                getToDateTime().toLocalDateTime()
                );
    }
}

