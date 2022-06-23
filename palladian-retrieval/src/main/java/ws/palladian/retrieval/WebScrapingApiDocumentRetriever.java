package ws.palladian.retrieval;

import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.w3c.dom.Document;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

/**
 * <p>
 * Download content from a different IP via the cloud.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://app.webscrapingapi.com/documentation/basic-request">Web Scraping API Docs</a>
 *      12.06.2022
 */
public class WebScrapingApiDocumentRetriever extends WebDocumentRetriever {
    private final String apiKey;
    private boolean useJsRendering = false;

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.webscrapingapi.key";

    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.SECONDS, 5);

    private final DocumentRetriever documentRetriever = new DocumentRetriever();

    public WebScrapingApiDocumentRetriever(Configuration configuration) {
        apiKey = configuration.getString(CONFIG_API_KEY);
    }

    public boolean isUseJsRendering() {
        return useJsRendering;
    }

    public void setUseJsRendering(boolean useJsRendering) {
        this.useJsRendering = useJsRendering;
    }

    @Override
    public Document getWebDocument(String url) {
        THROTTLE.hold();
        String requestUrl = "https://api.webscrapingapi.com/v1?api_key=" + apiKey + "&render_js=" + useJsRendering + "&url=" + UrlHelper.encodeParameter(url);
        Document webDocument = documentRetriever.getWebDocument(requestUrl);
        if (webDocument != null) {
            webDocument.setDocumentURI(url);
            callRetrieverCallback(webDocument);
        }
        return webDocument;
    }
}