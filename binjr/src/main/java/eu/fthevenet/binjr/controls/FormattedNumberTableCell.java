package eu.fthevenet.binjr.controls;


import java.text.Format;
import java.util.Locale;

import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;


/**
 * Extends a TextFieldTableCell to override the StringConverter and define the conversion format.
 *
 * @author Frederic Thevenet
 */
public class FormattedNumberTableCell<S, T extends Number> extends TextFieldTableCell<S,T> {

    private Format format;

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    //TODO complete support for specifying the format
    FormattedNumberTableCell(){
        super((StringConverter<T>)new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
    }
}

