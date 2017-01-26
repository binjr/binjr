package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.commons.controls.EditableTab;
import eu.fthevenet.binjr.data.adapters.DataAdapter;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.adapters.jrds.JRDSDataAdapter;
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

public class MainViewController implements Initializable {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    public static final String JRDS_HOSTNAME = "ngwps006";
    public static final int JRDS_PORT = 31001;
    public static final String JRDS_PATH = "/perf-ui";
    @FXML
    public VBox root;
    @FXML
    private Menu viewMenu;
    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem editRefresh;
    @FXML
    private TreeView<TimeSeriesBinding> treeview;
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
        dialog.initOwner(getStage());
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

    private Map<String, TimeSeriesController> seriesControllers = new HashMap<>();

    private void buildTreeViewForTarget(String target) {
        DataAdapter dp = JRDSDataAdapter.createHttp(JRDS_HOSTNAME, JRDS_PORT, JRDS_PATH);
        try {
            TreeItem<TimeSeriesBinding> root = dp.getTree();
            root.setExpanded(true);

            treeview.setRoot(root);
        } catch (DataAdapterException e) {
            logger.error("Failed to toSeries tree", e);
            displayException("Failed to toSeries tree", e);
        }
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
        assert treeview != null : "fx:id\"treeview\" was not injected!";
        assert seriesTabPane != null : "fx:id\"seriesTabPane\" was not injected!";

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
                        current.setMainViewController(MainViewController.this);
                        current.getCrossHair().showHorizontalMarkerProperty().bind(showHorizontalMarker);
                        current.getCrossHair().showVerticalMarkerProperty().bind(showVerticalMarker);
                        seriesControllers.put(newValue.getText(), current);
                        // add "+" tab
                        ((Label)newValue.getGraphic()).setText("New series " + nbSeries.getAndIncrement());
                        seriesTabPane.getTabs().add(new EditableTab("+"));

                    } catch (IOException ex) {
                        logger.error("Error loading time series", ex);
                    }
                }
                else {
                    // Content is already loaded. Update it if necessary.
                    Parent root = (Parent) newValue.getContent();
                    // Optionally get the controller from Map and manipulate the content
                    // via its controller.
                }
            }
        });
        // By default, select 1st tab and load its content.
        seriesTabPane.getSelectionModel().selectFirst();
      //  treeview.setRoot(new TreeItem<>("Hosts"));
        seriesTabPane.getTabs().add(new EditableTab("memprocPdh"));
        buildTreeViewForTarget("memprocPdh");
    }


    public void handleRefreshAction(ActionEvent actionEvent) {
        if (selectedTabController!= null){
            selectedTabController.invalidate(false, true, true);
        }

    }
//    private Tab createEditableTab(String text) {
//        final Label label = new Label(text);
//        final Tab tab = new Tab();
//        tab.setGraphic(label);
//
//        final TextField textField = new TextField();
//        label.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                if (event.getClickCount()==2) {
//                    textField.setText(label.getText());
//                    tab.setGraphic(textField);
//                    textField.selectAll();
//                    textField.requestFocus();
//                }
//            }
//        });
//
//
//        textField.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                label.setText(textField.getText());
//                tab.setGraphic(label);
//            }
//        });
//
//
//        textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
//            @Override
//            public void changed(ObservableValue<? extends Boolean> observable,
//                                Boolean oldValue, Boolean newValue) {
//                if (! newValue) {
//                    label.setText(textField.getText());
//                    tab.setGraphic(label);
//                }
//            }
//        });
//        return tab ;
//    }


    public void handleDumpHistoryAction(ActionEvent actionEvent) {
        if (selectedTabController != null) {
            TimeSeriesController.History h = selectedTabController.getBackwardHistory();
            logger.debug(() -> "Current Tab selection  history (backward):\n" + (h == null ? "null" : h.dump()));
        }
    }

    public  void displayException(String header, Exception e) {
        displayException(header, e, getStage());
    }


    public  void displayException(String header, Exception e, Window owner) {
        logger.debug(()-> "Displaying following exception to end user", e);
        ExceptionDialog dlg = new ExceptionDialog(e);
        dlg.initStyle(StageStyle.UTILITY);
        dlg.initOwner(owner);
        dlg.getDialogPane().setHeaderText(header);
        dlg.showAndWait();
    }


    private Stage getStage(){
        if (root != null && root.getScene() != null){
            return (Stage)root.getScene().getWindow();
        }
        return null;
    }

    @FXML
    public void handlePreferencesAction(ActionEvent actionEvent) {
        try {
            Dialog<String> dialog = new Dialog<>();
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setTitle("Preferences");
            dialog.setDialogPane(FXMLLoader.load(getClass().getResource("/views/PreferenceDialogView.fxml")));
            dialog.initOwner(getStage());
            dialog.showAndWait();
        } catch (Exception ex) {
            displayException("Failed to display preference dialog", ex);
        }
    }
}
