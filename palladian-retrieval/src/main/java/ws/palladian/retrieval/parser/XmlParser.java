package ws.palladian.retrieval.parser;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.syndication.io.XmlReader;

/**
 * <p>
 * Parser for XML documents, which provides some additional sanitizing capabilities concerning illegal characters, etc.
 * </p>
 * 
 * @author Philipp Katz
 */
public class XmlParser extends BaseDocumentParser implements DocumentParser {

    protected XmlParser() {
        // instances should be created by the factory.
    }

    @Override
    public Document parse(InputSource inputSource) throws ParserException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            // added by Philipp, 2011-01-28
            docBuilderFactory.setNamespaceAware(true);

            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            return docBuilder.parse(inputSource);
        } catch (ParserConfigurationException e) {
            throw new ParserException(e);
        } catch (IOException e) {
            throw new ParserException(e);
        } catch (SAXException e) {
            throw new ParserException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ParserException(e);
        } catch (Throwable th) {
            throw new ParserException(th);
        }
    }

    @Override
    public Document parse(InputStream inputStream) throws ParserException {

        try {
            // fix to parse XML documents with illegal characters, 2011-02-15
            // see http://java.net/projects/rome/lists/users/archive/2009-04/message/12
            // and http://info.tsachev.org/2009/05/skipping-invalid-xml-character-with.html
            // although XmlReader is from ROME, I suppose it can be used for general XML applications
            XmlReader xmlReader = new XmlReader(inputStream);
            Xml10FilterReader filterReader = new Xml10FilterReader(xmlReader);
            InputSource inputSource = new InputSource(filterReader);
            // LOGGER.debug("encoding : " + xmlReader.getEncoding());
            // end fix.

            return parse(inputSource);

        } catch (IOException e) {
            throw new ParserException(e);
        }
    }
}