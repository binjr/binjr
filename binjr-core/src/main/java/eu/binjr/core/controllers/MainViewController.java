/*
 *    Copyright 2016-2022 Frederic Thevenet
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

import eu.binjr.common.function.CheckedLambdas;
import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.common.javafx.controls.*;
import eu.binjr.common.logging.Logger;
import eu.binjr.common.text.StringUtils;
import eu.binjr.core.appearance.StageAppearanceManager;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.CannotLoadWorksheetException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.workspace.*;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserHistory;
import eu.binjr.core.preferences.UserPreferences;
import eu.binjr.core.update.UpdateManager;
import jakarta.xml.bind.JAXBException;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;
import org.controlsfx.control.MaskerPane;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;

/**
 * The controller class for the main view
 *
 * @author Frederic Thevenet
 */
public class MainViewController implements Initializable {
    static final int SETTINGS_PANE_DISTANCE = 250;
    private static final Logger logger = Logger.create(MainViewController.class);
    private static final String[] BINJR_FILE_PATTERN = new String[]{"*.bjr", "*.xml"};
    private static final double SEARCH_BAR_PANE_DISTANCE = 40;
    private static final PseudoClass HOVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("hover");
    private static final DataFormat GENERIC_BINDING_FORMAT = new DataFormat(SourceBinding.MIME_TYPE);
    private static final DataFormat TIME_SERIES_BINDING_FORMAT = new DataFormat(TimeSeriesBinding.MIME_TYPE);
    private static final DataFormat TEXT_FILES_BINDING_FORMAT = new DataFormat(TextFilesBinding.MIME_TYPE);
    private static final DataFormat LOG_FILES_BINDING_FORMAT = new DataFormat(LogFilesBinding.MIME_TYPE);

    private final Map<EditableTab, WorksheetController> seriesControllers = new WeakHashMap<>();
    private final Map<TitledPane, Source> sourcesAdapters = new WeakHashMap<>();
    private final BooleanProperty searchBarVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty searchBarHidden = new SimpleBooleanProperty(!searchBarVisible.get());
    private final BooleanProperty treeItemDragAndDropInProgress = new SimpleBooleanProperty(false);
    private BooleanBinding noWorksheetPresent;
    private BooleanBinding noSourcePresent;

    @FXML
    private MenuItem restoreClosedWorksheetMenu;
    @FXML
    private AnchorPane sourcePane;
    @FXML
    private MenuItem hideSourcePaneMenu;
    @FXML
    private StackPane newWorksheetDropTarget;
    @FXML
    private DrawerPane commandBar;
    @FXML
    private AnchorPane root;
    @FXML
    private Label addWorksheetLabel;
    @FXML
    private MaskerPane sourceMaskerPane;
    @FXML
    private MaskerPane worksheetMaskerPane;
    @FXML
    private Pane searchBarRoot;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;
    @FXML
    private Button hideSearchBarButton;
    @FXML
    private ToggleButton searchCaseSensitiveToggle;
    @FXML
    private StackPane sourceArea;
    List<TreeItem<SourceBinding>> searchResultSet;
    int currentSearchHit = -1;
    private final Workspace workspace;
    @FXML
    private MenuButton worksheetMenu;
    private Optional<String> associatedFile = Optional.empty();
    @FXML
    private Accordion sourcesPane;
    @FXML
    private TearableTabPane tearableTabPane;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private Menu openRecentMenu;
    @FXML
    private SplitPane contentView;
    @FXML
    private StackPane settingsPane;
    @FXML
    private StackPane worksheetArea;
    @FXML
    private Menu addSourceMenu;
    @FXML
    private StackPane curtains;
    @FXML
    private Label addSourceLabel;

    /**
     * Initializes a new instance of the {@link MainViewController} class.
     */
    public MainViewController() {
        super();
        this.workspace = new Workspace();
    }

    public static Optional<BindingsHierarchy[]> treeItemsAsChartList(Collection<TreeItem<SourceBinding>> treeItems, Node dlgRoot) {
        var charts = new ArrayList<BindingsHierarchy>();
        var totalBindings = 0;
        for (var treeItem : treeItems) {
            for (var t : TreeViewUtils.splitAboveLeaves(treeItem, true)) {
                var chart = new BindingsHierarchy();
                var binding = t.getValue();
                chart.setName(binding);
                for (var b : TreeViewUtils.flattenLeaves(t)) {
                    chart.getBindings().add(b);
                    totalBindings++;
                }
                charts.add(chart);
            }
        }
        if (totalBindings >= UserPreferences.getInstance().maxSeriesPerChartBeforeWarning.get().intValue()) {
            if (Dialogs.confirmDialog(dlgRoot,
                    "This action will add " + totalBindings + " series on a single worksheet.",
                    "Are you sure you want to proceed?"
            ) != ButtonType.YES) {
                return Optional.empty();
            }
        }
        return Optional.of(charts.toArray(BindingsHierarchy[]::new));
    }

    @FXML
    private void worksheetAreaOnDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(GENERIC_BINDING_FORMAT) ||
                db.hasContent(TIME_SERIES_BINDING_FORMAT) ||
                db.hasContent(TEXT_FILES_BINDING_FORMAT) ||
                db.hasContent(LOG_FILES_BINDING_FORMAT)) {
            event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert root != null : "fx:id\"root\" was not injected!";
        assert tearableTabPane != null : "fx:id\"tearableTabPane\" was not injected!";
        assert sourcesPane != null : "fx:id\"sourceTabPane\" was not injected!";
        assert saveMenuItem != null : "fx:id\"saveMenuItem\" was not injected!";
        assert openRecentMenu != null : "fx:id\"openRecentMenu\" was not injected!";
        assert contentView != null : "fx:id\"contentView\" was not injected!";
        noWorksheetPresent = Bindings.size(tearableTabPane.getTabs()).isEqualTo(0);
        noSourcePresent = Bindings.size(sourcesPane.getPanes()).isEqualTo(0);

        contentView.getDividers().stream().findFirst().ifPresent(divider -> {
            divider.setPosition(getWorkspace().getDividerPosition());
            getWorkspace().getBindingManager().bind(getWorkspace().dividerPositionProperty(), divider.positionProperty());
        });
        sourcesPane.mouseTransparentProperty().bind(noSourcePresent);
        addSourceLabel.visibleProperty().bind(noSourcePresent);
        workspace.sourcePaneVisibleProperty().addListener((observable, oldValue, newValue) -> toggleSourcePaneVisibilty(newValue));
        workspace.presentationModeProperty().addListener((observable, oldValue, newValue) -> {
            for (var w : workspace.getWorksheets()) {
                w.setEditModeEnabled(!newValue);
            }
            workspace.setSourcePaneVisible(!newValue);
        });
        for (var w : workspace.getWorksheets()) {
            w.setEditModeEnabled(!workspace.isPresentationMode());
        }
        workspace.setSourcePaneVisible(!workspace.isPresentationMode());
        toggleSourcePaneVisibilty(workspace.isSourcePaneVisible());
        sourcesPane.expandedPaneProperty().addListener(
                (ObservableValue<? extends TitledPane> observable, TitledPane oldPane, TitledPane newPane) -> {
                    if (UserPreferences.getInstance().preventFoldingAllSourcePanes.get()) {
                        boolean expandRequiered = true;
                        for (TitledPane pane : sourcesPane.getPanes()) {
                            if (pane.isExpanded()) {
                                expandRequiered = false;
                            }
                        }
                        if ((expandRequiered) && (oldPane != null)) {
                            Platform.runLater(() -> sourcesPane.setExpandedPane(oldPane));
                        }
                    }
                });
        addWorksheetLabel.visibleProperty().bind(noWorksheetPresent);
        tearableTabPane.setDetachedStageStyle(AppEnvironment.getInstance().getWindowsStyle());
        tearableTabPane.setNewTabFactory(this::worksheetTabFactory);
        tearableTabPane.getGlobalTabs().addListener((ListChangeListener<? super Tab>) this::onWorksheetTabChanged);
        tearableTabPane.setTearable(true);
        tearableTabPane.setOnOpenNewWindow(event -> {
            Stage stage = (Stage) event.getSource();
            stage.setTitle(AppEnvironment.APP_NAME);
            registerStageKeyEvents(stage);

            StackPane dropZone = new StackPane(ToolButtonBuilder.makeIconNode(Pos.CENTER, "new-tab-icon"));
            dropZone.getStyleClass().add("drop-zone");
            dropZone.setOnDragDropped(this::handleDragDroppedOnWorksheetArea);
            dropZone.setOnDragOver(this::worksheetAreaOnDragOver);
            dropZone.setOnDragExited(this::handleOnDragExitedNewWorksheet);
            dropZone.setOnDragEntered(this::handleOnDragEnteredNewWorksheet);
            var newPaneDropZone = new StackPane(dropZone);
            newPaneDropZone.getStyleClass().add("chart-viewport-parent");
            AnchorPane.setTopAnchor(newPaneDropZone, 0.0);
            AnchorPane.setLeftAnchor(newPaneDropZone, 0.0);
            AnchorPane.setRightAnchor(newPaneDropZone, 0.0);
            newPaneDropZone.setPrefHeight(34);
            newPaneDropZone.setMaxHeight(34);
            newPaneDropZone.managedProperty().bind(treeItemDragAndDropInProgressProperty());
            newPaneDropZone.visibleProperty().bind(treeItemDragAndDropInProgressProperty());
            ((Pane) stage.getScene().getRoot()).getChildren().add(newPaneDropZone);
            StageAppearanceManager.getInstance().register(stage);
        });
        tearableTabPane.setOnClosingWindow(event -> {
            StageAppearanceManager.getInstance().unregister((Stage) event.getSource());
            unregisterStageKeyEvents((Stage) event.getSource());
        });
        sourcesPane.getPanes().addListener(this::onSourceTabChanged);
        sourcesPane.addEventFilter(KeyEvent.KEY_PRESSED, (e -> {
            if (e.getCode() == KeyCode.F && e.isControlDown()) {
                handleShowSearchBar(null);
            }
        }));
        saveMenuItem.disableProperty().bind(workspace.dirtyProperty().not());
        commandBar.setSibling(contentView);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                invalidateSearchResults();
                findNext();
            }
        });
        searchCaseSensitiveToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            invalidateSearchResults();
            findNext();
        });
        searchBarVisible.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                searchField.requestFocus();
                if (searchBarHidden.getValue()) {
                    slidePanel(1, Duration.millis(0));
                    searchBarHidden.setValue(false);
                }
            } else {
                if (!searchBarHidden.getValue()) {
                    slidePanel(-1, Duration.millis(0));
                    searchBarHidden.setValue(true);
                }
            }
        });
        this.addSourceMenu.getItems().addAll(populateSourceMenu());
        this.addSourceMenu.setOnShowing(event -> addSourceMenu.getItems().setAll(populateSourceMenu()));
        newWorksheetDropTarget.managedProperty()
                .bind(tearableTabPane.emptyProperty().not().and(treeItemDragAndDropInProgressProperty()));
        newWorksheetDropTarget.visibleProperty()
                .bind(tearableTabPane.emptyProperty().not().and(treeItemDragAndDropInProgressProperty()));
        this.restoreClosedWorksheetMenu.disableProperty().bind(workspace.closedWorksheetQueueEmptyProperty());
        Platform.runLater(this::runAfterInitialize);
    }

    protected void runAfterInitialize() {
        UserPreferences userPrefs = UserPreferences.getInstance();
        Stage stage = Dialogs.getStage(root);
        stage.titleProperty().bind(Bindings.createStringBinding(
                () -> String.format("%s%s - %s",
                        (workspace.isDirty() ? "*" : ""),
                        workspace.pathProperty().getValue().toString(),
                        AppEnvironment.APP_NAME
                ),
                workspace.pathProperty(),
                workspace.dirtyProperty())
        );
        stage.setOnCloseRequest(event -> {
            if (!confirmAndClearWorkspace()) {
                event.consume();
            } else {
                saveWindowPositionAndQuit();
            }
        });
        registerStageKeyEvents(stage);
        if (associatedFile.isPresent()) {
            logger.debug(() -> "Opening associated file " + associatedFile.get());
            loadWorkspace(new File(associatedFile.get()));
        } else if (userPrefs.loadLastWorkspaceOnStartup.get()) {
            UserHistory.getInstance().mostRecentWorkspaces.peek().ifPresent(latestWorkspacePath -> {
                File latestWorkspace = latestWorkspacePath.toFile();
                if (latestWorkspace.exists()) {
                    loadWorkspace(latestWorkspace);
                } else {
                    logger.warn("Cannot reopen workspace " + latestWorkspace.getPath() + ": file does not exists");
                }
            });
        }
        if (userPrefs.checkForUpdateOnStartUp.get()) {
            UpdateManager.getInstance().asyncCheckForUpdate(
                    release -> UpdateManager.getInstance().showUpdateAvailableNotification(release, root), null, null
            );
        }
    }

    private void registerStageKeyEvents(Stage stage) {
        BindingManager manager = new BindingManager();
        stage.setUserData(manager);
        stage.addEventFilter(KeyEvent.KEY_PRESSED, manager.registerHandler(e -> {
            logger.trace(() -> "KEY_PRESSED event trapped, keycode=" + e.getCode());
            if (e.getCode() == KeyCode.F12) {
                AppEnvironment.getInstance().setDebugMode(!AppEnvironment.getInstance().isDebugMode());
            }
            if (e.getCode() == KeyCode.R && e.isControlDown()) {
                getSelectedWorksheetController().ifPresent(WorksheetController::refresh);
            }
            if (e.getCode() == KeyCode.F5) {
                getSelectedWorksheetController().ifPresent(w -> w.refresh(e.isControlDown()));
            }
            if (e.getCode() == KeyCode.M && e.isControlDown()) {
                handleTogglePresentationMode();
            }
            if (e.getCode() == KeyCode.P && e.isControlDown()) {
                getSelectedWorksheetController().ifPresent(WorksheetController::saveSnapshot);
            }
            if (e.getCode() == KeyCode.T && e.isControlDown() && !e.isShiftDown()) {
                editWorksheet(new XYChartsWorksheet());
            }
            if (e.isControlDown() && (e.getCode() == KeyCode.W || e.getCode() == KeyCode.F4)) {
                closeWorksheetTab((EditableTab) tearableTabPane.getSelectedTab());
            }
            if (e.getCode() == KeyCode.LEFT && e.isAltDown()) {
                getSelectedWorksheetController().ifPresent(WorksheetController::navigateBackward);
            }
            if (e.getCode() == KeyCode.RIGHT && e.isAltDown()) {
                getSelectedWorksheetController().ifPresent(WorksheetController::navigateForward);
            }
            if (e.getCode() == KeyCode.T && e.isShiftDown() && e.isControlDown()) {
                restoreLatestClosedWorksheet();
            }
        }));
        stage.addEventFilter(KeyEvent.KEY_PRESSED, manager.registerHandler(e -> handleControlKey(e, true)));
        stage.addEventFilter(KeyEvent.KEY_RELEASED, manager.registerHandler(e -> handleControlKey(e, false)));
        manager.attachListener(stage.focusedProperty(), (observable, oldValue, newValue) -> {
            //main stage lost focus -> invalidates shift or ctrl pressed
            AppEnvironment.getInstance().setShiftPressed(false);
            AppEnvironment.getInstance().setCtrlPressed(false);
        });
    }

    private void unregisterStageKeyEvents(Stage stage) {
        BindingManager manager = (BindingManager) stage.getUserData();
        if (manager != null) {
            manager.close();
        }
    }

    //region UI handlers
    @FXML
    protected void handleAboutAction(ActionEvent event) throws IOException {
        Dialog<String> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setTitle("About " + AppEnvironment.APP_NAME);
        dialog.setDialogPane(FXMLLoader.load(getResourceUrl("/eu/binjr/views/AboutBoxView.fxml")));
        dialog.initOwner(Dialogs.getStage(root));
        dialog.getDialogPane().getStylesheets().add(getResourceUrl(StageAppearanceManager.getFontFamilyCssPath()).toExternalForm());
        dialog.showAndWait();
    }

    private URL getResourceUrl(String path) throws IOException {
        Objects.requireNonNull(path);
        var url = getClass().getResource(path);
        if (url == null) {
            throw new IOException("Failed to load resource from: " + path);
        }
        return url;
    }

    @FXML
    protected void handleQuitAction(ActionEvent event) {
        if (confirmAndClearWorkspace()) {
            saveWindowPositionAndQuit();
        }
    }

    @FXML
    protected void handlePreferencesAction(ActionEvent actionEvent) {
        try {
            TranslateTransition openNav = new TranslateTransition(new Duration(350), settingsPane);
            openNav.setToX(SETTINGS_PANE_DISTANCE);
            openNav.play();
            commandBar.expand();
        } catch (Exception ex) {
            Dialogs.notifyException("Failed to display preference dialog", ex, root);
        }
    }

    @FXML
    protected void handleExpandCommandBar(ActionEvent actionEvent) {
        commandBar.toggle();
    }

    @FXML
    protected void handleAddNewWorksheet(Event event) {
        editWorksheet(new XYChartsWorksheet());
    }

    @FXML
    protected void handleAddSource(Event event) {
        Node sourceNode = (Node) event.getSource();
        ContextMenu sourceMenu = new ContextMenu();
        sourceMenu.getItems().addAll(populateSourceMenu());
        sourceMenu.show(sourceNode, Side.BOTTOM, 0, 0);
    }

    @FXML
    protected void handleHelpAction(ActionEvent event) {
        openUrlInBrowser(AppEnvironment.HTTP_BINJR_WIKI);
    }

    @FXML
    private void handleShortcutsAction(ActionEvent actionEvent) {
        openUrlInBrowser(AppEnvironment.HTTP_BINJR_SHORTCUTS);
    }

    @FXML
    protected void handleViewOnGitHub(ActionEvent event) {
        openUrlInBrowser(AppEnvironment.HTTP_GITHUB_REPO);
    }

    @FXML
    protected void handleBinjrWebsite(ActionEvent actionEvent) {
        openUrlInBrowser(AppEnvironment.HTTP_WWW_BINJR_EU);
    }

    private void openUrlInBrowser(String url) {
        try {
            Dialogs.launchUrlInExternalBrowser(url);
        } catch (IOException | URISyntaxException e) {
            logger.error("Failed to launch url in browser: " + url);
            logger.debug("Exception stack", e);
        }
    }

    @FXML
    protected void handleNewWorkspace(ActionEvent event) {
        confirmAndClearWorkspace();
    }

    @FXML
    protected void handleOpenWorkspace(ActionEvent event) {
        openWorkspaceFromFile();
    }

    @FXML
    protected void handleShowSearchBar(ActionEvent actionEvent) {
        this.searchBarVisible.setValue(true);
    }

    @FXML
    protected void handleHidePanel(ActionEvent actionEvent) {
        this.searchField.clear();
        this.searchBarVisible.setValue(false);
    }

    @FXML
    protected void handleFindNextInTreeView(ActionEvent actionEvent) {
        findNext();
    }

    @FXML
    protected void handleSaveWorkspace(ActionEvent event) {
        saveWorkspace();
    }

    @FXML
    protected void handleSaveAsWorkspace(ActionEvent event) {
        saveWorkspaceAs();
    }

    //endregion

    @FXML
    protected void handleDisplayChartProperties(ActionEvent actionEvent) {
        getSelectedWorksheetController().ifPresent(WorksheetController::toggleShowPropertiesPane);
    }

    @FXML
    protected void populateOpenRecentMenu(Event event) {
        Menu menu = (Menu) event.getSource();
        Collection<Path> recentPath = UserHistory.getInstance().mostRecentWorkspaces.getAll();
        if (!recentPath.isEmpty()) {
            menu.getItems().setAll(recentPath.stream().map(s -> {
                MenuItem m = new MenuItem(s.toString());
                m.setMnemonicParsing(false);
                m.setOnAction(e -> loadWorkspace(new File(((MenuItem) e.getSource()).getText())));
                return m;
            }).collect(Collectors.toList()));
        } else {
            MenuItem none = new MenuItem("none");
            none.setDisable(true);
            menu.getItems().setAll(none);
        }
    }

    private TitledPane newSourcePane(Source source) {
        TitledPane newPane = new TitledPane();
        Label label = new Label();
        source.getBindingManager().bind(label.textProperty(), source.nameProperty());
        GridPane titleRegion = new GridPane();
        titleRegion.setHgap(5);
        titleRegion.getColumnConstraints().add(
                new ColumnConstraints(65, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.LEFT, true));
        titleRegion.getColumnConstraints().add(
                new ColumnConstraints(65, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.NEVER, HPos.RIGHT, false));
        source.getBindingManager().bind(titleRegion.minWidthProperty(), newPane.widthProperty().subtract(30));
        source.getBindingManager().bind(titleRegion.maxWidthProperty(), newPane.widthProperty().subtract(30));

        // *** Toolbar ***
        HBox toolbar = new HBox();
        toolbar.getStyleClass().add("title-pane-tool-bar");
        toolbar.setAlignment(Pos.CENTER);

        Button closeButton = new ToolButtonBuilder<Button>()
                .setText("Close")
                .setTooltip("Close the connection to this source.")
                .setStyleClass("exit")
                .setIconStyleClass("cross-icon", "small-icon")
                .setAction(event -> {
                    if (Dialogs.confirmDialog(root, "Are you sure you want to remove source \"" + source.getName() + "\"?",
                            "WARNING: This will remove all associated series from existing worksheets."
                    ) == ButtonType.YES) {
                        sourcesPane.getPanes().remove(newPane);
                    }
                }).build(Button::new);
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        GridPane.setConstraints(label, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER);
        GridPane.setConstraints(toolbar, 1, 0, 1, 1, HPos.RIGHT, VPos.CENTER);
        newPane.setGraphic(titleRegion);
        newPane.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        newPane.setAnimated(false);
        source.getBindingManager().attachListener(newPane.expandedProperty(),
                (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                    if (!newValue) {
                        source.setEditable(false);
                    }
                });

        HBox editFieldsGroup = new HBox();
        DoubleBinding db = Bindings.createDoubleBinding(
                () -> editFieldsGroup.isVisible() ? USE_COMPUTED_SIZE : 0.0, editFieldsGroup.visibleProperty());
        source.getBindingManager().bind(editFieldsGroup.prefHeightProperty(), db);
        source.getBindingManager().bind(editFieldsGroup.maxHeightProperty(), db);
        source.getBindingManager().bind(editFieldsGroup.minHeightProperty(), db);
        source.getBindingManager().bindBidirectional(editFieldsGroup.visibleProperty(), source.editableProperty());
        editFieldsGroup.setSpacing(5);
        TextField sourceNameField = new TextField();
        source.getBindingManager().bindBidirectional(sourceNameField.textProperty(), source.nameProperty());
        editFieldsGroup.getChildren().add(sourceNameField);

        sourceNameField.setOnAction(source.getBindingManager().registerHandler(event -> source.setEditable(false)));
        source.getBindingManager().attachListener(sourceNameField.focusedProperty(),
                (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                    if (!newValue) {
                        source.setEditable(false);
                    }
                });


        ToggleButton editButton = new ToolButtonBuilder<ToggleButton>(source.getBindingManager())
                .setText("Settings")
                .setTooltip("Edit the source's settings")
                .setStyleClass("dialog-button")
                .setIconStyleClass("settings-icon", "small-icon")
                .setAction(event -> newPane.setExpanded(true))
                .bindBidirectionnal(ToggleButton::selectedProperty, source.editableProperty())
                .build(ToggleButton::new);
        ToggleButton filterButton = new ToolButtonBuilder<ToggleButton>(source.getBindingManager())
                .setText("Filter")
                .setTooltip("Filter the source tree")
                .setStyleClass("dialog-button")
                .setIconStyleClass("filter-icon", "small-icon")
                .setAction(event -> newPane.setExpanded(true))
                .bindBidirectionnal(ToggleButton::selectedProperty, source.filterableProperty())
                .build(ToggleButton::new);
        HBox.setHgrow(sourceNameField, Priority.ALWAYS);
        toolbar.getChildren().addAll(filterButton, editButton, closeButton);
        titleRegion.getChildren().addAll(label, editFieldsGroup, toolbar);

        newPane.setOnMouseClicked(source.getBindingManager().registerHandler(event -> {
            if (event.getClickCount() == 2) {
                source.setEditable(true);
                sourceNameField.selectAll();
                sourceNameField.requestFocus();
            }
            event.consume();
        }));
        return newPane;
    }

    //region private members
    private Collection<MenuItem> populateSourceMenu() {
        List<MenuItem> menuItems = new ArrayList<>();
        for (DataAdapterInfo adapterInfo : DataAdapterFactory.getInstance().getActiveAdapters()) {
            MenuItem menuItem = new MenuItem(adapterInfo.getName());
            menuItem.setOnAction(eventHandler -> {
                try {
                    if (adapterInfo.getAdapterDialog() != null) {
                        DataAdapterFactory.getInstance()
                                .getDialog(adapterInfo.getKey(), root)
                                .showAndWait()
                                .ifPresent(dataAdapters -> dataAdapters.forEach(this::addSource));
                    } else {
                        addSource(DataAdapterFactory.getInstance().newAdapter(adapterInfo.getKey()));
                    }
                } catch (NoAdapterFoundException e) {
                    Dialogs.notifyException("Could not find source adapter " + adapterInfo.getName(), e, root);
                } catch (CannotInitializeDataAdapterException e) {
                    Dialogs.notifyException("Could not initialize source adapter " + adapterInfo.getName(), e, root);
                }
            });
            menuItems.add(menuItem);
        }
        var none = new MenuItem("None");
        none.setDisable(true);
        return menuItems.size() > 0 ? menuItems : Collections.singletonList(none);
    }

    TreeView<SourceBinding> getSelectedTreeView() {
        if (sourcesPane == null || sourcesPane.getExpandedPane() == null) {
            return null;
        }
        var treeView = sourcesPane.getExpandedPane().getContent().lookup("#sourceTreeView");
        return (TreeView<SourceBinding>) treeView;
    }

    private void slidePanel(int show, Duration delay) {
        TranslateTransition openNav = new TranslateTransition(new Duration(200), searchBarRoot);
        openNav.setDelay(delay);
        openNav.setToY(show * -SEARCH_BAR_PANE_DISTANCE);
        openNav.play();
        openNav.setOnFinished(event -> AnchorPane.setBottomAnchor(sourceArea, show > 0 ? SEARCH_BAR_PANE_DISTANCE : 0));
    }

    private boolean confirmAndClearWorkspace() {
        if (!workspace.isDirty()) {
            closeWorkspace();
            return true;
        }
        // Make sure that main stage is visible before invoking modal dialog, else modal dialog may appear
        // behind main stage when made visible again.
        Dialogs.getStage(root).setIconified(false);
        ButtonType res = Dialogs.confirmSaveDialog(root,
                (workspace.hasPath() ? workspace.getPath().getFileName().toString() : "Untitled"));
        if (res == ButtonType.CANCEL) {
            return false;
        }
        if (res == ButtonType.YES && !saveWorkspace()) {
            return false;
        }
        closeWorkspace();
        return true;
    }

    private void closeWorkspace() {
        logger.debug(() -> "Clearing workspace");
        tearableTabPane.clearAllTabs();
        sourcesPane.getPanes().clear();
        seriesControllers.clear();
        sourcesAdapters.values().forEach(source -> {
            try {
                source.close();
            } catch (Exception e) {
                Dialogs.notifyException("Error closing Source", e, root);
            }
        });
        sourcesAdapters.clear();
        workspace.clear();
    }

    private void openWorkspaceFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Workspace");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("binjr workspaces", BINJR_FILE_PATTERN));
        Dialogs.getInitialDir(UserHistory.getInstance().mostRecentWorkspaces).ifPresent(fileChooser::setInitialDirectory);
        File selectedFile = fileChooser.showOpenDialog(Dialogs.getStage(root));
        if (selectedFile != null) {
            loadWorkspace(selectedFile);
        }
    }

    private void loadWorkspace(File file) {
        if (confirmAndClearWorkspace()) {
            sourceMaskerPane.setVisible(true);
            AsyncTaskManager.getInstance().submit(() -> {
                        Workspace wsFromfile = Workspace.from(file);
                        for (Source source : wsFromfile.getSources()) {
                            DataAdapter<?> da = DataAdapterFactory.getInstance().newAdapter(source.getAdapterClassName());
                            da.loadParams(source.getAdapterParams());
                            da.setId(source.getAdapterId());
                            source.setAdapter(da);
                            loadSource(source);
                        }
                        return wsFromfile;
                    },
                    event -> {
                        Workspace loadedWorkspace = (Workspace) event.getSource().getValue();
                        workspace.setPath(file.toPath());
                        contentView.getDividers().stream().findFirst().ifPresent(d -> d.setPosition(loadedWorkspace.getDividerPosition()));
                        sourceMaskerPane.setVisible(false);
                        loadWorksheets(loadedWorkspace);
                    }, event -> {
                        sourceMaskerPane.setVisible(false);
                        Dialogs.notifyException("An error occurred while loading workspace from file " +
                                        (file != null ? file.getName() : "null"),
                                event.getSource().getException(),
                                root);
                    });
        }
    }

    private void restoreLatestClosedWorksheet() {
        this.workspace.pollClosedWorksheet().ifPresent(w -> {
            try {
                loadWorksheet(w);
            } catch (CannotLoadWorksheetException e) {
                logger.warn("Could not restore worksheet: " + e.getMessage());
                logger.debug(() -> "Could not restore worksheet", e);
            }
        });
    }

    private void loadWorksheets(Workspace wsFromfile) {
        try {
            for (var worksheet : wsFromfile.getWorksheets()) {
                try {
                    loadWorksheet(worksheet);
                } catch (CannotLoadWorksheetException e) {
                    Dialogs.notifyException(e);
                }
            }
            workspace.cleanUp();
            UserHistory.getInstance().mostRecentWorkspaces.push(workspace.getPath());
            logger.debug(() -> "Recently loaded workspaces: " +
                    UserHistory.getInstance().mostRecentWorkspaces.getAll()
                            .stream()
                            .map(Path::toString)
                            .collect(Collectors.joining(" ")));

        } catch (Exception e) {
            Dialogs.notifyException("Error loading workspace", e, root);
        }
    }

    private boolean saveWorkspace() {
        try {
            if (workspace.hasPath()) {
                workspace.save();
                return true;
            } else {
                return saveWorkspaceAs();
            }
        } catch (IOException e) {
            Dialogs.notifyException("Failed to save snapshot to disk", e, root);
        } catch (JAXBException e) {
            Dialogs.notifyException("Error while serializing workspace", e, root);
        }
        return false;
    }

    private boolean saveWorkspaceAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Workspace");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("binjr workspaces", BINJR_FILE_PATTERN));
        Dialogs.getInitialDir(UserHistory.getInstance().mostRecentWorkspaces).ifPresent(fileChooser::setInitialDirectory);
        fileChooser.setInitialFileName(BINJR_FILE_PATTERN[0]);
        File selectedFile = fileChooser.showSaveDialog(Dialogs.getStage(root));
        if (selectedFile != null) {
            try {
                workspace.save(selectedFile);
                UserHistory.getInstance().mostRecentWorkspaces.push(workspace.getPath());
                return true;
            } catch (IOException e) {
                Dialogs.notifyException("Failed to save snapshot to disk", e, root);
            } catch (JAXBException e) {
                Dialogs.notifyException("Error while serializing workspace", e, root);
            }
        }
        return false;
    }

    private void addSource(DataAdapter<?> da) {
        Source newSource = Source.of(da);
        TitledPane newSourcePane = newSourcePane(newSource);
        newSourcePane.setContent(new MaskerPane());
        sourcesAdapters.put(newSourcePane, newSource);
        sourcesPane.getPanes().add(newSourcePane);
        newSourcePane.setExpanded(true);

        workspace.setPresentationMode(false);
        AsyncTaskManager.getInstance().submit(() -> buildTreeViewForTarget(newSource.getAdapter()),
                event -> {
                    Optional<TreeView<SourceBinding>> treeView =
                            (Optional<TreeView<SourceBinding>>) event.getSource().getValue();
                    if (treeView.isPresent()) {
                        newSourcePane.setContent(buildSourcePaneContent(treeView.get(), newSource));
                    } else {
                        newSourcePane.setContent(failedLoadingSource("Error connecting to " + newSource.getName()));
                    }
                },
                event -> {
                    newSourcePane.setContent(failedLoadingSource("Error connecting to " + newSource.getName()));
                    Dialogs.notifyException("Unexpected error getting data adapter:",
                            event.getSource().getException(),
                            root);
                });
    }

    private void loadSource(Source source) throws DataAdapterException {
        TitledPane newSourcePane = newSourcePane(source);
        buildTreeViewForTarget(source.getAdapter()).ifPresentOrElse(
                t -> newSourcePane.setContent(buildSourcePaneContent(t, source)),
                () -> newSourcePane.setContent(failedLoadingSource("Error connecting to " + source.getName())));
        sourcesAdapters.put(newSourcePane, source);
        Platform.runLater(() -> {
            sourcesPane.getPanes().add(newSourcePane);
            newSourcePane.setExpanded(true);
        });
    }

    private Label failedLoadingSource(String message) {
        Label errorLabel = new Label(message);
        var icon = new Region();
        icon.getStyleClass().addAll("warning-icon", "large-icon", "warning-label-icon");
        errorLabel.setGraphicTextGap(30);
        errorLabel.setContentDisplay(ContentDisplay.TOP);
        errorLabel.setWrapText(true);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setGraphic(icon);
        errorLabel.getStyleClass().add("warning-label");
        return errorLabel;
    }

    private Node buildSourcePaneContent(TreeView<SourceBinding> treeView, Source source) {
        TextField filterField = new TextField();
        filterField.setPromptText("Type in text to filter the tree view.");
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterField.setMaxWidth(Double.MAX_VALUE);
        filterField.getStyleClass().add("search-field-inner");
        var clearFilterbutton = new ToolButtonBuilder<Button>(source.getBindingManager())
                .setText("Clear")
                .setTooltip("Clear filter")
                .setStyleClass("dialog-button")
                .setIconStyleClass("cross-icon", "small-icon")
                .setAction(event -> filterField.clear())
                .bind(Node::visibleProperty,
                        Bindings.createBooleanBinding(() -> !filterField.getText().isBlank(), filterField.textProperty()))
                .build(Button::new);
        var filterCaseSensitiveToggle = new ToolButtonBuilder<ToggleButton>(source.getBindingManager())
                .setText("Aa")
                .setStyleClass("dialog-button")
                .setIconStyleClass("match-case-icon")
                .setTooltip("Match Case")
                .build(ToggleButton::new);
        var filterUseRegexToggle = new ToolButtonBuilder<ToggleButton>(source.getBindingManager())
                .setText(".*")
                .setStyleClass("dialog-button")
                .setIconStyleClass("regex-icon", "small-icon")
                .setTooltip("Use regular expression")
                .build(ToggleButton::new);
        var filterBar = new HBox(filterField, clearFilterbutton, filterCaseSensitiveToggle, filterUseRegexToggle);
        filterBar.getStyleClass().add("search-field-outer");
        filterBar.setPadding(new Insets(0, 3, 0, 3));
        filterBar.setSpacing(2);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        source.getBindingManager().attachListener(source.filterableProperty(), (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
            if (newValue) {
                filterField.requestFocus();
            }
        });
        source.getBindingManager().bind(filterBar.managedProperty(), source.filterableProperty());
        source.getBindingManager().bind(filterBar.visibleProperty(), filterBar.managedProperty());
        VBox.setMargin(filterBar, new Insets(10, 15, 5, 15));
        VBox sourcePaneContent = new VBox(filterBar);
        sourcePaneContent.getStyleClass().addAll("skinnable-pane-border", "chart-viewport-parent");
        AnchorPane.setBottomAnchor(sourcePaneContent, 0.0);
        AnchorPane.setLeftAnchor(sourcePaneContent, 0.0);
        AnchorPane.setRightAnchor(sourcePaneContent, 0.0);
        AnchorPane.setTopAnchor(sourcePaneContent, 0.0);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.setMaxHeight(Double.MAX_VALUE);
        treeView.setId("sourceTreeView");


        var treeFilterExpressionProperty = new SimpleObjectProperty<Pattern>();
        var filterCriteriaChanged = new SimpleBooleanProperty(false);
        Runnable onFilterChangeAction = () -> {
            try {
                treeFilterExpressionProperty.setValue(Pattern.compile((filterCaseSensitiveToggle.isSelected() ? "" : "(?i)") + filterField.getText()));
                filterCriteriaChanged.setValue(!filterCriteriaChanged.get());
            } catch (PatternSyntaxException e) {
                treeFilterExpressionProperty.setValue(null);
                if (filterUseRegexToggle.isSelected()) {
                    TextFieldValidator.fail(filterField, true, filterUseRegexToggle.selectedProperty());
                    logger.debug("Bad pattern", e);
                } else {
                    // validate change anyway if we don't care about the pattern
                    filterCriteriaChanged.setValue(!filterCriteriaChanged.get());
                }
            }
        };
        // Delay the search until at least the following amount of time elapsed since the last character was entered
        var delay = new PauseTransition(Duration.millis(UserPreferences.getInstance().searchFieldInputDelayMs.get().intValue()));
        source.getBindingManager().attachListener(filterField.textProperty(), o -> {
            delay.setOnFinished(event -> {
                onFilterChangeAction.run();
            });
            delay.playFromStart();
        });

        source.getBindingManager().attachListener(filterUseRegexToggle.selectedProperty(), observable -> onFilterChangeAction.run());
        source.getBindingManager().attachListener(filterCaseSensitiveToggle.selectedProperty(), observable -> onFilterChangeAction.run());

        source.getBindingManager().bind(((FilterableTreeItem<SourceBinding>) treeView.getRoot()).predicateProperty(), Bindings.createObjectBinding(() -> {
                    if (!source.isFilterable() ||
                            filterField.getText() == null ||
                            filterField.getText().length() < UserPreferences.getInstance().minCharsTreeFiltering.get().intValue())
                        return null;
                    return (parent, seriesBinding) -> {
                        boolean isMatch = seriesBinding != null;
                        if (filterUseRegexToggle.isSelected() && treeFilterExpressionProperty.getValue() != null) {
                            isMatch = isMatch && treeFilterExpressionProperty.getValue().matcher(seriesBinding.getTreeHierarchy()).find();
                        } else {
                            isMatch = isMatch && StringUtils.contains(
                                    seriesBinding.getTreeHierarchy(),
                                    filterField.getText(),
                                    filterCaseSensitiveToggle.isSelected());
                        }
                        if (isMatch) {
                            TreeViewUtils.expandBranch(parent, TreeViewUtils.ExpandDirection.UP);
                        }
                        return isMatch;
                    };
                },
                source.filterableProperty(),
                filterCriteriaChanged));
        sourcePaneContent.getChildren().addAll(treeView);
        return sourcePaneContent;
    }

    private boolean loadWorksheet(Worksheet<?> worksheet) throws CannotLoadWorksheetException {
        EditableTab newTab = loadWorksheetInTab(worksheet, false);
        tearableTabPane.getTabs().add(newTab);
        tearableTabPane.getSelectionModel().select(newTab);
        return false;
    }

    private void editWorksheet(Worksheet<?> worksheet) {
        try {
            editWorksheet(tearableTabPane.getSelectedTabPane(), worksheet);
        } catch (CannotLoadWorksheetException e) {
            Dialogs.notifyException("Error loading worksheet", e, root);
        }
    }

    private boolean editWorksheet(TabPane targetTabPane, Worksheet<?> worksheet) throws CannotLoadWorksheetException {
        var newTab = loadWorksheetInTab(worksheet, true);
        targetTabPane.getTabs().add(newTab);
        targetTabPane.getSelectionModel().select(newTab);
        return true;
    }

    private void reloadController(WorksheetController worksheetCtrl) throws CannotLoadWorksheetException {
        if (worksheetCtrl == null) {
            throw new IllegalArgumentException("Provided Worksheet controller cannot be null");
        }
        EditableTab tab = null;
        for (Map.Entry<EditableTab, WorksheetController> entry : seriesControllers.entrySet()) {
            if (entry.getValue().equals(worksheetCtrl)) {
                tab = entry.getKey();
            }
        }
        var worksheet = worksheetCtrl.getWorksheet();
        if (worksheet == null) {
            throw new IllegalStateException("WorksheetController is not associated to a valid Worksheet");
        }
        if (tab == null) {
            throw new IllegalStateException("cannot find associated tab or WorksheetController for " + worksheet.getName());
        }
        seriesControllers.remove(tab);
        tab.setContent(null);
        worksheetCtrl.close();
        loadWorksheet(worksheet, tab, false);
    }

    private WorksheetController loadWorksheet(Worksheet<?> worksheet, EditableTab newTab, boolean setToEditMode) throws CannotLoadWorksheetException {
        try {
            WorksheetController current = worksheet.getControllerClass()
                    .getDeclaredConstructor(this.getClass(),
                            worksheet.getClass(),
                            Collection.class)
                    .newInstance(this,
                            worksheet,
                            sourcesAdapters.values().stream()
                                    .map(Source::getAdapter)
                                    .collect(Collectors.toList()));
            try {
                // Register reload listener
                current.setReloadRequiredHandler(CheckedLambdas.wrap(this::reloadController));
                FXMLLoader fXMLLoader = new FXMLLoader(current.getClass().getResource(current.getView()));
                fXMLLoader.setController(current);
                Parent p = fXMLLoader.load();
                newTab.setContent(p);
            } catch (IOException ex) {
                logger.error("Error loading time series", ex);
            }
            seriesControllers.put(newTab, current);

            if (current.getWorksheet() instanceof Syncable syncableWorksheet) {
                current.getBindingManager().attachListener(current.selectedRangeProperty(),
                        (ChangeListener<TimeRange>) (observable, oldValue, newValue) -> {
                            getSelectedWorksheetController().ifPresent(worksheetController -> {
                                if (worksheetController.equals(current) && syncableWorksheet.isTimeRangeLinked()) {
                                    seriesControllers.values().stream()
                                            .filter(c -> c.getWorksheet() instanceof Syncable)
                                            .forEach(i -> {
                                                if (!i.equals(current) && ((Syncable) i.getWorksheet()).isTimeRangeLinked()) {
                                                    i.selectedRangeProperty().setValue(TimeRange.of(newValue));
                                                }
                                            });
                                }
                            });
                        }
                );
                current.getBindingManager().attachListener(syncableWorksheet.timeRangeLinkedProperty(),
                        (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                            if (newValue) {
                                seriesControllers.values()
                                        .stream()
                                        .filter(c -> !c.equals(current) &&
                                                c.getWorksheet() instanceof Syncable syncable &&
                                                syncable.isTimeRangeLinked())
                                        .map(c -> c.selectedRangeProperty().getValue())
                                        .findFirst()
                                        .ifPresent(timeRange -> current.selectedRangeProperty().setValue(timeRange));
                            }
                        }
                );
            }
            current.getBindingManager().bindBidirectional(newTab.nameProperty(), worksheet.nameProperty());
            if (setToEditMode) {
                logger.trace("Toggle edit mode for worksheet");
                current.setShowPropertiesPane(true);
            }
            newTab.setContextMenu(getTabContextMenu(newTab, worksheet, current.getBindingManager()));
            return current;
        } catch (Throwable e) {
            Throwable toThrow = e;
            // Rethrow original exception thrown by constructor, if captured
            if (e instanceof InvocationTargetException && e.getCause() != null) {
                toThrow = e.getCause();
            }
            logger.debug(() -> "Error loading worksheet into new tab", toThrow);
            throw new CannotLoadWorksheetException("Failed to load worksheet " +
                    (worksheet != null ? worksheet.getName() : "null") + ": " + toThrow.getMessage(), toThrow);
        }
    }

    private EditableTab loadWorksheetInTab(Worksheet<?> worksheet, boolean editMode) throws CannotLoadWorksheetException {
        workspace.setPresentationMode(false);
        var buttons = new ArrayList<ButtonBase>();
        if (worksheet instanceof Syncable syncable) {
            buttons.add(new ToolButtonBuilder<ToggleButton>()
                    .setText("link")
                    .setTooltip("Link Worksheet Timeline")
                    .setStyleClass("link")
                    .setIconStyleClass("link-icon", "small-icon")
                    .bindBidirectionnal(ToggleButton::selectedProperty, syncable.timeRangeLinkedProperty())
                    .build(ToggleButton::new));
        }
        Button closeTabButton = new ToolButtonBuilder<Button>()
                .setText("Close")
                .setTooltip("Close Worksheet")
                .setStyleClass("exit")
                .setIconStyleClass("cross-icon", "small-icon")
                .build(Button::new);
        buttons.add(closeTabButton);
        EditableTab newTab = new EditableTab("New worksheet", buttons.toArray(ButtonBase[]::new));
        loadWorksheet(worksheet, newTab, editMode);
        closeTabButton.setOnAction(event -> closeWorksheetTab(newTab));
        return newTab;
    }

    private void closeWorksheetTab(EditableTab tab) {
        if (tab == null) {
            logger.debug("Requested tab for closure is null");
            return;
        }
        if (Dialogs.confirmDialog(tab.getTabPane(), "Are you sure you want to close worksheet '" + tab.getName() + "'?" +
                        "\n\n(Closed worksheets can be restored by pressing ctrl+shift+T)", "",
                UserPreferences.getInstance().doNotWarnOnTabClose) == ButtonType.YES) {
            EventHandler<Event> handler = tab.getOnClosed();
            if (null != handler) {
                handler.handle(null);
            }
            tab.getTabPane().getTabs().remove(tab);
        }
    }

    private MenuItem getWorksheetMenuItem(EditableTab tab, Worksheet<?> worksheet, BindingManager manager) {
        var worksheetItem = new Menu();
        worksheetItem.setUserData(worksheet);
        manager.bind(worksheetItem.textProperty(), worksheet.nameProperty());
        worksheetItem.getItems().addAll(makeWorksheetMenuItem(tab, worksheet, manager));
        return worksheetItem;
    }

    private ContextMenu getTabContextMenu(EditableTab tab, Worksheet<?> worksheet, BindingManager manager) {
        var m = new ContextMenu();
        m.getItems().addAll(makeWorksheetMenuItem(tab, worksheet, manager));
        return m;
    }

    private ObservableList<MenuItem> makeWorksheetMenuItem(EditableTab tab, Worksheet<?> worksheet, BindingManager manager) {
        MenuItem close = new MenuItem("Close Worksheet");
        close.setOnAction(manager.registerHandler(event -> closeWorksheetTab(tab)));
        MenuItem closeOthers = new MenuItem("Close Other Worksheets");
        closeOthers.setOnAction(manager.registerHandler(event -> {
            if (Dialogs.confirmDialog(tab.getTabPane(), "Are you sure you want to close all worksheets except for '" + tab.getName() + "'?" +
                            "\n\n(Closed worksheets can be restored by pressing ctrl+shift+T)",
                    "") == ButtonType.YES) {
                var tabs = tab.getTabPane().getTabs();
                tabs.removeAll(tabs.stream()
                        .filter(tab1 -> !tab1.equals(tab))
                        .collect(Collectors.toList())
                );
            }
        }));
        MenuItem edit = new MenuItem("Rename Worksheet");
        edit.setOnAction(manager.registerHandler(event -> tab.setEditable(true)));
        MenuItem duplicate = new MenuItem("Duplicate Worksheet");
        duplicate.setOnAction(manager.registerHandler(event -> {
            editWorksheet(worksheet.duplicate());
        }));
        MenuItem detach = new MenuItem("Detach Worksheet");
        detach.setOnAction(manager.registerHandler(event -> {
            TearableTabPane pane = (TearableTabPane) tab.getTabPane();
            pane.detachTab(tab);
        }));
        var items = FXCollections.observableArrayList(
                close,
                closeOthers,
                new SeparatorMenuItem(),
                edit,
                duplicate,
                new SeparatorMenuItem(),
                detach
        );
        if (worksheet instanceof Syncable syncable) {
            MenuItem link = new MenuItem();
            manager.bind(link.textProperty(), Bindings.createStringBinding(() ->
                            syncable.isTimeRangeLinked() ? "Unlink Worksheet Timeline" : "Link Worksheet Timeline",
                    syncable.timeRangeLinkedProperty()));
            link.setOnAction(manager.registerHandler(event -> {
                syncable.setTimeRangeLinked(!syncable.isTimeRangeLinked());
            }));
            items.add(link);
        }
        return items;
    }

    private Image renderTextTooltip(String text) {
        var label = new Label("    " + text + "    ");
        label.getStyleClass().add("tooltip");
        // The label must be added to a scene so that CSS and layout are applied.
        StageAppearanceManager.getInstance().applyUiTheme(new Scene(label, Color.RED));
        return SnapshotUtils.scaledSnapshot(label, Dialogs.getOutputScaleX(root), Dialogs.getOutputScaleY(root));
    }

    private Optional<TreeView<SourceBinding>> buildTreeViewForTarget(DataAdapter<?> dp) {
        Objects.requireNonNull(dp, "DataAdapter instance provided to buildTreeViewForTarget cannot be null.");
        TreeView<SourceBinding> treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        Callback<TreeView<SourceBinding>, TreeCell<SourceBinding>> dragAndDropCellFactory = param -> {
            final TreeCell<SourceBinding> bindingTreeCell = new TreeCell<>();
            bindingTreeCell.itemProperty().addListener((observable, oldValue, newValue) -> bindingTreeCell.setText(newValue == null ? null : newValue.toString()));
            bindingTreeCell.setOnDragDetected(event -> {
                try {
                    if (bindingTreeCell.getItem() != null) {
                        treeItemDragAndDropInProgress.setValue(true);
                        Dragboard db = bindingTreeCell.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                        var toolTipText = StringUtils.ellipsize(
                                treeView.getSelectionModel()
                                        .getSelectedItems()
                                        .stream()
                                        .map(t -> t.getValue().getLegend())
                                        .collect(Collectors.joining(", ")),
                                100);
                        db.setDragView(renderTextTooltip(toolTipText));
                        ClipboardContent content = new ClipboardContent();
                        treeView.getSelectionModel().getSelectedItems().forEach(s -> content.put(
                                DataFormat.lookupMimeType(s.getValue().getMimeType()),
                                s.getValue().getTreeHierarchy()));
                        db.setContent(content);
                    } else {
                        logger.debug("No TreeItem selected: canceling drag and drop");
                    }
                } finally {
                    event.consume();
                }
            });
            bindingTreeCell.setOnDragDone(event -> {
                treeItemDragAndDropInProgress.setValue(false);
            });
            return bindingTreeCell;
        };
        treeView.setCellFactory(ContextMenuTreeViewCell.forTreeView(getTreeViewContextMenu(treeView), dragAndDropCellFactory));
        try {
            dp.onStart();
            FilterableTreeItem<SourceBinding> bindingTree = dp.getBindingTree();
            bindingTree.setExpanded(true);
            treeView.setRoot(bindingTree);
            return Optional.of(treeView);
        } catch (Throwable e) {
            Dialogs.notifyException("An error occurred while getting data from source " + dp.getSourceName(), e, root);
            // Failed to load tree: attempt to close DataAdapter
            try {
                dp.close();
            } catch (Throwable t) {
                logger.warn("An error occurred while attempting to close DataAdapter " + dp.getId(), e);
            }
        }
        return Optional.empty();
    }

    private void handleControlKey(KeyEvent event, boolean pressed) {
        switch (event.getCode()) {
            case SHIFT -> {
                AppEnvironment.getInstance().setShiftPressed(pressed);
                event.consume();
            }
            case CONTROL, META, SHORTCUT -> { // shortcut does not seem to register as Control on Windows here, so check them all.
                AppEnvironment.getInstance().setCtrlPressed(pressed);
                event.consume();
            }
            default -> {
            }
        }
    }

    private ContextMenu getTreeViewContextMenu(final TreeView<SourceBinding> treeView) {
        MenuItem expandBranch = new MenuItem("Expand Branch");
        expandBranch.setOnAction(event -> treeView.getSelectionModel().getSelectedItems().forEach(item -> TreeViewUtils.expandBranch(item, TreeViewUtils.ExpandDirection.DOWN)));
        MenuItem collapseBranch = new MenuItem("Collapse Branch");
        collapseBranch.setOnAction(event -> {
            // It is necessary to clone the list of selected nodes prior to start collapsing them as it resets
            // the selection, leading to a NoSuchElementException while iterating  getSelectedItems directly.
            var items = treeView.getSelectionModel().getSelectedItems().toArray(TreeItem<?>[]::new);
            for (var item : items) {
                TreeViewUtils.collapseBranch(item, TreeViewUtils.ExpandDirection.DOWN);
            }
        });

        Menu addToCurrent = new Menu("Add to current worksheet", null, new MenuItem("none"));
        addToCurrent.disableProperty().bind(noWorksheetPresent);
        addToCurrent.setOnShowing(event -> getSelectedWorksheetController().ifPresent(controller ->
                addToCurrent.getItems().setAll(controller
                        .getChartListContextMenu(treeView.getSelectionModel().getSelectedItems())
                        .getItems())));
        MenuItem addToNew = new MenuItem("Add to new worksheet");
        addToNew.setOnAction(event ->
                addToNewWorksheet(tearableTabPane.getSelectedTabPane(), treeView.getSelectionModel().getSelectedItems()));
        return new ContextMenu(expandBranch, collapseBranch, new SeparatorMenuItem(), addToCurrent, addToNew);
    }

    private void addToNewWorksheet(TabPane tabPane, Collection<TreeItem<SourceBinding>> rootItems) {
        // Schedule for later execution in order to let other drag and dropped event to complete before modal dialog gets displayed
        Platform.runLater(() -> {
            try {
                var charts = treeItemsAsChartList(rootItems, root);
                var title = StringUtils.ellipsize(rootItems.stream()
                        .map(t -> t.getValue().getLegend())
                        .collect(Collectors.joining(", ")), 50);
                if (charts.isPresent()) {
                    for (var t : rootItems
                            .stream()
                            .map(s -> s.getValue().getWorksheetClass())
                            .distinct()
                            .toList()) {
                        var worksheet = WorksheetFactory.getInstance().createWorksheet(t, title, charts.get());
                        editWorksheet(tabPane, worksheet);
                    }
                }
            } catch (Exception e) {
                Dialogs.notifyException("Error adding bindings to new worksheet", e, root);
            }
        });
    }

    private void findNext() {
        if (isNullOrEmpty(searchField.getText())) {
            return;
        }
        TreeView<SourceBinding> selectedTreeView = getSelectedTreeView();
        if (selectedTreeView == null) {
            return;
        }
        if (searchResultSet == null) {
            searchResultSet = TreeViewUtils.findAllInTree(selectedTreeView.getRoot(), i -> {
                if (i.getValue() == null || i.getValue().getLegend() == null) {
                    return false;
                }
                if (searchCaseSensitiveToggle.isSelected()) {
                    return i.getValue().getLegend().contains(searchField.getText());
                } else {
                    return i.getValue().getLegend().toLowerCase().contains(searchField.getText().toLowerCase());
                }
            });
        }
        if (!searchResultSet.isEmpty()) {
            currentSearchHit++;
            if (currentSearchHit > searchResultSet.size() - 1) {
                currentSearchHit = 0;
            }
            selectedTreeView.getSelectionModel().clearSelection();
            selectedTreeView.getSelectionModel().select(searchResultSet.get(currentSearchHit));
            selectedTreeView.scrollTo(selectedTreeView.getRow(searchResultSet.get(currentSearchHit)));
        } else {
            searchField.setStyle("-fx-background-color: #FF000040;");
        }
        logger.trace(() -> "Search for " + searchField.getText() + " yielded " + searchResultSet.size() + " match(es)");
    }

    private void invalidateSearchResults() {
        logger.trace("Invalidating search result");
        this.searchResultSet = null;
        this.currentSearchHit = -1;
    }

    private boolean isNullOrEmpty(String s) {
        return (s == null || s.trim().length() == 0);
    }

    private void onWorksheetTabChanged(ListChangeListener.Change<? extends Tab> c) {
        while (c.next()) {
            if (c.wasAdded()) {
                c.getAddedSubList().forEach(tab -> {
                    WorksheetController ctlr = seriesControllers.get(tab);
                    if (ctlr != null) {
                        workspace.addWorksheets(seriesControllers.get(tab).getWorksheet());
                        worksheetMenu.getItems().add(
                                getWorksheetMenuItem((EditableTab) tab,
                                        seriesControllers.get(tab).getWorksheet(),
                                        seriesControllers.get(tab).getBindingManager()));
                    } else {
                        logger.warn("Could not find a controller assigned to tab " + tab.getText());
                    }
                });
            }
            if (c.wasRemoved()) {
                c.getRemoved().forEach((tab -> {
                    WorksheetController ctlr = seriesControllers.get(tab);
                    // sever ref to allow collect
                    tab.setContent(null);
                    tab.setContextMenu(null);
                    if (ctlr != null) {
                        worksheetMenu.getItems()
                                .stream()
                                .filter(menuItem -> menuItem.getUserData() == ctlr.getWorksheet())
                                .findFirst()
                                .ifPresent(item -> worksheetMenu.getItems().remove(item));

                        workspace.removeWorksheets(ctlr.getWorksheet());
                        seriesControllers.remove(tab);
                        ctlr.close();
                    } else {
                        logger.warn("Could not find a controller assigned to tab " + tab.getText());
                    }
                }));
            }
        }
        logger.debug(() -> "Worksheets in current workspace: " +
                workspace.getWorksheets().stream()
                        .map(Worksheet::getName)
                        .reduce((s, s2) -> s + " " + s2)
                        .orElse("null"));
    }

    private void onSourceTabChanged(ListChangeListener.Change<? extends TitledPane> c) {
        AtomicBoolean removed = new AtomicBoolean(false);
        while (c.next()) {
            c.getAddedSubList().forEach(t -> workspace.addSource(sourcesAdapters.get(t)));
            c.getRemoved().forEach(t -> {
                removed.set(true);
                try {
                    Source removedSource = sourcesAdapters.remove(t);
                    if (removedSource != null) {
                        workspace.removeSource(removedSource);
                        logger.debug("Closing Source " + removedSource.getName());
                        removedSource.close();
                    } else {
                        logger.trace("No Source to close attached to tab " + t.getText());
                    }
                } catch (Exception e) {
                    Dialogs.notifyException("On error occurred while closing Source", e);
                }
            });
        }
        if (removed.get()) {
            refreshAllWorksheets();
        }
        logger.debug(() -> "Sources in current workspace: " +
                StreamSupport.stream(workspace.getSources().spliterator(), false)
                        .map(Source::getName)
                        .reduce((s, s2) -> s + " " + s2)
                        .orElse("null"));
    }

    private Optional<Tab> worksheetTabFactory(ActionEvent event) {
        try {
            return Optional.of(loadWorksheetInTab(new XYChartsWorksheet(), true));
        } catch (CannotLoadWorksheetException e) {
            logger.error(e);
            return Optional.empty();
        }
    }

    @FXML
    private void handleDragDroppedOnWorksheetArea(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(GENERIC_BINDING_FORMAT) ||
                db.hasContent(TIME_SERIES_BINDING_FORMAT) ||
                db.hasContent(TEXT_FILES_BINDING_FORMAT) ||
                db.hasContent(LOG_FILES_BINDING_FORMAT)) {
            TreeView<SourceBinding> treeView = getSelectedTreeView();
            if (treeView != null) {
                var items = treeView.getSelectionModel().getSelectedItems();
                if (items != null && !items.isEmpty()) {
                    var currentTabPane = (TabPane) ((Node) event.getGestureTarget()).getScene().lookup("#tearableTabPane");
                    addToNewWorksheet(currentTabPane != null ? currentTabPane : tearableTabPane.getSelectedTabPane(), items);
                } else {
                    logger.warn("Cannot complete drag and drop operation: selected TreeItem is null");
                }
            } else {
                logger.warn("Cannot complete drag and drop operation: selected TreeView is null");
            }
            event.consume();
        }
    }

    private void saveWindowPositionAndQuit() {
        Stage stage = Dialogs.getStage(root);
        if (stage != null) {
            UserPreferences.getInstance().windowLastPosition.set(
                    new Rectangle2D(
                            stage.getX(),
                            stage.getY(),
                            stage.getWidth(),
                            stage.getHeight()));
        }
        UpdateManager.getInstance().startUpdate();
        Platform.exit();
    }

    public Optional<WorksheetController> getSelectedWorksheetController() {
        Tab selectedTab = tearableTabPane.getSelectedTab();
        if (selectedTab == null) {
            return Optional.empty();
        }
        return Optional.of(seriesControllers.get(selectedTab));
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void refreshAllWorksheets() {
        seriesControllers.values().forEach(WorksheetController::refresh);
    }

    public Optional<String> getAssociatedFile() {
        return associatedFile;
    }

    public void setAssociatedFile(Optional<String> associatedFile) {
        this.associatedFile = associatedFile;
    }

    @FXML
    private void handleToggleSourcePaneVisibility(ActionEvent actionEvent) {
        workspace.setSourcePaneVisible(!workspace.isSourcePaneVisible());
    }

    private void toggleSourcePaneVisibilty(Boolean newValue) {
        if (!newValue) {
            getWorkspace().getBindingManager().suspend(getWorkspace().dividerPositionProperty());
            contentView.setDividerPositions(0.0);
            hideSourcePaneMenu.setText("Show Source Pane");
            sourcePane.setVisible(false);
            sourcePane.setMaxWidth(0.0);
        } else {
            sourcePane.setMaxWidth(Double.MAX_VALUE);
            sourcePane.setVisible(true);
            hideSourcePaneMenu.setText("Hide Source Pane");
            contentView.setDividerPositions(getWorkspace().getDividerPosition());
            getWorkspace().getBindingManager().resume();
        }
    }

    public void handleTogglePresentationMode() {
        workspace.setPresentationMode(!workspace.isPresentationMode());
    }

    @FXML
    private void handleOnDragExitedNewWorksheet(DragEvent event) {
        ((Region) event.getSource()).pseudoClassStateChanged(HOVER_PSEUDO_CLASS, false);
    }

    @FXML
    private void handleOnDragEnteredNewWorksheet(DragEvent event) {
        ((Region) event.getSource()).pseudoClassStateChanged(HOVER_PSEUDO_CLASS, true);
    }

    public boolean isTreeItemDragAndDropInProgress() {
        return treeItemDragAndDropInProgress.get();
    }

    public void setTreeItemDragAndDropInProgress(boolean treeItemDragAndDropInProgress) {
        this.treeItemDragAndDropInProgress.set(treeItemDragAndDropInProgress);
    }

    public BooleanProperty treeItemDragAndDropInProgressProperty() {
        return treeItemDragAndDropInProgress;
    }

    @FXML
    private void handleRestoreClosedWorksheet(ActionEvent actionEvent) {
        restoreLatestClosedWorksheet();
    }

    //endregion
}
