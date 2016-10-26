package eu.fthevenet.binjr.viewer;

import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.JRDSDataProvider;
import eu.fthevenet.binjr.data.TimeSeriesBuilder;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import jfxtras.scene.control.CalendarTextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;

public class MainViewController implements Initializable{
private static final Logger logger = LogManager.getLogger(MainViewController.class);
    @FXML
    public CalendarTextField beginDateTime;
    @FXML
    public CalendarTextField endDateTime;
    @FXML
    private AreaChart<Date, Number> chartView;
    @FXML
    private Menu editMenu;
    @FXML
    private MenuItem editRefresh;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert chartView != null : "fx:id\"chartView\" was not injected!";
        assert editMenu != null :  "fx:id\"editMenu\" was not injected!";
        assert beginDateTime != null : "fx:id\"beginDateTime\" was not injected!";
        assert endDateTime != null :  "fx:id\"endDateTime\" was not injected!";



        Instant end = Instant.now();// Instant.parse("2016-10-24T10:15:30Z");
        Instant begin = end.minus(60, ChronoUnit.MINUTES);
        beginDateTime.setCalendar(Calendar.getInstance());
        beginDateTime.getCalendar().setTime(Date.from(begin));
        endDateTime.setCalendar(Calendar.getInstance());


        beginDateTime.setOnInputMethodTextChanged(event -> {
            refreshChart();
        });

        endDateTime.setOnInputMethodTextChanged(event -> {
            refreshChart();
        });

        editRefresh.setOnAction(a-> refreshChart());

      //  this.refreshChart();



//            SimpleDateFormat format = new SimpleDateFormat( "HH:mm:ss" );
//            format.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
//            ((StableTicksAxis) chartView.getXAxis()).setAxisTickFormatter(
//                    new FixedFormatTickFormatter( format ) );

//        chartView.setOnMouseMoved(mouseEvent -> {
//            double xStart = chartView.getXAxis().getLocalToParentTransform().getTx();
//            double axisXRelativeMousePosition = mouseEvent.getX() - xStart;
////            logger.info(String.format(
////                    "%d, %d (%d, %d); %d - %d",
////                    (int) mouseEvent.getSceneX(), (int) mouseEvent.getSceneY(),
////                    (int) mouseEvent.getX(), (int) mouseEvent.getY(),
////                    (int) xStart,
////                    chartView.getXAxis().getValueForDisplay( axisXRelativeMousePosition ).intValue()
////            ) );
//        });

        //Panning works via either secondary (right) mouse or primary with ctrl held down
//        ChartPanManager panner = new ChartPanManager( chartView );
//        panner.setMouseFilter(mouseEvent -> {
//            if ( mouseEvent.getButton() == MouseButton.SECONDARY ||
//                    ( mouseEvent.getButton() == MouseButton.PRIMARY &&
//                            mouseEvent.isShortcutDown() ) ) {
//                //let it through
//            } else {
//                mouseEvent.consume();
//            }
//        });
//        panner.start();

//        //Zooming works only via primary mouse button without ctrl held down
//        JFXChartUtil.setupZooming( chartView, mouseEvent -> {
//            if ( mouseEvent.getButton() != MouseButton.PRIMARY ||
//                    mouseEvent.isShortcutDown() )
//                mouseEvent.consume();
//        });
 //       JFXChartUtil.addDoublePrimaryClickAutoRangeHandler( chartView );

    }


    private void refreshChart(){
        try(Profiler p= Profiler.start("Refreshing chart view")) {
            chartView.getData().clear();
            Map<String, XYChart.Series<Date, Number>> series = getRawData();
//            chartView.getData().addAll(
////                    series.get("InterruptTime"),
//                    series.get("DPCTime"),
//                    series.get("PrivilegedTime"),
//                    series.get("UserTime"),
//                    series.get("IdleTime"));
            chartView.getData().add(series.get("InterruptTime"));
            chartView.getData().add(series.get("DPCTime"));
            chartView.getData().add(series.get("PrivilegedTime"));
            chartView.getData().add(series.get("ProcessorTime"));
//            );
        }
        catch (IOException e){
            logger.error(()-> "Error getting data", e);
            throw new RuntimeException(e);
        }
    }

    private Map<String,XYChart.Series<Date, Number>> getRawData() throws IOException {

        String jrdsHost = "ngwps006:31001";
        String target = "ngwps006.mshome.net";
        String probe = "memprocPdh";
        String fileName = "e:\\temp\\test.csv";
        Instant end = endDateTime.getCalendar().getTime().toInstant();
        Instant begin =  beginDateTime.getCalendar().getTime().toInstant();

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
