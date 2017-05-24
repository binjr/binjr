/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.sources.jrds.adapters;

/**
 * An enumeration of JRDS tree tabs.
 *
 * @author Frederic Thevenet
 */
public enum JrdsTreeFilter {
    FILTER_TAB("All Filters", "filtertab"),
    SERVICE_TAB("All Services", "servicestab"),
    VIEWS_TAB("All Views", "viewstab"),
    HOSTS_TAB("All Hosts", "hoststab"),
    TAGS_TAB("All Tags", "tagstab");

    private String label;
    private String command;


    JrdsTreeFilter(String label, String command) {
        this.label = label;
        this.command = command;
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
}
