package ws.palladian.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * The FeedFinder downloads links to feeds on the web and stores them in the database.
 * 
 * @deprecated @see tud.iir.news.NewsAggregator instead
 * @author David Urbansky
 */
@Deprecated
public class FeedFinder {

    private static final Logger LOGGER = Logger.getLogger(FeedFinder.class);

    public static void searchFeeds() {

        ArrayList<String> categories = new ArrayList<String>();
        categories.add("animal");
        categories.add("podcast");
        categories.add("business");
        categories.add("education");
        categories.add("internet");
        categories.add("news");
        categories.add("technology");
        categories.add("comedy");
        categories.add("jobs");
        categories.add("fitness");
        categories.add("games");
        categories.add("medicine");
        categories.add("cooking");
        categories.add("marketing");
        categories.add("law");
        categories.add("products");
        categories.add("politics");
        categories.add("life");
        categories.add("science");
        categories.add("health");
        categories.add("sport");
        categories.add("music");
        categories.add("travel");
        categories.add("top");
        categories.add("art");
        categories.add("best");
        categories.add("movie");
        categories.add("popular");
        categories.add("finance");
        categories.add("");

        SourceRetriever sr = new SourceRetriever();
        sr.setSource(SourceRetrieverManager.YAHOO);
        sr.setResultCount(100);

        // do not allow duplicates
        HashSet<String> enteredURLs = new HashSet<String>();

        Iterator<String> categoryIterator = categories.iterator();
        while (categoryIterator.hasNext()) {
            String currentCategory = categoryIterator.next();

            LOGGER.info("search feeds for " + currentCategory);

            String[] queries = new String[4];
            queries[0] = "filetype:rss " + currentCategory;
            queries[1] = "filetype:xml rss " + currentCategory;
            queries[2] = "filetype:atom " + currentCategory;
            queries[3] = "filetype:rss2 " + currentCategory;

            for (int i = 0; i < queries.length; i++) {
                List<String> feedURLs = sr.getURLs(queries[i]);

                Iterator<String> feedURLIterator = feedURLs.iterator();
                while (feedURLIterator.hasNext()) {
                    String currentFeedURL = feedURLIterator.next();
                    if (enteredURLs.add(currentFeedURL)) {
                        LOGGER.info(currentFeedURL);
                        // DatabaseManager.getInstance().runUpdate("INSERT INTO feeds SET url = " + currentFeedURL);
                    }
                }
            }
        }

        LOGGER.info(SourceRetrieverManager.getInstance().getLogs());
    }

    public static void main(String[] args) {
        searchFeeds();
    }
}