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

package eu.binjr.core.data.dirtyable;

import javafx.beans.property.BooleanProperty;

/**
 * Classes implementing this interface can have the changes made on member decorated with the
 * {@link IsDirtyable} annotation tracked by an instance of {@link ChangeWatcher}.
 *
 * @author Frederic Thevenet
 */
public interface Dirtyable {
    /**
     * Returns true if the {@link Dirtyable} instance needs to be persisted, false otherwise
     *
     * @return true if the {@link Dirtyable} instance needs to be persisted, false otherwise
     */
    Boolean isDirty();

    /**
     * A {@link BooleanProperty} that observes the changes made to the {@link Dirtyable} instance
     *
     * @return a {@link BooleanProperty} that observes the changes made to the {@link Dirtyable} instance
     */
    BooleanProperty dirtyProperty();

    /**
     * Clear the dirty status of the {@link Dirtyable} instance
     */
    void cleanUp();
}
