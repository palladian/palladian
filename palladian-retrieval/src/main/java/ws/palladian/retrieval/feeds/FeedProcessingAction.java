package ws.palladian.retrieval.feeds;

import ws.palladian.retrieval.HttpResult;

public interface FeedProcessingAction {

    /**
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     */
    void onModified(Feed feed, HttpResult httpResult);

    /**
     * A second hook to perform in case the feed has not been changed.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     */
    void onUnmodified(Feed feed, HttpResult httpResult);

    /**
     * A third hook to perform an error handling.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     */
    void onException(Feed feed, HttpResult httpResult);

    /**
     * A fourth hook to perform in case we got a high http status code >= 400.
     * 
     * @param feed The {@link Feed} to perform the action for.
     * @param httpResult The {@link HttpResult} we got when downloading the {@link Feed}.
     */
    void onError(Feed feed, HttpResult httpResult);

}