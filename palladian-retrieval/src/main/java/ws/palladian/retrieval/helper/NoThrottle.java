package ws.palladian.retrieval.helper;

/**
 * <p>
 * No-operation {@link RequestThrottle}.
 * </p>
 * 
 * @author pk
 */
public final class NoThrottle implements RequestThrottle {

    public static final NoThrottle INSTANCE = new NoThrottle();

    private NoThrottle() {
        // singleton
    }

    @Override
    public void hold() {
    }

}
