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


import com.google.gson.Gson;
import eu.binjr.common.colors.ColorUtils;
import eu.binjr.common.javafx.charts.MetricStableTicksAxis;
import eu.binjr.common.javafx.charts.StableTicksAxis;
import eu.binjr.common.javafx.charts.XYChartCrosshair;
import eu.binjr.common.javafx.charts.ZonedDateTimeAxis;
import eu.binjr.common.javafx.controls.*;
import eu.binjr.common.javafx.richtext.CodeAreaHighlighter;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;
import eu.binjr.common.navigation.RingIterator;
import eu.binjr.common.preferences.MostRecentlyUsedList;
import eu.binjr.common.text.StringUtils;
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
import eu.binjr.core.preferences.UserHistory;
import eu.binjr.core.preferences.UserPreferences;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.WeakEventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static javafx.scene.control.SelectionMode.MULTIPLE;

public class LogWorksheetController extends WorksheetController implements Syncable {
    public static final String WORKSHEET_VIEW_FXML = "/eu/binjr/views/LogWorksheetView.fxml";
    private static final Logger logger = Logger.create(LogWorksheetController.class);
    private static final Gson gson = new Gson();
    public static final String PSEUDOCLASS_FAVORITES = "favorites";
    public static final String PSEUDOCLASS_HISTORY = "history";
    public static final String PSEUDOCLASS_CATEGORY = "category";
    private static final DateTimeFormatter TIMELINE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final double AXIS_WIDTH = 15.0;
    public static final double AXIS_HEIGHT = 15.0;
    private final LogWorksheet worksheet;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final UserPreferences userPrefs = UserPreferences.getInstance();
    private final Property<Collection<FacetEntry>> pathFacetEntries = new SimpleObjectProperty<>();
    private final MostRecentlyUsedList<String> mostRecentLogFilters = UserHistory.getInstance().mostRecentLogFilters;
    private final MostRecentlyUsedList<String> favoriteLogFilters = userPrefs.getFavorites().favoriteLogFilters;
    private StyleSpans<Collection<String>> syntaxHighlightStyleSpans;
    private RingIterator<CodeAreaHighlighter.SearchHitRange> searchHitIterator = RingIterator.of(Collections.emptyList());
    private final BooleanProperty filterApplied = new SimpleBooleanProperty(true);
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
    private HBox filterBar;
    @FXML
    private ToggleButton findToggleButton;
    @FXML
    private ToggleButton heatmapToggleButton;
    @FXML
    private HBox highlightControls;
    @FXML
    private Pane heatmapArea;
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
    @FXML
    private Button showSuggestButton;
    @FXML
    private Button favoriteButton;
    private BarChart<String, Integer> heatmap;
    private XYChart<ZonedDateTime, Double> timeline;

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

    @Override
    public void navigateBackward() {
        worksheet.getHistory().getPrevious().ifPresent(h -> restoreQueryParameters(h, false));
    }

    @Override
    public void navigateForward() {
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
        backButton.setOnAction(getBindingManager().registerHandler(event -> this.navigateBackward()));
        forwardButton.setOnAction(getBindingManager().registerHandler(event -> this.navigateForward()));
        getBindingManager().bind(backButton.disableProperty(), worksheet.getHistory().backward().emptyProperty());
        getBindingManager().bind(forwardButton.disableProperty(), worksheet.getHistory().forward().emptyProperty());
        // Query syntax help
        var syntaxPopupRoot = new StackPane();
        syntaxPopupRoot.getStyleClass().addAll("syntax-help-popup");
        syntaxPopupRoot.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
        syntaxPopupRoot.setPrefSize(600, 740);
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
        var queryHelpPopup = new PopupControl();
        queryHelpPopup.setAutoHide(true);
        queryHelpPopup.getScene().setRoot(syntaxPopupRoot);
        WeakEventHandler<ActionEvent> syntaxHelpEventHandler = getBindingManager().registerHandler(actionEvent -> {
            Node owner = (Node) actionEvent.getSource();
            Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            queryHelpPopup.show(owner.getScene().getWindow(), bounds.getMaxX() - 600, bounds.getMaxY());
        });
        querySyntaxButton.setOnAction(syntaxHelpEventHandler);

        // init filter popup
        var suggestTree = new TreeView<StylableTreeItem>();
        StylableTreeItem.setCellFactory(suggestTree);
        suggestTree.getStyleClass().add("suggest-popup");
        var suggestRoot = new FilterableTreeItem<>(new StylableTreeItem("Filter Suggestions", PSEUDOCLASS_CATEGORY));

        var historyRoot = new FilterableTreeItem<>(new StylableTreeItem("History", PSEUDOCLASS_CATEGORY, PSEUDOCLASS_HISTORY));
        historyRoot.setGraphic(ToolButtonBuilder.makeIconNode(Pos.CENTER, "time-icon", "medium-icon", "tree-item-icon"));
        historyRoot.setExpanded(true);
        var favoritesRoot = new FilterableTreeItem<>(new StylableTreeItem("Favorites", PSEUDOCLASS_CATEGORY, PSEUDOCLASS_FAVORITES));
        favoritesRoot.setGraphic(ToolButtonBuilder.makeIconNode(Pos.CENTER, "favorite-solid-icon", "small-icon", "tree-item-icon"));
        favoritesRoot.setExpanded(true);
        suggestRoot.getInternalChildren().addAll(favoritesRoot, historyRoot);
        suggestTree.setRoot(suggestRoot);
        suggestTree.setShowRoot(false);
        var suggestPopup = new PopupControl();
        suggestPopup.setAutoHide(true);
        var suggestPane = new VBox();
        suggestPane.setSpacing(5);
        suggestPane.getStyleClass().addAll("syntax-help-popup");
        suggestPane.setPadding(new Insets(6));
        var suggestFilterField = new TextField();
        suggestFilterField.getStyleClass().add("search-field-inner");
        suggestFilterField.setPromptText("Enter a filter query or select one from the suggestions below");
        Consumer<String> commitSuggest = value -> {
            suggestPopup.hide();
            filterTextField.setText(value);
            invalidateFilter(true);
        };
        Runnable cancelSuggest = () -> {
            suggestPopup.hide();
        };
        Runnable treeSelectionCommit = () -> {
            var selected = suggestTree.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getParent() != null && !selected.getParent().equals(suggestRoot)) {
                commitSuggest.accept(selected.getValue().getLabel());
            }
        };
        suggestFilterField.setOnAction(getBindingManager().registerHandler(event ->
                commitSuggest.accept(((TextField) event.getSource()).getText())));

        suggestFilterField.addEventFilter(KeyEvent.KEY_PRESSED, getBindingManager().registerHandler(e -> {
            var key = e.getCode();
            if (key == KeyCode.DOWN) {
                suggestTree.requestFocus();
                suggestRoot.getChildren().stream().findFirst().ifPresent(n -> {
                    suggestTree.getSelectionModel().select(n);
                });
            }
        }));

        suggestTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        suggestTree.addEventFilter(KeyEvent.KEY_PRESSED, getBindingManager().registerHandler(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                treeSelectionCommit.run();
            }
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelSuggest.run();
            }
        }));
        suggestTree.setOnMouseClicked(getBindingManager().registerHandler(event -> {
            treeSelectionCommit.run();
        }));

        favoriteButton.setOnAction(getBindingManager().registerHandler(actionEvent -> {
            var favText = filterTextField.getText();
            if (favoriteLogFilters.contains(favText)) {
                favoriteLogFilters.remove(favText);
            } else {
                favoriteLogFilters.push(favText);
            }
            updateFavoriteButtonStatus(favText);
        }));
        getBindingManager().bind(favoriteButton.visibleProperty(),
                Bindings.createBooleanBinding(() -> !filterTextField.getText().isEmpty(),
                        filterTextField.textProperty()));
        getBindingManager().bind(favoriteButton.managedProperty(), favoriteButton.visibleProperty());

        var goButton = new ToolButtonBuilder<Button>(getBindingManager())
                .setText("Go")
                .setTooltip("Filter the log view")
                .setStyleClass("dialog-button")
                .setIconStyleClass("forwardArrow-icon", "small-icon")
                .setFocusTraversable(false)
                .setAction(event -> commitSuggest.accept(suggestFilterField.getText())).build(Button::new);
        var clearSuggestButton = new ToolButtonBuilder<Button>(getBindingManager())
                .setText("Clear")
                .setTooltip("Clear filter suggestions")
                .setStyleClass("dialog-button")
                .setIconStyleClass("cross-icon", "small-icon")
                .setFocusTraversable(false)
                .setAction(event -> suggestFilterField.clear()).build(Button::new);
        var suggestSyntaxButton = new ToolButtonBuilder<Button>(getBindingManager())
                .setText("help")
                .setTooltip("Display Query Syntax Help")
                .setStyleClass("dialog-button")
                .setIconStyleClass("help-icon", "small-icon")
                .setFocusTraversable(false)
                .setAction(syntaxHelpEventHandler).build(Button::new);
        getBindingManager().bind(clearSuggestButton.visibleProperty(),
                Bindings.createBooleanBinding(() -> !suggestFilterField.getText().isEmpty(),
                        suggestFilterField.textProperty()));
        getBindingManager().bind(clearSuggestButton.managedProperty(), clearSuggestButton.visibleProperty());
        var collapseButton = new ToolButtonBuilder<Button>(getBindingManager())
                .setText("Collapse")
                .setTooltip("Hide Filter Suggestions")
                .setStyleClass("dialog-button")
                .setIconStyleClass("drop-down-icon", "small-icon")
                .setFocusTraversable(false)
                .setAction(event -> {
                    cancelSuggest.run();
                }).build(Button::new);
        var hb = new HBox(suggestFilterField, goButton, clearSuggestButton, suggestSyntaxButton, collapseButton);
        HBox.setHgrow(suggestFilterField, Priority.ALWAYS);
        hb.setFillHeight(true);
        hb.setSpacing(2);
        hb.setAlignment(Pos.CENTER_LEFT);

        suggestPane.getChildren().add(hb);
        var separator = new Region();
        separator.getStyleClass().add("horizontal-separator");
        suggestPane.getChildren().add(separator);
        suggestPane.getChildren().add(suggestTree);
        getBindingManager().bind(suggestPane.prefWidthProperty(), Bindings.add(10, filterBar.widthProperty()));
        suggestPopup.getScene().setRoot(suggestPane);
        getBindingManager().attachListener(suggestPopup.showingProperty(),
                (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                    if (newValue) {
                        suggestFilterField.clear();
                        suggestTree.getSelectionModel().clearSelection();
                        historyRoot.getInternalChildren().setAll(
                                mostRecentLogFilters.getAll()
                                        .stream()
                                        .map(s -> new FilterableTreeItem<>(new StylableTreeItem(s, PSEUDOCLASS_HISTORY)))
                                        .collect(Collectors.toList()));
                        favoritesRoot.getInternalChildren().setAll(
                                favoriteLogFilters.getAll()
                                        .stream()
                                        .sorted()
                                        .map(s -> new FilterableTreeItem<>(new StylableTreeItem(s, PSEUDOCLASS_FAVORITES)))
                                        .collect(Collectors.toList()));
                    }
                });
        showSuggestButton.setOnAction(getBindingManager().registerHandler(actionEvent -> {
            Node owner = filteringBar;
            Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            suggestPopup.show(owner.getScene().getWindow(), bounds.getMinX() - 7, bounds.getMinY() - 5);
            suggestFilterField.requestFocus();
        }));
        suggestPopup.setOnHiding(getBindingManager().registerHandler(windowEvent -> {
            filterTextField.requestFocus();
        }));

        getBindingManager().attachListener(suggestFilterField.textProperty(), (o -> {
            suggestRoot.setPredicate((treeItem, stylableTreeItem) -> {
                var isMatch = stylableTreeItem != null && StringUtils.contains(
                        stylableTreeItem.getLabel(),
                        suggestFilterField.getText(),
                        false);
                if (isMatch && userPrefs.expandSuggestTreeOnMatch.get()) {
                    TreeViewUtils.expandBranch(treeItem, TreeViewUtils.ExpandDirection.UP);
                }
                return isMatch;
            });
        }));

        filterTextField.setText(worksheet.getQueryParameters().getFilterQuery());
        getBindingManager().attachListener(filterTextField.textProperty(), (ChangeListener<String>) (o, oldVal, newVal) -> {
            updateFavoriteButtonStatus(newVal);
            filterApplied.setValue(false);
        });
        pager.setCurrentPageIndex(worksheet.getQueryParameters().getPage());
        getBindingManager().bind(paginationBar.managedProperty(), paginationBar.visibleProperty());
        getBindingManager().bind(paginationBar.visibleProperty(), pager.pageCountProperty().greaterThan(1));

        // Filter selection
        getBindingManager().bind(logsToolPane.managedProperty(), logsToolPane.visibleProperty());
        getBindingManager().bind(logsToolPane.visibleProperty(), filteringBar.visibleProperty().or(highlightControls.visibleProperty()));

        getBindingManager().bind(filteringBar.managedProperty(), filteringBar.visibleProperty());
        getBindingManager().bind(filteringBar.visibleProperty(), filterToggleButton.selectedProperty());
        getBindingManager().attachListener(severityListView.getSelectedPills(),
                (SetChangeListener<FacetPillsContainer.FacetPill>) l -> invalidateFilter(true));
        getBindingManager().attachListener(pager.currentPageIndexProperty(), (o, oldVal, newVal) -> invalidateFilter(false));
        filterTextField.setOnAction(getBindingManager().registerHandler(event -> invalidateFilter(true)));
        clearFilterButton.setOnAction(getBindingManager().registerHandler(actionEvent -> {
            filterTextField.clear();
            invalidateFilter(true);
        }));
        getBindingManager().bind(clearFilterButton.visibleProperty(),
                Bindings.createBooleanBinding(() -> !filterTextField.getText().isEmpty(), filterTextField.textProperty()));
        getBindingManager().bind(clearFilterButton.managedProperty(), clearFilterButton.visibleProperty());

        filterTextField.addEventFilter(KeyEvent.KEY_PRESSED, getBindingManager().registerHandler(e -> {
            var key = e.getCode();
            logger.trace(() -> "KEY_PRESSED event trapped, keycode=" + e.getCode());
            if ((key == KeyCode.K && e.isControlDown()) || key == KeyCode.UP || key == KeyCode.DOWN) {
                showSuggestButton.getOnAction().handle(new ActionEvent());
            }
            if (key == KeyCode.D && e.isControlDown()) {
                favoriteButton.fire();
            }

        }));

        applyFilterButton.setOnAction(getBindingManager().registerHandler(event -> invalidateFilter(true)));

        getBindingManager().bind(applyFilterButton.visibleProperty(), filterApplied.not());
        getBindingManager().bind(applyFilterButton.managedProperty(), applyFilterButton.visibleProperty());

        //Search bar initialization
        getBindingManager().bind(highlightControls.managedProperty(), highlightControls.visibleProperty());
        getBindingManager().bind(highlightControls.visibleProperty(), findToggleButton.selectedProperty());

        getBindingManager().bind(heatmapArea.managedProperty(), heatmapArea.visibleProperty());
        getBindingManager().bind(heatmapArea.visibleProperty(), heatmapToggleButton.selectedProperty());

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
                Bindings.createBooleanBinding(() -> !searchTextField.getText().isEmpty(), searchTextField.textProperty()));
        getBindingManager().bind(clearSearchButton.managedProperty(), clearSearchButton.visibleProperty());
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
        getBindingManager().bind(worksheet.dividerPositionProperty(), splitPane.getDividers().get(0).positionProperty());

        var eventTarget = root.getParent();
        if (eventTarget == null) {
            eventTarget = root;
        }
        eventTarget.addEventFilter(KeyEvent.KEY_RELEASED, getBindingManager().registerHandler(e -> {
            if (e.getCode() == KeyCode.K && e.isControlDown()) {
                filterToggleButton.setSelected(true);
                filterTextField.requestFocus();
            }
            if (e.getCode() == KeyCode.F && e.isControlDown()) {
                findToggleButton.setSelected(true);
                searchTextField.requestFocus();
            }
            if (e.getCode() == KeyCode.H && e.isControlDown()) {
                heatmapToggleButton.setSelected(!heatmapToggleButton.isSelected());
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

        getBindingManager().bind(progressStatus.textProperty(), Bindings.createStringBinding(() -> {
            if (progressIndicator.getProgress() < 0) {
                return "";
            } else {
                return String.format("%.0f%%", progressIndicator.getProgress() * 100);
            }

        }, progressIndicator.progressProperty()));
        getBindingManager().bind(progressIndicator.progressProperty(), worksheet.progressProperty());

        // Init heatmap
        initHeatmap();

        invalidate(false, false, false);
        super.initialize(location, resources);
    }


    private void initHeatmap() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setAutoRanging(true);
        xAxis.setAnimated(false);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setPrefHeight(AXIS_HEIGHT);
        xAxis.setMaxHeight(AXIS_HEIGHT);
        xAxis.setMinHeight(AXIS_HEIGHT);
        StableTicksAxis<Integer> yAxis = new MetricStableTicksAxis<>();
        yAxis.setAutoRanging(true);
        yAxis.setAnimated(false);
        yAxis.setTickLabelsVisible(true);
        yAxis.setTickMarkVisible(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setPrefWidth(AXIS_WIDTH);
        yAxis.setMinWidth(AXIS_WIDTH);
        yAxis.setMaxWidth(AXIS_WIDTH);

        heatmap = new BarChart<>(xAxis, yAxis);
        heatmap.setCategoryGap(0.0);
        heatmap.setBarGap(0.5);
        heatmap.setVerticalGridLinesVisible(false);
        heatmap.setHorizontalGridLinesVisible(false);
        heatmap.setCache(true);
        heatmap.setCacheHint(CacheHint.SPEED);
        heatmap.setCacheShape(true);
        heatmap.setFocusTraversable(true);
        heatmap.setLegendVisible(false);
        heatmap.setLegendSide(Side.BOTTOM);
        heatmap.setAnimated(false);
        heatmap.getStyleClass().add("heatmap-chart");
        AnchorPane.setLeftAnchor(heatmap, 0.0);
        AnchorPane.setRightAnchor(heatmap, 0.0);
        AnchorPane.setTopAnchor(heatmap, 0.0);
        AnchorPane.setBottomAnchor(heatmap, 0.0);
        heatmapArea.getChildren().add(heatmap);
        heatmapArea.setOnDragDetected(Event::consume);

        ZonedDateTimeAxis timeAxis = new ZonedDateTimeAxis(timeRangePicker.getZoneId());
        getBindingManager().bind(timeAxis.zoneIdProperty(), timeRangePicker.zoneIdProperty());
        timeAxis.setAnimated(false);
        timeAxis.setLowerBound(timeRangePicker.getTimeRange().getBeginning());
        timeAxis.setUpperBound(timeRangePicker.getTimeRange().getEnd());
        timeAxis.setAutoRanging(false);
        timeAxis.setSide(Side.BOTTOM);
        timeAxis.setTickLabelsVisible(true);
        timeAxis.setTickMarkVisible(true);
        timeAxis.setPrefHeight(AXIS_HEIGHT);
        timeAxis.setMaxHeight(AXIS_HEIGHT);
        timeAxis.setMinHeight(AXIS_HEIGHT);

        StableTicksAxis<Double> y = new MetricStableTicksAxis<>();
        y.setAutoRanging(false);
        y.setAnimated(false);
        y.setTickLabelsVisible(false);
        y.setTickMarkVisible(false);
        y.setMinorTickVisible(false);
        y.setPrefWidth(AXIS_WIDTH);
        y.setMinWidth(AXIS_WIDTH);
        y.setMaxWidth(AXIS_WIDTH);

        timeline = new LineChart<>(timeAxis, y);
        timeline.setVerticalGridLinesVisible(false);
        timeline.setHorizontalGridLinesVisible(false);

        AnchorPane.setLeftAnchor(timeline, 0.0);
        AnchorPane.setRightAnchor(timeline, 0.0);
        AnchorPane.setTopAnchor(timeline, 0.0);
        AnchorPane.setBottomAnchor(timeline, 0.0);
        heatmapArea.getChildren().add(timeline);

        LinkedHashMap<XYChart<ZonedDateTime, Double>, Function<Double, String>> map = new LinkedHashMap<>();
        map.put(timeline, Object::toString);
        var crossHair = new XYChartCrosshair<>(map, heatmapArea, TIMELINE_FORMATTER::format);
        crossHair.setDisplayFullHeightMarker(false);
        crossHair.setVerticalMarkerVisible(true);
        crossHair.setHorizontalMarkerVisible(false);

        crossHair.onSelectionDone(s -> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());
            var selection = s.get(timeline);
            var range = TimeRange.of(selection.getStartX(), selection.getEndX());
            timeRangePicker.updateSelectedRange(range);
            invalidateFilter(true);
        });
    }

    @Override
    public Worksheet<?> getWorksheet() {
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
        item.setOnAction((getBindingManager().registerHandler(event -> {
            addToCurrentWorksheet(treeView);
        })));
        return new ContextMenu(item);
    }

    private void updateFavoriteButtonStatus(String filterValue) {
        if (favoriteLogFilters.contains(filterValue)) {
            favoriteButton.getTooltip().setText("Remove from favorites");
            favoriteButton.setGraphic(ToolButtonBuilder.makeIconNode(Pos.CENTER, "favorite-solid-icon", "small-icon"));
        } else {
            favoriteButton.getTooltip().setText("Add to favorites");
            favoriteButton.setGraphic(ToolButtonBuilder.makeIconNode(Pos.CENTER, "favorite-icon", "small-icon"));
        }
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
            TreeView<SourceBinding> treeView = getParentController().getSelectedTreeView();
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
            if (sb instanceof LogFilesBinding logFilesBinding) {
                logFilesBindings.add(logFilesBinding);
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
                if (worksheet.getSeriesInfo().stream().filter(s -> s.getBinding().equals(binding)).findAny().isEmpty()) {
                    var i = TimeSeriesInfo.fromBinding(binding);
                    getBindingManager().attachListener(i.selectedProperty(), isVisibleListener);
                    getBindingManager().attachListener(i.selectedProperty(), (ChangeListener<Boolean>) (o, oldVal, newVal) -> invalidate(false, true));
                    getBindingManager().attachListener(i.displayColorProperty(), (ChangeListener<Color>) (o, oldVal, newVal) -> invalidate(false, false));
                    worksheet.getSeriesInfo().add(i);
                }
            }
        }
        this.invalidate(false, false, false);
    }

    @Override
    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
    }

    @Override
    protected void setEditChartMode(Boolean newValue) {
        if (!newValue) {
            getBindingManager().suspend(worksheet.dividerPositionProperty());
            splitPane.setDividerPositions(1.0);
            fileTablePane.setVisible(false);
            fileTablePane.setManaged(false);
            fileTablePane.setMaxHeight(0.0);
        } else {
            fileTablePane.setMaxHeight(Double.MAX_VALUE);
            fileTablePane.setManaged(true);
            fileTablePane.setVisible(true);
            splitPane.setDividerPositions(worksheet.getDividerPosition());
            getBindingManager().resume(worksheet.dividerPositionProperty());
        }
        setShowPropertiesPane(newValue);
        super.setEditChartMode(newValue);
    }

    @Override
    public void refresh() {
        invalidate(false, false, true);
    }

    private void invalidate(boolean saveToHistory, boolean resetPage) {
        invalidate(saveToHistory, resetPage, false);
    }

    private void invalidate(boolean saveToHistory, boolean resetPage, boolean requestUpdate) {
        Dialogs.runOnFXThread(() -> {
            makeFilesCss(worksheet.getSeriesInfo());
            if (resetPage) {
                worksheet.setQueryParameters(new LogQueryParameters.Builder(worksheet.getQueryParameters())
                        .setPage(0)
                        .build());
            }
            worksheet.getHistory().setHead(worksheet.getQueryParameters(), saveToHistory);
            queryLogIndex(requestUpdate);
        });
    }

    private void queryLogIndex(boolean requestUpdate) {
        try {
            AsyncTaskManager.getInstance().submit(() -> {
                        busyIndicator.setVisible(true);
                        return (SearchHitsProcessor) fetchDataFromSources(worksheet.getQueryParameters(), requestUpdate);
                    },
                    event -> {
                        getBindingManager().suspend();
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
                                        f.getStyleClass().add("facet-pill-" + userPrefs.mapSeverityStyle(f.getFacet().getLabel()));
                                        f.setSelected(worksheet.getQueryParameters().getSeverities().contains(f.getFacet().getLabel()));
                                    });
                            // Update filePath facet view
                            var fileFacetEntries = res.getFacetResults().get(LogFileIndex.PATH);
                            if (fileFacetEntries != null) {
                                this.pathFacetEntries.setValue(fileFacetEntries);
                            } else {
                                this.pathFacetEntries.setValue(Collections.emptyList());
                            }
                            // Update timestamp Range facet view
                            var timestampFacetEntries = res.getFacetResults().get(LogFileIndex.TIMESTAMP);
                            logger.trace(timestampFacetEntries.stream()
                                    .map(e -> String.format("%s: (%d)", ZonedDateTime.ofInstant(
                                            Instant.ofEpochMilli(Long.parseLong(e.getLabel())), ZoneId.systemDefault()), e.getNbOccurrences()))
                                    .collect(Collectors.joining("\n")));
                            List<XYChart.Data<String, Integer>> heatmapData = timestampFacetEntries.stream()
                                    .map(e -> new XYChart.Data<>(e.getLabel(), e.getNbOccurrences()))
                                    .toList();
                            XYChart.Series<String, Integer> heatmapSeries = new XYChart.Series<>();
                            heatmapSeries.getData().setAll(heatmapData);
                            heatmap.getData().setAll(heatmapSeries);
                            // Update timeline selection widget
                            if (timeline.getXAxis() instanceof ZonedDateTimeAxis timeAxis) {
                                timeAxis.setLowerBound(ZonedDateTime.ofInstant(
                                        Instant.ofEpochMilli(Long.parseLong(heatmapData.get(0).getXValue())),
                                        timeRangePicker.getZoneId()));
                                timeAxis.setUpperBound(ZonedDateTime.ofInstant(
                                        Instant.ofEpochMilli(Long.parseLong(heatmapData.get(heatmapData.size() - 1).getXValue())),
                                        timeRangePicker.getZoneId()));
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
                                            List.of(userPrefs.mapSeverityStyle(severity)),
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
                            getBindingManager().resume();
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
        super.close();
        if (closed.compareAndSet(false, true)) {
            timeRangePicker.dispose();
        }
    }

    @Override
    public String getView() {
        return WORKSHEET_VIEW_FXML;
    }


    private void intiLogFileTable() {
        DecimalFormatTableCellFactory<TimeSeriesInfo<SearchHit>, String> alignRightCellFactory = new DecimalFormatTableCellFactory<>();
        alignRightCellFactory.setAlignment(TextAlignment.LEFT);
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
            getBindingManager().attachListener(i.selectedProperty(), isVisibleListener);
            getBindingManager().attachListener(i.selectedProperty(), (ChangeListener<Boolean>) (o, oldVal, newVal) -> invalidate(false, true));
            getBindingManager().attachListener(i.displayColorProperty(), (ChangeListener<Color>) (o, oldVal, newVal) -> invalidate(false, false));
            // Explicitly call the listener to initialize the proper status of the checkbox
            isVisibleListener.invalidated(null);
        }

        showAllCheckBox.setOnAction(getBindingManager().registerHandler(event -> {
            ChangeListener<Boolean> r = (o, oldVal, newVal) -> invalidate(false, true);
            boolean b = ((CheckBox) event.getSource()).isSelected();
            worksheet.getSeriesInfo().forEach(s -> getBindingManager().detachAllChangeListeners(s.selectedProperty()));
            worksheet.getSeriesInfo().forEach(t -> t.setSelected(b));
            r.changed(null, null, null);
            worksheet.getSeriesInfo().forEach(s -> getBindingManager().attachListener(s.selectedProperty(), r));
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
        nameColumn.setEditable(true);
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(getBindingManager().registerHandler(
                t -> t.getTableView().getItems().get(
                        t.getTablePosition().getRow()).setDisplayName(t.getNewValue()))
        );

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
        fileTable.setOnKeyReleased(getBindingManager().registerHandler(event -> {
            if (event.getCode().equals(KeyCode.DELETE)) {
                var selected = new ArrayList<>(fileTable.getSelectionModel().getSelectedItems());
                selected.forEach(s -> {
                    getBindingManager().detachAllInvalidationListeners(s.selectedProperty());
                    getBindingManager().detachAllInvalidationListeners(s.displayColorProperty());
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
            String filter = filterTextField.getText();
            if (!filter.isBlank()) {
                mostRecentLogFilters.push(filter);
            }
            filterApplied.setValue(true);
        }
    }

    private void restoreQueryParameters(LogQueryParameters newParams, boolean resetPage) {
        if (newParams != worksheet.getQueryParameters()) {
            getBindingManager().suspend();
            try {
                worksheet.setQueryParameters(newParams);
                timeRangePicker.updateSelectedRange(newParams.getTimeRange());
                filterTextField.setText(newParams.getFilterQuery());
                pager.setCurrentPageIndex(newParams.getPage());
            } finally {
                getBindingManager().resume();
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

    private TimeSeriesProcessor<SearchHit> fetchDataFromSources(LogQueryParameters filter, boolean requestUpdate) throws DataAdapterException {
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
            if (byAdapterEntry.getKey() instanceof ProgressAdapter<SearchHit> adapter) {
                // Group all queries with the same adapter and path
                var bindingsByPath =
                        byAdapterEntry.getValue().stream().collect(groupingBy(o -> o.getBinding().getPath()));
                var data = adapter.fetchData(queryArgs, start, end,
                        bindingsByPath.values().stream()
                                .flatMap(Collection::stream)
                                .filter(TimeSeriesInfo::isSelected)
                                .collect(Collectors.toList()), requestUpdate, worksheet.progressProperty());
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
