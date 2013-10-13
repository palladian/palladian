package ws.palladian.retrieval.feeds;

import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * Default implementation for a {@link FeedProcessingAction}. Usually, one is only interested in implementing
 * {@link #onModified(Feed, HttpResult)}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class DefaultFeedProcessingAction implements FeedProcessingAction {

    @Override
    public void onModified(Feed feed, HttpResult httpResult) {
    }

    @Override
    public void onUnmodified(Feed feed, HttpResult httpResult) {
    }

    @Override
    public void onException(Feed feed, HttpResult httpResult) {
    }

    @Override
    public void onError(Feed feed, HttpResult httpResult) {
    }

}
