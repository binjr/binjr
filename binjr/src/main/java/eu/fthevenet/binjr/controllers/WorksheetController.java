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

import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.async.AsyncTaskManager;
import eu.fthevenet.binjr.data.workspace.Chart;
import eu.fthevenet.binjr.data.workspace.*;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.util.javafx.charts.*;
import eu.fthevenet.util.javafx.controls.ColorTableCell;
import eu.fthevenet.util.javafx.controls.DecimalFormatTableCellFactory;
import eu.fthevenet.util.javafx.controls.DelayedAction;
import eu.fthevenet.util.javafx.controls.ZonedDateTimePicker;
import eu.fthevenet.util.logging.Profiler;
import eu.fthevenet.util.text.BinaryPrefixFormatter;
import eu.fthevenet.util.text.MetricPrefixFormatter;
import eu.fthevenet.util.text.PrefixFormatter;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.MaskerPane;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;

/**
 * The controller class for the time series view.
 *
 * @author Frederic Thevenet
 */
public class WorksheetController implements Initializable, AutoCloseable {
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    private static final Logger logger = LogManager.getLogger(WorksheetController.class);
    private final GlobalPreferences globalPrefs = GlobalPreferences.getInstance();
    private final Worksheet<Double> worksheet;
    private static final double Y_AXIS_SEPARATION = 10;
    private final MainViewController parentController;

    @FXML
    public AnchorPane root;
    @FXML
    public AnchorPane chartParent;
    protected List<ChartViewPort<Double>> viewPorts = new ArrayList<>();
    @FXML
    private TextField yMinRange;
    @FXML
    private TextField yMaxRange;
    @FXML
    private Accordion seriesTableContainer;
    @FXML
    private Button backButton;
    @FXML
    private Button forwardButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button snapshotButton;
    @FXML
    private ZonedDateTimePicker startDate;
    @FXML
    private ZonedDateTimePicker endDate;
    @FXML
    private ToggleButton vCrosshair;
    @FXML
    private ToggleButton hCrosshair;
    @FXML
    private Button addChartButton;
    @FXML
    private MaskerPane worksheetMaskerPane;
    @FXML
    private ContextMenu seriesListMenu;

    // private StackPane settingsPane;
    private XYChartCrosshair<ZonedDateTime, Double> crossHair;
    private final ToggleGroup editButtonsGroup = new ToggleGroup();
    private ChartViewportsState currentState;// = new ChartViewportsState();

    private String name;
    private ChangeListener<Object> refreshOnPreferenceListener = (observable, oldValue, newValue) -> refresh();
    private ChangeListener<Object> refreshOnSelectSeries = (observable, oldValue, newValue) -> invalidateAll(false, false, false);
    private ChangeListener<ChartType> chartTypeListener;
    private ListChangeListener<Chart<Double>> chartListListener;
    private ChangeListener<? super UnitPrefixes> unitPrefixListener;
    private ChangeListener<Object> controllerReloadListener;
    public static final double TOOL_BUTTON_SIZE = 20;

    public WorksheetController(MainViewController parentController, Worksheet<Double> worksheet) throws IOException {
        this.parentController = parentController;
        this.worksheet = worksheet;
    }

    private ChartPropertiesController buildChartPropertiesController(Chart<Double> chart) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ChartPropertiesView.fxml"));
        ChartPropertiesController propertiesController = new ChartPropertiesController<>(getWorksheet(), chart);
        loader.setController(propertiesController);
        Pane settingsPane = loader.load();
        //  settingsPane = new StackPane(p);
        AnchorPane.setRightAnchor(settingsPane, ChartPropertiesController.SETTINGS_PANE_DISTANCE);
        AnchorPane.setBottomAnchor(settingsPane, 0.0);
        AnchorPane.setTopAnchor(settingsPane, 0.0);
        settingsPane.getStyleClass().add("toolPane");
        settingsPane.setPrefWidth(200);
        settingsPane.setMinWidth(200);
        chartParent.getChildren().add(settingsPane);
        //  settingsPane.toFront();
        Platform.runLater(settingsPane::toFront);
        return propertiesController;
    }

    //region [Properties]
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    //endregion

    /**
     * Returns the {@link Worksheet} instance associated with this controller
     *
     * @return the {@link Worksheet} instance associated with this controller
     */
    public Worksheet<Double> getWorksheet() {
        return this.worksheet;
    }

    //region [Initializable Members]
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //region *** Nodes injection checks ***
        assert root != null : "fx:id\"root\" was not injected!";
        assert chartParent != null : "fx:id\"chartParent\" was not injected!";
        assert seriesTableContainer != null : "fx:id\"seriesTableContainer\" was not injected!";
        assert backButton != null : "fx:id\"backButton\" was not injected!";
        assert forwardButton != null : "fx:id\"forwardButton\" was not injected!";
        assert startDate != null : "fx:id\"beginDateTime\" was not injected!";
        assert endDate != null : "fx:id\"endDateTime\" was not injected!";
        assert refreshButton != null : "fx:id\"refreshButton\" was not injected!";
        assert vCrosshair != null : "fx:id\"vCrosshair\" was not injected!";
        assert hCrosshair != null : "fx:id\"hCrosshair\" was not injected!";
        assert snapshotButton != null : "fx:id\"snapshotButton\" was not injected!";
        //endregion

        //region Control initialization
        try {
            initChartViewPorts();
        } catch (IOException e) {
            throw new RuntimeException("Failed to handle IOException", e);
        }
        initNavigationPane();
        initTableViewPane();
        //endregion
        Platform.runLater(() -> invalidateAll(false, false, false));
        //region *** Global preferences ***
        globalPrefs.downSamplingEnabledProperty().addListener(refreshOnPreferenceListener);
        globalPrefs.downSamplingThresholdProperty().addListener(refreshOnPreferenceListener);
        //endregion


    }

    // private DoubleBinding n;

    //region *** XYChart ***
    private void initChartViewPorts() throws IOException {
        ZonedDateTimeAxis xAxis = new ZonedDateTimeAxis(getWorksheet().getTimeZone());
        xAxis.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        xAxis.setAnimated(false);
        xAxis.setSide(Side.BOTTOM);
        for (Chart<Double> currentChart : getWorksheet().getCharts()) {
            StableTicksAxis yAxis;
            if (currentChart.getUnitPrefixes() == UnitPrefixes.BINARY) {
                yAxis = new BinaryStableTicksAxis();
            }
            else {
                yAxis = new MetricStableTicksAxis();
            }
            yAxis.autoRangingProperty().bindBidirectional(currentChart.autoScaleYAxisProperty());
            yAxis.setAnimated(false);
            yAxis.setTickSpacing(30);
            yAxis.labelProperty().bind(Bindings.createStringBinding(
                    () -> String.format("%s - %s", currentChart.getName(), currentChart.getUnit()),
                    currentChart.nameProperty(),
                    currentChart.unitProperty()));
            XYChart<ZonedDateTime, Double> viewPort;
            switch (currentChart.getChartType()) {
                case AREA:
                    viewPort = new AreaChart<>(xAxis, (ValueAxis) yAxis);
                    ((AreaChart) viewPort).setCreateSymbols(false);
                    break;
                case STACKED:
                    viewPort = new StackedAreaChart<>(xAxis, (ValueAxis) yAxis);
                    ((StackedAreaChart) viewPort).setCreateSymbols(false);
                    break;
                case SCATTER:
                    viewPort = new ScatterChart<>(xAxis, (ValueAxis) yAxis);
                    break;
                case LINE:
                default:
                    viewPort = new LineChart<>(xAxis, (ValueAxis) yAxis);
                    ((LineChart) viewPort).setCreateSymbols(false);
            }
            viewPort.setCache(true);
            viewPort.setCacheHint(CacheHint.SPEED);
            viewPort.setCacheShape(true);
            viewPort.setFocusTraversable(true);
            viewPort.setLegendVisible(false);
            viewPort.setAnimated(false);
            viewPorts.add(new ChartViewPort<>(currentChart, viewPort, buildChartPropertiesController(currentChart)));
        }
        for (int i = 0; i < viewPorts.size(); i++) {
            ChartViewPort<Double> v = viewPorts.get(i);
            XYChart<ZonedDateTime, Double> chart = v.getChart();
            int nbAdditionalCharts = getWorksheet().getCharts().size() - 1;
            DoubleBinding n = Bindings.createDoubleBinding(
                    () -> viewPorts.stream()
                            .filter(c -> !c.getChart().equals(chart))
                            .map(c -> c.getChart().getYAxis().getWidth())
                            .reduce(Double::sum).orElse(0.0) + (Y_AXIS_SEPARATION * nbAdditionalCharts),
                    viewPorts.stream().map(c -> c.getChart().getYAxis().widthProperty()).toArray(ReadOnlyDoubleProperty[]::new)
            );
            HBox hBox = new HBox(chart);
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.prefHeightProperty().bind(chartParent.heightProperty());
            hBox.prefWidthProperty().bind(chartParent.widthProperty());
            chart.minWidthProperty().bind(chartParent.widthProperty().subtract(n));
            chart.prefWidthProperty().bind(chartParent.widthProperty().subtract(n));
            chart.maxWidthProperty().bind(chartParent.widthProperty().subtract(n));
            if (i == 0) {
                chart.getYAxis().setSide(Side.LEFT);
            }
            else {
                chart.getYAxis().setSide(Side.RIGHT);
                chart.setVerticalZeroLineVisible(false);
                chart.setHorizontalZeroLineVisible(false);
                chart.setVerticalGridLinesVisible(false);
                chart.setHorizontalGridLinesVisible(false);
                chart.translateXProperty().bind(viewPorts.get(0).getChart().getYAxis().widthProperty());
                chart.getYAxis().translateXProperty().bind(Bindings.createDoubleBinding(
                        () -> viewPorts.stream()
                                .filter(c -> viewPorts.indexOf(c) != 0 && viewPorts.indexOf(c) < viewPorts.indexOf(v))
                                .map(c -> c.getChart().getYAxis().getWidth())
                                .reduce(Double::sum).orElse(0.0) + Y_AXIS_SEPARATION * (viewPorts.indexOf(v) - 1),
                        viewPorts.stream().map(c -> c.getChart().getYAxis().widthProperty()).toArray(ReadOnlyDoubleProperty[]::new)));
            }
            chartParent.getChildren().add(hBox);
        }
    }
    //endregion

    private void initNavigationPane() {
        //region *** Buttons ***
        backButton.setOnAction(this::handleHistoryBack);
        forwardButton.setOnAction(this::handleHistoryForward);
        refreshButton.setOnAction(this::handleRefresh);
        snapshotButton.setOnAction(this::handleTakeSnapshot);
        backButton.disableProperty().bind(getWorksheet().getBackwardHistory().emptyProperty());
        forwardButton.disableProperty().bind(getWorksheet().getForwardHistory().emptyProperty());
        addChartButton.setOnAction(this::handleAddNewChart);

        //endregion

        //region *** Time pickers ***

        currentState = new ChartViewportsState(getWorksheet().getFromDateTime(), getWorksheet().getToDateTime());

        for (ChartViewPort<Double> viewPort : viewPorts) {
            plotChart(viewPort, currentState.get(viewPort.getDataStore()).asSelection(), true);
        }

        endDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.dateTimeValueProperty().bindBidirectional(currentState.startXProperty());
        endDate.dateTimeValueProperty().bindBidirectional(currentState.endXProperty());

        //region *** Crosshair ***
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        LinkedHashMap<XYChart<ZonedDateTime, Double>, Function<Double, String>> map = new LinkedHashMap<>();
        viewPorts.forEach(v -> {
            map.put(v.chart, v.getPrefixFormatter()::format);
        });

        crossHair = new XYChartCrosshair<>(map,
                chartParent,
                dateTimeFormatter::format);
        crossHair.onSelectionDone(s -> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());
            currentState.setSelection(convertSelection(s), true);
        });
        hCrosshair.selectedProperty().bindBidirectional(globalPrefs.horizontalMarkerOnProperty());
        vCrosshair.selectedProperty().bindBidirectional(globalPrefs.verticalMarkerOnProperty());
        crossHair.horizontalMarkerVisibleProperty().bind(Bindings.createBooleanBinding(() -> globalPrefs.isShiftPressed() || hCrosshair.isSelected(), hCrosshair.selectedProperty(), globalPrefs.shiftPressedProperty()));
        crossHair.verticalMarkerVisibleProperty().bind(Bindings.createBooleanBinding(() -> globalPrefs.isCtrlPressed() || vCrosshair.isSelected(), vCrosshair.selectedProperty(), globalPrefs.ctrlPressedProperty()));
        //endregion
    }

    private Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> convertSelection(Map<XYChart<ZonedDateTime, Double>, XYChartSelection<ZonedDateTime, Double>> selection) {
        Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> result = new HashMap<>();
        selection.forEach((xyChart, xyChartSelection) -> {
            viewPorts.stream().filter(v -> v.getChart().equals(xyChart)).findFirst().ifPresent(viewPort -> result.put(viewPort.getDataStore(), xyChartSelection));
        });
        return result;
    }

    private void handleAddNewChart(ActionEvent actionEvent) {
        worksheet.getCharts().add(new Chart<>());
    }

    private void initTableViewPane() {
        for (ChartViewPort<Double> currentViewPort : viewPorts) {
            currentViewPort.getSeriesTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            CheckBox showAllCheckBox = new CheckBox();
            TableColumn<TimeSeriesInfo<Double>, Boolean> visibleColumn = new TableColumn<>();
            visibleColumn.setGraphic(showAllCheckBox);
            visibleColumn.setSortable(false);
            visibleColumn.setResizable(false);
            visibleColumn.setPrefWidth(32);

            InvalidationListener isVisibleListener = (observable) -> {
                boolean andAll = true;
                boolean orAll = false;
                for (TimeSeriesInfo<?> t : currentViewPort.getDataStore().getSeries()) {
                    andAll &= t.isSelected();
                    orAll |= t.isSelected();
                }
                showAllCheckBox.setIndeterminate(Boolean.logicalXor(andAll, orAll));
                showAllCheckBox.setSelected(andAll);
            };
            //TODO Make sure the following listeners do not prevent some object from being collected after a worksheet's been unloaded.
            visibleColumn.setCellValueFactory(p -> {
                p.getValue().selectedProperty().addListener(isVisibleListener);
                // Explicitly call the listener to initialize the proper status of the checkbox
                isVisibleListener.invalidated(null);
                return p.getValue().selectedProperty();
            });
            showAllCheckBox.setOnAction(event -> {
                boolean b = ((CheckBox) event.getSource()).isSelected();
                currentViewPort.getDataStore().getSeries().forEach(t -> t.setSelected(b));
            });

            DecimalFormatTableCellFactory<TimeSeriesInfo<Double>, String> alignRightCellFactory = new DecimalFormatTableCellFactory<>();
            alignRightCellFactory.setAlignment(TextAlignment.RIGHT);

            TableColumn<TimeSeriesInfo<Double>, Color> colorColumn = new TableColumn<>();
            colorColumn.setSortable(false);
            colorColumn.setResizable(false);
            colorColumn.setPrefWidth(32);

            TableColumn<TimeSeriesInfo<Double>, Boolean> nameColumn = new TableColumn<>("Name");
            nameColumn.setSortable(false);
            nameColumn.setPrefWidth(160);
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));


            TableColumn<TimeSeriesInfo<Double>, String> minColumn = new TableColumn<>("Min.");
            minColumn.setSortable(false);
            minColumn.setPrefWidth(75);
            minColumn.setCellFactory(alignRightCellFactory);

            TableColumn<TimeSeriesInfo<Double>, String> maxColumn = new TableColumn<>("Max.");
            maxColumn.setSortable(false);
            maxColumn.setPrefWidth(75);
            maxColumn.setCellFactory(alignRightCellFactory);

            TableColumn<TimeSeriesInfo<Double>, String> avgColumn = new TableColumn<>("Avg.");
            avgColumn.setSortable(false);
            avgColumn.setPrefWidth(75);
            avgColumn.setCellFactory(alignRightCellFactory);

            TableColumn<TimeSeriesInfo<Double>, String> currentColumn = new TableColumn<>("Current");
            currentColumn.setSortable(false);
            currentColumn.setPrefWidth(75);
            currentColumn.setCellFactory(alignRightCellFactory);
            currentColumn.getStyleClass().add("column-bold-text");

            TableColumn<TimeSeriesInfo<Double>, String> pathColumn = new TableColumn<>("Path");
            pathColumn.setSortable(false);
            pathColumn.setPrefWidth(400);

            currentColumn.setVisible(crossHair.isVerticalMarkerVisible());
            crossHair.verticalMarkerVisibleProperty().addListener((observable, oldValue, newValue) -> currentColumn.setVisible(newValue));

            visibleColumn.setCellFactory(CheckBoxTableCell.forTableColumn(visibleColumn));
            pathColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getBinding().getTreeHierarchy()));
            colorColumn.setCellFactory(param -> new ColorTableCell<>(colorColumn));
            colorColumn.setCellValueFactory(p -> p.getValue().displayColorProperty());
            avgColumn.setCellValueFactory(p -> Bindings.createStringBinding(
                    () -> p.getValue().getProcessor() == null ? "NaN" : currentViewPort.getPrefixFormatter().format(p.getValue().getProcessor().getAverageValue()),
                    p.getValue().processorProperty()));

            minColumn.setCellValueFactory(p -> Bindings.createStringBinding(
                    () -> p.getValue().getProcessor() == null ? "NaN" : currentViewPort.getPrefixFormatter().format(p.getValue().getProcessor().getMinValue()),
                    p.getValue().processorProperty()));

            maxColumn.setCellValueFactory(p -> Bindings.createStringBinding(
                    () -> p.getValue().getProcessor() == null ? "NaN" : currentViewPort.getPrefixFormatter().format(p.getValue().getProcessor().getMaxValue()),
                    p.getValue().processorProperty()));

            currentColumn.setCellValueFactory(p -> Bindings.createStringBinding(
                    () -> {
                        if (p.getValue().getProcessor() == null) {
                            return "NaN";
                        }
                        return currentViewPort.getPrefixFormatter().format(p.getValue().getProcessor().tryGetNearestValue(crossHair.getCurrentXValue()).orElse(Double.NaN));
                    }, crossHair.currentXValueProperty()));

            currentViewPort.getSeriesTable().setRowFactory(this::seriesTableRowFactory);

            currentViewPort.getSeriesTable().setOnKeyReleased(event -> {
                if (event.getCode().equals(KeyCode.DELETE)) {
                    removeSelectedBinding((TableView<TimeSeriesInfo<Double>>) event.getSource());
                }
            });
            currentViewPort.getSeriesTable().setItems(currentViewPort.getDataStore().getSeries());
            currentViewPort.getSeriesTable().getColumns().addAll(visibleColumn, colorColumn, nameColumn, minColumn, maxColumn, avgColumn, currentColumn, pathColumn);
            TitledPane newPane = new TitledPane(currentViewPort.getDataStore().getName(), currentViewPort.getSeriesTable());

            newPane.setOnDragOver(this::handleDragOverWorksheetView);
            newPane.setOnDragDropped(this::handleDragDroppedOnWorksheetView);
            newPane.setUserData(currentViewPort);

//            newPane.expandedProperty().addListener((observable) -> {
//
//                  if ( editButtonsGroup.getSelectedToggle() != null){
//                      currentViewPort.getDataStore().setShowProperties(true);
//                  }
//                    currentViewPort.getDataStore().setShowProperties(false);
//
//            });


            GridPane titleRegion = new GridPane();
            titleRegion.setHgap(5);
            titleRegion.getColumnConstraints().add(new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.LEFT, true));
            titleRegion.getColumnConstraints().add(new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.NEVER, HPos.RIGHT, false));
            titleRegion.minWidthProperty().bind(newPane.widthProperty().subtract(30));
            titleRegion.maxWidthProperty().bind(newPane.widthProperty().subtract(30));

            Label label = new Label();
            label.textProperty().bind(currentViewPort.getDataStore().nameProperty());
            label.visibleProperty().bind(currentViewPort.getDataStore().showPropertiesProperty().not());
            HBox editFieldsGroup = new HBox();
            DoubleBinding db = Bindings.createDoubleBinding(() -> editFieldsGroup.isVisible() ? USE_COMPUTED_SIZE : 0.0, editFieldsGroup.visibleProperty());
            editFieldsGroup.prefHeightProperty().bind(db);
            editFieldsGroup.maxHeightProperty().bind(db);
            editFieldsGroup.minHeightProperty().bind(db);
            editFieldsGroup.visibleProperty().bind(currentViewPort.getDataStore().showPropertiesProperty());
            editFieldsGroup.setSpacing(5);
            TextField chartNameField = new TextField();
            chartNameField.textProperty().bindBidirectional(currentViewPort.getDataStore().nameProperty());
            TextField unitNameField = new TextField();
            unitNameField.textProperty().bindBidirectional(currentViewPort.getDataStore().unitProperty());
            ChoiceBox<UnitPrefixes> unitPrefixChoiceBox = new ChoiceBox<>();
            unitPrefixChoiceBox.getItems().setAll(UnitPrefixes.values());
            unitPrefixChoiceBox.getSelectionModel().select(currentViewPort.getDataStore().getUnitPrefixes());
            currentViewPort.getDataStore().unitPrefixesProperty().bind(unitPrefixChoiceBox.getSelectionModel().selectedItemProperty());
            HBox.setHgrow(chartNameField, Priority.ALWAYS);
            titleRegion.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    chartNameField.selectAll();
                    chartNameField.requestFocus();
                    currentViewPort.getDataStore().setShowProperties(true);
                }
            });
            editFieldsGroup.getChildren().addAll(chartNameField, unitNameField, unitPrefixChoiceBox);

            // *** Toolbar ***
            HBox toolbar = new HBox();
            toolbar.getStyleClass().add("title-pane-tool-bar");
            toolbar.setAlignment(Pos.CENTER);
            Button closeButton = (Button) newToolBarButton(Button::new, "Close", "Remove this chart from the worksheet.", new String[]{"exit"}, new String[]{"cross-icon", "small-icon"});
            closeButton.setOnAction(event -> {
                if (Dialogs.confirmDialog(root, "Are you sure you want to remove chart \"" + currentViewPort.getDataStore().getName() + "\"?",
                        "", ButtonType.YES, ButtonType.NO) == ButtonType.YES) {
                    worksheet.getCharts().remove(currentViewPort.getDataStore());
                }
            });
            closeButton.disableProperty().bind(Bindings.createBooleanBinding(() -> worksheet.getCharts().size() > 1, worksheet.getCharts()).not());

            ToggleButton editButton = (ToggleButton) newToolBarButton(ToggleButton::new, "Settings", "Edit the chart's settings", new String[]{"dialog-button"}, new String[]{"settings-icon", "small-icon"});
            editButton.selectedProperty().bindBidirectional(currentViewPort.getDataStore().showPropertiesProperty());
            editButton.setOnAction(event -> newPane.setExpanded(true));

            editButtonsGroup.getToggles().add(editButton);

            Button moveUpButton = (Button) newToolBarButton(Button::new, "Up", "Move the chart up the list.", new String[]{"dialog-button"}, new String[]{"upArrow-icon", "small-icon"});
            moveUpButton.disableProperty().bind(Bindings.createBooleanBinding(() -> seriesTableContainer.getPanes().indexOf(newPane) == 0, seriesTableContainer.getPanes()));
            moveUpButton.visibleProperty().bind(currentViewPort.getDataStore().showPropertiesProperty());
            moveUpButton.setOnAction(event -> {
                int idx = worksheet.getCharts().indexOf(currentViewPort.dataStore);
                worksheet.getCharts().remove(currentViewPort.dataStore);
                worksheet.getCharts().add(idx - 1, currentViewPort.dataStore);
            });

            Button moveDownButton = (Button) newToolBarButton(Button::new, "Down", "Move the chart down the list.", new String[]{"dialog-button"}, new String[]{"downArrow-icon", "small-icon"});
            moveDownButton.disableProperty().bind(Bindings.createBooleanBinding(() -> seriesTableContainer.getPanes().indexOf(newPane) >= seriesTableContainer.getPanes().size() - 1, seriesTableContainer.getPanes()));
            moveDownButton.visibleProperty().bind(currentViewPort.getDataStore().showPropertiesProperty());
            moveDownButton.setOnAction(event -> {
                int idx = worksheet.getCharts().indexOf(currentViewPort.dataStore);
                worksheet.getCharts().remove(currentViewPort.dataStore);
                worksheet.getCharts().add(idx + 1, currentViewPort.dataStore);
            });


            toolbar.getChildren().addAll(moveUpButton, moveDownButton, editButton, closeButton);

            titleRegion.getChildren().addAll(label, editFieldsGroup, toolbar);
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER);
            GridPane.setConstraints(label, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER);
            GridPane.setConstraints(toolbar, 1, 0, 1, 1, HPos.RIGHT, VPos.CENTER);
            newPane.setGraphic(titleRegion);
            newPane.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            newPane.setAnimated(false);
            seriesTableContainer.getPanes().add(newPane);
        }
        Platform.runLater(() -> seriesTableContainer.getPanes().get(getWorksheet().getSelectedChart()).setExpanded(true));
        seriesTableContainer.expandedPaneProperty().addListener((ObservableValue<? extends TitledPane> observable, TitledPane oldPane, TitledPane newPane) -> {
            Boolean expandRequiered = true;
            for (TitledPane pane : seriesTableContainer.getPanes()) {
                if (pane.isExpanded()) {
                    expandRequiered = false;

                }
            }
            getAttachedViewport(newPane).ifPresent(nv -> {
                getWorksheet().setSelectedChart(viewPorts.indexOf(nv));
                if (editButtonsGroup.getSelectedToggle() != null) {
                    nv.getDataStore().setShowProperties(true);
                }

            });
            if ((expandRequiered) && (oldPane != null)) {
                getWorksheet().setSelectedChart(seriesTableContainer.getPanes().indexOf(oldPane));
                Platform.runLater(() -> {
                    seriesTableContainer.setExpandedPane(oldPane);
                });
            }
        });
    }

    private ButtonBase newToolBarButton(Supplier<ButtonBase> btnFactory, String text, String tooltipMsg, String[] styleClass, String[] iconStyleClass) {
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

    Optional<ChartViewPort<?>> getAttachedViewport(TitledPane pane) {
        if (pane != null && (pane.getUserData() instanceof ChartViewPort<?>)) {
            return Optional.of((ChartViewPort<?>) pane.getUserData());
        }
        return Optional.empty();
    }

    private void handleDragOverWorksheetView(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(MainViewController.TIME_SERIES_BINDING_FORMAT)) {
            event.acceptTransferModes(TransferMode.MOVE);
            event.consume();
        }
    }

    private void handleDragDroppedOnWorksheetView(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasContent(MainViewController.TIME_SERIES_BINDING_FORMAT)) {
            TreeView<TimeSeriesBinding<Double>> treeView = parentController.getSelectedTreeView();
            if (treeView != null) {
                TreeItem<TimeSeriesBinding<Double>> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null) {
                    Stage targetStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    if (targetStage != null) {
                        targetStage.requestFocus();
                    }
                    if (TransferMode.MOVE.equals(event.getAcceptedTransferMode())) {
                        try {
                            TitledPane droppedPane = (TitledPane) event.getSource();
                            droppedPane.setExpanded(true);
                            ChartViewPort<Double> viewPort = (ChartViewPort<Double>) droppedPane.getUserData();
                            List<TimeSeriesBinding<Double>> bindings = new ArrayList<>();
                            parentController.getAllBindingsFromBranch(item, bindings);
                            addBindings(bindings, viewPort.getDataStore());
                        } catch (Exception e) {
                            Dialogs.notifyException("Error adding bindings to existing worksheet", e);
                        }
                        logger.debug("dropped to " + event.toString());
                    }
                    else {
                        logger.warn("Unsupported drag and drop transfer mode: " + event.getAcceptedTransferMode());
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
    }

    //endregion


    @Override
    public void close() {
        if (chartListListener != null) {
            worksheet.getCharts().removeListener(chartListListener);
        }
        if (controllerReloadListener != null) {
            this.worksheet.getCharts().forEach(c -> {
                c.unitPrefixesProperty().removeListener(this.controllerReloadListener);
                c.chartTypeProperty().removeListener(this.controllerReloadListener);
            });
        }
        if (refreshOnPreferenceListener != null) {
            logger.debug(() -> "Unregister listeners attached to global preferences from controller for worksheet " + getWorksheet().getName());
            globalPrefs.downSamplingEnabledProperty().removeListener(refreshOnPreferenceListener);
            globalPrefs.downSamplingThresholdProperty().removeListener(refreshOnPreferenceListener);
            for (Chart<Double> chartData : getWorksheet().getCharts()) {
                for (TimeSeriesInfo<Double> t : chartData.getSeries()) {
                    t.selectedProperty().removeListener(refreshOnSelectSeries);
                }
            }
        }
    }

    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
        if (this.controllerReloadListener != null) {
            this.worksheet.getCharts().forEach(c -> {
                c.unitPrefixesProperty().removeListener(this.controllerReloadListener);
                c.chartTypeProperty().removeListener(this.controllerReloadListener);
            });
        }
        this.controllerReloadListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                logger.debug("Reloading worksheet controller because property changed from: " + oldValue + " to " + newValue);
                action.accept(this);
            }
        };
        this.worksheet.getCharts().forEach(c -> {
            c.unitPrefixesProperty().addListener(this.controllerReloadListener);
            c.chartTypeProperty().addListener(this.controllerReloadListener);
        });

        if (this.chartListListener != null) {
            worksheet.getCharts().removeListener(this.chartListListener);
        }
        this.chartListListener = c -> {
            while (c.next()) {
                if (c.wasPermutated()) {
                    for (int i = c.getFrom(); i < c.getTo(); ++i) {
                        // nothingfor now
                    }
                }
                else if (c.wasUpdated()) {
                    // nothingfor now
                }
                else {
                    if (c.wasAdded()) {
                        List<? extends Chart<Double>> added = c.getAddedSubList();
                        Chart<Double> chart = added.get(added.size() - 1);
                        int chartIndex = worksheet.getCharts().indexOf(chart);
                        worksheet.setSelectedChart(chartIndex);
                        chart.setShowProperties(true);
                    }
                    if (c.wasRemoved()) {
                        if (worksheet.getSelectedChart() == c.getFrom()) {
                            worksheet.setSelectedChart(Math.max(0, c.getFrom() - 1));
                        }
                        else if (worksheet.getSelectedChart() > c.getFrom()) {
                            worksheet.setSelectedChart(Math.max(0, worksheet.getSelectedChart() - 1));
                        }
                    }
                    logger.debug(() -> "Reloading worksheet controller because list changed: " + c.toString() + " in controller " + this.toString());
                    action.accept(this);
                }
            }
        };
        worksheet.getCharts().addListener(this.chartListListener);
    }

    //region *** protected members ***

    protected void addBindings(Collection<TimeSeriesBinding<Double>> bindings, Chart<Double> targetChart) {
        for (TimeSeriesBinding<Double> b : bindings) {
            TimeSeriesInfo<Double> newSeries = TimeSeriesInfo.fromBinding(b);
            newSeries.selectedProperty().addListener(refreshOnSelectSeries);
            targetChart.addSeries(newSeries);
        }
        invalidateAll(false, false, false);
    }

    protected void removeSelectedBinding(TableView<TimeSeriesInfo<Double>> seriesTable) {
        List<TimeSeriesInfo<Double>> selected = new ArrayList<>(seriesTable.getSelectionModel().getSelectedItems());
        seriesTable.getItems().removeAll(selected);
        seriesTable.getSelectionModel().clearSelection();
        invalidateAll(false, false, false);
    }

    protected void refresh() {
        invalidateAll(false, false, true);
    }

    @FXML
    protected void handleHistoryBack(ActionEvent actionEvent) {
        restoreSelectionFromHistory(getWorksheet().getBackwardHistory(), getWorksheet().getForwardHistory());
    }

    @FXML
    protected void handleHistoryForward(ActionEvent actionEvent) {
        restoreSelectionFromHistory(getWorksheet().getForwardHistory(), getWorksheet().getBackwardHistory());
    }

    @FXML
    protected void handleRefresh(ActionEvent actionEvent) {
        this.refresh();
    }

    @FXML
    protected void handleRemoveSeries(ActionEvent actionEvent) {
        removeSelectedBinding((TableView<TimeSeriesInfo<Double>>) actionEvent.getSource());
    }

    @FXML
    protected void handleTakeSnapshot(ActionEvent actionEvent) {
        saveSnapshot();
    }

    //endregion

    //region [Private Members]
    private void invalidateAll(boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh) {
        if (saveToHistory) {
            getWorksheet().getBackwardHistory().push(getWorksheet().getPreviousState());
            getWorksheet().getForwardHistory().clear();
        }
        getWorksheet().setPreviousState(currentState.asSelection());
        logger.debug(() -> getWorksheet().getBackwardHistory().dump());
        for (ChartViewPort<Double> viewPort : viewPorts) {
            invalidate(viewPort, saveToHistory, dontPlotChart, forceRefresh);
        }
    }

    private void invalidate(ChartViewPort<Double> viewPort, boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh) {
        try (Profiler p = Profiler.start("Refreshing chart " + getWorksheet().getName(), logger::trace)) {
            XYChartSelection<ZonedDateTime, Double> currentSelection = currentState.get(viewPort.getDataStore()).asSelection();
            logger.debug(() -> "currentSelection=" + (currentSelection == null ? "null" : currentSelection.toString()));

            if (!dontPlotChart) {
                plotChart(viewPort, currentSelection, forceRefresh);
            }
        }
    }

    //TODO make sure this is only called if worksheet is visible/current
    private void plotChart(ChartViewPort<Double> viewPort, XYChartSelection<ZonedDateTime, Double> currentSelection, boolean forceRefresh) {
        try (Profiler p = Profiler.start("Adding series to chart " + viewPort.getDataStore().getName(), logger::trace)) {
            worksheetMaskerPane.setVisible(true);
            AsyncTaskManager.getInstance().submit(() -> {
                        viewPort.getDataStore().fetchDataFromSources(currentSelection.getStartX(), currentSelection.getEndX(), forceRefresh);
                        return viewPort.getDataStore().getSeries()
                                .stream()
                                .filter(series -> {
                                    if (series.getProcessor() == null) {
                                        logger.warn("Series " + series.getDisplayName() + " does not contain any data to plot");
                                        return false;
                                    }
                                    if (!series.isSelected()) {
                                        logger.debug(() -> "Series " + series.getDisplayName() + " is not selected");
                                        return false;
                                    }
                                    return true;
                                })
                                .map(ts -> makeXYChartSeries(viewPort.getDataStore(), ts))
                                .collect(Collectors.toList());
                    },
                    event -> {
                        worksheetMaskerPane.setVisible(false);
                        viewPort.getChart().getData().setAll((Collection<? extends XYChart.Series<ZonedDateTime, Double>>) event.getSource().getValue());
                        // Force a redraw of the charts and their Y Axis considering their proper width.
                        new DelayedAction(Duration.millis(50), () -> viewPort.getChart().resize(0.0, 0.0)).submit();
                    },
                    event -> {
                        worksheetMaskerPane.setVisible(false);
                        Dialogs.notifyException("Failed to retrieve data from source", event.getSource().getException(), root);
                    });
        }
    }

    private XYChart.Series<ZonedDateTime, Double> makeXYChartSeries(Chart<Double> currentChart, TimeSeriesInfo<Double> series) {
        try (Profiler p = Profiler.start("Building  XYChart.Series data for" + series.getDisplayName(), logger::trace)) {
            XYChart.Series<ZonedDateTime, Double> newSeries = new XYChart.Series<>();
            newSeries.getData().setAll(series.getProcessor().getData());
            newSeries.nodeProperty().addListener((node, oldNode, newNode) -> {
                if (newNode != null) {
                    switch (currentChart.getChartType()) {
                        case AREA:
                        case STACKED:
                            ObservableList<Node> children = ((Group) newNode).getChildren();
                            if (children != null && children.size() >= 1) {
                                Path stroke = (Path) children.get(1);
                                Path fill = (Path) children.get(0);
                                logger.trace(() -> "Setting color of series " + series.getBinding().getLabel() + " to " + series.getDisplayColor());
                                stroke.visibleProperty().bind(currentChart.showAreaOutlineProperty());
                                stroke.strokeWidthProperty().bind(currentChart.strokeWidthProperty());
                                stroke.strokeProperty().bind(series.displayColorProperty());
                                fill.fillProperty().bind(Bindings.createObjectBinding(
                                        () -> series.getDisplayColor().deriveColor(0.0, 1.0, 1.0, currentChart.getGraphOpacity()),
                                        series.displayColorProperty(),
                                        currentChart.graphOpacityProperty()));
                            }
                            break;
                        case SCATTER:
                            //TODO set colors to points
                            break;
                        case LINE:
                            Path stroke = (Path) newNode;
                            logger.trace(() -> "Setting color of series " + series.getBinding().getLabel() + " to " + series.getDisplayColor());
                            stroke.strokeWidthProperty().bind(currentChart.strokeWidthProperty());
                            stroke.strokeProperty().bind(series.displayColorProperty());
                            break;
                        default:
                            break;
                    }
                }
            });
            return newSeries;
        }
    }

    private void saveSnapshot() {
        WritableImage snapImg = root.snapshot(null, null);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save SnapShot");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        fileChooser.setInitialDirectory(new File(globalPrefs.getMostRecentSaveFolder()));
        fileChooser.setInitialFileName(String.format("binjr_snapshot_%s.png", getWorksheet().getName()));
        File selectedFile = fileChooser.showSaveDialog(Dialogs.getStage(root));
        if (selectedFile != null) {
            try {
                if (selectedFile.getParent() != null) {
                    globalPrefs.setMostRecentSaveFolder(selectedFile.getParent());
                }
                ImageIO.write(
                        SwingFXUtils.fromFXImage(snapImg, null),
                        "png",
                        selectedFile);
            } catch (IOException e) {
                Dialogs.notifyException("Failed to save snapshot to disk", e, root);
            }
        }
    }

    private void restoreSelectionFromHistory(WorksheetNavigationHistory history, WorksheetNavigationHistory toHistory) {
        if (!history.isEmpty()) {
            toHistory.push(currentState.asSelection());
            currentState.setSelection(history.pop(), false);
        }
        else {
            logger.debug(() -> "History is empty: nothing to go back to.");
        }
    }

    private TableRow<TimeSeriesInfo<Double>> seriesTableRowFactory(TableView<TimeSeriesInfo<Double>> tv) {
        TableRow<TimeSeriesInfo<Double>> row = new TableRow<>();
        row.setOnDragDetected(event -> {
            if (!row.isEmpty()) {
                Integer index = row.getIndex();
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                db.setDragView(row.snapshot(null, null));
                ClipboardContent cc = new ClipboardContent();
                cc.put(SERIALIZED_MIME_TYPE, index);
                db.setContent(cc);
                event.consume();
            }
        });

        row.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE) && row.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        });

        row.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                TimeSeriesInfo<Double> draggedseries = tv.getItems().remove(draggedIndex);
                int dropIndex;
                if (row.isEmpty()) {
                    dropIndex = tv.getItems().size();
                }
                else {
                    dropIndex = row.getIndex();
                }
                tv.getItems().add(dropIndex, draggedseries);
                event.setDropCompleted(true);
                tv.getSelectionModel().clearAndSelect(dropIndex);
                invalidateAll(false, false, false);
                event.consume();
            }
        });
        return row;
    }

    public void toggleShowPropertiesPane() {
        ChartViewPort<Double> currentViewport = viewPorts.get(worksheet.getSelectedChart());
        if (currentViewport != null) {
            currentViewport.getDataStore().setShowProperties((editButtonsGroup.getSelectedToggle() == null));
        }
    }

    private class ChartViewPort<T extends Number> {
        private final Chart<T> dataStore;
        private final XYChart<ZonedDateTime, T> chart;
        private final ChartPropertiesController<T> propertiesController;
        private final PrefixFormatter prefixFormatter;
        private final TableView<TimeSeriesInfo<Double>> seriesTable;

        private ChartViewPort(Chart<T> dataStore, XYChart<ZonedDateTime, T> chart, ChartPropertiesController<T> propertiesController) {
            this.dataStore = dataStore;
            this.chart = chart;
            this.seriesTable = new TableView<>();
            this.seriesTable.getStyleClass().add("skinnable-pane-border");
            this.seriesTable.setEditable(true);
            this.propertiesController = propertiesController;
            switch (dataStore.getUnitPrefixes()) {
                case BINARY:
                    this.prefixFormatter = new BinaryPrefixFormatter();
                    break;
                case METRIC:
                    this.prefixFormatter = new MetricPrefixFormatter();
                    break;

                default:
                    throw new IllegalArgumentException("Unknown unit prefix");
            }
        }

        public ChartPropertiesController<T> getPropertiesController() {
            return propertiesController;
        }

        public XYChart<ZonedDateTime, T> getChart() {
            return chart;
        }

        public Chart<T> getDataStore() {
            return dataStore;
        }

        public PrefixFormatter getPrefixFormatter() {
            return prefixFormatter;
        }

        public TableView<TimeSeriesInfo<Double>> getSeriesTable() {
            return seriesTable;
        }

    }

//endregion

    /**
     * Represent the state of the time series view
     */
    private class ChartViewportsState {
        private HashMap<Chart<Double>, YAxisState> states = new HashMap<>();
        private final SimpleObjectProperty<ZonedDateTime> startX;
        private final SimpleObjectProperty<ZonedDateTime> endX;
        private boolean frozen;

        private class YAxisState {
            private final SimpleDoubleProperty startY;
            private final SimpleDoubleProperty endY;
            private final ChartViewPort<Double> chartViewPort;

            /**
             * Initializes a new instance of the {@link YAxisState} class.
             *
             * @param chartViewPort
             * @param startY        the lower bound of the Y axis
             * @param endY          the upper bound of the Y axis
             */
            public YAxisState(ChartViewPort<Double> chartViewPort, double startY, double endY) {
                this.chartViewPort = chartViewPort;
                this.startY = new SimpleDoubleProperty(roundYValue(startY));
                this.endY = new SimpleDoubleProperty(roundYValue(endY));

                this.startY.addListener((observable, oldValue, newValue) -> {
                    if (!frozen) {
                        invalidate(chartViewPort, false, true, false);
                    }
                });
                this.endY.addListener((observable, oldValue, newValue) -> {
                    if (!frozen) {
                        invalidate(chartViewPort, false, true, false);
                    }
                });
            }

            /**
             * Returns the current state as a {@link XYChartSelection}
             *
             * @return the current state as a {@link XYChartSelection}
             */
            XYChartSelection<ZonedDateTime, Double> asSelection() {
                return new XYChartSelection<>(
                        getStartX(),
                        getEndX(),
                        startY.get(),
                        endY.get(),
                        chartViewPort.getDataStore().isAutoScaleYAxis()
                );
            }

            /**
             * Sets the current state from a {@link XYChartSelection}
             *
             * @param selection the {@link XYChartSelection} to set as the current state
             * @param toHistory true if the change in state should be recorded in the history
             */
            void setSelection(XYChartSelection<ZonedDateTime, Double> selection, boolean toHistory) {
                if (toHistory) {
                    double r = (((ValueAxis<Double>) chartViewPort.getChart().getYAxis()).getUpperBound() - ((ValueAxis<Double>) chartViewPort.getChart().getYAxis()).getLowerBound()) - Math.abs(selection.getEndY() - selection.getStartY());
                    logger.debug(() -> "Y selection - Y axis range = " + r);
                    if (r > 0.0001) {
                        chartViewPort.getDataStore().setAutoScaleYAxis(false);
                    }
                }
                else {
                    // Disable auto range on Y axis if zoomed in
                    chartViewPort.getDataStore().setAutoScaleYAxis(selection.isAutoRangeY());
                }
                this.startY.set(roundYValue(selection.getStartY()));
                this.endY.set(roundYValue(selection.getEndY()));
            }

            private double roundYValue(double y) {
                return y;
            }
        }

        public ChartViewportsState(ZonedDateTime startX, ZonedDateTime endX) {
            this.startX = new SimpleObjectProperty<>(roundDateTime(startX));
            this.endX = new SimpleObjectProperty<>(roundDateTime(endX));


            this.startX.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidateAll(true, false, false);
                }
            });
            this.endX.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidateAll(true, false, false);
                }
            });

            for (ChartViewPort<Double> viewPort : viewPorts) {
                this.put(viewPort.getDataStore(), new YAxisState(viewPort, viewPort.getDataStore().getyAxisMinValue(), viewPort.getDataStore().getyAxisMaxValue()));
                viewPort.getDataStore().yAxisMinValueProperty().bindBidirectional(this.get(viewPort.getDataStore()).startY);
                ((ValueAxis<Double>) viewPort.getChart().getYAxis()).lowerBoundProperty().bindBidirectional(this.get(viewPort.getDataStore()).startY);
                viewPort.getDataStore().yAxisMaxValueProperty().bindBidirectional(this.get(viewPort.getDataStore()).endY);
                ((ValueAxis<Double>) viewPort.getChart().getYAxis()).upperBoundProperty().bindBidirectional(this.get(viewPort.getDataStore()).endY);
            }
            getWorksheet().fromDateTimeProperty().bind(this.startX);
            getWorksheet().toDateTimeProperty().bind(this.endX);

        }

        public ZonedDateTime getStartX() {
            return startX.get();
        }

        public SimpleObjectProperty<ZonedDateTime> startXProperty() {
            return startX;
        }

        public ZonedDateTime getEndX() {
            return endX.get();
        }

        public SimpleObjectProperty<ZonedDateTime> endXProperty() {
            return endX;
        }

        public Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> asSelection() {
            Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> selection = new HashMap<>();
            for (Map.Entry<Chart<Double>, YAxisState> e : states.entrySet()) {
                selection.put(e.getKey(), e.getValue().asSelection());
            }
            return selection;
        }

        public void setSelection(Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> selectionMap, boolean toHistory) {
            selectionMap.forEach((chart, xyChartSelection) -> states.get(chart).setSelection(xyChartSelection, toHistory));
            frozen = true;
            try {
                selectionMap.entrySet().stream().findFirst().ifPresent(entry -> {
                    ZonedDateTime newStartX = roundDateTime(entry.getValue().getStartX());
                    ZonedDateTime newEndX = roundDateTime(entry.getValue().getEndX());
                    boolean dontPlotChart = newStartX.isEqual(startX.get()) && newEndX.isEqual(endX.get());
                    this.startX.set(newStartX);
                    this.endX.set(newEndX);
                    selectionMap.forEach((chart, xyChartSelection) -> states.get(chart).setSelection(xyChartSelection, toHistory));
                    invalidateAll(toHistory, dontPlotChart, false);
                });
            } finally {
                frozen = false;
            }
        }

        public YAxisState put(Chart<Double> chart, YAxisState xyChartViewState) {
            return states.put(chart, xyChartViewState);
        }

        public YAxisState get(Chart<Double> chart) {
            return states.get(chart);
        }

        private ZonedDateTime roundDateTime(ZonedDateTime zdt) {
            return ZonedDateTime.of(zdt.getYear(),
                    zdt.getMonthValue(),
                    zdt.getDayOfMonth(),
                    zdt.getHour(),
                    zdt.getMinute(),
                    zdt.getSecond(),
                    0,
                    zdt.getZone()
            );
        }

    }


}
