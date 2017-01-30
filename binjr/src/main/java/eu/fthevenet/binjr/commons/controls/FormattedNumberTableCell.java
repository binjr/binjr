package eu.fthevenet.binjr.commons.controls;


import java.text.Format;
import java.util.Locale;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;


/**
 * Created by FTT2 on 30/01/2017.
 */
public class FormattedNumberTableCell<S, T extends Number> extends TextFieldTableCell<S,T> {

    private Format format;

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }


    public FormattedNumberTableCell(){
        super((StringConverter<T>)new NumberStringConverter(Locale.getDefault(Locale.Category.FORMAT)));
    }
}

