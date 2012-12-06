package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find the number of backlinks to the domain and concrete page using the SemRush
 * index.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public final class SemRush extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(SemRush.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "semrush";

    /** The ranking value types of this service **/
    public static final RankingType BACKLINKS_DOMAIN = new RankingType("semrushbacklinksdomain", "Backlinks Domain",
            "The Number of Backlinks to the Domain");
    public static final RankingType BACKLINKS_PAGE = new RankingType("semrushbacklinkspage", "Backlinks Page",
            "The Number of Backlinks to the Page");

    /** All available ranking types by {@link SemRush}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(BACKLINKS_DOMAIN, BACKLINKS_PAGE);


    @Override
    public Ranking getRanking(String url) {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        Long backlinksDomain = 0L;
        Long backlinksPage = 0L;
        String requestUrl = buildRequestUrl(url);


        DocumentRetriever documentRetriever = new DocumentRetriever();
        String text = documentRetriever.getText(requestUrl);

        try {
            backlinksDomain = Long.valueOf(StringHelper.getSubstringBetween(text, "<links_domain>", "</links_domain>"));
            backlinksPage = Long.valueOf(StringHelper.getSubstringBetween(text, "<links>", "</links>"));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        results.put(BACKLINKS_DOMAIN, (float)backlinksDomain);
        results.put(BACKLINKS_PAGE, (float)backlinksPage);
        return ranking;
    }

    /**
     * <p>
     * Build the request URL.
     * </p>
     * 
     * @param url The URL to search for.
     * @return The request URL.
     */
    private String buildRequestUrl(String url) {
        String requestUrl = "http://publicapi.bl.semrush.com/?url=" + UrlHelper.encodeParameter(url);
        return requestUrl;
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] a) {
        SemRush gpl = new SemRush();
        Ranking ranking = null;

        ranking = gpl
                .getRanking("http://webknox.com/p/best-funny-comic-strips");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(SemRush.BACKLINKS_DOMAIN) + " backlinks to the domain");
        System.out.println(ranking.getValues().get(SemRush.BACKLINKS_PAGE) + " backlinks to the page");
    }

}
