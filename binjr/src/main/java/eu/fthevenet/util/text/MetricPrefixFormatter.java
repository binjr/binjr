package eu.fthevenet.util.text;

/**
 * An implementation of {@link PrefixFormatter} for metric prefixes
 *
 * @author Frederic Thevenet
 */
public class MetricPrefixFormatter extends PrefixFormatter {

    /**
     * Initializes a new instance of the {@link MetricPrefixFormatter} class
     */
    public MetricPrefixFormatter() {
        super(1000, new String[]{"k", "M", "G", "T", "P", "E"});
    }
}
