package com.migesok.jaxb.adapter.javatime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code LocalDateTime} to ISO-8601 string
 * <p>
 * String format details: {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
 *
 * @see javax.xml.bind.annotation.adapters.XmlAdapter
 * @see LocalDateTime
 */
public class LocalDateTimeXmlAdapter extends TemporalAccessorXmlAdapter<LocalDateTime> {
    public LocalDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::from);
    }
}
