package ws.palladian.retrieval.ranking.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * RankingService implementation to find the visibility index for a given domain.
 * </p>
 *
 * @author David Urbansky
 */
public final class SistrixVisibilityIndex extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SistrixVisibilityIndex.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "sistrix";

    /** The ranking value types of this service **/
    public static final RankingType INDEX = new RankingType("index", "Sistrix Visibility Index", "The global visibility index of the domain according to Sistrix.");

    /** All available ranking types by {@link SistrixVisibilityIndex}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(INDEX);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);

        url = UrlHelper.getDomain(url, false);

        Double index = 0.;
        String requestUrl = "http://www.sichtbarkeitsindex.de/" + UrlHelper.encodeParameter(url);

        try {
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();

            if (response != null) {
                index = Double.valueOf(StringHelper.getSubstringBetween(response, "<h3>", "</h3>").replace(".", "").replace(",", "."));
                LOGGER.trace("Sistrix Visibility Index for " + url + " : " + index);
            }
        } catch (Exception e) {
            throw new RankingServiceException("url:" + url, e);
        }
        return builder.add(INDEX, index).create();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] a) throws RankingServiceException {
        SistrixVisibilityIndex gpl = new SistrixVisibilityIndex();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://wikipedia.org");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(SistrixVisibilityIndex.INDEX));
    }

}
