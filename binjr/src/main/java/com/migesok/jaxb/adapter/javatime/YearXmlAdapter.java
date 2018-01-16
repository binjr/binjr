package com.migesok.jaxb.adapter.javatime;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Year;

/**
 * {@code XmlAdapter} mapping JSR-310 {@code Year} to ISO proleptic year number
 * <p>
 * Year number interpretation details:
 * <ul>
 * <li>{@link Year#of(int)}</li>
 * <li>{@link Year#getValue()}</li>
 * </ul>
 *
 * @see XmlAdapter
 * @see Year
 */
public class YearXmlAdapter extends XmlAdapter<Integer, Year> {
    @Override
    public Year unmarshal(Integer isoYearInt) {
        return isoYearInt != null ? Year.of(isoYearInt) : null;
    }

    @Override
    public Integer marshal(Year year) {
        return year != null ? year.getValue() : null;
    }
}
