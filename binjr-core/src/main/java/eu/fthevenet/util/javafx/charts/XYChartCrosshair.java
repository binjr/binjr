/*
 *    Copyright 2016-2018 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.fthevenet.util.javafx.charts;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.gillius.jfxutils.JFXUtil.getXShift;
import static org.gillius.jfxutils.JFXUtil.getYShift;

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
    private final LinkedHashMap<XYChart<X, Y>, Function<Y, String>> charts;
    private final Function<X, String> xValuesFormatter;
    private final XYChartInfo chartInfo;
    private final BooleanProperty isSelecting = new SimpleBooleanProperty(false);
    private final Pane parent;
    private Point2D selectionStart = new Point2D(-1, -1);
    private Point2D mousePosition = new Point2D(-1, -1);
    private final Rectangle selection = new Rectangle(0, 0, 0, 0);
    private final BooleanProperty verticalMarkerVisible = new SimpleBooleanProperty();
    private final BooleanProperty horizontalMarkerVisible = new SimpleBooleanProperty();
    private Consumer<Map<XYChart<X, Y>, XYChartSelection<X, Y>>> selectionDoneEvent;
    private final Map<XYChart<X, Y>, Property<Y>> currentYValues = new HashMap<>();
    private final Property<X> currentXValue = new SimpleObjectProperty<>();
    private final XYChart<X, Y> masterChart;
    private final BooleanProperty isMouseOverChart = new SimpleBooleanProperty(false);


    /**
     * Initializes a new instance of the {@link XYChartCrosshair} class.
     *
     * @param charts           a map of the  {@link XYChart} to attach and their formatting function of the Y values.
     * @param parent           the parent node of the chart
     * @param xValuesFormatter a function used to format the display of X values as strings
     */
    public XYChartCrosshair(LinkedHashMap<XYChart<X, Y>, Function<Y, String>> charts, Pane parent, Function<X, String> xValuesFormatter) {
        this.charts = charts;
        applyStyle(this.verticalMarker);
        applyStyle(this.horizontalMarker);
        applyStyle(this.selection);
        this.xAxisLabel = newAxisLabel();
        this.yAxisLabel = newAxisLabel();
        this.parent = parent;
        parent.getChildren().addAll(xAxisLabel, yAxisLabel, verticalMarker, horizontalMarker, selection);
        this.xValuesFormatter = xValuesFormatter;
        masterChart = charts.keySet().stream().reduce((p, n) -> n).orElseThrow(() -> new IllegalStateException("Could not identify last element in chart linked hash map."));
        this.chartInfo = new XYChartInfo(masterChart, parent);
        masterChart.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        masterChart.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseMoved);
        masterChart.setOnMouseReleased(e -> {
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
            if (!newValue && !verticalMarkerVisible.get()) {
                isSelecting.set(false);
                currentYValues.forEach((key, value) -> value.setValue(null));
            }
        });
        verticalMarkerVisible.addListener((observable, oldValue, newValue) -> {
            drawVerticalMarker();
            if (!newValue && !horizontalMarkerVisible.get()) {
                isSelecting.set(false);
                currentXValue.setValue(null);
            }
        });
        masterChart.setOnMouseExited(event -> isMouseOverChart.set(false));
        masterChart.setOnMouseEntered(event -> isMouseOverChart.set(true));
        horizontalMarker.visibleProperty().bind(horizontalMarkerVisible.and(isMouseOverChart));
        yAxisLabel.visibleProperty().bind(horizontalMarkerVisible.and(isMouseOverChart));
        verticalMarker.visibleProperty().bind(verticalMarkerVisible.and(isMouseOverChart));
        xAxisLabel.visibleProperty().bind(verticalMarkerVisible.and(isMouseOverChart));
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
    public void onSelectionDone(Consumer<Map<XYChart<X, Y>, XYChartSelection<X, Y>>> action) {
        selectionDoneEvent = action;
    }

    public Y getCurrentYValue(XYChart<X, Y> chart) {
        return currentYValues.get(chart).getValue();
    }

//    public ReadOnlyProperty<Y> currentYValueProperty() {
//        return currentYValues;
//    }

    public X getCurrentXValue() {
        return currentXValue.getValue();
    }

    public Property<X> currentXValueProperty() {
        return currentXValue;
    }

    private void fireSelectionDoneEvent() {
        if (selectionDoneEvent != null && (selection.getWidth() > 0 && selection.getHeight() > 0)) {
            Map<XYChart<X, Y>, XYChartSelection<X, Y>> s = new HashMap<>();
            charts.forEach((c, f) -> s.put(
                    c,
                    new XYChartSelection<X, Y>(
                            getValueFromXcoord(selection.getX() - 0.5),
                            getValueFromXcoord(selection.getX() + selection.getWidth() - 0.5),
                            getValueFromYcoord(c, selection.getY() + selection.getHeight() - 0.5),
                            getValueFromYcoord(c, selection.getY() - 0.5),
                            selection.getHeight() != chartInfo.getPlotArea().getHeight())));
            selectionDoneEvent.accept(s);
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
        yAxisLabel.setLayoutX(Math.min(parent.getWidth() - yAxisLabel.getWidth(), chartInfo.getPlotArea().getMaxX() + 5));
        yAxisLabel.setLayoutY(Math.min(mousePosition.getY() + 5, chartInfo.getPlotArea().getMaxY() - yAxisLabel.getHeight()));

        StringBuilder yAxisText = new StringBuilder();
        charts.forEach((c, f) -> {
            currentYValues.computeIfAbsent(c, (k) -> new SimpleObjectProperty<Y>()).setValue(getValueFromYcoord(c, mousePosition.getY()));
            yAxisText.append(c.getYAxis().getLabel())
                    .append(": ")
                    .append(f.apply(currentYValues.get(c).getValue()))
                    .append("\n");
        });
        yAxisLabel.setText(yAxisText.toString());
    }

    private Y getValueFromYcoord(XYChart<X, Y> chart, double yPosition) {
        double yStart = chart.getYAxis().getLocalToParentTransform().getTy();
        double axisYRelativePosition = yPosition - getYShift(masterChart, parent) - (yStart * 1.5);
        return chart.getYAxis().getValueForDisplay(axisYRelativePosition);
    }

    private X getValueFromXcoord(double xPosition) {
        double xStart = masterChart.getXAxis().getLocalToParentTransform().getTx();
        double axisXRelativeMousePosition = xPosition - getXShift(masterChart, parent) - xStart;
        return masterChart.getXAxis().getValueForDisplay(axisXRelativeMousePosition - 5);
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
        currentXValue.setValue(getValueFromXcoord(mousePosition.getX()));
        xAxisLabel.setText(xValuesFormatter.apply(currentXValue.getValue()));
    }

    private void handleMouseMoved(MouseEvent event) {
        Rectangle2D area = chartInfo.getPlotArea();
        double xPos = event.getX() + getXShift(masterChart, parent);
        double yPos = event.getY() + getYShift(masterChart, parent);
        mousePosition = new Point2D(Math.max(area.getMinX(), Math.min(area.getMaxX(), xPos)), Math.max(area.getMinY(), Math.min(area.getMaxY(), yPos)));
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
        } else {
            selection.setY(verticalMarker.getStartY());
            selection.setHeight(verticalMarker.getEndY() - verticalMarker.getStartY());
        }
        if (verticalMarkerVisible.get()) {
            double width = verticalMarker.getStartX() - selectionStart.getX();
            selection.setX(width < 0 ? verticalMarker.getStartX() : selectionStart.getX());
            selection.setWidth(Math.abs(width));
        } else {
            selection.setX(horizontalMarker.getStartX());
            selection.setWidth(horizontalMarker.getEndX() - horizontalMarker.getStartX());
        }
    }

    private Label newAxisLabel() {
        Label label = new Label("");
        label.getStyleClass().add("crosshair-axis-label");
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
