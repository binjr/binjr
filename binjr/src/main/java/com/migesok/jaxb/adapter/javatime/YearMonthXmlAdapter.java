package com.migesok.jaxb.adapter.javatime;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.YearMonth;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code YearMonth} to a string such as 2007-12
 * <p>
 * String format details:
 * <ul>
 * <li>{@link YearMonth#parse(CharSequence)}</li>
 * <li>{@link YearMonth#toString()}</li>
 * </ul>
 *
 * @see XmlAdapter
 * @see YearMonth
 */
public class YearMonthXmlAdapter extends XmlAdapter<String, YearMonth> {
    @Override
    public YearMonth unmarshal(String stringValue) {
        return stringValue != null ? YearMonth.parse(stringValue) : null;
    }

    @Override
    public String marshal(YearMonth value) {
        return value != null ? value.toString() : null;
    }
}
