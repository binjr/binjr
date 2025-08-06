/*
 *    Copyright 2020-2025 Frederic Thevenet
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
import eu.binjr.common.logging.Logger;
import eu.binjr.common.navigation.NavigationHistory;
import eu.binjr.core.controllers.LogWorksheetController;
import eu.binjr.core.controllers.WorksheetController;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.dirtyable.ChangeWatcher;
import eu.binjr.core.data.dirtyable.IsDirtyable;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.adapters.ReloadStatus;
import eu.binjr.core.data.indexes.SearchHit;
import eu.binjr.core.preferences.UserPreferences;
import jakarta.xml.bind.annotation.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class LogWorksheet extends Worksheet<SearchHit> implements Syncable, Rangeable<SearchHit> {
    private static final Logger logger = Logger.create(LogWorksheet.class);
    private transient final NavigationHistory<LogQueryParameters> history = new NavigationHistory<>();
    private final transient ChangeWatcher status;
    private boolean syntaxHighlightEnabled = true;
    @IsDirtyable
    private final Property<Boolean> timeRangeLinked;
    @IsDirtyable
    private final Property<LogQueryParameters> queryParameters;
    @IsDirtyable
    private final ObservableList<LogFileSeriesInfo> seriesInfo;
    @IsDirtyable
    private final IntegerProperty textViewFontSize;
    @IsDirtyable
    private final DoubleProperty topDividerPosition;
    @IsDirtyable
    private final DoubleProperty bottomDividerPosition;
    @IsDirtyable
    private final BooleanProperty filterBarVisible;
    @IsDirtyable
    private final BooleanProperty findBarVisible;
    @IsDirtyable
    private final BooleanProperty heatmapVisible;


    private transient final DoubleProperty progress = new SimpleDoubleProperty(-1);

    private transient final  Property<ReloadStatus> indexingStatus = new SimpleObjectProperty<>(ReloadStatus.OK);

    public LogWorksheet() {
        this("New Worksheet (" + globalCounter.getAndIncrement() + ")",
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
        this.bottomDividerPosition = new SimpleDoubleProperty(0.85);
        this.topDividerPosition = new SimpleDoubleProperty(0);
        this.textViewFontSize = new SimpleIntegerProperty(UserPreferences.getInstance().defaultTextViewFontSize.get().intValue());
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
        seriesInfo.addAll(worksheet.getSeriesInfo().stream().map(LogFileSeriesInfo::new).toList());
    }

    @XmlAttribute
    public double getTopDividerPosition() {
        return topDividerPosition.getValue();
    }

    public void setTopDividerPosition(Double topDividerPosition) {
        this.topDividerPosition.setValue(topDividerPosition);
    }

    public Property<Number> topDividerPositionProperty() {
        return topDividerPosition;
    }

    @XmlAttribute
    public Double getBottomDividerPosition() {
        return bottomDividerPosition.getValue();
    }

    public void setBottomDividerPosition(Double bottomDividerPosition) {
        this.bottomDividerPosition.setValue(bottomDividerPosition);
    }

    public DoubleProperty bottomDividerPositionProperty() {
        return bottomDividerPosition;
    }

    @XmlElementWrapper(name = "Files")
    @XmlElements(@XmlElement(name = "Files"))
    public ObservableList<LogFileSeriesInfo> getSeriesInfo() {
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
    public void initWithBindings(String title, BindingsHierarchy... bindingsHierarchies) throws DataAdapterException {
        this.setName(title);
        for (var root : bindingsHierarchies) {
            // we're only interested in the leaves
            for (var b : root.getBindings()) {
                if (b instanceof LogFilesBinding logBinding) {
                    this.seriesInfo.add(LogFileSeriesInfo.fromBinding(logBinding));
                }
            }
        }
    }

    @Override
    protected List<? extends TimeSeriesInfo<SearchHit>> listAllSeriesInfo() {
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
    public List<LogFileSeriesInfo> getSeries() {
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

    @XmlTransient
    @Override
    public VisualizationType getVisualizationType() {
        return VisualizationType.EVENTS;
    }

    @Override
    public TimeRange getInitialTimeRange() {
        ZonedDateTime end = null;
        ZonedDateTime beginning = null;
        Map<DataAdapter<SearchHit>, List<TimeSeriesInfo<SearchHit>>> bindingsByAdapters =
                getSeries().stream().collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (var byAdapterEntry : bindingsByAdapters.entrySet()) {
            if (byAdapterEntry.getKey() instanceof Reloadable<SearchHit> adapter) {
                TimeRange timeRange = null;
                try {
                    timeRange = adapter.getInitialTimeRange("", byAdapterEntry.getValue());
                } catch (DataAdapterException e) {
                    logger.warn("An error occurred while attempting to retrieve initial time range for adapter " +
                            adapter.getId() + ": " + e.getMessage());
                    logger.debug(() -> "Stack trace", e);
                    timeRange = TimeRange.last24Hours();
                }
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

    public ReloadStatus getIndexingStatus() {
        return indexingStatus.getValue();
    }

    public Property<ReloadStatus> indexingStatusProperty() {
        return indexingStatus;
    }

}
