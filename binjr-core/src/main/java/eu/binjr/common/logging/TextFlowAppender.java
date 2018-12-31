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

package eu.binjr.common.logging;

import javafx.application.Platform;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TextFlowAppender for Log4j 2
 */
@Plugin(
        name = "TextFlowAppender",
        category = "Core",
        elementType = "appender",
        printObject = true)
public final class TextFlowAppender extends AbstractAppender {
    private static TextFlow textArea;
    private final Lock lock = new ReentrantLock();
    private final Map<Level, String> logColors = new HashMap<>();
    private final String defaultColor = "log-info";


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
    }

    /**
     * This method is where the appender does the work.
     *
     * @param event Log event with log data
     */
    @Override
    public void append(LogEvent event) {
        lock.lock();
        try {
            final String message = new String(getLayout().toByteArray(event));
            // append log text to TextArea
            if (textArea != null) {
                Text log = new Text(message);
                log.getStyleClass().add(logColors.getOrDefault(event.getLevel(), defaultColor));
                Platform.runLater(() -> {
                    try {
                        textArea.getChildren().add(log);
                    } catch (final Throwable t) {
                        System.out.println("Error while append to TextFlow: " + t.getMessage());
                    }
                });
            }
        } catch (final IllegalStateException ex) {
            ex.printStackTrace();
        } finally {
            lock.unlock();
        }
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
    public static void setTextFlow(TextFlow textFlow) {
        TextFlowAppender.textArea = textFlow;
    }
}