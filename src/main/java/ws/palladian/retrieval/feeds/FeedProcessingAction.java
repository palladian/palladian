package ws.palladian.retrieval.feeds;

import ws.palladian.retrieval.HttpResult;

public abstract class FeedProcessingAction {

    public Object[] arguments = null;

    public FeedProcessingAction() {
    }

    public FeedProcessingAction(Object[] parameters) {
        arguments = parameters;
    }

    /**
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    public abstract boolean performAction(Feed feed, HttpResult httpResult);

    /**
     * A second hook to perform an error handling.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    public abstract boolean performActionOnError(Feed feed, HttpResult httpResult);
}