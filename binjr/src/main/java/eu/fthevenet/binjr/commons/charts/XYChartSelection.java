package eu.fthevenet.binjr.commons.charts;

/**
 * Created by FTT2 on 08/12/2016.
 */
public  class XYChartSelection<X, Y> {
    private final X startX;
    private final X endX;
    private final Y startY;
    private final Y endY;

    public X getStartX() {
        return startX;
    }

    public X getEndX() {
        return endX;
    }

    public Y getStartY() {
        return startY;
    }

    public Y getEndY() {
        return endY;
    }

    public XYChartSelection(XYChartSelection<X,Y> selection){
        this.startX = selection.getStartX();
        this.endX = selection.getEndX();
        this.startY = selection.getStartY();
        this.endY = selection.getEndY();
    }

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
        //Same ref -> equal
        if (this == obj) {
            return true;
        }
        // target is null, this instance obviously isn't -> not equal
        if (obj == null) {
            return false;
        }
        // not the same class -> not equal
        if (getClass() != obj.getClass()) {
            return false;
        }
        // Evaluates all public properties' value
        XYChartSelection other = (XYChartSelection) obj;
        if (!evaluatesEquality(this.getEndX(), other.getEndX())){
            return false;
        }
        if (!evaluatesEquality(this.getStartX(), other.getStartX())){
            return false;
        }
        if (!evaluatesEquality(this.getEndY(), other.getEndY())){
            return false;
        }
        if (!evaluatesEquality(this.getStartY(), other.getStartY())){
            return false;
        }
        // All public properties have the same value -> equal
        return true;
    }

    private boolean evaluatesEquality(Object o1, Object o2){
        if (o1 ==  null) {
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
