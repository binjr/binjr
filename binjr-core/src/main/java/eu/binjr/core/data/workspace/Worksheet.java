/*
 *    Copyright 2017-2019 Frederic Thevenet
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
import eu.binjr.core.controllers.WorksheetNavigationHistory;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.Dirtyable;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * A class that represents and holds the current state of a single worksheet
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "Worksheet")
public class Worksheet implements Dirtyable {
    private static final Logger logger = LogManager.getLogger(Worksheet.class);
    private static final AtomicInteger globalCounter = new AtomicInteger(0);
    @IsDirtyable
    private ObservableList<Chart> charts;
    @IsDirtyable
    private Property<String> name;
    @IsDirtyable
    private Property<ZoneId> timeZone;
    @IsDirtyable
    private Property<ZonedDateTime> fromDateTime;
    @IsDirtyable
    private Property<ZonedDateTime> toDateTime;
    @IsDirtyable
    private Property<ChartLayout> chartLayout;
    @IsDirtyable
    private Property<Boolean> timeRangeLinked;
    @IsDirtyable
    private BooleanProperty chartLegendsVisible;

    private transient Map<Chart, XYChartSelection<ZonedDateTime, Double>> previousState;
    private transient final WorksheetNavigationHistory backwardHistory = new WorksheetNavigationHistory();
    private transient final WorksheetNavigationHistory forwardHistory = new WorksheetNavigationHistory();
    private transient Property<Integer> selectedChart;

    private transient final ChangeWatcher status;

    /**
     * Initializes a new instance of the {@link Worksheet} class
     */
    public Worksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() + ")",
                FXCollections.observableList(new LinkedList<>()),
                ZoneId.systemDefault(),
                ZonedDateTime.now().minus(24, ChronoUnit.HOURS),
                ZonedDateTime.now());
    }

    /**
     * Initializes a new instance of the {@link Worksheet} class with the provided name, chart type and zoneid
     *
     * @param name         the name for the new {@link Worksheet} instance
     * @param timezone     the {@link ZoneId} for the new {@link Worksheet} instance
     * @param fromDateTime the beginning of the time range represented on the worksheet.
     * @param toDateTime   the end of the time range represented on the worksheet.
     */
    public Worksheet(String name, ZonedDateTime fromDateTime, ZonedDateTime toDateTime, ZoneId timezone) {
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
     * Copy constructor to deep clone a {@link Worksheet} instance.
     *
     * @param initWorksheet the {@link Worksheet} instance to clone.
     */
    public Worksheet(Worksheet initWorksheet) {
        this(initWorksheet.getName(),
                initWorksheet.getCharts().stream()
                        .map(Chart::new)
                        .collect(Collectors.toCollection(() -> FXCollections.observableList(new LinkedList<>()))),
                initWorksheet.getTimeZone(),
                initWorksheet.getFromDateTime(),
                initWorksheet.getToDateTime(),
                initWorksheet.getChartLayout(),
                initWorksheet.isTimeRangeLinked(),
                initWorksheet.isChartLegendsVisible()
        );
    }

    /**
     * Initializes a new instance of the {@link Worksheet} class.
     *
     * @param name         the name for the new {@link Worksheet} instance
     * @param charts       the charts for the worksheet
     * @param timezone     the timezone for the worksheet
     * @param fromDateTime the start of the time range for the worksheet
     * @param toDateTime   the end of the time range for the worksheet
     */
    public Worksheet(String name,
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

    private Worksheet(String name,
                      List<Chart> charts,
                      ZoneId timezone,
                      ZonedDateTime fromDateTime,
                      ZonedDateTime toDateTime,
                      ChartLayout chartLayout,
                      boolean timeRangeLinked,
                      boolean chartLegendsVisible) {
        this.name = new SimpleStringProperty(name);
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
        this.chartLegendsVisible = new SimpleBooleanProperty(chartLegendsVisible);

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
     * The name of the {@link Worksheet}
     *
     * @return the name of the {@link Worksheet}
     */
    @XmlAttribute()
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
     * The {@link ZoneId} used by the {@link Worksheet} time series
     *
     * @return the {@link ZoneId} used by the {@link Worksheet} time series
     */
    @XmlAttribute
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
     * The lower bound of the time interval of the {@link Worksheet}'s times series
     *
     * @return the lower bound of the time interval of the {@link Worksheet}'s times series
     */
    @XmlAttribute
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
    @XmlAttribute
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
     * Returns the worksheet's currently selected chart.
     *
     * @return the worksheet's currently selected chart.
     */
    @XmlTransient
    public Integer getSelectedChart() {
        return selectedChart.getValue();
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
     * Sets  the worksheet's currently selected chart.
     *
     * @param selectedChart the worksheet's currently selected chart.
     */
    public void setSelectedChart(Integer selectedChart) {
        this.selectedChart.setValue(selectedChart);
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
     * Returns the previous state of the all the charts on the worksheet.
     *
     * @return the previous state of the all the charts on the worksheet.
     */
    @XmlTransient
    public Map<Chart, XYChartSelection<ZonedDateTime, Double>> getPreviousState() {
        return previousState;
    }

    /**
     * Sets the previous state of the all the charts on the worksheet.
     *
     * @param previousState the previous state of the all the charts on the worksheet.
     */
    public void setPreviousState(Map<Chart, XYChartSelection<ZonedDateTime, Double>> previousState) {
        this.previousState = previousState;
    }

    /**
     * Returns the backward history for the worksheet.
     *
     * @return the backward history for the worksheet.
     */
    @XmlTransient
    public WorksheetNavigationHistory getBackwardHistory() {
        return backwardHistory;
    }

    /**
     * Returns the forward history for the worksheet.
     *
     * @return the forward history for the worksheet.
     */
    @XmlTransient
    public WorksheetNavigationHistory getForwardHistory() {
        return forwardHistory;
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
     * The chartLayout property.
     *
     * @return the chartLayout property.
     */
    public Property<ChartLayout> chartLayoutProperty() {
        return chartLayout;
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
     * Returns true if the worksheet's timeline is linked to other worksheets, false otherwise.
     *
     * @return true if the worksheet's timeline is linked to other worksheets, false otherwise.
     */
    @XmlAttribute
    public Boolean isTimeRangeLinked() {
        return timeRangeLinked.getValue();
    }

    /**
     * The timeRangeLinked property.
     *
     * @return the timeRangeLinked property.
     */
    public Property<Boolean> timeRangeLinkedProperty() {
        return timeRangeLinked;
    }

    /**
     * Set to true if the worksheet's timeline is linked to other worksheets, false otherwise.
     *
     * @param timeRangeLinked true if the worksheet's timeline is linked to other worksheets, false otherwise.
     */
    public void setTimeRangeLinked(Boolean timeRangeLinked) {
        this.timeRangeLinked.setValue(timeRangeLinked);
    }

    public Boolean isChartLegendsVisible() {
        return chartLegendsVisible.getValue();
    }

    public BooleanProperty chartLegendsVisibleProperty() {
        return chartLegendsVisible;
    }

    public void setChartLegendsVisible(Boolean chartLegendsVisible) {
        this.chartLegendsVisible.setValue(chartLegendsVisible);
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
        IOUtils.closeCollectionElements(charts);
        this.status.close();
    }
    // endregion


}

