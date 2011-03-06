/**
 * Created on: 23.07.2010 09:46:46
 */
package ws.palladian.web.feeds;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.web.feeds.persistence.CollectionFeedSource;

/**
 * <p>
 * Tests whether the feed checker can correctly access feeds.
 * </p>
 * 
 * @author klemens.muthmann@googlemail.com
 * @version 1.0
 * @since 1.0
 * 
 */
public class FeedCheckerTest {

    private Collection<Feed> fixture;

    private FeedReader objectOfClassUnderTest;

    @Before
    public void setUp() throws Exception {
        fixture = new HashSet<Feed>();
        fixture.add(new Feed("http://www.tagesschau.de/xml/rss2"));
        objectOfClassUnderTest = new FeedReader(new CollectionFeedSource(fixture));
    }

    /**
     * Test method for {@link tud.iir.news.FeedReader#startContinuousReading()}.
     */
    @Test
    public void testContinuousReading() {
        objectOfClassUnderTest.startContinuousReading(10 * DateHelper.SECOND_MS);
        objectOfClassUnderTest.stopContinuousReading();
    }

}
