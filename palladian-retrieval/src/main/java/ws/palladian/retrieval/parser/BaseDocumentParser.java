package ws.palladian.retrieval.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * Base implementation for document parsers.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class BaseDocumentParser implements DocumentParser {

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

        // detect the encoding in advance, this avoids wrongly interpreted documents
        String charset = httpResult.getCharset();
        if (charset != null && Charset.isSupported(charset)) {
            inputSource.setEncoding(charset);
        }

        Document document = parse(inputSource);
        document.setDocumentURI(httpResult.getUrl());
        return document;
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
