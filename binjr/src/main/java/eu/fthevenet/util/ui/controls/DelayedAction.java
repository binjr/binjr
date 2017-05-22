package eu.fthevenet.util.ui.controls;

import eu.fthevenet.binjr.data.async.AsyncTaskManager;
import javafx.concurrent.Task;
import javafx.util.Duration;

/**
 * Schedule a tasks to start after the specified delay
 * @author Frederic Thevenet
 */
public class DelayedAction {
    private Task<Object> delayedTask;
    public DelayedAction(Duration delay, Runnable action){

        delayedTask = new Task<Object>() {
            @Override
            protected Object call() throws Exception {
                Thread.sleep(((long) delay.toMillis()));
                return null;
            }
        };
        delayedTask.setOnSucceeded(event ->action.run());
    }
    public void submit(){
        AsyncTaskManager.getInstance().submit(delayedTask);
    }
}
