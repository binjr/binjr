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

package eu.fthevenet.util.javafx.controls;

import eu.fthevenet.binjr.data.async.AsyncTaskManager;
import javafx.concurrent.Task;
import javafx.util.Duration;

/**
 * Schedule a tasks to start after the specified delay
 *
 * @author Frederic Thevenet
 */
public class DelayedAction {
    private Task<Object> delayedTask;

    public DelayedAction(Runnable action, Duration delay) {

        delayedTask = new Task<Object>() {
            @Override
            protected Object call() throws Exception {
                Thread.sleep(((long) delay.toMillis()));
                return null;
            }
        };
        delayedTask.setOnSucceeded(event -> action.run());

    }

    public void submit() {
        AsyncTaskManager.getInstance().submit(delayedTask);
    }

    public static void run(Runnable action, Duration delay) {
        new DelayedAction(action, delay).submit();
    }

}
