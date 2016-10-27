package eu.fthevenet.binjr.commons.fxchart;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.Chart;
import javafx.scene.chart.XYChart;

/**
 * Created with IntelliJ IDEA.
 * User: Pedro
 * Date: 09-09-2013
 * Time: 20:25
 * To change this template use File | Settings | File Templates.
 */
public interface IChart<X, Y> {
    XYChart getChart();
    Group getNodeRepresentation();

    void setPrefWidth(double size);
    void setPrefHeight(double size);

    void setMaxWidth(double size);
    void setMaxHeight(double size);

    ObservableList<XYChart.Series> getData();
    void setData(ObservableList<XYChart.Series<X, Y>> data);

    void setTitle(String title);
}
