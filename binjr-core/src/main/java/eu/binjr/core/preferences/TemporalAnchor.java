/*
 * Copyright 2023 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.core.preferences;

import java.time.LocalDateTime;

/**
 * An interface that encapsulates a {@link LocalDateTime} supplier used as temporal anchor to generate timestamps
 * from partial data.
 */
public interface TemporalAnchor {
    /**
     * Returns the {@link LocalDateTime} that represents the anchor at the instant when the method is invoked.
     *
     * @return the {@link LocalDateTime} that represents the anchor at the instant when the method is invoked.
     */
    LocalDateTime resolve();
}
