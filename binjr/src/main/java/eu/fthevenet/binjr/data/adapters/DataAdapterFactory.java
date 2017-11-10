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

package eu.fthevenet.binjr.data.adapters;

import java.util.Map;

/**
 * A functional interface to be used as a producer for {@link DataAdapter} instances.
 *
 * @author Frederic Thevenet
 */
@FunctionalInterface
public interface DataAdapterFactory<T> {
    /**
     * Initializes a new instance of the {@link DataAdapter} class from the provided URL and timezone
     *
     * @param params a map of parameters required to establish a connection to the source
     * @return a new instance of the {@link DataAdapter} class from the provided url and timezone
     */
    DataAdapter<T, ?> fromParams(Map<String, String> params);
}
