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
import eu.binjr.common.colors.ColorUtils;
import eu.binjr.common.javafx.controls.*;
import eu.binjr.common.javafx.richtext.CodeAreaHighlighter;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.common.navigation.RingIterator;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.indexes.Indexes;
import eu.binjr.core.data.indexes.SearchHit;
import eu.binjr.core.data.indexes.SearchHitsProcessor;
import eu.binjr.core.data.indexes.logs.LogFileIndex;
import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.timeseries.FacetEntry;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.LogWorksheet;
import eu.binjr.core.data.workspace.Syncable;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserPreferences;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.ReadOnlyStyledDocumentBuilder;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyleSpans;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static javafx.scene.control.SelectionMode.MULTIPLE;

public class LogWorksheetController extends WorksheetController implements Syncable {
    public static final String WORKSHEET_VIEW_FXML = "/eu/binjr/views/LogWorksheetView.fxml";
    private static final Logger logger = Logger.create(LogWorksheetController.class);
    private static final Gson gson = new Gson();
    private final LogWorksheet worksheet;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final UserPreferences userPrefs = UserPreferences.getInstance();
    private final Property<Collection<FacetEntry>> pathFacetEntries = new SimpleObjectProperty<>();
    private StyleSpans<Collection<String>> syntaxHighlightStyleSpans;
    private RingIterator<CodeAreaHighlighter.SearchHitRange> searchHitIterator = RingIterator.of(Collections.emptyList());
    private Path tmpCssPath;
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
    @FXML
    private Button querySyntaxButton;
    @FXML
    private VBox busyIndicator;
    @FXML
    private ProgressBar progressIndicator;
    @FXML
    private Label progressStatus;
    @FXML
    private FacetPillsContainer severityListView;
    @FXML
    private TextField filterTextField;
    @FXML
    private Button clearFilterButton;
    @FXML
    private Button applyFilterButton;
    @FXML
    private HBox filteringBar;
    @FXML
    private ToggleButton findToggleButton;
    @FXML
    private HBox highlightControls;
    @FXML
    private HBox paginationBar;
    @FXML
    private TableView<TimeSeriesInfo<SearchHit>> fileTable;
    @FXML
    private StackPane fileTablePane;
    @FXML
    private SplitPane splitPane;
    @FXML
    private VBox logsToolPane;

    public LogWorksheetController(MainViewController parent, LogWorksheet worksheet, Collection<DataAdapter<SearchHit>> adapters)
            throws NoAdapterFoundException {
        super(parent);
        this.worksheet = worksheet;
        for (TimeSeriesInfo<SearchHit> d : worksheet.getSeriesInfo()) {
            UUID id = d.getBinding().getAdapterId();
            DataAdapter<SearchHit> da = adapters
                    .stream()
                    .filter(a -> (id != null && a != null && a.getId() != null) && id.equals(a.getId()))
                    .findAny()
                    .orElseThrow(() -> new NoAdapterFoundException("Failed to find a valid adapter with id " +
                            (id != null ? id.toString() : "null")));
            d.getBinding().setAdapter(da);
        }
    }

    @FXML
    private void handleHistoryBack(ActionEvent actionEvent) {
        worksheet.getHistory().getPrevious().ifPresent(h -> restoreQueryParameters(h, false));
    }

    @FXML
    private void handleHistoryForward(ActionEvent actionEvent) {
        worksheet.getHistory().getNext().ifPresent(h -> restoreQueryParameters(h, false));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getBindingManager().attachListener(worksheet.textViewFontSizeProperty(),
                (ChangeListener<Integer>) (obs, oldVal, newVal) -> textOutput.setStyle("-fx-font-size: " + newVal + "pt;"));
        textOutput.setEditable(false);
        getBindingManager().bind(textOutput.wrapTextProperty(), wordWrapButton.selectedProperty());
        refreshButton.setOnAction(getBindingManager().registerHandler(event -> refresh()));
        // TimeRange Picker initialization
        timeRangePicker.timeRangeLinkedProperty().bindBidirectional(worksheet.timeRangeLinkedProperty());
        timeRangePicker.initSelectedRange(worksheet.getQueryParameters().getTimeRange());
        timeRangePicker.setOnSelectedRangeChanged((observable, oldValue, newValue) -> {
            invalidateFilter(true);
        });
        timeRangePicker.setOnResetInterval(() -> {
            try {
               return worksheet.getInitialTimeRange();
            } catch (Exception e) {
                Dialogs.notifyException("Error resetting time range", e);
            }
            return TimeRange.of(ZonedDateTime.now().minusHours(24), ZonedDateTime.now());
        });
        // Init navigation
        backButton.setOnAction(bindingManager.registerHandler(this::handleHistoryBack));
        forwardButton.setOnAction(bindingManager.registerHandler(this::handleHistoryForward));
        bindingManager.bind(backButton.disableProperty(), worksheet.getHistory().backward().emptyProperty());
        bindingManager.bind(forwardButton.disableProperty(), worksheet.getHistory().forward().emptyProperty());
        // Query syntax help
        var syntaxPopupRoot = new StackPane();
        syntaxPopupRoot.getStyleClass().addAll("syntax-help-popup");
        syntaxPopupRoot.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
        syntaxPopupRoot.setPrefSize(600, 700);
        var syntaxCheatSheet = new StyleClassedTextArea();
        syntaxCheatSheet.setEditable(false);
        syntaxCheatSheet.append("Query Syntax\n\n", "syntax-help-title");
        syntaxCheatSheet.append("Search for word \"foo\":\n", "syntax-help-text");
        syntaxCheatSheet.append(" foo \n\n", "syntax-help-code");
        syntaxCheatSheet.append("Search for word \"foo\" OR for word \"bar\":\n", "syntax-help-text");
        syntaxCheatSheet.append(" foo bar ", "syntax-help-code");
        syntaxCheatSheet.append(" or ", "syntax-help-text");
        syntaxCheatSheet.append(" foo OR bar \n\n", "syntax-help-code");
        syntaxCheatSheet.append("Search for phrase \"foo bar\":\n", "syntax-help-text");
        syntaxCheatSheet.append(" \"foo bar\" \n\n", "syntax-help-code");
        syntaxCheatSheet.append("Search for phrase \"foo bar\"  AND the phrase \"quick fox\":\n", "syntax-help-text");
        syntaxCheatSheet.append(" \"foo bar\" AND \"quick fox\" ", "syntax-help-code");
        syntaxCheatSheet.append(" or ", "syntax-help-text");
        syntaxCheatSheet.append(" \"foo bar\" +\"quick fox\" \n\n", "syntax-help-code");
        syntaxCheatSheet.append("Search for either the phrase \"foo bar\" AND the phrase \"quick fox\", or the phrase \"hello world\":  \n", "syntax-help-text");
        syntaxCheatSheet.append(" (\"foo bar\" AND \"quick fox\") OR \"hello world\" \n\n", "syntax-help-code");
        syntaxCheatSheet.append("Search for word \"foo\" and not \"bar\": (Note: The NOT operator cannot be used with just one term)\n", "syntax-help-text");
        syntaxCheatSheet.append(" foo NOT bar ", "syntax-help-code");
        syntaxCheatSheet.append(" or ", "syntax-help-text");
        syntaxCheatSheet.append(" foo -bar \n\n", "syntax-help-code");
        syntaxCheatSheet.append("Search for everything not containing \"foo bar\":\n", "syntax-help-text");
        syntaxCheatSheet.append(" *:* NOT \"foo bar\"", "syntax-help-code");
        syntaxCheatSheet.append(" or ", "syntax-help-text");
        syntaxCheatSheet.append(" *:* -\"foo bar\" \n\n", "syntax-help-code");
        syntaxCheatSheet.append("Search for any word that starts with \"foo\":\n", "syntax-help-text");
        syntaxCheatSheet.append(" foo* \n\n", "syntax-help-code");
        syntaxCheatSheet.append("Search for any word that starts with \"foo\" and ends with bar:\n", "syntax-help-text");
        syntaxCheatSheet.append(" foo*bar \n\n", "syntax-help-code");
        syntaxCheatSheet.append(" Search for a term similar in spelling to \"foobar\" (e.g. \"fuzzy search\"): \n", "syntax-help-text");
        syntaxCheatSheet.append(" foobar~ \n\n", "syntax-help-code");
        syntaxCheatSheet.append("Search for \"foo bar\" within 4 words from each other:\n", "syntax-help-text");
        syntaxCheatSheet.append(" \"foo bar\"~4 \n", "syntax-help-code");
        syntaxPopupRoot.getChildren().add(syntaxCheatSheet);
        var popup = new PopupControl();
        popup.setAutoHide(true);
        popup.getScene().setRoot(syntaxPopupRoot);
        querySyntaxButton.setOnAction(bindingManager.registerHandler(actionEvent -> {
            Node owner = (Node) actionEvent.getSource();
            Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            popup.show(owner.getScene().getWindow(), bounds.getMaxX() - 600, bounds.getMaxY());
        }));

        // init filter controls
        filterTextField.setText(worksheet.getQueryParameters().getFilterQuery());
        pager.setCurrentPageIndex(worksheet.getQueryParameters().getPage());
        bindingManager.bind(paginationBar.managedProperty(), paginationBar.visibleProperty());
        bindingManager.bind(paginationBar.visibleProperty(), pager.pageCountProperty().greaterThan(1));

        // Filter selection
        bindingManager.bind(logsToolPane.managedProperty(), logsToolPane.visibleProperty());
        bindingManager.bind(logsToolPane.visibleProperty(), filteringBar.visibleProperty().or(highlightControls.visibleProperty()));

        bindingManager.bind(filteringBar.managedProperty(), filteringBar.visibleProperty());
        bindingManager.bind(filteringBar.visibleProperty(), filterToggleButton.selectedProperty());
        bindingManager.attachListener(severityListView.getSelectedPills(),
                (SetChangeListener<FacetPillsContainer.FacetPill>) l -> invalidateFilter(true));
        bindingManager.attachListener(pager.currentPageIndexProperty(), (o, oldVal, newVal) -> invalidateFilter(false));
        filterTextField.setOnAction(bindingManager.registerHandler(event -> invalidateFilter(true)));
        clearFilterButton.setOnAction(bindingManager.registerHandler(actionEvent -> {
            filterTextField.clear();
            invalidateFilter(true);
        }));
        applyFilterButton.setOnAction(bindingManager.registerHandler(event -> invalidateFilter(true)));
        bindingManager.bind(clearFilterButton.visibleProperty(),
                Bindings.createBooleanBinding(() -> !filterTextField.getText().isBlank(), filterTextField.textProperty()));
        //Search bar initialization
        bindingManager.bind(highlightControls.managedProperty(), highlightControls.visibleProperty());
        bindingManager.bind(highlightControls.visibleProperty(), findToggleButton.selectedProperty());

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
        bindingManager.bind(clearSearchButton.visibleProperty(),
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

        // Init log files table view
        intiLogFileTable();

        splitPane.setDividerPositions(worksheet.getDividerPosition());
        bindingManager.bind(worksheet.dividerPositionProperty(), splitPane.getDividers().get(0).positionProperty());

        var eventTarget = root.getParent();
        if (eventTarget == null) {
            eventTarget = root;
        }
        eventTarget.addEventFilter(KeyEvent.KEY_RELEASED, bindingManager.registerHandler(e -> {
            if (e.getCode() == KeyCode.K && e.isControlDown()) {
                filterToggleButton.setSelected(true);
                filterTextField.requestFocus();
            }
            if (e.getCode() == KeyCode.F && e.isControlDown()) {
                findToggleButton.setSelected(true);
                searchTextField.requestFocus();
            }
        }));

        // Setup drag and drop
        textOutput.setOnDragOver(getBindingManager().registerHandler(this::handleDragOverWorksheetView));
        textOutput.setOnDragDropped(getBindingManager().registerHandler(this::handleDragDroppedOnWorksheetView));

        textOutput.setOnDragEntered(getBindingManager().registerHandler(event -> {
            textOutput.setStyle("-fx-background-color:  -fx-accent-translucide;");

        }));
        textOutput.setOnDragExited(getBindingManager().registerHandler(event -> {
            textOutput.setStyle("-fx-background-color:  -binjr-pane-background-color;");
        }));

        bindingManager.bind(progressStatus.textProperty(), Bindings.createStringBinding(() -> {
            if (progressIndicator.getProgress() < 0) {
                return "";
            } else {
                return String.format("%.0f%%", progressIndicator.getProgress() * 100);
            }

        }, progressIndicator.progressProperty()));
//        worksheet.getSeriesInfo().stream()
//                .map(t -> t.getBinding().getAdapter()).findAny().ifPresent(a ->
        bindingManager.bind(progressIndicator.progressProperty(), worksheet.progressProperty());

        refresh();
        super.initialize(location, resources);
    }

    @Override
    public Worksheet getWorksheet() {
        return worksheet;
    }

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
        MenuItem item = new MenuItem(worksheet.getName());
        item.setOnAction((bindingManager.registerHandler(event -> {
            addToCurrentWorksheet(treeView);
        })));
        return new ContextMenu(item);
    }

    private void handleDragOverWorksheetView(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(DataFormat.lookupMimeType(LogFilesBinding.MIME_TYPE))) {
            event.acceptTransferModes(TransferMode.MOVE);
            event.consume();
        }
    }

    private void handleDragDroppedOnWorksheetView(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(DataFormat.lookupMimeType(LogFilesBinding.MIME_TYPE))) {
            TreeView<SourceBinding> treeView = parentController.getSelectedTreeView();
            if (treeView != null) {
                TreeItem<SourceBinding> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null) {
                    Stage targetStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    if (targetStage != null) {
                        targetStage.requestFocus();
                    }
                    var items = treeView.getSelectionModel().getSelectedItems();
                    addToCurrentWorksheet(items);
                } else {
                    logger.warn("Cannot complete drag and drop operation: selected TreeItem is null");
                }
            } else {
                logger.warn("Cannot complete drag and drop operation: selected TreeView is null");
            }
            event.consume();
        }
    }

    private void addToCurrentWorksheet(Collection<TreeItem<SourceBinding>> sourceBindings) {
        try {
            // Schedule for later execution in order to let other drag and dropped event to complete before modal dialog gets displayed
            Platform.runLater(() -> {
                if (sourceBindings != null && !sourceBindings.isEmpty()) {
                    var l = sourceBindings.stream()
                            .flatMap(item -> TreeViewUtils.flattenLeaves(item, true).stream())
                            //      .map( binding-> (TimeSeriesInfo<SearchHit>)TimeSeriesInfo.fromBinding(binding))
                            .collect(Collectors.toList());
                    addBindings(l);

                }
            });
        } catch (Exception e) {
            Dialogs.notifyException("Error adding bindings to existing worksheet", e, root);
        }
    }

    private void addBindings(Collection<SourceBinding> sourceBindings) {
        Collection<LogFilesBinding> logFilesBindings = new ArrayList<>();
        for (var sb : sourceBindings) {
            if (sb instanceof LogFilesBinding) {
                logFilesBindings.add((LogFilesBinding) sb);
            }
        }
        if (logFilesBindings.size() >= userPrefs.maxSeriesPerChartBeforeWarning.get().intValue()) {
            if (Dialogs.confirmDialog(root,
                    "This action will add " + logFilesBindings.size() + " series on a single view.",
                    "Are you sure you want to proceed?",
                    ButtonType.YES, ButtonType.NO) != ButtonType.YES) {
                return;
            }
        }
        InvalidationListener isVisibleListener = (observable) -> {
            boolean andAll = true;
            boolean orAll = false;
            for (var t : worksheet.getSeries()) {
                andAll &= t.isSelected();
                orAll |= t.isSelected();
            }
            CheckBox showAllCheckBox = (CheckBox) fileTable.getColumns().get(0).getGraphic();
            showAllCheckBox.setIndeterminate(Boolean.logicalXor(andAll, orAll));
            showAllCheckBox.setSelected(andAll);
        };
        for (var binding : logFilesBindings) {
            if (binding instanceof LogFilesBinding) {
                //    logFilesBindings.add((LogFilesBinding) sb);
                if (worksheet.getSeriesInfo().stream().filter(s -> s.getBinding().equals(binding)).findAny().isEmpty()) {
                    var i = TimeSeriesInfo.fromBinding(binding);
                    bindingManager.attachListener(i.selectedProperty(), isVisibleListener);
                    bindingManager.attachListener(i.selectedProperty(), (ChangeListener<Boolean>) (o, oldVal, newVal) -> invalidate(false, true));
                    bindingManager.attachListener(i.displayColorProperty(), (ChangeListener<Color>) (o, oldVal, newVal) -> invalidate(false, false));
                    worksheet.getSeriesInfo().add(i);
                }
            }
        }
        this.refresh();
    }

    @Override
    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
    }

    @Override
    protected void setEditChartMode(Boolean newValue) {
        if (!newValue) {
            bindingManager.suspend(worksheet.dividerPositionProperty());
            splitPane.setDividerPositions(1.0);
            fileTablePane.setVisible(false);
            fileTablePane.setManaged(false);
            fileTablePane.setMaxHeight(0.0);
        } else {
            fileTablePane.setMaxHeight(Double.MAX_VALUE);
            fileTablePane.setManaged(true);
            fileTablePane.setVisible(true);
            splitPane.setDividerPositions(worksheet.getDividerPosition());
            bindingManager.resume(worksheet.dividerPositionProperty());
        }
        setShowPropertiesPane(newValue);
        super.setEditChartMode(newValue);
    }

    @Override
    public void refresh() {
        invalidate(false, false);
    }

    private void invalidate(boolean saveToHistory, boolean resetPage) {
        makeFilesCss(worksheet.getSeriesInfo());
        if (resetPage) {
            worksheet.setQueryParameters(new LogQueryParameters.Builder(worksheet.getQueryParameters())
                    .setPage(0)
                    .build());
        }
        worksheet.getHistory().setHead(worksheet.getQueryParameters(), saveToHistory);
        queryLogIndex();
    }

    private void queryLogIndex() {
        try {
            AsyncTaskManager.getInstance().submit(() -> {
                        busyIndicator.setVisible(true);
                        return (SearchHitsProcessor) fetchDataFromSources(worksheet.getQueryParameters());
                    },
                    event -> {
                        bindingManager.suspend();
                        try {
                            // Reset page number
                            var res = (SearchHitsProcessor) event.getSource().getValue();
                            pager.setPageCount((int) Math.ceil((double) res.getTotalHits() / res.getHitsPerPage()));
                            pager.setCurrentPageIndex(worksheet.getQueryParameters().getPage());
                            // Update severity facet view
                            var severityFacetEntries = res.getFacetResults().get(CaptureGroup.SEVERITY);
                            severityListView.setAllEntries((severityFacetEntries != null) ? severityFacetEntries : Collections.emptyList());
                            severityListView.getFacetPills()
                                    .forEach(f -> {
                                        f.getStyleClass().add("facet-pill-" + f.getFacet().getLabel().toLowerCase());
                                        f.setSelected(worksheet.getQueryParameters().getSeverities().contains(f.getFacet().getLabel()));
                                    });
                            // Update filePath facet view
                            var fileFacetEntries = res.getFacetResults().get(LogFileIndex.PATH);
                            if (fileFacetEntries != null) {
                                this.pathFacetEntries.setValue(fileFacetEntries);
                            } else {
                                this.pathFacetEntries.setValue(Collections.emptyList());
                            }
                            // Color and display message text
                            try (var p = Profiler.start("Display text", logger::perf)) {
                                var docBuilder = new ReadOnlyStyledDocumentBuilder<Collection<String>, String, Collection<String>>(
                                        SegmentOps.styledTextOps(),
                                        Collections.emptyList());
                                for (var data : res.getData()) {
                                    var hit = data.getYValue();
                                    var severity = hit.getFacets().get(CaptureGroup.SEVERITY).getLabel();
                                    var path = hit.getFacets().get(LogFileIndex.PATH).getLabel();
                                    var message = hit.getText().stripTrailing();
                                    docBuilder.addParagraph(
                                            message,
                                            List.of(severity),
                                            List.of("file-" + path.hashCode()));
                                }
                                // Add a dummy paragraph if result set is empty, otherwise doc creation will fail
                                if (res.getData().size() == 0) {
                                    docBuilder.addParagraph("", Collections.emptyList(), Collections.emptyList());
                                }
                                var doc = docBuilder.build();
                                syntaxHighlightStyleSpans = doc.getStyleSpans(0, doc.getText().length());
                                textOutput.replace(doc);
                                // Reset search highlight
                                if (!searchTextField.getText().isEmpty()) {
                                    doSearchHighlight(searchTextField.getText(),
                                            searchMatchCaseToggle.isSelected(),
                                            searchRegExToggle.isSelected());
                                }
                            } catch (Exception e) {
                                Dialogs.notifyException(e);
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


    private void intiLogFileTable() {
        DecimalFormatTableCellFactory<TimeSeriesInfo<SearchHit>, String> alignRightCellFactory = new DecimalFormatTableCellFactory<>();
        alignRightCellFactory.setAlignment(TextAlignment.LEFT);
        // alignRightCellFactory.setPattern("###");
        CheckBox showAllCheckBox = new CheckBox();
        TableColumn<TimeSeriesInfo<SearchHit>, Boolean> visibleColumn = new TableColumn<>();
        visibleColumn.setGraphic(showAllCheckBox);
        visibleColumn.setSortable(false);
        visibleColumn.setResizable(false);
        visibleColumn.setPrefWidth(32);
        visibleColumn.setCellValueFactory(p -> p.getValue().selectedProperty());
        visibleColumn.setCellFactory(CheckBoxTableCell.forTableColumn(visibleColumn));

        InvalidationListener isVisibleListener = (observable) -> {
            boolean andAll = true;
            boolean orAll = false;
            for (var t : worksheet.getSeriesInfo()) {
                andAll &= t.isSelected();
                orAll |= t.isSelected();
            }
            showAllCheckBox.setIndeterminate(Boolean.logicalXor(andAll, orAll));
            showAllCheckBox.setSelected(andAll);
        };

        for (var i : worksheet.getSeriesInfo()) {
            bindingManager.attachListener(i.selectedProperty(), isVisibleListener);
            bindingManager.attachListener(i.selectedProperty(), (ChangeListener<Boolean>) (o, oldVal, newVal) -> invalidate(false, true));
            bindingManager.attachListener(i.displayColorProperty(), (ChangeListener<Color>) (o, oldVal, newVal) -> invalidate(false, false));
            // Explicitly call the listener to initialize the proper status of the checkbox
            isVisibleListener.invalidated(null);
        }

        showAllCheckBox.setOnAction(bindingManager.registerHandler(event -> {
            ChangeListener<Boolean> r = (o, oldVal, newVal) -> invalidate(false, true);
            boolean b = ((CheckBox) event.getSource()).isSelected();
            worksheet.getSeriesInfo().forEach(s -> bindingManager.detachAllChangeListeners(s.selectedProperty()));
            worksheet.getSeriesInfo().forEach(t -> t.setSelected(b));
            r.changed(null, null, null);
            worksheet.getSeriesInfo().forEach(s -> bindingManager.attachListener(s.selectedProperty(), r));
        }));

        TableColumn<TimeSeriesInfo<SearchHit>, Color> colorColumn = new TableColumn<>();
        colorColumn.setSortable(false);
        colorColumn.setResizable(false);
        colorColumn.setPrefWidth(32);
        colorColumn.setCellFactory(param -> new ColorTableCell<>(colorColumn));
        colorColumn.setCellValueFactory(p -> p.getValue().displayColorProperty());

        TableColumn<TimeSeriesInfo<SearchHit>, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setSortable(false);
        nameColumn.setPrefWidth(350);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));

        TableColumn<TimeSeriesInfo<SearchHit>, String> eventNumColumn = new TableColumn<>("Nb events");
        eventNumColumn.setSortable(false);
        eventNumColumn.setPrefWidth(75);
        eventNumColumn.setCellFactory(alignRightCellFactory);
        eventNumColumn.setCellValueFactory(p -> Bindings.createStringBinding(
                () -> pathFacetEntries.getValue() == null ? "-" :
                        pathFacetEntries.getValue().stream()
                                .filter(e -> e.getLabel().equalsIgnoreCase(p.getValue().getBinding().getPath()))
                                .map(e -> Integer.toString(e.getNbOccurrences())).findFirst().orElse("0"),
                pathFacetEntries));

        TableColumn<TimeSeriesInfo<SearchHit>, String> pathColumn = new TableColumn<>("Path");
        pathColumn.setSortable(false);
        pathColumn.setPrefWidth(400);
        pathColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getBinding().getTreeHierarchy()));

        fileTable.getSelectionModel().setSelectionMode(MULTIPLE);
        fileTable.setOnKeyReleased(bindingManager.registerHandler(event -> {
            if (event.getCode().equals(KeyCode.DELETE)) {
                var selected = new ArrayList<>(fileTable.getSelectionModel().getSelectedItems());
                selected.forEach(s -> {
                    bindingManager.detachAllInvalidationListeners(s.selectedProperty());
                    bindingManager.detachAllInvalidationListeners(s.displayColorProperty());
                });
                fileTable.getItems().removeAll(selected);
                fileTable.getSelectionModel().clearSelection();
                invalidate(false, false);
            }
        }));

        fileTable.setItems(worksheet.getSeriesInfo());
        fileTable.getColumns().addAll(
                visibleColumn,
                colorColumn,
                nameColumn,
                eventNumColumn,
                pathColumn);
        TableViewUtils.autoFillTableWidthWithLastColumn(fileTable);
    }

    private void invalidateFilter(boolean resetPage) {
        var newParams = new LogQueryParameters.Builder()
                .setTimeRange(timeRangePicker.getSelectedRange())
                .setFilterQuery(filterTextField.getText())
                .setSeverities(severityListView.getFacetPills()
                        .stream()
                        .filter(p -> p != null && p.isSelected())
                        .map(p -> p.getFacet().getLabel())
                        .collect(Collectors.toSet()))
                .setPage(resetPage ? 0 : pager.getCurrentPageIndex())
                .build();
        if (newParams != worksheet.getQueryParameters()) {
            worksheet.setQueryParameters(newParams);
            invalidate(true, resetPage);
        }
    }

    private void restoreQueryParameters(LogQueryParameters newParams, boolean resetPage) {
        if (newParams != worksheet.getQueryParameters()) {
            bindingManager.suspend();
            try {
                worksheet.setQueryParameters(newParams);
                timeRangePicker.updateSelectedRange(newParams.getTimeRange());
                filterTextField.setText(newParams.getFilterQuery());
                pager.setCurrentPageIndex(newParams.getPage());
            } finally {
                bindingManager.resume();
            }
            invalidate(false, resetPage);
        }
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
        try (var p = Profiler.start("Applying search result highlights", logger::perf)) {
            var searchResults =
                    CodeAreaHighlighter.computeSearchHitsHighlighting(textOutput.getText(), searchText, matchCase, regEx);
            prevOccurrenceButton.setDisable(searchResults.getSearchHitRanges().isEmpty());
            nextOccurrenceButton.setDisable(searchResults.getSearchHitRanges().isEmpty());
            searchHitIterator = RingIterator.of(searchResults.getSearchHitRanges());
            searchResultsLabel.setText(searchResults.getSearchHitRanges().size() + " results");
            if (syntaxHighlightStyleSpans != null) {
                textOutput.setStyleSpans(0, syntaxHighlightStyleSpans.overlay(searchResults.getStyleSpans(),
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
    }

    private TimeSeriesProcessor<SearchHit> fetchDataFromSources(LogQueryParameters filter) throws DataAdapterException {
        // prune series from closed adapters
        worksheet.getSeriesInfo().removeIf(seriesInfo -> {
            if (seriesInfo.getBinding().getAdapter().isClosed()) {
                logger.debug(() -> seriesInfo.getDisplayName() + " will be pruned because attached adapter " +
                        seriesInfo.getBinding().getAdapter().getId() + " is closed.");
                return true;
            }
            return false;
        });
        if (filter.getTimeRange().getBeginning().toInstant().equals(Instant.EPOCH) &&
                filter.getTimeRange().getDuration() == java.time.Duration.ZERO) {
            var initialRange = worksheet.getInitialTimeRange();
            worksheet.setQueryParameters(new LogQueryParameters.Builder(filter).setTimeRange(initialRange).build());
            timeRangePicker.updateSelectedRange(initialRange);
        }
        var queryArgs = gson.toJson(filter);
        var start = worksheet.getQueryParameters().getTimeRange().getBeginning().toInstant();
        var end = worksheet.getQueryParameters().getTimeRange().getEnd().toInstant();
        var bindingsByAdapters =
                worksheet.getSeriesInfo().stream().collect(groupingBy(o -> o.getBinding().getAdapter()));
        for (var byAdapterEntry : bindingsByAdapters.entrySet()) {
            if ( byAdapterEntry.getKey() instanceof ProgressAdapter) {
                var adapter = (ProgressAdapter<SearchHit>) byAdapterEntry.getKey();
                // Group all queries with the same adapter and path
                var bindingsByPath =
                        byAdapterEntry.getValue().stream().collect(groupingBy(o -> o.getBinding().getPath()));
                var data = adapter.fetchData(queryArgs, start, end,
                        bindingsByPath.values().stream()
                                .flatMap(Collection::stream)
                                .filter(TimeSeriesInfo::isSelected)
                                .collect(Collectors.toList()), true, worksheet.progressProperty());
            }
        }
        Map<String, Collection<String>> facets = new HashMap<>();
        facets.put(LogFileIndex.PATH, worksheet.getSeriesInfo()
                .stream()
                .filter(TimeSeriesInfo::isSelected)
                .map(i -> i.getBinding().getPath())
                .collect(Collectors.toList()));
        var params = worksheet.getQueryParameters();
        facets.put(CaptureGroup.SEVERITY, params.getSeverities());
        try {
            return (facets.get(LogFileIndex.PATH).size() == 0) ? new SearchHitsProcessor() :
                    Indexes.LOG_FILES.get().search(start.toEpochMilli(), end.toEpochMilli(),
                            facets,
                            params.getFilterQuery(),
                            params.getPage(),
                            timeRangePicker.getZoneId());
        } catch (Exception e) {
            throw new DataAdapterException("Error fetching logs from index: " + e.getMessage(), e);
        }
    }

    private void makeFilesCss(Collection<TimeSeriesInfo<SearchHit>> info) {
        try {
            Path cssPath = getTmpCssPath();
            String cssUrl = cssPath.toUri().toURL().toExternalForm();
            if (root.getStylesheets().contains(cssUrl)) {
                root.getStylesheets().remove(cssUrl);
            }
            String cssStr = info
                    .stream()
                    .map((i) -> ".file-" + i.getBinding().getPath().hashCode() +
                            "{-fx-background-color:" + ColorUtils.toHex(i.getDisplayColor(), 0.2) + ";}")
                    .collect(Collectors.joining("\n"));
            Files.writeString(cssPath, cssStr, StandardOpenOption.TRUNCATE_EXISTING);
            root.getStylesheets().add(cssUrl);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private Path getTmpCssPath() throws IOException {

        if (this.tmpCssPath == null) {
            tmpCssPath = Files.createTempFile("tmp_", ".css");
            tmpCssPath.toFile().deleteOnExit();
        }
        return tmpCssPath;

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
