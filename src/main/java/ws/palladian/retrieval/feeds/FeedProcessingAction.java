package ws.palladian.retrieval.feeds;

public abstract class FeedProcessingAction {

    public Object[] arguments = null;

    public FeedProcessingAction() {
    }

    public FeedProcessingAction(Object[] parameters) {
        arguments = parameters;
    }

    /**
     * @param feed The {@link Feed} to perform the action for.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    public abstract boolean performAction(Feed feed);
}