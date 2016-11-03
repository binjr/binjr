package eu.fthevenet.binjr.commons.charts;

import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gillius.jfxutils.chart.XYChartInfo;

import java.util.function.Function;


/**
 * @author Frederic Thevenet
 */
public class ChartCrossHairManager<X, Y> {
    private static final Logger logger = LogManager.getLogger(ChartCrossHairManager.class);
    final private Line horizontalLine;
    final private Line verticalLine;
    final private Label xAxisLabel;
    final private Label yAxisLabel;
    final private XYChart<X, Y> chart;
//    final private Axis<X> xAxis;
//    final private Axis<Y> yAxis;
    final private Pane parent;
    final Function<X, String> xValuesFormatter;
    final Function<Y, String> yValuesFormatter;
    final XYChartInfo chartInfo;


    public ChartCrossHairManager(XYChart<X, Y> chart, Pane parent, Function<X, String> xValuesFormatter, Function<Y, String> yValuesFormatter) {
        this.chart = chart;
        this.verticalLine = newCrossHairLine();
        this.horizontalLine = newCrossHairLine();
        this.xAxisLabel = newAxisLabel();
        this.yAxisLabel = newAxisLabel();
        this.parent = parent;
        this.parent.getChildren().addAll(xAxisLabel, yAxisLabel, verticalLine, horizontalLine);
        this.xValuesFormatter = xValuesFormatter;
        this.yValuesFormatter = yValuesFormatter;
        this.chartInfo = new XYChartInfo(this.chart);

        this.chart.addEventHandler( MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        this.chart.addEventHandler( KeyEvent.KEY_PRESSED, event -> handleControlKey(event, true));
        this.chart.addEventHandler( KeyEvent.KEY_RELEASED,  event -> handleControlKey(event, false));

        chart.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            focusState(newValue);
        });



    }
    private void focusState(boolean value) {
        if (value) {
            logger.info("Focus Gained");
        }
        else {
            logger.info("Focus Lost");
        }
    }

    private void handleControlKey(KeyEvent event, boolean pressed) {
        if (event.getCode() == KeyCode.CONTROL) {
                horizontalLine.setVisible(pressed);
                verticalLine.setVisible(pressed);
                yAxisLabel.setVisible(pressed);
                xAxisLabel.setVisible(pressed);
                event.consume();
            }
    }

    private void handleMouseMoved(MouseEvent event){
        if (event.isShortcutDown() && chartInfo.isInPlotArea(event.getX(), event.getY())) {
            double yStart = chart.getYAxis().getLocalToParentTransform().getTy();
            double axisYRelativeMousePosition = event.getY() - yStart * 1.5;
            horizontalLine.setStartX(chartInfo.getPlotArea().getMinX() + 0.5);
            horizontalLine.setEndX(chartInfo.getPlotArea().getMaxX() + 0.5);
            horizontalLine.setStartY(event.getY() + 0.5);
            horizontalLine.setEndY(event.getY() + 0.5);
            yAxisLabel.setLayoutX(Math.max(chart.getLayoutX(), chartInfo.getPlotArea().getMinX() - yAxisLabel.getWidth() - 2));
            yAxisLabel.setLayoutY(Math.min(event.getY(), chartInfo.getPlotArea().getMaxY() - yAxisLabel.getHeight()));
            yAxisLabel.setText(yValuesFormatter.apply(chart.getYAxis().getValueForDisplay(axisYRelativeMousePosition)));

            double xStart = chart.getXAxis().getLocalToParentTransform().getTx();
            double axisXRelativeMousePosition = event.getX() - xStart;
            verticalLine.setStartX(event.getX() + 0.5);
            verticalLine.setEndX(event.getX() + 0.5);
            verticalLine.setStartY(chartInfo.getPlotArea().getMinY() + 0.5);
            verticalLine.setEndY(chartInfo.getPlotArea().getMaxY() + 0.5);
            xAxisLabel.setLayoutY(chartInfo.getPlotArea().getMaxY() + 4);
            xAxisLabel.setLayoutX(Math.min(event.getX(), chartInfo.getPlotArea().getMaxX() - xAxisLabel.getWidth()));
            xAxisLabel.setText(xValuesFormatter.apply(chart.getXAxis().getValueForDisplay(axisXRelativeMousePosition)));
        }
    }



    private Label newAxisLabel() {
        Label label = new Label("");
        label.getStyleClass().addAll("default-color3", "chart-line-symbol", "chart-series-line");
        label.setStyle("-fx-font-size: 10; -fx-font-weight: bold;");
        label.setTextFill(javafx.scene.paint.Color.DARKGRAY);
        label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        label.setMouseTransparent(true);
        label.setVisible(true);
        return label;
    }

    private Line newCrossHairLine() {
        Line line = new Line();
        line.setMouseTransparent(true);
        line.setSmooth(false);
        line.setStrokeWidth(1.0);
        line.setVisible(true);
        line.setStrokeType(StrokeType.CENTERED);
        line.setStroke(Color.DARKGREY);
        return line;
    }
}
