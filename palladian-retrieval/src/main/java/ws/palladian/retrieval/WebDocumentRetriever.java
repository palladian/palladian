package ws.palladian.retrieval;

import org.w3c.dom.Document;

/**
 * Created by David Urbansky on 07.10.2017.
 */
public interface WebDocumentRetriever {

    Document getWebDocument(String url);
}
