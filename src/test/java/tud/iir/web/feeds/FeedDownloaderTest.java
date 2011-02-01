package tud.iir.web.feeds;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import tud.iir.control.AllTests;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import tud.iir.web.Crawler;

public class FeedDownloaderTest {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDownloaderTest.class);

    private static FeedDownloader aggregator;

    @BeforeClass
    public static void before() {
        aggregator = new FeedDownloader();
    }

    @Test
    @Ignore
    public void readFeedFromFile() throws FeedDownloaderException {
        aggregator.getFeed("data/test/feeds/feed1.xml");
        aggregator.getFeed("data/test/feeds/feed2.xml");
    }
    
    @Test
    @Ignore
    public void evaluateParsing() {
        
        List<String> feedUrls = FileHelper.readFileToArray("data/feeds.txt");
        FeedDownloader feedDownloader = new FeedDownloader();
        StringBuilder output = new StringBuilder();
        int counter = 0;
        
        LOGGER.info("to check : " + feedUrls.size());
        
        for (String feedUrl : feedUrls) {
            counter++;
            LOGGER.info("checking " + counter + " : " + feedUrl);
            try {
                Feed feed = feedDownloader.getFeed(feedUrl);
                Iterator<FeedItem> itemIterator = feed.getItems().iterator();
                if (itemIterator.hasNext()) {
                    FeedItem item = itemIterator.next();
                    Date published = item.getPublished();
                    if (published == null) {
                        output.append("no pub date").append("\t").append(feed.getFeedUrl()).append("\n");
                    }
                }
            } catch (FeedDownloaderException e) {
                output.append("error (").append(e.getMessage()).append(")");
                output.append("\t").append(feedUrl).append("\n");
            }
        }
        
        FileHelper.writeToFile("data/feedsErrors.txt", output);
        
    }
    
    @Test
    //@Ignore
    public void evaluateDateParsing() throws FeedDownloaderException {
        
        Crawler c = new Crawler();
        Document doc = c.getXMLDocument("http://www.snopes.com/info/whatsnew.xml");
        FeedDownloader feedDownloader = new FeedDownloader();
        feedDownloader.useDateRecognition = true;
        
        // performance test concerning daterecognition
        StopWatch sw = new StopWatch();
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            // Feed feed = feedDownloader.getFeed(doc);
            // System.out.println(feed.getItems().iterator().next().getPublished());
            feedDownloader.getFeed(doc);
        }
        System.out.println("avg. ms: " + (float) sw.getElapsedTime() / iterations);
        
        // parse times
        // with daterecognition: 107.03ms
        // without daterecognition: 38.39ms

    }

    @Test
    @Ignore
    public void downloadFeed() throws FeedDownloaderException {
        if (AllTests.ALL_TESTS) {
            Feed feed = aggregator.getFeed("http://www.gizmodo.de/feed/atom");
            // System.out.println(feed);
        }
    }

}
