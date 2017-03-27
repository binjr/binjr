package eu.fthevenet.util.xml;

import javafx.scene.paint.Color;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * An {@link XmlAdapter} for {@link Color} objects
 *
 * @author Frederic Thevenet
 */
public class ColorXmlAdapter extends XmlAdapter<String, Color> {
    public ColorXmlAdapter() {
    }

    public Color unmarshal(String stringValue) {
        return Color.valueOf(stringValue);
    }

    public String marshal(Color value) {
        return value.toString();
    }
}