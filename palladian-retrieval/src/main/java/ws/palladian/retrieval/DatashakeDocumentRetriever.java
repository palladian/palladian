package ws.palladian.retrieval;

import org.apache.commons.configuration2.Configuration;
import org.w3c.dom.Document;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <p>
 * Download content from a different IP via a cloud.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://docs.datashake.com/webscraper/about/">Datashake API Docs</a>
 * 14.06.2021
 */
public class DatashakeDocumentRetriever extends WebDocumentRetriever {
    private boolean useJsRendering = false;

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.datashake.key";

    /**
     * Datashake allows 100 requests/second.
     */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.SECONDS, 100);

    private final DocumentRetriever documentRetriever = new DocumentRetriever();

    public DatashakeDocumentRetriever(Configuration configuration) {
        String apiKey = configuration.getString(CONFIG_API_KEY);
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("x-api-key", apiKey).create());
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
        String requestUrl = "https://webscraperapi.datashake.com/?render=" + useJsRendering + "&url=" + UrlHelper.encodeParameter(url);
        Document webDocument = documentRetriever.getWebDocument(requestUrl);
        if (webDocument != null) {
            webDocument.setDocumentURI(url);
            callRetrieverCallback(webDocument);
        }
        return webDocument;
    }

    @Override
    public void getWebDocuments(Collection<String> urls, Consumer<Document> callback, ProgressMonitor progressMonitor) {
        throw new UnsupportedOperationException("Can't do that yet.");
    }

    @Override
    public Set<Document> getWebDocuments(Collection<String> urls) {
        throw new UnsupportedOperationException("Can't do that yet.");
    }
}