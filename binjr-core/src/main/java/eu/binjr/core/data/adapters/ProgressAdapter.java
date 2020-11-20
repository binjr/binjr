/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.core.data.adapters;

import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import javafx.beans.property.DoubleProperty;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public interface ProgressAdapter<T> extends DataAdapter<T> {

    /**
     * Gets decoded data from the source as a map of {@link TimeSeriesProcessor},
     * for the time interval and {@link TimeSeriesInfo} specified.
     *
     * @param path        the path of the data in the source
     * @param begin       the start of the time interval.
     * @param end         the end of the time interval.
     * @param seriesInfo  the series to get data from.
     * @param bypassCache true if adapter cache should be bypassed, false otherwise.
     *                    This parameter is ignored if adapter does not support caching
     * @param progress    An observable property used to report progress on the current task.
     *                    Can be null.
     * @return the output stream in which to return data.
     * @throws DataAdapterException if an error occurs while retrieving data from the source.
     */
    Map<TimeSeriesInfo<T>, TimeSeriesProcessor<T>> fetchData(String path,
                                                             Instant begin,
                                                             Instant end,
                                                             List<TimeSeriesInfo<T>> seriesInfo,
                                                             boolean bypassCache,
                                                             DoubleProperty progress) throws DataAdapterException;

    /**
     * Returns a {@link TimeRange} to initiate a new {@link XYChartsWorksheet} with so that it is
     * set to a relevant period with regard to the chosen data sources.
     *
     * @param path       the path of the data in the source
     * @param seriesInfo the series to get data from.
     * @param progress   An observable property used to report progress on the current task.
     *                   Can be null.
     * @return the {@link TimeRange} to initiate a new {@link XYChartsWorksheet} with
     * @throws DataAdapterException if an error occurs.
     */
    default TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<T>> seriesInfo, DoubleProperty progress) throws DataAdapterException {
        var end = ZonedDateTime.now(getTimeZoneId());
        return TimeRange.of(end.minusHours(24), end);
    }

}
