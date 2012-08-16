package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * Get number of referring domains for specified URL from Majestic-SEO.
 * </p>
 * 
 * @see http://www.majesticseo.com/api_domainstats.php
 * @author Philipp Katz
 */
public final class MajesticSeo extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MajesticSeo.class);

    /** {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.majestic.key";

    /** The id of this service. */
    private static final String SERVICE_ID = "majesticSeo";

    /** The ranking value types of this service. */
    public static final RankingType REFERRING_DOMAINS = new RankingType("majestic_seo_referring_domains",
            "Majestic-SEO Referring Domains", "");

    /** All available ranking types by MajesticSeo. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(REFERRING_DOMAINS);

    private final String apiKey;

    /**
     * <p>
     * Create a new {@link MajesticSeo} ranking service.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key (<tt>api.majestic.key</tt>) for accessing
     *            the
     *            service.
     */
    public MajesticSeo(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link MajesticSeo} ranking service.
     * </p>
     * 
     * @param apiKey The required API key for accessing the service.
     */
    public MajesticSeo(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("The required API key is missing.");
        }
        this.apiKey = apiKey;
    }

    @Override
    public Ranking getRanking(String url) {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);

        String encUrl = UrlHelper.urlEncode(url);
        try {
            HttpResult httpResult = retriever.httpGet("http://api.majesticseo.com/getdomainstats.php?apikey=" + apiKey
                    + "&url=" + encUrl);
            DocumentParser xmlParser = ParserFactory.createXmlParser();
            Document doc = xmlParser.parse(httpResult);
            Node refDomainsNode = XPathHelper.getNode(doc, "/Results/Result/@StatsRefDomains");
            if (refDomainsNode != null) {
                String refDomains = refDomainsNode.getNodeValue();
                results.put(REFERRING_DOMAINS, Float.valueOf(refDomains));
            } else {
                results.put(REFERRING_DOMAINS, 0f);
            }
        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (ParserException e) {
            LOGGER.error(e);
        }
        return ranking;
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public String getApiKey() {
        return apiKey;
    }

}
