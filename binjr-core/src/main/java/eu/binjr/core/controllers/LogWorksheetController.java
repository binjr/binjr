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


import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.controls.TimeRangePicker;
import eu.binjr.common.javafx.richtext.CodeAreaHighlighter;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.navigation.RingIterator;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.timeseries.TextProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.timeseries.transform.SortTransform;
import eu.binjr.core.data.workspace.LogWorksheet;
import eu.binjr.core.data.workspace.Syncable;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserPreferences;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.controlsfx.control.MaskerPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public class LogWorksheetController extends WorksheetController implements Syncable {
    private static final Logger logger = Logger.create(LogWorksheetController.class);
    public static final String WORKSHEET_VIEW_FXML = "/eu/binjr/views/LogWorksheetView.fxml";
    private final LogWorksheet worksheet;
    //  private final Property<TimeRange> timeRangeProperty = new SimpleObjectProperty<>(TimeRange.of(ZonedDateTime.now().minusHours(1), ZonedDateTime.now()));
    private StyleSpans<Collection<String>> syntaxHilightStyleSpans;
    private RingIterator<CodeAreaHighlighter.SearchHitRange> searchHitIterator = RingIterator.of(Collections.emptyList());


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

    @Override
    public Worksheet getWorksheet() {
        return worksheet;
    }

    @FXML
    public MaskerPane busyIndicator;

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
        invalidate(null, false, true);
    }

    @Override
    public void invalidateAll(boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh) {
        invalidate(null, dontPlotChart, forceRefresh);
    }

    @Override
    public void invalidate(ChartViewPort viewPort, boolean dontPlot, boolean forceRefresh) {
        if (forceRefresh) {
            loadFile();
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
        textOutput.setParagraphGraphicFactory(LineNumberFactory.get(textOutput));
        textOutput.setEditable(false);
        getBindingManager().bind(textOutput.wrapTextProperty(), wordWrapButton.selectedProperty());
        refreshButton.setOnAction(getBindingManager().registerHandler(event -> refresh()));
        // TimeRange Picker initialization
        timeRangePicker.timeRangeLinkedProperty().bindBidirectional(worksheet.timeRangeLinkedProperty());
        timeRangePicker.zoneIdProperty().bindBidirectional(worksheet.timeZoneProperty());
        timeRangePicker.initSelectedRange(TimeRange.of(worksheet.getFromDateTime(), worksheet.getToDateTime()));
        timeRangePicker.setOnSelectedRangeChanged((observable, oldValue, newValue) -> {
            timeRange.set(TimeRange.of(newValue.getBeginning(), newValue.getEnd()));
            invalidateAll(true, false, true);
        });

        timeRange.getReadOnlyProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                worksheet.setFromDateTime(newValue.getBeginning());
                worksheet.setToDateTime(newValue.getEnd());
                timeRangePicker.updateSelectedRange(newValue);
            }
        });

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
        Platform.runLater(this::refresh);
        super.initialize(location, resources);
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

    public TimeSeriesProcessor<String> fetchDataFromSources() throws DataAdapterException {
        // prune series from closed adapters
        worksheet.getSeriesInfo().removeIf(seriesInfo -> {
            if (seriesInfo.getBinding().getAdapter().isClosed()) {
                logger.debug(() -> seriesInfo.getDisplayName() + " will be pruned because attached adapter " +
                        seriesInfo.getBinding().getAdapter().getId() + " is closed.");
                return true;
            }
            return false;
        });

        var bindingsByAdapters = worksheet.getSeriesInfo().stream().collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (var byAdapterEntry : bindingsByAdapters.entrySet()) {
            // Define the transforms to apply
            var adapter = (DataAdapter<String>) byAdapterEntry.getKey();
            var sort = new SortTransform();
            sort.setEnabled(adapter.isSortingRequired());
            // Group all queries with the same adapter and path
            var bindingsByPath = byAdapterEntry.getValue().stream().collect(groupingBy(o -> o.getBinding().getPath()));
//            for (var byPathEntry : bindingsByPath.entrySet()) {
//                String path = byPathEntry.getKey();
            // Get data from the adapter
            var data = adapter.fetchData(
                    String.join("|", bindingsByPath.keySet()),
                    worksheet.getFromDateTime().toInstant(),
                    worksheet.getToDateTime().toInstant(),
                    bindingsByPath.values().stream().flatMap(Collection::stream).collect(Collectors.toList()),
                    true);
            return data.values().stream().findFirst().orElse(new TextProcessor());
//                data.entrySet().parallelStream().forEach(entry -> {
//                    var info = entry.getKey();
//                    var proc = entry.getValue();
//                    //bind proc to timeSeries info
//                    info.setProcessor(proc);
//                });
            //   }


        }
        return null;
    }

    private void loadFile() {
        try {
            AsyncTaskManager.getInstance().submit(() -> {
                        busyIndicator.setVisible(true);
                        return fetchDataFromSources().getData().stream()
                                .map(XYChart.Data::getYValue)
                                .collect(Collectors.joining());
//                                        .collect(Collectors.joining())
//                        return worksheet.getSeriesInfo().stream()
//                                .map(info -> info.getProcessor()
//                                        .getData()
//                                        .stream()
//                                        .map(XYChart.Data::getYValue)
//                                        .collect(Collectors.joining()))
//                                .collect(Collectors.joining());
                    },
                    event -> {
                        busyIndicator.setVisible(false);
                        String data = (String) event.getSource().getValue();
                        textOutput.clear();
                        textOutput.replaceText(0, 0, data);
                        if (worksheet.isSyntaxHighlightEnabled()) {
                            this.syntaxHilightStyleSpans = CodeAreaHighlighter.computeSyntaxHighlighting(textOutput.getText());
                            textOutput.setStyleSpans(0, syntaxHilightStyleSpans);
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
}
