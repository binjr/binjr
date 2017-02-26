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
public class Worksheet<T extends Number> implements Serializable {
    private static final Logger logger = LogManager.getLogger(Worksheet.class);
    private static final AtomicInteger globalCounter = new AtomicInteger(0);
    private ObservableSetWrapper<TimeSeriesBinding<T>> series;
    private Property<String> name;
    private Property<ZoneId> timeZone;
    private ChartType chartType;
    private Property<XYChartSelection<ZonedDateTime, T>> selection;

    public Worksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() +")",
                ChartType.AREA,
                ZoneId.systemDefault(),
                new HashSet<TimeSeriesBinding<T>>(),
                new XYChartSelection<ZonedDateTime, T>(ZonedDateTime.now().minus(24, ChronoUnit.HOURS), ZonedDateTime.now(), (T)(Number)0,  (T)(Number)100));
    }

    public Worksheet(String name, ChartType chartType, ZoneId timezone){
        this(name, chartType, timezone,
                new HashSet<TimeSeriesBinding<T>>(),
                new XYChartSelection<ZonedDateTime, T>(ZonedDateTime.now().minus(24, ChronoUnit.HOURS), ZonedDateTime.now(), (T)(Number)0,  (T)(Number)100));
    }

    public Worksheet(String name, ChartType chartType, ZoneId timezone,Set<TimeSeriesBinding<T>> bindings,XYChartSelection<ZonedDateTime, T> selection ) {
        this.name = new SimpleStringProperty(name);
        this.chartType = chartType;
        this.series = new ObservableSetWrapper<TimeSeriesBinding<T>>(bindings);
        this.timeZone = new SimpleObjectProperty<>(timezone);
        this.selection = new SimpleObjectProperty<>(selection);
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

    public ObservableSetWrapper<TimeSeriesBinding<T>> getSeries() {
        return series;
    }

    public void setSeries(ObservableSetWrapper<TimeSeriesBinding<T>> series) {
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
        return chartType;
    }

    public void setChartType(ChartType chartType) {
        this.chartType = chartType;
    }

    public XYChartSelection<ZonedDateTime, T> getSelection() {
        return selection.getValue();
    }

    public Property<XYChartSelection<ZonedDateTime, T>> selectionProperty() {
        return selection;
    }

    public void setSelection(XYChartSelection<ZonedDateTime,T> selection) {
        this.selection.setValue(selection);
    }

}

