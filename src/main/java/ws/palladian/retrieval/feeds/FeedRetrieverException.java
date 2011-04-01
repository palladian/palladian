package ws.palladian.retrieval.feeds;

public class FeedRetrieverException extends Exception {

    private static final long serialVersionUID = -8787100315945118852L;

    public FeedRetrieverException(Throwable t) {
        super(t);
    }

    public FeedRetrieverException(String string) {
        super(string);
    }

}
