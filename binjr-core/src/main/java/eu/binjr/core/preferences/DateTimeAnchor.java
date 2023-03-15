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

import java.time.*;
import java.util.function.Supplier;

/**
 * An enumeration of common {@link TemporalAnchor} implementations.
 */
public enum DateTimeAnchor implements TemporalAnchor {
    EPOCH("1970-01-01 00:00:00", () -> LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))),
    TODAY("Current date (midnight)", () -> LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)),
    NOW("Current date and time", LocalDateTime::now);

    private final String label;
    private final Supplier<LocalDateTime> resolver;

    DateTimeAnchor(String label, Supplier<LocalDateTime> resolver) {
        this.label = label;
        this.resolver = resolver;
    }

    @Override
    public String toString() {
        return this.label;
    }

    @Override
    public LocalDateTime resolve() {
        return resolver.get();
    }
}
