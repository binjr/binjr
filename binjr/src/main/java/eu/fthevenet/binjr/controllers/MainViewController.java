/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.adapters.exceptions.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.exceptions.NoAdapterFoundException;
import eu.fthevenet.binjr.data.async.AsyncTaskManager;
import eu.fthevenet.binjr.data.workspace.Source;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;
import eu.fthevenet.binjr.data.workspace.Worksheet;
import eu.fthevenet.binjr.data.workspace.Workspace;
import eu.fthevenet.binjr.dialogs.DataAdapterDialog;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.dialogs.EditWorksheetDialog;
import eu.fthevenet.binjr.dialogs.UserInterfaceThemes;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.binjr.sources.jrds.adapters.JrdsAdapterDialog;
import eu.fthevenet.util.ui.controls.*;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The controller class for the main view
 *
 * @author Frederic Thevenet
 */
public class MainViewController implements Initializable {
    public static final int settingsPaneDistance = 250;
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    private static final DataFormat TIME_SERIES_BINDING_FORMAT = new DataFormat("TimeSeriesBindingFormat");
    private final Workspace workspace;
    private final Map<Tab, WorksheetController> seriesControllers = new HashMap<>();
    private final Map<Tab, DataAdapter> sourcesAdapters = new HashMap<>();
    @FXML
    public CommandBarPane commandBar;
    @FXML
    public AnchorPane root;
    @FXML
    public Label addWorksheetLabel;
    @FXML
    public MenuItem chartPropertiesMenuItem;
    @FXML
    public MaskerPane sourceMaskerPane;
    @FXML
    public MaskerPane worksheetMaskerPane;
    @FXML
    private MenuItem refreshMenuItem;
    @FXML
    private TabPaneNewButton sourcesTabPane;
    @FXML
    private TabPaneNewButton worksheetTabPane;
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

    private WorksheetController selectedWorksheetController;
    private double collapsedWidth = 48;
    private double expandedWidth = 200;
    private int animationDuration = 50;
    private Timeline showTimeline;
    private Timeline hideTimeline;
    private DoubleProperty commandBarWidth = new SimpleDoubleProperty(0.2);

    public MainViewController() {
        super();
        this.workspace = new Workspace();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert root != null : "fx:id\"root\" was not injected!";
        assert worksheetTabPane != null : "fx:id\"worksheetTabPane\" was not injected!";
        assert sourcesTabPane != null : "fx:id\"sourceTabPane\" was not injected!";
        assert saveMenuItem != null : "fx:id\"saveMenuItem\" was not injected!";
        assert openRecentMenu != null : "fx:id\"openRecentMenu\" was not injected!";
        assert contentView != null : "fx:id\"contentView\" was not injected!";

        GlobalPreferences prefs = GlobalPreferences.getInstance();
        prefs.userInterfaceThemeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                setUiTheme(newValue);
            }
        });
        setUiTheme(prefs.getUserInterfaceTheme());

        Binding<Boolean> selectWorksheetPresent = Bindings.size(worksheetTabPane.getTabs()).isEqualTo(0);
        Binding<Boolean> selectedSourcePresent = Bindings.size(sourcesTabPane.getTabs()).isEqualTo(0);
        refreshMenuItem.disableProperty().bind(selectWorksheetPresent);
        chartPropertiesMenuItem.disableProperty().bind(selectWorksheetPresent);
        sourcesTabPane.mouseTransparentProperty().bind(selectedSourcePresent);
        worksheetTabPane.mouseTransparentProperty().bind(selectWorksheetPresent);

        worksheetTabPane.setNewTabFactory(() -> {
            AtomicBoolean wasNewTabCreated = new AtomicBoolean(false);
            EditableTab newTab = new EditableTab("");
            new EditWorksheetDialog<>(new Worksheet<>(), root).showAndWait().ifPresent(w -> {
                loadWorksheet(w, newTab);
                wasNewTabCreated.set(true);
            });
            return wasNewTabCreated.get() ? Optional.of(newTab) : Optional.empty();
        });

        worksheetTabPane.getTabs().addListener((ListChangeListener<? super Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    workspace.addWorksheets(c.getAddedSubList().stream().map(t -> seriesControllers.get(t).getWorksheet()).collect(Collectors.toList()));
                }
                if (c.wasRemoved()) {
                    c.getRemoved().forEach((t -> {
                        WorksheetController ctlr = seriesControllers.get(t);
                        if (ctlr != null) {
                            workspace.removeWorksheets(ctlr.getWorksheet());
                            seriesControllers.remove(t);
                            ctlr.close();
                        }
                        else {
                            logger.warn("Could not find a controller assigned to tab " + t.getText());
                        }
                    }));
                }
            }
            logger.debug(() -> "Worksheets in current workspace: " + StreamSupport.stream(workspace.getWorksheets().spliterator(), false).map(Worksheet::getName).reduce((s, s2) -> s + " " + s2).orElse("null"));
        });

        sourcesTabPane.getTabs().addListener((ListChangeListener<? super Tab>) c -> {
            workspace.clearSources();
            workspace.addSources(c.getList()
                    .stream()
                    .filter(t -> sourcesAdapters.get(t) != null)
                    .map((t) -> Source.of(sourcesAdapters.get(t)))
                    .collect(Collectors.toList()));
            logger.debug(() -> "Sources in current workspace: " + StreamSupport.stream(workspace.getSources().spliterator(), false).map(Source::getName).reduce((s, s2) -> s + " " + s2).orElse("null"));
        });

        sourcesTabPane.setOnNewTabAction(this::handleAddJrdsSource);

        worksheetTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.selectedWorksheetController = seriesControllers.get(newValue);
            }
        });

        saveMenuItem.disableProperty().bind(workspace.dirtyProperty().not());

        worksheetArea.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(TIME_SERIES_BINDING_FORMAT)) {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            }
        });

        worksheetArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(TIME_SERIES_BINDING_FORMAT)) {
                TreeView<TimeSeriesBinding<Double>> treeView = getSelectedTreeView();
                if (treeView != null) {
                    TreeItem<TimeSeriesBinding<Double>> item = treeView.getSelectionModel().getSelectedItem();
                    if (item != null) {
                        addToNewWorksheet(item);
                    }
                    else {
                        logger.warn("Cannot complete drag and drop operation: selected TreeItem is null");
                    }
                }
                else {
                    logger.warn("Cannot complete drag and drop operation: selected TreeView is null");
                }
                event.consume();
            }
        });

        Platform.runLater(() -> {
            Stage stage = Dialogs.getStage(root);
            stage.titleProperty().bind(Bindings.createStringBinding(
                    () -> String.format("%s%s - binjr", (workspace.isDirty() ? "*" : ""), workspace.pathProperty().getValue().toString()),
                    workspace.pathProperty(),
                    workspace.dirtyProperty()));

            stage.setOnCloseRequest(event -> {
                if (!confirmAndClearWorkspace()) {
                    event.consume();
                }
            });
            stage.addEventFilter(KeyEvent.KEY_PRESSED, e -> handleControlKey(e, true));
            stage.addEventFilter(KeyEvent.KEY_RELEASED, e -> handleControlKey(e, false));
            stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
                //main stage lost focus -> invalidates shift or ctrl pressed
                prefs.setShiftPressed(false);
                prefs.setCtrlPressed(false);
            });
            if (prefs.isLoadLastWorkspaceOnStartup()) {
                File latestWorkspace = prefs.getMostRecentSavedWorkspace().toFile();
                if (latestWorkspace.exists()) {
                    loadWorkspace(latestWorkspace);
                }
                else {
                    logger.warn("Cannot reopen workspace " + latestWorkspace.getPath() + ": file does not exists");
                }
            }
            if (prefs.isCheckForUpdateOnStartUp() && (LocalDateTime.now().minus(1, ChronoUnit.HOURS).isAfter(prefs.getLastCheckForUpdate()))) {
                prefs.setLastCheckForUpdate(LocalDateTime.now());
                prefs.asyncCheckForUpdate(
                        githubRelease -> {
                            Notifications n = Notifications.create()
                                    .title("New release available!")
                                    .text("You are currently running binjr version " + prefs.getManifestVersion() + "\t\t.\nVersion " + githubRelease.getVersion() + " is now available.")
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
                );
            }
        });
        commandBarWidth.addListener((observable, oldValue, newValue) -> {
            doCommandBarResize(newValue.doubleValue());

        });
    }

    private TreeView<TimeSeriesBinding<Double>> getSelectedTreeView() {
        return (TreeView<TimeSeriesBinding<Double>>) sourcesTabPane.getSelectionModel().getSelectedItem().getContent();
    }


    //region UI handlers
    @FXML
    protected void handleAboutAction(ActionEvent event) throws IOException {
        Dialog<String> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("About binjr");
        dialog.setDialogPane(FXMLLoader.load(getClass().getResource("/views/AboutBoxView.fxml")));
        dialog.initOwner(Dialogs.getStage(root));
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
        if (selectedWorksheetController != null) {
            selectedWorksheetController.refresh();
        }
    }

    @FXML
    protected void handlePreferencesAction(ActionEvent actionEvent) {
        try {
            TranslateTransition openNav = new TranslateTransition(new Duration(350), settingsPane);
            openNav.setToX(settingsPaneDistance);
            openNav.play();

        } catch (Exception ex) {
            Dialogs.notifyException("Failed to display preference dialog", ex, root);
        }
    }

    @FXML
    public void handleExpandCommandBar(ActionEvent actionEvent) {
        if (!commandBar.isExpanded()) {
            show();
        }
        else {
            hide();
        }

        commandBar.setExpanded(!commandBar.isExpanded());
    }

    @FXML
    protected void handleAddNewWorksheet(Event event) {
        editWorksheet(new Worksheet<>());
    }

    @FXML
    protected void handleAddJrdsSource(Event actionEvent) {
        DataAdapterDialog dlg = new JrdsAdapterDialog(root);
        Tab newTab = new Tab();
        getAdapterDlg(newTab);
    }

    @FXML
    protected void handleHelpAction(ActionEvent event) {
        try {
            Dialogs.launchUrlInExternalBrowser(GlobalPreferences.HTTP_BINJR_WIKI);
        } catch (IOException | URISyntaxException e) {
            logger.error(e);
        }
    }

    @FXML
    protected void handleLatestReleaseAction(ActionEvent event) {
        try {
            Dialogs.launchUrlInExternalBrowser(GlobalPreferences.HTTP_LATEST_RELEASE);
        } catch (IOException | URISyntaxException e) {
            logger.error(e);
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
    protected void handleSaveWorkspace(ActionEvent event) {
        saveWorkspace();
    }

    @FXML
    protected void handleSaveAsWorkspace(ActionEvent event) {
        saveWorkspaceAs();
    }

    @FXML
    protected void handleDisplayChartProperties(ActionEvent actionEvent) {
        if (selectedWorksheetController != null) {
            selectedWorksheetController.showPropertiesPane(true);
        }
    }
    //endregion


    //region private members

    private void show() {
        if (hideTimeline != null) {
            hideTimeline.stop();
        }
        if (showTimeline != null && showTimeline.getStatus() == Animation.Status.RUNNING) {
            return;
        }
        Duration duration = Duration.millis(animationDuration);
        KeyFrame keyFrame = new KeyFrame(duration, new KeyValue(commandBarWidth, expandedWidth));
        showTimeline = new Timeline(keyFrame);
        showTimeline.setOnFinished(event -> new DelayedAction(Duration.millis(50), () -> AnchorPane.setLeftAnchor(contentView, expandedWidth)).submit());
        showTimeline.play();
    }

    private void hide() {
        if (showTimeline != null) {
            showTimeline.stop();
        }
        if (hideTimeline != null && hideTimeline.getStatus() == Animation.Status.RUNNING) {
            return;
        }
        if (commandBarWidth.get() <= collapsedWidth) {
            return;
        }
        Duration duration = Duration.millis(animationDuration);
        hideTimeline = new Timeline(new KeyFrame(duration, new KeyValue(commandBarWidth, collapsedWidth)));
        AnchorPane.setLeftAnchor(contentView, collapsedWidth);
        hideTimeline.play();
    }

    private void doCommandBarResize(double v) {
        commandBar.setMinWidth(v);
    }

    private void expandBranch(TreeItem<TimeSeriesBinding<Double>> branch) {
        if (branch == null) {
            return;
        }
        branch.setExpanded(true);
        if (branch.getChildren() != null) {
            for (TreeItem<TimeSeriesBinding<Double>> item : branch.getChildren()) {
                expandBranch(item);
            }
        }
    }

    private boolean confirmAndClearWorkspace() {
        if (!workspace.isDirty()) {
            clearWorkspace();
            return true;
        }
        ButtonType res = Dialogs.confirmSaveDialog(root, (workspace.hasPath() ? workspace.getPath().getFileName().toString() : "Untitled"));
        if (res == ButtonType.CANCEL) {
            return false;
        }
        if (res == ButtonType.YES) {
            if (!saveWorkspace()) {
                return false;
            }
        }
        clearWorkspace();
        return true;
    }

    private void clearWorkspace() {
        worksheetTabPane.getTabs().clear();
        sourcesTabPane.getTabs().clear();
        workspace.clear();
    }

    private void openWorkspaceFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Workspace");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("binjr workspaces", "*.bjr"));
        fileChooser.setInitialDirectory(new File(GlobalPreferences.getInstance().getMostRecentSaveFolder()));
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
                            DataAdapter da = (DataAdapter) source.getAdapterClass().newInstance();
                            da.setParams(source.getAdapterParams());
                            da.setId(source.getAdapterId());
                            loadAdapters(da);
                        }
                        return wsFromfile;
                    },
                    event -> {
                        workspace.setPath(file.toPath());
                        sourceMaskerPane.setVisible(false);
                        loadWorksheets((Workspace) event.getSource().getValue());
                    }, event -> {
                        sourceMaskerPane.setVisible(false);
                        Dialogs.notifyException("An error occurred while loading workspace from file " + (file != null ? file.getName() : "null"),
                                event.getSource().getException(),
                                root);
                    });
        }
    }

    private void loadWorksheets(Workspace wsFromfile) {
        try {
            for (Worksheet<Double> worksheet : wsFromfile.getWorksheets()) {
                for (TimeSeriesInfo<?> s : worksheet.getSeries()) {
                    UUID id = s.getBinding().getAdapterId();
                    DataAdapter<?> da = sourcesAdapters.values()
                            .stream()
                            .filter(a -> (id != null && a != null && a.getId() != null) && id.equals(a.getId()))
                            .findAny()
                            .orElseThrow(() -> new NoAdapterFoundException("Failed to find a valid adapter with id " + (id != null ? id.toString() : "null")));
                    s.getBinding().setAdapter(da);
                    s.selectedProperty().addListener((observable, oldValue, newValue) -> selectedWorksheetController.refresh());
                }
                loadWorksheet(worksheet);
            }
            workspace.cleanUp();
            GlobalPreferences.getInstance().putToRecentFiles(workspace.getPath().toString());
            logger.debug(() -> "Recently loaded workspaces: " + GlobalPreferences.getInstance().getRecentFiles().stream().collect(Collectors.joining(" ")));

        } catch (DataAdapterException e) {
            Dialogs.notifyException("Error while instantiating DataAdapter", e, root);
        } catch (Exception e) {
            Dialogs.notifyException("Error loading workspace", e, root);
        }
    }

    private boolean saveWorkspace() {
        try {
            if (workspace.hasPath()) {
                workspace.save();
                return true;
            }
            else {
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("binjr workspaces", "*.bjr"));
        fileChooser.setInitialDirectory(new File(GlobalPreferences.getInstance().getMostRecentSaveFolder()));
        fileChooser.setInitialFileName("*.bjr");
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

    private void getAdapterDlg(Tab newTab) {
        DataAdapterDialog dlg = new JrdsAdapterDialog(root);
        dlg.showAndWait().ifPresent(da -> {
            newTab.setText(da.getSourceName());
            sourceMaskerPane.setVisible(true);
            AsyncTaskManager.getInstance().submit(() -> buildTreeViewForTarget(da),
                    event -> {
                        sourceMaskerPane.setVisible(false);
                        Optional<TreeView<TimeSeriesBinding<Double>>> treeView = (Optional<TreeView<TimeSeriesBinding<Double>>>) event.getSource().getValue();
                        if (treeView.isPresent()) {
                            newTab.setContent(treeView.get());
                            sourcesAdapters.put(newTab, da);
                            sourcesTabPane.getTabs().add(newTab);
                            sourcesTabPane.getSelectionModel().select(newTab);
                        }
                    },
                    event -> sourceMaskerPane.setVisible(false));
        });
    }

    private void loadAdapters(DataAdapter da) throws DataAdapterException {
        Tab newTab = new Tab();
        newTab.setText(da.getSourceName());
        Optional<TreeView<TimeSeriesBinding<Double>>> treeView;
        treeView = buildTreeViewForTarget(da);
        if (treeView.isPresent()) {
            newTab.setContent(treeView.get());
            sourcesAdapters.put(newTab, da);
        }
        else {
            TreeItem<TimeSeriesBinding<Double>> i = new TreeItem<>();
            i.setValue(new TimeSeriesBinding<>());
            Label l = new Label("<Failed to connect to \"" + da.getSourceName() + "\">");
            l.setTextFill(Color.RED);
            i.setGraphic(l);
            newTab.setContent(new TreeView<>(i));
        }
        Platform.runLater(() -> {
            sourcesTabPane.getTabs().add(newTab);
            sourcesTabPane.getSelectionModel().select(newTab);
        });
    }

    private boolean loadWorksheet(Worksheet<Double> worksheet) {
        EditableTab newTab = new EditableTab("New worksheet");
        loadWorksheet(worksheet, newTab);
        worksheetTabPane.getTabs().add(newTab);
        worksheetTabPane.getSelectionModel().select(newTab);
        return false;
    }

    private void loadWorksheet(Worksheet<Double> worksheet, EditableTab newTab) {
        try {
            WorksheetController current;
            switch (worksheet.getChartType()) {
                case AREA:
                    current = new AreaChartWorksheetController(worksheet);
                    break;
                case STACKED:
                    current = new StackedAreaChartWorksheetController(worksheet);
                    break;
                case LINE:
                    current = new LineChartWorksheetController(worksheet);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported chart");
            }
            try {
                FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/views/WorksheetView.fxml"));
                fXMLLoader.setController(current);
                Parent p = fXMLLoader.load();
                newTab.setContent(p);
                p.setOnDragOver(event -> {
                    Dragboard db = event.getDragboard();
                    if (db.hasContent(TIME_SERIES_BINDING_FORMAT)) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                        event.consume();
                    }
                });

                p.setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    if (db.hasContent(TIME_SERIES_BINDING_FORMAT)) {
                        TreeView<TimeSeriesBinding<Double>> treeView = getSelectedTreeView();
                        if (treeView != null) {
                            TreeItem<TimeSeriesBinding<Double>> item = treeView.getSelectionModel().getSelectedItem();
                            if (item != null) {
                                if (TransferMode.COPY.equals(event.getAcceptedTransferMode())) {
                                    addToNewWorksheet(item);
                                }
                                else if (TransferMode.MOVE.equals(event.getAcceptedTransferMode())) {
                                    addToCurrentWorksheet(item);
                                }
                                else {
                                    logger.warn("Unsupported drag and drop tansfert mode: " + event.getAcceptedTransferMode());
                                }
                            }
                            else {
                                logger.warn("Cannot complete drag and drop operation: selected TreeItem is null");
                            }
                        }
                        else {
                            logger.warn("Cannot complete drag and drop operation: selected TreeView is null");
                        }
                        event.consume();
                    }
                });

            } catch (IOException ex) {
                logger.error("Error loading time series", ex);
            }
            selectedWorksheetController = current;
            seriesControllers.put(newTab, current);
            newTab.nameProperty().bindBidirectional(worksheet.nameProperty());
        } catch (Exception e) {
            Dialogs.notifyException("Error loading worksheet into new tab", e);
        }
    }

    private boolean editWorksheet(Worksheet<Double> worksheet) {
        AtomicBoolean wasNewTabCreated = new AtomicBoolean(false);
        EditWorksheetDialog<Double> dlg = new EditWorksheetDialog<>(worksheet, root);
        dlg.showAndWait().ifPresent(w -> {
            EditableTab newTab = new EditableTab("");
            loadWorksheet(w, newTab);
            worksheetTabPane.getTabs().add(newTab);
            worksheetTabPane.getSelectionModel().select(newTab);
            wasNewTabCreated.set(true);
        });
        return wasNewTabCreated.get();
    }

    private Optional<TreeView<TimeSeriesBinding<Double>>> buildTreeViewForTarget(DataAdapter dp) {

        TreeView<TimeSeriesBinding<Double>> treeView = new TreeView<>();

        Callback<TreeView<TimeSeriesBinding<Double>>, TreeCell<TimeSeriesBinding<Double>>> dragAndDropCellFactory = param -> {
            final TreeCell<TimeSeriesBinding<Double>> cell = new TreeCell<>();
            cell.itemProperty().addListener((observable, oldValue, newValue) -> cell.setText(newValue == null ? null : newValue.toString()));
            cell.setOnDragDetected((event) -> {
                System.out.println("cell setOnDragDetected");
                if (cell.getItem() != null) {
                    expandBranch(cell.getTreeItem());
                    Dragboard db = cell.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                    db.setDragView(cell.snapshot(null, null));
                    ClipboardContent content = new ClipboardContent();
                    content.put(TIME_SERIES_BINDING_FORMAT, cell.getItem().getTreeHierarchy());
                    db.setContent(content);
                }
                else {
                    logger.debug("No TreeItem selected: canceling drag and drop");
                }
                event.consume();
            });
            return cell;
        };
        treeView.setCellFactory(ContextMenuTreeViewCell.forTreeView(getTreeViewContextMenu(treeView), dragAndDropCellFactory));
        try {
            TreeItem<TimeSeriesBinding<Double>> root = dp.getBindingTree();
            root.setExpanded(true);
            treeView.setRoot(root);
            return Optional.of(treeView);
        } catch (DataAdapterException e) {
            Dialogs.notifyException("An error occurred while getting data from source " + dp.getSourceName(), e, root);
        }
        return Optional.empty();
    }

    private <T> void getAllBindingsFromBranch(TreeItem<T> branch, List<T> bindings) {
        if (branch.getChildren().size() > 0) {
            for (TreeItem<T> t : branch.getChildren()) {
                getAllBindingsFromBranch(t, bindings);
            }
        }
        else {
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

    private ContextMenu getTreeViewContextMenu(final TreeView<TimeSeriesBinding<Double>> treeView) {
        MenuItem addToCurrent = new MenuItem("Add to current worksheet");
        addToCurrent.disableProperty().bind(Bindings.size(worksheetTabPane.getTabs()).lessThanOrEqualTo(0));
        addToCurrent.setOnAction(event -> addToCurrentWorksheet(treeView.getSelectionModel().getSelectedItem()));
        MenuItem addToNew = new MenuItem("Add to new worksheet");
        addToNew.setOnAction(event -> addToNewWorksheet(treeView.getSelectionModel().getSelectedItem()));
        ContextMenu contextMenu = new ContextMenu(addToCurrent, addToNew);
        contextMenu.setOnShowing(event -> {
            expandBranch(treeView.getSelectionModel().getSelectedItem());
        });
        return contextMenu;
    }

    private void addToCurrentWorksheet(TreeItem<TimeSeriesBinding<Double>> treeItem) {
        try {
            if (selectedWorksheetController != null && treeItem != null) {
                List<TimeSeriesBinding<Double>> bindings = new ArrayList<>();
                getAllBindingsFromBranch(treeItem, bindings);
                selectedWorksheetController.addBindings(bindings);
            }
        } catch (Exception e) {
            Dialogs.notifyException("Error adding bindings to existing worksheet", e);
        }
    }

    private void addToNewWorksheet(TreeItem<TimeSeriesBinding<Double>> treeItem) {
        // Schedule for later execution in order to let other UI components get refreshed
        // before modal dialog gets displayed (fixes unsightly UI glitches on Linux)
        Platform.runLater(() -> {
            try {
                TimeSeriesBinding<Double> binding = treeItem.getValue();
                ZonedDateTime toDateTime;
                ZonedDateTime fromDateTime;
                if (selectedWorksheetController != null && selectedWorksheetController.getWorksheet() != null) {
                    toDateTime = selectedWorksheetController.getWorksheet().getToDateTime();
                    fromDateTime = selectedWorksheetController.getWorksheet().getFromDateTime();
                }
                else {
                    toDateTime = ZonedDateTime.now();
                    fromDateTime = toDateTime.minus(24, ChronoUnit.HOURS);
                }
                Worksheet<Double> worksheet = new Worksheet<>(binding.getLegend(),
                        binding.getGraphType(),
                        fromDateTime,
                        toDateTime,
                        ZoneId.systemDefault(),
                        binding.getUnitName(),
                        binding.getUnitPrefix());

                if (editWorksheet(worksheet) && selectedWorksheetController != null) {
                    List<TimeSeriesBinding<Double>> bindings = new ArrayList<>();
                    getAllBindingsFromBranch(treeItem, bindings);
                    selectedWorksheetController.addBindings(bindings);
                }
            } catch (Exception e) {
                Dialogs.notifyException("Error adding bindings to new worksheet", e);
            }
        });
    }

    private void setUiTheme(UserInterfaceThemes theme) {
        root.getStylesheets().clear();
        Application.setUserAgentStylesheet(null);
        root.getStylesheets().addAll(
                getClass().getResource("/css/Icons.css").toExternalForm(),
                getClass().getResource(theme.getCssPath()).toExternalForm());
    }


    public void populateOpenRecentMenu(Event event) {
        Menu openRecentMenu = (Menu) event.getSource();
        Collection<String> recentPath = GlobalPreferences.getInstance().getRecentFiles();
        if (recentPath.size() > 0) {
            openRecentMenu.getItems().setAll(recentPath.stream().map(s -> {
                MenuItem m = new MenuItem(s);
                m.setOnAction(e -> {
                    loadWorkspace(new File(((MenuItem) e.getSource()).getText()));
                });
                return m;
            }).collect(Collectors.toList()));
        }
        else {
            MenuItem none = new MenuItem("none");
            none.setDisable(true);
            openRecentMenu.getItems().setAll(none);
        }
    }
    //endregion
}
