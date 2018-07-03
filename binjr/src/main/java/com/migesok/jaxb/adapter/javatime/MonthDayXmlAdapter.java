package com.migesok.jaxb.adapter.javatime;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.MonthDay;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code MonthDay} to a string such as --12-03
 * <p>
 * String format details:
 * <ul>
 * <li>{@link MonthDay#parse(CharSequence)}</li>
 * <li>{@link MonthDay#toString()}</li>
 * </ul>
 *
 * @see XmlAdapter
 * @see MonthDay
 */
public class MonthDayXmlAdapter extends XmlAdapter<String, MonthDay> {
    @Override
    public MonthDay unmarshal(String stringValue) {
        return stringValue != null ? MonthDay.parse(stringValue) : null;
    }

    @Override
    public String marshal(MonthDay value) {
        return value != null ? value.toString() : null;
    }
}
