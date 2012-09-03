package ws.palladian.retrieval.feeds;

public enum FeedTaskResult {

    /**
     * The {@link FeedTask} has not been completed yet.
     */
    OPEN,

    /**
     * The {@link FeedTask} has been completed successfully.
     */
    SUCCESS,

    /**
     * The {@link FeedTask} has been completed successfully, but very slow so the execution time exceeded a warning
     * limit.
     */
    EXECUTION_TIME_WARNING,

    /**
     * The {@link FeedTask} has been completed, but a MISS occurred.
     */
    MISS,

    /**
     * The {@link FeedTask} has been completed, but the feed was unreachable.
     */
    UNREACHABLE,

    /**
     * The {@link FeedTask} has been completed, but the feed was unparsable.
     */
    UNPARSABLE,

    /**
     * An Error occurred processing this {@link FeedTask}.
     */
    ERROR
}
