package eu.fthevenet.binjr.data.adapters;

/**
 * Created by FTT2 on 23/01/2017.
 */
public interface TimeSeriesBinding<T extends Number> {
    String getLabel();
    String getPath();
    DataAdapter<T> getAdapter();


}
