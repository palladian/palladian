package ws.palladian.retrieval.feeds;

import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * Default implementation for a {@link FeedProcessingAction}. Usually, one is only interested in implementing
 * {@link #performAction(Feed, HttpResult)}.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public class DefaultFeedProcessingAction implements FeedProcessingAction {

    public Object[] arguments = null;

    public DefaultFeedProcessingAction() {
    }

    public DefaultFeedProcessingAction(Object[] parameters) {
        arguments = parameters;
    }

    @Override
    public boolean performAction(Feed feed, HttpResult httpResult) {
        return true;
    }

    @Override
    public boolean performActionOnUnmodifiedFeed(Feed feed, HttpResult httpResult) {
        return true;
    }

    @Override
    public boolean performActionOnException(Feed feed, HttpResult httpResult) {
        return true;
    }

    @Override
    public boolean performActionOnError(Feed feed, HttpResult httpResult) {
        return true;
    }

}
