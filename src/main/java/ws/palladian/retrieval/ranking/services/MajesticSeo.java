package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.XPathHelper;
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
public class MajesticSeo extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MajesticSeo.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "majesticSeo";

    /** The ranking value types of this service. */
    public static final RankingType REFERRING_DOMAINS = new RankingType("majestic_seo_referring_domains",
            "Majestic-SEO Referring Domains", "");

    private static final List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(REFERRING_DOMAINS);
    }

    private String apiKey;

    public MajesticSeo() {
        PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
            setApiKey(configuration.getString("api.majestic.key"));
        } else {
            LOGGER.warn("could not load configuration, ranking retrieval won't work");
        }
    }

    @Override
    public Ranking getRanking(String url) {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);

        String encUrl = UrlHelper.urlEncode(url);
        Document doc = retriever.getXMLDocument("http://api.majesticseo.com/getdomainstats.php?apikey=" + apiKey
                + "&url=" + encUrl);
        if (doc != null) {
            Node refDomainsNode = XPathHelper.getNode(doc, "/Results/Result/@StatsRefDomains");
            if (refDomainsNode != null) {
                String refDomains = refDomainsNode.getNodeValue();
                results.put(REFERRING_DOMAINS, Float.valueOf(refDomains));
            } else {
                results.put(REFERRING_DOMAINS, 0f);
            }
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

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

}
