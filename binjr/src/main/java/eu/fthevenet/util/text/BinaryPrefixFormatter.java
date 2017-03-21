package eu.fthevenet.util.text;

/**
 * An implementation of {@link PrefixFormatter} for binary prefixes
 *
 * @author Frederic Thevenet
 */
public class BinaryPrefixFormatter extends PrefixFormatter {
    /**
     * Initializes a new instance of the {@link BinaryPrefixFormatter} class
     */
    public BinaryPrefixFormatter() {
        super(1024,new String[] {"ki", "Mi", "Gi", "Ti", "Pi", "Ei"});
    }
}
