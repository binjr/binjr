package com.migesok.jaxb.adapter.javatime;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code LocalDate} to ISO-8601 string
 * <p>
 * It uses {@link DateTimeFormatter#ISO_DATE} for parsing and serializing,
 * time-zone information ignored.
 *
 * @see javax.xml.bind.annotation.adapters.XmlAdapter
 * @see LocalDate
 */
public class LocalDateXmlAdapter extends TemporalAccessorXmlAdapter<LocalDate> {
    public LocalDateXmlAdapter() {
        super(DateTimeFormatter.ISO_DATE, LocalDate::from);
    }
}
