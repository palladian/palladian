package ws.palladian.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.html.HtmlHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Retrieve a document using different document retrievers.
 * </p>
 *
 * @author David Urbansky
 */
public class CascadingDocumentRetriever extends JsEnabledDocumentRetriever {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CascadingDocumentRetriever.class);
    public static final String RETRIEVER_PLAIN = "Plain";
    public static final String RETRIEVER_RENDERING_POOL = "RenderingPool";
    public static final String RETRIEVER_PHANTOM_JS_CLOUD = "PhantomJsCloud";
    public static final String RETRIEVER_PROXY_CRAWL = "ProxyCrawl";
    public static final String RETRIEVER_SCRAPING_BEE = "ScrapingBee";

    /**
     * If this text is found, we try to resolve a captcha.
     */
    private String badDocumentIndicatorText;

    /**
     * Keep track of failing requests.
     */
    private final Map<String, Integer[]> failingThresholdAndNumberOfRequestsToSkip = new HashMap<>();
    // web document retriever -> [failed requests, skipped requests, successful requests]
    private final Map<String, Integer[]> requestTracker = new HashMap<>();

    private final DocumentRetriever documentRetriever;
    private final RenderingDocumentRetrieverPool renderingDocumentRetrieverPool;
    private final PhantomJsDocumentRetriever cloudDocumentRetriever;
    private final ProxyCrawlDocumentRetriever cloudDocumentRetriever2;
    private final ScrapingBeeDocumentRetriever cloudDocumentRetriever3;

    public CascadingDocumentRetriever(DocumentRetriever documentRetriever, RenderingDocumentRetrieverPool retrieverPool, PhantomJsDocumentRetriever cloudDocumentRetriever,
                                      ProxyCrawlDocumentRetriever cloudDocumentRetriever2, ScrapingBeeDocumentRetriever cloudDocumentRetriever3) {
        this.documentRetriever = documentRetriever;
        this.renderingDocumentRetrieverPool = retrieverPool;
        this.cloudDocumentRetriever = cloudDocumentRetriever;
        this.cloudDocumentRetriever2 = cloudDocumentRetriever2;
        this.cloudDocumentRetriever3 = cloudDocumentRetriever3;

        if (this.documentRetriever != null) {
            requestTracker.put(RETRIEVER_PLAIN, new Integer[]{0, 0, 0});
        }
        if (this.renderingDocumentRetrieverPool != null) {
            requestTracker.put(RETRIEVER_RENDERING_POOL, new Integer[]{0, 0, 0});
        }
        if (this.cloudDocumentRetriever != null) {
            requestTracker.put(RETRIEVER_PHANTOM_JS_CLOUD, new Integer[]{0, 0, 0});
        }
        if (this.cloudDocumentRetriever2 != null) {
            requestTracker.put(RETRIEVER_PROXY_CRAWL, new Integer[]{0, 0, 0});
        }
        if (this.cloudDocumentRetriever3 != null) {
            requestTracker.put(RETRIEVER_SCRAPING_BEE, new Integer[]{0, 0, 0});
        }
    }

    public String getBadDocumentIndicatorText() {
        return badDocumentIndicatorText;
    }

    public void setBadDocumentIndicatorText(String badDocumentIndicatorText) {
        this.badDocumentIndicatorText = badDocumentIndicatorText;
    }

    public String getText(String url, List<String> resolvingExplanation) {
        Document webDocument = getWebDocument(url, resolvingExplanation);
        if (webDocument == null) {
            return null;
        }
        return HtmlHelper.getInnerXml(webDocument);
    }

    /**
     * Do not use a certain retriever for numberOfRequestsToSkip requests if it failed more than failingThreshold times.
     *
     * @param failingThreshold       The number of requests to fail before ignoring.
     * @param numberOfRequestsToSkip The number of requests to skip before trying that retriever again.
     */
    public void pauseFailingRetriever(String retrieverKey, Integer failingThreshold, Integer numberOfRequestsToSkip) {
        failingThresholdAndNumberOfRequestsToSkip.put(retrieverKey, new Integer[]{failingThreshold, numberOfRequestsToSkip});
    }

    @Override
    public Document getWebDocument(String url) {
        return getWebDocument(url, null);
    }

    public Document getWebDocument(String url, List<String> resolvingExplanation) {
        if (resolvingExplanation == null) {
            resolvingExplanation = new ArrayList<>();
        }

        StopWatch stopWatch = new StopWatch();
        Document document = null;

        // try normal document retriever
        boolean goodDocument = false;
        if (documentRetriever != null && shouldMakeRequest(RETRIEVER_PLAIN)) {
            try {
                document = documentRetriever.getWebDocument(url);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            goodDocument = isGoodDocument(document);
            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(RETRIEVER_PLAIN, goodDocument);
            resolvingExplanation.add("used normal document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: "
                    + getSuccessfulRequestCount(RETRIEVER_PLAIN));
        }

        if (!goodDocument && renderingDocumentRetrieverPool != null && shouldMakeRequest(RETRIEVER_RENDERING_POOL)) {
            // try rendering retriever
            try {
                RenderingDocumentRetriever renderingDocumentRetriever = renderingDocumentRetrieverPool.acquire();
                configure(renderingDocumentRetriever);
                document = renderingDocumentRetriever.getWebDocument(url);

                goodDocument = isGoodDocument(document);
                renderingDocumentRetrieverPool.recycle(renderingDocumentRetriever);

                String message = goodDocument ? "success" : "fail";
                updateRequestTracker(RETRIEVER_RENDERING_POOL, goodDocument);
                resolvingExplanation.add("used rendering js retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: "
                        + getSuccessfulRequestCount(RETRIEVER_RENDERING_POOL));
            } catch (Exception throwable) {
                throwable.printStackTrace();
            }
        }

        if (!goodDocument && cloudDocumentRetriever != null && shouldMakeRequest(RETRIEVER_PHANTOM_JS_CLOUD)) {
            configure(cloudDocumentRetriever);
            document = cloudDocumentRetriever.getWebDocument(url);
            goodDocument = isGoodDocument(document);
            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(RETRIEVER_PHANTOM_JS_CLOUD, goodDocument);
            resolvingExplanation.add("used phantom js cloud document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: "
                    + getSuccessfulRequestCount(RETRIEVER_PHANTOM_JS_CLOUD));
        }

        if (!goodDocument && cloudDocumentRetriever2 != null && shouldMakeRequest(RETRIEVER_PROXY_CRAWL)) {
            document = cloudDocumentRetriever2.getWebDocument(url);
            goodDocument = isGoodDocument(document);
            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(RETRIEVER_PROXY_CRAWL, goodDocument);
            resolvingExplanation.add("used proxy crawl document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: "
                    + getSuccessfulRequestCount(RETRIEVER_PROXY_CRAWL));
        }

        if (!goodDocument && cloudDocumentRetriever3 != null && shouldMakeRequest(RETRIEVER_SCRAPING_BEE)) {
            document = cloudDocumentRetriever3.getWebDocument(url);
            goodDocument = isGoodDocument(document);
            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(RETRIEVER_SCRAPING_BEE, goodDocument);
            resolvingExplanation.add("used scraping bee document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: "
                    + getSuccessfulRequestCount(RETRIEVER_SCRAPING_BEE));
        }

        return document;
    }

    private void configure(JsEnabledDocumentRetriever renderingDocumentRetriever) {
        renderingDocumentRetriever.getWaitForElementMap().clear();
        renderingDocumentRetriever.setWaitForElementMap(getWaitForElementMap());
        renderingDocumentRetriever.setTimeoutSeconds(getTimeoutSeconds());
        renderingDocumentRetriever.setWaitExceptionCallback(getWaitExceptionCallback());
    }

    private void updateRequestTracker(String retrieverKey, boolean goodDocument) {
        Integer[] pair = requestTracker.get(retrieverKey);
        if (pair == null) {
            return;
        }
        if (!goodDocument) {
            pair[0]++;
        } else {
            pair[2]++;
        }
    }

    private boolean shouldMakeRequest(String retrieverKey) {
        Integer[] retrieverSettings = failingThresholdAndNumberOfRequestsToSkip.get(retrieverKey);
        if (retrieverSettings == null) {
            return true;
        }
        Integer[] pair = requestTracker.get(retrieverKey);
        if (pair == null) {
            return true;
        }
        // check whether enough requests were skipped, if so, reset
        if (pair[0] >= retrieverSettings[0]) {
            if (pair[1] >= retrieverSettings[1]) {
                pair[0] = 0;
                pair[1] = 0;
                return true;
            } else {
                pair[1]++;
                return false;
            }
        }
        return true;
    }

    public Integer getSuccessfulRequestCount(String retrieverKey) {
        Integer[] integers = requestTracker.get(retrieverKey);
        if (integers == null || integers.length < 3) {
            return null;
        }
        return integers[2];
    }

    private boolean isGoodDocument(Document document) {
        if (document == null) {
            return false;
        }
        String s = HtmlHelper.documentToReadableText(document);
        if (badDocumentIndicatorText != null && s.contains(badDocumentIndicatorText)) {
            return false;
        }
        return !(s.isEmpty());
    }

    @Override
    public void setTimeoutSeconds(int timeoutSeconds) {
        super.setTimeoutSeconds(timeoutSeconds);
        documentRetriever.getHttpRetriever().setConnectionTimeout((int) TimeUnit.SECONDS.toMillis(getTimeoutSeconds()));
    }
}
