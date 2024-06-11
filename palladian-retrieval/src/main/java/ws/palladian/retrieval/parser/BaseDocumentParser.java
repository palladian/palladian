package ws.palladian.retrieval.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.ParserException;
import ws.palladian.retrieval.HttpResult;

import java.io.*;
import java.nio.charset.Charset;

/**
 * <p>
 * Base implementation for document parsers.
 * </p>
 *
 * @author Philipp Katz
 */
public abstract class BaseDocumentParser implements DocumentParser {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDocumentParser.class);

    @Override
    public Document parse(InputStream inputStream) throws ParserException {
        return parse(new InputSource(inputStream));
    }

    @Override
    public Document parse(HttpResult httpResult) throws ParserException {
        byte[] content = httpResult.getContent();
        if (content.length == 0) {
            throw new ParserException("HttpResult has no content");
        }
        InputSource inputSource = new InputSource(new ByteArrayInputStream(content));

        // detect the encoding in advance, this prevents us from interpreting documents incorrectly
        String charset = httpResult.getCharset();
        boolean supportedCharset = isSupportedCharset(charset);
        if (supportedCharset) {
            inputSource.setEncoding(charset);
        }
        LOGGER.debug("Encoding of HttpResult: {}, is supported: {}", charset, supportedCharset);

        Document document = parse(inputSource);
        // set the (potentially redirected) URL
        String location = CollectionHelper.getLast(httpResult.getLocations());
        document.setDocumentURI(location);

        return document;
    }

    /**
     * Determine, whether the charset is supported, and catch exceptions thrown by {@link Charset#isSupported(String)}.
     *
     * @param charset The charset.
     * @return <code>true</code> in case the charset is supported, false otherwise.
     */
    protected static boolean isSupportedCharset(String charset) {
        if (charset == null) {
            return false;
        }
        try {
            Charset.isSupported(charset);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Document parse(File file) throws ParserException {
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new ParserException("File " + file + " not found", e);
        }
        Document document = parse(inputStream);
        String documentUri = file.toURI().toString();
        document.setDocumentURI(documentUri);
        return document;
    }

}
