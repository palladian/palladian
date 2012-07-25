package ws.palladian.retrieval.feeds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.junit.Test;

import ws.palladian.control.AllTests;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;

public class RomeFeedParserTest {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(RomeFeedParserTest.class);

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US);

    /**
     * Test downloading a feed from the web.
     * 
     * @throws FeedParserException
     */
    @Test
    public void testDownloadFeed() throws FeedParserException {
        if (AllTests.ALL_TESTS) {
            FeedParser romeFeedParser = new RomeFeedParser();
            romeFeedParser.getFeed("http://www.gizmodo.de/feed/atom");
        }
    }

    /**
     * Test, if feeds can be parsed. This is a very primitive way of testing, as we just assert no
     * {@link FeedParserException}s.
     * 
     * @throws FeedParserException
     * @throws FileNotFoundException
     */
    @Test
    public void testFeedParsing() throws FeedParserException, FileNotFoundException {

        FeedParser romeFeedParser = new RomeFeedParser();

        // Content is not allowed in prolog.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed004.xml"));

        // The processing instruction target matching "[xX][mM][lL]" is not allowed.
        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed009.xml")));
        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed085.xml")));
        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed086.xml")));

        // The reference to entity "L" must end with the ';' delimiter.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed010.xml"));

        // The markup declarations contained or pointed to by the document type declaration must be well-formed.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed013.xml"));

        // The reference to entity "F" must end with the ';' delimiter.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed030.xml"));

        // The entity name must immediately follow the '&' in the entity reference.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed035.xml"));

        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed055.xml")));
        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed063.xml")));
        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed065.xml")));
        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed067.xml")));

        // The reference to entity "M" must end with the ';' delimiter.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed068.xml"));

        // The entity "nbsp" was referenced, but not declared.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed069.xml"));

        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed070.xml")));

        // The entity name must immediately follow the '&' in the entity reference.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed078.xml"));

        // The entity name must immediately follow the '&' in the entity reference.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed081.xml"));

        // The entity "eacute" was referenced, but not declared.
        // RomeFeedParser.getFeed(ResourceHelper.getResourcePath("/feeds/feed082.xml"));

        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed084.xml")));

        // Sourceforge feeds; sourceforge02.xml failed because of illegal XML characters
        // An invalid XML character (Unicode: 0x4) was found in the CDATA section.
        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/sourceforge01.xml")));
        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/sourceforge02.xml")));

        // UTF-16
        // feedRetriever.setCleanStrings(false);
        romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed102.xml")));

    }

    /**
     * Test, if standard conform formats from constructed test set can be parsed. This means, that in contrast to
     * {@link #testFeedParsing()}, the feed data which is checked below is taken from synthetic sample data, not
     * "from the wild".
     * 
     * @throws FeedParserException
     * @throws ParseException
     * @throws FileNotFoundException
     */
    @Test
    public void testFeedParsing2() throws FeedParserException, ParseException, FileNotFoundException {

        FeedParser romeFeedParser = new RomeFeedParser();
        // feedRetriever.setCleanStrings(false);

        // verify, if author information is parsed correctly

        // //////////// Atom feeds ////////////
        Feed feed = romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/atomSample1.xml")));
        FeedItem feedItem = feed.getItems().iterator().next();

        feed = romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/atomSample2.xml")));
        feedItem = feed.getItems().iterator().next();
        // assertEquals("John Doe; Mary Duff", feedItem.getAuthors());
        // assertEquals(df.parse("2003-12-13 18:30:02.000 GMT"), feedItem.getPublished());

        // //////////// RSS feeds ////////////
        feed = romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/rss20Sample1.xml")));
        feedItem = feed.getItems().iterator().next();
        assertEquals("lawyer@boyer.net (Lawyer Boyer)", feedItem.getAuthors());
        assertEquals(df.parse("2009-09-06 16:45:00.000 GMT"), feedItem.getPublished());

        // RDF Site Summary 1.0; Content Module
        // http://web.resource.org/rss/1.0/modules/content/
        feed = romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/rssRdf10.xml")));
        feedItem = feed.getItems().iterator().next();
        assertEquals("<p>What a <em>beautiful</em> day!</p>", feedItem.getText());

    }

    /**
     * Test parsing of exotic "real world" feeds with foreign characters.
     * 
     * @throws FeedParserException
     * @throws FileNotFoundException 
     */
    @Test
    public void testFeedParsing3() throws FeedParserException, FileNotFoundException {

        FeedParser romeFeedParser = new RomeFeedParser();

        // TODO cleaning destroys the content
        // feedRetriever.setCleanStrings(false);

        // arabic characters
        Feed feed = romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed100.xml")));
        FeedItem item = feed.getItems().iterator().next();
        assertEquals("الجزيرة نت", feed.getMetaInformation().getTitle());
        assertEquals("اشتباكات ببنغازي توقع جرحى", item.getTitle());
        assertEquals(80, feed.getItems().size());

        // japanese characters
        feed = romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath("/feeds/feed101.xml")));
        item = feed.getItems().iterator().next();
        assertEquals("植村冒険賞に登山家、栗秋正寿さん", item.getTitle());

    }

    /**
     * Test date parsing mechanism. For the following feeds, the publish dates of the entries cannot be parsed by ROME
     * itself, but some dates can be retrieved using {@link DateGetterHelper}. Commented lines still cannot be parsed.
     * @throws FileNotFoundException 
     */
    @Test
    public void testDateParsing() throws ParseException, FileNotFoundException {

        // Wed, 02 Feb 2011 12:11:00 GMT
        checkDate("2011-02-02 12:11:00.000 CET", "/feeds/feed001.xml");

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
        checkDate("2011-01-11 04:00:00.000 GMT", "/feeds/feed012.xml");

        // Wed, 2 Feb 2011 15:00:00 GMT
        checkDate("2011-02-02 14:00:00.000 GMT", "/feeds/feed014.xml");

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
        checkDate("2011-01-29 23:00:00.000 GMT", "/feeds/feed029.xml");

        // nonzero nanoseconds cannot be parsed
        // LocalizeHelper.setUTCandEnglish();
        // 2011-04-08T17:33:04.0026Z
        // checkDate("2011-04-08 15:33:04.0026 GMT", "/feeds/feed087.xml");
        checkDate("2011-04-08 15:33:04.000 GMT", "/feeds/feed087.xml");
        // LocalizeHelper.restoreTimeZoneAndLocale();

        // feed cannot be parsed
        // 31 Jan 2011 20:34:17 EST
        // checkDate("2011-01-31 20:34:17.000 EST", "/feeds/feed030.xml");

        // Tuesday, 14 Sept 2010 16:30:00 EST
        // checkDate("2010-09-14 16:30:00.000 EST", "/feeds/feed031.xml");

        // feed cannot be parsed
        // Fri, 06 Aug 2010 09:53:48 +0000
        // checkDate("2010-08-06 09:53:48.000 GMT", "/feeds/feed035.xml");

        // Wed, 02 Feb 2011 2/2/2011 9:00:01 AM UT
        checkDate("2011-02-02 08:00:01.000 GMT", "/feeds/feed040.xml");

        // date cannot be parsed
        // Jan 25,2011
        // checkDate("2011-01-25 00:00:00.000 GMT", "/feeds/feed043.xml");

        // Wednesday, February 02, 2011 4:03:46 AM GMT
        checkDate("2011-02-02 03:03:46.000 GMT", "/feeds/feed045.xml");

        // Wed 2 Feb 2011 00:00:00 GMT
        checkDate("2011-02-01 23:00:00.000 GMT", "/feeds/feed049.xml");

        // Wed, 26 Jan 2011 09:25:02 -0500
        checkDate("2011-01-26 08:25:02.000 GMT-05:00", "/feeds/feed051.xml");

        // 11/30/2010 5:25:47 PM
        checkDate("2010-11-30 16:25:47.000 GMT", "/feeds/feed053.xml");

        // 2011-01-31T11:48:00
        checkDate("2011-01-31 10:48:00.000 GMT", "/feeds/feed054.xml");

        // Sun, 30 Jan 2011 00:00:00 -0600
        checkDate("2011-01-30 00:00:00.000 GMT-06:00", "/feeds/feed055.xml");

        // 2011-02-02T00:00:00.0000000-05:00
        checkDate("2011-02-01 23:00:00.000 GMT-05:00", "/feeds/feed061.xml");

        // Wed, 02 Feb 2011 09:12:43 -0500
        checkDate("2011-02-02 09:12:43.000 GMT-05:00", "/feeds/feed063.xml");

        // date cannot be parsed
        // 2011-02-2T10:00
        // checkDate("2011-02-02 10:00:00.000 GMT", "/feeds/feed073.xml");

        // Feb 2, 2011
        checkDate("2011-02-01 23:00:00.000 GMT", "/feeds/feed077.xml");

    }

    /**
     * Check, if feed item's publish dates were parsed correctly, by comparing the date of the <b>first</b> item to an
     * expected value.
     * 
     * @param expected
     * @param feedFile
     * @throws ParseException
     * @throws FileNotFoundException 
     */
    private void checkDate(String expected, String feedFile) throws ParseException, FileNotFoundException {

        FeedParser romeFeedParser = new RomeFeedParser();
        // DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");

        try {

            Date expectedDate = df.parse(expected);
            Feed feed = romeFeedParser.getFeed(new File(ResourceHelper.getResourcePath(feedFile)));

            // we always test the feed's first entry
            Date itemDate = feed.getItems().iterator().next().getPublished();
            assertEquals(feedFile, expectedDate, itemDate);

        } catch (FeedParserException e) {
            fail("feed " + feedFile + " could not be read : " + e.getMessage());
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
    private void buildTestsetWithErrors(String inputPath, String outputPath) {
        List<String> feedUrls = FileHelper.readFileToArray(inputPath);
        FeedParser romeFeedParser = new RomeFeedParser();
        StringBuilder output = new StringBuilder();
        int counter = 0;

        LOGGER.info("to check : " + feedUrls.size());

        for (String feedUrl : feedUrls) {
            counter++;
            LOGGER.info("checking " + counter + " : " + feedUrl);
            try {
                Feed feed = romeFeedParser.getFeed(feedUrl);
                Iterator<FeedItem> itemIterator = feed.getItems().iterator();
                if (itemIterator.hasNext()) {
                    FeedItem item = itemIterator.next();
                    Date published = item.getPublished();
                    if (published == null) {
                        output.append("no pub date").append("\t").append(feed.getFeedUrl()).append("\n");
                    }
                }
            } catch (FeedParserException e) {
                output.append("error (").append(e.getMessage()).append(")");
                output.append("\t").append(feedUrl).append("\n");
            }
        }

        FileHelper.writeToFile(outputPath, output);
    }

    /**
     * Compiles a test set from errors file, which was generated by {@link #buildTestsetWithErrors()}.
     */
    @SuppressWarnings("unused")
    private void downloadTestfilesFromTestset() {

        List<String> file = FileHelper.readFileToArray("data/_feeds_errors.txt");

        DocumentRetriever crawler = new DocumentRetriever();
        NumberFormat format = new DecimalFormat("000");
        int count = 0;

        for (String line : file) {
            String url = line.split("\t")[1];
            String download = crawler.getText(url);
            FileHelper.writeToFile("data/temp/feeds/feed" + format.format(++count) + ".xml", download);
        }
    }

    /**
     * Performance test concerning date recognition.
     * @throws FileNotFoundException 
     */
    @SuppressWarnings("unused")
    private void evaluateDateParsing() throws FeedParserException, FileNotFoundException {

        RomeFeedParser romeFeedParser = new RomeFeedParser();
        int numIterations = 100;
        File feed = new File(ResourceHelper.getResourcePath("/feeds/feed014.xml"));

        StopWatch sw = new StopWatch();
        romeFeedParser.setUseDateRecognition(false);
        for (int i = 0; i < numIterations; i++) {
            romeFeedParser.getFeed(feed);
        }
        LOGGER.info("without date recognition : " + (float) sw.getElapsedTime() / numIterations + " ms.");

        sw = new StopWatch();
        romeFeedParser.setUseDateRecognition(true);
        for (int i = 0; i < numIterations; i++) {
            romeFeedParser.getFeed(feed);
        }
        LOGGER.info("with date recognition : " + (float) sw.getElapsedTime() / numIterations + " ms.");

    }

    public static void main(String[] args) throws Exception {
        // RomeFeedParserTest feedRetrieverTest = new RomeFeedParserTest();
        // feedRetrieverTest.evaluateDateParsing();
        // feedRetrieverTest.buildTestsetWithErrors("data/_feeds_testset.txt", "data/_feeds_errors.txt");
    }

}
