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

package eu.binjr.core.data.workspace;

import javafx.beans.property.Property;

public interface Syncable {
    /**
     * Returns true if the worksheet's timeline is linked to other worksheets, false otherwise.
     *
     * @return true if the worksheet's timeline is linked to other worksheets, false otherwise.
     */
    Boolean isTimeRangeLinked();

    /**
     * The timeRangeLinked property.
     *
     * @return the timeRangeLinked property.
     */
    Property<Boolean> timeRangeLinkedProperty();

    /**
     * Set to true if the worksheet's timeline is linked to other worksheets, false otherwise.
     *
     * @param timeRangeLinked true if the worksheet's timeline is linked to other worksheets, false otherwise.
     */
    void setTimeRangeLinked(Boolean timeRangeLinked);
}
