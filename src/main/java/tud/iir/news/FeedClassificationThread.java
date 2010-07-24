package tud.iir.news;

public class FeedClassificationThread implements Runnable {

    private Feed feed;
    private FeedStore feedStore;

    public FeedClassificationThread(Feed feed, FeedStore feedStore) {
        this.feed = feed;
        this.feedStore = feedStore;
    }

    @Override
    public void run() {
        FeedClassifier.classify(feed);
        FeedClassifier.addMetaInformation(feed.getByteSize() / 1024.0, feed.getEntries().size());
        feedStore.updateFeed(feed);
    }
}
