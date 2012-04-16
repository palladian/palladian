package ws.palladian.retrieval.feeds;

import ws.palladian.retrieval.HttpResult;

public interface FeedProcessingAction {

    /**
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    boolean performAction(Feed feed, HttpResult httpResult);

    /**
     * A second hook to perform in case the feed has not been changed.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    boolean performActionOnUnmodifiedFeed(Feed feed, HttpResult httpResult);

    /**
     * A third hook to perform an error handling.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    boolean performActionOnException(Feed feed, HttpResult httpResult);

    /**
     * A fourth hook to perform in case we got a high http status code >= 400.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     * @return <code>true</code> if no error occurred, <code>false</code> otherwise.
     */
    boolean performActionOnError(Feed feed, HttpResult httpResult);

}