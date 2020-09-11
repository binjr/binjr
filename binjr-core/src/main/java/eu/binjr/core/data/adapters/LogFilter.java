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

import java.util.*;

public class LogFilter {
    public final String filterQuery;
    public final Set<String> severities;

    public static LogFilter empty() {
        return new LogFilter();
    }

    private LogFilter() {
        this("", new HashSet<>());
    }

    public LogFilter(String filterQuery, Collection<String> severities) {
        this(filterQuery, new HashSet<>(severities));
    }

    public LogFilter(String filterQuery, Set<String> severities) {
        this.filterQuery = filterQuery;
        this.severities = severities;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public Set<String> getSeverities() {
        return severities;
    }

    @Override
    public int hashCode() {
        return filterQuery.hashCode() + severities.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LogFilter filter = (LogFilter) obj;
        return filterQuery.equals(filter.filterQuery) &&
                severities.equals(filter.severities);
    }
}
