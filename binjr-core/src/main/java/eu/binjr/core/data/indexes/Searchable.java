/*
 *    Copyright 2020-2021 Frederic Thevenet
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

package eu.binjr.core.data.indexes;

import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.core.data.indexes.parser.EventParser;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Searchable extends Closeable {
    void add(String path,
             InputStream ias,
             EventParser parser,
             LongProperty progress,
             BooleanProperty cancellationRequested) throws IOException;

    void add(String path,
             InputStream ias,
             boolean commit,
             EventParser parser,
             LongProperty progress,
             BooleanProperty cancellationRequested) throws IOException;

    TimeRange getTimeRangeBoundaries(List<String> files, ZoneId zoneId) throws IOException;

    SearchHitsProcessor search(long start, long end,
                               Map<String, Collection<String>> params,
                               String query,
                               int page,
                               ZoneId zoneId,
                               boolean ignoreCache) throws Exception;
}
