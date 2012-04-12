package ws.palladian.retrieval.feeds.parser;

public class FeedParserException extends Exception {

    private static final long serialVersionUID = -8787100315945118852L;

    public FeedParserException(Throwable t) {
        super(t);
    }

    public FeedParserException(String string) {
        super(string);
    }

    /**
     * @param message
     * @param cause
     */
    public FeedParserException(String message, Throwable cause) {
        super(message, cause);
    }
    
    

}
