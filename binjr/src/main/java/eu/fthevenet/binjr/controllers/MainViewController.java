package eu.fthevenet.binjr.controllers;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;


public class MainViewController implements Initializable {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    @FXML
    public VBox root;
    @FXML
    public TextField RDPEpsilon;
    @FXML
    private Menu editMenu;
    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem editRefresh;
    @FXML
    private CheckBox showChartSymbols;
    @FXML
    TreeView<String> treeview;
    @FXML
    private CheckBox chkBoxEnableRDP;
    @FXML
    private CheckBox enableChartAnimation;
    @FXML
    private TabPane seriesTabPane;
    @FXML
    private MenuItem newTab;

    private SimpleBooleanProperty showVerticalMarker = new SimpleBooleanProperty();
    private SimpleBooleanProperty showHorizontalMarker = new SimpleBooleanProperty();


    @FXML
    protected void handleAboutAction(ActionEvent event) throws IOException {

        Dialog<String> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setDialogPane(FXMLLoader.load(getClass().getResource("/views/AboutBoxView.fxml")));
        dialog.showAndWait();

//        Stage dialog = new Stage();
//        dialog.initModality(Modality.APPLICATION_MODAL);
//        //dialog.initStyle(StageStyle.TRANSPARENT);
//        dialog.setScene(new Scene(FXMLLoader.load(getClass().getResource("/views/AboutBoxView2.fxml"))));
//        dialog.show();
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
        TreeItem<String> targetItem = new TreeItem<String>(target);
        root.getChildren().add(targetItem);

        targetItem.getChildren().addAll(
                new TreeItem<String>("ConvertStatus-convert-c0"),
                new TreeItem<String>("CrawlBoxManager-Crawler ObjectivePreviewSource"),
                new TreeItem<String>("CrawlManager-Crawler ObjectivePreviewSource"),
                new TreeItem<String>("DiskIOPdh-null"),
                new TreeItem<String>("Indexing-bg0"),
                new TreeItem<String>("IndexingAttributeGroupStore-bg0-0-1"),
                new TreeItem<String>("IndexingAttributeGroupStore-bg0-0-2"),
                new TreeItem<String>("IndexingAttributeGroupStore-bg0-1-1"),
                new TreeItem<String>("IndexingAttributeGroupStore-bg0-1-2"),
                new TreeItem<String>("IndexingAttributeGroupStore-bg0-2-1"),
                new TreeItem<String>("IndexingAttributeGroupStore-bg0-2-2"),
                new TreeItem<String>("IndexingAttributeGroupStore-bg0-3-1"),
                new TreeItem<String>("IndexingAttributeGroupStore-bg0-3-2"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-categories"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-creatorid"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-databaseid"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-effectiveyear"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-fileextension"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-fileplan"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-security"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-service"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-updaterid"),
                new TreeItem<String>("IndexingCategoryField-bg0-0-workgroup"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-categories"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-creatorid"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-databaseid"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-effectiveyear"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-fileextension"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-fileplan"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-security"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-service"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-updaterid"),
                new TreeItem<String>("IndexingCategoryField-bg0-1-workgroup"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-categories"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-creatorid"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-databaseid"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-effectiveyear"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-fileextension"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-fileplan"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-security"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-service"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-updaterid"),
                new TreeItem<String>("IndexingCategoryField-bg0-2-workgroup"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-categories"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-creatorid"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-databaseid"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-effectiveyear"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-fileextension"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-fileplan"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-security"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-service"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-updaterid"),
                new TreeItem<String>("IndexingCategoryField-bg0-3-workgroup"),
                new TreeItem<String>("IndexingField-bg0-0-ancestors"),
                new TreeItem<String>("IndexingField-bg0-0-cascadesource"),
                new TreeItem<String>("IndexingField-bg0-0-collectionid"),
                new TreeItem<String>("IndexingField-bg0-0-comment"),
                new TreeItem<String>("IndexingField-bg0-0-contentexception"),
                new TreeItem<String>("IndexingField-bg0-0-datearrive"),
                new TreeItem<String>("IndexingField-bg0-0-datecreated"),
                new TreeItem<String>("IndexingField-bg0-0-documentnbr"),
                new TreeItem<String>("IndexingField-bg0-0-externalid"),
                new TreeItem<String>("IndexingField-bg0-0-file_size"),
                new TreeItem<String>("IndexingField-bg0-0-filetitle"),
                new TreeItem<String>("IndexingField-bg0-0-id"),
                new TreeItem<String>("IndexingField-bg0-0-indexid"),
                new TreeItem<String>("IndexingField-bg0-0-indocid"),
                new TreeItem<String>("IndexingField-bg0-0-language"),
                new TreeItem<String>("IndexingField-bg0-0-metas"),
                new TreeItem<String>("IndexingField-bg0-0-note"),
                new TreeItem<String>("IndexingField-bg0-0-objhash"),
                new TreeItem<String>("IndexingField-bg0-0-objsec"),
                new TreeItem<String>("IndexingField-bg0-0-outdocid"),
                new TreeItem<String>("IndexingField-bg0-0-parentid"),
                new TreeItem<String>("IndexingField-bg0-0-physicalid"),
                new TreeItem<String>("IndexingField-bg0-0-pmkeys"),
                new TreeItem<String>("IndexingField-bg0-0-primarytype"),
                new TreeItem<String>("IndexingField-bg0-0-savnbr"),
                new TreeItem<String>("IndexingField-bg0-0-snapshotcontentversion"),
                new TreeItem<String>("IndexingField-bg0-0-snapshottimestamp"),
                new TreeItem<String>("IndexingField-bg0-0-snapshotversion"),
                new TreeItem<String>("IndexingField-bg0-0-source"),
                new TreeItem<String>("IndexingField-bg0-0-subtypedefid"),
                new TreeItem<String>("IndexingField-bg0-0-tablename"),
                new TreeItem<String>("IndexingField-bg0-0-text"),
                new TreeItem<String>("IndexingField-bg0-0-title"),
                new TreeItem<String>("IndexingField-bg0-0-url"),
                new TreeItem<String>("IndexingField-bg0-1-ancestors"),
                new TreeItem<String>("IndexingField-bg0-1-cascadesource"),
                new TreeItem<String>("IndexingField-bg0-1-collectionid"),
                new TreeItem<String>("IndexingField-bg0-1-comment"),
                new TreeItem<String>("IndexingField-bg0-1-contentexception"),
                new TreeItem<String>("IndexingField-bg0-1-datearrive"),
                new TreeItem<String>("IndexingField-bg0-1-datecreated"),
                new TreeItem<String>("IndexingField-bg0-1-documentnbr"),
                new TreeItem<String>("IndexingField-bg0-1-externalid"),
                new TreeItem<String>("IndexingField-bg0-1-file_size"),
                new TreeItem<String>("IndexingField-bg0-1-filetitle"),
                new TreeItem<String>("IndexingField-bg0-1-id"),
                new TreeItem<String>("IndexingField-bg0-1-indexid"),
                new TreeItem<String>("IndexingField-bg0-1-indocid"),
                new TreeItem<String>("IndexingField-bg0-1-language"),
                new TreeItem<String>("IndexingField-bg0-1-metas"),
                new TreeItem<String>("IndexingField-bg0-1-note"),
                new TreeItem<String>("IndexingField-bg0-1-objhash"),
                new TreeItem<String>("IndexingField-bg0-1-objsec"),
                new TreeItem<String>("IndexingField-bg0-1-outdocid"),
                new TreeItem<String>("IndexingField-bg0-1-parentid"),
                new TreeItem<String>("IndexingField-bg0-1-physicalid"),
                new TreeItem<String>("IndexingField-bg0-1-pmkeys"),
                new TreeItem<String>("IndexingField-bg0-1-primarytype"),
                new TreeItem<String>("IndexingField-bg0-1-savnbr"),
                new TreeItem<String>("IndexingField-bg0-1-snapshotcontentversion"),
                new TreeItem<String>("IndexingField-bg0-1-snapshottimestamp"),
                new TreeItem<String>("IndexingField-bg0-1-snapshotversion"),
                new TreeItem<String>("IndexingField-bg0-1-source"),
                new TreeItem<String>("IndexingField-bg0-1-subtypedefid"),
                new TreeItem<String>("IndexingField-bg0-1-tablename"),
                new TreeItem<String>("IndexingField-bg0-1-text"),
                new TreeItem<String>("IndexingField-bg0-1-title"),
                new TreeItem<String>("IndexingField-bg0-1-url"),
                new TreeItem<String>("IndexingField-bg0-2-ancestors"),
                new TreeItem<String>("IndexingField-bg0-2-cascadesource"),
                new TreeItem<String>("IndexingField-bg0-2-collectionid"),
                new TreeItem<String>("IndexingField-bg0-2-comment"),
                new TreeItem<String>("IndexingField-bg0-2-contentexception"),
                new TreeItem<String>("IndexingField-bg0-2-datearrive"),
                new TreeItem<String>("IndexingField-bg0-2-datecreated"),
                new TreeItem<String>("IndexingField-bg0-2-documentnbr"),
                new TreeItem<String>("IndexingField-bg0-2-externalid"),
                new TreeItem<String>("IndexingField-bg0-2-file_size"),
                new TreeItem<String>("IndexingField-bg0-2-filetitle"),
                new TreeItem<String>("IndexingField-bg0-2-id"),
                new TreeItem<String>("IndexingField-bg0-2-indexid"),
                new TreeItem<String>("IndexingField-bg0-2-indocid"),
                new TreeItem<String>("IndexingField-bg0-2-language"),
                new TreeItem<String>("IndexingField-bg0-2-metas"),
                new TreeItem<String>("IndexingField-bg0-2-note"),
                new TreeItem<String>("IndexingField-bg0-2-objhash"),
                new TreeItem<String>("IndexingField-bg0-2-objsec"),
                new TreeItem<String>("IndexingField-bg0-2-outdocid"),
                new TreeItem<String>("IndexingField-bg0-2-parentid"),
                new TreeItem<String>("IndexingField-bg0-2-physicalid"),
                new TreeItem<String>("IndexingField-bg0-2-pmkeys"),
                new TreeItem<String>("IndexingField-bg0-2-primarytype"),
                new TreeItem<String>("IndexingField-bg0-2-savnbr"),
                new TreeItem<String>("IndexingField-bg0-2-snapshotcontentversion"),
                new TreeItem<String>("IndexingField-bg0-2-snapshottimestamp"),
                new TreeItem<String>("IndexingField-bg0-2-snapshotversion"),
                new TreeItem<String>("IndexingField-bg0-2-source"),
                new TreeItem<String>("IndexingField-bg0-2-subtypedefid"),
                new TreeItem<String>("IndexingField-bg0-2-tablename"),
                new TreeItem<String>("IndexingField-bg0-2-text"),
                new TreeItem<String>("IndexingField-bg0-2-title"),
                new TreeItem<String>("IndexingField-bg0-2-url"),
                new TreeItem<String>("IndexingField-bg0-3-ancestors"),
                new TreeItem<String>("IndexingField-bg0-3-cascadesource"),
                new TreeItem<String>("IndexingField-bg0-3-collectionid"),
                new TreeItem<String>("IndexingField-bg0-3-comment"),
                new TreeItem<String>("IndexingField-bg0-3-contentexception"),
                new TreeItem<String>("IndexingField-bg0-3-datearrive"),
                new TreeItem<String>("IndexingField-bg0-3-datecreated"),
                new TreeItem<String>("IndexingField-bg0-3-documentnbr"),
                new TreeItem<String>("IndexingField-bg0-3-externalid"),
                new TreeItem<String>("IndexingField-bg0-3-file_size"),
                new TreeItem<String>("IndexingField-bg0-3-filetitle"),
                new TreeItem<String>("IndexingField-bg0-3-id"),
                new TreeItem<String>("IndexingField-bg0-3-indexid"),
                new TreeItem<String>("IndexingField-bg0-3-indocid"),
                new TreeItem<String>("IndexingField-bg0-3-language"),
                new TreeItem<String>("IndexingField-bg0-3-metas"),
                new TreeItem<String>("IndexingField-bg0-3-note"),
                new TreeItem<String>("IndexingField-bg0-3-objhash"),
                new TreeItem<String>("IndexingField-bg0-3-objsec"),
                new TreeItem<String>("IndexingField-bg0-3-outdocid"),
                new TreeItem<String>("IndexingField-bg0-3-parentid"),
                new TreeItem<String>("IndexingField-bg0-3-physicalid"),
                new TreeItem<String>("IndexingField-bg0-3-pmkeys"),
                new TreeItem<String>("IndexingField-bg0-3-primarytype"),
                new TreeItem<String>("IndexingField-bg0-3-savnbr"),
                new TreeItem<String>("IndexingField-bg0-3-snapshotcontentversion"),
                new TreeItem<String>("IndexingField-bg0-3-snapshottimestamp"),
                new TreeItem<String>("IndexingField-bg0-3-snapshotversion"),
                new TreeItem<String>("IndexingField-bg0-3-source"),
                new TreeItem<String>("IndexingField-bg0-3-subtypedefid"),
                new TreeItem<String>("IndexingField-bg0-3-tablename"),
                new TreeItem<String>("IndexingField-bg0-3-text"),
                new TreeItem<String>("IndexingField-bg0-3-title"),
                new TreeItem<String>("IndexingField-bg0-3-url"),
                new TreeItem<String>("IndexingSlice-bg0-0-i0"),
                new TreeItem<String>("IndexingSlice-bg0-1-i0"),
                new TreeItem<String>("IndexingSlice-bg0-2-i0"),
                new TreeItem<String>("IndexingSlice-bg0-3-i0"),
                new TreeItem<String>("LinguisticManager-dos729.eng12.ocl-dict0"),
                new TreeItem<String>("NetIOPdh"),
                new TreeItem<String>("SearchCommand-dos729.eng12.ocl-search-api-10010"),
                new TreeItem<String>("SearchIndex-dos729.eng12.ocl-bg0-0-i0"),
                new TreeItem<String>("SearchIndex-dos729.eng12.ocl-bg0-1-i0"),
                new TreeItem<String>("SearchIndex-dos729.eng12.ocl-bg0-2-i0"),
                new TreeItem<String>("SearchIndex-dos729.eng12.ocl-bg0-3-i0"),
                new TreeItem<String>("SearchIndexPageCache-dos729.eng12.ocl-bg0-0-i0-cache0"),
                new TreeItem<String>("SearchIndexPageCache-dos729.eng12.ocl-bg0-1-i0-cache0"),
                new TreeItem<String>("SearchIndexPageCache-dos729.eng12.ocl-bg0-2-i0-cache0"),
                new TreeItem<String>("SearchIndexPageCache-dos729.eng12.ocl-bg0-3-i0-cache0"),
                new TreeItem<String>("du"),
                new TreeItem<String>("jvmpool-connectors-java0"),
                new TreeItem<String>("jvmpool-convert-c0"),
                new TreeItem<String>("jvmpool-gateway"),
                new TreeItem<String>("jvmpool-index6-bg0-i0"),
                new TreeItem<String>("jvmpool-indexingserver-bg0"),
                new TreeItem<String>("jvmpool-searchserver-ss0"),
                new TreeItem<String>("memprocPdh"),
                new TreeItem<String>("processPdh-Global CloudView processes"),
                new TreeItem<String>("processPdh-connectors-java0"),
                new TreeItem<String>("processPdh-convert-c0"),
                new TreeItem<String>("processPdh-crawler-exa0"),
                new TreeItem<String>("processPdh-gateway"),
                new TreeItem<String>("processPdh-index6-bg0-i0"),
                new TreeItem<String>("processPdh-indexingserver-bg0"),
                new TreeItem<String>("processPdh-master"),
                new TreeItem<String>("processPdh-searchserver-ss0")
        );
    }


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
        assert editMenu != null : "fx:id\"editMenu\" was not injected!";

        assert root != null : "fx:id\"root\" was not injected!";
        assert RDPEpsilon != null : "fx:id\"RDPEpsilon\" was not injected!";
        assert showChartSymbols != null : "fx:id\"showChartSymbols\" was not injected!";
        assert treeview != null : "fx:id\"treeview\" was not injected!";
        assert enableChartAnimation != null : "fx:id\"enableChartAnimation\" was not injected!";
        assert seriesTabPane != null : "fx:id\"seriesTabPane\" was not injected!";
        // assert  seriesTab1Controller != null : "fx:id\"seriesTab1Controller\" was not injected!";

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> handleControlKey(e, true));
        root.addEventFilter(KeyEvent.KEY_RELEASED, e -> handleControlKey(e, false));

        seriesTabPane.getSelectionModel().clearSelection();

        seriesTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                System.out.println("Tab selected: " + newValue.getText());
                if (newValue.getContent() == null) {
                    try {
                        // Loading content on demand
                        FXMLLoader fXMLLoader = new FXMLLoader();
                        Parent p = (Parent) fXMLLoader.load(getClass().getResource("/views/TimeSeriesView.fxml").openStream());
                        //    Parent p = FXMLLoader.load(getClass().getResource("/views/TimeSeriesView.fxml"));
                        newValue.setContent(p);

                        // OPTIONAL : Store the controller if needed
                        TimeSeriesController current = fXMLLoader.getController();
                        current.getCrossHair().showHorizontalMarkerProperty().bind(showHorizontalMarker);
                        current.getCrossHair().showVerticalMarkerProperty().bind(showVerticalMarker);
                        current.getChart().createSymbolsProperty().bindBidirectional(showChartSymbols.selectedProperty());
                        current.getChart().animatedProperty().bindBidirectional(enableChartAnimation.selectedProperty());

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
//        treeview.getSelectionModel().selectedItemProperty()
//                .addListener((observable, old_val, selected) -> {
//                    logger.debug(() -> "Selected Text : " + selected.getValue());
//                    if (selected.isLeaf()) {
//                        currentProbe.setValue(selected.getValue());
//                        refreshChart();
//                    }
//                });


        seriesTabPane.getTabs().add(new Tab("memprocPdh"));

        buildTreeViewForTarget("memprocPdh");


//        final TextFormatter<Integer> formatter = new TextFormatter<>(new IntegerStringConverter());
//
//        formatter.valueProperty().bindBidirectional(reductionTarget);
//        formatter.valueProperty().addListener((observable, oldValue, newValue) -> refreshChart());


//        RDPEpsilon.setTextFormatter(formatter);


        //   editRefresh.setOnAction(a -> refreshChart());


        // chkBoxEnableRDP.selectedProperty().addListener((o, oldval, newVal) -> refreshChart());


    }


}
