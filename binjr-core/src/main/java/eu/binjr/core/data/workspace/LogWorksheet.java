/*
 *    Copyright 2020-2021 Frederic Thevenet
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
import eu.binjr.core.data.adapters.ProgressAdapter;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.indexes.SearchHit;
import eu.binjr.core.preferences.UserPreferences;
import jakarta.xml.bind.annotation.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.groupingBy;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class LogWorksheet extends Worksheet<SearchHit> implements Syncable, Rangeable<SearchHit> {

    private transient final NavigationHistory<LogQueryParameters> history = new NavigationHistory<>();
    private final transient ChangeWatcher status;
    private boolean syntaxHighlightEnabled = true;
    @IsDirtyable
    private final Property<Boolean> timeRangeLinked;
    @IsDirtyable
    private final Property<LogQueryParameters> queryParameters;
    @IsDirtyable
    private final ObservableList<TimeSeriesInfo<SearchHit>> seriesInfo;
    @IsDirtyable
    private final IntegerProperty textViewFontSize;
    @IsDirtyable
    private final DoubleProperty dividerPosition;
    @IsDirtyable
    private final BooleanProperty filterBarVisible;
    @IsDirtyable
    private final BooleanProperty findBarVisible;
    @IsDirtyable
    private final BooleanProperty heatmapVisible;


    private transient final DoubleProperty progress = new SimpleDoubleProperty(-1);

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
        this.dividerPosition = new SimpleDoubleProperty(0.85);
        this.textViewFontSize = new SimpleIntegerProperty(10);
        this.seriesInfo = FXCollections.observableList(new LinkedList<>());
        this.filterBarVisible = new SimpleBooleanProperty(UserPreferences.getInstance().logFilterBarVisible.get());
        this.findBarVisible = new SimpleBooleanProperty(UserPreferences.getInstance().logFindBarVisible.get());
        this.heatmapVisible = new SimpleBooleanProperty(UserPreferences.getInstance().logHeatmapVisible.get());

        // Change watcher must be initialized after dirtyable properties or they will not be tracked.
        this.status = new ChangeWatcher(this);
    }

    private LogWorksheet(LogWorksheet worksheet) {
        this(worksheet.getName(),
                worksheet.getQueryParameters(),
                worksheet.isEditModeEnabled(),
                worksheet.isTimeRangeLinked());
        seriesInfo.addAll(worksheet.getSeriesInfo().stream().map(TimeSeriesInfo::new).toList());
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
    public ObservableList<TimeSeriesInfo<SearchHit>> getSeriesInfo() {
        return seriesInfo;
    }

    @Override
    public Class<? extends WorksheetController> getControllerClass() {
        return LogWorksheetController.class;
    }

    @Override
    public LogWorksheet duplicate() {
        return new LogWorksheet(this);
    }

    @Override
    public void close() {

    }

    @Override
    @Deprecated
    public LogWorksheet clone(){
        return duplicate();
    }

    @Override
    public void initWithBindings(String title, BindingsHierarchy... bindingsHierarchies) throws DataAdapterException {
        this.setName(title);
        for (var root : bindingsHierarchies) {
            // we're only interested in the leaves
            for (var b : root.getBindings()) {
                if (b instanceof LogFilesBinding logBinding) {
                    this.seriesInfo.add(TimeSeriesInfo.fromBinding(logBinding));
                }
            }
        }
    }

    @Override
    protected List<TimeSeriesInfo<SearchHit>> listAllSeriesInfo() {
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
    public boolean isFilterBarVisible() {
        return filterBarVisible.get();
    }

    public BooleanProperty filterBarVisibleProperty() {
        return filterBarVisible;
    }

    public void setFilterBarVisible(boolean filterBarVisible) {
        this.filterBarVisible.set(filterBarVisible);
    }

    @XmlAttribute
    public boolean isFindBarVisible() {
        return findBarVisible.get();
    }

    public BooleanProperty findBarVisibleProperty() {
        return findBarVisible;
    }

    public void setFindBarVisible(boolean findBarVisible) {
        this.findBarVisible.set(findBarVisible);
    }

    @XmlAttribute
    public boolean isHeatmapVisible() {
        return heatmapVisible.get();
    }

    public BooleanProperty heatmapVisibleProperty() {
        return heatmapVisible;
    }

    public void setHeatmapVisible(boolean heatmapVisible) {
        this.heatmapVisible.set(heatmapVisible);
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
    public List<TimeSeriesInfo<SearchHit>> getSeries() {
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
            if (byAdapterEntry.getKey() instanceof ProgressAdapter<SearchHit> adapter) {
                var timeRange = adapter.getInitialTimeRange("", byAdapterEntry.getValue(), progressProperty());
                if (end == null || timeRange.getEnd().isAfter(end)) {
                    end = timeRange.getEnd();
                }
                if (beginning == null || timeRange.getEnd().isBefore(beginning)) {
                    beginning = timeRange.getBeginning();
                }
            }
        }
        return TimeRange.of(
                beginning == null ? ZonedDateTime.now().minusHours(24) : beginning,
                end == null ? ZonedDateTime.now() : end);
    }

    public NavigationHistory<LogQueryParameters> getHistory() {
        return history;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }
}
