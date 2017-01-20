package eu.fthevenet.binjr.data.providers.jrds;

/**
 * Created by FTT2 on 20/01/2017.
 */
public class JRDSTreeNode {
    private final String name;

    public JRDSTreeNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
