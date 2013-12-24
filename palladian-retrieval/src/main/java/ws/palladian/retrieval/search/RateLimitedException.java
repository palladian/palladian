package ws.palladian.retrieval.search;

/**
 * <p>
 * Exception, when the usage quota for a Web-based {@link Searcher} is exhausted.
 * </p>
 * 
 * @author pk
 * 
 */
public class RateLimitedException extends SearcherException {

    private static final long serialVersionUID = 1L;

    private final Integer timeUntilReset;

    /**
     * <p>
     * Create a new {@link RateLimitedException}.
     * 
     * @param message The message, not <code>null</code> or empty.
     * @param timeUntilReset The time in seconds, until the block expires. May be <code>null</code> in case this
     *            information is not available.
     */
    public RateLimitedException(String message, Integer timeUntilReset) {
        super(message);
        this.timeUntilReset = timeUntilReset;
    }

    public RateLimitedException(String message) {
        this(message, null);
    }

    /**
     * @return The time in seconds, until the block expires. <code>null</code> in case this information is not provided.
     */
    public Integer getTimeUntilReset() {
        return timeUntilReset;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RateLimitedException [");
        if (timeUntilReset != null) {
            builder.append("timeUntilReset=");
            builder.append(timeUntilReset);
        }
        builder.append("]");
        return builder.toString();
    }

}
