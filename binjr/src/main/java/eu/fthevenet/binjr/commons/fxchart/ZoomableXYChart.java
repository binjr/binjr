package eu.fthevenet.binjr.commons.fxchart;

        import javafx.beans.property.SimpleBooleanProperty;
        import javafx.beans.property.SimpleObjectProperty;
        import javafx.collections.ObservableList;
        import javafx.event.EventHandler;
        import javafx.geometry.Dimension2D;
        import javafx.geometry.Point2D;
        import javafx.scene.Cursor;
        import javafx.scene.Group;
        import javafx.scene.chart.Chart;
        import javafx.scene.chart.ValueAxis;
        import javafx.scene.chart.XYChart;
        import javafx.scene.input.MouseEvent;
        import javafx.scene.layout.Pane;
        import javafx.scene.paint.Color;
        import javafx.scene.shape.Rectangle;

public class ZoomableXYChart<X, Y> implements IChart<X, Y> {
    public static final MouseMode DEFAULT_MOUSE_MODE = MouseMode.ZOOM;

    ValueAxis xAxis;
    ValueAxis yAxis;

    Group chartGroup;
    Rectangle zoomRectangle;

    XYChart chart;

    private SimpleObjectProperty<MouseMode> mouseModeProperty = new SimpleObjectProperty<>();

    SimpleBooleanProperty isZoomingEnabled = new SimpleBooleanProperty(true);

    boolean isAxisValuesVisible;
    int tempMinorTickCount;


    // temporary variables used with zooming
    double initialMouseX;
    double initialMouseY;
    double initialMouseSceneX;
    double initialMouseSceneY;

    double lastMouseDragX;
    double lastMouseDragY;

    double startingXLowerBounds;
    double startingXUpperBounds;
    double startingYLowerBounds;
    double startingYUpperBounds;

    Cursor previousMouseCursor;

    boolean isShowingOnlyYPositiveValues;
    private boolean isVerticalPanningAllowed;

    public ZoomableXYChart(XYChart chart) throws Exception {
        if (!(chart.getXAxis() instanceof ValueAxis && chart.getYAxis() instanceof  ValueAxis))
        {
            throw new Exception("Axis of chart must be instances of ValueAxis");
        }
        this.chart = chart;
        isAxisValuesVisible = true;
        isShowingOnlyYPositiveValues = false;
        isVerticalPanningAllowed = true;

        xAxis = (ValueAxis) chart.getXAxis();
        yAxis = (ValueAxis) chart.getYAxis();

        setupZoom();
        setMouseMode(DEFAULT_MOUSE_MODE);
    }

    @Override
    public Group getNodeRepresentation()
    {
        return chartGroup;
    }

    public XYChart getChart()
    {
        return chart;
    }

    private Rectangle createZoomRectangle() {
        final Rectangle zoomArea = new Rectangle();
        zoomArea.setStrokeWidth(1);
        zoomArea.setStroke(Color.BLUE);
        Color BLUE_COLOR = Color.LIGHTBLUE;
        Color zoomAreaFill = new Color(BLUE_COLOR.getRed(), BLUE_COLOR.getGreen(), BLUE_COLOR.getBlue(), 0.5);
        zoomArea.setFill(zoomAreaFill);
        return zoomArea;
    }

    private void setupZoom()
    {
        zoomRectangle = createZoomRectangle();

        zoomRectangle.setVisible(false);
        chartGroup = new Group();
        chartGroup.getChildren().addAll(zoomRectangle, chart);

        chart.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                previousMouseCursor = chart.getCursor();

                initialMouseX = mouseEvent.getX();
                initialMouseY = mouseEvent.getY();
                initialMouseSceneX = mouseEvent.getSceneX();
                initialMouseSceneY = mouseEvent.getSceneY();

                lastMouseDragX = initialMouseSceneX;
                lastMouseDragY = initialMouseSceneY;

                if (mouseModeProperty.get() == MouseMode.ZOOM)
                {
                    setChartCursor(Cursor.CROSSHAIR);
                    zoomRectangle.setX(initialMouseX);
                    zoomRectangle.setY(initialMouseY);
                } else if (mouseModeProperty.get() == MouseMode.PAN)
                {
                    setChartCursor(Cursor.CLOSED_HAND);
                }
            }
        });
        chart.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                double mouseSceneX = mouseEvent.getSceneX();
                double mouseSceneY = mouseEvent.getSceneY();
                double dragX = mouseSceneX - lastMouseDragX;
                double dragY = mouseSceneY - lastMouseDragY;

                lastMouseDragX = mouseSceneX;
                lastMouseDragY = mouseSceneY;

                if (mouseModeProperty.get() == MouseMode.ZOOM)
                {
                    zoomRectangle.toFront();
                    zoomRectangle.setVisible(true);
                    zoomRectangle.setWidth(mouseSceneX - initialMouseSceneX);
                    zoomRectangle.setHeight(mouseSceneY - initialMouseSceneY);
                } else if (mouseModeProperty.get() == MouseMode.PAN)
                {
                    setAutoRanging(false);

                    Dimension2D chartDrag = sceneToChartDistance(dragX, dragY);

                    xAxis.setLowerBound(xAxis.getLowerBound() - chartDrag.getWidth());
                    xAxis.setUpperBound(xAxis.getUpperBound() - chartDrag.getWidth());

                    if (isVerticalPanningAllowed)
                    {
                        double newYLowerBound = yAxis.getLowerBound() + chartDrag.getHeight();
                        double newYUpperBound = yAxis.getUpperBound() + chartDrag.getHeight();

                        if (!isShowingOnlyYPositiveValues)
                        {
                            yAxis.setLowerBound(newYLowerBound);
                            yAxis.setUpperBound(newYUpperBound);
                        }
                        else {
                            yAxis.setLowerBound(newYLowerBound < 0 ? 0 : newYLowerBound);
                            yAxis.setUpperBound(newYUpperBound < 0 ? 0 : newYUpperBound);
                        }
                    }
                }
            }
        });

        chart.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseModeProperty.get() != MouseMode.ZOOM)
                    return;

                zoomRectangle.setVisible(false);
                setChartCursor(previousMouseCursor);

                double newMouseX = mouseEvent.getSceneX();
                double newMouseY = mouseEvent.getSceneY();

                if (newMouseX < initialMouseSceneX && newMouseY < initialMouseSceneY) // zoom out
                {
                    if (!(startingXLowerBounds == 0 && startingYLowerBounds == 0 && startingXUpperBounds == 0 && startingYUpperBounds == 0)) {
                        xAxis.setLowerBound(startingXLowerBounds);
                        xAxis.setUpperBound(startingXUpperBounds);
                        yAxis.setLowerBound(startingYLowerBounds);
                        yAxis.setUpperBound(startingYUpperBounds);
                        startingXLowerBounds = 0;
                        startingXUpperBounds = 0;
                        startingYLowerBounds = 0;
                        startingYUpperBounds = 0;
                    }
                } else if (newMouseX > initialMouseSceneX && newMouseY > initialMouseSceneY) // zoom in
                {
                    setAutoRanging(false);

                    double[] newLower = sceneToChartValues(initialMouseSceneX, newMouseY);
                    double[] newUpper = sceneToChartValues(newMouseX, initialMouseSceneY);

                    if (startingXLowerBounds == 0 && startingYLowerBounds == 0 && startingXUpperBounds == 0 && startingYUpperBounds == 0) {
                        startingXLowerBounds = xAxis.getLowerBound();
                        startingXUpperBounds = xAxis.getUpperBound();
                        startingYLowerBounds = yAxis.getLowerBound();
                        startingYUpperBounds = yAxis.getUpperBound();
                    }

                    xAxis.setLowerBound(newLower[0]);
                    xAxis.setUpperBound(newUpper[0]);
                    if (!isShowingOnlyYPositiveValues)
                    {
                        yAxis.setLowerBound(newLower[1]);
                        yAxis.setUpperBound(newUpper[1]);
                    }
                    else {
                        yAxis.setLowerBound(newLower[1] < 0 ? 0 : newLower[1]);
                        yAxis.setUpperBound(newUpper[1] < 0 ? 0 : newUpper[1]);
                    }
                }

            }
        });
    }

    private double[] sceneToChartValues(double sceneX, double sceneY)
    {
        double xDataLenght = xAxis.getUpperBound() - xAxis.getLowerBound();
        double yDataLenght = yAxis.getUpperBound() - yAxis.getLowerBound();
        double xPixelLenght = xAxis.getWidth();
        double yPixelLenght = yAxis.getHeight();

        Point2D leftBottomChartPos = xAxis.localToScene(0, 0);
        double xMinPixelCoord = leftBottomChartPos.getX();
        double yMinPixelCoord = leftBottomChartPos.getY();

        double chartXCoord = xAxis.getLowerBound() + ((sceneX - xMinPixelCoord) * xDataLenght / xPixelLenght);
        double chartYcoord = yAxis.getLowerBound() + ((yMinPixelCoord - sceneY) * yDataLenght / yPixelLenght);
        return new double[]{chartXCoord, chartYcoord};
    }

    private Dimension2D sceneToChartDistance(double sceneX, double sceneY)
    {
        double xDataLenght = xAxis.getUpperBound() - xAxis.getLowerBound();
        double yDataLenght = yAxis.getUpperBound() - yAxis.getLowerBound();
        double xPixelLenght = xAxis.getWidth();
        double yPixelLenght = yAxis.getHeight();

        double chartXDistance = sceneX * xDataLenght / xPixelLenght;
        double chartYDistance = sceneY * yDataLenght / yPixelLenght;
        return new Dimension2D(chartXDistance, chartYDistance);
    }

    public void setIsZoomingEnabled(boolean isEnabled)
    {
        isZoomingEnabled.set(isEnabled);
    }

    public boolean getIsZoomingEnabled()
    {
        return isZoomingEnabled.get();
    }

    public SimpleBooleanProperty isZoomingEnabledProperty()
    {
        return isZoomingEnabled;
    }

    public MouseMode getMouseMode() {
        return mouseModeProperty.get();
    }

    public void setMouseMode(MouseMode mode)
    {
        if (mode == this.getMouseMode())
            return;

        this.mouseModeProperty.set(mode);
        if (mouseModeProperty.get() == MouseMode.ZOOM)
        {
            setIsZoomingEnabled(true);
            setChartCursor(Cursor.DEFAULT);
        }
        else
        {
            setIsZoomingEnabled(false);
            if (mouseModeProperty.get() == MouseMode.PAN)
                setChartCursor(Cursor.OPEN_HAND);
        }
    }

    public SimpleObjectProperty<MouseMode> mouseModeProperty()
    {
        return mouseModeProperty;
    }

    public void setIsAxisValuesShowing(boolean isShowing)
    {
        isAxisValuesVisible = isShowing;
        xAxis.setTickLabelsVisible(isAxisValuesVisible);
        yAxis.setTickLabelsVisible(isAxisValuesVisible);
        xAxis.setTickMarkVisible(isAxisValuesVisible);
        yAxis.setTickMarkVisible(isAxisValuesVisible);

        if(!isAxisValuesVisible)
        {
            if (xAxis.getMinorTickCount() != 0) tempMinorTickCount = xAxis.getMinorTickCount();

            xAxis.setMinorTickCount(0);
            yAxis.setMinorTickCount(0);
        }
        else
        {
            if (xAxis.getMinorTickCount() == 0 && tempMinorTickCount != 0)
            {
                xAxis.setMinorTickCount(tempMinorTickCount);
                yAxis.setMinorTickCount(tempMinorTickCount);
            }
        }
    }

    public boolean getIsAxisValuesShowing()
    {
        return isAxisValuesVisible;
    }

    private void setAutoRanging(boolean isAutoRanging)
    {
        xAxis.setAutoRanging(isAutoRanging);
        yAxis.setAutoRanging(isAutoRanging);
    }


    public void setPrefWidth(double size)
    {
        chart.setPrefWidth(size);
    }

    public void setPrefHeight(double size)
    {
        chart.setPrefHeight(size);
    }

    public void setMaxWidth(double size)
    {
        chart.setMaxWidth(size);
    }

    public void setMaxHeight(double size)
    {
        chart.setMaxHeight(size);
    }

    @Override
    public ObservableList<XYChart.Series> getData() {
        return chart.getData();
    }

    public void setData(ObservableList<XYChart.Series<X, Y>> data)
    {
        setAutoRanging(true);
        chart.setData(data);
    }

    @Override
    public void setTitle(String title) {
        chart.setTitle(title);
    }

    public void setChartCursor(Cursor cursor)
    {
        Pane chartPane = (Pane) ReflectionUtils.forceFieldCall(Chart.class, "chartContent", chart);
        chartPane.setCursor(cursor);
    }

    public void setIsShowingOnlyYPositiveValues(boolean onlyYPositive)
    {
        isShowingOnlyYPositiveValues = onlyYPositive;
    }

    public void setIsVerticalPanningAllowed(boolean isAllowed)
    {
        isVerticalPanningAllowed = isAllowed;
    }
}