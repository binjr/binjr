/*
 *    Copyright 2024 Frederic Thevenet
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

package eu.binjr.sources.jvmgc.adapters;


import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterPreferences;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Defines the preferences associated with the Text files adapter.
 */
public class JvmGcAdapterPreferences extends DataAdapterPreferences {

    /**
     * Initialize a new instance of the {@link JvmGcAdapterPreferences} class associated to
     * a {@link DataAdapter} instance.
     *
     * @param dataAdapterClass the associated {@link DataAdapter}
     */
    public JvmGcAdapterPreferences(Class<? extends DataAdapter<?>> dataAdapterClass) {
        super(dataAdapterClass);
        loggingLevel.property().addListener((observableValue, julSeverity, newVal) -> {
            setJulLevel(newVal.getLevel());
        });
        setJulLevel(loggingLevel.get().getLevel());
    }

    public ObservablePreference<Boolean> isDetectRollingLogs = booleanPreference("isDetectRollingLogs", true);


    public ObservablePreference<JulSeverity> loggingLevel = enumPreference(JulSeverity.class, "loggingLevel", JulSeverity.SEVERE);

    public enum JulSeverity {
        OFF(Level.OFF),
        SEVERE(Level.SEVERE),
        WARNING(Level.WARNING),
        INFO(Level.INFO),
        CONFIG(Level.CONFIG),
        FINE(Level.FINE),
        FINER(Level.FINER),
        FINEST(Level.FINEST),
        ALL(Level.ALL);

        private Level level;

        JulSeverity(Level level) {
            this.level = level;
        }

        Level getLevel() {
            return this.level;
        }
    }

    public static void setJulLevel(Level newLvl) {
        java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
        if (rootLogger != null) {
            Handler[] handlers = rootLogger.getHandlers();
            rootLogger.setLevel(newLvl);
            for (Handler h : handlers) {
                if (h instanceof FileHandler)
                    h.setLevel(newLvl);
            }
        }
    }

}
