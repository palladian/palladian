package ws.palladian.retrieval.feeds.parser;

import ws.palladian.retrieval.parser.ParserException;

public class FeedParserException extends ParserException {

    private static final long serialVersionUID = -8787100315945118852L;

    public FeedParserException(Throwable t) {
        super(t);
    }

    public FeedParserException(String string) {
        super(string);
    }

    public FeedParserException(String message, Throwable cause) {
        super(message, cause);
    }

}
