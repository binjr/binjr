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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class LogFilter {
    private String filterQuery;
    private Set<String> severities;
    private int page;
    private boolean retrieveHits;

    public static LogFilter all() {
        return new LogFilter("", new HashSet<>(), 0, true);
    }

    public static LogFilter facetsOnly() {
        return new LogFilter("", new HashSet<>(), 0, false);
    }

    private LogFilter() {
        this("", new HashSet<>(), 0, true);
    }

    public LogFilter(String filterQuery, Set<String> severities, int page, boolean retrieveHits) {
        this.filterQuery = filterQuery;
        this.severities = severities;
        this.page = page;
        this.retrieveHits = retrieveHits;
    }

    @XmlAttribute
    public String getFilterQuery() {
        return filterQuery;
    }

    private void setFilterQuery(String value) {
        this.filterQuery = value;
    }

    @XmlAttribute
    public Set<String> getSeverities() {
        return severities;
    }

    @XmlAttribute
    public int getPage() {
        return page;
    }

    private void setPage(int value) {
        this.page = value;
    }

    @Override
    public int hashCode() {
        return filterQuery.hashCode() + severities.hashCode() + Integer.hashCode(page);
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
                severities.equals(filter.severities) &&
                page == filter.page;
    }

    @XmlTransient
    public boolean getRetrieveHits() {
        return retrieveHits;
    }

    private void setRetrieveHits(boolean retrieveHits) {
        this.retrieveHits = retrieveHits;
    }
}
