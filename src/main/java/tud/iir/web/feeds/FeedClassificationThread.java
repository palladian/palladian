package tud.iir.web.feeds;

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
//        List<FeedEntry> entries;
//		try {
//			entries = na.getEntries(feed.getFeedUrl());
//			 feed.setEntries(entries);
//		     FeedClassifier.addMetaInformation(feed.getByteSize() / 1024.0, feed.getEntries().size());
//		} catch (FeedAggregatorException e) {
//			Logger.getRootLogger().error(e.getMessage());
//		}
        //FeedClassifier.addMetaInformation(feed.getByteSize() / 1024.0, feed.getNumEntries());
        feedStore.updateFeed(feed);
    }
}
