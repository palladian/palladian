package ws.palladian.retrieval.search;

/**
 * <p>
 * Exception which can be thrown when a {@link Searcher} encounters an error.
 * </p>
 * 
 * @author Philipp Katz
 */
public class SearcherException extends Exception {

    private static final long serialVersionUID = 1L;

    public SearcherException(String message) {
        super(message);
    }

    public SearcherException(Throwable cause) {
        super(cause);
    }

    public SearcherException(String message, Throwable cause) {
        super(message, cause);
    }

}
