package ws.palladian.retrieval;

import org.apache.commons.configuration.Configuration;
import org.w3c.dom.Document;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.parser.ParserFactory;

import java.util.concurrent.TimeUnit;

/**
 * Use the Bright Data service to retrieve web documents, with optional JavaScript rendering.
 *
 * @author David Urbansky
 * @see <a href="https://docs.brightdata.com/scraping-automation/web-unlocker/introduction">Bright Data Docs</a>
 * @since 21.11.2025
 */
public class BrightDataDocumentRetriever extends JsEnabledDocumentRetriever {
    private final String apiKey;
    private final String zone;

    private int requestsLeft = Integer.MAX_VALUE;

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.brightdata.key";
    public static final String CONFIG_ZONE_KEY = "api.brightdata.zone";

    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 100);

    private final DocumentRetriever documentRetriever = new DocumentRetriever();

    public BrightDataDocumentRetriever(Configuration configuration) {
        apiKey = configuration.getString(CONFIG_API_KEY);
        zone = configuration.getString(CONFIG_ZONE_KEY);
        documentRetriever.getGlobalHeaders().put("Authorization", "Bearer " + apiKey);
        documentRetriever.getGlobalHeaders().put("Content-Type", "application/json");
    }

    @Override
    public Document getWebDocument(String url) {
        THROTTLE.hold();

        Document webDocument;
        try {
            String responseText = documentRetriever.postJsonObject("https://api.brightdata.com/request",
                    JsonObject.tryParse("{\n  \"zone\": \"" + zone + "\",\n  \"url\": \"" + url + "\",\n  \"format\": \"raw\",\n  \"method\": \"GET\",\n  \"country\": \"us\"}"),
                    false);
            webDocument = ParserFactory.createHtmlParser().parse(responseText);
            if (webDocument != null) {
                webDocument.setDocumentURI(url);
                callRetrieverCallback(webDocument);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return webDocument;
    }

    @Override
    public int requestsLeft() {
        return requestsLeft;
    }
}