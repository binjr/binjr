package eu.fthevenet.binjr.dialogs;

/**
 * An enumeration of supported user interface themes
 *
 * @author Frederic Thevenet
 */
public enum UserInterfaceThemes {
    MODERN("Modern", "/css/Modern.css"),
    CLASSIC("Classic", "/css/Classic.css");

    private final String cssPath;
    private String label;

    UserInterfaceThemes(String label, String cssPath) {
        this.label = label;
        this.cssPath = cssPath;
    }

    public String getCssPath() {
        return cssPath;
    }

    @Override
    public String toString() {
        return label;
    }


}
