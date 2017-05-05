package eu.fthevenet.util.ui.controls;

import javafx.concurrent.Task;
import javafx.util.Duration;


import java.util.concurrent.ForkJoinPool;

/**
 * Created by fred on 05/05/2017.
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
        ForkJoinPool.commonPool().submit(delayedTask);
    }

}
