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
import eu.fthevenet.binjr.data.async.AsyncTaskManager;
import eu.fthevenet.binjr.data.exceptions.NoAdapterFoundException;
import eu.fthevenet.binjr.data.workspace.Chart;
import eu.fthevenet.binjr.data.workspace.*;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.util.javafx.charts.*;
import eu.fthevenet.util.javafx.controls.ColorTableCell;
import eu.fthevenet.util.javafx.controls.DecimalFormatTableCellFactory;
import eu.fthevenet.util.javafx.controls.ZonedDateTimePicker;
import eu.fthevenet.util.javafx.listeners.ChangeListenerFactory;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
    private volatile boolean preventReload = false;
    AtomicBoolean closed = new AtomicBoolean(false);

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
    @FXML
    private MenuButton selectChartLayout;

    private XYChartCrosshair<ZonedDateTime, Double> crossHair;
    private final ToggleGroup editButtonsGroup = new ToggleGroup();
    private ChartViewportsState currentState;
    private String name;
    private final ChangeListenerFactory listenerFactory = new ChangeListenerFactory();
    public static final double TOOL_BUTTON_SIZE = 20;


    public WorksheetController(MainViewController parentController, Worksheet<Double> worksheet, Map<Tab, DataAdapter> sourcesAdapters) throws IOException, NoAdapterFoundException {
        this.parentController = parentController;
        this.worksheet = worksheet;

        // Attach bindings
        for (Chart<Double> chart : worksheet.getCharts()) {
            for (TimeSeriesInfo<?> s : chart.getSeries()) {
                UUID id = s.getBinding().getAdapterId();
                DataAdapter<?, ?> da = sourcesAdapters.values()
                        .stream()
                        .filter(a -> (id != null && a != null && a.getId() != null) && id.equals(a.getId()))
                        .findAny()
                        .orElseThrow(() -> new NoAdapterFoundException("Failed to find a valid adapter with id " + (id != null ? id.toString() : "null")));
                s.getBinding().setAdapter(da);

            }
        }
    }

    private ChartPropertiesController buildChartPropertiesController(Chart<Double> chart) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ChartPropertiesView.fxml"));
        ChartPropertiesController propertiesController = new ChartPropertiesController<>(getWorksheet(), chart);
        loader.setController(propertiesController);
        Pane settingsPane = loader.load();
        AnchorPane.setRightAnchor(settingsPane, ChartPropertiesController.SETTINGS_PANE_DISTANCE);
        AnchorPane.setBottomAnchor(settingsPane, 0.0);
        AnchorPane.setTopAnchor(settingsPane, 0.0);
        settingsPane.getStyleClass().add("toolPane");
        settingsPane.setPrefWidth(200);
        settingsPane.setMinWidth(200);
        chartParent.getChildren().add(settingsPane);
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

        try {
            initChartViewPorts();
            initNavigationPane();
            initTableViewPane();
            Platform.runLater(() -> invalidateAll(false, false, false));
            listenerFactory.attachListener(globalPrefs.downSamplingEnabledProperty(), ((observable, oldValue, newValue) -> refresh()));
            listenerFactory.attachListener(globalPrefs.downSamplingThresholdProperty(), ((observable, oldValue, newValue) -> refresh()));

        } catch (Exception e) {
            Platform.runLater(() -> Dialogs.notifyException("Error loading worksheet controller", e));
        }
    }


    private ZonedDateTimeAxis buildTimeAxis() {
        ZonedDateTimeAxis axis = new ZonedDateTimeAxis(getWorksheet().getTimeZone());
        axis.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        axis.setAnimated(false);
        axis.setSide(Side.BOTTOM);
        return axis;
    }

    private void initChartViewPorts() throws IOException {
        ZonedDateTimeAxis defaultXAxis = buildTimeAxis();
        for (Chart<Double> currentChart : getWorksheet().getCharts()) {
            ZonedDateTimeAxis xAxis;
            switch (worksheet.getChartLayout()) {
                case OVERLAID:
                    xAxis = defaultXAxis;
                    break;
                case STACKED:
                    xAxis = buildTimeAxis();
                    break;
                default:
                    xAxis = buildTimeAxis();
                    break;
            }
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

        selectChartLayout.visibleProperty().bind(Bindings.createBooleanBinding(() -> worksheet.getCharts().size() > 1, worksheet.getCharts()));
        selectChartLayout.getItems().setAll(Arrays.stream(ChartLayout.values()).map(chartLayout -> {
            MenuItem item = new MenuItem(chartLayout.toString());
            item.setOnAction(event -> worksheet.setChartLayout(chartLayout));
            return item;
        }).collect(Collectors.toList()));

        switch (worksheet.getChartLayout()) {
            case OVERLAID:
                setupOverlayChartLayout();
                break;
            case STACKED:
                setupStackedChartLayout();
                break;
        }
    }

    private void setupOverlayChartLayout() {
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
    }

    private void setupStackedChartLayout() {
        VBox vBox = new VBox();
        vBox.setAlignment(Pos.TOP_LEFT);
        vBox.prefHeightProperty().bind(chartParent.heightProperty());
        vBox.prefWidthProperty().bind(chartParent.widthProperty());
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
            vBox.getChildren().add(chart);
            chart.maxHeight(Double.MAX_VALUE);
            VBox.setVgrow(chart, Priority.ALWAYS);
            chart.getYAxis().setSide(Side.LEFT);
            chart.getYAxis().setPrefWidth(60.0);
            chart.getYAxis().setMinWidth(60.0);
            chart.getYAxis().setMaxWidth(60.0);
        }
        chartParent.getChildren().add(new ScrollPane(vBox));
        // setup crosshair
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        LinkedHashMap<XYChart<ZonedDateTime, Double>, Function<Double, String>> map = new LinkedHashMap<>();
        map.put(viewPorts.get(0).chart, viewPorts.get(0).getPrefixFormatter()::format);
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
        for (int i = 1; i < viewPorts.size(); i++) {
            LinkedHashMap<XYChart<ZonedDateTime, Double>, Function<Double, String>> m = new LinkedHashMap<>();
            m.put(viewPorts.get(i).chart, viewPorts.get(i).getPrefixFormatter()::format);
            XYChartCrosshair<ZonedDateTime, Double> ch = new XYChartCrosshair<>(m,
                    chartParent,
                    dateTimeFormatter::format);
            ch.onSelectionDone(s -> {
                logger.debug(() -> "Applying zoom selection: " + s.toString());
                currentState.setSelection(convertSelection(s), true);
            });
            ch.horizontalMarkerVisibleProperty().bind(Bindings.createBooleanBinding(() -> globalPrefs.isShiftPressed() || hCrosshair.isSelected(), hCrosshair.selectedProperty(), globalPrefs.shiftPressedProperty()));
            ch.verticalMarkerVisibleProperty().bind(Bindings.createBooleanBinding(() -> globalPrefs.isCtrlPressed() || vCrosshair.isSelected(), vCrosshair.selectedProperty(), globalPrefs.ctrlPressedProperty()));
        }
    }

    private void initNavigationPane() {
        backButton.setOnAction(this::handleHistoryBack);
        forwardButton.setOnAction(this::handleHistoryForward);
        refreshButton.setOnAction(this::handleRefresh);
        snapshotButton.setOnAction(this::handleTakeSnapshot);
        backButton.disableProperty().bind(getWorksheet().getBackwardHistory().emptyProperty());
        forwardButton.disableProperty().bind(getWorksheet().getForwardHistory().emptyProperty());
        addChartButton.setOnAction(this::handleAddNewChart);
        currentState = new ChartViewportsState(getWorksheet().getFromDateTime(), getWorksheet().getToDateTime());
        for (ChartViewPort<Double> viewPort : viewPorts) {
            currentState.get(viewPort.getDataStore()).ifPresent(state -> plotChart(viewPort, state.asSelection(), true));
        }
        endDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.dateTimeValueProperty().bindBidirectional(currentState.startXProperty());
        endDate.dateTimeValueProperty().bindBidirectional(currentState.endXProperty());
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

            ChangeListener<Boolean> refreshListener = (observable, oldValue, newValue) -> {
                if (worksheet.getChartLayout() == ChartLayout.OVERLAID) {
                    invalidateAll(false, false, false);
                }
                else {
                    invalidate(currentViewPort, false, false);
                }
            };

            currentViewPort.getDataStore().getSeries().forEach(doubleTimeSeriesInfo -> {
                listenerFactory.attachListener(doubleTimeSeriesInfo.selectedProperty(), refreshListener);
                listenerFactory.attachListener(doubleTimeSeriesInfo.selectedProperty(), isVisibleListener);
                // Explicitly call the listener to initialize the proper status of the checkbox
                isVisibleListener.invalidated(null);
            });

            visibleColumn.setCellValueFactory(p -> p.getValue().selectedProperty());
            visibleColumn.setCellFactory(CheckBoxTableCell.forTableColumn(visibleColumn));

            showAllCheckBox.setOnAction(event -> {
                boolean b = ((CheckBox) event.getSource()).isSelected();
                currentViewPort.getDataStore().getSeries().forEach(s -> listenerFactory.detachAllChangeListeners(s.selectedProperty()));
                currentViewPort.getDataStore().getSeries().forEach(t -> t.setSelected(b));
                refreshListener.changed(null, null, null);
                currentViewPort.getDataStore().getSeries().forEach(s -> listenerFactory.attachListener(s.selectedProperty(), refreshListener));

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
                this.preventReload = true;
                try {
                    worksheet.getCharts().remove(currentViewPort.dataStore);
                } finally {
                    this.preventReload = false;
                }
                worksheet.getCharts().add(idx - 1, currentViewPort.dataStore);
            });

            Button moveDownButton = (Button) newToolBarButton(Button::new, "Down", "Move the chart down the list.", new String[]{"dialog-button"}, new String[]{"downArrow-icon", "small-icon"});
            moveDownButton.disableProperty().bind(Bindings.createBooleanBinding(() -> seriesTableContainer.getPanes().indexOf(newPane) >= seriesTableContainer.getPanes().size() - 1, seriesTableContainer.getPanes()));
            moveDownButton.visibleProperty().bind(currentViewPort.getDataStore().showPropertiesProperty());
            moveDownButton.setOnAction(event -> {
                int idx = worksheet.getCharts().indexOf(currentViewPort.dataStore);
                this.preventReload = true;
                try {
                    worksheet.getCharts().remove(currentViewPort.dataStore);
                } finally {
                    this.preventReload = false;
                }
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

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            logger.debug(() -> "Closing worksheetController " + this.toString());
            listenerFactory.close();
            currentState.close();
            viewPorts = null;
        }
    }

    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
        ChangeListener<Object> controllerReloadListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                logger.debug("Reloading worksheet controller because property changed from: " + oldValue + " to " + newValue);
                action.accept(this);
            }
        };
        listenerFactory.attachListener(worksheet.chartLayoutProperty(), controllerReloadListener);

        this.worksheet.getCharts().forEach(c -> {
            listenerFactory.attachListener(c.unitPrefixesProperty(), controllerReloadListener);
            listenerFactory.attachListener(c.chartTypeProperty(), controllerReloadListener);
        });


        ListChangeListener<Chart<Double>> chartListListener = c -> {
            while (c.next()) {
                if (c.wasPermutated()) {
                    for (int i = c.getFrom(); i < c.getTo(); ++i) {
                        // nothingfor now
                    }
                }
                else if (c.wasUpdated()) {
                    // nothing for now
                }
                else {
                    if (!preventReload) {
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
                    else {
                        logger.debug(() -> "Reload explicitly prevented on change " + c.toString());
                    }
                }
            }
        };
        listenerFactory.attachListener(worksheet.getCharts(), chartListListener);

    }

    //region *** protected members ***

    protected void addBindings(Collection<TimeSeriesBinding<Double>> bindings, Chart<Double> targetChart) {
        InvalidationListener isVisibleListener = (observable) -> {
            viewPorts.stream().filter(v -> v.getDataStore().equals(targetChart)).findFirst().ifPresent(v -> {
                boolean andAll = true;
                boolean orAll = false;
                for (TimeSeriesInfo<?> t : targetChart.getSeries()) {
                    andAll &= t.isSelected();
                    orAll |= t.isSelected();
                }
                CheckBox showAllCheckBox = (CheckBox) v.getSeriesTable().getColumns().get(0).getGraphic();
                showAllCheckBox.setIndeterminate(Boolean.logicalXor(andAll, orAll));
                showAllCheckBox.setSelected(andAll);
            });
        };

        for (TimeSeriesBinding<Double> b : bindings) {
            TimeSeriesInfo<Double> newSeries = TimeSeriesInfo.fromBinding(b);
            listenerFactory.attachListener(newSeries.selectedProperty(), (observable, oldValue, newValue) -> viewPorts.stream().filter(v -> v.getDataStore().equals(targetChart)).findFirst().ifPresent(v -> invalidate(v, false, false)));
            listenerFactory.attachListener(newSeries.selectedProperty(), isVisibleListener);
            targetChart.addSeries(newSeries);
            // Explicitly call the listener to initialize the proper status of the checkbox
            isVisibleListener.invalidated(null);
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
            invalidate(viewPort, dontPlotChart, forceRefresh);
        }
    }

    private void invalidate(ChartViewPort<Double> viewPort, boolean dontPlot, boolean forceRefresh) {

        try (Profiler p = Profiler.start("Refreshing chart " + getWorksheet().getName() + "\\" + viewPort.getDataStore().getName() + " (dontPlot=" + dontPlot + ")", logger::trace)) {
            //logger.trace(() -> Arrays.stream(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n")));
            currentState.get(viewPort.getDataStore()).ifPresent(y -> {
                XYChartSelection<ZonedDateTime, Double> currentSelection = y.asSelection();
                logger.debug(() -> "currentSelection=" + (currentSelection == null ? "null" : currentSelection.toString()));
                if (!dontPlot) {
                    plotChart(viewPort, currentSelection, forceRefresh);
                }
            });
        }
    }

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
                        if (!closed.get()) {
                            worksheetMaskerPane.setVisible(false);
                            viewPort.getChart().getData().setAll((Collection<? extends XYChart.Series<ZonedDateTime, Double>>) event.getSource().getValue());
                            // Force a redraw of the charts and their Y Axis considering their proper width.
//                            new DelayedAction(() -> viewPort.getChart().resize(0.0, 0.0), Duration.millis(50)).submit();
                        }
                    },
                    event -> {
                        if (!closed.get()) {
                            worksheetMaskerPane.setVisible(false);
                            Dialogs.notifyException("Failed to retrieve data from source", event.getSource().getException(), root);
                        }
                    });
        }
    }

    private void abortIfClosed() {

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
        private Chart<T> dataStore;
        private XYChart<ZonedDateTime, T> chart;
        private ChartPropertiesController<T> propertiesController;
        private PrefixFormatter prefixFormatter;
        private TableView<TimeSeriesInfo<Double>> seriesTable;

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
    private class ChartViewportsState implements AutoCloseable {
        private HashMap<Chart<Double>, AxisState> axisStates = new HashMap<>();
        private final SimpleObjectProperty<ZonedDateTime> startX;
        private final SimpleObjectProperty<ZonedDateTime> endX;
        private final ChangeListener<ZonedDateTime> onRefreshAllRequired = (observable, oldValue, newValue) -> invalidateAll(true, false, false);

        @Override
        public void close() {
            this.startX.removeListener(onRefreshAllRequired);
            this.endX.removeListener(onRefreshAllRequired);
            axisStates.values().forEach(AxisState::close);
        }

        private class AxisState implements AutoCloseable {
            private final SimpleDoubleProperty startY;
            private final SimpleDoubleProperty endY;
            private final ChartViewPort<Double> chartViewPort;
            private final ChangeListener<Number> onRefreshViewportRequired;

            /**
             * Initializes a new instance of the {@link AxisState} class.
             *
             * @param chartViewPort
             * @param startY        the lower bound of the Y axis
             * @param endY          the upper bound of the Y axis
             */
            public AxisState(ChartViewPort<Double> chartViewPort, double startY, double endY) {
                this.chartViewPort = chartViewPort;
                this.onRefreshViewportRequired = (observable, oldValue, newValue) -> invalidate(chartViewPort, true, false);
                this.startY = new SimpleDoubleProperty(roundYValue(startY));
                this.endY = new SimpleDoubleProperty(roundYValue(endY));
                this.addListeners();
            }

            public void removeListeners() {
                this.startY.removeListener(onRefreshViewportRequired);
                this.endY.removeListener(onRefreshViewportRequired);
            }

            public void addListeners() {
                this.startY.addListener(onRefreshViewportRequired);
                this.endY.addListener(onRefreshViewportRequired);
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

            @Override
            public void close() {
                this.removeListeners();
            }
        }

        public void suspendAxisListeners() {
            this.startX.removeListener(onRefreshAllRequired);
            this.endX.removeListener(onRefreshAllRequired);
            axisStates.values().forEach(AxisState::removeListeners);
        }

        public void resumeAxisListeners() {
            this.startX.addListener(onRefreshAllRequired);
            this.endX.addListener(onRefreshAllRequired);
            axisStates.values().forEach(AxisState::addListeners);
        }

        public ChartViewportsState(ZonedDateTime startX, ZonedDateTime endX) {
            this.startX = new SimpleObjectProperty<>(roundDateTime(startX));
            this.endX = new SimpleObjectProperty<>(roundDateTime(endX));
            this.startX.addListener(onRefreshAllRequired);
            this.endX.addListener(onRefreshAllRequired);
            for (ChartViewPort<Double> viewPort : viewPorts) {
                this.put(viewPort.getDataStore(), new AxisState(viewPort, viewPort.getDataStore().getyAxisMinValue(), viewPort.getDataStore().getyAxisMaxValue()));
                this.get(viewPort.getDataStore()).ifPresent(y -> {
                    viewPort.getDataStore().yAxisMinValueProperty().bindBidirectional(y.startY);
                    ((ValueAxis<Double>) viewPort.getChart().getYAxis()).lowerBoundProperty().bindBidirectional(y.startY);
                    viewPort.getDataStore().yAxisMaxValueProperty().bindBidirectional(y.endY);
                    ((ValueAxis<Double>) viewPort.getChart().getYAxis()).upperBoundProperty().bindBidirectional(y.endY);
                });
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
            for (Map.Entry<Chart<Double>, AxisState> e : axisStates.entrySet()) {
                selection.put(e.getKey(), e.getValue().asSelection());
            }
            return selection;
        }

        public void setSelection(Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> selectionMap, boolean toHistory) {
            this.suspendAxisListeners();
            try {
                selectionMap.forEach((chart, xyChartSelection) -> get(chart).ifPresent(y -> y.setSelection(xyChartSelection, toHistory)));
                selectionMap.entrySet().stream().findFirst().ifPresent(entry -> {
                    ZonedDateTime newStartX = roundDateTime(entry.getValue().getStartX());
                    ZonedDateTime newEndX = roundDateTime(entry.getValue().getEndX());
                    boolean dontPlotChart = newStartX.isEqual(startX.get()) && newEndX.isEqual(endX.get());
                    this.startX.set(newStartX);
                    this.endX.set(newEndX);
                    selectionMap.forEach((chart, xyChartSelection) -> get(chart).ifPresent(y -> y.setSelection(xyChartSelection, toHistory)));
                    invalidateAll(toHistory, dontPlotChart, false);
                });
            } finally {
                this.resumeAxisListeners();
            }
        }

        public AxisState put(Chart<Double> chart, AxisState xyChartViewState) {
            return axisStates.put(chart, xyChartViewState);
        }

        public Optional<AxisState> get(Chart<Double> chart) {
            AxisState yState = axisStates.get(chart);
            if (yState != null) {
                return Optional.of(yState);
            }
            logger.debug(() -> "Could not find a saved state for chart " + chart.getName());
            return Optional.empty();
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

    @Override
    protected void finalize() throws Throwable {
        logger.trace(() -> "Finalizing worksheet controller: " + this.toString());
        super.finalize();
    }
}
