package ws.palladian.retrieval;

import org.apache.commons.configuration.Configuration;
import org.w3c.dom.Document;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

import java.util.concurrent.TimeUnit;

/**
 * Download content from a different IP via Crawlbase.
 *
 * @author David Urbansky
 * @see <a href="https://crawlbase.com/docs/crawling-api/">Crawlbase API Docs</a>
 * @since 18.05.2021
 */
public class ProxyCrawlDocumentRetriever extends JsEnabledDocumentRetriever {
    private final String apiKeyPlain;
    private final String apiKeyJs;

    private boolean useJsRendering = false;

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_TOKEN_PLAIN = "api.proxycrawl.tokenplain";
    public static final String CONFIG_TOKEN_JS = "api.proxycrawl.tokenjs";

    /**
     * ProxyCrawl allows 20 requests/second.
     */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.SECONDS, 20);

    private final DocumentRetriever documentRetriever = new DocumentRetriever();

    public ProxyCrawlDocumentRetriever(Configuration configuration) {
        this.apiKeyPlain = configuration.getString(CONFIG_TOKEN_PLAIN);
        this.apiKeyJs = configuration.getString(CONFIG_TOKEN_JS);
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
        String requestUrl = "https://api.crawlbase.com/?token=" + getActiveToken() + "&url=" + UrlHelper.encodeParameter(url);
        if (!getWaitConditionsForUrl(url).isEmpty()) {
            requestUrl += "&ajax_wait=true"; // no direct support for wait conditions, try to at least wait for all async requests to finish
        }
        Document d = documentRetriever.getWebDocument(requestUrl);
        if (d != null) {
            try {
                HttpResult httpResult = (HttpResult) d.getUserData(DocumentRetriever.HTTP_RESULT_KEY);
                int pcStatus = Integer.parseInt(httpResult.getHeaderString("pc_status"));
                if (pcStatus != 200) {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            d.setDocumentURI(url);
            callRetrieverCallback(d);
        }
        return d;
    }

    @Override
    public int requestsLeft() {
        return Integer.MAX_VALUE;
    }

    private String getActiveToken() {
        return isUseJsRendering() ? apiKeyJs : apiKeyPlain;
    }
}