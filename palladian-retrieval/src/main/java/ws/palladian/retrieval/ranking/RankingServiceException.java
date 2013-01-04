package ws.palladian.retrieval.ranking;

/**
 * <p>
 * An exception which is thrown in case the retrieval from a {@link RankingService} fails.
 * </p>
 * 
 * @author Philipp Katz
 */
public class RankingServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    public RankingServiceException(String message) {
        super(message);
    }

    public RankingServiceException(Throwable cause) {
        super(cause);
    }

    public RankingServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
