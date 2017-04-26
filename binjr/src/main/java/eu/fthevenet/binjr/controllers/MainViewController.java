package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.adapters.exceptions.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.exceptions.NoAdapterFoundException;
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
import eu.fthevenet.util.ui.controls.CommandBarPane;
import eu.fthevenet.util.ui.controls.ContextMenuTreeViewCell;
import eu.fthevenet.util.ui.controls.EditableTab;
import eu.fthevenet.util.ui.controls.TabPaneNewButton;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The controller class for the main view
 *
 * @author Frederic Thevenet
 */
public class MainViewController implements Initializable {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    @FXML
    public CommandBarPane commandBar;
    @FXML
    public HBox root;
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

    private WorksheetController selectedTabController;
    private DataAdapter<Double> selectedDataAdapter;
    private final Workspace workspace;
    private final Map<Tab, WorksheetController> seriesControllers = new HashMap<>();
    private final Map<Tab, DataAdapter> sourcesAdapters = new HashMap<>();

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

        GlobalPreferences.getInstance().userInterfaceThemeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                setUiTheme(newValue);
            }
        });
        setUiTheme(GlobalPreferences.getInstance().getUserInterfaceTheme());

        Binding<Boolean> selectWorksheetPresent = Bindings.size(worksheetTabPane.getTabs()).isEqualTo(0);
        Binding<Boolean> selectedSourcePresent = Bindings.size(sourcesTabPane.getTabs()).isEqualTo(0);
        refreshMenuItem.disableProperty().bind(selectWorksheetPresent);
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

        sourcesTabPane.setNewTabFactory(() -> {
            Tab newTab = new Tab();
            if (getAdapterDlg(newTab)) {
                return Optional.of(newTab);
            }
            return Optional.empty();
        });

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> handleControlKey(e, true));
        root.addEventFilter(KeyEvent.KEY_RELEASED, e -> handleControlKey(e, false));

        worksheetTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.selectedTabController = seriesControllers.get(newValue);
                Worksheet<Double> worksheet = selectedTabController.getWorksheet();
            }
        });

        sourcesTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedDataAdapter = (DataAdapter<Double>) newValue.getUserData();
            }
        });

        saveMenuItem.disableProperty().bind(workspace.dirtyProperty().not());

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

            stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (GlobalPreferences.getInstance().isEnableCrosshairOnKeyPressed() && !newValue && oldValue) {
                    //main stage lost focus -> invalidates crosshair
                    GlobalPreferences.getInstance().setHorizontalMarkerOn(false);
                    GlobalPreferences.getInstance().setVerticalMarkerOn(false);
                }
            });

            if (GlobalPreferences.getInstance().isLoadLastWorkspaceOnStartup()) {
                File latestWorkspace = GlobalPreferences.getInstance().getMostRecentSavedWorkspace().toFile();
                if (latestWorkspace.exists()) {
                    loadWorkspace(latestWorkspace);
                }
                else {
                    logger.warn("Cannot reopen workspace " + latestWorkspace.getPath() + ": file does not exists");
                }
            }

            if (GlobalPreferences.getInstance().isCheckForUpdateOnStartUp()) {
                GlobalPreferences.getInstance().asyncCheckForUpdate(
                        githubRelease -> {
                            Notifications n = Notifications.create()
                                    .title("New release available!")
                                    .text("You are currently running binjr version " + GlobalPreferences.getInstance().getManifestVersion() + "\t\t.\nVersion " + githubRelease.getVersion() + " is now available.")
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
        if (selectedTabController != null) {
            selectedTabController.refresh();
        }
    }

    @FXML
    protected void handlePreferencesAction(ActionEvent actionEvent) {
        try {
            Dialog<String> dialog = new Dialog<>();
            dialog.initModality(Modality.NONE);
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setTitle("Preferences");
            dialog.setDialogPane(FXMLLoader.load(getClass().getResource("/views/PreferenceDialogView.fxml")));
            dialog.initOwner(Dialogs.getStage(root));
            dialog.show();
        } catch (Exception ex) {
            Dialogs.displayException("Failed to display preference dialog", ex, root);
        }
    }

    @FXML
    protected void handleAddNewWorksheet(Event event) {
        editWorksheet(new Worksheet<>());
    }

    @FXML
    protected void handleAddJrdsSource(Event actionEvent) {
        DataAdapterDialog dlg = new JrdsAdapterDialog(root);
        Tab newTab = new Tab();
        if (getAdapterDlg(newTab)) {
            sourcesTabPane.getTabs().add(newTab);
            sourcesTabPane.getSelectionModel().select(newTab);
        }
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
    protected void handleTakeSnapshot(ActionEvent event) {
        if (selectedTabController != null) {
            selectedTabController.handleTakeSnapshot(event);
        }
    }
    //endregion


    //region private members

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
        try {
            if (confirmAndClearWorkspace()) {
                Workspace wsFromfile = Workspace.from(file);
                workspace.setPath(file.toPath());
                for (Source source : wsFromfile.getSources()) {
                    DataAdapter da = (DataAdapter) source.getAdapterClass().newInstance();
                    da.setParams(source.getAdapterParams());
                    da.setId(source.getAdapterId());
                    loadAdapters(da);
                }
                for (Worksheet<?> worksheet : wsFromfile.getWorksheets()) {
                    for (TimeSeriesInfo<?> s : worksheet.getSeries()) {
                        UUID id = s.getBinding().getAdapterId();
                        DataAdapter<?> da = sourcesAdapters.values()
                                .stream()
                                .filter(a -> (id != null && a != null && a.getId() != null) && id.equals(a.getId()))
                                .findAny()
                                .orElseThrow(() -> new NoAdapterFoundException("Failed to find a valid adapter with id " + (id != null ? id.toString() : "null")));
                        s.getBinding().setAdapter(da);
                        s.selectedProperty().addListener((observable, oldValue, newValue) -> selectedTabController.refresh());
                    }
                    loadWorksheet(worksheet);
                }
                workspace.cleanUp();
                GlobalPreferences.getInstance().putToRecentFiles(workspace.getPath().toString());
                logger.debug(() -> "Recently loaded workspaces: " + GlobalPreferences.getInstance().getRecentFiles().stream().collect(Collectors.joining(" ")));
            }
        } catch (IllegalAccessException | InstantiationException | DataAdapterException e) {
            Dialogs.displayException("Error while instantiating DataAdapter", e, root);
        } catch (IOException e) {
            GlobalPreferences.getInstance().removeFromRecentFiles(file.getPath());
            Dialogs.displayException("Error reading file " + file.getPath(), e, root);
        } catch (JAXBException e) {
            GlobalPreferences.getInstance().removeFromRecentFiles(file.getPath());
            Dialogs.displayException("Error while deserializing workspace", e, root);
        } catch (Exception e) {
            Dialogs.displayException("Error loading workspace", e, root);
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
            Dialogs.displayException("Failed to save snapshot to disk", e, root);
        } catch (JAXBException e) {
            Dialogs.displayException("Error while serializing workspace", e, root);
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
                Dialogs.displayException("Failed to save snapshot to disk", e, root);
            } catch (JAXBException e) {
                Dialogs.displayException("Error while serializing workspace", e, root);
            }
        }
        return false;
    }

    private boolean getAdapterDlg(Tab newTab) {
        AtomicBoolean res = new AtomicBoolean(false);
        DataAdapterDialog dlg = new JrdsAdapterDialog(root);
        dlg.showAndWait().ifPresent(da -> {
            newTab.setText(da.getSourceName());
            newTab.setUserData(da);
            Optional<TreeView<TimeSeriesBinding<Double>>> treeView;
            treeView = buildTreeViewForTarget(da);
            if (treeView.isPresent()) {
                newTab.setContent(treeView.get());
                sourcesAdapters.put(newTab, da);
                res.set(true);
            }
            else {
                res.set(false);
            }
        });
        return res.get();
    }

    // String label, String path, Color color, String legend, UnitPrefixes prefix, ChartType graphType, String unitName, String treeHierarchy, DataAdapter<T> adapter
    private void loadAdapters(DataAdapter da) throws DataAdapterException {
        Tab newTab = new Tab();
        newTab.setText(da.getSourceName());
        newTab.setUserData(da);
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

        sourcesTabPane.getTabs().add(newTab);
        sourcesTabPane.getSelectionModel().select(newTab);
    }

    private boolean loadWorksheet(Worksheet<?> worksheet) {
        EditableTab newTab = new EditableTab("New worksheet");
        loadWorksheet(worksheet, newTab);
        worksheetTabPane.getTabs().add(newTab);
        worksheetTabPane.getSelectionModel().select(newTab);
        return false;
    }

    private void loadWorksheet(Worksheet<?> worksheet, EditableTab newTab) {
        try {
            WorksheetController current;
            switch (worksheet.getChartType()) {
                case AREA:
                    current = new AreaChartWorksheetController(MainViewController.this, worksheet);
                    break;
                case STACKED:
                    current = new StackedAreaChartWorksheetController(MainViewController.this, worksheet);
                    break;
                case LINE:
                    current = new LineChartWorksheetController(MainViewController.this, worksheet);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported chart");
            }
            try {
                FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/views/WorksheetView.fxml"));
                fXMLLoader.setController(current);
                Parent p = fXMLLoader.load();
                newTab.setContent(p);
            } catch (IOException ex) {
                logger.error("Error loading time series", ex);
            }
            selectedTabController = current;
            seriesControllers.put(newTab, current);
            newTab.nameProperty().bindBidirectional(worksheet.nameProperty());
        } catch (Exception e) {
            Dialogs.displayException("Error loading worksheet into new tab", e);
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
        treeView.setCellFactory(ContextMenuTreeViewCell.forTreeView(getTreeViewContextMenu(treeView)));
        try {
            TreeItem<TimeSeriesBinding<Double>> root = dp.getBindingTree();
            root.setExpanded(true);
            treeView.setRoot(root);
            return Optional.of(treeView);
        } catch (DataAdapterException e) {
            Dialogs.displayException("An error occurred while getting data from source " + dp.getSourceName(), e, root);
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
        if (GlobalPreferences.getInstance().isEnableCrosshairOnKeyPressed()) {
            switch (event.getCode()) {
                case SHIFT:
                    GlobalPreferences.getInstance().setHorizontalMarkerOn(pressed);
                    event.consume();
                    break;
                case CONTROL:
                case META:
                case SHORTCUT: // shortcut does not seem to register as Control on Windows here, so check them all.
                    GlobalPreferences.getInstance().setVerticalMarkerOn(pressed);
                    event.consume();
                    break;
                default:
                    //do nothing
            }
        }
    }

    private ContextMenu getTreeViewContextMenu(final TreeView<TimeSeriesBinding<Double>> treeView) {
        MenuItem addToCurrent = new MenuItem("Add to current worksheet");
        addToCurrent.disableProperty().bind(Bindings.size(worksheetTabPane.getTabs()).lessThanOrEqualTo(0));
        addToCurrent.setOnAction(event -> {
            try {
                TreeItem<TimeSeriesBinding<Double>> treeItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedTabController != null && treeItem != null) {
                    List<TimeSeriesBinding<Double>> bindings = new ArrayList<>();
                    getAllBindingsFromBranch(treeItem, bindings);
                    selectedTabController.addBindings(bindings);
                }
            } catch (Exception e) {
                Dialogs.displayException("Error adding bindings to existing worksheet", e);
            }
        });
        MenuItem addToNew = new MenuItem("Add to new worksheet");
        addToNew.setOnAction(event -> {
            try {
                TreeItem<TimeSeriesBinding<Double>> treeItem = treeView.getSelectionModel().getSelectedItem();
                TimeSeriesBinding<Double> binding = treeItem.getValue();
                ZonedDateTime toDateTime;
                ZonedDateTime fromDateTime;
                if (selectedTabController != null && selectedTabController.getWorksheet() != null) {
                    toDateTime = selectedTabController.getWorksheet().getToDateTime();
                    fromDateTime = selectedTabController.getWorksheet().getFromDateTime();
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
                if (editWorksheet(worksheet) && selectedTabController != null) {
                    List<TimeSeriesBinding<Double>> bindings = new ArrayList<>();
                    getAllBindingsFromBranch(treeItem, bindings);
                    selectedTabController.addBindings(bindings);
                }
            } catch (Exception e) {
                Dialogs.displayException("Error adding bindings to new worksheet", e);
            }
        });
        ContextMenu contextMenu = new ContextMenu(addToCurrent, addToNew);
        contextMenu.setOnShowing(event -> {
            expandBranch(treeView.getSelectionModel().getSelectedItem());
        });
        return contextMenu;
    }

    private void setUiTheme(UserInterfaceThemes theme) {
        root.getStylesheets().clear();
        Application.setUserAgentStylesheet(null);
        root.getStylesheets().add(getClass().getResource(theme.getCssPath()).toExternalForm());
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

    public void handleExpandCommandBar(ActionEvent actionEvent) {
        commandBar.setExpanded(!commandBar.isExpanded());
    }
    //endregion
}
