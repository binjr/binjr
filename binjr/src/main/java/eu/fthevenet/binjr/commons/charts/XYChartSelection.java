package eu.fthevenet.binjr.commons.charts;

/**
 * Created by FTT2 on 08/12/2016.
 */
public interface XYChartSelection<X, Y> {
    X getStartX();

    X getEndX();

    Y getStartY();

    Y getEndY();
}
