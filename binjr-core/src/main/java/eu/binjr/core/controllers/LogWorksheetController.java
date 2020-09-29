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

package eu.binjr.core.controllers;


import com.google.gson.Gson;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.TimeRangePicker;
import eu.binjr.common.javafx.richtext.CodeAreaHighlighter;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.navigation.RingIterator;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.LogFilter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.data.timeseries.LogEventsProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.LogWorksheet;
import eu.binjr.core.data.workspace.Syncable;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserPreferences;
import javafx.animation.PauseTransition;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.MaskerPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;

import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class LogWorksheetController extends WorksheetController implements Syncable {
    private static final Logger logger = Logger.create(LogWorksheetController.class);
    public static final String WORKSHEET_VIEW_FXML = "/eu/binjr/views/LogWorksheetView.fxml";
    private static final Gson gson = new Gson();
    private final LogWorksheet worksheet;
    //  private final Property<TimeRange> timeRangeProperty = new SimpleObjectProperty<>(TimeRange.of(ZonedDateTime.now().minusHours(1), ZonedDateTime.now()));
    private StyleSpans<Collection<String>> syntaxHilightStyleSpans;
    private RingIterator<CodeAreaHighlighter.SearchHitRange> searchHitIterator = RingIterator.of(Collections.emptyList());
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public LogWorksheetController(MainViewController parent, LogWorksheet worksheet, Collection<DataAdapter<String>> adapters)
            throws NoAdapterFoundException {
        super(parent);
        this.worksheet = worksheet;
        for (var d : worksheet.getSeriesInfo()) {
            UUID id = d.getBinding().getAdapterId();
            DataAdapter<String> da = adapters
                    .stream()
                    .filter(a -> (id != null && a != null && a.getId() != null) && id.equals(a.getId()))
                    .findAny()
                    .orElseThrow(() -> new NoAdapterFoundException("Failed to find a valid adapter with id " +
                            (id != null ? id.toString() : "null")));
            d.getBinding().setAdapter(da);
        }
    }

    @FXML
    private AnchorPane root;

    @FXML
    private CodeArea textOutput;

    @FXML
    private ToggleButton wordWrapButton;

    @FXML
    private Button refreshButton;
    @FXML
    private Button backButton;
    @FXML
    private Button forwardButton;

    @FXML
    private TimeRangePicker timeRangePicker;

    @FXML
    private Button searchHistoryButton;

    @FXML
    private TextField searchTextField;

    @FXML
    private Button clearSearchButton;

    @FXML
    private ToggleButton searchMatchCaseToggle;

    @FXML
    private ToggleButton searchRegExToggle;

    @FXML
    private Label searchResultsLabel;

    @FXML
    private Button prevOccurrenceButton;

    @FXML
    private Button nextOccurrenceButton;

    @FXML
    private ToggleButton filterToggleButton;

    @FXML
    private Pagination pager;

    @Override
    public Worksheet getWorksheet() {
        return worksheet;
    }

    @FXML
    private MaskerPane busyIndicator;

    @FXML
    private CheckListView<FacetEntry> severityListView;

    @FXML
    private TextField filterTextField;

    @FXML
    private Button clearFilterButton;

    @FXML
    private Button applyFilterButton;

    @FXML
    private VBox filteringBar;


    @Override
    public Property<TimeRange> selectedRangeProperty() {
        return this.timeRangePicker.selectedRangeProperty();
    }

    @Override
    public Optional<ChartViewPort> getAttachedViewport(TitledPane pane) {
        return Optional.empty();
    }

    @Override
    public ContextMenu getChartListContextMenu(Collection<TreeItem<SourceBinding>> treeView) {
        return null;
    }

    @Override
    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
    }

    @Override
    public void refresh() {
        invalidate(true, true);
    }


    public void invalidate(boolean saveToHistory, boolean retrieveFacets) {
        //TODO handle history
        queryLogIndex(worksheet.getFilter(), retrieveFacets);
    }

    public void queryLogIndex(LogFilter filter, boolean retrieveFacets) {
        try {
            AsyncTaskManager.getInstance().submit(() -> {
                        busyIndicator.setVisible(true);
                        return (LogEventsProcessor) fetchDataFromSources(filter);
                    },
                    event -> {
                        bindingManager.suspend();
                        try {
                            var res = (LogEventsProcessor) event.getSource().getValue();
                            if (retrieveFacets) {

                                var checkedFacetLabels = severityListView.getCheckModel()
                                        .getCheckedItems().stream()
                                        .filter(Objects::nonNull)
                                        .map(FacetEntry::getLabel)
                                        .collect(toList());
                                severityListView.getCheckModel().clearChecks();
                                severityListView.getItems().setAll(res.getFacetResults().get("severity"));
                                severityListView.getItems()
                                        .stream()
                                        .filter(f -> checkedFacetLabels.contains(f.getLabel()))
                                        .forEach(f -> severityListView.getCheckModel().check(f));

                            }
                            String data = res.getData().stream()
                                    .map(XYChart.Data::getYValue)
                                    .collect(Collectors.joining());
                            textOutput.clear();
                            textOutput.replaceText(0, 0, data);
                            if (worksheet.isSyntaxHighlightEnabled()) {
                                this.syntaxHilightStyleSpans = CodeAreaHighlighter.computeSyntaxHighlighting(textOutput.getText());
                                textOutput.setStyleSpans(0, syntaxHilightStyleSpans);
                            }
                        } finally {
                            bindingManager.resume();
                            busyIndicator.setVisible(false);
                        }
                    }, event -> {
                        busyIndicator.setVisible(false);
                        Dialogs.notifyException("An error occurred while loading text file: " +
                                        event.getSource().getException().getMessage(),
                                event.getSource().getException(),
                                root);
                    });
        } catch (Exception e) {
            Dialogs.notifyException(e);
        }

    }

    @Override
    public void saveSnapshot() {

    }

    @Override
    public void toggleShowPropertiesPane() {

    }

    @Override
    public void setShowPropertiesPane(boolean value) {

    }

    @Override
    public List<ChartViewPort> getViewPorts() {
        return new ArrayList<>();
    }


    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            timeRangePicker.dispose();

            bindingManager.close();
        }
    }

    @Override
    public String getView() {
        return WORKSHEET_VIEW_FXML;
    }

    private final ReadOnlyObjectWrapper<TimeRange> timeRange = new ReadOnlyObjectWrapper<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getBindingManager().attachListener(worksheet.textViewFontSizeProperty(),
                (ChangeListener<Integer>) (obs, oldVal, newVal) -> textOutput.setStyle("-fx-font-size: " + newVal + "pt;"));
        //textOutput.setParagraphGraphicFactory(LineNumberFactory.get(textOutput));
        textOutput.setEditable(false);
        getBindingManager().bind(textOutput.wrapTextProperty(), wordWrapButton.selectedProperty());
        refreshButton.setOnAction(getBindingManager().registerHandler(event -> refresh()));
        // TimeRange Picker initialization
        timeRange.set(TimeRange.of(worksheet.getFromDateTime(), worksheet.getToDateTime()));
        timeRangePicker.timeRangeLinkedProperty().bindBidirectional(worksheet.timeRangeLinkedProperty());
        timeRangePicker.zoneIdProperty().bindBidirectional(worksheet.timeZoneProperty());
        timeRangePicker.initSelectedRange(timeRange.get());
        timeRangePicker.setOnSelectedRangeChanged((observable, oldValue, newValue) -> {
            timeRange.set(TimeRange.of(newValue.getBeginning(), newValue.getEnd()));
            refresh();
        });

        timeRange.getReadOnlyProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                worksheet.setFromDateTime(newValue.getBeginning());
                worksheet.setToDateTime(newValue.getEnd());
                timeRangePicker.updateSelectedRange(newValue);
            }
        });



        // init filter controls
        pager.setCurrentPageIndex(worksheet.getFilter().getPage());
        filterTextField.setText(worksheet.getFilter().getFilterQuery());
        //    severityListView.getItems().addAll();

        bindingManager.attachListener(worksheet.filterProperty(), (o, oldVal, newVal) -> {
            if (!oldVal.equals(newVal)) {
                refresh();
            }
        });

        // Filter selection
        bindingManager.bind(filteringBar.managedProperty(), filteringBar.visibleProperty());
        bindingManager.bind(filteringBar.visibleProperty(), filterToggleButton.selectedProperty());
        bindingManager.attachListener(severityListView.getCheckModel().getCheckedItems(),
                (ListChangeListener<FacetEntry>) l -> invalidateFilter());
        bindingManager.attachListener(pager.currentPageIndexProperty(), (o, oldVal, newVal) -> invalidateFilter());
        filterTextField.setOnAction(bindingManager.registerHandler(event -> invalidateFilter()));
        clearFilterButton.setOnAction(bindingManager.registerHandler(actionEvent -> {
            filterTextField.clear();
            invalidateFilter();
        }));
        applyFilterButton.setOnAction(bindingManager.registerHandler(event -> invalidateFilter()));

        // Pagination setup
        // pager.setPageFactory();

        //Search bar initialization
        prevOccurrenceButton.setOnAction(getBindingManager().registerHandler(event -> {
            if (searchHitIterator.hasPrevious()) {
                focusOnSearchHit(searchHitIterator.previous());
            }
        }));
        nextOccurrenceButton.setOnAction(getBindingManager().registerHandler(event -> {
            if (searchHitIterator.hasNext()) {
                focusOnSearchHit(searchHitIterator.next());
            }
        }));
        clearSearchButton.setOnAction(getBindingManager().registerHandler(event -> searchTextField.clear()));
        // Delay the search until at least the following amount of time elapsed since the last character was entered
        var delay = new PauseTransition(Duration.millis(UserPreferences.getInstance().searchFieldInputDelayMs.get().intValue()));
        getBindingManager().attachListener(searchTextField.textProperty(),
                (ChangeListener<String>) (obs, oldText, newText) -> {
                    delay.setOnFinished(event -> doSearchHighlight(newText,
                            searchMatchCaseToggle.isSelected(),
                            searchRegExToggle.isSelected()));
                    delay.playFromStart();
                });

        getBindingManager().attachListener(searchMatchCaseToggle.selectedProperty(),
                (ChangeListener<Boolean>) (obs, oldVal, newVal) ->
                        doSearchHighlight(searchTextField.getText(), newVal, searchRegExToggle.isSelected()));
        getBindingManager().attachListener(searchRegExToggle.selectedProperty(),
                (ChangeListener<Boolean>) (obs, oldVal, newVal) ->
                        doSearchHighlight(searchTextField.getText(), searchMatchCaseToggle.isSelected(), newVal));
        refresh();
        super.initialize(location, resources);
    }

    private void invalidateFilter() {
        worksheet.setFilter(
                new LogFilter(filterTextField.getText(),
                        severityListView.getCheckModel().getCheckedItems()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(FacetEntry::getLabel)
                                .collect(Collectors.toSet()),
                        pager.getCurrentPageIndex()));
    }

    private void focusOnSearchHit(CodeAreaHighlighter.SearchHitRange hit) {
        if (hit == null) {
            textOutput.selectRange(0, 0);
            searchResultsLabel.setText("No results");
        } else {
            textOutput.selectRange(hit.getStart(), hit.getEnd());
            textOutput.requestFollowCaret();
            searchResultsLabel.setText(String.format("%d/%d",
                    searchHitIterator.peekCurrentIndex() + 1,
                    searchHitIterator.peekLastIndex() + 1));
        }
    }

    private void doSearchHighlight(String searchText, boolean matchCase, boolean regEx) {
        var searchResults = CodeAreaHighlighter.computeSearchHitsHighlighting(textOutput.getText(), searchText, matchCase, regEx);
        prevOccurrenceButton.setDisable(searchResults.getSearchHitRanges().isEmpty());
        nextOccurrenceButton.setDisable(searchResults.getSearchHitRanges().isEmpty());
        searchHitIterator = RingIterator.of(searchResults.getSearchHitRanges());
        searchResultsLabel.setText(searchResults.getSearchHitRanges().size() + " results");
        if (syntaxHilightStyleSpans != null) {
            textOutput.setStyleSpans(0, syntaxHilightStyleSpans.overlay(searchResults.getStyleSpans(),
                    (strings, strings2) -> Stream.concat(strings.stream(),
                            strings2.stream()).collect(Collectors.toCollection(ArrayList<String>::new))));
        } else {
            textOutput.setStyleSpans(0, searchResults.getStyleSpans());
        }
        if (searchHitIterator.hasNext()) {
            focusOnSearchHit(searchHitIterator.next());
        } else {
            focusOnSearchHit(null);
        }
    }

    private TimeSeriesProcessor<String> fetchDataFromSources(LogFilter filter) throws DataAdapterException {
        // prune series from closed adapters
        worksheet.getSeriesInfo().removeIf(seriesInfo -> {
            if (seriesInfo.getBinding().getAdapter().isClosed()) {
                logger.debug(() -> seriesInfo.getDisplayName() + " will be pruned because attached adapter " +
                        seriesInfo.getBinding().getAdapter().getId() + " is closed.");
                return true;
            }
            return false;
        });

        if (worksheet.getFromDateTime().toInstant().equals(Instant.EPOCH) &&
                worksheet.getFromDateTime().equals(worksheet.getToDateTime())) {
            timeRange.set(worksheet.getInitialTimeRange());
        }
        var queryString = gson.toJson(filter);
        var bindingsByAdapters =
                worksheet.getSeriesInfo().stream().collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (var byAdapterEntry : bindingsByAdapters.entrySet()) {
            // Define the transforms to apply
            var adapter = (DataAdapter<String>) byAdapterEntry.getKey();
            // Group all queries with the same adapter and path
            var bindingsByPath =
                    byAdapterEntry.getValue().stream().collect(groupingBy(o -> o.getBinding().getPath()));
            var data = adapter.fetchData(
                    queryString,
                    worksheet.getFromDateTime().toInstant(),
                    worksheet.getToDateTime().toInstant(),
                    bindingsByPath.values().stream().flatMap(Collection::stream).collect(Collectors.toList()),
                    true);
            return data.values().stream().findFirst().orElse(new LogEventsProcessor());
        }
        return new LogEventsProcessor();
    }

    @Override
    public Boolean isTimeRangeLinked() {
        return null;
    }

    @Override
    public Property<Boolean> timeRangeLinkedProperty() {
        return null;
    }

    @Override
    public void setTimeRangeLinked(Boolean timeRangeLinked) {

    }

    public static class PaginationStatus {
        private int nbPages;
        private int currentPage;
        private int maxHitsPerPage;
        private long nbHits;

        public int getNbPages() {
            return nbPages;
        }

        public void setNbPages(int nbPages) {
            this.nbPages = nbPages;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getMaxHitsPerPage() {
            return maxHitsPerPage;
        }

        public void setMaxHitsPerPage(int maxHitsPerPage) {
            this.maxHitsPerPage = maxHitsPerPage;
        }

        public long getNbHits() {
            return nbHits;
        }

        public void setNbHits(long nbHits) {
            this.nbHits = nbHits;
        }
    }
}
