package eu.fthevenet.binjr.commons.charts;

/**
 * An immutable representation of the selection of the portion of a chart.
 *
 * @author Frederic Thevenet
 */
public class XYChartSelection<X, Y> {
    private final X startX;
    private final X endX;
    private final Y startY;
    private final Y endY;

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
     * Copy constructor for the {@link XYChartSelection} class.
     *
     * @param selection the {@link XYChartSelection} to clone.
     */
    public XYChartSelection(XYChartSelection<X, Y> selection) {
        this.startX = selection.getStartX();
        this.endX = selection.getEndX();
        this.startY = selection.getStartY();
        this.endY = selection.getEndY();
    }

    /**
     * Initializes a new instance of the {@link XYChartSelection} class
     *
     * @param startX the lower bound on the X axis of the selection
     * @param endX   the upper bound on the X axis of the selection
     * @param startY the lower bound on the Y axis of the selection
     * @param endY   the upper bound on the Y axis of the selection
     */
    public XYChartSelection(X startX, X endX, Y startY, Y endY) {
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.getStartX() == null) ? 0 : this.getStartX().hashCode());
        result = prime * result + ((this.getEndX() == null) ? 0 : this.getEndX().hashCode());
        result = prime * result + ((this.getStartY() == null) ? 0 : this.getStartY().hashCode());
        result = prime * result + ((this.getEndY() == null) ? 0 : this.getEndY().hashCode());
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
        if (!evaluatesEquality(this.getStartY(), other.getStartY())) {
            return false;
        }

        return true;
    }

    private boolean evaluatesEquality(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 != null) {
                return false;
            }
        }
        else if (!o1.equals(o2)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("Selection{startX=%s, endX=%s, startY=%s, endY=%s}", getStartX(), getEndX(), getStartY(), getEndY());
    }
}
