package ws.palladian.retrieval;

import org.apache.commons.configuration2.Configuration;
import org.w3c.dom.Document;
import ws.palladian.helper.UrlHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Download content from a different IP via an API.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://www.scraperapi.com/documentation/">Scraper API Docs</a>
 * 11.04.2023
 */
public class ScraperApiDocumentRetriever extends JsEnabledDocumentRetriever {
    private final String apiKey;

    private int requestsLeft = Integer.MAX_VALUE;

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.scraperapi.key";

    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.SECONDS, 10);

    private final DocumentRetriever documentRetriever = new DocumentRetriever();

    public ScraperApiDocumentRetriever(Configuration configuration) {
        apiKey = configuration.getString(CONFIG_API_KEY);
    }

    @Override
    public Document getWebDocument(String url) {
        THROTTLE.hold();
        String requestUrl = " http://api.scraperapi.com?api_key=" + apiKey + "&render=" + useJsRendering + "&url=" + UrlHelper.encodeURIComponent(url);
        Set<String> waitSelectors = useJsRendering ? getWaitConditionsForUrl(url) : Collections.emptySet();
        if (!waitSelectors.isEmpty()) {
            Map<String, String> headers = Optional.ofNullable(documentRetriever.getGlobalHeaders()).orElse(new HashMap<>());
            JsonObject instruction = new JsonObject();
            instruction.put("type", "wait_for_selector");
            JsonObject selectorJo = new JsonObject();
            selectorJo.put("type", "css");
            selectorJo.put("value", String.join(",", waitSelectors));
            instruction.put("selector", selectorJo);
            headers.put("x-sapi-instruction_set", new JsonArray(Collections.singleton(instruction)).toString());
            documentRetriever.setGlobalHeaders(headers);
        }
        Document webDocument = documentRetriever.getWebDocument(requestUrl);
        if (webDocument != null) {
            webDocument.setDocumentURI(url);
            callRetrieverCallback(webDocument);
        }
        if (requestsLeft == Integer.MAX_VALUE) {
            computeRequestsLeft();
        } else {
            requestsLeft--;
            if (useJsRendering) { // 10 credits with JS rendering
                requestsLeft -= 9;
            }
            // refresh every once in a while
            if (requestsLeft < 200 && Math.random() < 0.1) {
                computeRequestsLeft();
            }
        }
        if (!waitSelectors.isEmpty() && documentRetriever.getGlobalHeaders() != null) {
            documentRetriever.getGlobalHeaders().remove("x-sapi-instruction_set");
        }
        return webDocument;
    }

    @Override
    public int requestsLeft() {
        return requestsLeft;
    }

    public int computeRequestsLeft() {
        try {
            JsonObject response = new DocumentRetriever().tryGetJsonObject("http://api.scraperapi.com/account?api_key=" + apiKey);
            requestsLeft = response.tryGetInt("requestLimit") - response.tryGetInt("requestCount");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return requestsLeft;
    }
}