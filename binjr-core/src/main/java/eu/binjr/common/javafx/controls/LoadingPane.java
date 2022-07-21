/*
 *    Copyright 2022 Frederic Thevenet
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

/*
 *    Copyright 2022 Frederic Thevenet
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

package eu.binjr.common.javafx.controls;

import eu.binjr.core.dialogs.Dialogs;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;


import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoadingPane extends StackPane {
    private static final double HEIGHT = 64.0;
    private static final double WIDTH = 64.0;
    private final Canvas canvas;
    private final AtomicBoolean rendering = new AtomicBoolean(false);

    private final DoubleProperty targetFps;
    private final DoubleProperty rotationSpeed;
    private final ScheduledExecutorService scheduler;
    private final GraphicsContext context2D;
    private double currentAngle;
    private final Rotate transform;
    private final Image image;
    private double angularStep;

    public LoadingPane() {
        this(30, 90);
    }

    public LoadingPane(double targetFps, double rotationSpeed) {
        super();
        this.targetFps = new SimpleDoubleProperty(targetFps);
        this.rotationSpeed = new SimpleDoubleProperty(rotationSpeed);

        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });

        InvalidationListener computeAngularStep = (observable) -> this.angularStep = this.rotationSpeed.get() / this.targetFps.get();

        // Setup animation
        this.canvas = new Canvas(WIDTH, HEIGHT);
        this.context2D = canvas.getGraphicsContext2D();
        this.currentAngle = 0;
        this.transform = new Rotate(0, WIDTH / 2, HEIGHT / 2);
        this.image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/eu/binjr/images/loading_64.png")));
        this.targetFps.addListener(computeAngularStep);

        this.rotationSpeed.addListener(computeAngularStep);
        computeAngularStep.invalidated(null);

        this.setAlignment(Pos.CENTER);
        this.visibleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                startSpinning();
            }
        });
        this.getChildren().add(canvas);
        startSpinning();
        this.getStyleClass().setAll("canvas-masker-pane");
    }


    public double getTargetFps() {
        return this.targetFps.get();
    }

    public DoubleProperty targetFpsProperty() {
        return this.targetFps;
    }

    public void setTargetFps(double targetFps) {
        this.targetFps.set(targetFps);
    }

    public double getRotationSpeed() {
        return rotationSpeed.get();
    }

    public DoubleProperty rotationSpeedProperty() {
        return rotationSpeed;
    }

    public void setRotationSpeed(double rotationSpeed) {
        this.rotationSpeed.set(rotationSpeed);
    }


    private void render() {
        if (rendering.compareAndSet(false, true)) {
            try {
                Dialogs.runOnFXThread(() -> {
                    context2D.clearRect(0, 0, WIDTH, HEIGHT);
                    transform.setAngle(currentAngle);
                    context2D.setTransform(
                            transform.getMxx(),
                            transform.getMyx(),
                            transform.getMxy(),
                            transform.getMyy(),
                            transform.getTx(),
                            transform.getTy());
                    context2D.drawImage(image, 0, 0);
                    currentAngle += angularStep;
                });
            } finally {
                rendering.set(false);
            }
        }
    }

    private void startSpinning() {
        var task = scheduler.scheduleAtFixedRate(this::render, 0, Math.round(1000.0 / targetFps.get()), TimeUnit.MILLISECONDS);
        ChangeListener<Boolean> stopWhenNotVisible = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue) {
                    LoadingPane.this.visibleProperty().removeListener(this);
                    task.cancel(true);
                }
            }
        };
        this.visibleProperty().addListener(stopWhenNotVisible);
    }


}
