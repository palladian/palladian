package ws.palladian.helper;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ws.palladian.helper.io.ResourceHelper;

/**
 * <p>
 * Utility for parsing valid XHTML, only intended for testing. For real-life use, resort to the HTML parser provided in
 * the palladian-retrieval module.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class ParseUtil {

    public static final Document parseXhtml(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        documentBuilder.setEntityResolver(new XhtmlEntityResolver());
        return documentBuilder.parse(file);
    }

    private static final class XhtmlEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            String fileName = systemId.substring(systemId.lastIndexOf("/"));
            return new InputSource(ResourceHelper.getResourceStream(fileName));
        }

    }

    private ParseUtil() {
        // no instances.
    }

}
