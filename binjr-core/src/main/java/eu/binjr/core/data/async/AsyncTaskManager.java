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

package eu.binjr.core.data.async;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Defines methods to submit operation to be executed asynchronously to the UI thread, as well as a thread pool to run these tasks.
 *
 * @author Frederic Thevenet
 */
public class AsyncTaskManager {
    private static final Logger logger = LogManager.getLogger(AsyncTaskManager.class);
    private final ExecutorService threadPool;

    private AsyncTaskManager() {
        ThreadFactory threadFactory = new ThreadFactory() {
            final AtomicInteger threadNum = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("binjr-async-thread-" + threadNum.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        threadPool = Executors.newCachedThreadPool(threadFactory);
    }

    /**
     * Returns the singleton instance for {@link AsyncTaskManager}
     *
     * @return the singleton instance for {@link AsyncTaskManager}
     */
    public static AsyncTaskManager getInstance() {
        return AsyncTaskManagerHolder.instance;
    }

    /**
     * Submit a {@link Task} on the {@link AsyncTaskManager} thread pool
     *
     * @param task the {@link Task} instance to execute
     * @param <V>  the parameter type for the task
     * @return the result of the task
     */
    public <V> Future<?> submit(Task<V> task) {
        logger.trace(() -> "Task " + task.toString() + " submitted");
        return threadPool.submit(task);
    }

    /**
     * Submit an action as a {@link Callable} instance to be run asynchronously, as well as callback to handle success and failure of the main action.
     *
     * @param action      an action as a {@link Callable} instance to be run asynchronously
     * @param onSucceeded a callback to handle success
     * @param onFailed    a callback to handle failure
     * @param <V>         the parameter type for the task
     * @return the result of the task
     */
    public <V> Future<?> submit(Callable<V> action, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<V> t = new Task<V>() {
            @Override
            protected V call() throws Exception {
                return action.call();
            }
        };
        t.setOnSucceeded(onSucceeded);
        t.setOnFailed(onFailed);
        logger.trace(() -> "Task " + t.toString() + " submitted");

        return threadPool.submit(t);
    }


    private static class AsyncTaskManagerHolder {
        private static final AsyncTaskManager instance = new AsyncTaskManager();
    }
}
