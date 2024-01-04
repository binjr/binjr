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
import javafx.animation.PauseTransition;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BinjrLoadingPane extends StackPane {

    private static final PseudoClass LOADING_PSEUDO_CLASS = PseudoClass.getPseudoClass("loading");
    private static final int NB_FRAMES = 5;
    private final AtomicBoolean rendering = new AtomicBoolean(false);
    private final ObjectProperty<AnimationSize> animationSize = new SimpleObjectProperty<>(AnimationSize.LARGE);
    private final BooleanProperty loading = new BooleanPropertyBase(false) {
        public void invalidated() {
            pseudoClassStateChanged(LOADING_PSEUDO_CLASS, get());
            BinjrLoadingPane.this.imageView.setVisible(get());
        }

        @Override
        public Object getBean() {
            return BinjrLoadingPane.this;
        }

        @Override
        public String getName() {
            return "loading";
        }
    };

    private final DoubleProperty targetFps;
    private final IntegerProperty initialDelayMs;
    private final ScheduledExecutorService scheduler;
    private final ImageView imageView = new ImageView();
    private final Image[] frames = new Image[NB_FRAMES];
    private int frameIndex;

    public BinjrLoadingPane() {
        this(2, 500);
    }

    public BinjrLoadingPane(double targetFps, int initialDelayMs) {
        super();
        this.targetFps = new SimpleDoubleProperty(targetFps);
        this.initialDelayMs = new SimpleIntegerProperty(initialDelayMs);
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });

        animationSize.addListener((observable) -> {
            imageView.setFitWidth(animationSize.getValue().getSize());
            imageView.setFitHeight(animationSize.getValue().getSize());
        });

        // Setup animation
        imageView.getStyleClass().add("binjr-logo-view");
        imageView.setFitWidth(animationSize.getValue().getSize());
        imageView.setFitHeight(animationSize.getValue().getSize());
      //  imageView.setOpacity(.6);
        this.getChildren().add(imageView);
        this.frameIndex = 0;
        for (int i = 0; i < NB_FRAMES; i++) {
            this.frames[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/eu/binjr/images/loading_" + i + ".png")));
        }
        this.setAlignment(Pos.CENTER);
        this.visibleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                var delay = new PauseTransition(Duration.millis(this.initialDelayMs.get()));
                delay.setOnFinished((event) -> {
                    // Only start animation if masker is still visible after delay
                    if (this.isVisible()) {
                        startAnimation();
                    }
                });
                delay.playFromStart();
            }
        });
        startAnimation();
        this.getStyleClass().setAll("binjr-loading-pane");
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

    public int getInitialDelayMs() {
        return initialDelayMs.get();
    }

    public IntegerProperty initialDelayMsProperty() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(int initialDelayMs) {
        this.initialDelayMs.set(initialDelayMs);
    }

    private void render() {
        if (rendering.compareAndSet(false, true)) {
            try {
                Dialogs.runOnFXThread(() -> {
                    imageView.setImage(frames[frameIndex]);
                    if (++frameIndex >= NB_FRAMES) {
                        frameIndex = 0;
                    }
                });
            } finally {
                rendering.set(false);
            }
        }
    }

    private void startAnimation() {
        this.loading.set(true);
        var task = scheduler.scheduleAtFixedRate(this::render, 0, Math.round(1000.0 / targetFps.get()), TimeUnit.MILLISECONDS);
        ChangeListener<Boolean> stopWhenNotVisible = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue) {
                    BinjrLoadingPane.this.visibleProperty().removeListener(this);
                    BinjrLoadingPane.this.loading.set(false);
                    task.cancel(true);
                    frameIndex = 0;
                }
            }
        };
        this.visibleProperty().addListener(stopWhenNotVisible);
    }

    public void setAnimationSize(AnimationSize value){
        this.animationSize.setValue(value);
    }

    public AnimationSize getAnimationSize() {
        return animationSize.getValue();
    }

    public Property<AnimationSize> animationSizeProperty() {
        return animationSize;
    }

    public enum AnimationSize {
        SMALL(32),
        MEDIUM(64.0),
        LARGE(128.0),
        XL(256.0);

        private final double size;

        AnimationSize(double size) {
            this.size = size;
        }

        public double getSize() {
            return size;
        }
    }
}
