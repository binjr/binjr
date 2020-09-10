/*
 *    Copyright 2020 Frederic Thevenet
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

import eu.binjr.common.javafx.charts.XYChartSelection;
import eu.binjr.common.navigation.NavigationHistory;
import eu.binjr.core.controllers.LogWorksheetController;
import eu.binjr.core.controllers.WorksheetController;
import eu.binjr.core.data.adapters.LogFilesBinding;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.DataAdapterException;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.xml.bind.annotation.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class LogWorksheet extends Worksheet<String> implements Syncable, Rangeable<String> {

    private transient final NavigationHistory<Map<Chart, XYChartSelection<ZonedDateTime, Double>>> history = new NavigationHistory<>();

    @IsDirtyable
    private final Property<ZoneId> timeZone;
    @IsDirtyable
    private final Property<ZonedDateTime> fromDateTime;
    @IsDirtyable
    private final Property<ZonedDateTime> toDateTime;
    @IsDirtyable
    private final Property<Boolean> timeRangeLinked;

    private final transient ChangeWatcher status;
    @IsDirtyable
    private final ObservableList<TimeSeriesInfo<String>> seriesInfo = FXCollections.observableList(new LinkedList<>());

    @IsDirtyable
    private final IntegerProperty textViewFontSize = new SimpleIntegerProperty(10);
    private boolean syntaxHighlightEnabled = false;


    public LogWorksheet() {
        this("New File (" + globalCounter.getAndIncrement() + ")",
                true,
                ZoneId.systemDefault(),
                ZonedDateTime.ofInstant(Instant.EPOCH,  ZoneId.systemDefault()),
                ZonedDateTime.ofInstant(Instant.EPOCH,  ZoneId.systemDefault()),
                false);
    }

    protected LogWorksheet(String name, boolean editModeEnabled, ZoneId timezone, ZonedDateTime from, ZonedDateTime to, boolean isLinked) {
        super(name, editModeEnabled);
        this.timeZone = new SimpleObjectProperty<>(timezone);
        this.fromDateTime = new SimpleObjectProperty<>(from);
        this.toDateTime = new SimpleObjectProperty<>(to);
        this.timeRangeLinked = new SimpleBooleanProperty(isLinked);
        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);

    }

    private LogWorksheet(LogWorksheet worksheet) {
        this(worksheet.getName(),
                worksheet.isEditModeEnabled(),
                worksheet.getTimeZone(),
                worksheet.getFromDateTime(),
                worksheet.getToDateTime(),
                worksheet.isTimeRangeLinked());
        seriesInfo.addAll(worksheet.getSeriesInfo());
    }

    @XmlElementWrapper(name = "Files")
    @XmlElements(@XmlElement(name = "Files"))
    public ObservableList<TimeSeriesInfo<String>> getSeriesInfo() {
        return seriesInfo;
    }

    @Override
    public Class<? extends WorksheetController> getControllerClass() {
        return LogWorksheetController.class;
    }

    @Override
    public Worksheet<String> duplicate() {
        return new LogWorksheet(this);
    }

    @Override
    public void close() {

    }

    @Override
    public void initWithBindings(String title, BindingsHierarchy... bindingsHierarchies) throws DataAdapterException {
        this.setName(title);
        for (var root : bindingsHierarchies) {
            // we're only interested in the leaves
            for (var b : root.getBindings()) {
                if (b instanceof LogFilesBinding) {
                    this.seriesInfo.add(TimeSeriesInfo.fromBinding((LogFilesBinding) b));
                }
            }
        }
    }



    @Override
    protected List<TimeSeriesInfo<String>> listAllSeriesInfo() {
        return getSeriesInfo();
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


    @XmlAttribute
    public int getTextViewFontSize() {
        return textViewFontSize.get();
    }

    public IntegerProperty textViewFontSizeProperty() {
        return textViewFontSize;
    }

    public void setTextViewFontSize(int textViewFontSize) {
        this.textViewFontSize.set(textViewFontSize);
    }

    @XmlAttribute
    public boolean isSyntaxHighlightEnabled() {
        return syntaxHighlightEnabled;
    }

    public void setSyntaxHighlightEnabled(boolean syntaxHighlightEnabled) {
        this.syntaxHighlightEnabled = syntaxHighlightEnabled;
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
    public ZoneId getTimeZone() {
        return timeZone.getValue();
    }

    public Property<ZoneId> timeZoneProperty() {
        return timeZone;
    }

    public void setTimeZone(ZoneId timeZone) {
        this.timeZone.setValue(timeZone);
    }

    @XmlAttribute
    public ZonedDateTime getFromDateTime() {
        return fromDateTime.getValue();
    }

    public Property<ZonedDateTime> fromDateTimeProperty() {
        return fromDateTime;
    }

    public void setFromDateTime(ZonedDateTime fromDateTime) {
        this.fromDateTime.setValue(fromDateTime);
    }

    @XmlAttribute
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
    public List<TimeSeriesInfo<String>> getSeries() {
        return seriesInfo;
    }
}
