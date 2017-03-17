package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.workspace.TimeSeriesInfo;
import eu.fthevenet.binjr.data.workspace.Worksheet;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.util.logging.Profiler;
import eu.fthevenet.util.text.BinaryPrefixFormat;
import eu.fthevenet.util.text.MetricPrefixFormat;
import eu.fthevenet.util.text.PrefixFormat;
import eu.fthevenet.util.ui.charts.StableTicksAxis;
import eu.fthevenet.util.ui.charts.XYChartCrosshair;
import eu.fthevenet.util.ui.charts.XYChartSelection;
import eu.fthevenet.util.ui.charts.ZonedDateTimeAxis;
import eu.fthevenet.util.ui.controls.ColorTableCell;
import eu.fthevenet.util.ui.controls.ZonedDateTimePicker;
import eu.fthevenet.util.ui.dialogs.Dialogs;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The controller class for the time series view.
 *
 * @author Frederic Thevenet
 */
public abstract class WorksheetController implements Initializable {
    private static final Logger logger = LogManager.getLogger(WorksheetController.class);
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
    @FXML
    private Button resetYButton;
    @FXML
    private Button snapshotButton;

    @FXML
    private ZonedDateTimePicker startDate;
    @FXML
    private ZonedDateTimePicker endDate;
    @FXML
    private TableColumn<TimeSeriesInfo<Double>, String> sourceColumn;
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
    private Slider graphOpacitySlider;


    //   @FXML
    //   private MenuItem removeSeriesMenuItem;

    @FXML
    private ContextMenu seriesListMenu;

    private MainViewController mainViewController;
    // private ObservableList<TimeSeriesProcessor<Double>> seriesData = FXCollections.observableArrayList();
    // private SortedSet<TimeSeriesBinding<Double>> seriesBindings = new TreeSet<>();
    private XYChartCrosshair<ZonedDateTime, Double> crossHair;
    private XYChartViewState currentState;
    private XYChartSelection<ZonedDateTime, Double> previousState;
    private History backwardHistory = new History();
    private History forwardHistory = new History();
    private GlobalPreferences globalPrefs;
    private String name;
    private AtomicInteger seriesOrder = new AtomicInteger(0);
    private final Worksheet<Double> worksheet;
    private final DoubleProperty graphOpacity = new SimpleDoubleProperty(0.8);
    private final PrefixFormat prefixFormat;

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
    //endregion


    public WorksheetController(MainViewController mainViewController, Worksheet<Double> worksheet) {
        this.mainViewController = mainViewController;
        this.worksheet = worksheet;
        switch (this.worksheet.getUnitPrefixes()) {
            case BINARY:
                this.prefixFormat = new BinaryPrefixFormat();
                break;
            case METRIC:
                this.prefixFormat = new MetricPrefixFormat();
                break;

            default:
                throw new IllegalArgumentException("Unknown unit prefix");
        }
    }

    //region [Initializable Members]
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //region *** Nodes injection checks ***
        assert root != null : "fx:id\"root\" was not injected!";
        assert chartParent != null : "fx:id\"chartParent\" was not injected!";
        assert yMinRange != null : "fx:id\"yMinRange\" was not injected!";
        assert yMaxRange != null : "fx:id\"yMaxRange\" was not injected!";
        assert seriesTable != null : "fx:id\"seriesTable\" was not injected!";
        assert backButton != null : "fx:id\"backButton\" was not injected!";
        assert forwardButton != null : "fx:id\"forwardButton\" was not injected!";
        assert resetYButton != null : "fx:id\"resetYButton\" was not injected!";
        assert startDate != null : "fx:id\"beginDateTime\" was not injected!";
        assert endDate != null : "fx:id\"endDateTime\" was not injected!";
        assert sourceColumn != null : "fx:id\"sourceColumn\" was not injected!";
        assert colorColumn != null : "fx:id\"colorColumn\" was not injected!";
        assert refreshButton != null : "fx:id\"refreshButton\" was not injected!";
        assert vCrosshair != null : "fx:id\"vCrosshair\" was not injected!";
        assert hCrosshair != null : "fx:id\"hCrosshair\" was not injected!";
        assert snapshotButton != null : "fx:id\"snapshotButton\" was not injected!";
        assert graphOpacitySlider != null : "fx:id\"graphOpacitySlider\" was not injected!";
        assert visibleColumn != null : "fx:id\"visibleColumn\" was not injected!";
        assert avgColumn != null : "fx:id\"avgColumn\" was not injected!";
        assert minColumn != null : "fx:id\"minColumn\" was not injected!";
        assert maxColumn != null : "fx:id\"maxColumn\" was not injected!";
        assert currentColumn != null : "fx:id\"currentColumn\" was not injected!";
        //endregion

        //region *** XYChart ***
        ZonedDateTimeAxis xAxis = new ZonedDateTimeAxis(getWorksheet().getTimeZone());
        xAxis.setAnimated(false);
        xAxis.setSide(Side.BOTTOM);

        StableTicksAxis yAxis = new StableTicksAxis();
        yAxis.setSide(Side.LEFT);
        yAxis.setAutoRanging(true);
        yAxis.setAnimated(false);
        yAxis.setTickSpacing(30);

        chart = buildChart(xAxis, (ValueAxis) yAxis);
        chart.setFocusTraversable(true);
        chart.setLegendVisible(false);
        chartParent.getChildren().add(chart);

        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setTopAnchor(chart, 0.0);
        //endregion

        //region *** Buttons ***
        backButton.setOnAction(this::handleHistoryBack);
        forwardButton.setOnAction(this::handleHistoryForward);
        refreshButton.setOnAction(this::handleRefresh);
        resetYButton.setOnAction(this::handleResetYRangeButton);
        snapshotButton.setOnAction(this::handleTakeSnapshot);
        forwardButton.setOnAction(this::handleHistoryForward);
        backButton.disableProperty().bind(backwardHistory.emptyStackProperty);
        forwardButton.disableProperty().bind(forwardHistory.emptyStackProperty);
        //endregion

        //region *** Global preferences ***
        globalPrefs = GlobalPreferences.getInstance();
        chart.animatedProperty().bindBidirectional(globalPrefs.chartAnimationEnabledProperty());
        globalPrefs.downSamplingEnabledProperty().addListener((observable, oldValue, newValue) -> invalidate(false));
        globalPrefs.downSamplingThresholdProperty().addListener((observable, oldValue, newValue) -> invalidate(false));
        globalPrefs.useSourceColorsProperty().addListener((observable, oldValue, newValue) -> invalidate(false));
        //endregion

        //region *** Time pickers ***
        this.currentState = new XYChartViewState(getWorksheet().getFromDateTime(), getWorksheet().getToDateTime(), 0, 100);
        getWorksheet().fromDateTimeProperty().bind(currentState.startX);
        getWorksheet().toDateTimeProperty().bind(currentState.endX);
        plotChart(currentState.asSelection());
        endDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.zoneIdProperty().bind(getWorksheet().timeZoneProperty());
        startDate.dateTimeValueProperty().bindBidirectional(currentState.startX);
        endDate.dateTimeValueProperty().bindBidirectional(currentState.endX);
        //endregion

        //region *** Crosshair ***
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
        NumberStringConverter numberFormatter = new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT));
        crossHair = new XYChartCrosshair<>(chart, chartParent, dateTimeFormatter::format, (n) -> String.format("%,.2f", n.doubleValue()));
        setAndBindTextFormatter(yMinRange, numberFormatter, currentState.startY, ((ValueAxis<Double>) chart.getYAxis()).lowerBoundProperty());
        setAndBindTextFormatter(yMaxRange, numberFormatter, currentState.endY, ((ValueAxis<Double>) chart.getYAxis()).upperBoundProperty());
        crossHair.onSelectionDone(s -> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());
            currentState.setSelection(s, true);
        });
        crossHair.horizontalMarkerVisibleProperty().bindBidirectional(hCrosshair.selectedProperty());
        crossHair.verticalMarkerVisibleProperty().bindBidirectional(vCrosshair.selectedProperty());
        mainViewController.showHorizontalMarkerProperty().bindBidirectional(crossHair.horizontalMarkerVisibleProperty());
        mainViewController.showVerticalMarkerProperty().bindBidirectional(crossHair.verticalMarkerVisibleProperty());
        //endregion


        //region *** Series TableView ***
        visibleColumn.setCellFactory(CheckBoxTableCell.forTableColumn(visibleColumn));
        sourceColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getBinding().getAdapter().getSourceName()));
        colorColumn.setCellFactory(param -> new ColorTableCell<>(colorColumn));
        colorColumn.setCellValueFactory(p -> p.getValue().displayColorProperty());
        avgColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getProcessor() == null ? "NaN" : prefixFormat.format(p.getValue().getProcessor().getAverageValue())));


        minColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getProcessor() == null ? "NaN" : prefixFormat.format(p.getValue().getProcessor().getMinValue())));
        maxColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getProcessor() == null ? "NaN" : prefixFormat.format(p.getValue().getProcessor().getMaxValue())));

        // maxColumn.setCellFactory(p-> new TableCell<TimeSeriesInfo<Double>, Double>());

        currentColumn.setCellValueFactory(p -> new SimpleStringProperty(prefixFormat.format(Double.NaN)));

        seriesTable.setItems(worksheet.getSeries());
        seriesTable.setOnKeyReleased(event -> {
            if (event.getCode().equals(KeyCode.DELETE)) {
                removeSelectedBinding();
            }
        });
        //endregion
    }

    protected abstract XYChart<ZonedDateTime, Double> buildChart(ZonedDateTimeAxis xAxis, ValueAxis<Double> yAxis);

    public void addBindings(Collection<TimeSeriesBinding<Double>> bindings) {
        for (TimeSeriesBinding<Double> b : bindings) {
            TimeSeriesInfo<Double> newSeries = TimeSeriesInfo.fromBinding(b);
            newSeries.selectedProperty().addListener((observable, oldValue, newValue) -> invalidate(false));
            worksheet.addSeries(newSeries);
        }

        invalidate(false);
        chart.getYAxis().setAutoRanging(true);
    }


    public void removeSelectedBinding() {
        TimeSeriesInfo<Double> current = seriesTable.getSelectionModel().getSelectedItem();
        if (current != null) {
            seriesTable.getItems().remove(current);

            invalidate(false);
        }
    }
    //endregion

    public void invalidate(boolean saveToHistory) {
        invalidate(saveToHistory, false);
    }

    public void invalidate(boolean saveToHistory, boolean dontPlotChart) {
        logger.debug(() -> "Refreshing chart");
        XYChartSelection<ZonedDateTime, Double> currentSelection = currentState.asSelection();
        logger.debug(() -> "currentSelection=" + (currentSelection == null ? "null" : currentSelection.toString()));
        if (saveToHistory) {
            this.backwardHistory.push(previousState);
            this.forwardHistory.clear();
        }
        previousState = currentState.asSelection();
        logger.debug(() -> backwardHistory.dump());
        if (!dontPlotChart) {
            plotChart(currentSelection);
        }
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
    protected void handleResetYRangeButton(ActionEvent actionEvent) {
        chart.getYAxis().setAutoRanging(true);
    }

    @FXML
    protected void handleRefresh(ActionEvent actionEvent) {
        this.invalidate(false);
    }
    //endregion

    //region [Private Members]
    private <T extends Number> void setAndBindTextFormatter(TextField textField, StringConverter<T> converter, Property<T> stateProperty, Property<T> axisBoundProperty) {
        final TextFormatter<T> formatter = new TextFormatter<>(converter);
        formatter.valueProperty().bindBidirectional(stateProperty);
        axisBoundProperty.bindBidirectional(stateProperty);
        formatter.valueProperty().addListener((observable, o, v) -> chart.getYAxis().setAutoRanging(false));
        textField.setTextFormatter(formatter);
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

    private void applyOpacityToSeries(Iterable<TimeSeriesInfo<Double>> series, Double opacity) {
        for (TimeSeriesInfo<Double> s : series) {
            if (s.getDisplayColor() != null) {
                s.setDisplayColor(Color.color(s.getDisplayColor().getRed(),
                        s.getDisplayColor().getGreen(),
                        s.getDisplayColor().getBlue(),
                        opacity));
            }
        }
    }

    //TODO make sure this is only called if worksheet is visible/current
    private void plotChart(XYChartSelection<ZonedDateTime, Double> currentSelection) {
        try (Profiler p = Profiler.start("Plotting chart")) {
            chart.getData().clear();

            getWorksheet().fillData(currentSelection.getStartX(), currentSelection.getEndX());

            graphOpacitySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    applyOpacityToSeries(worksheet.getSeries(), newValue.doubleValue());
                }
            });

            chart.getData().addAll(getWorksheet().getSeries()
                    .stream()
                    .filter(series -> series.getProcessor() != null && series.isSelected())
                    .map(this::makeXYChartSeries)
                    .collect(Collectors.toList()));

            applyOpacityToSeries(worksheet.getSeries(), graphOpacitySlider.getValue());

        } catch (DataAdapterException e) {
            Dialogs.displayException("Failed to retrieve data from source", e, root);
        }
    }

    private XYChart.Series<ZonedDateTime, Double> makeXYChartSeries(TimeSeriesInfo<Double> series) {
        XYChart.Series<ZonedDateTime, Double> s = new XYChart.Series<>();
        s.getData().addAll(series.getProcessor().getData());
        s.nodeProperty().addListener((node, oldNode, newNode) -> {
            if (newNode != null) {
                ObservableList<Node> children = ((Group) newNode).getChildren();
                if (children != null) {
                    if (children.size() >= 1) {
                        Path stroke = (Path) children.get(1);
                        Path fill = (Path) children.get(0);
                        stroke.setVisible(false);
                        if (series.getBinding().getColor() == null || !GlobalPreferences.getInstance().isUseSourceColors()) {
                            // use default jFX theme colors for series
                            stroke.strokeProperty().addListener((observable, oldValue, newValue) -> {
                                if (newValue != null) {
                                    series.setDisplayColor((Color) newValue);
                                }
                            });
                        }
                        logger.trace(() -> "Setting color of series " + series.getBinding().getLabel() + " to " + series.getDisplayColor());
                        fill.fillProperty().bind(series.displayColorProperty());
                    }
                }
            }
        });
        return s;
    }

    public void handleRemoveSeries(ActionEvent actionEvent) {
        removeSelectedBinding();
    }

    @FXML
    protected void handleTakeSnapshot(ActionEvent actionEvent) {
        WritableImage snapImg = root.snapshot(null, null);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save SnapShot");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        fileChooser.setInitialDirectory(new File(globalPrefs.getMostRecentSaveFolder()));
        fileChooser.setInitialFileName(String.format("binjr_snapshot_%s.png", getWorksheet().getName()));
        File selectedFile = fileChooser.showSaveDialog(Dialogs.getStage(root));
        if (selectedFile != null) {
            try {
                globalPrefs.setMostRecentSaveFolder(selectedFile.getParent());
                ImageIO.write(
                        SwingFXUtils.fromFXImage(snapImg, null),
                        "png",
                        selectedFile);
            } catch (IOException e) {
                Dialogs.displayException("Failed to save snapshot to disk", e, root);
            }
        }
    }

    public Worksheet<Double> getWorksheet() {
        return this.worksheet;
    }

    /**
     * Wraps a stack to record user navigation steps.
     */
    private class History {
        private final Stack<XYChartSelection<ZonedDateTime, Double>> stack = new Stack<>();
        private final SimpleBooleanProperty emptyStackProperty = new SimpleBooleanProperty(true);

        /**
         * Put the provided {@link XYChartSelection} on the top of the stack.
         *
         * @param state the provided {@link XYChartSelection}
         * @return the provided {@link XYChartSelection}
         */
        XYChartSelection<ZonedDateTime, Double> push(XYChartSelection<ZonedDateTime, Double> state) {
            if (state == null) {
                logger.warn(() -> "Trying to push null state into backwardHistory");
                return null;
            }
            else {
                emptyStackProperty.set(false);
                return this.stack.push(state);
            }
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
         * Returns the current state as a {@link XYChartSelection}
         *
         * @return the current state as a {@link XYChartSelection}
         */
        XYChartSelection<ZonedDateTime, Double> asSelection() {
            return new XYChartSelection<>(
                    startX.get(),
                    endX.get(),
                    startY.get(),
                    endY.get()
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
                this.startY.set(roundYValue(selection.getStartY()));
                this.endY.set(roundYValue(selection.getEndY()));
                invalidate(toHistory, dontPlotChart);
            } finally {
                frozen = false;
            }
        }

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
                    invalidate(true, false);
                }
            });
            this.endX.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(true, false);
                }
            });
            this.startY.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(true, true);
                }
            });
            this.endY.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(true, true);
                }
            });
        }

        private double roundYValue(double y) {
            return Math.round(y);
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
