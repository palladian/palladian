package ws.palladian.retrieval.ranking.services;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;

/**
 * <p>
 * Web tests for different {@link RankingService}s. These tests are run as integration tests.
 * </p>
 * 
 * @author Philipp Katz
 */
@RunWith(Parameterized.class)
public class RankingServicesIT {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(RankingServicesIT.class);

    private final RankingService rankingService;

    @Parameters
    public static Collection<Object[]> rankers() throws ConfigurationException, FileNotFoundException {
        Configuration configuration = loadConfiguration();
        List<Object[]> rankers = CollectionHelper.newArrayList();
        rankers.add(new Object[] {new AlexaRank()});
        rankers.add(new Object[] {new BibsonomyBookmarks(configuration)});
        rankers.add(new Object[] {new BitlyClicks(configuration)});
        rankers.add(new Object[] {new Compete(configuration)});
        rankers.add(new Object[] {new DeliciousBookmarks()});
        rankers.add(new Object[] {new DmozIndexed()});
        rankers.add(new Object[] {new FacebookLinkStats()});
        // rankers.add(new Object[] {new Foursquare(configuration)});
        rankers.add(new Object[] {new FriendfeedAggregatedStats()});
        rankers.add(new Object[] {new FriendfeedStats()});
        rankers.add(new Object[] {new GoogleCachedPage()});
        rankers.add(new Object[] {new GooglePageRank()});
        rankers.add(new Object[] {new GooglePlusLikes()});
        rankers.add(new Object[] {new LinkedInShares()});
        rankers.add(new Object[] {new MajesticSeo(configuration)});
        rankers.add(new Object[] {new PinterestPins()});
        rankers.add(new Object[] {new PlurkPosts(configuration)});
        rankers.add(new Object[] {new RedditStats()});
        rankers.add(new Object[] {new SemRush()});
        rankers.add(new Object[] {new SharethisStats(configuration)});
        rankers.add(new Object[] {new StumbleUponViews()});
        rankers.add(new Object[] {new TwitterTweets()});
        rankers.add(new Object[] {new WebOfTrust()});
        rankers.add(new Object[] {new Webutation()});
        rankers.add(new Object[] {new YandexCitationIndex()});
        return rankers;
    }

    private static Configuration loadConfiguration() throws ConfigurationException, FileNotFoundException {
        return new PropertiesConfiguration(ResourceHelper.getResourceFile("/palladian-test.properties"));
    }

    public RankingServicesIT(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * <p>
     * Check, that the request is processed in under 30 seconds and that no exceptions occur. We do no further checking
     * about the actual results, as this is to fragile.
     * </p>
     */
    @Test(timeout = 30000)
    public void testSearch() {
        LOGGER.info("testing " + rankingService.getServiceId());
        try {
            StopWatch stopWatch = new StopWatch();
            // Ranking result = rankingService.getRanking("http://global.nytimes.com");
            Ranking result = rankingService.getRanking("http://google.com");
            LOGGER.info("# results from " + rankingService.getServiceId() + ": " + result.getValues());
            LOGGER.info("retieval took " + stopWatch);
        } catch (RankingServiceException e) {
            LOGGER.error("Fail for " + rankingService.getServiceId() + ": " + e.getMessage(), e);
            fail();
        }
    }

}
