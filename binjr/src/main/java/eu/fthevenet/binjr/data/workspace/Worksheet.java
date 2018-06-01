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

import eu.fthevenet.binjr.data.dirtyable.ChangeWatcher;
import eu.fthevenet.binjr.data.dirtyable.Dirtyable;
import eu.fthevenet.binjr.data.dirtyable.IsDirtyable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * A class that represents and holds the current state of a single worksheet
 *
 * @author Frederic Thevenet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "Worksheet")
public class Worksheet<T> implements Dirtyable, AutoCloseable {
    private static final Logger logger = LogManager.getLogger(Worksheet.class);
    private static final AtomicInteger globalCounter = new AtomicInteger(0);
    @IsDirtyable
    private ObservableList<Chart<T>> charts;
    @IsDirtyable
    private Property<String> name;
    @IsDirtyable
    private Property<ZoneId> timeZone;
    @IsDirtyable
    private Property<ZonedDateTime> fromDateTime;
    @IsDirtyable
    private Property<ZonedDateTime> toDateTime;

    private final ChangeWatcher status;

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
     * @param name      the name for the new {@link Worksheet} instance
     * @param timezone  the {@link ZoneId} for the new {@link Worksheet} instance
     */
    public Worksheet(String name, ZonedDateTime fromDateTime, ZonedDateTime toDateTime, ZoneId timezone) {
        this(name,
                FXCollections.observableList(new LinkedList<>()),
                timezone,
                fromDateTime,
                toDateTime
        );
    }

    /**
     * Copy constructor to deep clone a {@link Worksheet} instance.
     *
     * @param initWorksheet the {@link Worksheet} instance to clone.
     */
    public Worksheet(Worksheet<T> initWorksheet) {
        this(initWorksheet.getName(),
                initWorksheet.getCharts().stream()
                        .map(Chart::new)
                        .collect(Collectors.toCollection(() -> FXCollections.observableList(new LinkedList<>()))),
                initWorksheet.getTimeZone(),
                initWorksheet.getFromDateTime(),
                initWorksheet.getToDateTime()
        );
    }


    public Worksheet(String name,
                     List<Chart<T>> charts,
                      ZoneId timezone,
                      ZonedDateTime fromDateTime,
                      ZonedDateTime toDateTime) {
        this.name = new SimpleStringProperty(name);
        this.charts = FXCollections.observableList(new LinkedList<>(charts));
        if (this.charts.isEmpty()) {
            this.charts.add(new Chart<>());
        }
        this.timeZone = new SimpleObjectProperty<>(timezone);
        this.fromDateTime = new SimpleObjectProperty<>(fromDateTime);
        this.toDateTime = new SimpleObjectProperty<>(toDateTime);

        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
    }


    public Chart<T> getDefaultChart() {
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


    @XmlElementWrapper(name = "Charts")
    @XmlElements(@XmlElement(name = "Chart"))
    public ObservableList<Chart<T>> getCharts() {
        return charts;
    }

    public void setCharts(ObservableList<Chart<T>> charts) {
        this.charts = charts;
    }



    @Override
    public String toString() {
        return String.format("%s - %s",
                getName(),
                getTimeZone().toString()
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
        charts.forEach(Chart::close);
    }


}

