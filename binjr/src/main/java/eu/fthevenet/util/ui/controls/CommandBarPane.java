package eu.fthevenet.util.ui.controls;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

/**
 * @author Frederic Thevenet
 */
public class CommandBarPane extends AnchorPane {
    private int collapsedWidth = 48;
    private int expandedWidth = 200;
    private int animationDuration = 50;
    private Duration delay = new Duration(50);
    private static PseudoClass EXPANDED_PSEUDO_CLASS = PseudoClass.getPseudoClass("expanded");
    private Timeline showTimeline;
    private Timeline hideTimeline;
    private DoubleProperty commandBarWidth = new SimpleDoubleProperty(0.2);


    private void show() {
        if (hideTimeline != null) {
            hideTimeline.stop();
        }
        if (showTimeline != null && showTimeline.getStatus() == Animation.Status.RUNNING) {
            return;
        }
        Duration duration = Duration.millis(getAnimationDuration());
        KeyFrame keyFrame = new KeyFrame(duration, new KeyValue(commandBarWidth, getExpandedWidth()));
        showTimeline = new Timeline(keyFrame);
        showTimeline.setOnFinished(event -> this.expanded.set(true));
        showTimeline.setDelay(delay);
        showTimeline.play();
    }


    private void hide() {
        this.expanded.set(false);
        if (showTimeline != null) {
            showTimeline.stop();
        }
        if (hideTimeline != null && hideTimeline.getStatus() == Animation.Status.RUNNING) {
            return;
        }
        if (commandBarWidth.get() <= getInitialWidth()) {
            return;
        }
        Duration duration = Duration.millis(getAnimationDuration());
        KeyFrame keyFrame = new KeyFrame(duration, new KeyValue(commandBarWidth, getInitialWidth()));
        hideTimeline = new Timeline(keyFrame);
        hideTimeline.play();
    }

    public CommandBarPane() {
        commandBarWidth.addListener((observable, oldValue, newValue) -> {
            this.setMinWidth(newValue.doubleValue());
        });
    }


    BooleanProperty expanded = new BooleanPropertyBase(false) {
        public void invalidated() {
            pseudoClassStateChanged(EXPANDED_PSEUDO_CLASS, get());
        }

        @Override
        public Object getBean() {
            return CommandBarPane.this;
        }

        @Override
        public String getName() {
            return "expanded";
        }
    };

    public void setExpanded(boolean expanded) {
        if (expanded && !this.expanded.get()) {
            show();
        }
        if (!expanded && this.expanded.get()) {
            hide();
        }
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    public int getInitialWidth() {
        return collapsedWidth;
    }

    public int getExpandedWidth() {
        return expandedWidth;
    }

    public void setCollapsedWidth(int collapsedWidth) {
        this.collapsedWidth = collapsedWidth;
    }

    public void setExpandedWidth(int expandedWidth) {
        this.expandedWidth = expandedWidth;
    }

    public int getAnimationDuration() {
        return animationDuration;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
    }
}
