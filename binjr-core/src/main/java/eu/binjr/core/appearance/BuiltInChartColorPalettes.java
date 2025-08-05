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

package eu.binjr.core.appearance;

import eu.binjr.common.colors.ColorPalette;
import javafx.scene.paint.Color;

public enum BuiltInChartColorPalettes {
    VIBRANT("Vibrant", new ColorPalette(
            Color.valueOf("#3366CC"),
            Color.valueOf("#66AA00"),
            Color.valueOf("#5555DD"),
            Color.valueOf("#DD5555"),
            Color.valueOf("#FF3737"),
            Color.valueOf("#BB44CC"),
            Color.valueOf("#EF0AEF"),
            Color.valueOf("#22AA99"),
            Color.valueOf("#905BFD"),
            Color.valueOf("#F54882"),
            Color.valueOf("#109618"),
            Color.valueOf("#EE9911"),
            Color.valueOf("#4381BF"),
            Color.valueOf("#D66300"),
            Color.valueOf("#DDDD00"),
            Color.valueOf("#FE3912"),
            Color.valueOf("#0099C6"),
            Color.valueOf("#5054E6")
    )),
    PASTEL("Pastel", new ColorPalette(
            Color.LIGHTBLUE,
            Color.LIGHTCORAL,
            Color.LIGHTCYAN,
            Color.LIGHTGRAY,
            Color.LIGHTGREEN,
            Color.LEMONCHIFFON,
            Color.LAVENDER,
            Color.LIGHTPINK,
            Color.LIGHTSALMON,
            Color.LIGHTSEAGREEN,
            Color.LIGHTSKYBLUE,
            Color.LIGHTSLATEGRAY,
            Color.LIGHTSTEELBLUE,
            Color.LIGHTYELLOW,
            Color.MEDIUMBLUE,
            Color.MEDIUMORCHID,
            Color.MEDIUMPURPLE,
            Color.MEDIUMSEAGREEN,
            Color.MEDIUMSPRINGGREEN,
            Color.MEDIUMTURQUOISE,
            Color.MEDIUMVIOLETRED,
            Color.MINTCREAM)
    ),
    FIRE("Fire", new ColorPalette(
            Color.valueOf("#faffb0"),
            Color.valueOf("#fff20e"),
            Color.valueOf("#ffdc17"),
            Color.valueOf("#ffc319"),
            Color.valueOf("#ffa515"),
            Color.valueOf("#ff820f"),
            Color.valueOf("#ff5a07"),
            Color.valueOf("#ff1500"),
            Color.valueOf("#dc2211"),
            Color.valueOf("#ba2b19"),
            Color.valueOf("#9c2d1c"),
            Color.valueOf("#74291c")
    )),
    ICE("Ice", new ColorPalette(
            Color.valueOf("#e2e6ee"),
            Color.valueOf("#d9dfea"),
            Color.valueOf("#c2cce0"),
            Color.valueOf("#a8b8d6"),
            Color.valueOf("#8da4cf"),
            Color.valueOf("#7290cb"),
            Color.valueOf("#567dca"),
            Color.valueOf("#3969cf"),
            Color.valueOf("#1654d9"),
            Color.valueOf("#002ff0"),
            Color.valueOf("#0022d4"),
            Color.valueOf("#04297c")
    )),
    GRAY_SCALE("Gray Scale", new ColorPalette(
            Color.valueOf("#FFFFFF"),
            Color.valueOf("#E0E0E0"),
            Color.valueOf("#CCCCCC"),
            Color.valueOf("#B8B8B8"),
            Color.valueOf("#8F8F8F"),
            Color.valueOf("#707070"),
            Color.valueOf("#525252"),
            Color.valueOf("#3D3D3D"),
            Color.valueOf("#292929"),
            Color.valueOf("#0A0A0A")
    ));

    private final ColorPalette palette;
    private final String name;

    BuiltInChartColorPalettes(String name, ColorPalette colors) {
        this.palette = colors;
        this.name = name;
    }

    public ColorPalette getPalette() {
        return palette;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
