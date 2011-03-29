package ws.palladian.retrieval.feeds;

public class FeedDownloaderException extends Exception {

    private static final long serialVersionUID = -8787100315945118852L;

    public FeedDownloaderException(Throwable t) {
        super(t);
    }

    public FeedDownloaderException(String string) {
        super(string);
    }

}
