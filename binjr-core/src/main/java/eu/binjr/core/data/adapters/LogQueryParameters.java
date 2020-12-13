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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class LogQueryParameters {
    private String filterQuery;
    private Set<String> severities;
    private int page;
    private TimeRange timeRange;


    public static LogQueryParameters empty() {
        return new LogQueryParameters();
    }

    private LogQueryParameters() {
        this(TimeRange.of(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()),
                ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())),
                "", new HashSet<>(), 0);
    }

    private LogQueryParameters(TimeRange timeRange, String filterQuery, Set<String> severities, int page) {
        this.filterQuery = filterQuery;
        this.severities = severities;
        this.page = page;
        this.timeRange = timeRange;
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

    @XmlAttribute
    public TimeRange getTimeRange() {
        return timeRange;
    }

    private void setTimeRange(TimeRange value) {
        this.timeRange = value;
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
        LogQueryParameters filter = (LogQueryParameters) obj;
        return filterQuery.equals(filter.filterQuery) &&
                severities.equals(filter.severities) &&
                timeRange.equals(filter.timeRange) &&
                page == filter.page;
    }

    public static class Builder{
        private String filterQuery;
        private Set<String> severities;
        private int page;
        private TimeRange timeRange;

        public Builder(){
            this.timeRange = TimeRange.of(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()),
                    ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()));
            this.filterQuery ="";
            this.page = 0;
            this.severities= new HashSet<>();
        }

        public Builder(LogQueryParameters params){
            this.timeRange =params.getTimeRange();
            this.filterQuery =params.getFilterQuery();
            this.page = params.getPage();
            this.severities= params.getSeverities();
        }

        public Builder setTimeRange(TimeRange value){
            this.timeRange = value;
            return this;
        }
        public Builder setFilterQuery(String value){
            this.filterQuery = value;
            return this;
        }
        public Builder setPage(int value){
            this.page = value;
            return this;
        }
        public Builder setSeverities(Set<String> value){
            this.severities = value;
            return this;
        }

        public LogQueryParameters build(){
            return new LogQueryParameters(timeRange, filterQuery, severities, page);
        }
    }

}
