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

package eu.binjr.core.controllers;


import eu.binjr.common.colors.ColorUtils;
import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.common.javafx.richtext.CodeAreaHighlighter;
import eu.binjr.common.javafx.richtext.HighlightPatternException;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.navigation.RingIterator;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.timeseries.transform.SortTransform;
import eu.binjr.core.data.workspace.TextFilesWorksheet;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserPreferences;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
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
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public class TextWorksheetController extends WorksheetController {
    private static final Logger logger = Logger.create(TextWorksheetController.class);
    public static final String WORKSHEET_VIEW_FXML = "/eu/binjr/views/TextWorksheetView.fxml";
    private final TextFilesWorksheet worksheet;
    private final Property<TimeRange> timeRangeProperty = new SimpleObjectProperty<>(TimeRange.of(ZonedDateTime.now().minusHours(1), ZonedDateTime.now()));
    private StyleSpans<Collection<String>> syntaxHilightStyleSpans;
    private RingIterator<CodeAreaHighlighter.SearchHitRange> searchHitIterator = RingIterator.of(Collections.emptyList());


    public TextWorksheetController(MainViewController parent, TextFilesWorksheet worksheet, Collection<DataAdapter<String>> adapters)
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
    public Worksheet<?> getWorksheet() {
        return worksheet;
    }

    @FXML
    public MaskerPane busyIndicator;

    @Override
    public Property<TimeRange> selectedRangeProperty() {
        return timeRangeProperty;
    }

    @Override
    public Optional<ChartViewPort> getAttachedViewport(TitledPane pane) {
        return Optional.empty();
    }

    @Override
    public ContextMenu getChartListContextMenu(Collection<TreeItem<SourceBinding>> treeView) {
        MenuItem item = new MenuItem(worksheet.getName());
        item.setDisable(true);
        return new ContextMenu(item);
    }

    @Override
    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
    }

    @Override
    public void refresh() {
        invalidate(null, false, true);
    }

    public void invalidateAll(boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh) {
        invalidate(null, dontPlotChart, forceRefresh);
    }

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
        super.close();
    }

    @Override
    public String getView() {
        return WORKSHEET_VIEW_FXML;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getBindingManager().attachListener(worksheet.textViewFontSizeProperty(),
                (ChangeListener<Integer>) (obs, oldVal, newVal) -> textOutput.setStyle("-fx-font-size: " + newVal + "pt;"));
        textOutput.setParagraphGraphicFactory(LineNumberFactory.get(textOutput));
        textOutput.setEditable(false);
        getBindingManager().bind(textOutput.wrapTextProperty(), wordWrapButton.selectedProperty());
        refreshButton.setOnAction(getBindingManager().registerHandler(event -> refresh()));

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
        getBindingManager().bind(clearSearchButton.visibleProperty(),
                Bindings.createBooleanBinding(() -> !searchTextField.getText().isBlank(), searchTextField.textProperty()));
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
        CodeAreaHighlighter.SearchHighlightResults searchResults;
        try {
            searchTextField.setStyle("");
            searchResults = CodeAreaHighlighter.computeSearchHitsHighlighting(textOutput.getText(), searchText, matchCase, regEx);
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
        } catch (HighlightPatternException e) {
            if (searchRegExToggle.isSelected()) {
                logger.debug(e.getMessage(), e);
                searchTextField.setStyle(String.format("-fx-background-color: %s;",
                        ColorUtils.toHex(UserPreferences.getInstance().invalidInputColor.get())));
                searchResultsLabel.setText("Bad pattern");
            }
        }
    }

    public void fetchDataFromSources() throws DataAdapterException {
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
            for (var byPathEntry : bindingsByPath.entrySet()) {
                String path = byPathEntry.getKey();
                logger.trace("Fetch sub-task '" + path + "' started");
                // Get data from the adapter
                var range = adapter.getInitialTimeRange(path, byPathEntry.getValue());
                var data = adapter.fetchData(
                        path,
                        range.getBeginning().toInstant(),
                        range.getEnd().toInstant(),
                        byPathEntry.getValue(),
                        true);
                data.entrySet().parallelStream().forEach(entry -> {
                    var info = entry.getKey();
                    var proc = entry.getValue();
                    //bind proc to timeSeries info
                    info.setProcessor(proc);
                });
            }
        }
    }

    private void loadFile() {
        try {
            AsyncTaskManager.getInstance().submit(() -> {
                        busyIndicator.setVisible(true);
                        fetchDataFromSources();
                        return worksheet.getSeriesInfo().stream()
                                .map(info -> info.getProcessor()
                                        .getData()
                                        .stream()
                                        .map(XYChart.Data::getYValue)
                                        .collect(Collectors.joining()))
                                .collect(Collectors.joining());
                    },
                    event -> {
                        busyIndicator.setVisible(false);
                        String data = (String) event.getSource().getValue();
                        textOutput.clear();
                        textOutput.replaceText(0, 0, data);
                        if (worksheet.isSyntaxHighlightEnabled()) {
                            this.syntaxHilightStyleSpans = CodeAreaHighlighter.computeXmlSyntaxHighlighting(textOutput.getText());
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
}
