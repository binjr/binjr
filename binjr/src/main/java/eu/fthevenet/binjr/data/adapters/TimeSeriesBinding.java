package eu.fthevenet.binjr.data.adapters;

import javafx.scene.paint.Color;

/**
 * Represents a binding between a time series and the {@link DataAdapter} used to produce it.
 *
 * @author Frederic Thevenet
 */
public interface TimeSeriesBinding<T extends Number> {
    /**
     * Returns the label of the binding
     *
     * @return the label of the binding
     */
    String getLabel();

    /**
     * Returns the path of the binding
     *
     * @return the path of the binding
     */
    String getPath();

    /**
     * Returns the {@link DataAdapter} of the binding
     *
     * @return the {@link DataAdapter} of the binding
     */
    DataAdapter<T> getAdapter();

    String getColor();

    String getLegend();

    String getGraphType();

//    int getUnitBase();

    //boolean isEnabled();


}
