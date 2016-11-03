package eu.fthevenet.binjr.viewer;

import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.JRDSDataProvider;
import eu.fthevenet.binjr.data.TimeSeriesBuilder;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import jfxtras.scene.control.CalendarTextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.fthevenet.binjr.commons.charts.ChartCrossHairManager;
import org.gillius.jfxutils.chart.XYChartInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class MainViewController implements Initializable {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);

    @FXML
    public VBox root;

    @FXML
    public AnchorPane chartParent;

    @FXML
    public CalendarTextField beginDateTime;
    @FXML
    public CalendarTextField endDateTime;
    @FXML
    private AreaChart<Date, Number> chart;
    @FXML
    private Menu editMenu;
    @FXML
    private MenuItem editRefresh;
    XYChartInfo chartInfo;
    private boolean dragging;
    private boolean wasYAnimated;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert chart != null : "fx:id\"chart\" was not injected!";
        assert editMenu != null : "fx:id\"editMenu\" was not injected!";
        assert beginDateTime != null : "fx:id\"beginDateTime\" was not injected!";
        assert endDateTime != null : "fx:id\"endDateTime\" was not injected!";
        assert chartParent != null : "fx:id\"chartParent\" was not injected!";
        assert root != null : "fx:id\"root\" was not injected!";


        Instant end =  Instant.parse("2016-10-25T18:30:00Z");
        Instant begin = end.minus(6*60, ChronoUnit.MINUTES);
        beginDateTime.setCalendar(Calendar.getInstance());
        beginDateTime.getCalendar().setTime(Date.from(begin));
        endDateTime.setCalendar(Calendar.getInstance());
        endDateTime.getCalendar().setTime(Date.from(end));



        editRefresh.setOnAction(a -> refreshChart());

        chart.getYAxis().setAutoRanging(false);

        this.refreshChart();





        ChartCrossHairManager<Date, Number> crossHair = new ChartCrossHairManager<>(chart, chartParent, Date::toString, Object::toString);


    }

    private void refreshChart() {
        try (Profiler p = Profiler.start("Refreshing chart view")) {
            chart.getData().clear();
            Map<String, XYChart.Series<Date, Number>> series = getRawData();
//            chart.getData().addAll(
////                    series.get("InterruptTime"),
//                    series.get("DPCTime"),
//                    series.get("PrivilegedTime"),
//                    series.get("UserTime"),
//                    series.get("IdleTime"));
            //  chart.getData().add(series.get("InterruptTime"));
            //  chart.getData().add(series.get("DPCTime"));

            chart.getData().add(series.get("PrivilegedTime"));
            chart.getData().add(series.get("ProcessorTime"));

            chart.getData().add(series.get("FreeVirtualMemory"));
            chart.getData().add(series.get("TotalVirtualMemory"));
//            );
        } catch (IOException e) {
            logger.error(() -> "Error getting data", e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, XYChart.Series<Date, Number>> getRawData() throws IOException {

        String jrdsHost = "ngwps006:31001";
        String target = "ngwps006.mshome.net";
        String probe = "memprocPdh";
        String fileName = "e:\\temp\\test.csv";
        Instant end = endDateTime.getCalendar().getTime().toInstant();
        Instant begin = beginDateTime.getCalendar().getTime().toInstant();

        JRDSDataProvider dp = new JRDSDataProvider(jrdsHost);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (dp.getData(target, probe, begin, end, out)) {
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                return TimeSeriesBuilder.fromCSV(in);
            }
            else {
                throw new IOException(String.format("Failed to retrieve data from JRDS for %s %s %s %s", target, probe, begin.toString(), end.toString()));
            }
        }
    }
}
