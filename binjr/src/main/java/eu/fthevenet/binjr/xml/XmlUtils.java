package eu.fthevenet.binjr.xml;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A collections of convenience methods to help with serialization and deserialization of XML to and from Java objects.
 *
 * @author Frederic Thevenet
 */
public class XmlUtils {

    public static <T> T deSerialize(Class<T> docClass, File file) throws JAXBException, IOException {
        try(FileInputStream fin = new FileInputStream(file)) {
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
//        String packageName = docClass.getPackage().getName();
//        JAXBContext jc = JAXBContext.newInstance(packageName);
//        Unmarshaller u = jc.createUnmarshaller();
//        JAXBElement<T> doc = u.unmarshal(source, docClass);
       return JAXB.unmarshal( source, docClass);
        //return doc.getValue();
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
        try(FileOutputStream fout = new FileOutputStream(file)) {
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
