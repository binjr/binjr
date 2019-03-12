/*
 *    Copyright 2016-2019 Frederic Thevenet
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

package eu.binjr.common.javafx.charts;

/**
 * An immutable representation of the selection of the portion of a chart.
 *
 * @author Frederic Thevenet
 */
public class XYChartSelection<X, Y> {
    private static final int PRIME = 31;
    private final X startX;
    private final X endX;
    private final Y startY;
    private final Y endY;

    private final boolean autoRangeY;

    /**
     * Returns the lower bound on the X axis of the selection
     *
     * @return the lower bound on the X axis of the selection
     */
    public X getStartX() {
        return startX;
    }

    /**
     * Returns the upper bound on the X axis of the selection
     *
     * @return the upper bound on the X axis of the selection
     */
    public X getEndX() {
        return endX;
    }

    /**
     * Returns the lower bound on the Y axis of the selection
     *
     * @return the lower bound on the Y axis of the selection
     */
    public Y getStartY() {
        return startY;
    }

    /**
     * Returns the upper bound on the Y axis of the selection
     *
     * @return the upper bound on the Y axis of the selection
     */
    public Y getEndY() {
        return endY;
    }

    /**
     * Return true is auto range is enabled on the Y axis, false otherwise.
     *
     * @return true is auto range is enabled on the Y axis, false otherwise.
     */
    public boolean isAutoRangeY() {
        return autoRangeY;
    }

    /**
     * Copy constructor for the {@link XYChartSelection} class.
     *
     * @param selection the {@link XYChartSelection} to clone.
     */
    public XYChartSelection(XYChartSelection<X, Y> selection) {
        this.startX = selection.getStartX();
        this.endX = selection.getEndX();
        this.startY = selection.getStartY();
        this.endY = selection.getEndY();
        this.autoRangeY = selection.isAutoRangeY();
    }

    /**
     * Initializes a new instance of the {@link XYChartSelection} class
     *
     * @param startX     the lower bound on the X axis of the selection
     * @param endX       the upper bound on the X axis of the selection
     * @param startY     the lower bound on the Y axis of the selection
     * @param endY       the upper bound on the Y axis of the selection
     * @param autoRangeY set to true to adapt the range on the Y axis automatically
     */
    public XYChartSelection(X startX, X endX, Y startY, Y endY, boolean autoRangeY) {
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
        this.autoRangeY = autoRangeY;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = PRIME * result + ((this.getStartX() == null) ? 0 : this.getStartX().hashCode());
        result = PRIME * result + ((this.getEndX() == null) ? 0 : this.getEndX().hashCode());
        result = PRIME * result + ((this.getStartY() == null) ? 0 : this.getStartY().hashCode());
        result = PRIME * result + ((this.getEndY() == null) ? 0 : this.getEndY().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        XYChartSelection other = (XYChartSelection) obj;
        if (!evaluatesEquality(this.getEndX(), other.getEndX())) {
            return false;
        }
        if (!evaluatesEquality(this.getStartX(), other.getStartX())) {
            return false;
        }
        if (!evaluatesEquality(this.getEndY(), other.getEndY())) {
            return false;
        }
        return evaluatesEquality(this.getStartY(), other.getStartY());
    }

    private boolean evaluatesEquality(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else return o1.equals(o2);
    }

    @Override
    public String toString() {
        return String.format("Selection{startX=%s, endX=%s, startY=%s, endY=%s}", getStartX(), getEndX(), getStartY(), getEndY());
    }
}
