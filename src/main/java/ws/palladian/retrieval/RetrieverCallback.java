package ws.palladian.retrieval;

import org.w3c.dom.Document;

/**
 * An interface for the RetrieverCallback.
 * 
 * @author David Urbansky
 */
public interface RetrieverCallback {

    void onFinishRetrieval(Document document);

}
