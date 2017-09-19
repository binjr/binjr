/* 
 * Copyright 2014 Jens Deters.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fthevenet.util.javafx.controls;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Robot;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

/**
 * @author Jens Deters (www.jensd.de)
 * @version 1.0.0
 * @since 14-10-2014
 */
public class MouseRobot {

    private static final Robot robot = Application.GetApplication().createRobot();

    public static Point2D getMousePosition() {
        return new Point2D(robot.getMouseX(), robot.getMouseY());
    }

    public static Point2D getMouseOnScreenPosition(MouseEvent event) {
        return new Point2D(event.getScreenX(), event.getScreenY());
    }

    public static Point2D getMouseInScenePosition(MouseEvent event) {
        return new Point2D(event.getSceneX(), event.getSceneY());
    }

}
