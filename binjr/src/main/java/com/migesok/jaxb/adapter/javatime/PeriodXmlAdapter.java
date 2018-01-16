package com.migesok.jaxb.adapter.javatime;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Period;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code Period} to ISO-8601 string
 * <p>
 * String format details:
 * <ul>
 * <li>{@link Period#parse(CharSequence)}</li>
 * <li>{@link Period#toString()}</li>
 * </ul>
 *
 * @see XmlAdapter
 * @see Period
 */
public class PeriodXmlAdapter extends XmlAdapter<String, Period> {
    @Override
    public Period unmarshal(String stringValue) {
        return stringValue != null ? Period.parse(stringValue) : null;
    }

    @Override
    public String marshal(Period value) {
        return value != null ? value.toString() : null;
    }
}
