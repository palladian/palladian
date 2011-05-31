package ws.palladian.retrieval.parser;

import java.io.InputStream;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public interface DocumentParser {
    
    Document parse(InputSource inputSource) throws ParserException;
    Document parse(InputStream inputStream) throws ParserException;

}
