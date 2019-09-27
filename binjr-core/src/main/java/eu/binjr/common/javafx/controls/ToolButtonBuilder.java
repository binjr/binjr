/*
 *    Copyright 2019 Frederic Thevenet
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
 *    Copyright 2019 Frederic Thevenet
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
 *    Copyright 2019 Frederic Thevenet
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
 *    Copyright 2019 Frederic Thevenet
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

import eu.binjr.common.javafx.bindings.BindingManager;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ToolButtonBuilder<T extends ButtonBase> {

    private final BindingManager bindingManager;
    private double height = 20.0;
    private double width = 20.0;
    private String text = "";
    private String tooltip = "";
    private List<String> styleClass = new ArrayList<>();
    private List<String> iconStyleClass = new ArrayList<>();
    private EventHandler<ActionEvent> action = null;
    private final List<Consumer<T>> bindings = new ArrayList<>();

    public ToolButtonBuilder() {
        this(null);
    }

    public ToolButtonBuilder(BindingManager bindingManager) {
        this.bindingManager = bindingManager;
    }

    private T buildButton(T btn) {
        btn.setText(text);
        btn.setTooltip(makeTooltip(tooltip));
        btn.setPrefHeight(height);
        btn.setMaxHeight(height);
        btn.setMinHeight(height);
        btn.setPrefWidth(width);
        btn.setMaxWidth(width);
        btn.setMinWidth(width);
        if (styleClass != null) {
            btn.getStyleClass().addAll(styleClass);
        }
        if (iconStyleClass != null && !iconStyleClass.isEmpty()) {
            Region icon = new Region();
            icon.getStyleClass().addAll(iconStyleClass);
            btn.setGraphic(icon);
            btn.setAlignment(Pos.CENTER);
            btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        } else {
            btn.setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
        if (action != null) {
            btn.setOnAction(bindingManager != null ? bindingManager.registerHandler(action) : action);
        }
        bindings.forEach(buttonBaseConsumer -> buttonBaseConsumer.accept(btn));
        return btn;
    }

    public ToolButtonBuilder<T> setHeight(double height) {
        this.height = height;
        return this;
    }

    public ToolButtonBuilder<T> setWidth(double width) {
        this.width = width;
        return this;
    }

    public ToolButtonBuilder<T> setText(String text) {
        this.text = text;
        return this;
    }

    public ToolButtonBuilder<T> setTooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public ToolButtonBuilder<T> setStyleClass(String... styleClass) {
        this.styleClass = new ArrayList<>(Arrays.asList(styleClass));
        return this;
    }

    public ToolButtonBuilder<T> setIconStyleClass(String... iconStyleClass) {
        this.iconStyleClass = new ArrayList<>(Arrays.asList(iconStyleClass));
        return this;
    }

    public ToolButtonBuilder<T> setAction(EventHandler<ActionEvent> action) {
        this.action = action;
        return this;
    }

    public <R> ToolButtonBuilder<T> bindBidirectionnal(Function<T, Property<R>> mapper, Property<R> property) {
        bindings.add(t -> {
            if (bindingManager != null) {
                bindingManager.bindBidirectional(mapper.apply(t), property);
            } else {
                mapper.apply(t).bindBidirectional(property);
            }
        });
        return this;
    }

    public <R, U extends R> ToolButtonBuilder<T> bind(Function<T, Property<R>> mapper, ObservableValue<U> observableValue) {
        bindings.add(buttonBase -> {
            if (bindingManager != null) {
                bindingManager.bind(mapper.apply(buttonBase), observableValue);
            } else {
                mapper.apply(buttonBase).bind(observableValue);
            }
        });
        return this;
    }

    public T build(Supplier<T> generator) {
        return buildButton(generator.get());
    }

    public static Node makeIconNode(Pos position, String... iconStyles) {
        Region r = new Region();
        r.getStyleClass().addAll(iconStyles);
        HBox box = new HBox(r);
        box.setAlignment(position);
        box.getStyleClass().add("icon-container");
        return box;
    }

    private Tooltip makeTooltip(String text) {
        var tooltip = new Tooltip(text);
        var delayProp = UserPreferences.getInstance().tooltipShowDelayMs.property();
        tooltip.showDelayProperty().bind(Bindings.createObjectBinding(() -> Duration.millis(delayProp.getValue().doubleValue()), delayProp));
        return tooltip;
    }

}
