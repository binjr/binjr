package com.migesok.jaxb.adapter.javatime;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code LocalTime} to ISO-8601 string
 * <p>
 * String format details: {@link DateTimeFormatter#ISO_LOCAL_TIME}
 *
 * @see javax.xml.bind.annotation.adapters.XmlAdapter
 * @see LocalTime
 */
public class LocalTimeXmlAdapter extends TemporalAccessorXmlAdapter<LocalTime> {
    public LocalTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_LOCAL_TIME, LocalTime::from);
    }
}
