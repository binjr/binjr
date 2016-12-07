package eu.fthevenet.binjr.commons.charts;

/**
 * Created by FTT2 on 07/12/2016.
 */
public class SelectionArgs<X, Y> {
    private  X startX;
    private  X endX;
    private  Y startY;
    private  Y endY;

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

    public void setStartX(X startX) {
        this.startX = startX;
    }

    public void setEndX(X endX) {
        this.endX = endX;
    }

    public void setStartY(Y startY) {
        this.startY = startY;
    }

    public void setEndY(Y endY) {
        this.endY = endY;
    }

    public SelectionArgs(SelectionArgs<X,Y> selection){
        this.startX = selection.getStartX();
        this.endX = selection.getEndX();
        this.startY = selection.getStartY();
        this.endY = selection.getEndY();
    }

    public SelectionArgs(X startX, X endX, Y startY, Y endY) {
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SelectionArgs{");
        sb.append("startX=").append(startX);
        sb.append(", endX=").append(endX);
        sb.append(", startY=").append(startY);
        sb.append(", endY=").append(endY);
        sb.append('}');
        return sb.toString();
    }
}
