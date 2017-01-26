package eu.fthevenet.binjr.data.timeseries.transform;

import eu.fthevenet.binjr.data.timeseries.TimeSeries;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by FTT2 on 26/01/2017.
 */
public class TransformPipeline<T extends Number> {
   private final List<TimeSeriesTransform<T>> transforms = new LinkedList<>();

   public TransformPipeline(){

   }

    public List<TimeSeriesTransform<T>> getTransforms() {
        return transforms;
    }
}
