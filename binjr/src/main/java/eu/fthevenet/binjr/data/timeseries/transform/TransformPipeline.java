package eu.fthevenet.binjr.data.timeseries.transform;

import java.util.LinkedList;
import java.util.List;

//FIXME not use at the moment
public class TransformPipeline<T extends Number> {
   private final List<TimeSeriesTransform<T>> transforms = new LinkedList<>();

   public TransformPipeline(){

   }

    public List<TimeSeriesTransform<T>> getTransforms() {
        return transforms;
    }
}
