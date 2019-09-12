/*
 *    Copyright 2019 Frederic Thevenet
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

package eu.binjr.core.data.timeseries.transform;

import javafx.scene.chart.XYChart;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A transform that aligns the first and last timestamps from the series actually retrieved by the adapter with
 * the exact values requested by the user.
 * <p>
 * The algorithm assumes that the insertion order in the provided list is consistent with the timestamp of the
 * samples it contains.
 * If this is not the case, the {@link SortTransform} should be applied prior to this one.
 *
 * @author Frederic Thevenet
 */
public class AlignBoundariesTransform extends TimeSeriesTransform {

    private final ZonedDateTime startTime;
    private final ZonedDateTime endTime;

    /**
     * Base constructor for {@link TimeSeriesTransform} instances.
     */
    public AlignBoundariesTransform(ZonedDateTime startTime, ZonedDateTime endTime) {
        super("AlignBoundariesTransform");
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    protected List<XYChart.Data<ZonedDateTime, Double>> apply(List<XYChart.Data<ZonedDateTime, Double>> data) {
        if (data.isEmpty()){
            return data;
        }
        //Align the lower (earlier) boundary of the series
        var iterator = data.iterator();
        XYChart.Data<ZonedDateTime, Double> firstSample = iterator.next();
        if (firstSample.getXValue().isAfter(startTime)) {
            // if the first available sample is later than the requested start time,
            // add a sample 1ns after last sample with a 0 value then another sample at start time in order to
            // simulate a brick-wall filter, as NaN isn't well supported by JavaFX Charts
            data.add(new XYChart.Data<>(firstSample.getXValue().minus(1, ChronoUnit.NANOS), 0.0));
            data.add(0, new XYChart.Data<>(startTime, 0.0));
        } else if (firstSample.getXValue().isBefore(startTime)) {
            // remove all samples with timestamps occurring before the requested start time.
            var previous = firstSample;
            while (iterator.hasNext() && firstSample.getXValue().isBefore(startTime)) {
                previous = firstSample;
                iterator.remove();
                firstSample = iterator.next();
            }
            // use the known sample right before start time to interpolate the value of inserted sample
            var lowerBound = new XYChart.Data<>(startTime, interpolate(previous, firstSample, startTime));
            data.add(0, lowerBound);
        }

        // Align the higher (later) boundary of the series
        var lastIterator = data.listIterator(data.size());
        XYChart.Data<ZonedDateTime, Double> lastSample = lastIterator.previous();
        if (lastSample.getXValue().isBefore(endTime)) {
            data.add(new XYChart.Data<>(lastSample.getXValue().plus(1, ChronoUnit.NANOS), 0.0));
            data.add(new XYChart.Data<>(endTime, 0.0));
        } else if (lastSample.getXValue().isAfter(endTime)) {
            var next = lastSample;
            while (lastIterator.hasPrevious() && lastSample.getXValue().isAfter(endTime)) {
                next = lastSample;
                lastIterator.remove();
                lastSample = lastIterator.previous();
            }
            var upperBound = new XYChart.Data<>(endTime, interpolate(lastSample, next, endTime));
            data.add(upperBound);
        }
        return data;
    }

    private Double interpolate(XYChart.Data<ZonedDateTime, Double> val1, XYChart.Data<ZonedDateTime, Double> val2, ZonedDateTime time) {
        var x3 = (double) time.toInstant().toEpochMilli();
        var x1 = (double) val1.getXValue().toInstant().toEpochMilli();
        var y1 = val1.getYValue();
        var x2 = (double) val2.getXValue().toInstant().toEpochMilli();
        var y2 = val2.getYValue();
        return (y2 - y1) / (x2 - x1) * (x3 - x1) + y1;
    }
}
