/*
 *    Copyright 2017-2021 Frederic Thevenet
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

import javafx.animation.*;
import javafx.beans.property.*;
import javafx.css.PseudoClass;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

/**
 * An {@link AnchorPane} that can slide in or out from a side of its attached window.
 *
 * @author Frederic Thevenet
 */
public class DrawerPane extends AnchorPane {
    private static final PseudoClass EXPANDED_PSEUDO_CLASS = PseudoClass.getPseudoClass("expanded");
    private final DoubleProperty collapsedWidth = new SimpleDoubleProperty(48);
    private final DoubleProperty expandedWidth = new SimpleDoubleProperty(200);
    private final IntegerProperty animationDuration = new SimpleIntegerProperty(50);
    private final Property<PaneAnimation> animation = new SimpleObjectProperty<>(PaneAnimation.NONE);
    private Timeline showTimeline;
    private Timeline hideTimeline;
    private final DoubleProperty commandBarWidth = new SimpleDoubleProperty(0.2);
    private final Property<Side> side = new SimpleObjectProperty<>(Side.LEFT);
    private final Property<Node> sibling = new SimpleObjectProperty<>();

    public enum PaneAnimation {
        NONE,
        GROW,
        SLIDE
    }

    private final BooleanProperty expanded = new BooleanPropertyBase(false) {
        public void invalidated() {
            pseudoClassStateChanged(EXPANDED_PSEUDO_CLASS, get());
        }

        @Override
        public Object getBean() {
            return DrawerPane.this;
        }

        @Override
        public String getName() {
            return "expanded";
        }
    };

    public DrawerPane() {
        expanded.addListener((observable, oldValue, newValue) -> {
            switch (animation.getValue()) {
                case NONE:
                    movePane(newValue);
                    break;
                case GROW:
                    if (newValue)
                        growPane();
                    else
                        shrinkPane();
                    break;
                case SLIDE:
                    slidePane(newValue);
                    break;
            }
        });
        commandBarWidth.addListener((observable, oldValue, newValue) -> {
            doCommandBarResize(newValue.doubleValue());
        });
    }


    private void movePane(boolean show) {
        Double value = show ? expandedWidth.getValue() : collapsedWidth.getValue();
        if (side.getValue().isVertical()) {
            this.setTranslateX(value);
        } else {
            this.setTranslateY(value);
        }
        switch (side.getValue()) {
            case LEFT:
                this.setTranslateX(value);
                break;
            case RIGHT:
                this.setTranslateX(-1 * value);
                break;
            case TOP:
                this.setTranslateY(value);
                break;
            case BOTTOM:
                this.setTranslateY(-1 * value);
                break;
        }
        anchorNode(sibling.getValue(), value);
    }

    private void growPane() {
        if (hideTimeline != null) {
            hideTimeline.stop();
        }
        if (showTimeline != null && showTimeline.getStatus() == Animation.Status.RUNNING) {
            return;
        }
        Duration duration = Duration.millis(animationDuration.getValue());
        KeyFrame keyFrame = new KeyFrame(duration, new KeyValue(commandBarWidth, expandedWidth.getValue()));
        showTimeline = new Timeline(keyFrame);
        if (sibling.getValue() != null) {
            showTimeline.setOnFinished(event -> new DelayedAction(
                    () -> anchorNode(sibling.getValue(), expandedWidth.getValue()),
                    Duration.millis(50)).submit());
        }
        showTimeline.play();
        this.expanded.setValue(true);
    }

    private void shrinkPane() {
        if (showTimeline != null) {
            showTimeline.stop();
        }
        if (hideTimeline != null && hideTimeline.getStatus() == Animation.Status.RUNNING) {
            return;
        }
        if (commandBarWidth.get() <= collapsedWidth.getValue()) {
            return;
        }
        Duration duration = Duration.millis(animationDuration.getValue());
        hideTimeline = new Timeline(new KeyFrame(duration, new KeyValue(commandBarWidth, collapsedWidth.getValue())));
        anchorNode(sibling.getValue(), collapsedWidth.getValue());
        hideTimeline.play();
        this.expanded.setValue(false);
    }


    private void doCommandBarResize(double v) {
        if (side.getValue().isVertical()) {
            this.setMinWidth(v);
        } else {
            this.setMinHeight(v);
        }
    }

    private void slidePane(boolean show) {
        Double value = show ? expandedWidth.getValue() : collapsedWidth.getValue();
        TranslateTransition transition = new TranslateTransition(new Duration(animationDuration.get()), this);
        switch (side.getValue()) {
            case LEFT:
                transition.setToX(value);
                break;
            case RIGHT:
                transition.setToX(-1 * value);
                break;
            case TOP:
                transition.setToY(value);
                break;
            case BOTTOM:
                transition.setToY(-1 * value);
                break;
        }
        transition.play();
        if (show) {
            transition.setOnFinished(event -> new DelayedAction(() -> anchorNode(sibling.getValue(), value), Duration.millis(50)).submit());
        } else {
            anchorNode(sibling.getValue(), value);
        }
    }

    private void anchorNode(Node node, double distance) {
        if (node != null) {
            switch (getSide()) {
                case LEFT:
                    AnchorPane.setLeftAnchor(node, distance);
                    break;
                case BOTTOM:
                    AnchorPane.setBottomAnchor(node, distance);
                    break;
                case RIGHT:
                    AnchorPane.setRightAnchor(node, distance);
                    break;
                case TOP:
                    AnchorPane.setTopAnchor(node, distance);
                    break;
            }
        }
    }

    public Side getSide() {
        return side.getValue();
    }

    public Property<Side> sideProperty() {
        return side;
    }

    public void setSide(Side side) {
        this.side.setValue(side);
    }

    public double getCollapsedWidth() {
        return collapsedWidth.get();
    }

    public DoubleProperty collapsedWidthProperty() {
        return collapsedWidth;
    }

    public void setCollapsedWidth(double collapsedWidth) {
        this.collapsedWidth.set(collapsedWidth);
    }

    public double getExpandedWidth() {
        return expandedWidth.get();
    }

    public DoubleProperty expandedWidthProperty() {
        return expandedWidth;
    }

    public void setExpandedWidth(double expandedWidth) {
        this.expandedWidth.set(expandedWidth);
    }

    public int getAnimationDuration() {
        return animationDuration.get();
    }

    public IntegerProperty animationDurationProperty() {
        return animationDuration;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration.set(animationDuration);
    }

    public PaneAnimation getAnimation() {
        return animation.getValue();
    }

    public Property<PaneAnimation> animationProperty() {
        return animation;
    }

    public void setAnimation(PaneAnimation animation) {
        this.animation.setValue(animation);
    }

    public Node getSibling() {
        return sibling.getValue();
    }

    public Property<Node> siblingProperty() {
        return sibling;
    }

    public void setSibling(Node sibling) {
        this.sibling.setValue(sibling);
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    public ReadOnlyBooleanProperty expandedProperty() {
        return ReadOnlyBooleanProperty.readOnlyBooleanProperty(expanded);
    }

    public void expand() {
        this.expanded.set(true);
    }

    public void collapse() {
        this.expanded.set(false);
    }

    public void toggle() {
        this.expanded.set(!expanded.getValue());
    }
}
