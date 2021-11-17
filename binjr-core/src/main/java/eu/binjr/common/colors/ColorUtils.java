/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.common.colors;

import eu.binjr.common.logging.Logger;
import javafx.scene.paint.Color;

/**
 * Utility methods to convert color encodings
 *
 * @author Frederic Thevenet
 */
public class ColorUtils {
    private static final Logger logger = Logger.create(ColorUtils.class);

    public static String toRGBAString(Color color) {
        return toRGBAString(color, color.getOpacity());
    }

    public static String toRGBAString(Color color, double alpha) {
        if (color == null) {
            throw new IllegalArgumentException("Argument color is null");
        }
        if (alpha > 1.0 || alpha < 0) {
            throw new IllegalArgumentException("alpha parameter value is out of bound: " + alpha);
        }

        StringBuilder sb = new StringBuilder("rgba(")
                .append(color.getRed())
                .append(",")
                .append(color.getGreen())
                .append(",")
                .append(color.getBlue())
                .append(",")
                .append(alpha)
                .append(")");

        logger.debug(() -> "RGBA representation = " + sb.toString());
        return sb.toString();
    }

    public static String toHex(Color color, String defaultHexValue) {
        if (defaultHexValue == null) {
            throw new IllegalArgumentException("Default color hex value cannot be null");
        }
        if (color == null) {
            return defaultHexValue;
        }
        return toHex(color);
    }

    public static String toHex(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Argument color is null");
        }
        return toHex(color, color.getOpacity());
    }

    public static String toHex(Color color, double alpha) {
        if (color == null) {
            throw new IllegalArgumentException("Argument color is null");
        }
        return String.format("#%02x%02x%02x%02x",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255),
                Math.round(alpha * 255));
    }

    public static Color copy(Color color){
        return  Color.color(color.getRed(),
                color.getGreen(),
                color.getBlue(),
                color.getOpacity());
    }
}
