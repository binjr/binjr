package eu.fthevenet.util.text;

/**
 * Created by FTT2 on 17/03/2017.
 */
public class BinaryPrefixFormat extends PrefixFormat {

    public BinaryPrefixFormat() {
        super(1024,new String[] {"ki", "Mi", "Gi", "Ti", "Pi", "Ei"});
    }
}
