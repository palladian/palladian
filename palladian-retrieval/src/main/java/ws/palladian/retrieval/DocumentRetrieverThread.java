package ws.palladian.retrieval;

import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Runnable which allows simultaneous downloads of web pages; the URLs to be processed are taken from the supplied
 * queue, and for each downloaded and parsed document, a {@link RetrieverCallback} is triggered.
 * 
 * @author Philipp Katz
 * 
 */
class DocumentRetrieverThread implements Runnable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DocumentRetrieverThread.class);

    private final BlockingQueue<String> urlQueue;
    private final RetrieverCallback<Document> callback;
    private final DocumentRetriever documentRetriever;

    protected DocumentRetrieverThread(BlockingQueue<String> urlQueue, RetrieverCallback<Document> callback,
            DocumentRetriever documentRetriever) {
        super();
        this.urlQueue = urlQueue;
        this.callback = callback;
        this.documentRetriever = documentRetriever;
    }

    @Override
    public void run() {

        // keep running, until the queue is empty
        while (urlQueue.size() > 0) {

            String url = urlQueue.poll();
            if (url == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error(e);
                }
                continue;
            }

            Document document = documentRetriever.getWebDocument(url);
            if (document != null) {
                callback.onFinishRetrieval(document);
            }
        }

    }

}
