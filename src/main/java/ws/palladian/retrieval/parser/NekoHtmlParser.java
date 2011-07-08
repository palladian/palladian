package ws.palladian.retrieval.parser;

import java.io.IOException;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * <p>
 * Wrapper for CyberNeko HTML Parser.
 * </p>
 * 
 * @see http://nekohtml.sourceforge.net/
 * @author Philipp Katz
 */
public class NekoHtmlParser extends BaseDocumentParser implements DocumentParser {

    @Override
    public Document parse(InputSource inputSource) throws ParserException {

        DOMParser parser = new DOMParser();

        try {

            // experimental fix for http://redmine.effingo.de/issues/5
            // also see: tud.iir.web.CrawlerTest.testNekoWorkarounds()
            // Philipp, 2010-11-10
            //
            // FIXME 2011-01-06; this seems to cause this problem:
            // http://sourceforge.net/tracker/?func=detail&aid=3109537&group_id=195122&atid=952178
            // catching Throwable in #setDocument above; guess we have to wait for a new Neko release,
            // supposedly breaking other stuff :(
            parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", true);
            parser.setProperty("http://cyberneko.org/html/properties/filters",
                    new XMLDocumentFilter[] { new TBODYFix() });
            // end fix.

            // TODO
            // http://nekohtml.sourceforge.net/faq.html#uppercase
            // parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower")

            parser.parse(inputSource);

        } catch (SAXNotRecognizedException e) {
            throw new ParserException(e);
        } catch (SAXNotSupportedException e) {
            throw new ParserException(e);
        } catch (SAXException e) {
            throw new ParserException(e);
        } catch (IOException e) {
            throw new ParserException(e);
        } catch (Throwable t) {
            // dirty workaround, as NekoHTML produces many kind of unexpected exceptions
            throw new ParserException(t);
        }

        return parser.getDocument();

    }

}
