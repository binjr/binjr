/*
 *    Copyright 2017-2019 Frederic Thevenet
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
import eu.binjr.core.preferences.GlobalPreferences;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
public final class TextFlowAppender extends AbstractAppender {
    private TextFlow textArea;
    private final Lock textAreaLock = new ReentrantLock();
    private final Map<Level, String> logColors = new HashMap<>();
    private final String defaultColor = "log-info";
    private final LogBuffer<Text, Object> logBuffer = new LogBuffer<>();

    protected TextFlowAppender(String name, Filter filter,
                               Layout<? extends Serializable> layout,
                               final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        logColors.put(Level.TRACE, "log-trace");
        logColors.put(Level.DEBUG, "log-debug");
        logColors.put(Level.INFO, "log-info");
        logColors.put(Level.WARN, "log-warn");
        logColors.put(Level.ERROR, "log-error");
        logColors.put(Level.FATAL, "log-fatal");
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

    /**
     * Set TextFlow to append
     *
     * @param textFlow TextFlow to append
     */
    public void setTextFlow(TextFlow textFlow) {
        textAreaLock.lock();
        try {
            this.textArea = textFlow;
        } finally {
            textAreaLock.unlock();
        }
    }

    /**
     * Clear the circular buffer used by the appender
     */
    public synchronized void clearBuffer() {
        logBuffer.clear();
    }

    /**
     * This method is where the appender does the work.
     *
     * @param event Log event with log data
     */
    @Override
    public synchronized void append(LogEvent event) {
        new String(getLayout().toByteArray(event)).lines().forEach(
                message -> {
                    Text log = new Text(message + "\n");
                    log.getStyleClass().add(logColors.getOrDefault(event.getLevel(), defaultColor));
                    logBuffer.put(log, null);
                });
    }

    private void refreshTextFlow() {
        if (textAreaLock.tryLock()) {
            try {
                if (textArea != null && logBuffer.isDirty()) {
                    logBuffer.clean();
                    Dialogs.runOnFXThread(() -> {
                        textArea.getChildren().clear();
                        textArea.getChildren().addAll(logBuffer.keySet());
                    });
                }
            } finally {
                textAreaLock.unlock();
            }
        }
    }

    private class LogBuffer<K, V> extends LinkedHashMap<K, V> {
        private volatile boolean dirty;

        public void clean() {
            this.dirty = false;
        }

        @Override
        public V put(K key, V value) {
            dirty = true;
            return super.put(key, value);
        }

        @Override
        public void clear() {
            dirty = true;
            super.clear();
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > GlobalPreferences.getInstance().getConsoleMaxLineCapacity();
        }

        public boolean isDirty() {
            return dirty;
        }
    }
}