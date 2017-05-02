package eu.fthevenet.util.ui.charts;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
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
 * Draws a crosshair on top of an {@link XYChart} and handles selection of a portion of the chart view.
 *
 * @author Frederic Thevenet
 */
public class XYChartCrosshair<X, Y> {
    private static final Logger logger = LogManager.getLogger(XYChartCrosshair.class);
    private static final double SELECTION_OPACITY = 0.5;

    private final Line horizontalMarker = new Line();
    private final Line verticalMarker = new Line();
    private final Label xAxisLabel;
    private final Label yAxisLabel;
    private final XYChart<X, Y> chart;
    private final Function<X, String> xValuesFormatter;
    private final Function<Y, String> yValuesFormatter;
    private final XYChartInfo chartInfo;
    private final BooleanProperty isSelecting = new SimpleBooleanProperty(false);
    private Point2D selectionStart = new Point2D(-1, -1);
    private Point2D mousePosition = new Point2D(-1, -1);
    private final Rectangle selection = new Rectangle(0, 0, 0, 0);
    private final BooleanProperty verticalMarkerVisible = new SimpleBooleanProperty();
    private final BooleanProperty horizontalMarkerVisible = new SimpleBooleanProperty();
    private Consumer<XYChartSelection<X, Y>> selectionDoneEvent;

    /**
     * Initializes a new instance of the {@link XYChartCrosshair} class.
     *
     * @param chart            the {@link XYChart} to attach the crosshair to.
     * @param parent           the parent node of the chart
     * @param xValuesFormatter a function used to format the display of X values as strings
     * @param yValuesFormatter a function used to format the display of Y values as strings
     */
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
            if (!oldValue && newValue) {
                selectionStart = new Point2D(verticalMarker.getStartX(), horizontalMarker.getStartY());
            }
            drawSelection();
            selection.setVisible(newValue);
        });

        horizontalMarkerVisible.addListener((observable, oldValue, newValue) -> {
            drawHorizontalMarker();
            horizontalMarker.setVisible(newValue);
            yAxisLabel.setVisible(newValue);
            if (!newValue && !verticalMarkerVisible.get()) {
                isSelecting.set(false);
            }
        });

        verticalMarkerVisible.addListener((observable, oldValue, newValue) -> {
            drawVerticalMarker();
            verticalMarker.setVisible(newValue);
            xAxisLabel.setVisible(newValue);
            if (!newValue && !horizontalMarkerVisible.get()) {
                isSelecting.set(false);
            }
        });

        chart.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            focusState(newValue);
        });
    }

    /**
     * Gets the boolean property that tracks the visibility of the vertical marker of the crosshair
     *
     * @return the boolean property that tracks the visibility of the vertical marker of the crosshair
     */
    public BooleanProperty verticalMarkerVisibleProperty() {
        return verticalMarkerVisible;
    }

    /**
     * Gets the boolean property that tracks the visibility of the horizontal marker of the crosshair
     *
     * @return the boolean property that tracks the visibility of the horizontal marker of the crosshair
     */
    public BooleanProperty horizontalMarkerVisibleProperty() {
        return horizontalMarkerVisible;
    }

    /**
     * Returns true if the vertical marker is visible, false otherwise
     *
     * @return true if the vertical marker is visible, false otherwise
     */
    public boolean isVerticalMarkerVisible() {
        return verticalMarkerVisible.get();
    }

    /**
     * Returns true if the horizontal marker is visible, false otherwise
     *
     * @return true if the horizontal marker is visible, false otherwise
     */
    public boolean isHorizontalMarkerVisible() {
        return horizontalMarkerVisible.get();
    }

    /**
     * Sets the visibility of the  vertical marker
     *
     * @param verticalMarkerVisible the visibility of the  vertical marker
     */
    public void setVerticalMarkerVisible(boolean verticalMarkerVisible) {
        this.verticalMarkerVisible.set(verticalMarkerVisible);
    }

    /**
     * Sets the visibility of the  horizontal marker
     *
     * @param horizontalMarkerVisible the visibility of the  horizontal marker
     */
    public void setHorizontalMarkerVisible(boolean horizontalMarkerVisible) {
        this.horizontalMarkerVisible.set(horizontalMarkerVisible);
    }

    /**
     * Sets the action to be triggered when selection is complete
     *
     * @param action the action to be triggered when selection is complete
     */
    public void onSelectionDone(Consumer<XYChartSelection<X, Y>> action) {
        selectionDoneEvent = action;
    }

    private void fireSelectionDoneEvent() {
        if (selectionDoneEvent != null && (selection.getWidth() > 0 && selection.getHeight() > 0)) {
            selectionDoneEvent.accept(
                    new XYChartSelection<X, Y>(
                            getValueFromXcoord(selection.getX() - 0.5),
                            getValueFromXcoord(selection.getX() + selection.getWidth() - 0.5),
                            getValueFromYcoord(selection.getY() + selection.getHeight() - 0.5),
                            getValueFromYcoord(selection.getY() - 0.5)
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
    }

    private void handleMouseMoved(MouseEvent event) {
        Rectangle2D area = chartInfo.getPlotArea();
        mousePosition = new Point2D(Math.max(area.getMinX(), Math.min(area.getMaxX(), event.getX())), Math.max(area.getMinY(), Math.min(area.getMaxY(), event.getY())));
        if (horizontalMarkerVisible.get()) {
            drawHorizontalMarker();
        }
        if (verticalMarkerVisible.get()) {
            drawVerticalMarker();
        }
        if (event.isPrimaryButtonDown() && (verticalMarkerVisible.get() || horizontalMarkerVisible.get())) {
            isSelecting.set(true);
            drawSelection();
        }
    }

    private void drawSelection() {
        if (selectionStart.getX() < 0 || selectionStart.getY() < 0) {
            return;
        }
        if (horizontalMarkerVisible.get()) {
            double height = horizontalMarker.getStartY() - selectionStart.getY();
            selection.setY(height < 0 ? horizontalMarker.getStartY() : selectionStart.getY());
            selection.setHeight(Math.abs(height));
        }
        else {
            selection.setY(verticalMarker.getStartY());
            selection.setHeight(verticalMarker.getEndY() - verticalMarker.getStartY());
        }
        if (verticalMarkerVisible.get()) {
            double width = verticalMarker.getStartX() - selectionStart.getX();
            selection.setX(width < 0 ? verticalMarker.getStartX() : selectionStart.getX());
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
}
