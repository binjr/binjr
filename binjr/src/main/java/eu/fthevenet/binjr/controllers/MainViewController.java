package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.controls.ContextMenuTreeViewCell;
import eu.fthevenet.binjr.controls.EditableTab;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.workspace.Worksheet;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.dialogs.EditWorksheetDialog;
import eu.fthevenet.binjr.dialogs.GetDataAdapterDialog;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.binjr.sources.jrds.adapters.JRDSDataAdapter;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The controller class for the main view
 *
 * @author Frederic Thevenet
 */
public class MainViewController implements Initializable {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    @FXML
    public VBox root;
    @FXML
    private Menu worksheetsMenu;
    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem refreshMenuItem;
    @FXML
    private TabPane sourcesTabPane;
    @FXML
    private TabPane seriesTabPane;

    @FXML
    private MenuItem snapshotMenuItem;

    @FXML
    private CheckMenuItem showXmarkerMenuItem;
    @FXML
    private CheckMenuItem showYmarkerMenuItem;
    @FXML
    private Label addSourceLabel;
    @FXML
    private Label addWorksheetLabel;

    @FXML
    private HBox worksheetStatusBar;
    @FXML
    private Label nameLabel;
    @FXML
    private Label zoneIdLabel;
    @FXML
    private Label chartTypeLabel;
    @FXML
    private Label unitLabel;
    @FXML
    private Label baseLabel;

    @FXML
    private HBox sourceStatusBar;
    @FXML
    private Label sourceLabel;



    private SimpleBooleanProperty showVerticalMarker = new SimpleBooleanProperty();
    private TimeSeriesController selectedTabController;
    private DataAdapter<Double> selectedDataAdapter;
    private SimpleBooleanProperty showHorizontalMarker = new SimpleBooleanProperty();

    public MainViewController() {
        super();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert worksheetsMenu != null : "fx:id\"editMenu\" was not injected!";
        assert root != null : "fx:id\"root\" was not injected!";
        assert seriesTabPane != null : "fx:id\"seriesTabPane\" was not injected!";
        assert sourcesTabPane != null : "fx:id\"sourceTabPane\" was not injected!";
        assert addSourceLabel != null : "fx:id\"addSourceLabel\" was not injected!";
        assert addWorksheetLabel != null : "fx:id\"addWorksheetLabel\" was not injected!";
        assert snapshotMenuItem != null : "fx:id\"snapshotMenuItem\" was not injected!";

        Binding<Boolean> selectWorksheetPresent = Bindings.size(seriesTabPane.getTabs()).isEqualTo(0);
        Binding<Boolean> selectedSourcePresent = Bindings.size(sourcesTabPane.getTabs()).isEqualTo(0);
        showXmarkerMenuItem.disableProperty().bind(selectWorksheetPresent);
        showYmarkerMenuItem.disableProperty().bind(selectWorksheetPresent);
        snapshotMenuItem.disableProperty().bind(selectWorksheetPresent);
        refreshMenuItem.disableProperty().bind(selectWorksheetPresent);
        sourcesTabPane.mouseTransparentProperty().bind(selectedSourcePresent);
        seriesTabPane.mouseTransparentProperty().bind(selectWorksheetPresent);


        addSourceLabel.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> addSourceLabel.setStyle("-fx-text-fill: #7c7c7c;"));
        addSourceLabel.addEventFilter(MouseEvent.MOUSE_EXITED, e -> addSourceLabel.setStyle("-fx-text-fill: #c3c3c3;"));
        addSourceLabel.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> handleAddJRDSSource(new ActionEvent()));

        addWorksheetLabel.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> addWorksheetLabel.setStyle("-fx-text-fill: #7c7c7c;"));
        addWorksheetLabel.addEventFilter(MouseEvent.MOUSE_EXITED, e -> addWorksheetLabel.setStyle("-fx-text-fill: #c3c3c3;"));
        addWorksheetLabel.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> handleAddNewWorksheet(new ActionEvent()));

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> handleControlKey(e, true));
        root.addEventFilter(KeyEvent.KEY_RELEASED, e -> handleControlKey(e, false));

        showXmarkerMenuItem.selectedProperty().bindBidirectional(showVerticalMarker);
        showYmarkerMenuItem.selectedProperty().bindBidirectional(showHorizontalMarker);



        Platform.runLater(() -> {
                    Button newTabButton = new Button("+");
                    newTabButton.setFocusTraversable(false);
                    newTabButton.setPrefHeight(24);
                    newTabButton.setMaxHeight(newTabButton.getPrefHeight());
                    newTabButton.setMinHeight(newTabButton.getPrefHeight());
                    newTabButton.setOnAction(this::handleAddNewWorksheet);

                    Pane tabHeaderBg = (Pane) seriesTabPane.lookup(".tab-header-background");
                    tabHeaderBg.getChildren().add(newTabButton);
                    StackPane.setAlignment(newTabButton, Pos.BOTTOM_LEFT);
                    StackPane.setMargin(newTabButton, new Insets(0, 0, 0, 0));

                    Pane headersRegion = (Pane) seriesTabPane.lookup(".headers-region");
                    newTabButton.translateXProperty().bind(
                            headersRegion.widthProperty().add(5)
                    );



                    Button newSourceButton = new Button("+");
                    newSourceButton.setFocusTraversable(false);
                    newSourceButton.setPrefHeight(24);
                    newSourceButton.setMaxHeight(newSourceButton.getPrefHeight());
                    newSourceButton.setMinHeight(newSourceButton.getPrefHeight());

                    newSourceButton.setOnAction(this::handleAddJRDSSource);

                    Pane sourceHeaderBg = (Pane) sourcesTabPane.lookup(".tab-header-background");
                    sourceHeaderBg.getChildren().add(newSourceButton);
                    StackPane.setAlignment(newSourceButton, Pos.BOTTOM_LEFT);
                    StackPane.setMargin(newSourceButton, new Insets(0, 0, 0, 0));

                    Pane srcHeadersRegion = (Pane) sourcesTabPane.lookup(".headers-region");

                    newSourceButton.translateXProperty().bind(
                            sourceHeaderBg.widthProperty()
                                    .subtract(srcHeadersRegion.widthProperty())
                                    .subtract(newSourceButton.widthProperty())
                                    .subtract(5)
                    );


                }
        );

        seriesTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.selectedTabController = seriesControllers.get(newValue);
                Worksheet worksheet = selectedTabController.getWorksheet();
                worksheetStatusBar.setVisible(true);
                nameLabel.textProperty().bind(worksheet.nameProperty());
                zoneIdLabel.setText(worksheet.getTimeZone().toString());
                chartTypeLabel.setText(worksheet.getChartType().toString());
                unitLabel.setText(worksheet.getUnit());
                baseLabel.setText(worksheet.getUnitPrefixes().toString());
            }
            else{
                worksheetStatusBar.setVisible(false);
            }
        });

        sourcesTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedDataAdapter = (DataAdapter<Double>) newValue.getUserData();
                sourceStatusBar.setVisible(true);
                sourceLabel.setText(selectedDataAdapter.getSourceName());
            }
            else{
                sourceStatusBar.setVisible(false);
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
        Platform.exit();
    }

    @FXML
    protected void handleRefreshAction(ActionEvent actionEvent) {
        if (selectedTabController != null) {
            selectedTabController.invalidate(false, true, true);
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
    protected void handleAddNewWorksheet(ActionEvent event) {
        editWorksheet(new Worksheet());
    }

    @FXML
    protected void handleAddJRDSSource(ActionEvent actionEvent) {
        GetDataAdapterDialog dlg = new GetDataAdapterDialog("Connect to a JRDS source", JRDSDataAdapter::fromUrl, root);
        dlg.showAndWait().ifPresent(da -> {
            Tab newTab = new Tab(da.getSourceName());
            newTab.setUserData(da);
            TreeView<TimeSeriesBinding<Double>> treeView;
            treeView = buildTreeViewForTarget(da);
            newTab.setContent(treeView);
            sourcesTabPane.getTabs().add(newTab);
            sourcesTabPane.getSelectionModel().select(newTab);
        });

    }

    @FXML
    protected void handleTakeSnapshot(ActionEvent event) {
        if (selectedTabController !=null){
            selectedTabController.handleTakeSnapshot(event);
        }
    }
    //endregion

    //region Public properties
    public boolean isShowVerticalMarker() {
        return showVerticalMarker.getValue();
    }

    public SimpleBooleanProperty showVerticalMarkerProperty() {
        return showVerticalMarker;
    }

    public boolean isShowHorizontalMarker() {
        return showHorizontalMarker.getValue();
    }

    public SimpleBooleanProperty showHorizontalMarkerProperty() {
        return showHorizontalMarker;
    }


    //endregion

    //region private members

    private boolean editWorksheet(Worksheet worksheet) {
        AtomicBoolean wasNewTabCreated = new AtomicBoolean(false);
        EditWorksheetDialog<Double> dlg = new EditWorksheetDialog<>(worksheet, root);
        dlg.showAndWait().ifPresent(w -> {
            try {
                EditableTab newTab = new EditableTab("New worksheet");
                TimeSeriesController current;
                switch (w.getChartType()) {
                    case AREA:
                        current = new AreaChartTimeSeriesController(MainViewController.this, w);
                        break;
                    case STACKED:
                        current = new StackedAreaChartTimeSeriesController(MainViewController.this, w);
                        break;
                    case LINE:
                        current = new LineChartTimeSeriesController(MainViewController.this, w);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported chart");
                }
                try {
                    FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/views/TimeSeriesView.fxml"));
                    fXMLLoader.setController(current);
                    Parent p = fXMLLoader.load();
                    newTab.setContent(p);
                } catch (IOException ex) {
                    logger.error("Error loading time series", ex);
                }
                selectedTabController = current;

                seriesControllers.put(newTab, current);
                newTab.nameProperty().bindBidirectional(w.nameProperty());
                seriesTabPane.getTabs().add(newTab);
                seriesTabPane.getSelectionModel().select(newTab);
            }
            catch (Exception e){
                Dialogs.displayException("Error creating new worksheet tab", e);

            }finally {
                wasNewTabCreated.set(true);
            }
        });
        return wasNewTabCreated.get();
    }

    private Map<Tab, TimeSeriesController> seriesControllers = new HashMap<>();

    private TreeView<TimeSeriesBinding<Double>> buildTreeViewForTarget(DataAdapter dp) {
        TreeView<TimeSeriesBinding<Double>> treeView = new TreeView<>();

        treeView.setCellFactory(ContextMenuTreeViewCell.forTreeView(getTreeViewContextMenu(treeView)));
        try {
            TreeItem<TimeSeriesBinding<Double>> root = dp.getBindingTree();
            root.setExpanded(true);
            treeView.setRoot(root);
        } catch (DataAdapterException e) {
            Dialogs.displayException("An error occurred while building the tree from " + dp.getSourceName(), e, root);
        }
        return treeView;
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
                showHorizontalMarker.set(pressed);
                event.consume();
                break;
            case CONTROL:
            case META:
            case SHORTCUT: // shortcut does not seem to register as Control on Windows here, so check them all.
                showVerticalMarker.set(pressed);
                event.consume();
                break;
            default:
                //do nothing
        }
    }

    private ContextMenu getTreeViewContextMenu(final TreeView<TimeSeriesBinding<Double>> treeView) {
        MenuItem addToCurrent = new MenuItem("Add to current worksheet");
        addToCurrent.disableProperty().bind(Bindings.size(seriesTabPane.getTabs()).lessThanOrEqualTo(0));
        addToCurrent.setOnAction(event -> {
            TreeItem<TimeSeriesBinding<Double>> treeItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedTabController != null && treeItem != null) {
                List<TimeSeriesBinding<Double>> bindings = new ArrayList<>();
                getAllBindingsFromBranch(treeItem, bindings);
                selectedTabController.addBindings(bindings);
            }
        });
        MenuItem addToNew = new MenuItem("Add to new worksheet");
        addToNew.setOnAction(event -> {
            TreeItem<TimeSeriesBinding<Double>> treeItem = treeView.getSelectionModel().getSelectedItem();
            Worksheet worksheet = new Worksheet(treeItem.getValue().getLegend(), treeItem.getValue().getGraphType(), ZoneId.systemDefault());
            if (editWorksheet(worksheet) && selectedTabController != null) {
                List<TimeSeriesBinding<Double>> bindings = new ArrayList<>();
                getAllBindingsFromBranch(treeItem, bindings);
                selectedTabController.addBindings(bindings);
            }
        });
        return new ContextMenu(addToCurrent, addToNew);
    }

    public void handleHelpAction(ActionEvent event) {
        try {
            Dialogs.launchUrlInExternalBrowser(GlobalPreferences.HTTP_WWW_BINJR_EU);
        } catch (IOException | URISyntaxException e) {
            logger.error(e);
        }
    }

    public void handleLatestReleaseAction(ActionEvent event) {
        try {
            Dialogs.launchUrlInExternalBrowser(GlobalPreferences.HTTP_LATEST_RELEASE);
        } catch (IOException | URISyntaxException e) {
            logger.error(e);
        }
    }
    //endregion
}
