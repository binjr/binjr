package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.commons.charts.ChartCrosshairManager;
import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.providers.JRDSDataProvider;
import eu.fthevenet.binjr.data.timeseries.TimeSeriesBuilder;
import eu.fthevenet.binjr.data.timeseries.transform.LargestTriangleThreeBucketsTransform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    @FXML
    public CalendarTextField endDateTime;



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


    private Property<String> currentHost = new SimpleStringProperty("ngwps006:31001/perf-ui");
    private Property<String> currentTarget = new SimpleStringProperty("ngwps006.mshome.net");
    private Property<String> currentProbe = new SimpleStringProperty("memprocPdh");
    private Map<String, Boolean> selectedSeriesCache = new HashMap<>();
   private ChartCrosshairManager<Date, Number> crossHair;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert chart != null : "fx:id\"chart\" was not injected!";
        assert beginDateTime != null : "fx:id\"beginDateTime\" was not injected!";
        assert endDateTime != null : "fx:id\"endDateTime\" was not injected!";
        assert chartParent != null : "fx:id\"chartParent\" was not injected!";
        assert yAutoRange != null : "fx:id\"yAutoRange\" was not injected!";
        assert yMinRange != null : "fx:id\"yMinRange\" was not injected!";
        assert yMaxRange != null : "fx:id\"yMaxRange\" was not injected!";
        assert seriesList != null : "fx:id\"seriesList\" was not injected!";

        Instant end = Instant.now();// Instant.parse("2016-10-25T22:00:00Z");
        Instant begin = end.minus(12 * 60, ChronoUnit.MINUTES);
        beginDateTime.setCalendar(Calendar.getInstance());
        beginDateTime.getCalendar().setTime(Date.from(begin));
        endDateTime.setCalendar(Calendar.getInstance());
        endDateTime.getCalendar().setTime(Date.from(end));

        beginDateTime.textProperty().addListener((observable, oldValue, newValue) -> refreshChart());
        endDateTime.textProperty().addListener((observable, oldValue, newValue) -> refreshChart());

        seriesList.setCellFactory(CheckBoxListCell.forListView(SelectableListItem::selectedProperty));

        crossHair = new ChartCrosshairManager<>(chart, chartParent, Date::toString, (n) -> String.format("%,.2f", n.doubleValue()));

        chart.getYAxis().autoRangingProperty().bindBidirectional(yAutoRange.selectedProperty());
        setAndBingTextFormatter(yMinRange, new NumberStringConverter(), ((ValueAxis<Number>) chart.getYAxis()).lowerBoundProperty(), (o, oldval, newVal) -> refreshChart());
        setAndBingTextFormatter(yMaxRange, new NumberStringConverter(), ((ValueAxis<Number>) chart.getYAxis()).upperBoundProperty(), (o, oldval, newVal) -> refreshChart());

        this.refreshChart();
    }

    private <T extends Number> void setAndBingTextFormatter(TextField textField, StringConverter<T> converter, Property<T> property, ChangeListener<? super T> listener) {
        final TextFormatter<T> formatter = new TextFormatter<T>(converter);
        formatter.valueProperty().bindBidirectional(property);
        formatter.valueProperty().addListener(listener);
        textField.setTextFormatter(formatter);
    }

    public void invalidate(){
        this.refreshChart();
    }

    private void refreshChart() {
        try (Profiler p = Profiler.start("Refreshing chart view")) {
            chart.getData().clear();
            Map<String, XYChart.Series<Date, Number>> series = getRawData(
                    currentHost.getValue(),
                    currentTarget.getValue(),
                    currentProbe.getValue(),
                    beginDateTime.getCalendar().getTime().toInstant(),
                    endDateTime.getCalendar().getTime().toInstant());
            chart.getData().addAll(series.values());
            seriesList.getItems().clear();
            for (XYChart.Series s : chart.getData()) {
                SelectableListItem i = new SelectableListItem(s.getName(), true);

                i.selectedProperty().addListener((obs, wasOn, isNowOn) -> {
                    logger.trace(i.getName() + " changed on state from " + wasOn + " to " + isNowOn);
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

    public ChartCrosshairManager<Date, Number> getCrossHair() {
        return crossHair;
    }

    public AreaChart<Date, Number> getChart() {
        return chart;
    }
}
