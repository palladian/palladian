package ws.palladian.retrieval.feeds;

public abstract class FeedProcessingAction {

    public Object[] arguments = null;

    public FeedProcessingAction() {
    }

    public FeedProcessingAction(Object[] parameters) {
        arguments = parameters;
    }

    public abstract void performAction(Feed feed);
}