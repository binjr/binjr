/*
 *    Copyright 2017-2020 Frederic Thevenet
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
import eu.binjr.common.javafx.charts.XYChartSelection;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.navigation.NavigationHistory;
import eu.binjr.core.controllers.WorksheetController;
import eu.binjr.core.controllers.XYChartsWorksheetController;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import jakarta.xml.bind.annotation.*;
import java.beans.Transient;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


/**
 * A class that represents a worksheet that contains XY Charts
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class XYChartsWorksheet extends Worksheet<Double> implements Syncable {
    private static final Logger logger = Logger.create(XYChartsWorksheet.class);
    private transient final NavigationHistory<Map<Chart, XYChartSelection<ZonedDateTime, Double>>> history = new NavigationHistory<>();
    private transient final ChangeWatcher status;
    private final transient Property<Integer> selectedChart;
    @IsDirtyable
    private ObservableList<Chart> charts;
    @IsDirtyable
    private final Property<ZoneId> timeZone;
    @IsDirtyable
    private final Property<ZonedDateTime> fromDateTime;
    @IsDirtyable
    private final Property<ZonedDateTime> toDateTime;
    @IsDirtyable
    private final Property<ChartLayout> chartLayout;
    @IsDirtyable
    private final Property<Boolean> timeRangeLinked;
    @IsDirtyable
    private final DoubleProperty dividerPosition;

    //  private Class<? extends WorksheetController> controllerClass = XYChartsWorksheetController.class;

    /**
     * Initializes a new instance of the {@link XYChartsWorksheet} class
     */
    public XYChartsWorksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() + ")",
                FXCollections.observableList(new LinkedList<>()),
                ZoneId.systemDefault(),
                ZonedDateTime.now().minus(24, ChronoUnit.HOURS),
                ZonedDateTime.now());
    }

    /**
     * Initializes a new instance of the {@link XYChartsWorksheet} class with the provided name, chart type and zoneid
     *
     * @param name         the name for the new {@link XYChartsWorksheet} instance
     * @param timezone     the {@link ZoneId} for the new {@link XYChartsWorksheet} instance
     * @param fromDateTime the beginning of the time range represented on the worksheet.
     * @param toDateTime   the end of the time range represented on the worksheet.
     */
    public XYChartsWorksheet(String name, ZonedDateTime fromDateTime, ZonedDateTime toDateTime, ZoneId timezone) {
        this(name,
                FXCollections.observableList(new LinkedList<>()),
                timezone,
                fromDateTime,
                toDateTime,
                ChartLayout.STACKED,
                false,
                true);
    }

    /**
     * Copy constructor to deep clone a {@link XYChartsWorksheet} instance.
     *
     * @param initWorksheet the {@link XYChartsWorksheet} instance to clone.
     */
    public XYChartsWorksheet(XYChartsWorksheet initWorksheet) {
        this(initWorksheet.getName(),
                initWorksheet.getCharts().stream()
                        .map(Chart::new)
                        .collect(Collectors.toCollection(() -> FXCollections.observableList(new LinkedList<>()))),
                initWorksheet.getTimeZone(),
                initWorksheet.getFromDateTime(),
                initWorksheet.getToDateTime(),
                initWorksheet.getChartLayout(),
                initWorksheet.isTimeRangeLinked(),
                initWorksheet.isEditModeEnabled()
        );
    }

    /**
     * Initializes a new instance of the {@link XYChartsWorksheet} class.
     *
     * @param name         the name for the new {@link XYChartsWorksheet} instance
     * @param charts       the charts for the worksheet
     * @param timezone     the timezone for the worksheet
     * @param fromDateTime the start of the time range for the worksheet
     * @param toDateTime   the end of the time range for the worksheet
     */
    public XYChartsWorksheet(String name,
                             List<Chart> charts,
                             ZoneId timezone,
                             ZonedDateTime fromDateTime,
                             ZonedDateTime toDateTime) {
        this(name,
                charts,
                timezone,
                fromDateTime,
                toDateTime,
                ChartLayout.STACKED,
                false,
                true);
    }

    public XYChartsWorksheet(String name,
                             List<Chart> charts,
                             TimeRange range) {
        this(name,
                charts,
                range.getZoneId(),
                range.getBeginning(),
                range.getEnd(),
                ChartLayout.STACKED,
                false,
                true);
    }

    private XYChartsWorksheet(String name,
                              List<Chart> charts,
                              ZoneId timezone,
                              ZonedDateTime fromDateTime,
                              ZonedDateTime toDateTime,
                              ChartLayout chartLayout,
                              boolean timeRangeLinked,
                              boolean chartLegendsVisible) {
        super(name, chartLegendsVisible);
        this.charts = FXCollections.observableList(new LinkedList<>(charts));
        if (this.charts.isEmpty()) {
            this.charts.add(new Chart());
        }
        this.timeZone = new SimpleObjectProperty<>(timezone);
        this.fromDateTime = new SimpleObjectProperty<>(fromDateTime);
        this.toDateTime = new SimpleObjectProperty<>(toDateTime);
        this.chartLayout = new SimpleObjectProperty<>(chartLayout);
        this.timeRangeLinked = new SimpleBooleanProperty(timeRangeLinked);
        this.selectedChart = new SimpleObjectProperty<>(0);
        this.dividerPosition = new SimpleDoubleProperty(0.7);
        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
    }

    /**
     * Returns the default chart for the worksheet.
     *
     * @return the default chart for the worksheet.
     */
    public Chart getDefaultChart() {
        return charts.get(0);
    }

    /**
     * The {@link ZoneId} used by the {@link XYChartsWorksheet} time series
     *
     * @return the {@link ZoneId} used by the {@link XYChartsWorksheet} time series
     */
    @XmlAttribute
    public ZoneId getTimeZone() {
        return timeZone.getValue();
    }

    /**
     * The {@link ZoneId} used by the {@link XYChartsWorksheet} time series
     *
     * @param timeZone the {@link ZoneId} used by the {@link XYChartsWorksheet} time series
     */
    public void setTimeZone(ZoneId timeZone) {
        this.timeZone.setValue(timeZone);
    }

    /**
     * The {@link ZoneId} used by the {@link XYChartsWorksheet} time series
     *
     * @return An instance of {@link Property} for the {@link ZoneId} used by the {@link XYChartsWorksheet} time series
     */
    public Property<ZoneId> timeZoneProperty() {
        return timeZone;
    }

    /**
     * The lower bound of the time interval of the {@link XYChartsWorksheet}'s times series
     *
     * @return the lower bound of the time interval of the {@link XYChartsWorksheet}'s times series
     */
    @XmlAttribute
    public ZonedDateTime getFromDateTime() {
        return fromDateTime.getValue();
    }

    /**
     * The lower bound of the time interval of the {@link XYChartsWorksheet}'s times series
     *
     * @param fromDateTime the lower bound of the time interval of the {@link XYChartsWorksheet}'s times series
     */
    public void setFromDateTime(ZonedDateTime fromDateTime) {
        this.fromDateTime.setValue(fromDateTime);
    }

    /**
     * The lower bound of the time interval of the {@link XYChartsWorksheet}'s times series
     *
     * @return An instance of {@link Property} for the lower bound of the time interval of the {@link XYChartsWorksheet}'s times series
     */
    public Property<ZonedDateTime> fromDateTimeProperty() {
        return fromDateTime;
    }

    /**
     * The upper bound of the time interval of the {@link XYChartsWorksheet}'s times series
     *
     * @return the upper bound of the time interval of the {@link XYChartsWorksheet}'s times series
     */
    @XmlAttribute
    public ZonedDateTime getToDateTime() {
        return toDateTime.getValue();
    }

    /**
     * The upper bound of the time interval of the {@link XYChartsWorksheet}'s times series
     *
     * @param toDateTime the upper bound of the time interval of the {@link XYChartsWorksheet}'s times series
     */
    public void setToDateTime(ZonedDateTime toDateTime) {
        this.toDateTime.setValue(toDateTime);
    }

    /**
     * The upper bound of the time interval of the {@link XYChartsWorksheet}'s times series
     *
     * @return An instance of {@link Property} for the upper bound of the time interval of the {@link XYChartsWorksheet}'s times series
     */
    public Property<ZonedDateTime> toDateTimeProperty() {
        return toDateTime;
    }

    /**
     * Returns the worksheet's currently selected chart.
     *
     * @return the worksheet's currently selected chart.
     */
    @XmlTransient
    public Integer getSelectedChart() {
        return selectedChart.getValue();
    }

    /**
     * Sets  the worksheet's currently selected chart.
     *
     * @param selectedChart the worksheet's currently selected chart.
     */
    public void setSelectedChart(Integer selectedChart) {
        this.selectedChart.setValue(selectedChart);
    }

    /**
     * The selectedChart property.
     *
     * @return the selectedChart property.
     */
    public Property<Integer> selectedChartProperty() {
        return selectedChart;
    }

    /**
     * Returns all the {@link Chart} instance in the worksheet.
     *
     * @return all the {@link Chart} instance in the worksheet.
     */
    @XmlElementWrapper(name = "Charts")
    @XmlElements(@XmlElement(name = "Chart"))
    public ObservableList<Chart> getCharts() {
        return charts;
    }

    /**
     * Sets the {@link Chart} instance in the worksheet.
     *
     * @param charts the {@link Chart} instance in the worksheet.
     */
    public void setCharts(ObservableList<Chart> charts) {
        this.charts = charts;
    }

    @Override
    public String toString() {
        return String.format("%s - %s",
                getName(),
                getTimeZone().toString()
        );
    }

    /**
     * Returns the total number of series on the worksheet.
     *
     * @return the total number of series on the worksheet.
     */
    @XmlTransient
    public int getTotalNumberOfSeries() {
        return getCharts().stream().map(v -> v.getSeries().size()).reduce(0, Integer::sum);
    }

    /**
     * Returns the way charts are laid out on the worksheet.
     *
     * @return the way charts are laid out on the worksheet.
     */
    @XmlAttribute
    public ChartLayout getChartLayout() {
        return chartLayout.getValue();
    }

    /**
     * Specify the way charts are laid out on the worksheet.
     *
     * @param chartLayout the way charts are laid out on the worksheet.
     */
    public void setChartLayout(ChartLayout chartLayout) {
        this.chartLayout.setValue(chartLayout);
    }

    /**
     * The chartLayout property.
     *
     * @return the chartLayout property.
     */
    public Property<ChartLayout> chartLayoutProperty() {
        return chartLayout;
    }

    @Override
    @XmlAttribute
    public Boolean isTimeRangeLinked() {
        return timeRangeLinked.getValue();
    }

    @Override
    public Property<Boolean> timeRangeLinkedProperty() {
        return timeRangeLinked;
    }

    @Override
    public void setTimeRangeLinked(Boolean timeRangeLinked) {
        this.timeRangeLinked.setValue(timeRangeLinked);
    }

    @XmlAttribute
    public Double getDividerPosition() {
        return dividerPosition.getValue();
    }

    public void setDividerPosition(Double dividerPosition) {
        this.dividerPosition.setValue(dividerPosition);
    }

    public DoubleProperty dividerPositionProperty() {
        return dividerPosition;
    }

    @Transient
    public NavigationHistory<Map<Chart, XYChartSelection<ZonedDateTime, Double>>> getHistory() {
        return history;
    }


    @Override
    public Class<? extends WorksheetController> getControllerClass() {
        return XYChartsWorksheetController.class;
    }

    @Override
    public Worksheet<Double> duplicate() {
        return new XYChartsWorksheet(this);
    }

    // region Dirtyable
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
    // endregion

    // region Closeable
    @Override
    public void close() {
        logger.debug(() -> "Closing Worksheet " + this.toString());
        IOUtils.closeAll(charts);
        this.status.close();
    }

    @Override
    public void initWithBindings(String name, BindingsHierarchy... bindingsHierarchies) throws DataAdapterException {
        this.charts.clear();
        ZonedDateTime toDateTime = null;
        ZonedDateTime fromDateTime = null;
        Comparator<ZonedDateTime> comparator = Comparator.comparing(ZonedDateTime::toEpochSecond);
        for (var bindingsHierarchy : bindingsHierarchies) {
            if (bindingsHierarchy.getRoot() instanceof TimeSeriesBinding) {
                TimeSeriesBinding binding = (TimeSeriesBinding) bindingsHierarchy.getRoot();
                Chart chart = new Chart(
                        binding.getLegend(),
                        binding.getGraphType(),
                        binding.getUnitName(),
                        binding.getUnitPrefix());

                for (var b : bindingsHierarchy.getBindings()) {
                    if (b instanceof TimeSeriesBinding) {
                        TimeSeriesBinding tsb = (TimeSeriesBinding) b;
                        chart.addSeries(TimeSeriesInfo.fromBinding(tsb));
                    }
                }

                var range = chart.getInitialTimeRange();
                if (toDateTime == null || comparator.compare(range.getEnd(), toDateTime) > 0) {
                    toDateTime = range.getEnd();
                }
                if (fromDateTime == null || comparator.compare(range.getBeginning(), fromDateTime) < 0) {
                    fromDateTime = range.getBeginning();
                }

                toDateTime = toDateTime != null ? toDateTime : ZonedDateTime.now();
                fromDateTime = fromDateTime != null ? fromDateTime : toDateTime.minusHours(24);
                this.setName(name);
                this.charts.add(chart);
                this.setTimeZone(toDateTime.getZone());
                this.setFromDateTime(fromDateTime);
                this.setToDateTime(toDateTime);
            }
        }
    }

    @Override
    protected List<TimeSeriesInfo<Double>> listAllSeriesInfo() {
        List<TimeSeriesInfo<Double>> allInfo = new ArrayList<>();
        for (Chart chart : getCharts()) {
            allInfo.addAll( chart.getSeries());
        }
        return allInfo;
    }
}





