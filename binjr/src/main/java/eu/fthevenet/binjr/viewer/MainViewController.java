package eu.fthevenet.binjr.viewer;

import eu.fthevenet.binjr.commons.logging.Profiler;
import eu.fthevenet.binjr.data.JRDSDataProvider;
import eu.fthevenet.binjr.data.TimeSeriesBuilder;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import jfxtras.scene.control.CalendarTextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gillius.jfxutils.chart.DefaultChartInputContext;
import org.gillius.jfxutils.chart.XYChartInfo;

import java.awt.*;
import java.awt.geom.Arc2D;
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

        chartInfo = new XYChartInfo(chart, chart);

        final javafx.scene.control.Label xLabel = new javafx.scene.control.Label("");
        xLabel.getStyleClass().addAll("default-color3", "chart-line-symbol", "chart-series-line");
        xLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold;");
        xLabel.setTextFill(javafx.scene.paint.Color.DARKGRAY);
        xLabel.setMinSize(javafx.scene.control.Label.USE_PREF_SIZE, javafx.scene.control.Label.USE_PREF_SIZE);

        final javafx.scene.control.Label yLabel = new javafx.scene.control.Label("");
        yLabel.getStyleClass().addAll("default-color3", "chart-line-symbol", "chart-series-line");
        yLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold;");
        yLabel.setTextFill(javafx.scene.paint.Color.DARKGRAY);
        yLabel.setMinSize(javafx.scene.control.Label.USE_PREF_SIZE, javafx.scene.control.Label.USE_PREF_SIZE);


        Instant end = Instant.now();// Instant.parse("2016-10-24T10:15:30Z");
        Instant begin = end.minus(60, ChronoUnit.MINUTES);
        beginDateTime.setCalendar(Calendar.getInstance());
        beginDateTime.getCalendar().setTime(Date.from(begin));
        endDateTime.setCalendar(Calendar.getInstance());
        final Line horizontalLine = new Line(0, 0, 0, 0);
        final Line verticalLine = new Line(0, 0, 0, 0);
//        verticalLine.setStrokeWidth(0.5);
//        horizontalLine.setStrokeWidth(0.5);
        horizontalLine.setVisible(false);
        verticalLine.setVisible(false);
        yLabel.setVisible(false);
        xLabel.setVisible(false);

        chartParent.getChildren().addAll(xLabel, yLabel);
        chartParent.getChildren().addAll(horizontalLine, verticalLine);

        editRefresh.setOnAction(a -> refreshChart());

      //  chart.getYAxis().setAutoRanging(false);

        this.refreshChart();



        final javafx.scene.shape.Rectangle selection = new Rectangle(0, 0, 0, chart.getYAxis().getHeight());





//        chart.getParent().add(horizontalLine);
//        chart.add(verticalLine);
//
//        crosshairArea.setOnMouseMoved(event -> {
//            label.setVisible(true);
//            label.setText(" Location: "+event.getX()+", "+event.getY());
//
//            horizontalLine.setStartY(event.getY());
//            horizontalLine.setEndY(event.getY());
//
//            verticalLine.setStartX(event.getX());
//            verticalLine.setEndX(event.getX());
//
//        });
//        crosshairArea.setOnMouseExited(event -> label.setVisible(false));


//            SimpleDateFormat format = new SimpleDateFormat( "HH:mm:ss" );
//            format.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
//            ((StableTicksAxis) chart.getXAxis()).setAxisTickFormatter(
//                    new FixedFormatTickFormatter( format ) );

//        boolean ctrlPressed;
//        chart.setOnKeyPressed(event->{
//            if (event.getCode() == KeyCode.CONTROL){
//                verticalLine.setVisible(true);
//                label.setVisible(true);
//            }
//
//        });
//
//        chart.setOnKeyReleased(event->{
//            if (event.getCode() == KeyCode.CONTROL){
//                verticalLine.setVisible(false);
//                label.setVisible(false);
//            }
//        });

        root.addEventFilter(KeyEvent.KEY_RELEASED, (event) -> {
            if (event.getCode() == KeyCode.CONTROL) {
                horizontalLine.setVisible(false);
                verticalLine.setVisible(false);

                yLabel.setVisible(false);
                xLabel.setVisible(false);
                event.consume();
            }
        });

        root.addEventFilter(KeyEvent.KEY_PRESSED, (event) -> {
            if (event.getCode() == KeyCode.CONTROL) {
                logger.debug(()-> event.getTarget().toString());
                horizontalLine.setVisible(true);
                verticalLine.setVisible(true);
                yLabel.setVisible(true);
                xLabel.setVisible(true);
                event.consume();
            }
        });


        chart.setOnMouseMoved(mouseEvent -> {


//                verticalLine.setVisible(true);
//                label.setVisible(true);
                if (mouseEvent.isShortcutDown() &&chartInfo.isInPlotArea(mouseEvent.getX(), mouseEvent.getY())) {

                    double yStart = chart.getYAxis().getLocalToParentTransform().getTy();
                    double axisYRelativeMousePosition = mouseEvent.getY() - yStart*1.5;

                    logger.info("axisYRelativeMousePosition = " + axisYRelativeMousePosition);
                    logger.info("yStart = " + yStart);
                    logger.info("mouseEvent.getY = " + mouseEvent.getY());

                    logger.info("chart.getYAxis().getValueForDisplay(axisYRelativeMousePosition) = " + chart.getYAxis().getValueForDisplay(axisYRelativeMousePosition));
                    logger.info("chart.getYAxis().getValueForDisplay(mouseEvent.getY()) = " + chart.getYAxis().getValueForDisplay(mouseEvent.getY()));

                    horizontalLine.setStartX(chartInfo.getPlotArea().getMinX());
                    horizontalLine.setEndX(chartInfo.getPlotArea().getMinX()+5);
                    horizontalLine.setStartY(mouseEvent.getY());
                    horizontalLine.setEndY(mouseEvent.getY());
                    yLabel.setLayoutX(Math.max( chart.getLayoutX(),chartInfo.getPlotArea().getMinX() - yLabel.getWidth() - 2));
                    yLabel.setLayoutY( Math.min(mouseEvent.getY(),chartInfo.getPlotArea().getMaxY() - yLabel.getHeight()));
                    yLabel.setText(String.format("%.2f", chart.getYAxis().getValueForDisplay(axisYRelativeMousePosition).doubleValue()));


                    double xStart = chart.getXAxis().getLocalToParentTransform().getTx();
                    double axisXRelativeMousePosition = mouseEvent.getX() - xStart;
                    verticalLine.setStartX(mouseEvent.getX());
                    verticalLine.setEndX(mouseEvent.getX());
                    verticalLine.setStartY(chartInfo.getPlotArea().getMaxY() - 5);
                    verticalLine.setEndY(chartInfo.getPlotArea().getMaxY());
                    xLabel.setLayoutY(chartInfo.getPlotArea().getMaxY() + 2);
                    xLabel.setLayoutX( Math.min(mouseEvent.getX(),chartInfo.getPlotArea().getMaxX() - xLabel.getWidth()));
                    xLabel.setText(chart.getXAxis().getValueForDisplay(axisXRelativeMousePosition).toString());

                }

            }

        );

        SimpleDoubleProperty rectinitX = new SimpleDoubleProperty();

//        EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//
//                if (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED) {
//                    rectinitX.set(mouseEvent.getX());
//                } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED || mouseEvent.getEventType() == MouseEvent.MOUSE_MOVED) {
//                    LineChart<Number, Number> lineChart = (LineChart<Number, Number>) pane.getCenter();
//                    NumberAxis xAxis = (NumberAxis) lineChart.getXAxis();
//
//                    double newXlower = xAxis.getLowerBound(), newXupper = xAxis.getUpperBound();
//                    double Delta = 0.3;
//
//                    if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
//                        if (rectinitX.get() < mouseEvent.getX()) {
//                            Delta *= -1;
//                        }
//                        newXlower = xAxis.getLowerBound() + Delta;
//                        newXupper = xAxis.getUpperBound() + Delta;
//
//                        xAxis.setLowerBound(newXlower);
//                        xAxis.setUpperBound(newXupper);
//
//                        DoubleProperty p1 = xAxis.scaleXProperty();
//                        DoubleProperty p2 = xAxis.translateXProperty();
//
//                        double horizontalValueRange = xAxis.getUpperBound() - xAxis.getLowerBound();
//                        double horizontalWidthPixels = xAxis.getWidth();
//                        //pixels per unit
//                        double xScale = horizontalWidthPixels / horizontalValueRange;
//
//                        Set<Node> nodes = lineChart.lookupAll(".chart-vertical-grid-lines");
//                        for (Node n: nodes) {
//                            Path p = (Path) n;
//                            double currLayoutX = p.getLayoutX();
//                            p.setLayoutX(currLayoutX + (Delta*-1) * xScale);
//                        }
//                        double lox = xAxis.getLayoutX();
//                    }
//                    rectinitX.set(mouseEvent.getX());
//                }
//            }
//        };


        //Panning works via either secondary (right) mouse or primary with ctrl held down
//        ChartPanManager panner = new ChartPanManager( chart );
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
//        JFXChartUtil.setupZooming( chart, mouseEvent -> {
//            if ( mouseEvent.getButton() != MouseButton.PRIMARY ||
//                    mouseEvent.isShortcutDown() )
//                mouseEvent.consume();
//        });
        //       JFXChartUtil.addDoublePrimaryClickAutoRangeHandler( chart );

    }
//
//    private void startDrag( MouseEvent event ) {
//        DefaultChartInputContext context = new DefaultChartInputContext( chartInfo, event.getX(), event.getY() );
//
//           lastY = event.getY();
//
//            wasYAnimated = chart.getYAxis().getAnimated();
//
//        chart.getYAxis().setAnimated( false );
//        chart.getYAxis().setAutoRanging( false );
//
//            dragging = true;
//        }
//    }
//
//    private void drag( MouseEvent event ) {
//        if ( !dragging )
//            return;
//
//        if ( panMode == AxisConstraint.Both || panMode == AxisConstraint.Horizontal ) {
//            double dX = ( event.getX() - lastX ) / -xAxis.getScale();
//            lastX = event.getX();
//            xAxis.setAutoRanging( false );
//            xAxis.setLowerBound( xAxis.getLowerBound() + dX );
//            xAxis.setUpperBound( xAxis.getUpperBound() + dX );
//        }
//
//        if ( panMode == AxisConstraint.Both || panMode == AxisConstraint.Vertical ) {
//            double dY = ( event.getY() - lastY ) / -yAxis.getScale();
//            lastY = event.getY();
//            yAxis.setAutoRanging( false );
//            yAxis.setLowerBound( yAxis.getLowerBound() + dY );
//            yAxis.setUpperBound( yAxis.getUpperBound() + dY );
//        }
//    }
//
//    private void release() {
//        if ( !dragging )
//            return;
//
//        dragging = false;
//
//        xAxis.setAnimated( wasXAnimated );
//        yAxis.setAnimated( wasYAnimated );
//    }
//
    private void showLabel(Date value) {

    }

    private javafx.scene.control.Label createDataThresholdLabel(int value) {
        final javafx.scene.control.Label label = new javafx.scene.control.Label(value + "");
        label.getStyleClass().addAll("default-color0", "chart-line-symbol", "chart-series-line");
        label.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");

        label.setTextFill(javafx.scene.paint.Color.DARKGRAY);

        label.setMinSize(javafx.scene.control.Label.USE_PREF_SIZE, javafx.scene.control.Label.USE_PREF_SIZE);
        return label;
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
            chart.getData().add(series.get("InterruptTime"));
            chart.getData().add(series.get("DPCTime"));
            chart.getData().add(series.get("PrivilegedTime"));
            chart.getData().add(series.get("ProcessorTime"));
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
