package com.migesok.jaxb.adapter.javatime;


import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

import static java.util.Objects.requireNonNull;

/**
 * {@code XmlAdapter} mapping any JSR-310 {@code TemporalAccessor} to string using provided {@code DateTimeFormatter}
 * <p>
 * Example:
 * <pre>
 * {@code
 *  public class DottedDateXmlAdapter extends TemporalAccessorXmlAdapter<LocalDate> {
 *      public DottedDateXmlAdapter() {
 *          super(DateTimeFormatter.ofPattern("dd.MM.yyyy"), LocalDate::from);
 *      }
 *  }
 * }
 * </pre>
 *
 * @param <T> mapped temporal type
 * @see XmlAdapter
 * @see TemporalAccessor
 * @see DateTimeFormatter
 */
public class TemporalAccessorXmlAdapter<T extends TemporalAccessor> extends XmlAdapter<String, T> {
    private final DateTimeFormatter formatter;
    private final TemporalQuery<? extends T> temporalQuery;

    /**
     * @param formatter     the formatter for printing and parsing, not null
     * @param temporalQuery the query defining the type to parse to, not null
     */
    public TemporalAccessorXmlAdapter( DateTimeFormatter formatter,
                                       TemporalQuery<? extends T> temporalQuery) {
        this.formatter = requireNonNull(formatter, "formatter must not be null");
        this.temporalQuery = requireNonNull(temporalQuery, "temporal query must not be null");
    }

    @Override
    public T unmarshal(String stringValue) {
        return stringValue != null ? formatter.parse(stringValue, temporalQuery) : null;
    }

    @Override
    public String marshal(T value) {
        return value != null ? formatter.format(value) : null;
    }
}
