/*
 *    Copyright 2016-2019 Frederic Thevenet
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
 */

package eu.binjr.core.controllers;

import eu.binjr.common.io.IOUtils;
import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.common.javafx.charts.*;
import eu.binjr.common.javafx.controls.*;
import eu.binjr.common.logging.Profiler;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.async.AsyncTaskManager;
import eu.binjr.core.data.exceptions.NoAdapterFoundException;
import eu.binjr.core.data.workspace.Chart;
import eu.binjr.core.data.workspace.*;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.GlobalPreferences;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyDoubleProperty;
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
import javafx.scene.shape.Circle;
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
    private static final double Y_AXIS_SEPARATION = 10;
    private static final double TOOL_BUTTON_SIZE = 20;
    private final GlobalPreferences globalPrefs = GlobalPreferences.getInstance();
    private final MainViewController parentController;
    private final ToggleGroup editButtonsGroup = new ToggleGroup();
    private final BindingManager bindingManager = new BindingManager();

    @FXML
    public AnchorPane root;
    protected List<ChartViewPort> viewPorts = new ArrayList<>();
    private Worksheet worksheet;
    private volatile boolean preventReload = false;
    private AtomicBoolean closed = new AtomicBoolean(false);
    @FXML
    private AnchorPane chartParent;
    @FXML
    private AnchorPane chartViewport;
    @FXML
    private TextField yMinRange;
    @FXML
    private TextField yMaxRange;
    @FXML
    private Accordion seriesTableContainer;
    @FXML
    private SplitPane splitPane;
    @FXML
    private Button backButton;
    @FXML
    private Button forwardButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button snapshotButton;
    @FXML
    private Button toggleTableViewButton;
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
    @FXML
    private TimeRangePicker timeRangePicker;
    @FXML
    private AnchorPane chartsLegendsPane;
    @FXML
    private DrawerPane chartProperties;

    private ChartViewportsState currentState;
    private String name;

    public WorksheetController(MainViewController parentController, Worksheet worksheet, Collection<DataAdapter> sourcesAdapters)
            throws NoAdapterFoundException {
        this.parentController = parentController;
        this.worksheet = worksheet;
        // Attach bindings
        for (Chart chart : worksheet.getCharts()) {
            for (TimeSeriesInfo s : chart.getSeries()) {
                UUID id = s.getBinding().getAdapterId();
                DataAdapter da = sourcesAdapters
                        .stream()
                        .filter(a -> (id != null && a != null && a.getId() != null) && id.equals(a.getId()))
                        .findAny()
                        .orElseThrow(() -> new NoAdapterFoundException("Failed to find a valid adapter with id " + (id != null ? id.toString() : "null")));
                s.getBinding().setAdapter(da);
            }
        }
    }

    private ChartPropertiesController buildChartPropertiesController(Chart chart) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/eu/binjr/views/ChartPropertiesView.fxml"));
        ChartPropertiesController propertiesController = new ChartPropertiesController(getWorksheet(), chart);
        loader.setController(propertiesController);
        Pane settingsPane = loader.load();
        chartProperties.getChildren().add(settingsPane);
        AnchorPane.setRightAnchor(settingsPane, 0.0);
        AnchorPane.setBottomAnchor(settingsPane, 0.0);
        AnchorPane.setTopAnchor(settingsPane, 0.0);
        AnchorPane.setLeftAnchor(settingsPane, 0.0);
        settingsPane.getStyleClass().add("toolPane");
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
    public Worksheet getWorksheet() {
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
        assert refreshButton != null : "fx:id\"refreshButton\" was not injected!";
        assert vCrosshair != null : "fx:id\"vCrosshair\" was not injected!";
        assert hCrosshair != null : "fx:id\"hCrosshair\" was not injected!";
        assert snapshotButton != null : "fx:id\"snapshotButton\" was not injected!";

        try {
            initChartViewPorts();
            initNavigationPane();
            initTableViewPane();
            Platform.runLater(() -> invalidateAll(false, false, false));
            bindingManager.attachListener(globalPrefs.downSamplingEnabledProperty(), ((observable, oldValue, newValue) -> refresh()));
            bindingManager.attachListener(globalPrefs.downSamplingThresholdProperty(), ((observable, oldValue, newValue) -> refresh()));
            bindingManager.attachListener(getWorksheet().chartLegendsVisibleProperty(), (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                setChartsLegendvisibilty(newValue);
            });
            setChartsLegendvisibilty(getWorksheet().isChartLegendsVisible());

        } catch (Exception e) {
            Platform.runLater(() -> Dialogs.notifyException("Error loading worksheet controller", e, root));
        }
    }

    private void setChartsLegendvisibilty(Boolean newValue) {
        if (!newValue) {
            splitPane.setDividerPositions(1.0, 0.0);
            toggleTableViewButton.setText("Show Charts Legends");
            chartsLegendsPane.setVisible(false);
            chartsLegendsPane.setMaxHeight(0.0);
        } else {
            chartsLegendsPane.setMaxHeight(Double.MAX_VALUE);
            chartsLegendsPane.setVisible(true);
            toggleTableViewButton.setText("Hide Charts Legends");
            splitPane.setDividerPositions(0.7, 0.3);

        }
    }

    private ZonedDateTimeAxis buildTimeAxis() {
        ZonedDateTimeAxis axis = new ZonedDateTimeAxis(getWorksheet().getTimeZone());
        bindingManager.bind(axis.zoneIdProperty(), getWorksheet().timeZoneProperty());
        axis.setAnimated(false);
        axis.setSide(Side.BOTTOM);
        return axis;
    }

    private void initChartViewPorts() throws IOException {
        ZonedDateTimeAxis defaultXAxis = buildTimeAxis();
        for (Chart currentChart : getWorksheet().getCharts()) {
            ZonedDateTimeAxis xAxis;
            switch (worksheet.getChartLayout()) {
                case OVERLAID:
                    xAxis = defaultXAxis;
                    break;
                case STACKED:
                default:
                    xAxis = buildTimeAxis();
                    break;
            }
            StableTicksAxis yAxis;
            if (currentChart.getUnitPrefixes() == UnitPrefixes.BINARY) {
                yAxis = new BinaryStableTicksAxis();
            } else {
                yAxis = new MetricStableTicksAxis();
            }
            yAxis.autoRangingProperty().bindBidirectional(currentChart.autoScaleYAxisProperty());
            yAxis.setAnimated(false);
            yAxis.setTickSpacing(30);

            bindingManager.bind(yAxis.labelProperty(),
                    Bindings.createStringBinding(
                            () -> String.format("%s - %s", currentChart.getName(), currentChart.getUnit()),
                            currentChart.nameProperty(),
                            currentChart.unitProperty()));
            XYChart<ZonedDateTime, Double> viewPort;
            switch (currentChart.getChartType()) {
                case AREA:
                    viewPort = new AreaChart<>(xAxis, yAxis);
                    ((AreaChart) viewPort).setCreateSymbols(false);
                    break;
                case STACKED:
                    viewPort = new StackedAreaChart<>(xAxis, yAxis);
                    ((StackedAreaChart) viewPort).setCreateSymbols(false);
                    break;
                case SCATTER:
                    viewPort = new ScatterChart<>(xAxis, yAxis);
                    break;
                case LINE:
                default:
                    viewPort = new LineChart<>(xAxis, yAxis);
                    ((LineChart) viewPort).setCreateSymbols(false);
            }
            viewPort.setCache(true);
            viewPort.setCacheHint(CacheHint.SPEED);
            viewPort.setCacheShape(true);
            viewPort.setFocusTraversable(true);
            viewPort.setLegendVisible(false);
            viewPort.setAnimated(false);

            viewPorts.add(new ChartViewPort(currentChart, viewPort, buildChartPropertiesController(currentChart)));


            viewPort.getYAxis().addEventFilter(MouseEvent.MOUSE_CLICKED, bindingManager.registerHandler(event -> {
                for (int i = 0; i < viewPorts.size(); i++) {
                    if (viewPorts.get(i).getChart() == viewPort) {
                        getWorksheet().setSelectedChart(i);
                    }
                }
            }));
        }
        bindingManager.bind(selectChartLayout.disableProperty(),
                Bindings.createBooleanBinding(() -> worksheet.getCharts().size() > 1, worksheet.getCharts()).not());
        selectChartLayout.getItems().setAll(Arrays.stream(ChartLayout.values()).map(chartLayout -> {
            MenuItem item = new MenuItem(chartLayout.toString());
            item.setOnAction(bindingManager.registerHandler(event -> worksheet.setChartLayout(chartLayout)));
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
        if (viewPorts.size() > 1) {
            ChangeListener<Integer> changeListener = (observable, oldValue, newValue) -> {
                ChartViewPort previousChart;
                if (oldValue > -1 && viewPorts.size() > oldValue && (previousChart = viewPorts.get(oldValue)) != null) {
                    ((StableTicksAxis) previousChart.getChart().getYAxis()).setSelected(false);
                }
                ChartViewPort selectedChart;
                if (newValue > -1 && viewPorts.size() > newValue && (selectedChart = viewPorts.get(newValue)) != null) {
                    ((StableTicksAxis) selectedChart.getChart().getYAxis()).setSelected(true);
                }
                TitledPane seriesPane;
                if (newValue > -1 && seriesTableContainer.getPanes().size() > newValue && (seriesPane = seriesTableContainer.getPanes().get(newValue)) != null) {
                    seriesPane.setExpanded(true);
                }
            };
            bindingManager.attachListener(getWorksheet().selectedChartProperty(), changeListener);
            changeListener.changed(getWorksheet().selectedChartProperty(), 0, getWorksheet().getSelectedChart());
        }
    }

    private void setupOverlayChartLayout() {
        for (int i = 0; i < viewPorts.size(); i++) {
            ChartViewPort v = viewPorts.get(i);
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
            hBox.setPickOnBounds(false);
            chart.setPickOnBounds(false);
            chart.getChildrenUnmodifiable()
                    .stream()
                    .filter(node -> node.getStyleClass().contains("chart-content"))
                    .findFirst()
                    .ifPresent(node -> node.setPickOnBounds(false));
            hBox.setAlignment(Pos.CENTER_LEFT);
            bindingManager.bind(hBox.prefHeightProperty(), chartParent.heightProperty());
            bindingManager.bind(hBox.prefWidthProperty(), chartParent.widthProperty());
            bindingManager.bind(chart.minWidthProperty(), chartParent.widthProperty().subtract(n));
            bindingManager.bind(chart.prefWidthProperty(), chartParent.widthProperty().subtract(n));
            bindingManager.bind(chart.maxWidthProperty(), chartParent.widthProperty().subtract(n));
            if (i == 0) {
                chart.getYAxis().setSide(Side.LEFT);
            } else {
                chart.getYAxis().setSide(Side.RIGHT);
                chart.setVerticalZeroLineVisible(false);
                chart.setHorizontalZeroLineVisible(false);
                chart.setVerticalGridLinesVisible(false);
                chart.setHorizontalGridLinesVisible(false);
                bindingManager.bind(chart.translateXProperty(), viewPorts.get(0).getChart().getYAxis().widthProperty());
                bindingManager.bind(chart.getYAxis().translateXProperty(), Bindings.createDoubleBinding(
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
        viewPorts.forEach(v -> map.put(v.getChart(), v.getPrefixFormatter()::format));
        var crossHair = new XYChartCrosshair<>(map, chartParent, dateTimeFormatter::format);
        viewPorts.forEach(v -> v.setCrosshair(crossHair));
        crossHair.onSelectionDone(s -> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());
            currentState.setSelection(convertSelection(s), true);
        });
        hCrosshair.selectedProperty().bindBidirectional(globalPrefs.horizontalMarkerOnProperty());
        vCrosshair.selectedProperty().bindBidirectional(globalPrefs.verticalMarkerOnProperty());
        bindingManager.bind(crossHair.horizontalMarkerVisibleProperty(),
                Bindings.createBooleanBinding(() -> globalPrefs.isShiftPressed() || hCrosshair.isSelected(),
                        hCrosshair.selectedProperty(),
                        globalPrefs.shiftPressedProperty()));
        bindingManager.bind(crossHair.verticalMarkerVisibleProperty(),
                Bindings.createBooleanBinding(() -> globalPrefs.isCtrlPressed() || vCrosshair.isSelected(),
                        vCrosshair.selectedProperty(),
                        globalPrefs.ctrlPressedProperty()));
    }

    private void setupStackedChartLayout() {
        VBox vBox = new VBox();
        vBox.getStyleClass().add("chart-viewport-parent");
        vBox.setAlignment(Pos.TOP_LEFT);
        bindingManager.bind(vBox.prefHeightProperty(), chartParent.heightProperty());
        bindingManager.bind(vBox.prefWidthProperty(), chartParent.widthProperty());
        for (int i = 0; i < viewPorts.size(); i++) {
            ChartViewPort v = viewPorts.get(i);
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
        map.put(viewPorts.get(0).getChart(), viewPorts.get(0).getPrefixFormatter()::format);
        var crossHair = new XYChartCrosshair<>(map, chartParent, dateTimeFormatter::format);
        crossHair.displayFullHeightMarkerProperty().bind(GlobalPreferences.getInstance().fullHeightCrosshairMarkerProperty());
        viewPorts.get(0).setCrosshair(crossHair);
        crossHair.onSelectionDone(s -> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());
            currentState.setSelection(convertSelection(s), true);
        });
        bindingManager.bindBidirectional(hCrosshair.selectedProperty(), globalPrefs.horizontalMarkerOnProperty());
        bindingManager.bindBidirectional(vCrosshair.selectedProperty(), globalPrefs.verticalMarkerOnProperty());
        bindingManager.bind(crossHair.horizontalMarkerVisibleProperty(),
                Bindings.createBooleanBinding(() ->
                                globalPrefs.isShiftPressed() || hCrosshair.isSelected(),
                        hCrosshair.selectedProperty(),
                        globalPrefs.shiftPressedProperty()));
        bindingManager.bind(crossHair.verticalMarkerVisibleProperty(),
                Bindings.createBooleanBinding(() ->
                                globalPrefs.isCtrlPressed() || vCrosshair.isSelected(),
                        vCrosshair.selectedProperty(),
                        globalPrefs.ctrlPressedProperty()));
        for (int i = 1; i < viewPorts.size(); i++) {
            LinkedHashMap<XYChart<ZonedDateTime, Double>, Function<Double, String>> m = new LinkedHashMap<>();
            m.put(viewPorts.get(i).getChart(), viewPorts.get(i).getPrefixFormatter()::format);
            XYChartCrosshair<ZonedDateTime, Double> ch = new XYChartCrosshair<>(m, chartParent, dateTimeFormatter::format);
            ch.displayFullHeightMarkerProperty().bind(GlobalPreferences.getInstance().fullHeightCrosshairMarkerProperty());
            ch.onSelectionDone(s -> {
                logger.debug(() -> "Applying zoom selection: " + s.toString());
                currentState.setSelection(convertSelection(s), true);
            });
            bindingManager.bind(ch.horizontalMarkerVisibleProperty(),
                    Bindings.createBooleanBinding(() ->
                                    globalPrefs.isShiftPressed() || hCrosshair.isSelected(),
                            hCrosshair.selectedProperty(),
                            globalPrefs.shiftPressedProperty()));
            bindingManager.bind(ch.verticalMarkerVisibleProperty(),
                    Bindings.createBooleanBinding(() ->
                                    globalPrefs.isCtrlPressed() || vCrosshair.isSelected(),
                            vCrosshair.selectedProperty(),
                            globalPrefs.ctrlPressedProperty()));
            viewPorts.get(i).setCrosshair(ch);
        }
    }

    Property<TimeRangePicker.TimeRange> selectedRangeProperty() {
        return this.timeRangePicker.selectedRangeProperty();
    }

    private void initNavigationPane() {
        backButton.setOnAction(bindingManager.registerHandler(this::handleHistoryBack));
        forwardButton.setOnAction(bindingManager.registerHandler(this::handleHistoryForward));
        refreshButton.setOnAction(bindingManager.registerHandler(this::handleRefresh));
        snapshotButton.setOnAction(bindingManager.registerHandler(this::handleTakeSnapshot));
        toggleTableViewButton.setOnAction(bindingManager.registerHandler(this::handleToggleTableViewButton));
        bindingManager.bind(backButton.disableProperty(), getWorksheet().getBackwardHistory().emptyProperty());
        bindingManager.bind(forwardButton.disableProperty(), getWorksheet().getForwardHistory().emptyProperty());
        addChartButton.setOnAction(bindingManager.registerHandler(this::handleAddNewChart));
        currentState = new ChartViewportsState(this, getWorksheet().getFromDateTime(), getWorksheet().getToDateTime());
        for (ChartViewPort viewPort : viewPorts) {
            currentState.get(viewPort.getDataStore()).ifPresent(state -> plotChart(viewPort, state.asSelection(), true));
        }
        timeRangePicker.timeRangeLinkedProperty().bindBidirectional(getWorksheet().timeRangeLinkedProperty());
        timeRangePicker.zoneIdProperty().bindBidirectional(getWorksheet().timeZoneProperty());
        timeRangePicker.initSelectedRange(TimeRangePicker.TimeRange.of(currentState.getStartX(), currentState.getEndX()));
        timeRangePicker.setOnSelectedRangeChanged((observable, oldValue, newValue) -> {
            currentState.setSelection(currentState.selectTimeRange(newValue.getBeginning(), newValue.getEnd()), true);
        });

        currentState.timeRangeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                timeRangePicker.updateSelectedRange(newValue);
            }
        });
    }

    private Map<Chart, XYChartSelection<ZonedDateTime, Double>> convertSelection(Map<XYChart<ZonedDateTime, Double>, XYChartSelection<ZonedDateTime, Double>> selection) {
        Map<Chart, XYChartSelection<ZonedDateTime, Double>> result = new HashMap<>();
        selection.forEach((xyChart, xyChartSelection) -> {
            viewPorts.stream()
                    .filter(v -> v.getChart().equals(xyChart))
                    .findFirst()
                    .ifPresent(viewPort -> result.put(viewPort.getDataStore(), xyChartSelection));
        });
        return result;
    }

    private void handleAddNewChart(ActionEvent actionEvent) {
        worksheet.getCharts().add(new Chart());
    }

    private void initTableViewPane() {
        for (ChartViewPort currentViewPort : viewPorts) {
            currentViewPort.getSeriesTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            CheckBox showAllCheckBox = new CheckBox();
            TableColumn<TimeSeriesInfo, Boolean> visibleColumn = new TableColumn<>();
            visibleColumn.setGraphic(showAllCheckBox);
            visibleColumn.setSortable(false);
            visibleColumn.setResizable(false);
            visibleColumn.setPrefWidth(32);

            InvalidationListener isVisibleListener = (observable) -> {
                boolean andAll = true;
                boolean orAll = false;
                for (TimeSeriesInfo t : currentViewPort.getDataStore().getSeries()) {
                    andAll &= t.isSelected();
                    orAll |= t.isSelected();
                }
                showAllCheckBox.setIndeterminate(Boolean.logicalXor(andAll, orAll));
                showAllCheckBox.setSelected(andAll);
            };

            ChangeListener<Boolean> refreshListener = (observable, oldValue, newValue) -> {
                if (worksheet.getChartLayout() == ChartLayout.OVERLAID) {
                    invalidateAll(false, false, false);
                } else {
                    invalidate(currentViewPort, false, false);
                }
            };

            currentViewPort.getDataStore().getSeries().forEach(doubleTimeSeriesInfo -> {
                bindingManager.attachListener(doubleTimeSeriesInfo.selectedProperty(), refreshListener);
                bindingManager.attachListener(doubleTimeSeriesInfo.selectedProperty(), isVisibleListener);
                // Explicitly call the listener to initialize the proper status of the checkbox
                isVisibleListener.invalidated(null);
            });

            visibleColumn.setCellValueFactory(p -> p.getValue().selectedProperty());
            visibleColumn.setCellFactory(CheckBoxTableCell.forTableColumn(visibleColumn));

            showAllCheckBox.setOnAction(bindingManager.registerHandler(event -> {
                ChangeListener<Boolean> r = (observable, oldValue, newValue) -> {
                    if (worksheet.getChartLayout() == ChartLayout.OVERLAID) {
                        invalidateAll(false, false, false);
                    } else {
                        invalidate(currentViewPort, false, false);
                    }
                };
                boolean b = ((CheckBox) event.getSource()).isSelected();
                currentViewPort.getDataStore().getSeries().forEach(s -> bindingManager.detachAllChangeListeners(s.selectedProperty()));
                currentViewPort.getDataStore().getSeries().forEach(t -> t.setSelected(b));
                r.changed(null, null, null);
                currentViewPort.getDataStore().getSeries().forEach(s -> bindingManager.attachListener(s.selectedProperty(), r));

            }));

            DecimalFormatTableCellFactory<TimeSeriesInfo, String> alignRightCellFactory = new DecimalFormatTableCellFactory<>();
            alignRightCellFactory.setAlignment(TextAlignment.RIGHT);

            TableColumn<TimeSeriesInfo, Color> colorColumn = new TableColumn<>();
            colorColumn.setSortable(false);
            colorColumn.setResizable(false);
            colorColumn.setPrefWidth(32);

            TableColumn<TimeSeriesInfo, Boolean> nameColumn = new TableColumn<>("Name");
            nameColumn.setSortable(false);
            nameColumn.setPrefWidth(160);
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));

            TableColumn<TimeSeriesInfo, String> minColumn = new TableColumn<>("Min.");
            minColumn.setSortable(false);
            minColumn.setPrefWidth(75);
            minColumn.setCellFactory(alignRightCellFactory);

            TableColumn<TimeSeriesInfo, String> maxColumn = new TableColumn<>("Max.");
            maxColumn.setSortable(false);
            maxColumn.setPrefWidth(75);
            maxColumn.setCellFactory(alignRightCellFactory);

            TableColumn<TimeSeriesInfo, String> avgColumn = new TableColumn<>("Avg.");
            avgColumn.setSortable(false);
            avgColumn.setPrefWidth(75);
            avgColumn.setCellFactory(alignRightCellFactory);

            TableColumn<TimeSeriesInfo, String> currentColumn = new TableColumn<>("Current");
            currentColumn.setSortable(false);
            currentColumn.setPrefWidth(75);
            currentColumn.setCellFactory(alignRightCellFactory);
            currentColumn.getStyleClass().add("column-bold-text");

            TableColumn<TimeSeriesInfo, String> pathColumn = new TableColumn<>("Path");
            pathColumn.setSortable(false);
            pathColumn.setPrefWidth(400);


            currentColumn.setVisible(getSelectedViewPort().getCrosshair().isVerticalMarkerVisible());
            bindingManager.attachListener(getSelectedViewPort().getCrosshair().verticalMarkerVisibleProperty(),
                    (ChangeListener<Boolean>) (observable, oldValue, newValue) -> currentColumn.setVisible(newValue));

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
                        return currentViewPort.getPrefixFormatter().format(p.getValue()
                                .getProcessor()
                                .tryGetNearestValue(getSelectedViewPort().getCrosshair().getCurrentXValue())
                                .orElse(Double.NaN));
                    }, getSelectedViewPort().getCrosshair().currentXValueProperty()));

            currentViewPort.getSeriesTable().setRowFactory(this::seriesTableRowFactory);
            currentViewPort.getSeriesTable().setOnKeyReleased(bindingManager.registerHandler(event -> {
                if (event.getCode().equals(KeyCode.DELETE)) {
                    removeSelectedBinding(currentViewPort.getSeriesTable());
                }
            }));

            currentViewPort.getSeriesTable().setItems(currentViewPort.getDataStore().getSeries());
            currentViewPort.getSeriesTable().getColumns().addAll(visibleColumn, colorColumn, nameColumn, minColumn, maxColumn, avgColumn, currentColumn, pathColumn);
            TitledPane newPane = new TitledPane(currentViewPort.getDataStore().getName(), currentViewPort.getSeriesTable());

            newPane.setOnDragOver(bindingManager.registerHandler(this::handleDragOverWorksheetView));
            newPane.setOnDragDropped(bindingManager.registerHandler(this::handleDragDroppedOnWorksheetView));
            newPane.setUserData(currentViewPort);

            GridPane titleRegion = new GridPane();
            titleRegion.setHgap(5);
            titleRegion.getColumnConstraints().add(new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.LEFT, true));
            titleRegion.getColumnConstraints().add(new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.NEVER, HPos.RIGHT, false));
            bindingManager.bind(titleRegion.minWidthProperty(), newPane.widthProperty().subtract(30));
            bindingManager.bind(titleRegion.maxWidthProperty(), newPane.widthProperty().subtract(30));

            Label label = new Label();
            bindingManager.bind(label.textProperty(), currentViewPort.getDataStore().nameProperty());
            bindingManager.bind(label.visibleProperty(), currentViewPort.getDataStore().showPropertiesProperty().not());
            HBox editFieldsGroup = new HBox();
            DoubleBinding db = Bindings.createDoubleBinding(() -> editFieldsGroup.isVisible() ? USE_COMPUTED_SIZE : 0.0, editFieldsGroup.visibleProperty());
            bindingManager.bind(editFieldsGroup.prefHeightProperty(), db);
            bindingManager.bind(editFieldsGroup.maxHeightProperty(), db);
            bindingManager.bind(editFieldsGroup.minHeightProperty(), db);
            bindingManager.bind(editFieldsGroup.visibleProperty(), currentViewPort.getDataStore().showPropertiesProperty());
            editFieldsGroup.setSpacing(5);
            TextField chartNameField = new TextField();
            chartNameField.textProperty().bindBidirectional(currentViewPort.getDataStore().nameProperty());
            TextField unitNameField = new TextField();
            unitNameField.textProperty().bindBidirectional(currentViewPort.getDataStore().unitProperty());
            ChoiceBox<UnitPrefixes> unitPrefixChoiceBox = new ChoiceBox<>();
            unitPrefixChoiceBox.getItems().setAll(UnitPrefixes.values());
            unitPrefixChoiceBox.getSelectionModel().select(currentViewPort.getDataStore().getUnitPrefixes());
            bindingManager.bind(currentViewPort.getDataStore().unitPrefixesProperty(), unitPrefixChoiceBox.getSelectionModel().selectedItemProperty());
            HBox.setHgrow(chartNameField, Priority.ALWAYS);
            titleRegion.setOnMouseClicked(bindingManager.registerHandler(event -> {
                if (event.getClickCount() == 2) {
                    chartNameField.selectAll();
                    chartNameField.requestFocus();
                    currentViewPort.getDataStore().setShowProperties(true);
                }
            }));
            editFieldsGroup.getChildren().addAll(chartNameField, unitNameField, unitPrefixChoiceBox);

            // *** Toolbar ***
            HBox toolbar = new HBox();
            toolbar.getStyleClass().add("title-pane-tool-bar");
            toolbar.setAlignment(Pos.CENTER);
            Button closeButton = (Button) newToolBarButton(Button::new, "Close", "Remove this chart from the worksheet.", new String[]{"exit"}, new String[]{"cross-icon", "small-icon"});
            closeButton.setOnAction(bindingManager.registerHandler(event -> {
                if (Dialogs.confirmDialog(root, "Are you sure you want to remove chart \"" + currentViewPort.getDataStore().getName() + "\"?",
                        "", ButtonType.YES, ButtonType.NO) == ButtonType.YES) {
                    worksheet.getCharts().remove(currentViewPort.getDataStore());
                }
            }));
            bindingManager.bind(closeButton.disableProperty(), Bindings.createBooleanBinding(() -> worksheet.getCharts().size() > 1, worksheet.getCharts()).not());

            ToggleButton editButton = (ToggleButton) newToolBarButton(ToggleButton::new, "Settings", "Edit the chart's settings", new String[]{"dialog-button"}, new String[]{"settings-icon", "small-icon"});
            editButton.selectedProperty().bindBidirectional(currentViewPort.getDataStore().showPropertiesProperty());
            editButton.setOnAction(bindingManager.registerHandler(event -> newPane.setExpanded(true)));

            editButtonsGroup.getToggles().add(editButton);

            Button moveUpButton = (Button) newToolBarButton(Button::new, "Up", "Move the chart up the list.", new String[]{"dialog-button"}, new String[]{"upArrow-icon", "small-icon"});
            bindingManager.bind(moveUpButton.disableProperty(),
                    Bindings.createBooleanBinding(() -> seriesTableContainer.getPanes().indexOf(newPane) == 0, seriesTableContainer.getPanes()));
            bindingManager.bind(moveUpButton.visibleProperty(), currentViewPort.getDataStore().showPropertiesProperty());
            moveUpButton.setOnAction(bindingManager.registerHandler(event -> {
                int idx = worksheet.getCharts().indexOf(currentViewPort.getDataStore());
                this.preventReload = true;
                try {
                    worksheet.getCharts().remove(currentViewPort.getDataStore());
                } finally {
                    this.preventReload = false;
                }
                worksheet.getCharts().add(idx - 1, currentViewPort.getDataStore());
            }));

            Button moveDownButton = (Button) newToolBarButton(Button::new, "Down", "Move the chart down the list.", new String[]{"dialog-button"}, new String[]{"downArrow-icon", "small-icon"});
            bindingManager.bind(moveDownButton.disableProperty(), Bindings.createBooleanBinding(() -> seriesTableContainer.getPanes().indexOf(newPane) >= seriesTableContainer.getPanes().size() - 1, seriesTableContainer.getPanes()));
            bindingManager.bind(moveDownButton.visibleProperty(), currentViewPort.getDataStore().showPropertiesProperty());
            moveDownButton.setOnAction(bindingManager.registerHandler(event -> {
                int idx = worksheet.getCharts().indexOf(currentViewPort.getDataStore());
                this.preventReload = true;
                try {
                    worksheet.getCharts().remove(currentViewPort.getDataStore());
                } finally {
                    this.preventReload = false;
                }
                worksheet.getCharts().add(idx + 1, currentViewPort.getDataStore());
            }));

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
        bindingManager.attachListener(editButtonsGroup.selectedToggleProperty(), (ChangeListener<Toggle>) (observable, oldValue, newValue) -> {
            if(newValue != null){
                chartProperties.expand();
            }else{
                chartProperties.collapse();
            }
        });
        chartProperties.setSibling(chartParent);
        if (editButtonsGroup.getSelectedToggle()!=null){
            chartProperties.expand();
        }

        bindingManager.attachListener(seriesTableContainer.expandedPaneProperty(),
                (ObservableValue<? extends TitledPane> observable, TitledPane oldPane, TitledPane newPane) -> {
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

    Optional<ChartViewPort> getAttachedViewport(TitledPane pane) {
        if (pane != null && (pane.getUserData() instanceof ChartViewPort)) {
            return Optional.of((ChartViewPort) pane.getUserData());
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
            TreeView<TimeSeriesBinding> treeView = parentController.getSelectedTreeView();
            if (treeView != null) {
                TreeItem<TimeSeriesBinding> item = treeView.getSelectionModel().getSelectedItem();
                if (item != null) {
                    Stage targetStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    if (targetStage != null) {
                        targetStage.requestFocus();
                    }
                    if (TransferMode.MOVE.equals(event.getAcceptedTransferMode())) {
                        try {
                            TitledPane droppedPane = (TitledPane) event.getSource();
                            droppedPane.setExpanded(true);
                            ChartViewPort viewPort = (ChartViewPort) droppedPane.getUserData();
                            List<TimeSeriesBinding> bindings = new ArrayList<>();
                            parentController.getAllBindingsFromBranch(item, bindings);
                            addBindings(bindings, viewPort.getDataStore());
                        } catch (Exception e) {
                            Dialogs.notifyException("Error adding bindings to existing worksheet", e, root);
                        }
                        logger.debug("dropped to " + event.toString());
                    } else {
                        logger.warn("Unsupported drag and drop transfer mode: " + event.getAcceptedTransferMode());
                    }
                } else {
                    logger.warn("Cannot complete drag and drop operation: selected TreeItem is null");
                }
            } else {
                logger.warn("Cannot complete drag and drop operation: selected TreeView is null");
            }
            event.consume();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            logger.debug(() -> "Closing worksheetController " + this.toString());
            bindingManager.close();
            currentState.close();
            hCrosshair.selectedProperty().unbindBidirectional(globalPrefs.horizontalMarkerOnProperty());
            vCrosshair.selectedProperty().unbindBidirectional(globalPrefs.verticalMarkerOnProperty());
            currentState = null;
            this.seriesTableContainer.getPanes().forEach(pane -> {
                pane.setUserData(null);
                pane.setContent(null);
            });
            this.seriesTableContainer.getPanes().clear();
            //Workaround JDK-8220012
            this.seriesTableContainer.getPanes().add(new TitledPane());
            IOUtils.closeCollectionElements(viewPorts);
            viewPorts = null;
            timeRangePicker.dispose();
            this.worksheet = null;
        }
    }

    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
        ChangeListener<Object> controllerReloadListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                logger.debug(() -> "Reloading worksheet controller because property changed from: " + oldValue + " to " + newValue);
                action.accept(this);
                this.close();
            }
        };
        bindingManager.attachListener(worksheet.chartLayoutProperty(), controllerReloadListener);

        this.worksheet.getCharts().forEach(c -> {
            bindingManager.attachListener(c.unitPrefixesProperty(), controllerReloadListener);
            bindingManager.attachListener(c.chartTypeProperty(), controllerReloadListener);
        });

        ListChangeListener<Chart> chartListListener = c -> {
            while (c.next()) {
                if (c.wasPermutated()) {
                    for (int i = c.getFrom(); i < c.getTo(); ++i) {
                        // nothing for now
                    }
                } else if (c.wasUpdated()) {
                    // nothing for now
                } else {
                    if (!preventReload) {
                        if (c.wasAdded()) {
                            List<? extends Chart> added = c.getAddedSubList();
                            Chart chart = added.get(added.size() - 1);
                            int chartIndex = worksheet.getCharts().indexOf(chart);
                            worksheet.setSelectedChart(chartIndex);
                            chart.setShowProperties(true);
                        }
                        if (c.wasRemoved()) {
                            if (worksheet.getSelectedChart() == c.getFrom()) {
                                worksheet.setSelectedChart(Math.max(0, c.getFrom() - 1));
                            } else if (worksheet.getSelectedChart() > c.getFrom()) {
                                worksheet.setSelectedChart(Math.max(0, worksheet.getSelectedChart() - 1));
                            }
                        }
                        logger.debug(() -> "Reloading worksheet controller because list changed: " + c.toString() + " in controller " + this.toString());
                        action.accept(this);
                    } else {
                        logger.debug(() -> "Reload explicitly prevented on change " + c.toString());
                    }
                }
            }
        };
        bindingManager.attachListener(worksheet.getCharts(), chartListListener);
    }

    //region *** protected members ***
    protected void addBindings(Collection<TimeSeriesBinding> bindings, Chart targetChart) {
        if (bindings.size() >= GlobalPreferences.getInstance().getMaxSeriesPerChartBeforeWarning()){
            if (Dialogs.confirmDialog(root,
                    "This action will add " +bindings.size() +" series on a single chart." ,
                    "Are you sure you want to proceed?",
                    ButtonType.YES,ButtonType.NO) != ButtonType.YES){
                return;
            }
        }
        InvalidationListener isVisibleListener = (observable) -> {
            viewPorts.stream().filter(v -> v.getDataStore().equals(targetChart)).findFirst().ifPresent(v -> {
                boolean andAll = true;
                boolean orAll = false;
                for (TimeSeriesInfo t : targetChart.getSeries()) {
                    andAll &= t.isSelected();
                    orAll |= t.isSelected();
                }
                CheckBox showAllCheckBox = (CheckBox) v.getSeriesTable().getColumns().get(0).getGraphic();
                showAllCheckBox.setIndeterminate(Boolean.logicalXor(andAll, orAll));
                showAllCheckBox.setSelected(andAll);
            });
        };
        for (TimeSeriesBinding b : bindings) {
            TimeSeriesInfo newSeries = TimeSeriesInfo.fromBinding(b);
            bindingManager.attachListener(newSeries.selectedProperty(), (observable, oldValue, newValue) -> viewPorts.stream().filter(v -> v.getDataStore().equals(targetChart)).findFirst().ifPresent(v -> invalidate(v, false, false)));
            bindingManager.attachListener(newSeries.selectedProperty(), isVisibleListener);
            targetChart.addSeries(newSeries);
            // Explicitly call the listener to initialize the proper status of the checkbox
            isVisibleListener.invalidated(null);
        }
        invalidateAll(false, false, false);
    }

    protected void removeSelectedBinding(TableView<TimeSeriesInfo> seriesTable) {
        List<TimeSeriesInfo> selected = new ArrayList<>(seriesTable.getSelectionModel().getSelectedItems());
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
    protected void handleTakeSnapshot(ActionEvent actionEvent) {
        saveSnapshot();
    }

    @FXML
    protected void handleToggleTableViewButton(ActionEvent actionEvent) {
        getWorksheet().setChartLegendsVisible(!getWorksheet().isChartLegendsVisible());
    }
    //endregion

    //region [Private Members]
    void invalidateAll(boolean saveToHistory, boolean dontPlotChart, boolean forceRefresh) {
        if (saveToHistory) {
            getWorksheet().getBackwardHistory().push(getWorksheet().getPreviousState());
            getWorksheet().getForwardHistory().clear();
        }
        getWorksheet().setPreviousState(currentState.asSelection());
        logger.debug(() -> getWorksheet().getBackwardHistory().dump());
        for (ChartViewPort viewPort : viewPorts) {
            invalidate(viewPort, dontPlotChart, forceRefresh);
        }
    }

    void invalidate(ChartViewPort viewPort, boolean dontPlot, boolean forceRefresh) {
        try (Profiler p = Profiler.start("Refreshing chart " + getWorksheet().getName() + "\\" + viewPort.getDataStore().getName() + " (dontPlot=" + dontPlot + ")", logger::trace)) {
            currentState.get(viewPort.getDataStore()).ifPresent(y -> {
                XYChartSelection<ZonedDateTime, Double> currentSelection = y.asSelection();
                logger.debug(() -> "currentSelection=" + (currentSelection == null ? "null" : currentSelection.toString()));
                if (!dontPlot) {
                    plotChart(viewPort, currentSelection, forceRefresh);
                }
            });
        }
    }

    private void plotChart(ChartViewPort viewPort, XYChartSelection<ZonedDateTime, Double> currentSelection, boolean forceRefresh) {
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
                            if (getWorksheet().getChartLayout() == ChartLayout.OVERLAID) {
                                // Force a redraw of the charts and their Y Axis considering their proper width.
                                new DelayedAction(() -> viewPort.getChart().resize(0.0, 0.0), Duration.millis(50)).submit();
                            }
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

    private XYChart.Series<ZonedDateTime, Double> makeXYChartSeries(Chart currentChart, TimeSeriesInfo series) {
        try (Profiler p = Profiler.start("Building  XYChart.Series data for" + series.getDisplayName(), logger::trace)) {
            XYChart.Series<ZonedDateTime, Double> newSeries = new XYChart.Series<>();
            newSeries.getData().setAll(series.getProcessor().getData());
            if (currentChart.getChartType() == ChartType.SCATTER) {
                for (var data : newSeries.getData()) {
                    var c = new Circle();
                    bindingManager.bind(c.radiusProperty(), currentChart.strokeWidthProperty());
                    bindingManager.bind(c.fillProperty(), series.displayColorProperty());
                    data.setNode(c);
                }
            } else {
                bindingManager.attachListener(newSeries.nodeProperty(), (ChangeListener<Node>) (node, oldNode, newNode) -> {
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
            }
            return newSeries;
        }
    }

    private void saveSnapshot() {
        WritableImage snapImg = root.snapshot(null, null);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save SnapShot");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        fileChooser.setInitialDirectory(globalPrefs.getMostRecentSaveFolder().toFile());
        fileChooser.setInitialFileName(String.format("binjr_snapshot_%s.png", getWorksheet().getName()));
        File selectedFile = fileChooser.showSaveDialog(Dialogs.getStage(root));
        if (selectedFile != null) {
            try {
                if (selectedFile.getParent() != null) {
                    globalPrefs.setMostRecentSaveFolder(selectedFile.toPath());
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
        } else {
            logger.debug(() -> "History is empty: nothing to go back to.");
        }
    }

    private ChartViewPort getSelectedViewPort() {
        var v = viewPorts.get(getWorksheet().getSelectedChart());
        if (v != null) {
            return v;
        }
        throw new IllegalStateException("Could not retreive selected viewport on current worksheet");
    }

    private TableRow<TimeSeriesInfo> seriesTableRowFactory(TableView<TimeSeriesInfo> tv) {
        TableRow<TimeSeriesInfo> row = new TableRow<>();
        row.setOnDragDetected(bindingManager.registerHandler(event -> {
            if (!row.isEmpty()) {
                Integer index = row.getIndex();
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                db.setDragView(row.snapshot(null, null));
                ClipboardContent cc = new ClipboardContent();
                cc.put(SERIALIZED_MIME_TYPE, index);
                db.setContent(cc);
                event.consume();
            }
        }));

        row.setOnDragOver(bindingManager.registerHandler(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE) && row.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        }));

        row.setOnDragDropped(bindingManager.registerHandler(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                TimeSeriesInfo draggedseries = tv.getItems().remove(draggedIndex);
                int dropIndex;
                if (row.isEmpty()) {
                    dropIndex = tv.getItems().size();
                } else {
                    dropIndex = row.getIndex();
                }
                tv.getItems().add(dropIndex, draggedseries);
                event.setDropCompleted(true);
                tv.getSelectionModel().clearAndSelect(dropIndex);
                invalidateAll(false, false, false);
                event.consume();
            }
        }));
        return row;
    }

    public void toggleShowPropertiesPane() {
        ChartViewPort currentViewport = viewPorts.get(worksheet.getSelectedChart());
        if (currentViewport != null) {
            currentViewport.getDataStore().setShowProperties((editButtonsGroup.getSelectedToggle() == null));
        }
    }

    public void setShowPropertiesPane(boolean value) {
        ChartViewPort currentViewport = viewPorts.get(worksheet.getSelectedChart());
        if (currentViewport != null) {
            currentViewport.getDataStore().setShowProperties(value);
        }
    }

    public void toggleChartLegendVisibility() {
        ChartViewPort currentViewport = viewPorts.get(worksheet.getSelectedChart());
        if (currentViewport != null) {
            currentViewport.getDataStore().setShowProperties((editButtonsGroup.getSelectedToggle() == null));
        }
    }

    BindingManager getBindingManager() {
        return bindingManager;
    }

    //endregion
}
