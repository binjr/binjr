package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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
import javafx.util.converter.NumberStringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.property.BeanProperty;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;


public class MainViewController implements Initializable {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    @FXML
    public VBox root;
    @FXML
    public TextField downSamplingThreshold;
    @FXML
    private Menu viewMenu;
    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem editRefresh;
    @FXML
    private CheckBox showChartSymbols;
    @FXML
    private TreeView<String> treeview;
    @FXML
    private CheckBox enableDownSampling;
    @FXML
    private CheckBox enableChartAnimation;
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
        seriesTabPane.getTabs().add(new Tab("New series (" + nbSeries.incrementAndGet() + ")"));
    }

    private ObjectProperty<Integer> reductionTarget = new SimpleObjectProperty<>(2000);
    private Map<String, TimeSeriesController> seriesControllers = new HashMap<>();

    private void buildTreeViewForTarget(String target) {
        TreeItem<String> root = treeview.getRoot();
        root.setExpanded(true);
        TreeItem<String> targetItem = new TreeItem<>(target);
        root.getChildren().add(targetItem);

        targetItem.getChildren().addAll(
                new TreeItem<>("ConvertStatus-convert-c0"),
                new TreeItem<>("CrawlBoxManager-Crawler ObjectivePreviewSource"),
                new TreeItem<>("CrawlManager-Crawler ObjectivePreviewSource"),
                new TreeItem<>("DiskIOPdh-null"),
                new TreeItem<>("Indexing-bg0"),
                new TreeItem<>("IndexingAttributeGroupStore-bg0-0-1"),
                new TreeItem<>("IndexingAttributeGroupStore-bg0-0-2"),
                new TreeItem<>("IndexingAttributeGroupStore-bg0-1-1"),
                new TreeItem<>("IndexingAttributeGroupStore-bg0-1-2"),
                new TreeItem<>("IndexingAttributeGroupStore-bg0-2-1"),
                new TreeItem<>("IndexingAttributeGroupStore-bg0-2-2"),
                new TreeItem<>("IndexingAttributeGroupStore-bg0-3-1"),
                new TreeItem<>("IndexingAttributeGroupStore-bg0-3-2"),
                new TreeItem<>("IndexingCategoryField-bg0-0-categories"),
                new TreeItem<>("IndexingCategoryField-bg0-0-creatorid"),
                new TreeItem<>("IndexingCategoryField-bg0-0-databaseid"),
                new TreeItem<>("IndexingCategoryField-bg0-0-effectiveyear"),
                new TreeItem<>("IndexingCategoryField-bg0-0-fileextension"),
                new TreeItem<>("IndexingCategoryField-bg0-0-fileplan"),
                new TreeItem<>("IndexingCategoryField-bg0-0-security"),
                new TreeItem<>("IndexingCategoryField-bg0-0-service"),
                new TreeItem<>("IndexingCategoryField-bg0-0-updaterid"),
                new TreeItem<>("IndexingCategoryField-bg0-0-workgroup"),
                new TreeItem<>("IndexingCategoryField-bg0-1-categories"),
                new TreeItem<>("IndexingCategoryField-bg0-1-creatorid"),
                new TreeItem<>("IndexingCategoryField-bg0-1-databaseid"),
                new TreeItem<>("IndexingCategoryField-bg0-1-effectiveyear"),
                new TreeItem<>("IndexingCategoryField-bg0-1-fileextension"),
                new TreeItem<>("IndexingCategoryField-bg0-1-fileplan"),
                new TreeItem<>("IndexingCategoryField-bg0-1-security"),
                new TreeItem<>("IndexingCategoryField-bg0-1-service"),
                new TreeItem<>("IndexingCategoryField-bg0-1-updaterid"),
                new TreeItem<>("IndexingCategoryField-bg0-1-workgroup"),
                new TreeItem<>("IndexingCategoryField-bg0-2-categories"),
                new TreeItem<>("IndexingCategoryField-bg0-2-creatorid"),
                new TreeItem<>("IndexingCategoryField-bg0-2-databaseid"),
                new TreeItem<>("IndexingCategoryField-bg0-2-effectiveyear"),
                new TreeItem<>("IndexingCategoryField-bg0-2-fileextension"),
                new TreeItem<>("IndexingCategoryField-bg0-2-fileplan"),
                new TreeItem<>("IndexingCategoryField-bg0-2-security"),
                new TreeItem<>("IndexingCategoryField-bg0-2-service"),
                new TreeItem<>("IndexingCategoryField-bg0-2-updaterid"),
                new TreeItem<>("IndexingCategoryField-bg0-2-workgroup"),
                new TreeItem<>("IndexingCategoryField-bg0-3-categories"),
                new TreeItem<>("IndexingCategoryField-bg0-3-creatorid"),
                new TreeItem<>("IndexingCategoryField-bg0-3-databaseid"),
                new TreeItem<>("IndexingCategoryField-bg0-3-effectiveyear"),
                new TreeItem<>("IndexingCategoryField-bg0-3-fileextension"),
                new TreeItem<>("IndexingCategoryField-bg0-3-fileplan"),
                new TreeItem<>("IndexingCategoryField-bg0-3-security"),
                new TreeItem<>("IndexingCategoryField-bg0-3-service"),
                new TreeItem<>("IndexingCategoryField-bg0-3-updaterid"),
                new TreeItem<>("IndexingCategoryField-bg0-3-workgroup"),
                new TreeItem<>("IndexingField-bg0-0-ancestors"),
                new TreeItem<>("IndexingField-bg0-0-cascadesource"),
                new TreeItem<>("IndexingField-bg0-0-collectionid"),
                new TreeItem<>("IndexingField-bg0-0-comment"),
                new TreeItem<>("IndexingField-bg0-0-contentexception"),
                new TreeItem<>("IndexingField-bg0-0-datearrive"),
                new TreeItem<>("IndexingField-bg0-0-datecreated"),
                new TreeItem<>("IndexingField-bg0-0-documentnbr"),
                new TreeItem<>("IndexingField-bg0-0-externalid"),
                new TreeItem<>("IndexingField-bg0-0-file_size"),
                new TreeItem<>("IndexingField-bg0-0-filetitle"),
                new TreeItem<>("IndexingField-bg0-0-id"),
                new TreeItem<>("IndexingField-bg0-0-indexid"),
                new TreeItem<>("IndexingField-bg0-0-indocid"),
                new TreeItem<>("IndexingField-bg0-0-language"),
                new TreeItem<>("IndexingField-bg0-0-metas"),
                new TreeItem<>("IndexingField-bg0-0-note"),
                new TreeItem<>("IndexingField-bg0-0-objhash"),
                new TreeItem<>("IndexingField-bg0-0-objsec"),
                new TreeItem<>("IndexingField-bg0-0-outdocid"),
                new TreeItem<>("IndexingField-bg0-0-parentid"),
                new TreeItem<>("IndexingField-bg0-0-physicalid"),
                new TreeItem<>("IndexingField-bg0-0-pmkeys"),
                new TreeItem<>("IndexingField-bg0-0-primarytype"),
                new TreeItem<>("IndexingField-bg0-0-savnbr"),
                new TreeItem<>("IndexingField-bg0-0-snapshotcontentversion"),
                new TreeItem<>("IndexingField-bg0-0-snapshottimestamp"),
                new TreeItem<>("IndexingField-bg0-0-snapshotversion"),
                new TreeItem<>("IndexingField-bg0-0-source"),
                new TreeItem<>("IndexingField-bg0-0-subtypedefid"),
                new TreeItem<>("IndexingField-bg0-0-tablename"),
                new TreeItem<>("IndexingField-bg0-0-text"),
                new TreeItem<>("IndexingField-bg0-0-title"),
                new TreeItem<>("IndexingField-bg0-0-url"),
                new TreeItem<>("IndexingField-bg0-1-ancestors"),
                new TreeItem<>("IndexingField-bg0-1-cascadesource"),
                new TreeItem<>("IndexingField-bg0-1-collectionid"),
                new TreeItem<>("IndexingField-bg0-1-comment"),
                new TreeItem<>("IndexingField-bg0-1-contentexception"),
                new TreeItem<>("IndexingField-bg0-1-datearrive"),
                new TreeItem<>("IndexingField-bg0-1-datecreated"),
                new TreeItem<>("IndexingField-bg0-1-documentnbr"),
                new TreeItem<>("IndexingField-bg0-1-externalid"),
                new TreeItem<>("IndexingField-bg0-1-file_size"),
                new TreeItem<>("IndexingField-bg0-1-filetitle"),
                new TreeItem<>("IndexingField-bg0-1-id"),
                new TreeItem<>("IndexingField-bg0-1-indexid"),
                new TreeItem<>("IndexingField-bg0-1-indocid"),
                new TreeItem<>("IndexingField-bg0-1-language"),
                new TreeItem<>("IndexingField-bg0-1-metas"),
                new TreeItem<>("IndexingField-bg0-1-note"),
                new TreeItem<>("IndexingField-bg0-1-objhash"),
                new TreeItem<>("IndexingField-bg0-1-objsec"),
                new TreeItem<>("IndexingField-bg0-1-outdocid"),
                new TreeItem<>("IndexingField-bg0-1-parentid"),
                new TreeItem<>("IndexingField-bg0-1-physicalid"),
                new TreeItem<>("IndexingField-bg0-1-pmkeys"),
                new TreeItem<>("IndexingField-bg0-1-primarytype"),
                new TreeItem<>("IndexingField-bg0-1-savnbr"),
                new TreeItem<>("IndexingField-bg0-1-snapshotcontentversion"),
                new TreeItem<>("IndexingField-bg0-1-snapshottimestamp"),
                new TreeItem<>("IndexingField-bg0-1-snapshotversion"),
                new TreeItem<>("IndexingField-bg0-1-source"),
                new TreeItem<>("IndexingField-bg0-1-subtypedefid"),
                new TreeItem<>("IndexingField-bg0-1-tablename"),
                new TreeItem<>("IndexingField-bg0-1-text"),
                new TreeItem<>("IndexingField-bg0-1-title"),
                new TreeItem<>("IndexingField-bg0-1-url"),
                new TreeItem<>("IndexingField-bg0-2-ancestors"),
                new TreeItem<>("IndexingField-bg0-2-cascadesource"),
                new TreeItem<>("IndexingField-bg0-2-collectionid"),
                new TreeItem<>("IndexingField-bg0-2-comment"),
                new TreeItem<>("IndexingField-bg0-2-contentexception"),
                new TreeItem<>("IndexingField-bg0-2-datearrive"),
                new TreeItem<>("IndexingField-bg0-2-datecreated"),
                new TreeItem<>("IndexingField-bg0-2-documentnbr"),
                new TreeItem<>("IndexingField-bg0-2-externalid"),
                new TreeItem<>("IndexingField-bg0-2-file_size"),
                new TreeItem<>("IndexingField-bg0-2-filetitle"),
                new TreeItem<>("IndexingField-bg0-2-id"),
                new TreeItem<>("IndexingField-bg0-2-indexid"),
                new TreeItem<>("IndexingField-bg0-2-indocid"),
                new TreeItem<>("IndexingField-bg0-2-language"),
                new TreeItem<>("IndexingField-bg0-2-metas"),
                new TreeItem<>("IndexingField-bg0-2-note"),
                new TreeItem<>("IndexingField-bg0-2-objhash"),
                new TreeItem<>("IndexingField-bg0-2-objsec"),
                new TreeItem<>("IndexingField-bg0-2-outdocid"),
                new TreeItem<>("IndexingField-bg0-2-parentid"),
                new TreeItem<>("IndexingField-bg0-2-physicalid"),
                new TreeItem<>("IndexingField-bg0-2-pmkeys"),
                new TreeItem<>("IndexingField-bg0-2-primarytype"),
                new TreeItem<>("IndexingField-bg0-2-savnbr"),
                new TreeItem<>("IndexingField-bg0-2-snapshotcontentversion"),
                new TreeItem<>("IndexingField-bg0-2-snapshottimestamp"),
                new TreeItem<>("IndexingField-bg0-2-snapshotversion"),
                new TreeItem<>("IndexingField-bg0-2-source"),
                new TreeItem<>("IndexingField-bg0-2-subtypedefid"),
                new TreeItem<>("IndexingField-bg0-2-tablename"),
                new TreeItem<>("IndexingField-bg0-2-text"),
                new TreeItem<>("IndexingField-bg0-2-title"),
                new TreeItem<>("IndexingField-bg0-2-url"),
                new TreeItem<>("IndexingField-bg0-3-ancestors"),
                new TreeItem<>("IndexingField-bg0-3-cascadesource"),
                new TreeItem<>("IndexingField-bg0-3-collectionid"),
                new TreeItem<>("IndexingField-bg0-3-comment"),
                new TreeItem<>("IndexingField-bg0-3-contentexception"),
                new TreeItem<>("IndexingField-bg0-3-datearrive"),
                new TreeItem<>("IndexingField-bg0-3-datecreated"),
                new TreeItem<>("IndexingField-bg0-3-documentnbr"),
                new TreeItem<>("IndexingField-bg0-3-externalid"),
                new TreeItem<>("IndexingField-bg0-3-file_size"),
                new TreeItem<>("IndexingField-bg0-3-filetitle"),
                new TreeItem<>("IndexingField-bg0-3-id"),
                new TreeItem<>("IndexingField-bg0-3-indexid"),
                new TreeItem<>("IndexingField-bg0-3-indocid"),
                new TreeItem<>("IndexingField-bg0-3-language"),
                new TreeItem<>("IndexingField-bg0-3-metas"),
                new TreeItem<>("IndexingField-bg0-3-note"),
                new TreeItem<>("IndexingField-bg0-3-objhash"),
                new TreeItem<>("IndexingField-bg0-3-objsec"),
                new TreeItem<>("IndexingField-bg0-3-outdocid"),
                new TreeItem<>("IndexingField-bg0-3-parentid"),
                new TreeItem<>("IndexingField-bg0-3-physicalid"),
                new TreeItem<>("IndexingField-bg0-3-pmkeys"),
                new TreeItem<>("IndexingField-bg0-3-primarytype"),
                new TreeItem<>("IndexingField-bg0-3-savnbr"),
                new TreeItem<>("IndexingField-bg0-3-snapshotcontentversion"),
                new TreeItem<>("IndexingField-bg0-3-snapshottimestamp"),
                new TreeItem<>("IndexingField-bg0-3-snapshotversion"),
                new TreeItem<>("IndexingField-bg0-3-source"),
                new TreeItem<>("IndexingField-bg0-3-subtypedefid"),
                new TreeItem<>("IndexingField-bg0-3-tablename"),
                new TreeItem<>("IndexingField-bg0-3-text"),
                new TreeItem<>("IndexingField-bg0-3-title"),
                new TreeItem<>("IndexingField-bg0-3-url"),
                new TreeItem<>("IndexingSlice-bg0-0-i0"),
                new TreeItem<>("IndexingSlice-bg0-1-i0"),
                new TreeItem<>("IndexingSlice-bg0-2-i0"),
                new TreeItem<>("IndexingSlice-bg0-3-i0"),
                new TreeItem<>("LinguisticManager-dos729.eng12.ocl-dict0"),
                new TreeItem<>("NetIOPdh"),
                new TreeItem<>("SearchCommand-dos729.eng12.ocl-search-api-10010"),
                new TreeItem<>("SearchIndex-dos729.eng12.ocl-bg0-0-i0"),
                new TreeItem<>("SearchIndex-dos729.eng12.ocl-bg0-1-i0"),
                new TreeItem<>("SearchIndex-dos729.eng12.ocl-bg0-2-i0"),
                new TreeItem<>("SearchIndex-dos729.eng12.ocl-bg0-3-i0"),
                new TreeItem<>("SearchIndexPageCache-dos729.eng12.ocl-bg0-0-i0-cache0"),
                new TreeItem<>("SearchIndexPageCache-dos729.eng12.ocl-bg0-1-i0-cache0"),
                new TreeItem<>("SearchIndexPageCache-dos729.eng12.ocl-bg0-2-i0-cache0"),
                new TreeItem<>("SearchIndexPageCache-dos729.eng12.ocl-bg0-3-i0-cache0"),
                new TreeItem<>("du"),
                new TreeItem<>("jvmpool-connectors-java0"),
                new TreeItem<>("jvmpool-convert-c0"),
                new TreeItem<>("jvmpool-gateway"),
                new TreeItem<>("jvmpool-index6-bg0-i0"),
                new TreeItem<>("jvmpool-indexingserver-bg0"),
                new TreeItem<>("jvmpool-searchserver-ss0"),
                new TreeItem<>("memprocPdh"),
                new TreeItem<>("processPdh-Global CloudView processes"),
                new TreeItem<>("processPdh-connectors-java0"),
                new TreeItem<>("processPdh-convert-c0"),
                new TreeItem<>("processPdh-crawler-exa0"),
                new TreeItem<>("processPdh-gateway"),
                new TreeItem<>("processPdh-index6-bg0-i0"),
                new TreeItem<>("processPdh-indexingserver-bg0"),
                new TreeItem<>("processPdh-master"),
                new TreeItem<>("processPdh-searchserver-ss0")
        );
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
        assert downSamplingThreshold != null : "fx:id\"RDPEpsilon\" was not injected!";
        assert showChartSymbols != null : "fx:id\"showChartSymbols\" was not injected!";
        assert treeview != null : "fx:id\"treeview\" was not injected!";
        assert enableChartAnimation != null : "fx:id\"enableChartAnimation\" was not injected!";
        assert seriesTabPane != null : "fx:id\"seriesTabPane\" was not injected!";
        assert enableDownSampling != null : "fx:id\"enableDownSampling\" was not injected!";

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> handleControlKey(e, true));
        root.addEventFilter(KeyEvent.KEY_RELEASED, e -> handleControlKey(e, false));
        vMarkerToggle.selectedProperty().bindBidirectional(showHorizontalMarker);
        hMarkerToggle.selectedProperty().bindBidirectional(showVerticalMarker);
        showXmarkerMenuItem.selectedProperty().bindBidirectional(showVerticalMarker);
        showYmarkerMenuItem.selectedProperty().bindBidirectional(showHorizontalMarker);

//        enableChartAnimation.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().chartAnimationEnabledProperty());
//        showChartSymbols.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().sampleSymbolsVisibleProperty());
//        enableDownSampling.selectedProperty().bindBidirectional(GlobalPreferences.getInstance().downSamplingEnabledProperty());
//        final TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
//        downSamplingThreshold.setTextFormatter(formatter);
//        formatter.valueProperty().bindBidirectional(GlobalPreferences.getInstance().downSamplingThresholdProperty());


        seriesTabPane.getSelectionModel().clearSelection();
        seriesTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
              if (newValue == null){
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
//                        current.getChart().createSymbolsProperty().bindBidirectional(showChartSymbols.selectedProperty());
//                        current.getChart().animatedProperty().bindBidirectional(enableChartAnimation.selectedProperty());


                        seriesControllers.put(newValue.getText(), current);

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
        treeview.setRoot(new TreeItem<>("Hosts"));
        seriesTabPane.getTabs().add(new Tab("memprocPdh"));
        buildTreeViewForTarget("memprocPdh");
    }


    public void handleRefreshAction(ActionEvent actionEvent) {
        if (selectedTabController!= null){
            selectedTabController.invalidate(false, true, true);
        }

    }

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
            dialog.initStyle(StageStyle.UNIFIED);
            dialog.setDialogPane(FXMLLoader.load(getClass().getResource("/views/PreferenceDialogView.fxml")));
            dialog.initOwner(getStage());
            dialog.showAndWait();
        } catch (Exception ex) {
            displayException("Failed to display preference dialog", ex);
        }
    }
}
