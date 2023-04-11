package ws.palladian.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.util.*;
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
    public static final String RETRIEVER_SCRAPER_API = "ScraperApi";

    /**
     * If this text is found, we try to resolve a captcha.
     */
    private List<String> badDocumentIndicatorTexts;
    private List<String> goodDocumentIndicatorTexts;

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
    private final ScraperApiDocumentRetriever cloudDocumentRetriever4;

    public CascadingDocumentRetriever(DocumentRetriever documentRetriever, RenderingDocumentRetrieverPool retrieverPool, PhantomJsDocumentRetriever cloudDocumentRetriever,
            ProxyCrawlDocumentRetriever cloudDocumentRetriever2, ScrapingBeeDocumentRetriever cloudDocumentRetriever3) {
        this(documentRetriever, retrieverPool, cloudDocumentRetriever, cloudDocumentRetriever2, cloudDocumentRetriever3, null);
    }

    public CascadingDocumentRetriever(DocumentRetriever documentRetriever, RenderingDocumentRetrieverPool retrieverPool, PhantomJsDocumentRetriever cloudDocumentRetriever,
            ProxyCrawlDocumentRetriever cloudDocumentRetriever2, ScrapingBeeDocumentRetriever cloudDocumentRetriever3, ScraperApiDocumentRetriever cloudDocumentRetriever4) {
        this.documentRetriever = documentRetriever;
        this.renderingDocumentRetrieverPool = retrieverPool;
        this.cloudDocumentRetriever = cloudDocumentRetriever;
        this.cloudDocumentRetriever2 = cloudDocumentRetriever2;
        this.cloudDocumentRetriever3 = cloudDocumentRetriever3;
        this.cloudDocumentRetriever4 = cloudDocumentRetriever4;

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
        if (this.cloudDocumentRetriever4 != null) {
            requestTracker.put(RETRIEVER_SCRAPER_API, new Integer[]{0, 0, 0});
        }
    }

    public String getBadDocumentIndicatorText() {
        return badDocumentIndicatorTexts == null ? null : CollectionHelper.getFirst(badDocumentIndicatorTexts);
    }

    public void setBadDocumentIndicatorText(String badDocumentIndicatorText) {
        this.badDocumentIndicatorTexts = Arrays.asList(badDocumentIndicatorText);
    }

    public List<String> getBadDocumentIndicatorTexts() {
        return badDocumentIndicatorTexts != null ? badDocumentIndicatorTexts : new ArrayList<>(1);
    }

    public void setBadDocumentIndicatorTexts(List<String> badDocumentIndicatorTexts) {
        this.badDocumentIndicatorTexts = badDocumentIndicatorTexts;
    }

    public void addBadDocumentIndicatorText(String badDocumentIndicatorText) {
        if (this.badDocumentIndicatorTexts == null) {
            this.badDocumentIndicatorTexts = new ArrayList<>();
        }
        badDocumentIndicatorTexts.add(badDocumentIndicatorText);
    }

    public List<String> getGoodDocumentIndicatorTexts() {
        return goodDocumentIndicatorTexts != null ? goodDocumentIndicatorTexts : new ArrayList<>(1);
    }

    public void addGoodDocumentIndicatorText(String goodDocumentIndicatorText) {
        if (this.goodDocumentIndicatorTexts == null) {
            this.goodDocumentIndicatorTexts = new ArrayList<>();
        }
        goodDocumentIndicatorTexts.add(goodDocumentIndicatorText);
    }

    public String getText(String url, List<String> resolvingExplanation) {
        Document webDocument = getWebDocument(url, resolvingExplanation, null);
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
        return getWebDocument(url, null, null);
    }

    @Override
    public Document getWebDocument(String url, Thread thread) {
        return getWebDocument(url, null, thread);
    }

    public Document getWebDocument(String url, List<String> resolvingExplanation, Thread thread) {
        if (resolvingExplanation == null) {
            resolvingExplanation = new ArrayList<>();
        }

        StopWatch stopWatch = new StopWatch();
        Document document = null;

        // try normal document retriever
        boolean goodDocument = false;
        if (documentRetriever != null && shouldMakeRequest(RETRIEVER_PLAIN, null)) {
            try {
                if (thread != null) {
                    thread.setName("Retrieving (plain): " + url);
                }
                document = documentRetriever.getWebDocument(url);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            goodDocument = isGoodDocument(document);
            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(RETRIEVER_PLAIN, goodDocument);
            resolvingExplanation.add(
                    "used normal document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(
                            RETRIEVER_PLAIN));
        }

        if (!goodDocument && renderingDocumentRetrieverPool != null && shouldMakeRequest(RETRIEVER_RENDERING_POOL, null)) {
            // try rendering retriever
            RenderingDocumentRetriever renderingDocumentRetriever = null;

            try {
                if (thread != null) {
                    thread.setName("Retrieving (rendering): " + url);
                }
                renderingDocumentRetriever = renderingDocumentRetrieverPool.acquire();
                //                for (Consumer<Document> retrieverCallback : getRetrieverCallbacks()) {
                //                    renderingDocumentRetriever.addRetrieverCallback(retrieverCallback);
                //                }

                configure(renderingDocumentRetriever);
                document = renderingDocumentRetriever.getWebDocument(url);

                //                renderingDocumentRetriever.getRetrieverCallbacks().clear();

                goodDocument = isGoodDocument(document);

                String message = goodDocument ? "success" : "fail";
                updateRequestTracker(RETRIEVER_RENDERING_POOL, goodDocument);
                resolvingExplanation.add(
                        "used rendering js retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(
                                RETRIEVER_RENDERING_POOL));
            } catch (Exception throwable) {
                throwable.printStackTrace();
            } finally {
                if (renderingDocumentRetriever != null) {
                    renderingDocumentRetrieverPool.recycle(renderingDocumentRetriever);
                }
            }
        }

        if (!goodDocument && cloudDocumentRetriever != null && shouldMakeRequest(RETRIEVER_PHANTOM_JS_CLOUD, cloudDocumentRetriever)) {
            if (thread != null) {
                thread.setName("Retrieving (phantomjs): " + url);
            }
            configure(cloudDocumentRetriever);
            document = cloudDocumentRetriever.getWebDocument(url);
            goodDocument = isGoodDocument(document);
            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(RETRIEVER_PHANTOM_JS_CLOUD, goodDocument);
            resolvingExplanation.add(
                    "used phantom js cloud document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(
                            RETRIEVER_PHANTOM_JS_CLOUD));
        }

        if (!goodDocument && cloudDocumentRetriever2 != null && shouldMakeRequest(RETRIEVER_PROXY_CRAWL, cloudDocumentRetriever2)) {
            if (thread != null) {
                thread.setName("Retrieving (proxycrawl): " + url);
            }
            configure(cloudDocumentRetriever2);
            document = cloudDocumentRetriever2.getWebDocument(url);
            goodDocument = isGoodDocument(document);
            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(RETRIEVER_PROXY_CRAWL, goodDocument);
            resolvingExplanation.add(
                    "used proxy crawl document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(
                            RETRIEVER_PROXY_CRAWL));
        }

        if (!goodDocument && cloudDocumentRetriever3 != null && shouldMakeRequest(RETRIEVER_SCRAPING_BEE, cloudDocumentRetriever3)) {
            if (thread != null) {
                thread.setName("Retrieving (scrapingbee): " + url);
            }
            configure(cloudDocumentRetriever3);
            document = cloudDocumentRetriever3.getWebDocument(url);
            goodDocument = isGoodDocument(document);
            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(RETRIEVER_SCRAPING_BEE, goodDocument);
            resolvingExplanation.add(
                    "used scraping bee document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(
                            RETRIEVER_SCRAPING_BEE));
        }

        if (!goodDocument && cloudDocumentRetriever4 != null && shouldMakeRequest(RETRIEVER_SCRAPER_API, cloudDocumentRetriever4)) {
            if (thread != null) {
                thread.setName("Retrieving (scraperapi): " + url);
            }
            configure(cloudDocumentRetriever4);
            document = cloudDocumentRetriever4.getWebDocument(url);
            goodDocument = isGoodDocument(document);
            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(RETRIEVER_SCRAPER_API, goodDocument);
            resolvingExplanation.add(
                    "used scraperapi document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(
                            RETRIEVER_SCRAPER_API));
        }

        if (document != null) {
            callRetrieverCallback(document);
        }

        return document;
    }

    private void configure(JsEnabledDocumentRetriever renderingDocumentRetriever) {
        renderingDocumentRetriever.deleteAllCookies();
        renderingDocumentRetriever.getWaitForElementMap().clear();
        renderingDocumentRetriever.setWaitForElementMap(getWaitForElementMap());
        renderingDocumentRetriever.setTimeoutSeconds(getTimeoutSeconds());
        renderingDocumentRetriever.setWaitExceptionCallback(getWaitExceptionCallback());
        renderingDocumentRetriever.setCookies(this.cookies);
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

    private boolean shouldMakeRequest(String retrieverKey, JsEnabledDocumentRetriever renderingDocumentRetriever) {
        Integer[] retrieverSettings = failingThresholdAndNumberOfRequestsToSkip.get(retrieverKey);
        if (renderingDocumentRetriever != null && renderingDocumentRetriever.requestsLeft() < 1) {
            return false;
        }
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
        String s = HtmlHelper.getInnerXml(document);
        if (!getGoodDocumentIndicatorTexts().isEmpty()) {
            if (StringHelper.containsAny(s, getGoodDocumentIndicatorTexts())) {
                return true;
            } else {
                return false;
            }
        }
        if (StringHelper.containsAny(s, getBadDocumentIndicatorTexts())) {
            return false;
        }
        return !(s.isEmpty());
    }

    @Override
    public void setTimeoutSeconds(int timeoutSeconds) {
        super.setTimeoutSeconds(timeoutSeconds);
        if (documentRetriever != null) {
            documentRetriever.getHttpRetriever().setConnectionTimeout((int) TimeUnit.SECONDS.toMillis(getTimeoutSeconds()));
        }
    }

    @Override
    public int requestsLeft() {
        return Integer.MAX_VALUE;
    }

    public boolean renderJs(boolean renderJs) {
        boolean originalValue = false;
        if (cloudDocumentRetriever2 != null) {
            originalValue = cloudDocumentRetriever2.isUseJsRendering();
            cloudDocumentRetriever2.setUseJsRendering(renderJs);
        }
        if (cloudDocumentRetriever3 != null) {
            originalValue = cloudDocumentRetriever3.isUseJsRendering();
            cloudDocumentRetriever3.setUseJsRendering(renderJs);
        }
        return originalValue;
    }
}
