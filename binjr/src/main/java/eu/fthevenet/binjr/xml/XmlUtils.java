package eu.fthevenet.binjr.xml;

import org.xml.sax.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;

/**
 * @author Frederic Thevenet
 */
public class XmlUtils {
    public static  <T> T unmarshal(Class<T> docClass, InputStream inputStream) throws JAXBException {
        return unmarshal(docClass, new StreamSource(inputStream));
    }

    public static  <T> T unmarshal(Class<T> docClass, String xmlString) throws JAXBException {
        return unmarshal(docClass, new StreamSource(new StringReader(xmlString)));
    }

    private static  <T> T unmarshal(Class<T> docClass, StreamSource source) throws JAXBException {
        String packageName = docClass.getPackage().getName();
        JAXBContext jc = JAXBContext.newInstance(packageName);
        Unmarshaller u = jc.createUnmarshaller();
        JAXBElement<T> doc = u.unmarshal(source, docClass);
        return doc.getValue();
    }

    public static Source toNonValidatingSAXSource(InputStream in) throws SAXException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        InputSource inputSource = new InputSource(in);
       return new SAXSource(xmlReader, inputSource);
    }
}
