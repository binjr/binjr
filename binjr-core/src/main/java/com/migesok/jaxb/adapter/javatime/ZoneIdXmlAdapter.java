package com.migesok.jaxb.adapter.javatime;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.ZoneId;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code ZoneId} and {@code ZoneOffset} to the time-zone ID string
 * <p>
 * Time-zone ID format details:
 * <ul>
 * <li>{@link ZoneId#of(String)}</li>
 * <li>{@link ZoneId#getId()}</li>
 * </ul>
 *
 * @see XmlAdapter
 * @see ZoneId
 * @see java.time.ZoneOffset
 */
public class ZoneIdXmlAdapter extends XmlAdapter<String, ZoneId> {
    @Override
    public ZoneId unmarshal(String stringValue) {
        return stringValue != null ? ZoneId.of(stringValue) : null;
    }

    @Override
    public String marshal(ZoneId value) {
        return value != null ? value.getId() : null;
    }
}
