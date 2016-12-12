package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.commons.charts.XYChartCrosshair;
import eu.fthevenet.binjr.commons.charts.XYChartSelection;
import eu.fthevenet.binjr.commons.controls.DateTimePicker;
import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.providers.JRDSDataProvider;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesBuilder;
import eu.fthevenet.binjr.data.timeseries.transform.LargestTriangleThreeBucketsTransform;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import jfxtras.scene.control.CalendarTextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Created by FTT2 on 02/12/2016.
 */
public class TimeSeriesController implements Initializable {
    private static final Logger logger = LogManager.getLogger(TimeSeriesController.class);
    @FXML
    public AnchorPane chartParent;
    @FXML
    public CalendarTextField beginDateTime;
    //    @FXML
//    public CalendarTextField endDateTime;
    @FXML
    private AreaChart<Date, Number> chart;
    @FXML
    private CheckBox yAutoRange;
    @FXML
    private TextField yMinRange;
    @FXML
    private TextField yMaxRange;
    @FXML
    ListView<SelectableListItem> seriesList;
    @FXML
    private Button backButton;

    @FXML
    private DateTimePicker startDate;
    @FXML
    private DateTimePicker endDate;


    private Property<String> currentHost = new SimpleStringProperty("ngwps006:31001/perf-ui");
    private Property<String> currentTarget = new SimpleStringProperty("ngwps006.mshome.net");
    private Property<String> currentProbe = new SimpleStringProperty("memprocPdh");
    private Map<String, Boolean> selectedSeriesCache = new HashMap<>();
    private XYChartCrosshair<Date, Number> crossHair;

    private State currentState;
    private XYChartSelection<Date, Number> previousState;
    private History selectionHistory = new History();
    private boolean invalidating;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert chart != null : "fx:id\"chart\" was not injected!";
//        assert beginDateTime != null : "fx:id\"beginDateTime\" was not injected!";
//        assert endDateTime != null : "fx:id\"endDateTime\" was not injected!";
        assert chartParent != null : "fx:id\"chartParent\" was not injected!";
        assert yAutoRange != null : "fx:id\"yAutoRange\" was not injected!";
        assert yMinRange != null : "fx:id\"yMinRange\" was not injected!";
        assert yMaxRange != null : "fx:id\"yMaxRange\" was not injected!";
        assert seriesList != null : "fx:id\"seriesList\" was not injected!";
        assert backButton != null : "fx:id\"backButton\" was not injected!";

        assert startDate != null : "fx:id\"beginDateTime\" was not injected!";
        assert endDate != null : "fx:id\"endDateTime\" was not injected!";

        this.currentState = new State(LocalDateTime.now().minusDays(1), LocalDateTime.now(), 100, 0);
       // this.previousState = currentState.asSelection();

        backButton.disableProperty().bind(selectionHistory.emptyStackProperty);

        startDate.dateTimeValueProperty().bindBidirectional(currentState.startX);
        endDate.dateTimeValueProperty().bindBidirectional(currentState.endX);
     //   startDate.dateTimeValueProperty().addListener((observable, o, v) -> invalidate());
     //   endDate.dateTimeValueProperty().addListener((observable, o, v) ->  invalidate());


//        beginDateTime.textProperty().addListener((observable, oldValue, newValue) -> {
//            currentState.setStartX(beginDateTime.getCalendar().getTime());
//              //   refreshChart();
//        });
//
//        endDateTime.textProperty().addListener((observable, oldValue, newValue) -> {
//           currentState.setEndX(endDateTime.getCalendar().getTime());
//              //  refreshChart();
//        });
//        beginDateTime.setCalendar(Calendar.getInstance());
//        beginDateTime.getCalendar().setTime(java.util.Date.from(begin));
//        endDateTime.setCalendar(Calendar.getInstance());
//        endDateTime.getCalendar().setTime(java.util.Date.from(end));

        seriesList.setCellFactory(CheckBoxListCell.forListView(SelectableListItem::selectedProperty));
        String defaultFormat = "yyyy/MM/dd HH:mm:ss";
        SimpleDateFormat formatter;

        formatter = new SimpleDateFormat(defaultFormat);
        crossHair = new XYChartCrosshair<>(chart, chartParent, formatter::format, (n) -> String.format("%,.2f", n.doubleValue()));

        chart.getYAxis().autoRangingProperty().bindBidirectional(yAutoRange.selectedProperty());
        setAndBindTextFormatter(yMinRange, new NumberStringConverter(), currentState.endY,((ValueAxis<Number>) chart.getYAxis()).lowerBoundProperty());
        setAndBindTextFormatter(yMaxRange, new NumberStringConverter(), currentState.startY,((ValueAxis<Number>) chart.getYAxis()).upperBoundProperty());

//
//        ((ValueAxis<Number>) chart.getYAxis()).lowerBoundProperty().bindBidirectional (currentState.endY);
//        ((ValueAxis<Number>) chart.getYAxis()).upperBoundProperty().bindBidirectional(currentState.startY);

        crossHair.onSelectionDone(s-> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());

            currentState.setSelection(s, true);
            yAutoRange.setSelected(false);
        });

        invalidate(true);
    }

    private <T extends Number> void setAndBindTextFormatter(TextField textField, StringConverter<T> converter, Property<T> stateProperty, Property<T> axisBoundProperty) {
        final TextFormatter<T> formatter = new TextFormatter<T>(converter);
      //  stateProperty.bind(formatter.valueProperty());
        formatter.valueProperty().bindBidirectional(stateProperty);
     //   axisBoundProperty.bind(formatter.valueProperty());
      //  formatter.valueProperty().bind(axisBoundProperty);

//        formatter.valueProperty().bindBidirectional(stateProperty);
        formatter.valueProperty().addListener((observable, o, v) -> {
            yAutoRange.setSelected(false);
        });
        textField.setTextFormatter(formatter);
    }

    public void invalidate(boolean historize) {

        logger.trace(() -> "Refreshing chart");
        XYChartSelection<Date, Number> currentSelection = currentState.asSelection();
        logger.debug(() ->"currentSelection=" + (currentSelection == null ? "null" : currentSelection.toString()));
        logger.debug(() -> "previousState=" + (previousState == null ? "null" : previousState.toString()));
        if (currentSelection.equals(previousState)) {
            logger.debug(() -> "State hasn't change, no need to redraw the graph");
            return;
        }
        if (historize) {
            this.selectionHistory.push(previousState);
            previousState = currentState.asSelection();
        }
        refreshChart(currentSelection);
    }


    public XYChartCrosshair<Date, Number> getCrossHair() {
        return crossHair;
    }

    public AreaChart<Date, Number> getChart() {
        return chart;
    }

    private void restoreSelectionFromHistory() {
        if (!selectionHistory.isEmpty()) {
            XYChartSelection<Date, Number> state = selectionHistory.pop();
            logger.debug(()-> "Restoring selection from history: " + (state != null ? state.toString() : "null"));
            currentState.setSelection(state, false);
       //     applySelectionToCurrentView(state);
       //     invalidate(false);
        }
        else {
            logger.debug(() -> "History is empty: nothing to go back to.");
        }
    }

//    private void applySelectionToCurrentView(XYChartSelection<Date, Number> s) {
//
////        yMinRange.setText(s.getEndY().toString());
////        yMaxRange.setText(s.getStartY().toString());
//
//     //   this.refreshChart(true);
//    }

//    Semaphore  refreshReentrancyGuard = new Semaphore(1);
//
//    private void refreshChart() {
//        logger.trace(() -> "Refreshing chart");
//        try {
//            if (refreshReentrancyGuard.tryAcquire()) {
//                unsafeRefreshChart();
//            }
//            else {
//                logger.debug("Attempt to reenter refreshChart() ignored.");
//            }
//        }
//        finally {
//            refreshReentrancyGuard.release();
//        }
//    }

    private void refreshChart(XYChartSelection<Date, Number> currentSelection) {


        ((ValueAxis<Number>) chart.getYAxis()).setLowerBound(currentSelection.getEndY().doubleValue());
        ((ValueAxis<Number>) chart.getYAxis()).setUpperBound(currentSelection.getStartY().doubleValue());


        try (Profiler p = Profiler.start("Refreshing chart view")) {
            chart.getData().clear();
            Map<String, XYChart.Series<Date, Number>> series = getRawData(
                    currentHost.getValue(),
                    currentTarget.getValue(),
                    currentProbe.getValue(),
                    currentSelection.getStartX().toInstant(),
                    currentSelection.getEndX().toInstant());
            chart.getData().addAll(series.values());
            seriesList.getItems().clear();
            for (XYChart.Series s : chart.getData()) {
                SelectableListItem i = new SelectableListItem(s.getName(), true);
                i.selectedProperty().addListener((obs, wasOn, isNowOn) -> {
                    selectedSeriesCache.put(s.getName(), isNowOn);
                    s.getNode().visibleProperty().bindBidirectional(i.selectedProperty());
                });
                i.setSelected(selectedSeriesCache.getOrDefault(s.getName(), true));
                seriesList.getItems().add(i);
            }
        } catch (IOException | ParseException e) {
            logger.error(() -> "Error getting data", e);
            throw new RuntimeException(e);
        }
//        currentState.endY.set(((ValueAxis<Number>) chart.getYAxis()).getLowerBound());
//        currentState.startY.set(((ValueAxis<Number>) chart.getYAxis()).getUpperBound());

    }

    private Map<String, XYChart.Series<Date, Number>> getRawData(String jrdsHost, String target, String probe, Instant begin, Instant end) throws IOException, ParseException {
        JRDSDataProvider dp = new JRDSDataProvider(jrdsHost);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (dp.getData(target, probe, begin, end, out)) {
                TimeSeriesBuilder timeSeriesBuilder = new TimeSeriesBuilder();
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                return timeSeriesBuilder.fromCSV(in)
                        .transform(new LargestTriangleThreeBucketsTransform(1000))
                        .build();//, counters);
            }
            else {
                throw new IOException(String.format("Failed to retrieve data from JRDS for %s %s %s %s", target, probe, begin.toString(), end.toString()));
            }
        }
    }

    public void handleHistoryBack(ActionEvent actionEvent) {
        this.restoreSelectionFromHistory();
    }

    public class History {
        private Stack<XYChartSelection<Date, Number>> stack = new Stack<>();
        public SimpleBooleanProperty emptyStackProperty = new SimpleBooleanProperty(true);

        public XYChartSelection<Date, Number> push(XYChartSelection<Date, Number> state) {
            if (state == null){
                logger.warn(()-> "Trying to push null state into history");
                return null;
            }
            else {
                emptyStackProperty.set(false);
                return this.stack.push(state);
            }
        }

        public XYChartSelection<Date, Number> pop() {
            XYChartSelection<Date, Number> r = this.stack.pop();
            emptyStackProperty.set(stack.isEmpty());
            return r;
        }

        public boolean isEmpty() {
            return emptyStackProperty.get();
        }
    }

    /**
     * Created by FTT2 on 07/12/2016.
     */
    public class State {

        private final SimpleObjectProperty<LocalDateTime> startX;
        private final SimpleObjectProperty<LocalDateTime> endX;
        private final SimpleDoubleProperty startY;
        private final SimpleDoubleProperty endY;

        private boolean frozen;

     //   private final SimpleObjectProperty<State> state = new SimpleObjectProperty<>();

        public XYChartSelection<Date, Number> asSelection() {
            return new XYChartSelection<>(
                    Date.from(ZonedDateTime.of(startX.get(), ZoneId.systemDefault()).toInstant()),
                    Date.from(ZonedDateTime.of(endX.get(), ZoneId.systemDefault()).toInstant()),
                    startY.get(),
                    endY.get()
            );
        }

        public void setSelection(XYChartSelection<Date, Number> selection, boolean toHistory) {
            frozen = true;
            try {
                this.startX.set(roundDateTime(LocalDateTime.ofInstant(selection.getStartX().toInstant(), ZoneId.systemDefault())));
                this.endX.set(roundDateTime(LocalDateTime.ofInstant(selection.getEndX().toInstant(), ZoneId.systemDefault())));
                this.startY.set(roundYValue(selection.getStartY().doubleValue()));
                this.endY.set(roundYValue(selection.getEndY().doubleValue()));
                invalidate(toHistory);
            }
            finally {
                frozen = false;
            }
        }

        public State(LocalDateTime startX, LocalDateTime endX, double startY, double endY) {
            this.startX = new SimpleObjectProperty<>(roundDateTime(startX));
            this.endX = new SimpleObjectProperty<>(roundDateTime(endX));
            this.startY = new SimpleDoubleProperty(roundYValue(startY));
            this.endY = new SimpleDoubleProperty(roundYValue(endY));

            this.startX.addListener((observable, oldValue, newValue) -> { if (!frozen) invalidate(true);});
            this.endX.addListener((observable, oldValue, newValue) -> { if (!frozen) invalidate(true);});
            this.startY.addListener((observable, oldValue, newValue) -> { if (!frozen) invalidate(true);});
            this.endY.addListener((observable, oldValue, newValue) -> { if (!frozen) invalidate(true);});
        }

        private double roundYValue(double y){
            return Math.round(y);
        }


        private LocalDateTime roundDateTime(LocalDateTime ldt){
            return  LocalDateTime.of(ldt.getYear(),
                    ldt.getMonth(),
                    ldt.getDayOfMonth(),
                    ldt.getHour(),
                    ldt.getMinute(),
                    ldt.getSecond());
        }

        @Override
        public String toString() {
            return String.format("State{startX=%s, endX=%s, startY=%s, endY=%s}", startX.get().toString(), endX.get().toString(), startY.get(), endY.get());
        }

    }
}
