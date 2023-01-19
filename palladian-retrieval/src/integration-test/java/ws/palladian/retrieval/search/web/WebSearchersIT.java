package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.images.FlickrSearcher;
import ws.palladian.retrieval.search.images.PixabaySearcher;
import ws.palladian.retrieval.search.socialmedia.InstagramSearcher;
import ws.palladian.retrieval.search.socialmedia.RedditSearcher;
import ws.palladian.retrieval.search.socialmedia.TwitterSearcher;
import ws.palladian.retrieval.search.videos.VimeoSearcher;
import ws.palladian.retrieval.search.videos.YouTubeSearcher;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * <p>
 * Web tests for different {@link WebSearcher}s. These tests are run as integration tests.
 * </p>
 *
 * @author Philipp Katz
 */
@RunWith(Parameterized.class)
public class WebSearchersIT {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSearchersIT.class);

    private final Searcher<?> searcher;

    @SuppressWarnings("deprecation")
    @Parameters(name = "{0}")
    public static Collection<Object[]> searchers() throws ConfigurationException, FileNotFoundException {
        Configuration configuration = loadConfiguration();
        List<Object[]> searchers = new ArrayList<>();

        // web page searchers
        // searchers.add(new Object[] {new DuckDuckGoSearcher()});
        // searchers.add(new Object[] {new GooglePlusSearcher(configuration)});
        // searchers.add(new Object[] {new GoogleCustomSearcher(configuration)});
        // searchers.add(new Object[] {new TopsyUrlSearcher(configuration)});
        searchers.add(new Object[]{new WebKnoxSearcher(configuration)});
        searchers.add(new Object[]{new WikipediaSearcher()});

        // social media searchers
        searchers.add(new Object[]{new InstagramSearcher(configuration)});
        // searchers.add(new Object[] {new FacebookSearcher(configuration)});
        searchers.add(new Object[]{new TwitterSearcher(configuration)});
        searchers.add(new Object[]{new RedditSearcher()});
        // searchers.add(new Object[] {new YelpSearcher(configuration)});

        // news searchers
        // searchers.add(new Object[] {new NewsSeecrSearcher(configuration)});
        // searchers.add(new Object[] {new HakiaNewsSearcher(configuration)});
        // searchers.add(new Object[] {new FarooNewsSearcher()});

        // video searchers
        searchers.add(new Object[]{new VimeoSearcher(configuration)});
        searchers.add(new Object[]{new YouTubeSearcher(configuration)});

        // image searchers
        searchers.add(new Object[]{new FlickrSearcher(configuration)});
        searchers.add(new Object[]{new PixabaySearcher(configuration)});

        return searchers;
    }

    private static Configuration loadConfiguration() throws ConfigurationException, FileNotFoundException {
        return new PropertiesConfiguration(ResourceHelper.getResourceFile("/palladian-test.properties"));
    }

    public WebSearchersIT(Searcher<?> searcher) {
        this.searcher = searcher;
    }

    /**
     * <p>
     * Check, that search request is processed in under 30 seconds and that no exceptions occur. We do no further
     * checking about the actual results, as this is to fragile.
     * </p>
     *
     * @throws SearcherException
     */
    @Test(timeout = 30000)
    public void testSearch() {
        LOGGER.info("testing " + searcher.getName());
        try {
            StopWatch stopWatch = new StopWatch();
            List<?> result = searcher.search("cat", 30);
            LOGGER.info("# results for query from {}: {}", searcher.getName(), result.size());
            LOGGER.info("query took {}", stopWatch);
            if (result.isEmpty()) {
                fail();
            }
        } catch (SearcherException e) {
            LOGGER.error("Fail for {}: {}", new Object[]{searcher.getName(), e.getMessage(), e});
            fail();
        }
    }

}
