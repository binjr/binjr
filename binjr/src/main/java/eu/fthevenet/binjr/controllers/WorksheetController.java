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
import eu.fthevenet.binjr.data.workspace.*;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.util.javafx.charts.*;
import eu.fthevenet.util.javafx.controls.ColorTableCell;
import eu.fthevenet.util.javafx.controls.ZonedDateTimePicker;
import eu.fthevenet.util.logging.Profiler;
import eu.fthevenet.util.text.BinaryPrefixFormatter;
import eu.fthevenet.util.text.MetricPrefixFormatter;
import eu.fthevenet.util.text.PrefixFormatter;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.stage.FileChooser;
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

/**
 * The controller class for the time series view.
 *
 * @author Frederic Thevenet
 */
public abstract class WorksheetController implements Initializable, AutoCloseable {
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    private static final Logger logger = LogManager.getLogger(WorksheetController.class);
    private final GlobalPreferences globalPrefs = GlobalPreferences.getInstance();
    private final Worksheet<Double> worksheet;
    private final PrefixFormatter prefixFormatter;

    @FXML
    public AnchorPane root;
    @FXML
    public AnchorPane chartParent;
    @FXML
    protected XYChart<ZonedDateTime, Double> chart;
    @FXML
    private TextField yMinRange;
    @FXML
    private TextField yMaxRange;
    @FXML
    private TableView<TimeSeriesInfo<Double>> seriesTable;
    @FXML
    private Button backButton;
    @FXML
    private Button forwardButton;
    @FXML
    private Button refreshButton;
    //    @FXML
//    private ToggleButton autoScaleYAxisToggle;
    @FXML
    private Button snapshotButton;
    @FXML
    private ZonedDateTimePicker startDate;
    @FXML
    private ZonedDateTimePicker endDate;
    @FXML
    private TableColumn<TimeSeriesInfo<Double>, String> pathColumn;
    @FXML
    private TableColumn<TimeSeriesInfo<Double>, Color> colorColumn;
    @FXML
    private TableColumn<TimeSeriesInfo<Double>, Boolean> visibleColumn;
    @FXML
    private TableColumn<TimeSeriesInfo<Double>, String> minColumn;
    @FXML
    private TableColumn<TimeSeriesInfo<Double>, String> maxColumn;
    @FXML
    private TableColumn<TimeSeriesInfo<Double>, String> avgColumn;
    @FXML
    private TableColumn<TimeSeriesInfo<Double>, String> currentColumn;
    @FXML
    private ToggleButton vCrosshair;
    @FXML
    private ToggleButton hCrosshair;
    @FXML
    private MaskerPane worksheetMaskerPane;
    @FXML
    private ContextMenu seriesListMenu;

    private StackPane settingsPane;
    private ChartPropertiesController propertiesController;
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

    public WorksheetController(Worksheet<Double> worksheet) throws IOException {
        this.worksheet = worksheet;
        switch (this.worksheet.getDefaultChart().getUnitPrefixes()) {
            case BINARY:
                this.prefixFormatter = new BinaryPrefixFormatter();
                break;
            case METRIC:
                this.prefixFormatter = new MetricPrefixFormatter();
                break;

            default:
                throw new IllegalArgumentException("Unknown unit prefix");
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ChartPropertiesView.fxml"));
        propertiesController = new ChartPropertiesController<>(getWorksheet().getDefaultChart());
        loader.setController(propertiesController);
        Parent p = loader.load();
        settingsPane = new StackPane(p);
        AnchorPane.setRightAnchor(settingsPane, ChartPropertiesController.SETTINGS_PANE_DISTANCE);
        AnchorPane.setBottomAnchor(settingsPane, 0.0);
        AnchorPane.setTopAnchor(settingsPane, 0.0);
        settingsPane.getStyleClass().add("toolPane");
        settingsPane.setPrefWidth(200);
        settingsPane.setMinWidth(200);
    }

    //region [Properties]
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public XYChartCrosshair<ZonedDateTime, Double> getCrossHair() {
        return crossHair;
    }

    public XYChart<ZonedDateTime, Double> getChart() {
        return chart;
    }

    public abstract ChartType getChartType();

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
//        assert yMinRange != null : "fx:id\"yMinRange\" was not injected!";
//        assert yMaxRange != null : "fx:id\"yMaxRange\" was not injected!";
        assert seriesTable != null : "fx:id\"seriesTable\" was not injected!";
        assert backButton != null : "fx:id\"backButton\" was not injected!";
        assert forwardButton != null : "fx:id\"forwardButton\" was not injected!";
        //    assert autoScaleYAxisToggle != null : "fx:id\"autoScaleYAxisToggle\" was not injected!";
        assert startDate != null : "fx:id\"beginDateTime\" was not injected!";
        assert endDate != null : "fx:id\"endDateTime\" was not injected!";
        assert pathColumn != null : "fx:id\"pathColumn\" was not injected!";
        assert colorColumn != null : "fx:id\"colorColumn\" was not injected!";
        assert refreshButton != null : "fx:id\"refreshButton\" was not injected!";
        assert vCrosshair != null : "fx:id\"vCrosshair\" was not injected!";
        assert hCrosshair != null : "fx:id\"hCrosshair\" was not injected!";
        assert snapshotButton != null : "fx:id\"snapshotButton\" was not injected!";
        assert visibleColumn != null : "fx:id\"visibleColumn\" was not injected!";
        assert avgColumn != null : "fx:id\"avgColumn\" was not injected!";
        assert minColumn != null : "fx:id\"minColumn\" was not injected!";
        assert maxColumn != null : "fx:id\"maxColumn\" was not injected!";
        assert currentColumn != null : "fx:id\"currentColumn\" was not injected!";
        //endregion

        //region Control initialization
        initChartPane();
        initNavigationPane();
        initChartSettingPane();
        initTableViewPane();
        //endregion

        //region *** Global preferences ***
        globalPrefs.downSamplingEnabledProperty().addListener(refreshOnPreferenceListener);
        globalPrefs.downSamplingThresholdProperty().addListener(refreshOnPreferenceListener);
        //endregion
    }

    private void initChartPane() {
        //region *** XYChart ***
        ZonedDateTimeAxis xAxis = new ZonedDateTimeAxis(getWorksheet().getTimeZone());
        xAxis.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        xAxis.setAnimated(false);
        xAxis.setSide(Side.BOTTOM);
        StableTicksAxis yAxis;
        if (worksheet.getDefaultChart().getUnitPrefixes() == UnitPrefixes.BINARY) {
            yAxis = new BinaryStableTicksAxis();
        }
        else {
            yAxis = new MetricStableTicksAxis();
        }
        yAxis.setSide(Side.LEFT);
        yAxis.autoRangingProperty().bindBidirectional(worksheet.getDefaultChart().autoScaleYAxisProperty());
        yAxis.setAnimated(false);
        yAxis.setTickSpacing(30);
        yAxis.labelProperty().bind(worksheet.getDefaultChart().unitProperty());
        chart = buildChart(xAxis, (ValueAxis) yAxis);
        chart.setCache(true);
        chart.setCacheHint(CacheHint.SPEED);
        chart.setCacheShape(true);
        chart.setFocusTraversable(true);
        chart.setLegendVisible(false);
        chart.animatedProperty().bindBidirectional(globalPrefs.chartAnimationEnabledProperty());
        chartParent.getChildren().add(chart);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setTopAnchor(chart, 0.0);
        //endregion
    }

    private void initNavigationPane() {
        //region *** Buttons ***
        backButton.setOnAction(this::handleHistoryBack);
        forwardButton.setOnAction(this::handleHistoryForward);
        refreshButton.setOnAction(this::handleRefresh);
        snapshotButton.setOnAction(this::handleTakeSnapshot);
        backButton.disableProperty().bind(backwardHistory.emptyStackProperty);
        forwardButton.disableProperty().bind(forwardHistory.emptyStackProperty);
        //endregion

        //region *** Time pickers ***
        this.currentState = new XYChartViewState(getWorksheet().getFromDateTime(), getWorksheet().getToDateTime(), 0, 100);
        getWorksheet().fromDateTimeProperty().bind(currentState.startX);
        getWorksheet().toDateTimeProperty().bind(currentState.endX);
        plotChart(currentState.asSelection(), true);
        endDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.dateTimeValueProperty().bindBidirectional(currentState.startX);
        endDate.dateTimeValueProperty().bindBidirectional(currentState.endX);
        //endregion

        //region *** Crosshair ***
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;//  DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);

        crossHair = new XYChartCrosshair<>(chart, chartParent, dateTimeFormatter::format, n -> String.format("%,.2f", n.doubleValue()));

        crossHair.onSelectionDone(s -> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());
            currentState.setSelection(s, true);
        });
        hCrosshair.selectedProperty().bindBidirectional(globalPrefs.horizontalMarkerOnProperty());
        vCrosshair.selectedProperty().bindBidirectional(globalPrefs.verticalMarkerOnProperty());
        crossHair.horizontalMarkerVisibleProperty().bind(Bindings.createBooleanBinding(() -> globalPrefs.isShiftPressed() || hCrosshair.isSelected(), hCrosshair.selectedProperty(), globalPrefs.shiftPressedProperty()));
        crossHair.verticalMarkerVisibleProperty().bind(Bindings.createBooleanBinding(() -> globalPrefs.isCtrlPressed() || vCrosshair.isSelected(), vCrosshair.selectedProperty(), globalPrefs.ctrlPressedProperty()));
        currentColumn.setVisible(crossHair.isVerticalMarkerVisible());
        crossHair.verticalMarkerVisibleProperty().addListener((observable, oldValue, newValue) -> currentColumn.setVisible(newValue));
        worksheet.getCharts().get(0).yAxisMinValueProperty().bindBidirectional(currentState.startY);
        ((ValueAxis<Double>) chart.getYAxis()).lowerBoundProperty().bindBidirectional(currentState.startY);
        worksheet.getDefaultChart().yAxisMaxValueProperty().bindBidirectional(currentState.endY);
        ((ValueAxis<Double>) chart.getYAxis()).upperBoundProperty().bindBidirectional(currentState.endY);

        //endregion
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
        chartPropertiesButton.selectedProperty().bindBidirectional(propertiesController.visibleProperty());
        chartParent.getChildren().add(chartPropertiesButton);
        chartParent.getChildren().add(settingsPane);
    }

    private void initTableViewPane() {
        seriesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        visibleColumn.setCellFactory(CheckBoxTableCell.forTableColumn(visibleColumn));
        pathColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getBinding().getTreeHierarchy()));
        colorColumn.setCellFactory(param -> new ColorTableCell<>(colorColumn));
        colorColumn.setCellValueFactory(p -> p.getValue().displayColorProperty());
        avgColumn.setCellValueFactory(p -> Bindings.createStringBinding(
                () -> p.getValue().getProcessor() == null ? "NaN" : prefixFormatter.format(p.getValue().getProcessor().getAverageValue()),
                p.getValue().processorProperty()));

        minColumn.setCellValueFactory(p -> Bindings.createStringBinding(
                () -> p.getValue().getProcessor() == null ? "NaN" : prefixFormatter.format(p.getValue().getProcessor().getMinValue()),
                p.getValue().processorProperty()));

        maxColumn.setCellValueFactory(p -> Bindings.createStringBinding(
                () -> p.getValue().getProcessor() == null ? "NaN" : prefixFormatter.format(p.getValue().getProcessor().getMaxValue()),
                p.getValue().processorProperty()));

        currentColumn.setCellValueFactory(p -> Bindings.createStringBinding(
                () -> {
                    if (p.getValue().getProcessor() == null) {
                        return "NaN";
                    }
                    return prefixFormatter.format(p.getValue().getProcessor().tryGetNearestValue(crossHair.getCurrentXValue()).orElse(Double.NaN));
                }, crossHair.currentXValueProperty()));

        seriesTable.setRowFactory(this::seriesTableRowFactory);

        seriesTable.setOnKeyReleased(event -> {
            if (event.getCode().equals(KeyCode.DELETE)) {
                removeSelectedBinding();
            }
        });
        seriesTable.setItems(worksheet.getDefaultChart().getSeries());
    }

    //endregion

    @Override
    public void close() {
        if (refreshOnPreferenceListener != null) {
            logger.debug(() -> "Unregister listeners attached to global preferences from controller for worksheet " + getWorksheet().getName());
            globalPrefs.downSamplingEnabledProperty().removeListener(refreshOnPreferenceListener);
            globalPrefs.downSamplingThresholdProperty().removeListener(refreshOnPreferenceListener);
            for (TimeSeriesInfo<Double> t : worksheet.getDefaultChart().getSeries()) {
                t.selectedProperty().removeListener(refreshOnSelectSeries);
            }
        }
    }

    public void showPropertiesPane(boolean show) {
        this.chartPropertiesButton.setSelected(show);
    }

    public void setReloadRequiredHandler(Consumer<WorksheetController> action) {
        if (this.chartTypeListener != null) {
            this.worksheet.getDefaultChart().chartTypeProperty().removeListener(this.chartTypeListener);
        }
        this.chartTypeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                logger.debug("Reloading worksheet controller because chart type change from: " + oldValue + " to " + newValue);
                action.accept(this);
            }
        };
        this.worksheet.getDefaultChart().chartTypeProperty().addListener(this.chartTypeListener);
    }

    //region *** protected members ***

    protected abstract XYChart<ZonedDateTime, Double> buildChart(ZonedDateTimeAxis xAxis, ValueAxis<Double> yAxis);

    protected void addBindings(Collection<TimeSeriesBinding<Double>> bindings) {
        for (TimeSeriesBinding<Double> b : bindings) {
            TimeSeriesInfo<Double> newSeries = TimeSeriesInfo.fromBinding(b);
            newSeries.selectedProperty().addListener(refreshOnSelectSeries);
            worksheet.getDefaultChart().addSeries(newSeries);
        }
        invalidate(false, false, false);
    }

    protected void removeSelectedBinding() {
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
        removeSelectedBinding();
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
        try (Profiler p = Profiler.start("Adding series to chart " + getWorksheet().getName(), logger::trace)) {
            worksheetMaskerPane.setVisible(true);
            AsyncTaskManager.getInstance().submit(() -> {
                        getWorksheet().fetchDataFromSources(currentSelection.getStartX(), currentSelection.getEndX(), forceRefresh);
                        return getWorksheet().getDefaultChart().getSeries()
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
                                .map(this::makeXYChartSeries)
                                .collect(Collectors.toList());
                    },
                    event -> {
                        worksheetMaskerPane.setVisible(false);
                        chart.getData().setAll((Collection<? extends XYChart.Series<ZonedDateTime, Double>>) event.getSource().getValue());
                    },
                    event -> {
                        worksheetMaskerPane.setVisible(false);
                        Dialogs.notifyException("Failed to retrieve data from source", event.getSource().getException(), root);
                    });
        }
    }

    private XYChart.Series<ZonedDateTime, Double> makeXYChartSeries(TimeSeriesInfo<Double> series) {
        try (Profiler p = Profiler.start("Building  XYChart.Series data for" + series.getDisplayName(), logger::trace)) {
            XYChart.Series<ZonedDateTime, Double> newSeries = new XYChart.Series<>();
            newSeries.getData().setAll(series.getProcessor().getData());
            newSeries.nodeProperty().addListener((node, oldNode, newNode) -> {
                Chart<Double> currentChart = worksheet.getDefaultChart();
                if (newNode != null) {
                    switch (getChartType()) {
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
                TimeSeriesInfo<Double> draggedseries = seriesTable.getItems().remove(draggedIndex);
                int dropIndex;
                if (row.isEmpty()) {
                    dropIndex = seriesTable.getItems().size();
                }
                else {
                    dropIndex = row.getIndex();
                }
                seriesTable.getItems().add(dropIndex, draggedseries);
                event.setDropCompleted(true);
                seriesTable.getSelectionModel().clearAndSelect(dropIndex);
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

        /**
         * Initializes a new instance of the {@link XYChartViewState} class.
         *
         * @param startX the start of the time interval
         * @param endX   the end of the time interval
         * @param startY the lower bound of the Y axis
         * @param endY   the upper bound of the Y axis
         */
        public XYChartViewState(ZonedDateTime startX, ZonedDateTime endX, double startY, double endY) {
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
                    worksheet.getDefaultChart().isAutoScaleYAxis()
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
                    double r = (((ValueAxis<Double>) chart.getYAxis()).getUpperBound() - ((ValueAxis<Double>) chart.getYAxis()).getLowerBound()) - Math.abs(selection.getEndY() - selection.getStartY());
                    logger.debug(() -> "Y selection - Y axis range = " + r);
                    if (r > 0.0001) {
                        worksheet.getDefaultChart().setAutoScaleYAxis(false);
                    }
                }
                else {
                    worksheet.getDefaultChart().setAutoScaleYAxis(selection.isAutoRangeY());
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
