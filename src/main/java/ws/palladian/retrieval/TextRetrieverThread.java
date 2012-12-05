package ws.palladian.retrieval;

import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

/**
 * <p>
 * Runnable which allows simultaneous downloads of web pages; the URLs to be processed are taken from the supplied
 * queue, and for each downloaded document, a {@link RetrieverCallback} is triggered.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
class TextRetrieverThread implements Runnable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TextRetrieverThread.class);

    private final BlockingQueue<String> urlQueue;
    private final RetrieverCallback<String> callback;
    private final DocumentRetriever documentRetriever;

    protected TextRetrieverThread(BlockingQueue<String> urlQueue, RetrieverCallback<String> callback,
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

            String text = documentRetriever.getText(url);
            if (text != null) {
                callback.onFinishRetrieval(text);
            }
        }

    }

}
