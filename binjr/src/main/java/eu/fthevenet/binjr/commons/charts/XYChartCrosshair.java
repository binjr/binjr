package eu.fthevenet.binjr.commons.charts;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gillius.jfxutils.chart.XYChartInfo;

import java.util.function.Consumer;
import java.util.function.Function;


/**
 * @author Frederic Thevenet
 */
public class XYChartCrosshair<X, Y> {
    private static final Logger logger = LogManager.getLogger(XYChartCrosshair.class);
    public static final double SELECTION_OPACITY = 0.5;
    final private Line horizontalMarker = new Line();
    final private Line verticalMarker = new Line();
    final private Label xAxisLabel;
    final private Label yAxisLabel;
    final private XYChart<X, Y> chart;
    private final Function<X, String> xValuesFormatter;
    private final Function<Y, String> yValuesFormatter;
    private final XYChartInfo chartInfo;
    SimpleBooleanProperty isSelecting = new SimpleBooleanProperty(false);
    private Point2D selectionStart = new Point2D(-1, -1);
    private Point2D mousePosition = new Point2D(-1, -1);
    private Rectangle selection = new Rectangle(0, 0, 0, 0);
    private SimpleBooleanProperty showVerticalMarker = new SimpleBooleanProperty();

    public boolean isShowVerticalMarker() {
        return showVerticalMarker.get();
    }

    public boolean isShowHorizontalMarker() {
        return showHorizontalMarker.get();
    }

    private SimpleBooleanProperty showHorizontalMarker = new SimpleBooleanProperty();
    private Consumer<XYChartSelection<X,Y>> selectionDoneEvent;
    private boolean selecting;

    //private Event onDoneSelecting =new Event()

    public XYChartCrosshair(XYChart<X, Y> chart, Pane parent, Function<X, String> xValuesFormatter, Function<Y, String> yValuesFormatter) {
        this.chart = chart;
        applyStyle(this.verticalMarker);
        applyStyle(this.horizontalMarker);
        applyStyle(this.selection);
        this.xAxisLabel = newAxisLabel();
        this.yAxisLabel = newAxisLabel();
        parent.getChildren().addAll(xAxisLabel, yAxisLabel, verticalMarker, horizontalMarker, selection);
        this.xValuesFormatter = xValuesFormatter;
        this.yValuesFormatter = yValuesFormatter;
        this.chartInfo = new XYChartInfo(this.chart);
        this.chart.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        this.chart.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseMoved);
        this.chart.setOnMousePressed(e -> {
                    if (showHorizontalMarker.get() || showVerticalMarker.get()) {
                        selectionStart = new Point2D(mousePosition.getX(), mousePosition.getY());
                    }
                }
        );

        this.chart.setOnMouseReleased(e -> {
            if (isSelecting.get()) {
                fireSelectionDoneEvent();
                drawVerticalMarker();
                drawHorizontalMarker();
            }
            isSelecting.set(false);
        });

        isSelecting.addListener((observable, oldValue, newValue) -> {
            logger.debug(() -> "observable=" + observable + " oldValue=" + oldValue + " newValue=" + newValue);
            drawSelection();
            selection.setVisible(newValue);
        });

        showHorizontalMarker.addListener((observable, oldValue, newValue) -> {
            drawHorizontalMarker();
            horizontalMarker.setVisible(newValue);
            yAxisLabel.setVisible(newValue);
            if (!newValue && !showVerticalMarker.get()) {
                isSelecting.set(false);
            }
        });

        showVerticalMarker.addListener((observable, oldValue, newValue) -> {
            drawVerticalMarker();
            verticalMarker.setVisible(newValue);
            xAxisLabel.setVisible(newValue);
            if (!newValue && !showHorizontalMarker.get()) {
                isSelecting.set(false);
            }
        });

        chart.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            focusState(newValue);
        });
    }

    public void onSelectionDone(Consumer<XYChartSelection<X,Y>> action) {
        selectionDoneEvent = action;
    }

    private void fireSelectionDoneEvent() {
        if (selectionDoneEvent != null && (selection.getWidth() > 0 && selection.getHeight() > 0)) {
            selectionDoneEvent.accept(
                    new Selection<X, Y>(
                            getValueFromXcoord(selection.getX()),
                            getValueFromXcoord(selection.getX() + selection.getWidth()),
                            getValueFromYcoord(selection.getY()),
                            getValueFromYcoord(selection.getY() + selection.getHeight())
                    )
            );
        }
    }

    private void focusState(boolean value) {
        if (value) {
            logger.info("Focus Gained");
        }
        else {
            logger.info("Focus Lost");
        }
    }

    private void drawHorizontalMarker() {
        if (mousePosition.getY() < 0) {
            return;
        }
        horizontalMarker.setStartX(chartInfo.getPlotArea().getMinX() + 0.5);
        horizontalMarker.setEndX(chartInfo.getPlotArea().getMaxX() + 0.5);
        horizontalMarker.setStartY(mousePosition.getY() + 0.5);
        horizontalMarker.setEndY(mousePosition.getY() + 0.5);
        yAxisLabel.setLayoutX(Math.max(chart.getLayoutX(), chartInfo.getPlotArea().getMinX() - yAxisLabel.getWidth() - 2));
        yAxisLabel.setLayoutY(Math.min(mousePosition.getY(), chartInfo.getPlotArea().getMaxY() - yAxisLabel.getHeight()));
        yAxisLabel.setText(yValuesFormatter.apply(getValueFromYcoord(mousePosition.getY())));
    }

    private Y getValueFromYcoord(double yPosition) {
        double yStart = chart.getYAxis().getLocalToParentTransform().getTy();
        double axisYRelativePosition = yPosition - yStart * 1.5;
        return chart.getYAxis().getValueForDisplay(axisYRelativePosition);
    }

    private X getValueFromXcoord(double xPosition) {
        double xStart = chart.getXAxis().getLocalToParentTransform().getTx();
        double axisXRelativeMousePosition = xPosition - xStart;
        return chart.getXAxis().getValueForDisplay(axisXRelativeMousePosition - 5);
    }

    private void drawVerticalMarker() {
        if (mousePosition.getX() < 0) {
            return;
        }
        verticalMarker.setStartX(mousePosition.getX() + 0.5);
        verticalMarker.setEndX(mousePosition.getX() + 0.5);
        verticalMarker.setStartY(chartInfo.getPlotArea().getMinY() + 0.5);
        verticalMarker.setEndY(chartInfo.getPlotArea().getMaxY() + 0.5);
        xAxisLabel.setLayoutY(chartInfo.getPlotArea().getMaxY() + 4);
        xAxisLabel.setLayoutX(Math.min(mousePosition.getX(), chartInfo.getPlotArea().getMaxX() - xAxisLabel.getWidth()));
        xAxisLabel.setText(xValuesFormatter.apply(getValueFromXcoord(mousePosition.getX())));
        logger.trace(xAxisLabel::getText);
    }

    private void handleMouseMoved(MouseEvent event) {
        Rectangle2D area = chartInfo.getPlotArea();
        mousePosition = new Point2D(Math.max(area.getMinX(), Math.min(area.getMaxX(), event.getX())), Math.max(area.getMinY(), Math.min(area.getMaxY(), event.getY())));
        if (event.isShiftDown()) {
            drawHorizontalMarker();
        }
        if (event.isControlDown()) {
            drawVerticalMarker();
        }
        if (event.isPrimaryButtonDown() && (showVerticalMarker.get() || showHorizontalMarker.get())) {
            isSelecting.set(true);
            drawSelection();
        }

    }

    private void drawSelection() {
        if (selectionStart.getX() < 0 || selectionStart.getY() < 0) {
            return;
        }
        if (showHorizontalMarker.get()) {
            double height = mousePosition.getY() - (selectionStart.getY() - 1.0);
            selection.setY(height > 0 ? selectionStart.getY() : mousePosition.getY() + 1);
            selection.setHeight(Math.abs(height));
        }
        else {
            selection.setY(verticalMarker.getStartY());
            selection.setHeight(verticalMarker.getEndY() - verticalMarker.getStartY());
        }
        if (showVerticalMarker.get()) {
            double width = mousePosition.getX() - (selectionStart.getX() - 1.0);
            selection.setX(width > 0 ? selectionStart.getX() : mousePosition.getX() + 1);
            selection.setWidth(Math.abs(width));
        }
        else {
            selection.setX(horizontalMarker.getStartX());
            selection.setWidth(horizontalMarker.getEndX() - horizontalMarker.getStartX());
        }
    }

    private Label newAxisLabel() {
        Label label = new Label("");
        label.getStyleClass().addAll("default-color3", "chart-line-symbol", "chart-series-line");
        label.setStyle("-fx-font-size: 10; -fx-font-weight: bold;");
        label.setTextFill(javafx.scene.paint.Color.DARKGRAY);
        label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        label.setMouseTransparent(true);
        label.setVisible(false);
        return label;
    }


    private void applyStyle(Shape shape) {
        shape.setMouseTransparent(true);
        shape.setSmooth(false);
        shape.setStrokeWidth(1.0);
        shape.setVisible(false);
        shape.setStrokeType(StrokeType.CENTERED);
        shape.setStroke(Color.STEELBLUE);
        Color fillColor = Color.LIGHTSTEELBLUE;
        shape.setFill(new Color(
                fillColor.getRed(),
                fillColor.getGreen(),
                fillColor.getBlue(),
                SELECTION_OPACITY));
    }

    public SimpleBooleanProperty showVerticalMarkerProperty() {
        return showVerticalMarker;
    }

    public SimpleBooleanProperty showHorizontalMarkerProperty() {
        return showHorizontalMarker;
    }

    /**
     * Created by FTT2 on 07/12/2016.
     */
    public static class Selection<X, Y> implements XYChartSelection<X, Y> {
        private final X startX;
        private final X endX;
        private final Y startY;
        private final Y endY;


        @Override
        public X getStartX() {
            return startX;
        }

        @Override
        public X getEndX() {
            return endX;
        }

        @Override
        public Y getStartY() {
            return startY;
        }

        @Override
        public Y getEndY() {
            return endY;
        }

         public Selection(XYChartSelection<X,Y> selection){
            this.startX = selection.getStartX();
            this.endX = selection.getEndX();
            this.startY = selection.getStartY();
            this.endY = selection.getEndY();
        }

        public Selection(X startX, X endX, Y startY, Y endY) {
            this.startX = startX;
            this.endX = endX;
            this.startY = startY;
            this.endY = endY;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Selection{");
            sb.append("startX=").append(startX);
            sb.append(", endX=").append(endX);
            sb.append(", startY=").append(startY);
            sb.append(", endY=").append(endY);
            sb.append('}');
            return sb.toString();
        }


    }
}
