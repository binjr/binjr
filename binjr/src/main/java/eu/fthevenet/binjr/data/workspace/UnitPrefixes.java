package eu.fthevenet.binjr.data.workspace;

/**
 * An enumeration of the support unit prefixes
 *
 * @author Frederic Thevenet
 */
public enum UnitPrefixes {
    METRIC("Metric"),
    BINARY("Binary");

    private String label;

    UnitPrefixes(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
