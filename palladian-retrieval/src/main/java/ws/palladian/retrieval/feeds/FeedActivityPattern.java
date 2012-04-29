package ws.palladian.retrieval.feeds;

/**
 * <p>
 * Possible activity/update classes for feeds.
 * </p>
 * 
 * @author Philipp Katz
 */
public enum FeedActivityPattern {

    // ////////////////// possible classes for feeds ////////////////////
    /** Feed class is not known yet. */
    CLASS_UNKNOWN(0, "unknown"),

    /** Feed is dead, that is, it does not return a valid document. */
    CLASS_DEAD(1, "dead"),

    /** Feed is alive but has zero entries. */
    CLASS_EMPTY(2, "empty"),

    /** Feed is alive but has only one single entry. */
    CLASS_SINGLE_ENTRY(3, "single entry"),

    /** Feed was active but is not anymore. */
    CLASS_ZOMBIE(4, "zombie"),

    /** Feed posts appear not often and at different intervals. */
    CLASS_SPONTANEOUS(5, "spontaneous"),

    /** Feed posts are done at daytime with a longer gap at night. */
    CLASS_SLICED(6, "sliced"),

    /** Feed posts are 24/7 at a similar interval. */
    CLASS_CONSTANT(7, "constant"),

    /** all posts in the feed are updated together at a certain time */
    CLASS_CHUNKED(8, "chunked"),

    /** All post entries are generated at request time (have publish timestamps) */
    CLASS_ON_THE_FLY(9, "on the fly");

    private final int identifier;
    private final String className;

    FeedActivityPattern(int identifier, String className) {
        this.identifier = identifier;
        this.className = className;
    }

    /**
     * <p>
     * Get a {@link FeedActivityPattern} by its identifier.
     * </p>
     * 
     * @param identifier
     * @return The FeedActivityPattern with the specified identifier.
     * @throws IllegalArgumentException in case no FeedActivityPattern with the specified identifier exists.
     */
    public static FeedActivityPattern fromIdentifier(int identifier) {
        for (FeedActivityPattern pattern : values()) {
            if (pattern.getIdentifier() == identifier) {
                return pattern;
            }
        }
        throw new IllegalArgumentException("No FeedActivityPattern with identifier " + identifier);
    }

    public int getIdentifier() {
        return identifier;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return getClassName();
    }

}
