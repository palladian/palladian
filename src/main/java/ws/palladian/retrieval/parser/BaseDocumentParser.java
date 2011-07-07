package ws.palladian.retrieval.parser;

import java.io.ByteArrayInputStream;

import org.w3c.dom.Document;

import ws.palladian.retrieval.HttpResult;

public abstract class BaseDocumentParser implements DocumentParser {

    @Override
    public Document parse(HttpResult httpResult) throws ParserException {
        byte[] content = httpResult.getContent();
        if (content.length == 0) {
            throw new ParserException("HttpResult has no content");
        }
        Document document = parse(new ByteArrayInputStream(content));
        document.setDocumentURI(httpResult.getUrl());
        return document;
    }

}
