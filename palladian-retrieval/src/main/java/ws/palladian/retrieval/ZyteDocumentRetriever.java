package ws.palladian.retrieval;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.parser.ParserFactory;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Use the Zyte API to retrieve web documents, with optional JavaScript rendering.
 *
 * @author David Urbansky
 * @see <a href="https://docs.zyte.com/zyte-api/usage/reference.html">Zyte API Reference</a>
 * @since 20.03.2026
 */
public class ZyteDocumentRetriever extends JsEnabledDocumentRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZyteDocumentRetriever.class);

    private final String apiKey;

    private int requestsLeft = Integer.MAX_VALUE;

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.zyte.key";

    private static final String API_URL = "https://api.zyte.com/v1/extract";

    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 100);

    private final DocumentRetriever documentRetriever = new DocumentRetriever();

    public ZyteDocumentRetriever(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    public ZyteDocumentRetriever(String apiKey) {
        this.apiKey = apiKey;
        String basicAuth = Base64.getEncoder().encodeToString((apiKey + ":").getBytes());
        documentRetriever.getGlobalHeaders().put("Authorization", "Basic " + basicAuth);
        documentRetriever.getGlobalHeaders().put("Content-Type", "application/json");
    }

    @Override
    public Document getWebDocument(String url) {
        Document webDocument;
        try {
            String responseText = getText(url);
            if (responseText == null) {
                return null;
            }
            webDocument = ParserFactory.createHtmlParser().parse(responseText);
            if (webDocument != null) {
                webDocument.setDocumentURI(url);
                callRetrieverCallback(webDocument);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
        return webDocument;
    }

    @Override
    public String getText(String url) {
        THROTTLE.hold();

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.put("url", url);

            if (useJsRendering) {
                requestBody.put("browserHtml", true);
            } else {
                requestBody.put("httpResponseBody", true);
            }

            String response = documentRetriever.postJsonObject(API_URL, requestBody, false);

            if (response == null) {
                return null;
            }

            if (useJsRendering) {
                // browserHtml returns the HTML directly in the "browserHtml" field
                JsonObject jsonResponse = JsonObject.tryParse(response);
                if (jsonResponse != null && jsonResponse.containsKey("browserHtml")) {
                    return jsonResponse.getString("browserHtml");
                }
                return response;
            } else {
                // httpResponseBody returns base64-encoded content in the "httpResponseBody" field
                JsonObject jsonResponse = JsonObject.tryParse(response);
                if (jsonResponse != null && jsonResponse.containsKey("httpResponseBody")) {
                    String base64Body = jsonResponse.getString("httpResponseBody");
                    return new String(Base64.getDecoder().decode(base64Body));
                }
                return response;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public int requestsLeft() {
        return requestsLeft;
    }
}
