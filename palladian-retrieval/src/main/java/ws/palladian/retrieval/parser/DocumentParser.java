package ws.palladian.retrieval.parser;

import java.io.File;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * Interface for document parsers. A {@link DocumentParser} takes various types of inputs and produces a
 * {@link Document}.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface DocumentParser {

    /**
     * <p>
     * Parse a {@link Document} from the provided {@link InputSource}.
     * </p>
     * 
     * @param inputSource
     * @return
     * @throws ParserException In case, parsing fails.
     */
    Document parse(InputSource inputSource) throws ParserException;

    /**
     * <p>
     * Parse a {@link Document} from the provided {@link InputStream}.
     * </p>
     * 
     * @param inputStream
     * @return
     * @throws ParserException In case, parsing fails.
     */
    Document parse(InputStream inputStream) throws ParserException;

    /**
     * <p>
     * Parse a {@link Document} from the provided {@link HttpResult}, which can be obtained via
     * {@link DocumentRetriever}.
     * </p>
     * 
     * @param httpResult
     * @return
     * @throws ParserException In case, parsing fails.
     */
    Document parse(HttpResult httpResult) throws ParserException;

    /**
     * <p>
     * Parse a {@link Document} from the provided {@link File}.
     * </p>
     * 
     * @param file
     * @return
     * @throws ParserException In case, parsing fails.
     */
    Document parse(File file) throws ParserException;

}
