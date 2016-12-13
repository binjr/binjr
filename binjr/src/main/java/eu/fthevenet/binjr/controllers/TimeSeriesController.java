package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.commons.charts.XYChartCrosshair;
import eu.fthevenet.binjr.commons.charts.XYChartSelection;
import eu.fthevenet.binjr.commons.controls.ZonedDateTimePicker;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private AreaChart<ZonedDateTime, Number> chart;
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
    private ZonedDateTimePicker startDate;
    @FXML
    private ZonedDateTimePicker endDate;


    private Property<String> currentHost = new SimpleStringProperty("ngwps006:31001/perf-ui");
    private Property<String> currentTarget = new SimpleStringProperty("ngwps006.mshome.net");
    private Property<String> currentProbe = new SimpleStringProperty("memprocPdh");
    private Map<String, Boolean> selectedSeriesCache = new HashMap<>();
    private XYChartCrosshair<ZonedDateTime, Number> crossHair;

    private State currentState;
    private XYChartSelection<ZonedDateTime, Number> previousState;

    private History history = new History();
    private boolean invalidating;

    private ZoneId currentZoneId = ZoneId.systemDefault();

    public History getHistory() {
        return history;
    }

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

//        LocalDateTime now = LocalDateTime.now();
//        now=   LocalDateTime.of(now.getYear(),
//                now.getMonth(),
//                now.getDayOfMonth(),
//                now.getHour(),
//                now.getMinute(),
//                now.getSecond());
//
//        XYChartSelection<Date,Number> initialSelection = new XYChartSelection<Date, Number>(
//                Date.from(ZonedDateTime.of(now.minusDays(1), ZoneId.systemDefault()).toInstant()),
//                Date.from(ZonedDateTime.of(now, ZoneId.systemDefault()).toInstant()),
//                0,
//                0);
//
//        plotChart(initialSelection);
//        this.currentState = new State(
//                LocalDateTime.ofInstant(initialSelection.getStartX().toInstant(), ZoneId.systemDefault()),
//                LocalDateTime.ofInstant(initialSelection.getEndX().toInstant(), ZoneId.systemDefault()),
//                initialSelection.getStartY().doubleValue(),
//                initialSelection.getEndY().doubleValue());


        this.currentState = new State(ZonedDateTime.now().minus(1, ChronoUnit.DAYS), ZonedDateTime.now(), 0, 0);
        plotChart(currentState.asSelection());


        backButton.disableProperty().bind(history.emptyStackProperty);

        startDate.dateTimeValueProperty().bindBidirectional(currentState.startX);
        endDate.dateTimeValueProperty().bindBidirectional(currentState.endX);

        seriesList.setCellFactory(CheckBoxListCell.forListView(SelectableListItem::selectedProperty));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        crossHair = new XYChartCrosshair<>(chart, chartParent, formatter::format, (n) -> String.format("%,.2f", n.doubleValue()));

        chart.getYAxis().autoRangingProperty().bindBidirectional(yAutoRange.selectedProperty());
        setAndBindTextFormatter(yMinRange, new NumberStringConverter(), currentState.endY,((ValueAxis<Number>) chart.getYAxis()).lowerBoundProperty());
        setAndBindTextFormatter(yMaxRange, new NumberStringConverter(), currentState.startY,((ValueAxis<Number>) chart.getYAxis()).upperBoundProperty());

        crossHair.onSelectionDone(s-> {
            logger.debug(() -> "Applying zoom selection: " + s.toString());

            currentState.setSelection(s, true);
            yAutoRange.setSelected(false);
        });

      //  invalidate(false);
    }

    private <T extends Number> void setAndBindTextFormatter(TextField textField, StringConverter<T> converter, Property<T> stateProperty, Property<T> axisBoundProperty) {
        final TextFormatter<T> formatter = new TextFormatter<T>(converter);
        //  stateProperty.bind(formatter.valueProperty());

        formatter.valueProperty().bindBidirectional(stateProperty);
        //   axisBoundProperty.bind(formatter.valueProperty());
        //  formatter.valueProperty().bind(axisBoundProperty);

//        formatter.valueProperty().bindBidirectional(stateProperty);

        axisBoundProperty.bindBidirectional(stateProperty);
        formatter.valueProperty().addListener((observable, o, v) -> {
            yAutoRange.setSelected(false);
        });
        textField.setTextFormatter(formatter);
    }

    public void invalidate(boolean saveToHistory) {

        logger.trace(() -> "Refreshing chart");
        XYChartSelection<ZonedDateTime, Number> currentSelection = currentState.asSelection();
        logger.debug(() -> "currentSelection=" + (currentSelection == null ? "null" : currentSelection.toString()));
        //  logger.debug(() -> "previousState=" + (previousState == null ? "null" : previousState.toString()));

        if (currentSelection.equals(previousState)) {
            logger.debug(() -> "State hasn't change, no need to redraw the graph");
            return;
        }

        if (saveToHistory) {
            this.history.push(previousState);
         //   previousState = currentSelection;
        }
          previousState = currentState.asSelection();
        logger.debug(() -> history.dump());
        plotChart(currentSelection);
    }


    public XYChartCrosshair<ZonedDateTime, Number> getCrossHair() {
        return crossHair;
    }

    public AreaChart<ZonedDateTime, Number> getChart() {
        return chart;
    }

    private void restoreSelectionFromHistory() {
        if (!history.isEmpty()) {
            XYChartSelection<ZonedDateTime, Number> state = history.pop();
            logger.debug(()-> "Restoring selection from history: " + (state != null ? state.toString() : "null"));
            currentState.setSelection(state, false);
        }
        else {
            logger.debug(() -> "History is empty: nothing to go back to.");
        }
    }

    private void plotChart(XYChartSelection<ZonedDateTime, Number> currentSelection) {
        try (Profiler p = Profiler.start("Plotting chart")) {
            chart.getData().clear();
            Map<String, XYChart.Series<ZonedDateTime, Number>> series = getRawData(
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
    }

    private Map<String, XYChart.Series<ZonedDateTime, Number>> getRawData(String jrdsHost, String target, String probe, Instant begin, Instant end) throws IOException, ParseException {
        JRDSDataProvider dp = new JRDSDataProvider(jrdsHost);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (dp.getData(target, probe, begin, end, out)) {
                TimeSeriesBuilder timeSeriesBuilder = new TimeSeriesBuilder(currentZoneId);
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                return timeSeriesBuilder.fromCSV(in)
                      //  .transform(new LargestTriangleThreeBucketsTransform(1000))
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
        private Stack<XYChartSelection<ZonedDateTime, Number>> stack = new Stack<>();
        public SimpleBooleanProperty emptyStackProperty = new SimpleBooleanProperty(true);

        public XYChartSelection<ZonedDateTime, Number> push(XYChartSelection<ZonedDateTime, Number> state) {
            if (state == null){
                logger.warn(()-> "Trying to push null state into history");
                return null;
            }
            else {
                emptyStackProperty.set(false);
                return this.stack.push(state);
            }
        }

        public XYChartSelection<ZonedDateTime, Number> pop() {
            XYChartSelection<ZonedDateTime, Number> r = this.stack.pop();
            emptyStackProperty.set(stack.isEmpty());
            return r;
        }

        public boolean isEmpty() {
            return emptyStackProperty.get();
        }


        public String dump() {
            final StringBuilder sb = new StringBuilder("History:");
            AtomicInteger pos = new AtomicInteger(0);
            if (this.isEmpty()){
                sb.append(" { empty }");
            }
            else{
                stack.forEach(h-> {
                    sb.append("\n" + pos.incrementAndGet() + " ->").append(h.toString());
                } );
            }

            return sb.toString();
        }
    }

    /**
     * Created by FTT2 on 07/12/2016.
     */
    public class State {

        private final SimpleObjectProperty<ZonedDateTime> startX;
        private final SimpleObjectProperty<ZonedDateTime> endX;
        private final SimpleDoubleProperty startY;
        private final SimpleDoubleProperty endY;

        private boolean frozen;

     //   private final SimpleObjectProperty<State> state = new SimpleObjectProperty<>();

        public XYChartSelection<ZonedDateTime, Number> asSelection() {
            return new XYChartSelection<>(
                    startX.get(),
                    endX.get(),
                    startY.get(),
                    endY.get()
            );
        }

        public void setSelection(XYChartSelection<ZonedDateTime, Number> selection, boolean toHistory) {
            frozen = true;
            try {
                this.startX.set(roundDateTime(selection.getStartX()));
                this.endX.set(roundDateTime(selection.getEndX()));
                this.startY.set(roundYValue(selection.getStartY().doubleValue()));
                this.endY.set(roundYValue(selection.getEndY().doubleValue()));
                invalidate(toHistory);
            }
            finally {
                frozen = false;
            }
        }



        public State(ZonedDateTime startX, ZonedDateTime endX, double startY, double endY) {
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


        private ZonedDateTime roundDateTime(ZonedDateTime zdt){
            return  ZonedDateTime.of(zdt.getYear(),
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
            return String.format("State{startX=%s, endX=%s, startY=%s, endY=%s}", startX.get().toString(), endX.get().toString(), startY.get(), endY.get());
        }

    }
}
