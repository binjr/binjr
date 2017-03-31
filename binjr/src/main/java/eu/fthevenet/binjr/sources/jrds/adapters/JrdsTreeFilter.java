package eu.fthevenet.binjr.sources.jrds.adapters;

/**
 * An enumeration of JRDS tree tabs.
 *
 * @author Frederic Thevenet
 */
public enum JrdsTreeFilter {
    FILTER_TAB("All Filters", "filtertab"),
    SERVICE_TAB("All Services", "servicestab"),
    VIEWS_TAB("All Views", "viewstab"),
    HOSTS_TAB("All Hosts", "hoststab"),
    TAGS_TAB("All Tags", "tagstab");

    private String label;
    private String command;


    JrdsTreeFilter(String label, String command) {
        this.label = label;
        this.command = command;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Gets the command keyword associated with the enum entry
     *
     * @return the command keyword associated with the enum entry
     */
    public String getCommand() {
        return command;
    }
}
