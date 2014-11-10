package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation for Yandex Citation Index value from Yandex.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public final class YandexCitationIndex extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(YandexCitationIndex.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "yandexcitationindex";

    /** The ranking value types of this service **/
    public static final RankingType CITATIONINDEX = new RankingType("citationindex", "Yandex Citation Index",
            "The Yandex Citation Index value from Yandex");

    /** All available ranking types by {@link YandexCitationIndex}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(CITATIONINDEX);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);
        if (isBlocked()) {
            return builder.create();
        }

        Integer citationIndex = null;
        try {
            String requestUrl = buildRequestUrl(url);
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();
            if (response != null) {
                citationIndex = 0;
                String leftBorder = UrlHelper.getDomain(url).replace("http://", "").replace("www.", "")
                        + "/\" target=\"_blank\">";
                String citationIndexString = StringHelper.getSubstringBetween(response, leftBorder, "</td>\n</tr>");
                if (citationIndexString.isEmpty()) {
                    // result stays 0 if response empty -> url not found
                    citationIndexString = StringHelper.getSubstringBetween(response, "b-cy_error-cy\">", "</p>");
                    citationIndexString = StringHelper.trim(citationIndexString.replaceAll(".*? — ?", ""));
                    citationIndexString = StringHelper
                            .replaceProtectedSpace(citationIndexString.replaceAll("<a.*", ""));
                    citationIndexString = citationIndexString.trim();
                } else {
                    citationIndexString = StringHelper.getSubstringBetween(citationIndexString, "<td>", null);
                }
                citationIndex = Integer.parseInt(citationIndexString);
                LOGGER.trace("Yandex Citation Index for " + url + " : " + citationIndex);
            }
        } catch (Exception e) {
            checkBlocked();
            throw new RankingServiceException("Exception " + e.getMessage(), e);
        }
        return builder.add(CITATIONINDEX, citationIndex).create();
    }

    /**
     * <p>
     * Build the request URL -> only the domain part of the URL is taken into account.
     * </p>
     * 
     * @param url The URL to search for.
     * @return The request URL.
     */
    private String buildRequestUrl(String url) {
        return "http://yaca.yandex.ru/yca/cy/ch/"+UrlHelper.getDomain(url).replace("http://", "");
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            String requestUrl = buildRequestUrl("http://yaca.yandex.ru/yca/cy/ch/www.google.com");
            status = retriever.httpGet(requestUrl).getStatusCode();
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        }
        if (status == 200) {
            blocked = false;
            lastCheckBlocked = new Date().getTime();
            return false;
        }
        blocked = true;
        lastCheckBlocked = new Date().getTime();
        LOGGER.error("Yandex Citation Index Ranking Service is momentarily blocked. Will check again in 1min. Try changing your IP-address.");
        return true;
    }

    @Override
    public boolean isBlocked() {
        if (new Date().getTime() - lastCheckBlocked < checkBlockedIntervall) {
            return blocked;
        } else {
            return checkBlocked();
        }
    }

    @Override
    public void resetBlocked() {
        blocked = false;
        lastCheckBlocked = new Date().getTime();
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
        YandexCitationIndex tic = new YandexCitationIndex();
        Ranking ranking = tic.getRanking("http://cinefreaks.com");
        System.out.println(ranking);

        ranking = tic.getRanking("http://en.wikipedia.org/Wiki/Germany");
        System.out.println(ranking);

        ranking = tic.getRanking("http://google.com");
        System.out.println(ranking);
    }

}
