package ws.palladian.retrieval.parser;

import java.io.IOException;

import nu.validator.htmlparser.dom.HtmlDocumentBuilder;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <p>
 * Validator.nu HTML parser. Quotation from the web page: <i>The Validator.nu HTML Parser is an implementation of the
 * HTML5 parsing algorithm in Java. The parser is designed to work as a drop-in replacement for the XML parser in
 * applications that already support XHTML 1.x content with an XML parser and use SAX, DOM or XOM to interface with the
 * parser. Low-level functionality is provided for applications that wish to perform their own IO and support
 * document.write() with scripting. The parser core compiles on Google Web Toolkit and can be automatically translated
 * into C++. (The C++ translation capability is currently used for porting the parser for use in Gecko.)</i>
 * </p>
 * 
 * @see <a href="http://about.validator.nu/htmlparser/">The Validator.nu HTML Parser</a>
 * @author Philipp Katz
 */
public final class ValidatorNuParser extends BaseDocumentParser {

    protected ValidatorNuParser() {
        // instances should be created by the factory.
    }

    @Override
    public Document parse(InputSource inputSource) throws ParserException {
        try {
            HtmlDocumentBuilder builder = new HtmlDocumentBuilder();
            Document document = builder.parse(inputSource);
            // keep the URL, if it was supplied via the input source
            String systemId = inputSource.getSystemId();
            if (StringUtils.isNotEmpty(systemId)) {
                document.setDocumentURI(systemId);
            }
            return document;
        } catch (SAXException e) {
            throw new ParserException(e);
        } catch (IOException e) {
            throw new ParserException(e);
        }
    }

}
