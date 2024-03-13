package ws.palladian.retrieval.parser;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import ws.palladian.persistence.ParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parser for HTML documents using Jsoup.
 *
 * @author David Urbansky
 */
public final class JsoupParser extends BaseDocumentParser {
    protected JsoupParser() {
        // instances should be created by the factory.
    }

    @Override
    public Document parse(InputSource inputSource) throws ParserException {
        InputStream r = inputSource.getByteStream();
        try {
            r.reset();
            StringBuilder b = new StringBuilder();
            int c;
            while ((c = r.read()) > -1) {
                b.appendCodePoint(c);
            }
            r.reset(); // Reset for possible further actions
            String xml = b.toString();
            org.jsoup.nodes.Document parse = Jsoup.parse(xml);
            return W3CDom.convert(parse);
        } catch (IOException e) {
            throw new ParserException(e);
        }
    }
}
