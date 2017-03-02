@XmlJavaTypeAdapters({
        @XmlJavaTypeAdapter(type=ZonedDateTime.class,
                value=ZonedDateTimeXmlAdapter.class),
        @XmlJavaTypeAdapter(type=ZoneId.class,
                value=ZoneIdXmlAdapter.class),
        @XmlJavaTypeAdapter(type=LocalDateTime.class,
                value=LocalDateTimeXmlAdapter.class),
        @XmlJavaTypeAdapter(type=Instant.class,
                value=InstantXmlAdapter.class),
})
package eu.fthevenet.binjr.data.workspace;

import com.migesok.jaxb.adapter.javatime.InstantXmlAdapter;
import com.migesok.jaxb.adapter.javatime.LocalDateTimeXmlAdapter;
import com.migesok.jaxb.adapter.javatime.ZoneIdXmlAdapter;
import com.migesok.jaxb.adapter.javatime.ZonedDateTimeXmlAdapter;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;