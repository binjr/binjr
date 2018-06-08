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
import javafx.beans.property.*;
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
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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

    private StackPane settingsPane;
    // private ChartPropertiesController propertiesController;
    private ToggleButton chartPropertiesButton;
    private XYChartCrosshair<ZonedDateTime, Double> crossHair;

    private XYChartViewState currentState;
    private XYChartSelection<ZonedDateTime, Double> previousState;
    private History backwardHistory = new History();
    private History forwardHistory = new History();
    private String name;
    private ChangeListener<Object> refreshOnPreferenceListener = (observable, oldValue, newValue) -> refresh();
    private ChangeListener<Object> refreshOnSelectSeries = (observable, oldValue, newValue) -> invalidate(false, false, false);
    private ChangeListener<ChartType> chartTypeListener;
    private ListChangeListener<Chart<Double>> chartListListener;


    public WorksheetController(MainViewController parentController, Worksheet<Double> worksheet) throws IOException {
        this.parentController = parentController;
        this.worksheet = worksheet;
    }

    private ChartPropertiesController buildChartPropertiesController(Chart<Double> chart) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ChartPropertiesView.fxml"));
        ChartPropertiesController propertiesController = new ChartPropertiesController<>(chart);
        loader.setController(propertiesController);
        Parent p = loader.load();
        settingsPane = new StackPane(p);
        AnchorPane.setRightAnchor(settingsPane, ChartPropertiesController.SETTINGS_PANE_DISTANCE);
        AnchorPane.setBottomAnchor(settingsPane, 0.0);
        AnchorPane.setTopAnchor(settingsPane, 0.0);
        settingsPane.getStyleClass().add("toolPane");
        settingsPane.setPrefWidth(200);
        settingsPane.setMinWidth(200);
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
        //    initChartSettingPane();
        initTableViewPane();
        //endregion
        Platform.runLater(() -> invalidate(false, false, false));

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
        backButton.disableProperty().bind(backwardHistory.emptyStackProperty);
        forwardButton.disableProperty().bind(forwardHistory.emptyStackProperty);
        addChartButton.setOnAction(this::handleAddNewChart);

        //endregion

        //region *** Time pickers ***
        this.currentState = new XYChartViewState(viewPorts.get(0), getWorksheet().getFromDateTime(), getWorksheet().getToDateTime(), 0, 100);
        getWorksheet().fromDateTimeProperty().bind(currentState.startX);
        getWorksheet().toDateTimeProperty().bind(currentState.endX);
        plotChart(currentState.asSelection(), true);
        endDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.dateTimeValueProperty().bindBidirectional(currentState.startX);
        endDate.dateTimeValueProperty().bindBidirectional(currentState.endX);
        //endregion

        //region *** Crosshair ***
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        crossHair = new XYChartCrosshair<>(viewPorts.get(viewPorts.size() - 1).chart, chartParent, dateTimeFormatter::format, n -> String.format("%,.2f", n.doubleValue()));
        crossHair.onSelectionDone(s -> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());
            currentState.setSelection(s, true);
        });
        hCrosshair.selectedProperty().bindBidirectional(globalPrefs.horizontalMarkerOnProperty());
        vCrosshair.selectedProperty().bindBidirectional(globalPrefs.verticalMarkerOnProperty());
        crossHair.horizontalMarkerVisibleProperty().bind(Bindings.createBooleanBinding(() -> globalPrefs.isShiftPressed() || hCrosshair.isSelected(), hCrosshair.selectedProperty(), globalPrefs.shiftPressedProperty()));
        crossHair.verticalMarkerVisibleProperty().bind(Bindings.createBooleanBinding(() -> globalPrefs.isCtrlPressed() || vCrosshair.isSelected(), vCrosshair.selectedProperty(), globalPrefs.ctrlPressedProperty()));

//        worksheet.getCharts().get(0).yAxisMinValueProperty().bindBidirectional(currentState.startY);
//        ((ValueAxis<Double>) viewPorts.getYAxis()).lowerBoundProperty().bindBidirectional(currentState.startY);
//        worksheet.getDefaultChart().yAxisMaxValueProperty().bindBidirectional(currentState.endY);
//        ((ValueAxis<Double>) viewPorts.getYAxis()).upperBoundProperty().bindBidirectional(currentState.endY);

        //endregion
    }

    private void handleAddNewChart(ActionEvent actionEvent) {
        worksheet.getCharts().add(new Chart<>());
    }

    private void initChartSettingPane() {
        Region r = new Region();
        r.getStyleClass().add("settings-icon");
        chartPropertiesButton = new ToggleButton("", r);
        chartPropertiesButton.setMinSize(40, 40);
        chartPropertiesButton.setPrefSize(40, 40);
        chartPropertiesButton.setMaxSize(40, 40);
        chartPropertiesButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        chartPropertiesButton.getStyleClass().add("chart-properties-button");
        AnchorPane.setTopAnchor(chartPropertiesButton, 5.0);
        AnchorPane.setRightAnchor(chartPropertiesButton, 5.0);
        // chartPropertiesButton.selectedProperty().bindBidirectional(propertiesController.visibleProperty());
        chartParent.getChildren().add(chartPropertiesButton);
        chartParent.getChildren().add(settingsPane);
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

            TableColumn<TimeSeriesInfo<Double>, String> maxColumn = new TableColumn<>("Max.");
            maxColumn.setSortable(false);
            maxColumn.setPrefWidth(75);

            TableColumn<TimeSeriesInfo<Double>, String> avgColumn = new TableColumn<>("Avg.");
            avgColumn.setSortable(false);
            avgColumn.setPrefWidth(75);

            TableColumn<TimeSeriesInfo<Double>, String> currentColumn = new TableColumn<>("Current");
            currentColumn.setSortable(false);
            currentColumn.setPrefWidth(75);

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

            GridPane titleRegion = new GridPane();
            titleRegion.getColumnConstraints().add(new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.LEFT, true));
            titleRegion.getColumnConstraints().add(new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.NEVER, HPos.RIGHT, false));
            titleRegion.minWidthProperty().bind(newPane.widthProperty().subtract(30));
            titleRegion.maxWidthProperty().bind(newPane.widthProperty().subtract(30));
            TextField textField = new TextField();
            DoubleBinding db = Bindings.createDoubleBinding(() -> textField.isVisible() ? USE_COMPUTED_SIZE : 0.0, textField.visibleProperty());
            textField.prefHeightProperty().bind(db);
            textField.maxHeightProperty().bind(db);
            textField.minHeightProperty().bind(db);
            Label label = new Label();
            label.textProperty().bind(currentViewPort.getDataStore().nameProperty());
            textField.visibleProperty().bind(label.visibleProperty().not());
            titleRegion.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    textField.setText(currentViewPort.getDataStore().getName());
                    label.setVisible(false);
                    textField.selectAll();
                    textField.requestFocus();
                }
            });
            textField.setOnAction(event -> {
                if (!textField.getText().isEmpty()) {
                    currentViewPort.getDataStore().setName(textField.getText());
                }
                label.setVisible(true);

            });
            textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    if (!textField.getText().isEmpty()) {
                        currentViewPort.getDataStore().setName(textField.getText());
                    }
                    label.setVisible(true);
                }
            });
            HBox toolbar = new HBox();
            toolbar.setSpacing(5);
            toolbar.setAlignment(Pos.CENTER);
            final double BUTTON_SIZE = 14;
            Button closeButton = new Button("Close");
            closeButton.setPrefHeight(BUTTON_SIZE);
            closeButton.setMaxHeight(BUTTON_SIZE);
            closeButton.setMinHeight(BUTTON_SIZE);
            closeButton.setPrefWidth(BUTTON_SIZE);
            closeButton.setMaxWidth(BUTTON_SIZE);
            closeButton.setMinWidth(BUTTON_SIZE);
            closeButton.getStyleClass().add("exit");
            closeButton.setAlignment(Pos.CENTER);
            Region icon = new Region();
            icon.getStyleClass().add("cross-icon");
            icon.setStyle(" -icon-scale-x: 1.5;-icon-scale-y: 1.5");
            closeButton.setGraphic(icon);
            closeButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            closeButton.setOnAction(event -> worksheet.getCharts().remove(currentViewPort.dataStore));
            closeButton.disableProperty().bind(Bindings.createBooleanBinding(() -> worksheet.getCharts().size() > 1, worksheet.getCharts()).not());
            closeButton.setTooltip(new Tooltip("Remove this chart from the worksheet."));

            Button editButton = new Button("Settings");
            editButton.setPrefHeight(BUTTON_SIZE);
            editButton.setMaxHeight(BUTTON_SIZE);
            editButton.setMinHeight(BUTTON_SIZE);
            editButton.setPrefWidth(BUTTON_SIZE);
            editButton.setMaxWidth(BUTTON_SIZE);
            editButton.setMinWidth(BUTTON_SIZE);
            editButton.getStyleClass().add("dialog-button");
            editButton.setAlignment(Pos.CENTER);
            Region editIcon = new Region();
            editIcon.getStyleClass().add("settings-icon");
            editIcon.setStyle(" -icon-scale-x: 1.5;-icon-scale-y: 1.5");
            editButton.setGraphic(editIcon);
            editButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            editButton.setTooltip(new Tooltip("Edit the chart's settings"));

            // editButton.setOnAction();
            toolbar.getChildren().addAll(editButton, closeButton);

            titleRegion.getChildren().addAll(label, textField, toolbar);
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER);
            GridPane.setConstraints(label, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER);
            GridPane.setConstraints(toolbar, 1, 0, 1, 1, HPos.RIGHT, VPos.CENTER);

            newPane.setGraphic(titleRegion);
            newPane.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            newPane.setAnimated(false);
            seriesTableContainer.getPanes().add(newPane);
        }

        seriesTableContainer.expandedPaneProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                getWorksheet().setSelectedChart(seriesTableContainer.getPanes().indexOf(newValue));
            }
            else {
                getWorksheet().setSelectedChart(0);
            }
        }));

        Platform.runLater(() -> seriesTableContainer.getPanes().get(getWorksheet().getSelectedChart()).setExpanded(true));
        /* Make sure the accordion can never be completely collapsed */
        seriesTableContainer.expandedPaneProperty().addListener((ObservableValue<? extends TitledPane> observable, TitledPane oldPane, TitledPane newPane) -> {
            Boolean expand = true; // This value will change to false if there's (at least) one pane that is in "expanded" state, so we don't have to expand anything manually
            for (TitledPane pane : seriesTableContainer.getPanes()) {
                if (pane.isExpanded()) {
                    expand = false;
                }
            }
            /* Here we already know whether we need to expand the old pane again */
            if ((expand) && (oldPane != null)) {
                Platform.runLater(() -> {
                    seriesTableContainer.setExpandedPane(oldPane);
                });
            }
        });
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
                        //parentController.addToCurrentWorksheet(item);
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

    public void showPropertiesPane(boolean show) {
        this.chartPropertiesButton.setSelected(show);
    }

    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
//        if (this.chartTypeListener != null) {
//           this.worksheet.getDefaultChart().chartTypeProperty().removeListener(this.chartTypeListener);
//
//        }
//        this.chartTypeListener = (observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                logger.debug("Reloading worksheet controller because chart type change from: " + oldValue + " to " + newValue);
//                action.accept(this);
//            }
//        };
//        this.worksheet.getDefaultChart().chartTypeProperty().addListener(this.chartTypeListener);

        /////////////////////////////////////////////////////////////////////

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
                        worksheet.setSelectedChart(worksheet.getCharts().indexOf(added.get(added.size() - 1)));
                    }
                    if (c.wasRemoved()) {
                        //   List<? extends Chart<Double>> removed = c.getRemoved();
                        if (worksheet.getSelectedChart() == c.getFrom()) {
                            worksheet.setSelectedChart(Math.max(0, c.getFrom() - 1));
                        }
                    }
                    logger.debug(() -> "Observable list change=" + c.toString() + " in ctrler " + this.toString());
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
        invalidate(false, false, false);
    }

    protected void removeSelectedBinding(TableView<TimeSeriesInfo<Double>> seriesTable) {
        List<TimeSeriesInfo<Double>> selected = new ArrayList<>(seriesTable.getSelectionModel().getSelectedItems());
        seriesTable.getItems().removeAll(selected);
        seriesTable.getSelectionModel().clearSelection();
        invalidate(false, false, false);
    }

    protected void refresh() {
        invalidate(false, false, true);
    }

    @FXML
    protected void handleHistoryBack(ActionEvent actionEvent) {
        restoreSelectionFromHistory(backwardHistory, forwardHistory);
    }

    @FXML
    protected void handleHistoryForward(ActionEvent actionEvent) {
        restoreSelectionFromHistory(forwardHistory, backwardHistory);
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

    private void invalidate(boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh) {
        try (Profiler p = Profiler.start("Refreshing chart " + getWorksheet().getName(), logger::trace)) {
            XYChartSelection<ZonedDateTime, Double> currentSelection = currentState.asSelection();
            logger.debug(() -> "currentSelection=" + (currentSelection == null ? "null" : currentSelection.toString()));
            if (saveToHistory) {
                this.backwardHistory.push(previousState);
                this.forwardHistory.clear();
            }
            previousState = currentState.asSelection();
            logger.debug(() -> backwardHistory.dump());
            if (!dontPlotChart) {
                plotChart(currentSelection, forceRefresh);
            }
        }
    }

    //TODO make sure this is only called if worksheet is visible/current
    private void plotChart(XYChartSelection<ZonedDateTime, Double> currentSelection, boolean forceRefresh) {

        for (ChartViewPort<Double> viewPort : viewPorts) {
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
                        },
                        event -> {
                            worksheetMaskerPane.setVisible(false);
                            Dialogs.notifyException("Failed to retrieve data from source", event.getSource().getException(), root);
                        });
            }
        }
        // This is a bit of a hack destined to force a redraw of the charts and their Y Axis considering their proper width.
        // Ideally, this should be timed right after the width property of concerned controls are updated, instead of an
        // arbitrary delay.
        new DelayedAction(Duration.millis(50), () -> viewPorts.forEach(v -> v.getChart().resize(0.0, 0.0))).submit();
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

    private void restoreSelectionFromHistory(History history, History toHistory) {
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
                invalidate(false, false, false);
                event.consume();
            }
        });
        return row;
    }

    /**
     * Wraps a stack to record user navigation steps.
     */
    private class History {
        private final Deque<XYChartSelection<ZonedDateTime, Double>> stack = new ArrayDeque<>();
        private final SimpleBooleanProperty emptyStackProperty = new SimpleBooleanProperty(true);

        /**
         * Put the provided {@link XYChartSelection} on the top of the stack.
         *
         * @param state the provided {@link XYChartSelection}
         * @return the provided {@link XYChartSelection}
         */
        void push(XYChartSelection<ZonedDateTime, Double> state) {
            if (state == null) {
                logger.warn(() -> "Trying to push null state into backwardHistory");
                return;
            }
            emptyStackProperty.set(false);
            this.stack.push(state);
        }

        /**
         * Clears the history
         */
        void clear() {
            this.stack.clear();
            emptyStackProperty.set(true);
        }

        /**
         * Gets the topmost {@link XYChartSelection} from the stack.
         *
         * @return the topmost {@link XYChartSelection} from the stack.
         */
        XYChartSelection<ZonedDateTime, Double> pop() {
            XYChartSelection<ZonedDateTime, Double> r = this.stack.pop();
            emptyStackProperty.set(stack.isEmpty());
            return r;
        }

        /**
         * Returns true if the underlying stack is empty, false otherwise.
         *
         * @return true if the underlying stack is empty, false otherwise.
         */
        boolean isEmpty() {
            return emptyStackProperty.get();
        }

        @Override
        public String toString() {
            return this.dump();
        }

        /**
         * Dumps the content of the stack as a string
         *
         * @return the content of the stack as a string
         */
        private String dump() {
            final StringBuilder sb = new StringBuilder("History:");
            AtomicInteger pos = new AtomicInteger(0);
            if (this.isEmpty()) {
                sb.append(" { empty }");
            }
            else {
                stack.forEach(h -> sb.append("\n").append(pos.incrementAndGet()).append(" ->").append(h.toString()));
            }
            return sb.toString();
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
    private class XYChartViewState {
        private final SimpleObjectProperty<ZonedDateTime> startX;
        private final SimpleObjectProperty<ZonedDateTime> endX;
        private final SimpleDoubleProperty startY;
        private final SimpleDoubleProperty endY;
        private boolean frozen;
        private final ChartViewPort<Double> chartViewPort;

        /**
         * Initializes a new instance of the {@link XYChartViewState} class.
         *
         * @param chartViewPort
         * @param startX        the start of the time interval
         * @param endX          the end of the time interval
         * @param startY        the lower bound of the Y axis
         * @param endY          the upper bound of the Y axis
         */
        public XYChartViewState(ChartViewPort<Double> chartViewPort, ZonedDateTime startX, ZonedDateTime endX, double startY, double endY) {
            this.chartViewPort = chartViewPort;
            this.startX = new SimpleObjectProperty<>(roundDateTime(startX));
            this.endX = new SimpleObjectProperty<>(roundDateTime(endX));
            this.startY = new SimpleDoubleProperty(roundYValue(startY));
            this.endY = new SimpleDoubleProperty(roundYValue(endY));

            this.startX.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(true, false, false);
                }
            });
            this.endX.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(true, false, false);
                }
            });
            this.startY.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(false, true, false);
                }
            });
            this.endY.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(false, true, false);
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
                    startX.get(),
                    endX.get(),
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
            frozen = true;
            try {
                ZonedDateTime newStartX = roundDateTime(selection.getStartX());
                ZonedDateTime newEndX = roundDateTime(selection.getEndX());
                boolean dontPlotChart = newStartX.isEqual(startX.get()) && newEndX.isEqual(endX.get());
                this.startX.set(newStartX);
                this.endX.set(newEndX);
                // Disable auto range on Y axis if zoomed in
                if (toHistory) {
                    double r = (((ValueAxis<Double>) chartViewPort.getChart().getYAxis()).getUpperBound() - ((ValueAxis<Double>) chartViewPort.getChart().getYAxis()).getLowerBound()) - Math.abs(selection.getEndY() - selection.getStartY());
                    logger.debug(() -> "Y selection - Y axis range = " + r);
                    if (r > 0.0001) {
                        chartViewPort.getDataStore().setAutoScaleYAxis(false);
                    }
                }
                else {
                    chartViewPort.getDataStore().setAutoScaleYAxis(selection.isAutoRangeY());
                }

                this.startY.set(roundYValue(selection.getStartY()));
                this.endY.set(roundYValue(selection.getEndY()));
                invalidate(toHistory, dontPlotChart, false);
            } finally {
                frozen = false;
            }
        }

        private double roundYValue(double y) {
            return y;
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

        @Override
        public String toString() {
            return String.format("XYChartViewState{startX=%s, endX=%s, startY=%s, endY=%s}", startX.get().toString(), endX.get().toString(), startY.get(), endY.get());
        }
    }
}
