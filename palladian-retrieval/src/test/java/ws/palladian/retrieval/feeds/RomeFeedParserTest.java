package ws.palladian.retrieval.feeds;

import static org.junit.Assert.assertEquals;

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
import org.junit.Ignore;
import org.junit.Test;

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
    @Test
    @Ignore
    public void evaluateDateParsing() throws FeedParserException, FileNotFoundException {

        RomeFeedParser romeFeedParser = new RomeFeedParser();
        int numIterations = 10000;
        File feed = new File(ResourceHelper.getResourcePath("/feeds/feed014.xml"));

        StopWatch sw = new StopWatch();
        romeFeedParser.setUseDateRecognition(false);
        for (int i = 0; i < numIterations; i++) {
            romeFeedParser.getFeed(feed);
        }
        System.out.println("without date recognition : " + (float) sw.getElapsedTime() / numIterations + " ms.");

        sw = new StopWatch();
        romeFeedParser.setUseDateRecognition(true);
        for (int i = 0; i < numIterations; i++) {
            romeFeedParser.getFeed(feed);
        }
        System.out.println("with date recognition : " + (float) sw.getElapsedTime() / numIterations + " ms.");

    }

    public static void main(String[] args) throws Exception {
         RomeFeedParserTest feedRetrieverTest = new RomeFeedParserTest();
         feedRetrieverTest.evaluateDateParsing();
        // feedRetrieverTest.buildTestsetWithErrors("data/_feeds_testset.txt", "data/_feeds_errors.txt");
    }

}
