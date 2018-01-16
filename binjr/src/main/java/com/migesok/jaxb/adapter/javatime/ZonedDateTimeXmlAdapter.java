package com.migesok.jaxb.adapter.javatime;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code ZonedDateTime} to ISO-8601 string
 * <p>
 * String format details: {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}
 *
 * @see javax.xml.bind.annotation.adapters.XmlAdapter
 * @see ZonedDateTime
 */
public class ZonedDateTimeXmlAdapter extends TemporalAccessorXmlAdapter<ZonedDateTime> {
    public ZonedDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_ZONED_DATE_TIME, ZonedDateTime::from);
    }
}
