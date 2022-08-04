/*
 *    Copyright 2020-2022 Frederic Thevenet
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
import eu.binjr.core.data.indexes.parser.EventFormat;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.List;

public interface Indexable<T> extends Closeable {

    void add(String path,
             InputStream ias,
             boolean commit,
             EventFormat<T> parser,
             EnrichDocumentFunction<T> enrichDocumentFunction,
             LongProperty progress,
             Property<IndexingStatus> indexingStatus) throws IOException;

    void add(String path,
             InputStream ias,
             EventFormat<T> parser,
             EnrichDocumentFunction<T> enrichDocumentFunction,
             LongProperty progress,
             Property<IndexingStatus> indexingStatus) throws IOException;

    TimeRange getTimeRangeBoundaries(List<String> files, ZoneId zoneId) throws IOException;

}
