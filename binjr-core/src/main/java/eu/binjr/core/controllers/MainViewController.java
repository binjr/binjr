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

import eu.binjr.common.github.GithubRelease;
import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.common.javafx.controls.*;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterFactory;
import eu.binjr.core.data.adapters.DataAdapterInfo;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.workspace.*;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.dialogs.StageAppearanceManager;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.GlobalPreferences;
import eu.binjr.core.preferences.UpdateManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
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
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;

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
import java.util.function.Supplier;
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
    private static final String BINJR_FILE_PATTERN = "*.bjr";
    private static final double SEARCH_BAR_PANE_DISTANCE = 40;
    private static final double TOOL_BUTTON_SIZE = 20;
    //    private static final double COLLAPSED_WIDTH = 48;
//    private static final double EXPANDED_WIDTH = 200;
//    private static final int ANIMATION_DURATION = 50;
    public AnchorPane sourcePane;
    public MenuItem hideSourcePaneMenu;

    private Workspace workspace;
    private final Map<EditableTab, WorksheetController> seriesControllers = new WeakHashMap<>();
    private final Map<TitledPane, Source> sourcesAdapters = new WeakHashMap<>();
    private final BooleanProperty searchBarVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty searchBarHidden = new SimpleBooleanProperty(!searchBarVisible.get());
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
    public AnchorPane searchBarRoot;
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
//    private Timeline showTimeline;
//    private Timeline hideTimeline;

    private Property<TimeRangePicker.TimeRange> linkedTimeRange =
            new SimpleObjectProperty<>(TimeRangePicker.TimeRange.of(ZonedDateTime.now(), ZonedDateTime.now()));
    private Optional<String> associatedFile = Optional.empty();
    @FXML
    private MenuItem refreshMenuItem;
    @FXML
    private Accordion sourcesPane;
    @FXML
    private TearableTabPane worksheetTabPane;
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
        assert worksheetTabPane != null : "fx:id\"worksheetTabPane\" was not injected!";
        assert sourcesPane != null : "fx:id\"sourceTabPane\" was not injected!";
        assert saveMenuItem != null : "fx:id\"saveMenuItem\" was not injected!";
        assert openRecentMenu != null : "fx:id\"openRecentMenu\" was not injected!";
        assert contentView != null : "fx:id\"contentView\" was not injected!";

        Binding<Boolean> selectWorksheetPresent = Bindings.size(worksheetTabPane.getTabs()).isEqualTo(0);
        Binding<Boolean> selectedSourcePresent = Bindings.size(sourcesPane.getPanes()).isEqualTo(0);
        refreshMenuItem.disableProperty().bind(selectWorksheetPresent);
        sourcesPane.mouseTransparentProperty().bind(selectedSourcePresent);
        workspace.sourcePaneVisibleProperty().addListener((observable, oldValue, newValue) -> toggleSourcePaneVisibilty(newValue));
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
                        Platform.runLater(() -> {
                            sourcesPane.setExpandedPane(oldPane);
                        });
                    }
                });

        addWorksheetLabel.visibleProperty().bind(selectWorksheetPresent);
        worksheetTabPane.setNewTabFactory(this::worksheetTabFactory);
        worksheetTabPane.getGlobalTabs().addListener((ListChangeListener<? super Tab>) this::onWorksheetTabChanged);
        worksheetTabPane.setTearable(true);
        worksheetTabPane.setOnOpenNewWindow(event -> {
            Stage stage = (Stage) event.getSource();
            stage.setTitle(AppEnvironment.APP_NAME);
            registerStageKeyEvents(stage);
            StageAppearanceManager.getInstance().register(stage);
        });
        worksheetTabPane.setOnClosingWindow(event -> {
            StageAppearanceManager.getInstance().unregister((Stage) event.getSource());
            unregisterStageKeyEvents((Stage) event.getSource());
        });
        sourcesPane.getPanes().addListener(this::onSourceTabChanged);
        saveMenuItem.disableProperty().bind(workspace.dirtyProperty().not());
        commandBar.setSibling(contentView);

        worksheetArea.setOnDragOver(this::worksheetAreaOnDragOver);
        worksheetArea.setOnDragDropped(this::handleDragDroppedOnWorksheetArea);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                invalidateSearchResults();
                findNext();
            }
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
        searchCaseSensitiveToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            invalidateSearchResults();
            findNext();
        });
        this.addSourceMenu.getItems().addAll(populateSourceMenu());
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
                Platform.exit();
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
                    this::onAvailableUpdate, null, null
            );
        }
       // liftCurtains();
    }

    public void liftCurtains() {
        FadeTransition ft = new FadeTransition(Duration.millis(250), curtains);
        ft.setDelay(Duration.millis(400));
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.play();
        ft.setOnFinished(event -> {
            root.getChildren().remove(curtains);
        });
    }

    private void registerStageKeyEvents(Stage stage) {
        BindingManager manager = new BindingManager();
        stage.setUserData(manager);
        stage.addEventFilter(KeyEvent.KEY_RELEASED, manager.registerHandler(e -> {
            if (e.getCode() == KeyCode.F12) {
                AppEnvironment.getInstance().setDebugMode(!AppEnvironment.getInstance().isDebugMode());
            }
            if (e.getCode() == KeyCode.B && e.isControlDown()) {
                handleToggleChartLegendVisibility();
            }
            if (e.getCode() == KeyCode.P && e.isControlDown()) {
                handleDisplayChartProperties(null);
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
            Platform.exit();
        }
    }

    @FXML
    protected void handleRefreshAction(ActionEvent actionEvent) {
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

    private ButtonBase newToolBarButton(Supplier<ButtonBase> btnFactory,
                                        String text, String tooltipMsg,
                                        String[] styleClass,
                                        String[] iconStyleClass) {
        ButtonBase btn = btnFactory.get();
        btn.setText(text);
        btn.setPrefHeight(TOOL_BUTTON_SIZE);
        btn.setMaxHeight(TOOL_BUTTON_SIZE);
        btn.setMinHeight(TOOL_BUTTON_SIZE);
        btn.setPrefWidth(TOOL_BUTTON_SIZE);
        btn.setMaxWidth(TOOL_BUTTON_SIZE);
        btn.setMinWidth(TOOL_BUTTON_SIZE);
        btn.getStyleClass().addAll(styleClass);
        btn.setAlignment(Pos.CENTER);
        Region icon = new Region();
        icon.getStyleClass().addAll(iconStyleClass);
        btn.setGraphic(icon);
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        btn.setTooltip(new Tooltip(tooltipMsg));
        return btn;
    }

    private TitledPane newSourcePane(Source source) {
        TitledPane newPane = new TitledPane();
        Label label = new Label();
        source.getBindingManager().bind(label.textProperty(), source.nameProperty());
        GridPane titleRegion = new GridPane();
        titleRegion.setHgap(5);
        titleRegion.getColumnConstraints().add(
                new ColumnConstraints(45, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.LEFT, true));
        titleRegion.getColumnConstraints().add(
                new ColumnConstraints(45, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.NEVER, HPos.RIGHT, false));
        source.getBindingManager().bind(titleRegion.minWidthProperty(), newPane.widthProperty().subtract(30));
        source.getBindingManager().bind(titleRegion.maxWidthProperty(), newPane.widthProperty().subtract(30));

        // *** Toolbar ***
        HBox toolbar = new HBox();
        toolbar.getStyleClass().add("title-pane-tool-bar");
        toolbar.setAlignment(Pos.CENTER);
        Button closeButton = (Button) newToolBarButton(Button::new,
                "Close", "Close the connection to this source.",
                new String[]{"exit"},
                new String[]{"cross-icon", "small-icon"}
        );
        closeButton.setOnAction(event -> {
            if (Dialogs.confirmDialog(root, "Are you sure you want to remove source \"" + source.getName() + "\"?",
                    "WARNING: This will remove all associated series from existing worksheets.",
                    ButtonType.YES, ButtonType.NO) == ButtonType.YES) {
                sourcesPane.getPanes().remove(newPane);
            }
        });

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


        ToggleButton editButton = (ToggleButton) newToolBarButton(
                ToggleButton::new,
                "Settings", "Edit the source's settings",
                new String[]{"dialog-button"},
                new String[]{"settings-icon", "small-icon"});
        source.getBindingManager().bindBidirectional(editButton.selectedProperty(), source.editableProperty());
        editButton.setOnAction(event -> newPane.setExpanded(true));

        HBox.setHgrow(sourceNameField, Priority.ALWAYS);
        toolbar.getChildren().addAll(editButton, closeButton);
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
                    showAdapterDialog(DataAdapterFactory.getInstance().getDialog(adapterInfo.getKey(), root));
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
        return (TreeView<TimeSeriesBinding>) sourcesPane.getExpandedPane().getContent();
    }


    private void slidePanel(int show, Duration delay) {
        TranslateTransition openNav = new TranslateTransition(new Duration(200), searchBarRoot);
        openNav.setDelay(delay);
        openNav.setToY(show * -SEARCH_BAR_PANE_DISTANCE);
        openNav.play();
        openNav.setOnFinished(event -> AnchorPane.setBottomAnchor(sourceArea, show > 0 ? SEARCH_BAR_PANE_DISTANCE : 0));
    }

    private void expandBranch(TreeItem<TimeSeriesBinding> branch) {
        if (branch == null) {
            return;
        }
        branch.setExpanded(true);
        if (branch.getChildren() != null) {
            for (TreeItem<TimeSeriesBinding> item : branch.getChildren()) {
                expandBranch(item);
            }
        }
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
        worksheetTabPane.clearAllTabs();
        sourcesPane.getPanes().clear();
        //sourcesPane = new Accordion();
        seriesControllers.clear();
        sourcesAdapters.values().forEach(source -> {
            try {
                source.close();
            } catch (Exception e) {
                Dialogs.notifyException("Error closing Source", e, root);
            }
        });
        sourcesAdapters.clear();
        workspace.close();
        //   workspace = new Workspace();
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
                        workspace.setPath(file.toPath());
                        sourceMaskerPane.setVisible(false);
                        loadWorksheets((Workspace) event.getSource().getValue());
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
        fileChooser.setInitialFileName(BINJR_FILE_PATTERN);
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

    private void showAdapterDialog(Dialog<DataAdapter> dlg) {
        dlg.showAndWait().ifPresent(da -> {
            Source newSource = Source.of(da);
            TitledPane newSourcePane = newSourcePane(newSource);
            sourceMaskerPane.setVisible(true);
            AsyncTaskManager.getInstance().submit(() -> buildTreeViewForTarget(da),
                    event -> {
                        sourceMaskerPane.setVisible(false);
                        Optional<TreeView<TimeSeriesBinding>> treeView =
                                (Optional<TreeView<TimeSeriesBinding>>) event.getSource().getValue();
                        if (treeView.isPresent()) {
                            newSourcePane.setContent(treeView.get());
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
        });
    }

    private void loadSource(Source source) throws DataAdapterException {
        TitledPane newSourcePane = newSourcePane(source);
        Optional<TreeView<TimeSeriesBinding>> treeView;
        treeView = buildTreeViewForTarget(source.getAdapter());
        if (treeView.isPresent()) {
            newSourcePane.setContent(treeView.get());
            sourcesAdapters.put(newSourcePane, source);
        } else {
            TreeItem<TimeSeriesBinding> i = new TreeItem<>();
            i.setValue(new TimeSeriesBinding());
            Label l = new Label("<Failed to connect to \"" + source.getName() + "\">");
            l.setTextFill(Color.RED);
            i.setGraphic(l);
            newSourcePane.setContent(new TreeView<>(i));
        }
        Platform.runLater(() -> {
            sourcesPane.getPanes().add(newSourcePane);
            newSourcePane.setExpanded(true);
        });
    }

    private boolean loadWorksheet(Worksheet worksheet) {
        EditableTab newTab = loadWorksheetInTab(worksheet, false);
        worksheetTabPane.getTabs().add(newTab);
        worksheetTabPane.getSelectionModel().select(newTab);
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
        if (tab == null) {
            throw new IllegalStateException("cannot find associated tab or WorksheetController for " + worksheetCtrl.getName());
        }
        seriesControllers.remove(tab);
        tab.setContent(null);
        Worksheet worksheet = worksheetCtrl.getWorksheet();
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

                p.setOnDragOver(current.getBindingManager().registerHandler(this::handleDragOverWorksheetView));
                //p.setOnDragOver(this::handleDragOverWorksheetView);
                p.setOnDragDropped(current.getBindingManager().registerHandler(this::handleDragDroppedOnWorksheetView));
                //   p.setOnDragDropped(this::handleDragDroppedOnWorksheetView);
            } catch (IOException ex) {
                logger.error("Error loading time series", ex);
            }
            seriesControllers.put(newTab, current);
            current.getBindingManager().attachListener(current.getWorksheet().timeRangeLinkedProperty(),
                    (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                        if (newValue) {
                            current.getBindingManager().bindBidirectional(current.selectedRangeProperty(), linkedTimeRange);
                        } else {
                            current.getBindingManager().unbindBidirectionnal(current.selectedRangeProperty(), linkedTimeRange);
                        }
                    });
            if (current.getWorksheet().isTimeRangeLinked()) {
                current.getBindingManager().bindBidirectional(current.selectedRangeProperty(), linkedTimeRange);
            }
            current.getBindingManager().bindBidirectional(newTab.nameProperty(), worksheet.nameProperty());
            if (setToEditMode) {
                logger.trace("Toggle edit mode for worksheet");
                current.setShowPropertiesPane(true);
            }
            newTab.setContextMenu(getTabMenu(newTab, worksheet, current.getBindingManager()));
            return current;
        } catch (Exception e) {
            Dialogs.notifyException("Error loading worksheet into new tab", e, root);
            return null;
        }
    }

    private EditableTab loadWorksheetInTab(Worksheet worksheet, boolean editMode) {
        Button closeTabButton = (Button) newToolBarButton(
                Button::new,
                "Close", "Close Tab",
                new String[]{"exit"},
                new String[]{"cross-icon", "small-icon"});
        EditableTab newTab = new EditableTab("New worksheet", closeTabButton);
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

    private ContextMenu getTabMenu(EditableTab tab, Worksheet worksheet, BindingManager manager) {
        MenuItem close = new MenuItem("Close Tab");
        close.setOnAction(manager.registerHandler(event -> closeWorksheetTab(tab)));
        MenuItem closeOthers = new MenuItem("Close Other Tabs");
        closeOthers.setOnAction(manager.registerHandler(event -> {
            if (Dialogs.confirmDialog(tab.getTabPane(), "Are you sure you want to close all tabs except for '" + tab.getName() + "'?",
                    "", ButtonType.YES, ButtonType.NO) == ButtonType.YES) {
                var tabs = tab.getTabPane().getTabs();
                tabs.removeAll(tabs.stream()
                        .filter(tab1 -> !tab1.equals(tab))
                        .collect(Collectors.toList())
                );
            }
        }));
        MenuItem edit = new MenuItem("Edit Tab");
        edit.setOnAction(manager.registerHandler(event -> tab.setEditable(true)));
        MenuItem duplicate = new MenuItem("Duplicate Tab");
        duplicate.setOnAction(manager.registerHandler(event -> {
            editWorksheet(new Worksheet(worksheet));
        }));
        MenuItem detach = new MenuItem("Detach Tab");
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
        return new ContextMenu(
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
        TabPane targetTabPane = worksheetTabPane.getSelectedTabPane();
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
        Callback<TreeView<TimeSeriesBinding>, TreeCell<TimeSeriesBinding>> dragAndDropCellFactory = param -> {
            final TreeCell<TimeSeriesBinding> bindingTreeCell = new TreeCell<>();
            bindingTreeCell.itemProperty().addListener((observable, oldValue, newValue) -> bindingTreeCell.setText(newValue == null ? null : newValue.toString()));
            bindingTreeCell.setOnDragDetected(event -> {
                try {
                    if (bindingTreeCell.getItem() != null) {
                        expandBranch(bindingTreeCell.getTreeItem());
                        Dragboard db = bindingTreeCell.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                        db.setDragView(renderTextTooltip(bindingTreeCell.getItem().toString()));
                        ClipboardContent content = new ClipboardContent();
                        content.put(TIME_SERIES_BINDING_FORMAT, bindingTreeCell.getItem().getTreeHierarchy());
                        db.setContent(content);
                    } else {
                        logger.debug("No TreeItem selected: canceling drag and drop");
                    }
                } finally {
                    event.consume();
                }
            });
            return bindingTreeCell;
        };
        treeView.setCellFactory(ContextMenuTreeViewCell.forTreeView(getTreeViewContextMenu(treeView), dragAndDropCellFactory));
        try {
            dp.onStart();
            TreeItem<TimeSeriesBinding> bindingTree = dp.getBindingTree();
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

    <T> void getAllBindingsFromBranch(TreeItem<T> branch, List<T> bindings) {
        if (branch.getChildren().size() > 0) {
            for (TreeItem<T> t : branch.getChildren()) {
                getAllBindingsFromBranch(t, bindings);
            }
        } else {
            bindings.add(branch.getValue());
        }
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

    private ContextMenu getChartListContextMenu(final TreeView<TimeSeriesBinding> treeView) {
        ContextMenu contextMenu = new ContextMenu(getSelectedWorksheetController().getWorksheet().getCharts()
                .stream()
                .map(c -> {
                    MenuItem m = new MenuItem(c.getName());
                    m.setOnAction(e -> addToCurrentWorksheet(treeView.getSelectionModel().getSelectedItem(), c));
                    return m;
                })
                .toArray(MenuItem[]::new));

        MenuItem newChart = new MenuItem("Add to new chart");
        newChart.setOnAction(event -> addToNewChartInCurrentWorksheet(treeView.getSelectionModel().getSelectedItem()));
        contextMenu.getItems().addAll(new SeparatorMenuItem(), newChart);
        return contextMenu;
    }

    private ContextMenu getTreeViewContextMenu(final TreeView<TimeSeriesBinding> treeView) {
        Menu addToCurrent = new Menu("Add to current worksheet", null, new MenuItem("none"));
        addToCurrent.disableProperty().bind(Bindings.size(worksheetTabPane.getTabs()).lessThanOrEqualTo(0));
        addToCurrent.setOnShowing(event -> addToCurrent.getItems().setAll(getChartListContextMenu(treeView).getItems()));
        MenuItem addToNew = new MenuItem("Add to new worksheet");
        addToNew.setOnAction(event -> addToNewWorksheet(treeView.getSelectionModel().getSelectedItem()));
        ContextMenu contextMenu = new ContextMenu(addToCurrent, addToNew);
        contextMenu.setOnShowing(event -> expandBranch(treeView.getSelectionModel().getSelectedItem()));
        return contextMenu;
    }

    private void addToNewChartInCurrentWorksheet(TreeItem<TimeSeriesBinding> treeItem) {
        try {
            Worksheet worksheet = getSelectedWorksheetController().getWorksheet();
            TimeSeriesBinding binding = treeItem.getValue();
            Chart chart = new Chart(
                    binding.getLegend(),
                    binding.getGraphType(),
                    binding.getUnitName(),
                    binding.getUnitPrefix()
            );
            List<TimeSeriesBinding> bindings = new ArrayList<>();
            getAllBindingsFromBranch(treeItem, bindings);
            for (TimeSeriesBinding b : bindings) {
                chart.addSeries(TimeSeriesInfo.fromBinding(b));
            }
            worksheet.getCharts().add(chart);
        } catch (Exception e) {
            Dialogs.notifyException("Error adding bindings to new chart", e, root);
        }
    }

    private void addToCurrentWorksheet(TreeItem<TimeSeriesBinding> treeItem, Chart targetChart) {
        try {
            if (getSelectedWorksheetController() != null && treeItem != null) {
                List<TimeSeriesBinding> bindings = new ArrayList<>();
                getAllBindingsFromBranch(treeItem, bindings);
                getSelectedWorksheetController().addBindings(bindings, targetChart);
            }
        } catch (Exception e) {
            Dialogs.notifyException("Error adding bindings to existing worksheet", e, root);
        }
    }

    private void addToNewWorksheet(TreeItem<TimeSeriesBinding> treeItem) {
        // Schedule for later execution in order to let other UI components worksheetTabFactory refreshed
        // before modal dialog gets displayed (fixes unsightly UI glitches on Linux)
        Platform.runLater(() -> {
            try {
                TimeSeriesBinding binding = treeItem.getValue();
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

                List<Chart> chartList = new ArrayList<>();
                chartList.add(new Chart(
                        binding.getLegend(),
                        binding.getGraphType(),
                        binding.getUnitName(),
                        binding.getUnitPrefix()
                ));
                Worksheet worksheet = new Worksheet(binding.getLegend(),
                        chartList,
                        zoneId,
                        fromDateTime,
                        toDateTime
                );
                if (editWorksheet(worksheet) && getSelectedWorksheetController() != null) {
                    List<TimeSeriesBinding> bindings = new ArrayList<>();
                    getAllBindingsFromBranch(treeItem, bindings);
                    getSelectedWorksheetController().addBindings(bindings, getSelectedWorksheetController().getWorksheet().getDefaultChart());
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
            selectedTreeView.getSelectionModel().select(searchResultSet.get(currentSearchHit));
            selectedTreeView.scrollTo(selectedTreeView.getRow(searchResultSet.get(currentSearchHit)));
        } else {
            searchField.setStyle("-fx-background-color: #ffcccc;");
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
                workspace.addWorksheets(c.getAddedSubList().stream().map(t -> seriesControllers.get(t).getWorksheet()).collect(Collectors.toList()));
            }
            if (c.wasRemoved()) {
                c.getRemoved().forEach((t -> {
                    WorksheetController ctlr = seriesControllers.get(t);
                    // sever ref to allow collect
                    t.setContent(null);
                    t.setContextMenu(null);
                    if (ctlr != null) {
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

    private void handleDragDroppedOnWorksheetArea(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(TIME_SERIES_BINDING_FORMAT)) {
            TreeView<TimeSeriesBinding> treeView = getSelectedTreeView();
            if (treeView != null) {
                TreeItem<TimeSeriesBinding> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null) {
                    addToNewWorksheet(item);
                } else {
                    logger.warn("Cannot complete drag and drop operation: selected TreeItem is null");
                }
            } else {
                logger.warn("Cannot complete drag and drop operation: selected TreeView is null");
            }
            event.consume();
        }
    }

    private void onAvailableUpdate(GithubRelease githubRelease) {
        Notifications n = Notifications.create()
                .title("New release available!")
                .text("You are currently running " + AppEnvironment.APP_NAME + " version " +
                        AppEnvironment.getInstance().getVersion() +
                        "\t\t.\nVersion " + githubRelease.getVersion() + " is now available.")
                .hideAfter(Duration.seconds(20))
                .position(Pos.BOTTOM_RIGHT)
                .owner(root);
        n.action(new Action("Download", actionEvent -> {
            String newReleaseUrl = githubRelease.getHtmlUrl();
            if (newReleaseUrl != null && newReleaseUrl.trim().length() > 0) {
                try {
                    Dialogs.launchUrlInExternalBrowser(newReleaseUrl);
                } catch (IOException | URISyntaxException e) {
                    logger.error("Failed to launch url in browser " + newReleaseUrl, e);
                }
            }
            n.hideAfter(Duration.seconds(0));
        }));
        n.showInformation();
    }

    private void handleDragOverWorksheetView(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(TIME_SERIES_BINDING_FORMAT)) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        }
    }

    private void handleDragDroppedOnWorksheetView(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(TIME_SERIES_BINDING_FORMAT)) {
            TreeView<TimeSeriesBinding> treeView = getSelectedTreeView();
            if (treeView != null) {
                TreeItem<TimeSeriesBinding> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null) {
                    Stage targetStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    if (targetStage != null) {
                        targetStage.requestFocus();
                    }
                    if (TransferMode.COPY.equals(event.getAcceptedTransferMode())) {
                        addToNewChartInCurrentWorksheet(item);
                    } else if (TransferMode.MOVE.equals(event.getAcceptedTransferMode())) {
                        if (getSelectedWorksheetController().getWorksheet().getCharts().size() > 1) {
                            getChartListContextMenu(treeView).show((Node) event.getTarget(), event.getScreenX(), event.getSceneY());
                        } else {
                            addToCurrentWorksheet(treeView.getSelectionModel().getSelectedItem(), getSelectedWorksheetController().getWorksheet().getDefaultChart());
                        }
                    } else {
                        logger.warn("Unsupported drag and drop transfer mode: " + event.getAcceptedTransferMode());
                    }
                } else {
                    logger.warn("Cannot complete drag and drop operation: selected TreeItem is null");
                }
            } else {
                logger.warn("Cannot complete drag and drop operation: selected TreeView is null");
            }
            event.consume();
        }
    }

    public WorksheetController getSelectedWorksheetController() {
        Tab selectedTab = worksheetTabPane.getSelectedTab();
        if (selectedTab == null) {
            return null;
        }
        return seriesControllers.get(selectedTab);
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
            contentView.setDividerPositions(0.0, 1.0);
            hideSourcePaneMenu.setText("Show Source Pane");
            sourcePane.setVisible(false);
            sourcePane.setMaxWidth(0.0);
        } else {
            sourcePane.setMaxWidth(Double.MAX_VALUE);
            sourcePane.setVisible(true);
            hideSourcePaneMenu.setText("Hide Source Pane");
            contentView.setDividerPositions(0.35, 0.65);

        }
    }

    public void handleToggleChartLegendVisibility() {
        if (getSelectedWorksheetController() != null) {
            var worksheet = getSelectedWorksheetController().getWorksheet();
            if (worksheet != null) {
                worksheet.setChartLegendsVisible(!worksheet.isChartLegendsVisible());
            }
        }
    }

    //endregion
}
