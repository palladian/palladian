package ws.palladian.web.feeds;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import ws.palladian.control.AllTests;
import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.web.Crawler;

public class FeedDownloaderTest {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDownloaderTest.class);

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");

    /**
     * Test downloading a feed from the web.
     * 
     * @throws FeedDownloaderException
     */
    @Test
    public void testDownloadFeed() throws FeedDownloaderException {
        if (AllTests.ALL_TESTS) {
            FeedDownloader feedDownloader = new FeedDownloader();
            feedDownloader.getFeed("http://www.gizmodo.de/feed/atom");
        }
    }

    /**
     * Test, if feeds can be parsed. This is a very primitive way of testing, as we just assert no
     * {@link FeedDownloaderException}s.
     * 
     * @throws FeedDownloaderException
     */
    @Test
    public void testFeedParsing() throws FeedDownloaderException {

        FeedDownloader feedDownloader = new FeedDownloader();

        // Content is not allowed in prolog.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed004.xml").getFile());

        // The processing instruction target matching "[xX][mM][lL]" is not allowed.
        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed009.xml").getFile());
        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed085.xml").getFile());
        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed086.xml").getFile());

        // The reference to entity "L" must end with the ';' delimiter.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed010.xml").getFile());

        // The markup declarations contained or pointed to by the document type declaration must be well-formed.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed013.xml").getFile());

        // The reference to entity "F" must end with the ';' delimiter.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed030.xml").getFile());

        // The entity name must immediately follow the '&' in the entity reference.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed035.xml").getFile());

        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed055.xml").getFile());
        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed063.xml").getFile());
        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed065.xml").getFile());
        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed067.xml").getFile());

        // The reference to entity "M" must end with the ';' delimiter.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed068.xml").getFile());

        // The entity "nbsp" was referenced, but not declared.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed069.xml").getFile());

        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed070.xml").getFile());

        // The entity name must immediately follow the '&' in the entity reference.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed078.xml").getFile());

        // The entity name must immediately follow the '&' in the entity reference.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed081.xml").getFile());

        // The entity "eacute" was referenced, but not declared.
        // feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed082.xml").getFile());

        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed084.xml").getFile());

        // Sourceforge feeds; sourceforge02.xml failed because of illegal XML characters
        // An invalid XML character (Unicode: 0x4) was found in the CDATA section.
        feedDownloader.getFeed(FeedDatabaseTest.class.getResource("/feeds/sourceforge01.xml").getFile());
        feedDownloader.getFeed(FeedDatabaseTest.class.getResource("/feeds/sourceforge02.xml").getFile());

        // UTF-16
        feedDownloader.setCleanStrings(false);
        feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed102.xml").getFile());

    }

    /**
     * Test, if standard conform formats from constructed test set can be parsed. This means, that in contrast to
     * {@link #testFeedParsing()}, the feed data which is checked below is taken from synthetic sample data, not
     * "from the wild".
     * 
     * @throws FeedDownloaderException
     * @throws ParseException
     */
    @Test
    public void testFeedParsing2() throws FeedDownloaderException, ParseException {

        FeedDownloader feedDownloader = new FeedDownloader();
        feedDownloader.setCleanStrings(false);

        // verify, if author information is parsed correctly

        // //////////// Atom feeds ////////////
        Feed feed = feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/atomSample1.xml").getFile());
        FeedItem feedItem = feed.getItems().iterator().next();
        Assert.assertEquals("John Doe; Mary Duff", feedItem.getAuthors());
        Assert.assertEquals(df.parse("2003-12-13 18:30:02.000 GMT"), feedItem.getPublished());

        feed = feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/atomSample2.xml").getFile());
        feedItem = feed.getItems().iterator().next();
        Assert.assertEquals("John Doe; Mary Duff", feedItem.getAuthors());
        Assert.assertEquals(df.parse("2003-12-13 18:30:02.000 GMT"), feedItem.getPublished());

        // //////////// RSS feeds ////////////
        feed = feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/rss20Sample1.xml").getFile());
        feedItem = feed.getItems().iterator().next();
        Assert.assertEquals("lawyer@boyer.net (Lawyer Boyer)", feedItem.getAuthors());
        Assert.assertEquals(df.parse("2009-09-06 16:45:00.000 GMT"), feedItem.getPublished());

        // RDF Site Summary 1.0; Content Module
        // http://web.resource.org/rss/1.0/modules/content/
        feed = feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/rssRdf10.xml").getFile());
        feedItem = feed.getItems().iterator().next();
        Assert.assertEquals("<p>What a <em>beautiful</em> day!</p>", feedItem.getItemText());

    }

    /**
     * Test parsing of exotic "real world" feeds with foreign characters.
     * 
     * @throws FeedDownloaderException
     */
    @Test
    public void testFeedParsing3() throws FeedDownloaderException {

        FeedDownloader feedDownloader = new FeedDownloader();

        // TODO cleaning destroys the content
        feedDownloader.setCleanStrings(false);

        // arabic characters
        Feed feed = feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed100.xml").getFile());
        FeedItem item = feed.getItems().iterator().next();
        Assert.assertEquals("الجزيرة نت", feed.getTitle());
        Assert.assertEquals("اشتباكات ببنغازي توقع جرحى", item.getTitle());
        Assert.assertEquals(80, feed.getItems().size());

        // japanese characters
        feed = feedDownloader.getFeed(FeedDownloader.class.getResource("/feeds/feed101.xml").getFile());
        item = feed.getItems().iterator().next();
        Assert.assertEquals("植村冒険賞に登山家、栗秋正寿さん", item.getTitle());

    }

    /**
     * Test date parsing mechanism. For the following feeds, the publish dates of the entries cannot be parsed by ROME
     * itself, but some dates can be retrieved using {@link DateGetterHelper}. Commented lines still cannot be parsed.
     */
    @Test
    public void testDateParsing() throws ParseException {

        // Wed, 02 Feb 2011 12:11:00 GMT
        checkDate("2011-02-02 12:11:00.000 GMT", "/feeds/feed001.xml");

        // feed cannot be parsed, as it contains strange white space
        // 2011-02-02T06:33:52.690
        // checkDate("2011-02-02 06:33:52.690 GMT", "/feeds/feed004.xml");

        // Fri, 28 Jan 2011 10:45:15 -0500
        checkDate("2011-01-28 10:45:15.000 GMT-05:00", "/feeds/feed005.xml");

        // Wed, 02 Feb 2011 10:26:21 -0500
        checkDate("2011-02-02 10:26:21.000 GMT-05:00", "/feeds/feed006.xml");

        // date cannot be parsed
        // Wed, 2, Feb 2011 9:36
        // checkDate("2011-02-02 09:36:00.000 GMT", "/feeds/feed007.xml");

        // date cannot be parsed
        // 2011-01-31
        // checkDate("2011-01-31 00:00:00.000 GMT", "/feeds/feed008.xml");

        // feed cannot be parsed
        // Thu, 31 Mar 2011 19:00:00 -0500
        // checkDate("2011-03-31 19:00:00.000 GMT-05:00", "/feeds/feed009.xml");

        // feed cannot be parsed
        // Wed, 02 Feb 2011 05:37:00 EST
        // checkDate("2011-02-02 05:37:00.000 EST", "/feeds/feed010.xml");

        // Tue, 11 Jan 2011 00:00:00 -0500
        checkDate("2011-01-11 00:00:00.000 GMT-05:00", "/feeds/feed012.xml");

        // Wed, 2 Feb 2011 15:00:00 GMT
        checkDate("2011-02-02 15:00:00.000 GMT", "/feeds/feed014.xml");

        // date cannot be parsed
        // Tue, Feb 01,2011 11:33:33PM
        // checkDate("2011-02-01 23:33:33.000 GMT", "/feeds/feed018.xml");

        // date cannot be parsed
        // Wed, 02 Feb 2011 09:00:00 EST
        // checkDate("2011-02-02 09:00:00.000 EST", "/feeds/feed021.xml");

        // date cannot be parsed
        // Tue, 01 February 2011 15:15:56
        // checkDate("2011-02-01 15:15:56.000 GMT", "/feeds/feed024.xml");

        // Yesterday +0000
        // feed026.xml

        // date cannot be parsed
        // Wed, February 2, 2011 10:05 AM
        // checkDate("2011-02-02 10:05:00.000 GMT", "/feeds/feed028.xml");

        // 1/30/11
        checkDate("2011-01-30 00:00:00.000 GMT", "/feeds/feed029.xml");

        // feed cannot be parsed
        // 31 Jan 2011 20:34:17 EST
        // checkDate("2011-01-31 20:34:17.000 EST", "/feeds/feed030.xml");

        // Tuesday, 14 Sept 2010 16:30:00 EST
        // checkDate("2010-09-14 16:30:00.000 EST", "/feeds/feed031.xml");

        // feed cannot be parsed
        // Fri, 06 Aug 2010 09:53:48 +0000
        // checkDate("2010-08-06 09:53:48.000 GMT", "/feeds/feed035.xml");

        // Wed, 02 Feb 2011 2/2/2011 9:00:01 AM UT
        checkDate("2011-02-02 09:00:01.000 GMT", "/feeds/feed040.xml");

        // date cannot be parsed
        // Jan 25,2011
        // checkDate("2011-01-25 00:00:00.000 GMT", "/feeds/feed043.xml");

        // Wednesday, February 02, 2011 4:03:46 AM GMT
        checkDate("2011-02-02 04:03:46.000 GMT", "/feeds/feed045.xml");

        // Wed 2 Feb 2011 00:00:00 GMT
        checkDate("2011-02-02 00:00:00.000 GMT", "/feeds/feed049.xml");

        // Wed, 26 Jan 2011 09:25:02 -0500
        checkDate("2011-01-26 09:25:02.000 GMT-05:00", "/feeds/feed051.xml");

        // 11/30/2010 5:25:47 PM
        checkDate("2010-11-30 17:25:47.000 GMT", "/feeds/feed053.xml");

        // 2011-01-31T11:48:00
        checkDate("2011-01-31 11:48:00.000 GMT", "/feeds/feed054.xml");

        // Sun, 30 Jan 2011 00:00:00 -0600
        checkDate("2011-01-30 00:00:00.000 GMT-06:00", "/feeds/feed055.xml");

        // 2011-02-02T00:00:00.0000000-05:00
        checkDate("2011-02-02 00:00:00.000 GMT-05:00", "/feeds/feed061.xml");

        // Wed, 02 Feb 2011 09:12:43 -0500
        checkDate("2011-02-02 09:12:43.000 GMT-05:00", "/feeds/feed063.xml");

        // date cannot be parsed
        // 2011-02-2T10:00
        // checkDate("2011-02-02 10:00:00.000 GMT", "/feeds/feed073.xml");

        // Feb 2, 2011
        checkDate("2011-02-02 00:00:00.000 GMT", "/feeds/feed077.xml");

    }

    /**
     * Check, if feed item's publish dates were parsed correctly, by comparing the date of the <b>first</b> item to an
     * expected value.
     * 
     * @param expected
     * @param feedFile
     * @throws ParseException
     */
    private void checkDate(String expected, String feedFile) throws ParseException {

        FeedDownloader feedDownloader = new FeedDownloader();
        // DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");

        try {

            Date expectedDate = df.parse(expected);
            Feed feed = feedDownloader.getFeed(FeedDownloaderTest.class.getResource(feedFile).getFile());

            // we always test the feed's first entry
            Date itemDate = feed.getItems().iterator().next().getPublished();
            Assert.assertEquals(feedFile, expectedDate, itemDate);

        } catch (FeedDownloaderException e) {
            Assert.fail("feed " + feedFile + " could not be read : " + e.getMessage());
        }

    }

    // ///////////////////////////////////////////////////////////////
    // Code for testset compilation below
    // ///////////////////////////////////////////////////////////////

    /**
     * Build a list of feeds causing problems from a testset with feed URLs.
     * The current definition of "problem" is:
     * 
     * a) Feed cannot be parsed
     * 
     * b) Feed's items have no publish date (this needs to be verified manually: for some feeds, the publish date cannot
     * be parsed, but some feeds contain no publish dates at all)
     */
    @SuppressWarnings("unused")
    private void buildTestsetWithErrors() {
        List<String> feedUrls = FileHelper.readFileToArray("data/_feeds_testset.txt");
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

        FileHelper.writeToFile("data/_feeds_errors.txt", output);
    }

    /**
     * Compiles a test set from errors file, which was generated by {@link #buildTestsetWithErrors()}.
     */
    @SuppressWarnings("unused")
    private void downloadTestfilesFromTestset() {

        List<String> file = FileHelper.readFileToArray("data/_feeds_errors.txt");

        Crawler crawler = new Crawler();
        NumberFormat format = new DecimalFormat("000");
        int count = 0;

        for (String line : file) {
            String url = line.split("\t")[1];
            String download = crawler.download(url);
            FileHelper.writeToFile("data/temp/feeds/feed" + format.format(++count) + ".xml", download);
        }
    }

    /**
     * Performance test concerning date recognition.
     */
    private void evaluateDateParsing() throws FeedDownloaderException {

        FeedDownloader feedDownloader = new FeedDownloader();
        int numIterations = 100;
        String feedPath = FeedDownloader.class.getResource("/feeds/feed014.xml").getFile();

        StopWatch sw = new StopWatch();
        feedDownloader.setUseDateRecognition(false);
        for (int i = 0; i < numIterations; i++) {
            feedDownloader.getFeed(feedPath);
        }
        LOGGER.info("without date recognition : " + (float) sw.getElapsedTime() / numIterations + " ms.");

        sw = new StopWatch();
        feedDownloader.setUseDateRecognition(true);
        for (int i = 0; i < numIterations; i++) {
            feedDownloader.getFeed(feedPath);
        }
        LOGGER.info("with date recognition : " + (float) sw.getElapsedTime() / numIterations + " ms.");

    }

    public static void main(String[] args) throws Exception {
        FeedDownloaderTest feedDownloaderTest = new FeedDownloaderTest();
        feedDownloaderTest.evaluateDateParsing();
    }

}
