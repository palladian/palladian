package ws.palladian.web.feeds;

import org.junit.Ignore;
import org.junit.Test;

public class FeedClassifierTest {

    /**
     * XXX rethink how to test this, not trivial since time in feed is fixed and classification algorithm uses time
     * differences which change at execution
     * can not test this since it depends on current time (works though)
     */
    @Test
    @Ignore
    public void testFeedClassification() {

        // http://beta.rottentomatoes.com/syndication/rss/upcoming.xml
        // Assert.assertEquals(FeedClassifier.CLASS_ON_THE_FLY, FeedClassifier.classify("data/test/feeds/feed1.xml", new
        // FeedStoreDummy()));

        // http://29content6.com/nipponsport/NIPPONSPORTpodcast.xml
        // Assert.assertEquals(FeedClassifier.CLASS_SPONTANUOUS, FeedClassifier.classify("data/test/feeds/feed2.xml",
        // new FeedStoreDummy()));

        // http://beta.rottentomatoes.com/syndication/rss/upcoming.xml (set back to older date)
        // Assert.assertEquals(FeedClassifier.CLASS_CHUNKED, FeedClassifier.classify("data/test/feeds/feed3.xml",new
        // FeedStoreDummy()));

        // http://www.amazingwomenbbs.com/amazingwomen.rss
        // Assert.assertEquals(FeedClassifier.CLASS_ZOMBIE, FeedClassifier.classify("data/test/feeds/feed4.xml",new
        // FeedStoreDummy()));

        // http://feeds.nydailynews.com/nydnrss/news
        // Assert.assertEquals(FeedClassifier.CLASS_CONSTANT, FeedClassifier.classify("data/test/feeds/feed5.xml",new
        // FeedStoreDummy()));

        // http://feeds.gawker.com/lifehacker/full
        // Assert.assertEquals(FeedClassifier.CLASS_SLICED, FeedClassifier.classify("data/test/feeds/feed6.xml", new
        // FeedStoreDummy()));

        // http://www.reddit.com/new/.rssl
        // Assert.assertEquals(FeedClassifier.CLASS_CONSTANT, FeedClassifier.classify("data/test/feeds/feed7.xml", new
        // FeedStoreDummy()));
    }

}