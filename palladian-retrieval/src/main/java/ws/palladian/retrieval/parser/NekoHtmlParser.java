package ws.palladian.retrieval.parser;

import java.io.IOException;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.HTMLTagBalancerFixed;
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
 * @see <a href="CyberNeko HTML Parser">http://nekohtml.sourceforge.net/</a>
 * @author Philipp Katz
 */
public final class NekoHtmlParser extends BaseDocumentParser implements DocumentParser {

    @Override
    public Document parse(InputSource inputSource) throws ParserException {

        DOMParser parser = new DOMParser(new HTMLConfiguration());

        try {

            parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");

            parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", true);

            // fix for issue where inserted elements did not get the correct namespace
            // implemented in HTMLTagBalancerFixed for now. For more information see
            // https://bitbucket.org/palladian/palladian/issue/29/tr-fix-for-neko-html
            // https://sourceforge.net/tracker/?func=detail&aid=3151253&group_id=195122&atid=952178
            parser.setFeature("http://cyberneko.org/html/features/balance-tags", false);
            XMLDocumentFilter[] filters = { new HTMLTagBalancerFixed(), new PreflightFilter() };
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

            // per dafault, assume UTF-8 encoding (makes more sense than Windows-1252 default)
            parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "UTF-8");

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
            // NekoHTML produces many kind of unexpected exceptions, we can catch them here.
            throw new ParserException(t);
        }

        return parser.getDocument();

    }

}
