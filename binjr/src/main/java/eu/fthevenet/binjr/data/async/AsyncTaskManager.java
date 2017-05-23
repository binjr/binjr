package eu.fthevenet.binjr.data.async;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Frederic Thevenet
 */
public class AsyncTaskManager {
    private static final Logger logger = LogManager.getLogger(AsyncTaskManager.class);
    private  final ExecutorService threadPool;

    private static class AsyncTaskManagerHolder {
        private final static AsyncTaskManager instance = new AsyncTaskManager();
    }

    public static AsyncTaskManager getInstance() {
        return AsyncTaskManagerHolder.instance;
    }

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

  public <V> Future<?> submit(Task<V> task){
        return threadPool.submit(task);
  }


    public <V> Future<?> submit(Callable<V> mainAction, EventHandler<WorkerStateEvent> successAction, EventHandler<WorkerStateEvent> errorAction) {
        Task<V> t = new Task<V>() {
            @Override
            protected V call() throws Exception {
                return mainAction.call();
            }
        };
        t.setOnSucceeded(successAction);
        t.setOnFailed(errorAction);


        return threadPool.submit(t);
    }
}
