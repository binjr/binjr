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
        String[] rgba = stringValue.split(";");
        return new Color(
                Double.parseDouble(rgba[0]),
                Double.parseDouble(rgba[1]),
                Double.parseDouble(rgba[2]),
                Double.parseDouble(rgba[3])
                );
    }

    public String marshal(Color value) {
        return String.format("%f;%f;%f;%f", value.getRed(), value.getGreen(), value.getBlue(), value.getOpacity());
    }
}