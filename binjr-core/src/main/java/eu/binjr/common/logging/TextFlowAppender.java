/*
 *    Copyright 2017-2022 Frederic Thevenet
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

import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * TextFlowAppender for Log4j 2
 *
 * @author Frederic Thevenet
 */
@Plugin(
        name = "TextFlowAppender",
        category = "Core",
        elementType = "appender",
        printObject = true)
public class TextFlowAppender extends AbstractAppender {
    private final Lock renderTextLock = new ReentrantLock();
    private final Map<Level, String> logColors = new HashMap<>();
    private final String defaultColor = "log-info";
    private final LogBuffer logBuffer = new LogBuffer();
    private Consumer<Collection<Log>> renderTextDelegate;

    protected TextFlowAppender(String name, Filter filter,
                               Layout<? extends Serializable> layout,
                               final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
        logColors.put(Level.TRACE, "trace");
        logColors.put(Level.DEBUG, "debug");
        logColors.put(Logger.PERF, "perf");
        logColors.put(Level.INFO, "info");
        logColors.put(Level.WARN, "warn");
        logColors.put(Level.ERROR, "error");
        logColors.put(Level.FATAL, "fatal");
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (AppEnvironment.getInstance().isDebugMode()) {
                    refreshTextFlow();
                }
            }
        }, 500, 500);
    }

    /**
     * Factory method. Log4j will parse the configuration and call this factory
     * method to construct the appender with
     * the configured attributes.
     *
     * @param name   Name of appender
     * @param layout Log layout of appender
     * @param filter Filter for appender
     * @return The TextFlowAppender
     */
    @PluginFactory
    public static TextFlowAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter) {
        if (name == null) {
            LOGGER.error("No name provided for TextFlowAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new TextFlowAppender(name, filter, layout, true);
    }

    public void setRenderTextDelegate(Consumer<Collection<Log>> delegate) {
        this.renderTextDelegate = delegate;
    }

    /**
     * Clear the circular buffer used by the appender
     */
    public void clearBuffer() {
        renderTextLock.lock();
        try {
            logBuffer.clear();
        } finally {
            renderTextLock.unlock();
        }
    }

    /**
     * This method is where the appender does the work.
     *
     * @param event Log event with log data
     */
    @Override
    public void append(LogEvent event) {
        renderTextLock.lock();
        try {
            new String(getLayout().toByteArray(event)).lines().forEach(
                    message -> {
                        Log log = new Log(message, logColors.getOrDefault(event.getLevel(), defaultColor));
                        logBuffer.push(log);
                    });
        } finally {
            renderTextLock.unlock();
        }
    }

    private void refreshTextFlow() {
        Dialogs.runOnFXThread(() -> {
            if (renderTextLock.tryLock()) {
                try {
                    if (renderTextDelegate != null && logBuffer.isDirty()) {
                        logBuffer.clean();
                        renderTextDelegate.accept(logBuffer.getLogs());
                    }
                } finally {
                    renderTextLock.unlock();
                }
            }
        });
    }

    public record Log(String message, String styleClass) {

        public String getMessage() {
            return message;
        }

        public String getStyleClass() {
            return styleClass;
        }
    }

    private static class LogBuffer {
        private volatile boolean dirty;
        private long lineCounter = 0;
        private final LinkedHashMap<Long, Log> buffer = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Log> eldest) {
                return size() > UserPreferences.getInstance().consoleMaxLineCapacity.get().intValue();
            }
        };

        public void clear() {
            dirty = true;
            buffer.clear();
            lineCounter = 0;
        }

        public void clean() {
            this.dirty = false;
        }

        public Log push(Log log) {
            dirty = true;
            return buffer.put(lineCounter++, log);
        }

        public boolean isDirty() {
            return dirty;
        }

        public Collection<Log> getLogs() {
            return buffer.values();
        }
    }
}