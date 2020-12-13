/*
 *    Copyright 2017-2018 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.common.xml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * A collections of convenience methods to help with serialization and deserialization of XML to and from Java objects.
 *
 * @author Frederic Thevenet
 */
public class XmlUtils {

    public static String getFirstAttributeValue(File file, String attribute) throws IOException, XMLStreamException {
        // Create stream reader
        XMLStreamReader xmlr = XMLInputFactoryHolder.instance.createXMLStreamReader(new FileInputStream(file));
        // Main event loop
        while (xmlr.hasNext()) {
            // Process single event
            switch (xmlr.getEventType()) {
                // Process start tags
                case XMLStreamReader.START_ELEMENT:
                    // Check attributes for first start tag
                    for (int i = 0; i < xmlr.getAttributeCount(); i++) {
                        // Get attribute name
                        String localName = xmlr.getAttributeName(i).getLocalPart();
                        if (localName.equals(attribute)) {
                            // Return value
                            return xmlr.getAttributeValue(i);
                        }
                    }
                    return null;
            }
            // Move to next event
            xmlr.next();
        }
        return null;
    }

    public static <T> T deSerialize(File file, Class<?>... classes) throws JAXBException, IOException {
        try (FileInputStream fin = new FileInputStream(file)) {
            return deSerialize(fin, classes);
        }
    }

    /**
     * Deserialize the XML content of a stream into a Java object of the specified type.
     *
     * @param classes     The classes of the object to unmarshall the XML as
     * @param inputStream An input stream containing the XML to deserialize
     * @param <T>         The type of object to unmarshall the XML as
     * @return The deserialized object
     * @throws JAXBException if an error occurs during deserialization
     */
    public static <T> T deSerialize(InputStream inputStream, Class<?>... classes) throws JAXBException {
        return deSerialize(new StreamSource(inputStream), classes);
    }

    /**
     * Deserialize the XML content of a string into a Java object of the specified type.
     *
     * @param classes   The classes of the object to unmarshall the XML as
     * @param xmlString The XML to deserialize, as a string
     * @param <T>       The type of object to unmarshall the XML as
     * @return The deserialized object
     * @throws JAXBException if an error occurs during deserialization
     */
    public static <T> T deSerialize(String xmlString, Class<?>... classes) throws JAXBException {
        return deSerialize(new StreamSource(new StringReader(xmlString)), classes);
    }

    private static <T> T deSerialize(StreamSource source, Class<?>... classes) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(classes);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        return (T) unmarshaller.unmarshal(source);
    }

    /**
     * Returns a {@link SAXSource} for the provided {@link InputStream} that explicitly forfeit external DTD validation
     *
     * @param in the {@link InputStream} for the {@link SAXSource}
     * @return a {@link SAXSource} for the provided {@link InputStream} that explicitly forfeit external DTD validation
     * @throws SAXException                 if a SAX error occurs
     * @throws ParserConfigurationException if a configuration error occurs
     */
    public static Source toNonValidatingSAXSource(InputStream in) throws SAXException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        InputSource inputSource = new InputSource(in);
        return new SAXSource(xmlReader, inputSource);
    }

    public static <T> void serialize(T object, Path path, Class<?>... classes) throws JAXBException, IOException {
        serialize(object, path.toFile(), classes);
    }

    public static <T> void serialize(T object, File file, Class<?>... classes) throws JAXBException, IOException {
        try (FileOutputStream fout = new FileOutputStream(file)) {
            serialize(object, fout, classes);
        }
    }

    public static <T> String serialize(T object, Class<?>... classes) throws IOException, JAXBException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            serialize(object, out, classes);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }

    }

    public static <T> void serialize(T object, OutputStream out, Class<?>... classes) throws JAXBException {
        if (classes == null || classes.length == 0) {
            classes = new Class<?>[]{object.getClass()};
        }
        JAXBContext jaxbContext = JAXBContext.newInstance(classes);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(object, out);
    }

    private static class XMLInputFactoryHolder {
        private final static XMLInputFactory instance = XMLInputFactory.newInstance();
    }
}
