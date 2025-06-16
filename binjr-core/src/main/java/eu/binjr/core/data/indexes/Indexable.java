/*
 *    Copyright 2020-2025 Frederic Thevenet
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
import eu.binjr.core.data.adapters.ReloadStatus;
import eu.binjr.core.data.indexes.parser.EventFormat;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;

import java.io.Closeable;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Indexable extends Closeable {

    <T> void add(String path,
                 T source,
                 boolean commit,
                 EventFormat<T> eventFormat,
                 EventToDocumentMapper eventToDocumentMapper,
                 LongProperty progress,
                 Property<ReloadStatus> cancellationRequested) throws IOException;

    <T> void add(String path,
                 T source,
                 boolean commit,
                 EventFormat<T> eventFormat,
                 EventToDocumentMapper eventToDocumentMapper,
                 LongProperty progress,
                 Property<ReloadStatus> cancellationRequested,
                 BiFunction<String, ParsedEvent, String> computePathFacetValue,
                 Function<T, List<String>> computeDeletePaths) throws IOException;

    TimeRange getTimeRangeBoundaries(List<String> files, ZoneId zoneId) throws IOException;

    Map<String, ReloadStatus> getIndexedFiles();

}
