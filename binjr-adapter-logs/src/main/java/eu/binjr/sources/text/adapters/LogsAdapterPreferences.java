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

package eu.binjr.sources.text.adapters;


import com.google.gson.Gson;
import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterPreferences;

public class LogsAdapterPreferences extends DataAdapterPreferences {
    private static final Gson gson = new Gson();

    public ObservablePreference<Number> defaultTextViewFontSize = integerPreference("defaultTextViewFontSize", 10);

    public ObservablePreference<String[]> folderFilters = objectPreference(String[].class,
            "folderFilters",
            new String[]{"*"},
            gson::toJson,
            s -> gson.fromJson(s, String[].class));

    public ObservablePreference<String[]> fileExtensionFilters = objectPreference(String[].class,
            "fileExtensionFilters",
            new String[]{".log", ".txt"},
            gson::toJson,
            s -> gson.fromJson(s, String[].class));

    public ObservablePreference<String> timestampPattern = stringPreference("timestampPattern",
            "\\d{4}[\\/-]\\d{2}[\\/-]\\d{2}[\\-\\s]\\d{2}:\\d{2}:\\d{2}([\\.,]\\d+)?");

    public ObservablePreference<String> severityPattern = stringPreference("severityPattern",
            "(?i)TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL");

    public ObservablePreference<String> loggerPattern = stringPreference("severityPattern", "[\\w\\d\\.\\-_]*");

    public ObservablePreference<String> threadPattern = stringPreference("threadPattern",
            "([\\\"\\w\\d\\.\\,\\-_\\@\\s\\/\\:\\#\\\\\\=\\{\\}\\&\\+\\%\\)\\(]*)((\\.\\.\\.\\[).*(ing\\]))?");

    public ObservablePreference<String> msgPattern = stringPreference("msgPattern", ".*");

    public ObservablePreference<String> linePattern = stringPreference("linePattern",
            "\\[(?<time>%s)\\]\\s+\\[\\s?(?<severity>%s)\\s?\\]\\s+\\[(?<thread>%s)\\]\\s+\\[(?<logger>%s)\\]\\s(?<message>%s)");

    public ObservablePreference<Number> hitsPerPage = integerPreference("hitsPerPage", 10000);

    public ObservablePreference<IndexDirectoryLocation> indexDirectoryLocation =
            enumPreference(IndexDirectoryLocation.class, "indexDirectoryLocation", IndexDirectoryLocation.MEMORY );

    public ObservablePreference<Number> parsingThreadNumber = integerPreference("parsingThreadNumber", 0);

    public ObservablePreference<Number> blockingQueueCapacity = integerPreference("blockingQueueCapacity", 10000);

    public ObservablePreference<Number> parsingThreadDrainSize = integerPreference("parsingThreadDrainSize", 512);

    public LogsAdapterPreferences(Class<? extends DataAdapter<?>> dataAdapterClass) {
        super(dataAdapterClass);
    }
}
