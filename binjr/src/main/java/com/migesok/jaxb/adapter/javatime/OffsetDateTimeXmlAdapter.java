package com.migesok.jaxb.adapter.javatime;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code OffsetDateTime} to ISO-8601 string
 * <p>
 * String format details: {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}
 *
 * @see javax.xml.bind.annotation.adapters.XmlAdapter
 * @see OffsetDateTime
 */
public class OffsetDateTimeXmlAdapter extends TemporalAccessorXmlAdapter<OffsetDateTime> {
    public OffsetDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_OFFSET_DATE_TIME, OffsetDateTime::from);
    }
}
