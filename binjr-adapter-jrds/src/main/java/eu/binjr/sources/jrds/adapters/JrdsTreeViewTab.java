/*
 *    Copyright 2017-2018 Frederic Thevenet
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

package eu.binjr.sources.jrds.adapters;

/**
 * An enumeration of JRDS tree tabs.
 *
 * @author Frederic Thevenet
 */
public enum JrdsTreeViewTab {
    /**
     * All Hosts
     */
    HOSTS_TAB("All Hosts", "hoststab", null),
    /**
     * Single Host
     */
    SINGLE_HOST("Host: ", "hoststab", "host"),
    /**
     * All Services
     */
    SERVICE_TAB("All Services", "servicestab", null),
    /**
     * All Views
     */
    VIEWS_TAB("All Views", "viewstab", null),
    /**
     * All Filters
     */
    FILTER_TAB("All Filters", "filtertab", null),
    /**
     * Single Filter
     */
    SINGLE_FILTER("Filter: ", "filtertab", "filter"),
    /**
     * All Tags
     */
    TAGS_TAB("All Tags", "tagstab", null),
    /**
     * Single Tag
     */
    SINGLE_TAG("Tag: ", "tagstab", "filter");

    private final String argument;
    private final String label;
    private final String command;

    JrdsTreeViewTab(String label, String command, String argument) {
        this.label = label;
        this.command = command;
        this.argument = argument;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Gets the command keyword associated with the enum entry
     *
     * @return the command keyword associated with the enum entry
     */
    public String getCommand() {
        return command;
    }

    /**
     * Gets the argument associated with the enum entry
     *
     * @return the argument associated with the enum entry
     */
    public String getArgument() {
        return argument;
    }
}
