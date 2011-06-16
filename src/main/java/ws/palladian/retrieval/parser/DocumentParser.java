package ws.palladian.retrieval.parser;

import java.io.InputStream;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import ws.palladian.retrieval.HttpResult;

public interface DocumentParser {
    
    Document parse(InputSource inputSource) throws ParserException;
    Document parse(InputStream inputStream) throws ParserException;
    Document parse(HttpResult httpResult) throws ParserException;

}
