package ws.palladian.retrieval.helper;

/**
 * <p>
 * A {@link RequestThrottle} allows to limit the number of actions (typically requests sent to some web API), in order
 * to avoid being blocked because of rate limits.
 * </p>
 * 
 * @author pk
 * 
 */
public interface RequestThrottle {

    /**
     * <p>
     * Check, whether the next request can be performed and record the performed request in the counter. If the request
     * should wait, block (i.e. sleep) for the necessary amount of time.
     * </p>
     */
    void hold();

}
