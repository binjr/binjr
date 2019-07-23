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

package eu.binjr.core.dialogs;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ToolButtonBuilder {
    private double height = 20.0;
    private double width = 20.0;
    private String text =null;
    private String tooltip =null;
    private List<String> styleClass = new ArrayList<>(Collections.singletonList("dialog-button"));
    private List<String> iconStyleClass = new ArrayList<>();
    private EventHandler<ActionEvent> action = null;


    private ButtonBase makeButton( ButtonBase btn){
        btn.setText(text);
        btn.setPrefHeight(height);
        btn.setMaxHeight(height);
        btn.setMinHeight(height);
        btn.setPrefWidth(width);
        btn.setMaxWidth(width);
        btn.setMinWidth(width);
        if (styleClass!= null) {
            btn.getStyleClass().addAll(styleClass);
        }
        if (iconStyleClass != null) {
            Region icon = new Region();
            icon.getStyleClass().addAll(iconStyleClass);
            btn.setGraphic(icon);
            btn.setAlignment(Pos.CENTER);
            btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
        if (tooltip!= null) {
            btn.setTooltip(new Tooltip(tooltip));
        }
        if (action != null) {
            btn.setOnAction(action);
        }
        return btn;
    }

    public ToolButtonBuilder setHeight(double height) {
        this.height = height;
        return this;
    }

    public ToolButtonBuilder setWidth(double width) {
        this.width = width;
        return this;
    }

    public ToolButtonBuilder setText(String text) {
        this.text = text;
        return this;
    }

    public ToolButtonBuilder setTooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public ToolButtonBuilder setStyleClass(List<String> styleClass) {
        this.styleClass = styleClass;
        return this;
    }

    public ToolButtonBuilder addStyleClass(String... styleClass){
        this.styleClass.addAll(Arrays.asList(styleClass));
        return this;
    }

    public ToolButtonBuilder setIconStyleClass(List<String> iconStyleClass) {
        this.iconStyleClass = iconStyleClass;
        return this;
    }

    public ToolButtonBuilder addIconStyleClass(String... iconStyleClass){
        this.iconStyleClass.addAll(Arrays.asList(iconStyleClass));
        return this;
    }

    public ToolButtonBuilder setAction(EventHandler<ActionEvent> action) {
        this.action = action;
        return this;
    }

    public Button button(){
        return (Button) makeButton(new Button());
    }

    public ToggleButton toggleButton(){
        return (ToggleButton)makeButton(new ToggleButton());
    }

    public<T extends ButtonBase> T buttonBase(Supplier<T> factory){
        return (T)makeButton(factory.get());
    }


}
