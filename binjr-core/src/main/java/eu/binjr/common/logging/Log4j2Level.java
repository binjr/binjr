/*
 *    Copyright 2019 Frederic Thevenet
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

package eu.binjr.common.logging;


import org.apache.logging.log4j.Level;

import java.util.Arrays;

public enum Log4j2Level {
    OFF(Level.OFF),
    FATAL(Level.FATAL),
    ERROR(Level.ERROR),
    WARN(Level.WARN),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE),
    ALL(Level.ALL);

    private final Level level;

    Log4j2Level(Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }

    public static Log4j2Level valueOf(String value, Log4j2Level defaultValue) {
        if (value != null &&
                Arrays.stream(Log4j2Level.values()).map(Enum::toString).anyMatch(s -> s.equalsIgnoreCase(value))) {
            return Log4j2Level.valueOf(value);
        }
        return defaultValue;
    }

    public static Log4j2Level valueOf(Level value){
        return valueOf(value, Log4j2Level.INFO);
    }

    public static Log4j2Level valueOf(Level value, Log4j2Level defaultValue) {
        return Arrays.stream(Log4j2Level.values())
                .filter(l -> l.getLevel().equals(value))
                .findFirst()
                .orElse(defaultValue);
    }
}
