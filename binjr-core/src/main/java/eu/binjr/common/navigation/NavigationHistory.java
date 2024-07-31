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

package eu.binjr.common.navigation;

import eu.binjr.common.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps a stack to record user navigation steps.
 */
public class NavigationHistory<T> {
    private static final Logger logger = Logger.create(NavigationHistory.class);
    private final HistoryQueue<T> forward = new HistoryQueue<>();
    private final HistoryQueue<T> backward = new HistoryQueue<>();

    public T getHead() {
        return head.getValue();
    }

    public Property<T> headProperty() {
        return head;
    }

    private final Property<T> head = new SimpleObjectProperty<>();
    private T previousItem;

    public HistoryQueue<T> forward() {
        return forward;
    }

    public HistoryQueue<T> backward() {
        return backward;
    }

    public synchronized void clear() {
        forward.clear();
        backward.clear();
    }

    public synchronized void setHead(T item, boolean save) {
        if (save) {
            logger.debug((() -> "Saving to history: item=" + item.toString() + " previous=" + previousItem.toString()));
            this.backward.push(previousItem);
            this.forward.clear();
        }
        previousItem = item;
        head.setValue(item);
    }

    public Optional<T> getPrevious() {
        return restoreSelectionFromHistory(backward, forward);
    }

    public Optional<T> getNext() {
        return restoreSelectionFromHistory(forward, backward);
    }

    public synchronized String dump() {
        return "Backward navigation history:\n" + backward.dump() +
                "Forward navigation history:\n" + forward.dump();
    }

    private synchronized Optional<T> restoreSelectionFromHistory(HistoryQueue<T> history, HistoryQueue<T> toHistory) {
        if (!history.isEmpty()) {
            toHistory.push(getHead());
            return Optional.of(history.pop());
        } else {
            logger.debug(() -> "NavigationHistory is empty: nothing to go back to.");
            return Optional.empty();
        }
    }

    public class HistoryQueue<T> {

        private final Deque<T> stack = new ArrayDeque<>();

        private final SimpleBooleanProperty empty = new SimpleBooleanProperty(true);


        private void push(T state) {
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
        private void clear() {
            this.stack.clear();
            empty.set(true);
        }


        private T pop() {
            T r = this.stack.pop();
            empty.set(stack.isEmpty());
            return r;
        }


        public SimpleBooleanProperty emptyProperty() {
            return empty;
        }

        /**
         * Returns true if the underlying stack is empty, false otherwise.
         *
         * @return true if the underlying stack is empty, false otherwise.
         */
        public boolean isEmpty() {
            return empty.get();
        }

        /**
         * Dumps the content of the stack as a string
         *
         * @return the content of the stack as a string
         */
        private String dump() {
            final StringBuilder sb = new StringBuilder();
            AtomicInteger pos = new AtomicInteger(0);
            if (this.isEmpty()) {
                sb.append(" { empty }");
            } else {
                stack.forEach(h -> sb.append(pos.incrementAndGet()).append(" ->").append(h.toString()).append("\n"));
            }
            return sb.toString();
        }
    }
}
