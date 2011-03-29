package ws.palladian.retrieval;

import org.w3c.dom.Document;

/**
 * An interface for the CrawlerCallback.
 * 
 * @author David Urbansky
 */
public interface CrawlerCallback {

    void crawlerCallback(Document document);

}
