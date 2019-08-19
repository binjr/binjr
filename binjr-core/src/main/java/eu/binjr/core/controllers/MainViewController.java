/*
 *    Copyright 2016-2019 Frederic Thevenet
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

import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.common.javafx.controls.*;
import eu.binjr.common.text.StringUtils;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.adapters.DataAdapterInfo;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.workspace.Source;
import eu.binjr.core.data.workspace.Worksheet;
import eu.binjr.core.data.workspace.Workspace;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.dialogs.StageAppearanceManager;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.GlobalPreferences;
import eu.binjr.core.update.UpdateManager;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.MaskerPane;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;
import org.eclipse.fx.ui.controls.tree.TreeItemPredicate;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    static final DataFormat TIME_SERIES_BINDING_FORMAT = new DataFormat("TimeSeriesBindingFormat");
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    private static final String[] BINJR_FILE_PATTERN = new String[]{ "*.bjr", "*.xml"};
    private static final double SEARCH_BAR_PANE_DISTANCE = 40;
    private static PseudoClass HOVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("hover");
    private final Map<EditableTab, WorksheetController> seriesControllers = new WeakHashMap<>();
    private final Map<TitledPane, Source> sourcesAdapters = new WeakHashMap<>();
    private final BooleanProperty searchBarVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty searchBarHidden = new SimpleBooleanProperty(!searchBarVisible.get());
    private final BooleanProperty treeItemDragAndDropInProgress = new SimpleBooleanProperty(false);
    public AnchorPane sourcePane;
    public MenuItem hideSourcePaneMenu;
    public StackPane newWorksheetDropTarget;
    @FXML
    public DrawerPane commandBar;
    @FXML
    public AnchorPane root;
    @FXML
    public Label addWorksheetLabel;
    @FXML
    public MaskerPane sourceMaskerPane;
    @FXML
    public MaskerPane worksheetMaskerPane;
    @FXML
    public Pane searchBarRoot;
    @FXML
    public TextField searchField;
    @FXML
    public Button searchButton;
    @FXML
    public Button hideSearchBarButton;
    @FXML
    public ToggleButton searchCaseSensitiveToggle;
    @FXML
    public StackPane sourceArea;
    List<TreeItem<TimeSeriesBinding>> searchResultSet;
    int currentSearchHit = -1;
    private Workspace workspace;
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

    /**
     * Initializes a new instance of the {@link MainViewController} class.
     */
    public MainViewController() {
        super();
        this.workspace = new Workspace();
    }

    @FXML
    private void worksheetAreaOnDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(TIME_SERIES_BINDING_FORMAT)) {
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
        Binding<Boolean> selectWorksheetPresent = Bindings.size(tearableTabPane.getTabs()).isEqualTo(0);
        Binding<Boolean> selectedSourcePresent = Bindings.size(sourcesPane.getPanes()).isEqualTo(0);
        contentView.getDividers().stream().findFirst().ifPresent(divider -> {
            divider.setPosition(getWorkspace().getDividerPosition());
            getWorkspace().getBindingManager().bind(getWorkspace().dividerPositionProperty(), divider.positionProperty());
        });
        sourcesPane.mouseTransparentProperty().bind(selectedSourcePresent);
        workspace.sourcePaneVisibleProperty().addListener((observable, oldValue, newValue) -> toggleSourcePaneVisibilty(newValue));
        workspace.presentationModeProperty().addListener((observable, oldValue, newValue) -> {
            for (var w : workspace.getWorksheets()) {
                w.setChartLegendsVisible(!newValue);
            }
            workspace.setSourcePaneVisible(!newValue);
        });
        for (var w : workspace.getWorksheets()) {
            w.setChartLegendsVisible(!workspace.isPresentationMode());
        }
        workspace.setSourcePaneVisible(!workspace.isPresentationMode());
        toggleSourcePaneVisibilty(workspace.isSourcePaneVisible());
        sourcesPane.expandedPaneProperty().addListener(
                (ObservableValue<? extends TitledPane> observable, TitledPane oldPane, TitledPane newPane) -> {
                    boolean expandRequiered = true;
                    for (TitledPane pane : sourcesPane.getPanes()) {
                        if (pane.isExpanded()) {
                            expandRequiered = false;
                        }
                    }
                    if ((expandRequiered) && (oldPane != null)) {
                        Platform.runLater(() -> sourcesPane.setExpandedPane(oldPane));
                    }
                });
        addWorksheetLabel.visibleProperty().bind(selectWorksheetPresent);
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
        newWorksheetDropTarget.managedProperty()
                .bind(tearableTabPane.emptyProperty().not().and(treeItemDragAndDropInProgressProperty()));
        newWorksheetDropTarget.visibleProperty()
                .bind(tearableTabPane.emptyProperty().not().and(treeItemDragAndDropInProgressProperty()));
        Platform.runLater(this::runAfterInitialize);
    }

    protected void runAfterInitialize() {
        GlobalPreferences prefs = GlobalPreferences.getInstance();
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
        } else if (prefs.isLoadLastWorkspaceOnStartup()) {
            prefs.getMostRecentSavedWorkspace().ifPresent(path -> {
                File latestWorkspace = path.toFile();
                if (latestWorkspace.exists()) {
                    loadWorkspace(latestWorkspace);
                } else {
                    logger.warn("Cannot reopen workspace " + latestWorkspace.getPath() + ": file does not exists");
                }
            });
        }

        if (prefs.isCheckForUpdateOnStartUp()) {
            UpdateManager.getInstance().asyncCheckForUpdate(
                    release -> UpdateManager.getInstance().showUpdateAvailableNotification(release, root), null, null
            );
        }
    }

    private void registerStageKeyEvents(Stage stage) {
        BindingManager manager = new BindingManager();
        stage.setUserData(manager);
        stage.addEventFilter(KeyEvent.KEY_RELEASED, manager.registerHandler(e -> {
            if (e.getCode() == KeyCode.F12) {
                AppEnvironment.getInstance().setDebugMode(!AppEnvironment.getInstance().isDebugMode());
            }
            if (e.getCode() == KeyCode.F5) {
                handleRefreshAction();
            }
            if (e.getCode() == KeyCode.M && e.isControlDown()) {
                handleTogglePresentationMode();
            }
            if (e.getCode() == KeyCode.P && e.isControlDown()) {
                if (getSelectedWorksheetController() != null) {
                    getSelectedWorksheetController().saveSnapshot();
                }
            }
        }));
        stage.addEventFilter(KeyEvent.KEY_PRESSED, manager.registerHandler(e -> handleControlKey(e, true)));
        stage.addEventFilter(KeyEvent.KEY_RELEASED, manager.registerHandler(e -> handleControlKey(e, false)));
        manager.attachListener(stage.focusedProperty(), (observable, oldValue, newValue) -> {
            //main stage lost focus -> invalidates shift or ctrl pressed
            GlobalPreferences.getInstance().setShiftPressed(false);
            GlobalPreferences.getInstance().setCtrlPressed(false);
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
        dialog.setDialogPane(FXMLLoader.load(getClass().getResource("/eu/binjr/views/AboutBoxView.fxml")));
        dialog.initOwner(Dialogs.getStage(root));
        dialog.getDialogPane().getStylesheets().add(getClass().getResource(StageAppearanceManager.getFontFamilyCssPath()).toExternalForm());
        dialog.showAndWait();
    }

    @FXML
    protected void handleQuitAction(ActionEvent event) {
        if (confirmAndClearWorkspace()) {
            saveWindowPositionAndQuit();
        }
    }

    private void handleRefreshAction() {
        if (getSelectedWorksheetController() != null) {
            getSelectedWorksheetController().refresh();
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
    public void handleExpandCommandBar(ActionEvent actionEvent) {
        commandBar.toggle();
    }

    @FXML
    protected void handleAddNewWorksheet(Event event) {
        editWorksheet(new Worksheet());
    }

    @FXML
    private void handleAddSource(Event event) {
        Node sourceNode = (Node) event.getSource();
        ContextMenu sourceMenu = new ContextMenu();
        sourceMenu.getItems().addAll(populateSourceMenu());
        sourceMenu.show(sourceNode, Side.BOTTOM, 0, 0);
    }

    @FXML
    protected void handleHelpAction(ActionEvent event) {
        try {
            Dialogs.launchUrlInExternalBrowser(AppEnvironment.HTTP_BINJR_WIKI);
        } catch (IOException | URISyntaxException e) {
            logger.error("Failed to launch url in browser: " + AppEnvironment.HTTP_BINJR_WIKI);
            logger.debug("Exception stack", e);
        }
    }

    @FXML
    protected void handleViewOnGitHub(ActionEvent event) {
        try {
            Dialogs.launchUrlInExternalBrowser(AppEnvironment.HTTP_GITHUB_REPO);
        } catch (IOException | URISyntaxException e) {
            logger.error("Failed to launch url in browser: " + AppEnvironment.HTTP_GITHUB_REPO);
            logger.debug("Exception stack", e);
        }
    }

    @FXML
    protected void handleBinjrWebsite(ActionEvent actionEvent) {
        try {
            Dialogs.launchUrlInExternalBrowser(AppEnvironment.HTTP_WWW_BINJR_EU);
        } catch (IOException | URISyntaxException e) {
            logger.error("Failed to launch url in browser: " + AppEnvironment.HTTP_WWW_BINJR_EU);
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
    public void handleHidePanel(ActionEvent actionEvent) {
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

    @FXML
    protected void handleDisplayChartProperties(ActionEvent actionEvent) {
        if (getSelectedWorksheetController() != null) {
            getSelectedWorksheetController().toggleShowPropertiesPane();
        }
    }

    //endregion

    @FXML
    protected void populateOpenRecentMenu(Event event) {
        Menu menu = (Menu) event.getSource();
        Collection<String> recentPath = GlobalPreferences.getInstance().getRecentFiles();
        if (!recentPath.isEmpty()) {
            menu.getItems().setAll(recentPath.stream().map(s -> {
                MenuItem m = new MenuItem(s);
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
                            "WARNING: This will remove all associated series from existing worksheets.",
                            ButtonType.YES, ButtonType.NO) == ButtonType.YES) {
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

        titleRegion.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                source.setEditable(true);
                sourceNameField.selectAll();
                sourceNameField.requestFocus();
            }
        });
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
                        DataAdapterFactory.getInstance().getDialog(adapterInfo.getKey(), root).showAndWait().ifPresent(this::addSource);
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
        return menuItems;
    }

    TreeView<TimeSeriesBinding> getSelectedTreeView() {
        if (sourcesPane == null || sourcesPane.getExpandedPane() == null) {
            return null;
        }
        var treeView = sourcesPane.getExpandedPane().getContent().lookup("#sourceTreeView");
        return (TreeView<TimeSeriesBinding>) treeView;
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
        fileChooser.setInitialDirectory(GlobalPreferences.getInstance().getMostRecentSaveFolder().toFile());
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
                            DataAdapter da = DataAdapterFactory.getInstance().newAdapter(source.getAdapterClassName());
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

    private void loadWorksheets(Workspace wsFromfile) {
        try {
            for (Worksheet worksheet : wsFromfile.getWorksheets()) {
                loadWorksheet(worksheet);
            }
            workspace.cleanUp();
            GlobalPreferences.getInstance().putToRecentFiles(workspace.getPath().toString());
            logger.debug(() -> "Recently loaded workspaces: " + String.join(" ", GlobalPreferences.getInstance().getRecentFiles()));

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
        fileChooser.setInitialDirectory(GlobalPreferences.getInstance().getMostRecentSaveFolder().toFile());
        fileChooser.setInitialFileName(BINJR_FILE_PATTERN[0]);
        File selectedFile = fileChooser.showSaveDialog(Dialogs.getStage(root));
        if (selectedFile != null) {
            try {
                workspace.save(selectedFile);
                GlobalPreferences.getInstance().putToRecentFiles(workspace.getPath().toString());
                return true;
            } catch (IOException e) {
                Dialogs.notifyException("Failed to save snapshot to disk", e, root);
            } catch (JAXBException e) {
                Dialogs.notifyException("Error while serializing workspace", e, root);
            }
        }
        return false;
    }

    private void addSource(DataAdapter da) {
        Source newSource = Source.of(da);
        TitledPane newSourcePane = newSourcePane(newSource);
        sourceMaskerPane.setVisible(true);
        workspace.setPresentationMode(false);
        AsyncTaskManager.getInstance().submit(() -> buildTreeViewForTarget(da),
                event -> {
                    sourceMaskerPane.setVisible(false);
                    Optional<TreeView<TimeSeriesBinding>> treeView =
                            (Optional<TreeView<TimeSeriesBinding>>) event.getSource().getValue();
                    if (treeView.isPresent()) {
                        newSourcePane.setContent(buildSourcePaneContent(treeView.get(), newSource));
                        sourcesAdapters.put(newSourcePane, newSource);
                        sourcesPane.getPanes().add(newSourcePane);
                        newSourcePane.setExpanded(true);
                    }
                },
                event -> {
                    sourceMaskerPane.setVisible(false);
                    Dialogs.notifyException("Unexpected error getting data adapter:",
                            event.getSource().getException(),
                            root);
                });
    }

    private void loadSource(Source source) throws DataAdapterException {
        TitledPane newSourcePane = newSourcePane(source);
        Optional<TreeView<TimeSeriesBinding>> treeView;
        treeView = buildTreeViewForTarget(source.getAdapter());
        newSourcePane.setContent(buildSourcePaneContent((treeView.orElseGet(() -> {
            FilterableTreeItem<TimeSeriesBinding> i = new FilterableTreeItem<>(new TimeSeriesBinding());
            Label l = new Label("<Failed to connect to \"" + source.getName() + "\">");
            l.setTextFill(Color.RED);
            i.setGraphic(l);
            return new TreeView<>(i);
        })), source));
        sourcesAdapters.put(newSourcePane, source);
        Platform.runLater(() -> {
            sourcesPane.getPanes().add(newSourcePane);
            newSourcePane.setExpanded(true);
        });
    }

    private Node buildSourcePaneContent(TreeView<TimeSeriesBinding> treeView, Source source) {
        TextField filterField = new TextField();
        filterField.setPromptText("Type in text to filter the tree view.");
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterField.setMaxWidth(Double.MAX_VALUE);
        var clearFilterbutton = new ToolButtonBuilder<Button>()
                .setText("Clear")
                .setTooltip("Clear filter")
                .setStyleClass("dialog-button")
                .setIconStyleClass("trash-icon", "small-icon")
                .setAction(event -> filterField.clear())
                .build(Button::new);
        var filterCaseSensitiveToggle = new ToolButtonBuilder<ToggleButton>()
                .setText("Aa")
                .setStyleClass("dialog-button")
                .setIconStyleClass("match-case-icon")
                .setTooltip("Match Case")
                .build(ToggleButton::new);
        var filterBar = new HBox(filterField, filterCaseSensitiveToggle, clearFilterbutton);
        filterBar.setSpacing(5.0);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        source.filterableProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                filterField.requestFocus();
            }
        });
        filterBar.managedProperty().bind(source.filterableProperty());
        filterBar.visibleProperty().bind(filterBar.managedProperty());
        VBox.setMargin(filterBar, new Insets(10, 5, 5, 15));
        VBox sourcePaneContent = new VBox(filterBar);
        sourcePaneContent.getStyleClass().addAll("skinnable-pane-border", "chart-viewport-parent");
        AnchorPane.setBottomAnchor(sourcePaneContent, 0.0);
        AnchorPane.setLeftAnchor(sourcePaneContent, 0.0);
        AnchorPane.setRightAnchor(sourcePaneContent, 0.0);
        AnchorPane.setTopAnchor(sourcePaneContent, 0.0);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.setMaxHeight(Double.MAX_VALUE);
        treeView.setId("sourceTreeView");
        ((FilterableTreeItem<TimeSeriesBinding>) treeView.getRoot()).predicateProperty().bind(Bindings.createObjectBinding(() -> {
                    if (!source.isFilterable() ||
                            filterField.getText() == null ||
                            filterField.getText().length() < GlobalPreferences.getInstance().getMinCharsTreeFiltering())
                        return null;
                    return (TreeItemPredicate<TimeSeriesBinding>) (parent, seriesBinding) -> {
                        var isMatch = seriesBinding != null && StringUtils.contains(
                                seriesBinding.getTreeHierarchy(),
                                filterField.getText(),
                                filterCaseSensitiveToggle.isSelected());
                        if (isMatch) {
                            TreeViewUtils.expandBranch(parent, TreeViewUtils.ExpandDirection.UP);
                        }
                        return isMatch;
                    };
                },
                source.filterableProperty(),
                filterField.textProperty(),
                filterCaseSensitiveToggle.selectedProperty()));
        sourcePaneContent.getChildren().addAll(treeView);
        return sourcePaneContent;
    }

    private boolean loadWorksheet(Worksheet worksheet) {
        EditableTab newTab = loadWorksheetInTab(worksheet, false);
        tearableTabPane.getTabs().add(newTab);
        tearableTabPane.getSelectionModel().select(newTab);
        return false;
    }

    private void reloadController(WorksheetController worksheetCtrl) {
        if (worksheetCtrl == null) {
            throw new IllegalArgumentException("Provided Worksheet controller cannot be null");
        }
        EditableTab tab = null;
        for (Map.Entry<EditableTab, WorksheetController> entry : seriesControllers.entrySet()) {
            if (entry.getValue().equals(worksheetCtrl)) {
                tab = entry.getKey();
            }
        }
        Worksheet worksheet = worksheetCtrl.getWorksheet();
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

    private WorksheetController loadWorksheet(Worksheet worksheet, EditableTab newTab, boolean setToEditMode) {
        try {
            WorksheetController current = new WorksheetController(this, worksheet,
                    sourcesAdapters.values().stream().map(Source::getAdapter).collect(Collectors.toList()));
            try {
                // Register reload listener
                current.setReloadRequiredHandler(this::reloadController);
                FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/eu/binjr/views/WorksheetView.fxml"));
                fXMLLoader.setController(current);
                Parent p = fXMLLoader.load();
                newTab.setContent(p);
            } catch (IOException ex) {
                logger.error("Error loading time series", ex);
            }
            seriesControllers.put(newTab, current);
            current.getBindingManager().attachListener(current.selectedRangeProperty(),
                    (ChangeListener<TimeRangePicker.TimeRange>) (observable, oldValue, newValue) -> {
                        if (getSelectedWorksheetController().equals(current) && current.getWorksheet().isTimeRangeLinked()) {
                            seriesControllers.values().forEach(i -> {
                                if (!i.equals(current) && i.getWorksheet().isTimeRangeLinked()) {
                                    i.selectedRangeProperty().setValue(TimeRangePicker.TimeRange.of(newValue));
                                }
                            });
                        }
                    }
            );
            current.getBindingManager().attachListener(current.getWorksheet().timeRangeLinkedProperty(),
                    (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                        if (newValue) {
                            seriesControllers.values()
                                    .stream()
                                    .filter(c -> !c.equals(current) && c.getWorksheet().isTimeRangeLinked())
                                    .map(c -> c.selectedRangeProperty().getValue())
                                    .findFirst()
                                    .ifPresent(timeRange -> current.selectedRangeProperty().setValue(timeRange));
                        }
                    }
            );
            current.getBindingManager().bindBidirectional(newTab.nameProperty(), worksheet.nameProperty());
            if (setToEditMode) {
                logger.trace("Toggle edit mode for worksheet");
                current.setShowPropertiesPane(true);
            }
            newTab.setContextMenu(getTabContextMenu(newTab, worksheet, current.getBindingManager()));
            return current;
        } catch (Exception e) {
            Dialogs.notifyException("Error loading worksheet into new tab", e, root);
            return null;
        }
    }

    private EditableTab loadWorksheetInTab(Worksheet worksheet, boolean editMode) {
        workspace.setPresentationMode(false);
        Button closeTabButton = new ToolButtonBuilder<Button>()
                .setText("Close")
                .setTooltip("Close Worksheet")
                .setStyleClass("exit")
                .setIconStyleClass("cross-icon", "small-icon")
                .build(Button::new);
        ToggleButton linkTabButton = new ToolButtonBuilder<ToggleButton>()
                .setText("link")
                .setTooltip("Link Worksheet Timeline")
                .setStyleClass("link")
                .setIconStyleClass("link-icon", "small-icon")
                .bindBidirectionnal(ToggleButton::selectedProperty, worksheet.timeRangeLinkedProperty())
                .build(ToggleButton::new);
        EditableTab newTab = new EditableTab("New worksheet", linkTabButton, closeTabButton);
        loadWorksheet(worksheet, newTab, editMode);
        closeTabButton.setOnAction(event -> closeWorksheetTab(newTab));
        return newTab;
    }

    private void closeWorksheetTab(EditableTab tab) {
        if (Dialogs.confirmDialog(tab.getTabPane(), "Are you sure you want to close tab '" + tab.getName() + "'?", "",
                ButtonType.YES, ButtonType.NO) == ButtonType.YES) {
            EventHandler<Event> handler = tab.getOnClosed();
            if (null != handler) {
                handler.handle(null);
            }
            tab.getTabPane().getTabs().remove(tab);
        }
    }

    private MenuItem getWorksheetMenuItem(EditableTab tab, Worksheet worksheet, BindingManager manager) {
        var worksheetItem = new Menu();
        worksheetItem.setUserData(worksheet);
        manager.bind(worksheetItem.textProperty(), worksheet.nameProperty());
        worksheetItem.getItems().addAll(makeWorksheetMenuItem(tab, worksheet, manager));
        return worksheetItem;
    }

    private ContextMenu getTabContextMenu(EditableTab tab, Worksheet worksheet, BindingManager manager) {
        var m = new ContextMenu();
        m.getItems().addAll(makeWorksheetMenuItem(tab, worksheet, manager));
        return m;
    }

    private ObservableList<MenuItem> makeWorksheetMenuItem(EditableTab tab, Worksheet worksheet, BindingManager manager) {
        MenuItem close = new MenuItem("Close Worksheet");
        close.setOnAction(manager.registerHandler(event -> closeWorksheetTab(tab)));
        MenuItem closeOthers = new MenuItem("Close Other Worksheets");
        closeOthers.setOnAction(manager.registerHandler(event -> {
            if (Dialogs.confirmDialog(tab.getTabPane(), "Are you sure you want to close all worksheets except for '" + tab.getName() + "'?",
                    "", ButtonType.YES, ButtonType.NO) == ButtonType.YES) {
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
            editWorksheet(new Worksheet(worksheet));
        }));
        MenuItem detach = new MenuItem("Detach Worksheet");
        detach.setOnAction(manager.registerHandler(event -> {
            TearableTabPane pane = (TearableTabPane) tab.getTabPane();
            pane.detachTab(tab);
        }));
        MenuItem link = new MenuItem();
        manager.bind(link.textProperty(), Bindings.createStringBinding(() ->
                        worksheet.isTimeRangeLinked() ? "Unlink Worksheet Timeline" : "Link Worksheet Timeline",
                worksheet.timeRangeLinkedProperty()));
        link.setOnAction(manager.registerHandler(event -> {
            worksheet.setTimeRangeLinked(!worksheet.isTimeRangeLinked());
        }));
        return FXCollections.observableArrayList(
                close,
                closeOthers,
                new SeparatorMenuItem(),
                edit,
                duplicate,
                new SeparatorMenuItem(),
                detach,
                link
        );
    }

    private boolean editWorksheet(Worksheet worksheet) {
        return editWorksheet(tearableTabPane.getSelectedTabPane(), worksheet);
    }

    private boolean editWorksheet(TabPane targetTabPane, Worksheet worksheet) {
        var newTab = loadWorksheetInTab(worksheet, true);
        targetTabPane.getTabs().add(newTab);
        targetTabPane.getSelectionModel().select(newTab);
        return true;
    }

    private Image renderTextTooltip(String text) {
        var label = new Label("    " + text + "    ");
        label.getStyleClass().add("tooltip");
        // The label must be added to a scene so that CSS and layout are applied.
        StageAppearanceManager.getInstance().applyUiTheme(new Scene(label, Color.TRANSPARENT));
        return label.snapshot(null, null);
    }

    private Optional<TreeView<TimeSeriesBinding>> buildTreeViewForTarget(DataAdapter dp) {
        Objects.requireNonNull(dp, "DataAdapter instance provided to buildTreeViewForTarget cannot be null.");
        TreeView<TimeSeriesBinding> treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        Callback<TreeView<TimeSeriesBinding>, TreeCell<TimeSeriesBinding>> dragAndDropCellFactory = param -> {
            final TreeCell<TimeSeriesBinding> bindingTreeCell = new TreeCell<>();
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
                        treeView.getSelectionModel().getSelectedItems().forEach(s -> content.put(TIME_SERIES_BINDING_FORMAT, s.getValue().getTreeHierarchy()));
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
            FilterableTreeItem<TimeSeriesBinding> bindingTree = dp.getBindingTree();
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
            case SHIFT:
                GlobalPreferences.getInstance().setShiftPressed(pressed);
                event.consume();
                break;
            case CONTROL:
            case META:
            case SHORTCUT: // shortcut does not seem to register as Control on Windows here, so check them all.
                GlobalPreferences.getInstance().setCtrlPressed(pressed);
                event.consume();
                break;
            default:
                //do nothing
        }
    }

    private ContextMenu getTreeViewContextMenu(final TreeView<TimeSeriesBinding> treeView) {
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
        addToCurrent.disableProperty().bind(Bindings.size(tearableTabPane.getTabs()).lessThanOrEqualTo(0));
        addToCurrent.setOnShowing(event -> addToCurrent.getItems().setAll(getSelectedWorksheetController().getChartListContextMenu(treeView).getItems()));
        MenuItem addToNew = new MenuItem("Add to new worksheet");
        addToNew.setOnAction(event -> addToNewWorksheet(tearableTabPane.getSelectedTabPane(), treeView.getSelectionModel().getSelectedItems()));
        return new ContextMenu(expandBranch, collapseBranch, new SeparatorMenuItem(), addToCurrent, addToNew);
    }

    private void addToNewWorksheet(TabPane tabPane, Collection<TreeItem<TimeSeriesBinding>> rootItems) {
        // Schedule for later execution in order to let other drag and dropped event to complete before modal dialog gets displayed
        Platform.runLater(() -> {
            try {
                ZonedDateTime toDateTime;
                ZonedDateTime fromDateTime;
                ZoneId zoneId;
                if (getSelectedWorksheetController() != null && getSelectedWorksheetController().getWorksheet() != null) {
                    toDateTime = getSelectedWorksheetController().getWorksheet().getToDateTime();
                    fromDateTime = getSelectedWorksheetController().getWorksheet().getFromDateTime();
                    zoneId = getSelectedWorksheetController().getWorksheet().getTimeZone();
                } else {
                    toDateTime = ZonedDateTime.now();
                    fromDateTime = toDateTime.minus(24, ChronoUnit.HOURS);
                    zoneId = ZoneId.systemDefault();
                }
                WorksheetController.treeItemsAsChartList(rootItems, root).ifPresent(
                        charts -> editWorksheet(tabPane,
                                new Worksheet(StringUtils.ellipsize(rootItems.stream().map(t -> t.getValue().getLegend()).collect(Collectors.joining(", ")), 50),
                                        charts,
                                        zoneId,
                                        fromDateTime,
                                        toDateTime))
                );
            } catch (Exception e) {
                Dialogs.notifyException("Error adding bindings to new worksheet", e, root);
            }
        });
    }

    private void findNext() {
        if (isNullOrEmpty(searchField.getText())) {
            return;
        }
        TreeView<TimeSeriesBinding> selectedTreeView = getSelectedTreeView();
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
            searchField.setStyle("");
            currentSearchHit++;
            if (currentSearchHit > searchResultSet.size() - 1) {
                currentSearchHit = 0;
            }
            selectedTreeView.getSelectionModel().clearSelection();
            selectedTreeView.getSelectionModel().select(searchResultSet.get(currentSearchHit));
            selectedTreeView.scrollTo(selectedTreeView.getRow(searchResultSet.get(currentSearchHit)));
        } else {
            searchField.setStyle("-fx-background-color: #FF002040;");
        }
        logger.trace(() -> "Search for " + searchField.getText() + " yielded " + searchResultSet.size() + " match(es)");
    }

    private void invalidateSearchResults() {
        logger.trace("Invalidating search result");
        searchField.setStyle("");
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
                    workspace.addWorksheets(seriesControllers.get(tab).getWorksheet());
                    worksheetMenu.getItems().add(
                            getWorksheetMenuItem((EditableTab) tab,
                                    seriesControllers.get(tab).getWorksheet(),
                                    seriesControllers.get(tab).getBindingManager()));
                });
            }
            if (c.wasRemoved()) {
                c.getRemoved().forEach((t -> {
                    WorksheetController ctlr = seriesControllers.get(t);
                    // sever ref to allow collect
                    t.setContent(null);
                    t.setContextMenu(null);
                    if (ctlr != null) {
                        worksheetMenu.getItems()
                                .stream()
                                .filter(menuItem -> menuItem.getUserData() == ctlr.getWorksheet())
                                .findFirst()
                                .ifPresent(item -> worksheetMenu.getItems().remove(item));

                        workspace.removeWorksheets(ctlr.getWorksheet());
                        seriesControllers.remove(t);
                        ctlr.close();
                    } else {
                        logger.warn("Could not find a controller assigned to tab " + t.getText());
                    }
                }));
            }
        }
        logger.debug(() -> "Worksheets in current workspace: " +
                StreamSupport.stream(workspace.getWorksheets().spliterator(), false)
                        .map(Worksheet::getName)
                        .reduce((s, s2) -> s + " " + s2)
                        .orElse("null"));
    }

    private void onSourceTabChanged(ListChangeListener.Change<? extends TitledPane> c) {
        AtomicBoolean removed = new AtomicBoolean(false);
        while (c.next()) {
            c.getAddedSubList().forEach(t -> {
                workspace.addSource(sourcesAdapters.get(t));
            });
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
        return Optional.of(loadWorksheetInTab(new Worksheet(), true));
    }

    @FXML
    private void handleDragDroppedOnWorksheetArea(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(TIME_SERIES_BINDING_FORMAT)) {
            TreeView<TimeSeriesBinding> treeView = getSelectedTreeView();
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
            GlobalPreferences.getInstance().setWindowLastPosition(
                    new Rectangle2D(
                            stage.getX(),
                            stage.getY(),
                            stage.getWidth(),
                            stage.getHeight()));
        }
        UpdateManager.getInstance().startUpdate();
        Platform.exit();
    }

    public WorksheetController getSelectedWorksheetController() {
        Tab selectedTab = tearableTabPane.getSelectedTab();
        if (selectedTab == null) {
            return null;
        }
        return seriesControllers.get(selectedTab);
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

    public void handleToggleSourcePaneVisibility(ActionEvent actionEvent) {
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

    //endregion
}
