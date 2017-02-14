package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.controls.EditableTab;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.sources.jrds.adapters.JRDSDataAdapter;
import eu.fthevenet.binjr.dialogs.GetDataAdapterDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.dialog.ExceptionDialog;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Menu viewMenu;
    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem editRefresh;
    @FXML
    private TabPane sourcesTabPane;
    @FXML
    private TabPane seriesTabPane;
    @FXML
    private MenuItem newTab;
    @FXML
    private ToggleSwitch hMarkerToggle;
    @FXML
    private ToggleSwitch vMarkerToggle;
    @FXML
    private CheckMenuItem showXmarkerMenuItem;
    @FXML
    private CheckMenuItem showYmarkerMenuItem;
    private SimpleBooleanProperty showVerticalMarker = new SimpleBooleanProperty();
    private SimpleBooleanProperty showHorizontalMarker = new SimpleBooleanProperty();

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

    private AtomicInteger nbSeries = new AtomicInteger(0);

    @FXML
    protected void handleNewTabAction(ActionEvent actionEvent) {
        //seriesTabPane.getTabs().add(new Tab("New series (" + nbSeries.incrementAndGet() + ")"));
    }

    private Map<Tab, TimeSeriesController> seriesControllers = new HashMap<>();

    private TreeView<TimeSeriesBinding> buildTreeViewForTarget(DataAdapter dp) {
        TreeView<TimeSeriesBinding> treeView = new TreeView<>();
        try {
            TreeItem<TimeSeriesBinding> root = dp.getBindingTree();

            root.setExpanded(true);

            treeView.setRoot(root);
            treeView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    TreeItem<TimeSeriesBinding> item = treeView.getSelectionModel().getSelectedItem();
                    if (selectedTabController != null && item !=null) {
                        selectedTabController.addBinding(item.getValue());
                    }
                }
            });
        } catch (DataAdapterException e) {
           Dialogs.displayException("An error occurred while building the tree from " + (dp != null ? dp.getSourceName() : "null"), e, root);
        }
        return treeView;
    }

    private TimeSeriesController selectedTabController;

    private void handleControlKey(KeyEvent event, boolean pressed) {
        switch (event.getCode()) {
            case SHIFT:
                showHorizontalMarker.set(pressed);
                event.consume();
                break;

            case CONTROL:
                showVerticalMarker.set(pressed);
                event.consume();
                break;

            default:
                //do nothing
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert viewMenu != null : "fx:id\"editMenu\" was not injected!";
        assert root != null : "fx:id\"root\" was not injected!";
        assert seriesTabPane != null : "fx:id\"seriesTabPane\" was not injected!";
        assert sourcesTabPane != null : "fx:id\"sourceTabPane\" was not injected!";

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> handleControlKey(e, true));
        root.addEventFilter(KeyEvent.KEY_RELEASED, e -> handleControlKey(e, false));
        vMarkerToggle.selectedProperty().bindBidirectional(showHorizontalMarker);
        hMarkerToggle.selectedProperty().bindBidirectional(showVerticalMarker);
        showXmarkerMenuItem.selectedProperty().bindBidirectional(showVerticalMarker);
        showYmarkerMenuItem.selectedProperty().bindBidirectional(showHorizontalMarker);

        seriesTabPane.getSelectionModel().clearSelection();
        seriesTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                if (newValue == null) {
                    return;
                }
                if (newValue.getContent() == null) {
                    try {
                        // Loading content on demand
                        FXMLLoader fXMLLoader = new FXMLLoader();
                        Parent p = fXMLLoader.load(getClass().getResource("/views/TimeSeriesView.fxml").openStream());
                        newValue.setContent(p);
                        // Store the controllers
                        TimeSeriesController current = fXMLLoader.getController();
                        selectedTabController = current;
                        // Init time series controller
                        // TODO clean-up initialization of timeSeriescontrollers
                        current.setMainViewController(MainViewController.this);
                        current.getCrossHair().horizontalMarkerVisibleProperty().bind(showHorizontalMarker);
                        current.getCrossHair().verticalMarkerVisibleProperty().bind(showVerticalMarker);
                        seriesControllers.put(newValue, current);
                        // add "+" tab
                        ((Label) newValue.getGraphic()).setText("New worksheet(" + nbSeries.getAndIncrement() + ")");
                        seriesTabPane.getTabs().add(new EditableTab("+"));

                    } catch (IOException ex) {
                        logger.error("Error loading time series", ex);
                    }
                }
                else {
                    Parent root = (Parent) newValue.getContent();
                }
            }
        });
        // By default, select 1st tab and load its content.
        seriesTabPane.getSelectionModel().selectFirst();
        seriesTabPane.getTabs().add(new EditableTab("New worksheet"));
        seriesTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null){
                this.selectedTabController = seriesControllers.get(newValue);
            }
        });

        sourcesTabPane.getSelectionModel().clearSelection();
        sourcesTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            if (newValue.getContent() == null) {
                TreeView<TimeSeriesBinding> treeView;
                @SuppressWarnings("unchecked")
                DataAdapter<Double> da = (DataAdapter<Double>)newValue.getUserData();
                treeView = buildTreeViewForTarget(da);
                newValue.setContent(treeView);
            }
        });
    }

    public void handleRefreshAction(ActionEvent actionEvent) {
        if (selectedTabController != null) {
            selectedTabController.invalidate(false, true, true);
        }
    }

    @FXML
    public void handlePreferencesAction(ActionEvent actionEvent) {
        try {
            Dialog<String> dialog = new Dialog<>();
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setTitle("Preferences");
            dialog.setDialogPane(FXMLLoader.load(getClass().getResource("/views/PreferenceDialogView.fxml")));
            dialog.initOwner(Dialogs.getStage(root));
            dialog.showAndWait();
        } catch (Exception ex) {
            Dialogs.displayException("Failed to display preference dialog", ex, root);
        }
    }

    public void handleAddJRDSSource(ActionEvent actionEvent) {
        GetDataAdapterDialog dlg = new GetDataAdapterDialog( "Add a JRDS source", JRDSDataAdapter::fromUrl, root);
        dlg.showAndWait().ifPresent( da ->
        {
            Tab newTab = new Tab(da.getSourceName());
                newTab.setUserData(da);
                sourcesTabPane.getTabs().add(newTab);
                sourcesTabPane.getSelectionModel().select(newTab);
        });

    }
}
