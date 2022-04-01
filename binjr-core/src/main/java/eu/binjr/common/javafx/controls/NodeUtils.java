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

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Transform;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Defines a collection of helper methods that work with @{@link Node}
 */
public final class NodeUtils {

    /**
     * Takes a snapshot of the provided node and render an image using the same output scale as it containing stage.
     *
     * @param node the node to take a snapshot of.
     * @return the rendered image.
     */
    public static WritableImage scaledSnapshot(Node node) {
        return scaledSnapshot(node, Color.TRANSPARENT, getOutputScaleX(node), getOutputScaleY(node));
    }

    /**
     * Takes a snapshot of the provided node and render an image using the specified output scale.
     *
     * @param node      the node to take a snapshot of.
     * @param fillColor the fill color.
     * @return the rendered image.
     */
    public static WritableImage scaledSnapshot(Node node, Paint fillColor) {
        return scaledSnapshot(node, fillColor, getOutputScaleX(node), getOutputScaleY(node));
    }

    /**
     * Takes a snapshot of the provided node and render an image using the specified output scale.
     *
     * @param node      the node to take a snapshot of.
     * @param fillColor the fill color.
     * @param scaleX    the X output scale to use.
     * @param scaleY    the Y output scale to use.
     * @return the rendered image.
     */
    public static WritableImage scaledSnapshot(Node node, Paint fillColor, double scaleX, double scaleY) {
        SnapshotParameters spa = new SnapshotParameters();
        spa.setFill(fillColor);
        spa.setTransform(Transform.scale(scaleX, scaleY));
        return node.snapshot(spa, null);
    }

    /**
     * Returns the vertical output scale of the {@link Stage} that contains the provided {@link Node}
     *
     * @param node the node contained by the stage to get the output scale for.
     * @return the vertical output scale
     */
    public static double getOutputScaleX(Node node) {
        var stage = getStage(node);
        return stage == null ? Screen.getPrimary().getOutputScaleX() : stage.getOutputScaleX();
    }

    /**
     * Returns the horizontal output scale of the {@link Stage} that contains the provided {@link Node}
     *
     * @param node the node contained by the stage to get the output scale for.
     * @return the horizontal output scale.
     */
    public static double getOutputScaleY(Node node) {
        var stage = getStage(node);
        return stage == null ? Screen.getPrimary().getOutputScaleY() : stage.getOutputScaleY();
    }

    /**
     * Returns the {@link Stage} instance to which the provided {@link Node} is attached
     *
     * @param node the node to get the stage for.
     * @return the {@link Stage} instance to which the provided {@link Node} is attached
     */
    public static Stage getStage(Node node) {
        if (node != null && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        return null;
    }
}
