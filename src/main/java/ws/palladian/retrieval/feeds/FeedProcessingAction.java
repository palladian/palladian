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
     * A second hook to perform in case the feed has not been changed.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    public abstract boolean performActionOnUnmodifiedFeed(Feed feed, HttpResult httpResult);

    /**
     * A third hook to perform an error handling.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    public abstract boolean performActionOnException(Feed feed, HttpResult httpResult);

    /**
     * A fourth hook to perform in case we got a high http status code >= 400.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    public abstract boolean performActionOnHighHttpStatusCode(Feed feed, HttpResult httpResult);

}