package eu.fthevenet.binjr.data.workspace;

/**
 * An enumeration of the type of charts supported.
 *
 * @author Frederic Thevenet
 */
public enum ChartType {
    STACKED("Stacked area chart"),
    AREA("Area chart"),
    LINE("Line chart");

    private String label;

    ChartType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
