package ws.palladian.retrieval.parser;

import java.io.ByteArrayInputStream;

import org.w3c.dom.Document;

import ws.palladian.retrieval.HttpResult;

public abstract class BaseDocumentParser implements DocumentParser {

    @Override
    public Document parse(HttpResult httpResult) throws ParserException {
        Document document = parse(new ByteArrayInputStream(httpResult.getContent()));
        document.setDocumentURI(httpResult.getUrl());
        return document;
    }

}
