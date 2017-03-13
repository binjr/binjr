package eu.fthevenet.binjr.controls;

import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by FTT2 on 13/02/2017.
 */
public class ColorUtils {
    private static final Logger logger = LogManager.getLogger(ColorUtils.class);

    public static String toRGBAString(Color color){
        return toRGBAString(color, color.getOpacity());
    }

    public static String toRGBAString(Color color, double alpha){
        if (color == null){
            throw  new IllegalArgumentException("Argument color is null");
        }
        if (alpha > 1.0 || alpha < 0) {
            throw new IllegalArgumentException("alpha parameter value is out of bound: " + alpha);
        }

        StringBuilder sb = new StringBuilder("rgba(")
                .append(color.getRed())
                .append(",")
                .append(color.getGreen())
                .append(",")
                .append(color.getBlue())
                .append(",")
                .append(alpha)
                .append(")");

        logger.debug(()-> "RGBA representation = " + sb.toString());
        return sb.toString();
    }

    public static String toHex(Color color, String defaultHexValue) {
        if (defaultHexValue == null) {
            throw new IllegalArgumentException("Default color hex value cannot be null");
        }
        if (color == null) {
            return defaultHexValue;
        }
        return toHex(color);
    }

    public static String toHex(Color color){
        if (color == null){
            throw  new IllegalArgumentException("Argument color is null");
        }
        return toHex(color, color.getOpacity());
    }

    public static String toHex(Color color, double alpha){
        if (color == null){
            throw  new IllegalArgumentException("Argument color is null");
        }
        return String.format("#%02x%02x%02x%02x",
                Math.round(color.getRed()*255) ,
                Math.round(color.getGreen()*255),
                Math.round(color.getBlue()*255),
                Math.round(alpha*255));
    }
}
