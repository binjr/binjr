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

import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.navigation.NavigationHistory;
import eu.binjr.core.controllers.LogWorksheetController;
import eu.binjr.core.controllers.WorksheetController;
import eu.binjr.core.data.adapters.LogFilesBinding;
import eu.binjr.core.data.adapters.LogQueryParameters;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.LogEvent;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.xml.bind.annotation.*;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.groupingBy;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class LogWorksheet extends Worksheet<LogEvent> implements Syncable, Rangeable<LogEvent> {

    private transient final NavigationHistory<LogQueryParameters> history = new NavigationHistory<>();
    private final transient ChangeWatcher status;
    private boolean syntaxHighlightEnabled = true;
    @IsDirtyable
    private final Property<Boolean> timeRangeLinked;
    @IsDirtyable
    private final Property<LogQueryParameters> queryParameters;
    @IsDirtyable
    private final ObservableList<TimeSeriesInfo<LogEvent>> seriesInfo = FXCollections.observableList(new LinkedList<>());
    @IsDirtyable
    private final IntegerProperty textViewFontSize = new SimpleIntegerProperty(10);
    @IsDirtyable
    private final DoubleProperty dividerPosition;

    public LogWorksheet() {
        this("New File (" + globalCounter.getAndIncrement() + ")",
                LogQueryParameters.empty(),
                true,
                false);
    }

    protected LogWorksheet(String name,
                           LogQueryParameters queryParameters,
                           boolean editModeEnabled,
                           boolean isLinked) {
        super(name, editModeEnabled);

        this.timeRangeLinked = new SimpleBooleanProperty(isLinked);
        this.queryParameters = new SimpleObjectProperty<>(queryParameters);
        this.dividerPosition = new SimpleDoubleProperty(0.7);
        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
    }

    private LogWorksheet(LogWorksheet worksheet) {
        this(worksheet.getName(),
                worksheet.getQueryParameters(),
                worksheet.isEditModeEnabled(),
                worksheet.isTimeRangeLinked());
        seriesInfo.addAll(worksheet.getSeriesInfo());
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


    @XmlElementWrapper(name = "Files")
    @XmlElements(@XmlElement(name = "Files"))
    public ObservableList<TimeSeriesInfo<LogEvent>> getSeriesInfo() {
        return seriesInfo;
    }

    @Override
    public Class<? extends WorksheetController> getControllerClass() {
        return LogWorksheetController.class;
    }

    @Override
    public Worksheet<LogEvent> duplicate() {
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
    protected List<TimeSeriesInfo<LogEvent>> listAllSeriesInfo() {
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


    @Override
    public List<TimeSeriesInfo<LogEvent>> getSeries() {
        return seriesInfo;
    }

    @XmlElement
    public LogQueryParameters getQueryParameters() {
        return queryParameters.getValue();
    }

    public Property<LogQueryParameters> queryParametersProperty() {
        return queryParameters;
    }

    public void setQueryParameters(LogQueryParameters queryParameters) {
        this.queryParameters.setValue(queryParameters);
    }

    @Override
    public TimeRange getInitialTimeRange() throws DataAdapterException {
        ZonedDateTime end = null;
        ZonedDateTime beginning = null;
        var bindingsByAdapters =
                getSeries().stream().collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (var byAdapterEntry : bindingsByAdapters.entrySet()) {
            var adapter = byAdapterEntry.getKey();
            // Group all queries with the same adapter
            var bindingsByPath =
                    byAdapterEntry.getValue().stream().collect(groupingBy(o -> o.getBinding().getPath()));
                var timeRange = adapter.getInitialTimeRange("", byAdapterEntry.getValue());
                if (end == null || timeRange.getEnd().isAfter(end)) {
                    end = timeRange.getEnd();
                }
                if (beginning == null || timeRange.getEnd().isBefore(beginning)) {
                    beginning = timeRange.getBeginning();
                }
        }
        return TimeRange.of(
                beginning == null ? ZonedDateTime.now().minusHours(24) : beginning,
                end == null ? ZonedDateTime.now() : end);
    }

    public NavigationHistory<LogQueryParameters> getHistory() {
        return history;
    }
}
