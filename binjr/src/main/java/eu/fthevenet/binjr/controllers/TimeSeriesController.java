package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.charts.XYChartCrosshair;
import eu.fthevenet.binjr.charts.XYChartSelection;
import eu.fthevenet.binjr.controls.ZonedDateTimePicker;
import eu.fthevenet.binjr.data.timeseries.DoubleTimeSeries;
import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.logging.Profiler;
import eu.fthevenet.binjr.data.adapters.DataAdapterException;
import eu.fthevenet.binjr.data.adapters.TimeSeriesBinding;
import eu.fthevenet.binjr.data.timeseries.TimeSeries;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The controller class for the time series view.
 *
 * @author Frederic Thevenet
 */
public class TimeSeriesController implements Initializable {
    private static final Logger logger = LogManager.getLogger(TimeSeriesController.class);
    @FXML
    public AnchorPane root;
    @FXML
    public AnchorPane chartParent;
    @FXML
    private AreaChart<ZonedDateTime, Double> chart;
    @FXML
    private TextField yMinRange;
    @FXML
    private TextField yMaxRange;
    @FXML
    private TableView<TimeSeries<Double>> seriesTable;
    @FXML
    private Button backButton;
    @FXML
    private Button forwardButton;
    @FXML
    private Button resetYButton;
    @FXML
    private ZonedDateTimePicker startDate;
    @FXML
    private ZonedDateTimePicker endDate;
    @FXML
    private TableColumn<TimeSeries<Double>, String> sourceColumn;
    @FXML
    private TableColumn<TimeSeries<Double>, String> colorColumn;

    private MainViewController mainViewController;
    private ObservableList<TimeSeries<Double>> seriesData = FXCollections.observableArrayList();
    private List<TimeSeriesBinding<Double>> seriesBindings = new ArrayList<>();
    // private ObservableMap<TimeSeriesBinding<Double>, TimeSeries<Double>> series = FXCollections.observableHashMap();
    private XYChartCrosshair<ZonedDateTime, Double> crossHair;
    private XYChartViewState currentState;
    private XYChartSelection<ZonedDateTime, Double> previousState;
    private History backwardHistory = new History();
    private History forwardHistory = new History();
    private GlobalPreferences globalPrefs;
    private String name;


    //region [Properties]
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MainViewController getMainViewController() {
        return mainViewController;
    }

    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    public XYChartCrosshair<ZonedDateTime, Double> getCrossHair() {
        return crossHair;
    }

    public XYChart<ZonedDateTime, Double> getChart() {
        return chart;
    }
    //endregion

    //region [Initializable Members]
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert root != null : "fx:id\"root\" was not injected!";
        assert chart != null : "fx:id\"chart\" was not injected!";
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

        globalPrefs = GlobalPreferences.getInstance();

        chart.createSymbolsProperty().bindBidirectional(globalPrefs.sampleSymbolsVisibleProperty());
        chart.animatedProperty().bindBidirectional(globalPrefs.chartAnimationEnabledProperty());
        globalPrefs.downSamplingEnabledProperty().addListener((observable, oldValue, newValue) -> invalidate(false, true, true));
        globalPrefs.downSamplingThresholdProperty().addListener((observable, oldValue, newValue) -> invalidate(false, true, true));

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
        NumberStringConverter numberFormatter = new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT));

        ZonedDateTime now = ZonedDateTime.now();
        this.currentState = new XYChartViewState(now.minus(24, ChronoUnit.HOURS), now, 0, 100);
        plotChart(currentState.asSelection());

        backButton.disableProperty().bind(backwardHistory.emptyStackProperty);
        forwardButton.disableProperty().bind(forwardHistory.emptyStackProperty);
        startDate.dateTimeValueProperty().bindBidirectional(currentState.startX);
        endDate.dateTimeValueProperty().bindBidirectional(currentState.endX);
        crossHair = new XYChartCrosshair<>(chart, chartParent, dateTimeFormatter::format, (n) -> String.format("%,.2f", n.doubleValue()));

        setAndBindTextFormatter(yMinRange, numberFormatter, currentState.startY, ((ValueAxis<Double>) chart.getYAxis()).lowerBoundProperty());
        setAndBindTextFormatter(yMaxRange, numberFormatter, currentState.endY, ((ValueAxis<Double>) chart.getYAxis()).upperBoundProperty());
        crossHair.onSelectionDone(s -> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());
            currentState.setSelection(s, true);
        });

        sourceColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getBinding().getAdapter().getSourceName()));

//        colorColumn.setCellFactory(param -> new TableCell<TimeSeries<Double>, Color>() {
//
//            @Override
//            public void updateItem(Color item, boolean empty) {
//                super.updateItem(item, empty);
//                TimeSeries<Double> current = param.getTableView().getSelectionModel().getSelectedItem();
//     //           if (current != null) {
//                    setStyle("-fx-background-color:#ff4500");// + current.getBinding().getColor().toString());
//       //         }
//            }
//        });

        seriesTable.setItems(seriesData);
        seriesTable.setOnKeyReleased(event -> {
            if (event.getCode().equals(KeyCode.DELETE)) {
                TimeSeries<Double> current = seriesTable.getSelectionModel().getSelectedItem();
                if (current != null) {
                    seriesTable.getItems().remove(current);
                    seriesBindings.remove(current.getBinding());
                    invalidate(false, true, true);
                }
            }
        });
    }

    public void addBinding(TimeSeriesBinding<Double> binding) {
        this.seriesBindings.add(binding);
        invalidate(false, true, true);
        chart.getYAxis().setAutoRanging(true);
    }
    //endregion

    public void invalidate(boolean saveToHistory, boolean plotChart) {
        invalidate(saveToHistory, plotChart, false);
    }

    public void invalidate(boolean saveToHistory, boolean plotChart, boolean forceRefresh) {
        logger.debug(() -> "Refreshing chart");
        XYChartSelection<ZonedDateTime, Double> currentSelection = currentState.asSelection();
        logger.debug(() -> "currentSelection=" + (currentSelection == null ? "null" : currentSelection.toString()));
        if (!currentSelection.equals(previousState) || forceRefresh) {
            if (saveToHistory) {
                this.backwardHistory.push(previousState);
                this.forwardHistory.clear();
            }
            previousState = currentState.asSelection();
            logger.debug(() -> backwardHistory.dump());
            if (plotChart) {
                plotChart(currentSelection);
            }
        }
        else {
            logger.debug(() -> "XYChartViewState hasn't change, no need to redraw the graph");
        }
    }
    //region [UI event handlers]
    /**
     * Handles user interaction with the "back" button.
     * @param actionEvent the event
     */
    public void handleHistoryBack(ActionEvent actionEvent) {
        restoreSelectionFromHistory(backwardHistory, forwardHistory);
    }

    /**
     *  Handles user interaction with the "forward" button.
     * @param actionEvent the event
     */
    public void handleHistoryForward(ActionEvent actionEvent) {
        restoreSelectionFromHistory(forwardHistory, backwardHistory);
    }

    /**
     * Handles user interaction with the "reset" button.
     * @param actionEvent the event
     */
    public void handleResetYRangeButton(ActionEvent actionEvent) {
        chart.getYAxis().setAutoRanging(true);
    }

    /**
     * Handles user interaction with the "refresh" button.
     * @param actionEvent the event
     */
    public void handleRefresh(ActionEvent actionEvent) {
        this.invalidate(false, true, true);
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

    private void plotChart(XYChartSelection<ZonedDateTime, Double> currentSelection) {
        try (Profiler p = Profiler.start("Plotting chart")) {
            chart.getData().clear();
            seriesData.clear();
            seriesData.addAll(TimeSeries.fromBinding(
                    seriesBindings,
                    currentSelection.getStartX(),
                    currentSelection.getEndX()));

            chart.getData().addAll(seriesData.stream().map(TimeSeries::asSeries).collect(Collectors.toList()));
//            seriesTable.getItems().clear();
//
//            for (XYChart.Series s : chart.getData()) {
//                SelectableListItem i = new SelectableListItem(s.getName(), true);
////                i.selectedProperty().addListener((obs, wasOn, isNowOn) -> {
////                   selectedSeriesCache.put(s.getName(), isNowOn);
////                    s.getNode().visibleProperty().bindBidirectional(i.selectedProperty());
////                });
////                i.setSelected(selectedSeriesCache.getOrDefault(s.getName(), true));
//                seriesTable.getItems().add(i);
//            }
        } catch (DataAdapterException /*| IOException | ParseException*/ e) {
            Dialogs.displayException("Failed to retrieve data from source", e ,root);
        }
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
         * @param toHistory true if the change in state should be recoreded in the history
         */
        void setSelection(XYChartSelection<ZonedDateTime, Double> selection, boolean toHistory) {
            frozen = true;
            try {
                ZonedDateTime newStartX = roundDateTime(selection.getStartX());
                ZonedDateTime newEndX = roundDateTime(selection.getEndX());
                boolean plotChart = !(newStartX.isEqual(startX.get()) && newEndX.isEqual(endX.get()));
                this.startX.set(newStartX);
                this.endX.set(newEndX);
                this.startY.set(roundYValue(selection.getStartY()));
                this.endY.set(roundYValue(selection.getEndY()));
                invalidate(toHistory, plotChart);
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
        XYChartViewState(ZonedDateTime startX, ZonedDateTime endX, double startY, double endY) {
            this.startX = new SimpleObjectProperty<>(roundDateTime(startX));
            this.endX = new SimpleObjectProperty<>(roundDateTime(endX));
            this.startY = new SimpleDoubleProperty(roundYValue(startY));
            this.endY = new SimpleDoubleProperty(roundYValue(endY));

            this.startX.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(true, true);
                }
            });
            this.endX.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(true, true);
                }
            });
            this.startY.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(true, false);
                }
            });
            this.endY.addListener((observable, oldValue, newValue) -> {
                if (!frozen) {
                    invalidate(true, false);
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


    //endregion
}
