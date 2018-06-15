/*
 *    Copyright 2018 Frederic Thevenet
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

package eu.fthevenet.binjr.controllers;

import eu.fthevenet.binjr.data.workspace.Chart;
import eu.fthevenet.util.javafx.charts.XYChartSelection;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Wraps a stack to record user navigation steps.
 */
public class WorksheetNavigationHistory {
    private static final Logger logger = LogManager.getLogger(WorksheetNavigationHistory.class);
    private final Deque<Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>>> stack = new ArrayDeque<>();
    private final SimpleBooleanProperty empty = new SimpleBooleanProperty(true);

    /**
     * Put the provided {@link XYChartSelection} on the top of the stack.
     *
     * @param state the provided {@link XYChartSelection}
     * @return the provided {@link XYChartSelection}
     */
    void push(Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> state) {
        if (state == null) {
            logger.warn(() -> "Trying to push null state into backwardHistory");
            return;
        }
        empty.set(false);
        this.stack.push(state);
    }

    /**
     * Clears the history
     */
    void clear() {
        this.stack.clear();
        empty.set(true);
    }

    /**
     * Gets the topmost {@link XYChartSelection} from the stack.
     *
     * @return the topmost {@link XYChartSelection} from the stack.
     */
    Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> pop() {
        Map<Chart<Double>, XYChartSelection<ZonedDateTime, Double>> r = this.stack.pop();
        empty.set(stack.isEmpty());
        return r;
    }

    /**
     * Returns true if the underlying stack is empty, false otherwise.
     *
     * @return true if the underlying stack is empty, false otherwise.
     */
    boolean isEmpty() {
        return empty.get();
    }

    BooleanProperty emptyProperty() {
        return empty;
    }

    @Override
    public String toString() {
        return this.dump();
    }

    /**
     * Dumps the content of the stack as a string
     *
     * @return the content of the stack as a string
     */
    public String dump() {
        final StringBuilder sb = new StringBuilder("History:");
        AtomicInteger pos = new AtomicInteger(0);
        if (this.isEmpty()) {
            sb.append(" { empty }");
        }
        else {
            stack.forEach(h -> sb.append("\n").append(pos.incrementAndGet()).append(" ->").append(h.entrySet().stream().map(e -> e.getKey().getName() + ": " + e.getValue().toString()).collect(Collectors.joining(" "))));
        }
        return sb.toString();
    }
}
