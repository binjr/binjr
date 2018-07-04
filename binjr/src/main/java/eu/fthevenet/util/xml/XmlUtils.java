/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.util.xml;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Path;

/**
 * A collections of convenience methods to help with serialization and deserialization of XML to and from Java objects.
 *
 * @author Frederic Thevenet
 */
public class XmlUtils {

    private static class XMLInputFactoryHolder {
        private final static XMLInputFactory instance = XMLInputFactory.newInstance();
    }

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

    public static <T> T deSerialize(Class<T> docClass, File file) throws JAXBException, IOException {
        try (FileInputStream fin = new FileInputStream(file)) {
            return deSerialize(docClass, fin);
        }
    }

    /**
     * Deserialize the XML content of a stream into a Java object of the specified type.
     *
     * @param docClass    The class of the object to unmarshall the XML as
     * @param inputStream An input stream containing the XML to deserialize
     * @param <T>         The type of object to unmarshall the XML as
     * @return The deserialized object
     * @throws JAXBException if an error occurs during deserialization
     */
    public static <T> T deSerialize(Class<T> docClass, InputStream inputStream) throws JAXBException {
        return deSerialize(docClass, new StreamSource(inputStream));
    }

    /**
     * Deserialize the XML content of a string into a Java object of the specified type.
     *
     * @param docClass  The class of the object to unmarshall the XML as
     * @param xmlString The XML to deserialize, as a string
     * @param <T>       The type of object to unmarshall the XML as
     * @return The deserialized object
     * @throws JAXBException if an error occurs during deserialization
     */
    public static <T> T deSerialize(Class<T> docClass, String xmlString) throws JAXBException {
        return deSerialize(docClass, new StreamSource(new StringReader(xmlString)));
    }

    private static <T> T deSerialize(Class<T> docClass, StreamSource source) throws JAXBException {
        return JAXB.unmarshal(source, docClass);
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

    public static <T> void serialize(T object, Path path) throws JAXBException, IOException {
        serialize(object, path.toFile());
    }

    public static <T> void serialize(T object, File file) throws JAXBException, IOException {
        try (FileOutputStream fout = new FileOutputStream(file)) {
            serialize(object, fout);
        }
    }

    public static <T> void serialize(T object, OutputStream out) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(object, out);
    }
}
