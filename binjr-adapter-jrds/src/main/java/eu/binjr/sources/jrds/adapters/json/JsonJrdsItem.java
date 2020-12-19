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

package eu.binjr.sources.jrds.adapters.json;

import java.util.Arrays;

/**
 * JRDS item data class
 */
public class JsonJrdsItem {
    /**
     * The name
     */
    public String name;
    /**
     * The id
     */
    public String id;
    /**
     * The type
     */
    public String type;
    /**
     * The filter
     */
    public String filter;
    /**
     * Children list
     */
    public JsonTreeRef[] children;

    /**
     * TreeRef
     */
    public static class JsonTreeRef {
        /**
         * reference
         */
        public String _reference;
    }

    @Override
    public String toString() {
        return "JsonJrdsItem{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", filter='" + filter + '\'' +
                ", children=" + Arrays.toString(children) +
                '}';
    }
}
