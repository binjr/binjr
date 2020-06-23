/*
 *    Copyright 2016-2020 Frederic Thevenet
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

package eu.binjr.common.javafx.charts;

import eu.binjr.common.javafx.bindings.BindingManager;
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
    private final Rectangle selection = new Rectangle(0, 0, 0, 0);
    private final BooleanProperty verticalMarkerVisible = new SimpleBooleanProperty();
    private final BooleanProperty horizontalMarkerVisible = new SimpleBooleanProperty();
    private final Map<XYChart<X, Y>, Property<Y>> currentYValues = new HashMap<>();
    private final Property<X> currentXValue = new SimpleObjectProperty<>();
    private final XYChart<X, Y> masterChart;
    private final BooleanProperty isMouseOverChart = new SimpleBooleanProperty(false);
    private final BindingManager bindingManager = new BindingManager();
    private final BooleanProperty displayFullHeightMarker = new SimpleBooleanProperty(false);
    private Point2D selectionStart = new Point2D(-1, -1);
    private Point2D mousePosition = new Point2D(-1, -1);
    private Consumer<Map<XYChart<X, Y>, XYChartSelection<X, Y>>> selectionDoneEvent;


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
        masterChart.addEventHandler(MouseEvent.MOUSE_MOVED, bindingManager.registerHandler(this::handleMouseMoved));
        masterChart.addEventHandler(MouseEvent.MOUSE_DRAGGED, bindingManager.registerHandler(this::handleMouseMoved));
        masterChart.setOnMouseReleased(bindingManager.registerHandler(e -> {
            if (isSelecting.get()) {
                fireSelectionDoneEvent();
                drawVerticalMarker();
                drawHorizontalMarker();
            }
            isSelecting.set(false);
        }));
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
        masterChart.setOnMouseExited(bindingManager.registerHandler(event -> isMouseOverChart.set(false)));
        masterChart.setOnMouseEntered(bindingManager.registerHandler(event -> isMouseOverChart.set(true)));
        bindingManager.bind(horizontalMarker.visibleProperty(), horizontalMarkerVisible.and(isMouseOverChart));
        bindingManager.bind(yAxisLabel.visibleProperty(), horizontalMarkerVisible.and(isMouseOverChart));
        bindingManager.bind(verticalMarker.visibleProperty(), verticalMarkerVisible.and(isMouseOverChart));
        bindingManager.bind(xAxisLabel.visibleProperty(), verticalMarkerVisible.and(isMouseOverChart));
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
     * Sets the visibility of the  vertical marker
     *
     * @param verticalMarkerVisible the visibility of the  vertical marker
     */
    public void setVerticalMarkerVisible(boolean verticalMarkerVisible) {
        this.verticalMarkerVisible.set(verticalMarkerVisible);
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

    /**
     * Returns the Y value for currently selected set of coordinates for the provided chart.
     *
     * @param chart The chart to retrieve the current Y value from.
     * @return the Y value for currently selected set of coordinates for the provided chart.
     */
    public Y getCurrentYValue(XYChart<X, Y> chart) {
        return currentYValues.get(chart).getValue();
    }

    /**
     * Retusn  the X value for currently selected set of coordinates.
     *
     * @return the X value for currently selected set of coordinates.
     */
    public X getCurrentXValue() {
        return currentXValue.getValue();
    }

    /**
     * The currentXValue property.
     *
     * @return the currentXValue property.
     */
    public Property<X> currentXValueProperty() {
        return currentXValue;
    }

    public boolean isDisplayFullHeightMarker() {
        return displayFullHeightMarker.get();
    }

    public void setDisplayFullHeightMarker(boolean displayFullHeightMarker) {
        this.displayFullHeightMarker.set(displayFullHeightMarker);
    }

    public BooleanProperty displayFullHeightMarkerProperty() {
        return displayFullHeightMarker;
    }

    public void dispose() {
        logger.debug(() -> "Disposing XYChartCrossHair " + toString());
        selectionDoneEvent = null;
        bindingManager.close();
    }

    private void fireSelectionDoneEvent() {
        if (selectionDoneEvent != null && (selection.getWidth() > 0 && selection.getHeight() > 0)) {
            var s = new HashMap<XYChart<X, Y>, XYChartSelection<X, Y>>();
            charts.forEach((c, f) -> s.put(c, new XYChartSelection<>(
                    getValueFromXcoord(selection.getX() - 0.5),
                    getValueFromXcoord(selection.getX() + selection.getWidth() - 0.5),
                    getValueFromYcoord(c, Math.min(chartInfo.getPlotArea().getMaxY(), (selection.getY() + selection.getHeight()) - 0.5)),
                    getValueFromYcoord(c, Math.max(chartInfo.getPlotArea().getMinY(), (selection.getY() - 0.5))),
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
        if (displayFullHeightMarker.getValue()) {
            verticalMarker.setStartY(2);
            verticalMarker.setEndY(parent.getHeight() - 2);
        } else {
            verticalMarker.setStartY(chartInfo.getPlotArea().getMinY() + 0.5);
            verticalMarker.setEndY(chartInfo.getPlotArea().getMaxY() + 0.5);
        }
        xAxisLabel.setLayoutY(chartInfo.getPlotArea().getMaxY() + 4);
        xAxisLabel.setLayoutX(Math.min(mousePosition.getX() + 4, chartInfo.getPlotArea().getMaxX() - xAxisLabel.getWidth()));
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
