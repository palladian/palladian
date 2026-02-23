package ws.palladian.retrieval;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.parser.ParserFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

/**
 * Use the Oxylabs Web Unblocker service to retrieve web documents, with optional JavaScript rendering.
 *
 * @author David Urbansky
 * @see <a href="https://developers.oxylabs.io/advanced-proxy-solutions/web-unblocker/making-requests">Oxylabs Docs</a>
 * @since 23.02.2026
 */
public class OxylabsDocumentRetriever extends JsEnabledDocumentRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(OxylabsDocumentRetriever.class);

    private final String username;
    private final String password;

    private int requestsLeft = Integer.MAX_VALUE;

    /**
     * Identifier for the credentials when supplied via {@link Configuration}.
     */
    public static final String CONFIG_USERNAME = "api.oxylabs.username";
    public static final String CONFIG_PASSWORD = "api.oxylabs.password";

    private static final String PROXY_HOST = "unblock.oxylabs.io";
    private static final int PROXY_PORT = 60000;

    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 100);

    private final DocumentRetriever documentRetriever = new DocumentRetriever();

    public OxylabsDocumentRetriever(Configuration configuration) {
        username = configuration.getString(CONFIG_USERNAME);
        password = configuration.getString(CONFIG_PASSWORD);
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
            HttpRequest2 request = new HttpRequest2Builder(HttpMethod.GET, url).addHeader("X-Oxylabs-Render", "html").addHeader("X-Oxylabs-Geo-Location", "United States").create();

            HttpResult result = execute(request);

            if (result != null && result.getStatusCode() == 200) {
                return result.getStringContent();
            } else if (result != null) {
                LOGGER.warn("Oxylabs returned status " + result.getStatusCode() + " for URL " + url + ": " + result.getStringContent());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public int requestsLeft() {
        return requestsLeft;
    }

    public BufferedImage getWebImage(String url) {
        THROTTLE.hold();

        try {
            HttpRequest2 request = new HttpRequest2Builder(HttpMethod.GET, url).addHeader("X-Oxylabs-Geo-Location", "United States").create();

            HttpResult result = execute(request);

            if (result != null && result.getStatusCode() == 200) {
                byte[] content = result.getContent();
                if (content != null && content.length > 0) {
                    return ImageIO.read(new ByteArrayInputStream(content));
                }
            } else if (result != null) {
                LOGGER.warn("Oxylabs returned status " + result.getStatusCode() + " for URL " + url + ": " + result.getStringContent());
            }
        } catch (Exception e) {
            LOGGER.error("Oxylabs image retrieval failed for URL " + url + ": " + e.getMessage(), e);
        }
        return null;
    }

    private HttpResult execute(HttpRequest2 request) {
        try {
            ImmutableProxy proxy = new ImmutableProxy(PROXY_HOST, PROXY_PORT, username, password);
            HttpRetriever httpRetriever = documentRetriever.getHttpRetriever();
            httpRetriever.setProxyProvider(new ProxyProvider() {
                @Override
                public Proxy getProxy(String url) {
                    return proxy;
                }

                @Override
                public void removeProxy(Proxy proxy, int statusCode) {
                }

                @Override
                public void removeProxy(Proxy proxy, Throwable error) {
                }

                @Override
                public void removeProxy(Proxy proxy) {
                }

                @Override
                public void promoteProxy(Proxy proxy) {
                }
            });
            return httpRetriever.execute(request);
        } catch (Exception e) {
            LOGGER.error("Oxylabs request failed: " + e.getMessage(), e);
            return null;
        }
    }
}
