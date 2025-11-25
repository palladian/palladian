package ws.palladian.retrieval;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Retrieve a document using different document retrievers.
 *
 * @author David Urbansky
 */
public class CascadingDocumentRetriever extends JsEnabledDocumentRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(CascadingDocumentRetriever.class);

    /**
     * If this text is found, we try to resolve a captcha.
     */
    private List<String> badDocumentIndicatorTexts;
    private List<String> goodDocumentIndicatorTexts;

    /**
     * Keep track of failing requests.
     */
    private final Map<String, Integer[]> failingThresholdAndNumberOfRequestsToSkip = new ConcurrentHashMap<>();
    // web document retriever -> [failed requests, skipped requests, successful requests]
    private final Map<String, Integer[]> requestTracker = new ConcurrentHashMap<>();

    private final DocumentRetriever documentRetriever;
    private final RenderingDocumentRetrieverPool renderingDocumentRetrieverPool;
    private final List<JsEnabledDocumentRetriever> cloudDocumentRetrievers = new ArrayList<>();

    public CascadingDocumentRetriever(DocumentRetriever documentRetriever, RenderingDocumentRetrieverPool retrieverPool, JsEnabledDocumentRetriever... cloudDocumentRetrievers) {
        this.documentRetriever = documentRetriever;
        this.renderingDocumentRetrieverPool = retrieverPool;

        this.cloudDocumentRetrievers.addAll(Arrays.asList(cloudDocumentRetrievers));

        if (this.documentRetriever != null) {
            requestTracker.put(DocumentRetriever.class.getName(), new Integer[]{0, 0, 0});
        }
        if (this.renderingDocumentRetrieverPool != null) {
            requestTracker.put(RenderingDocumentRetrieverPool.class.getName(), new Integer[]{0, 0, 0});
        }
        for (JsEnabledDocumentRetriever cloudDocumentRetriever : cloudDocumentRetrievers) {
            if (cloudDocumentRetriever != null) {
                requestTracker.put(cloudDocumentRetriever.getClass().getName(), new Integer[]{0, 0, 0});
            }
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

    public Document getWebDocument(String url, Consumer<Pair<WebDocumentRetriever, Document>> retrieverDocumentCallback) {
        return getWebDocument(url, null, null, retrieverDocumentCallback);
    }

    @Override
    public Document getWebDocument(String url, Thread thread) {
        return getWebDocument(url, null, thread);
    }

    @Override
    public void close() {
        super.close();
        if (documentRetriever != null) {
            documentRetriever.close();
        }
        for (JsEnabledDocumentRetriever cloudDocumentRetriever : cloudDocumentRetrievers) {
            if (cloudDocumentRetriever != null) {
                cloudDocumentRetriever.close();
            }
        }
    }

    public Document getWebDocument(String url, List<String> resolvingExplanation, Thread thread) {
        return getWebDocument(url, resolvingExplanation, thread, null);
    }

    public Document getWebDocument(String url, List<String> resolvingExplanation, Thread thread, Consumer<Pair<WebDocumentRetriever, Document>> retrieverDocumentCallback) {
        if (resolvingExplanation == null) {
            resolvingExplanation = new ArrayList<>();
        }

        StopWatch stopWatch = new StopWatch();
        Document document = null;

        // try normal document retriever
        boolean goodDocument = false;
        if (documentRetriever != null && shouldMakeRequest(documentRetriever)) {
            try {
                if (thread != null) {
                    thread.setName("Retrieving (" + DocumentRetriever.class.getName() + "): " + url);
                }
                document = documentRetriever.getWebDocument(url);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            goodDocument = isGoodDocument(document);

            if (goodDocument && retrieverDocumentCallback != null) {
                retrieverDocumentCallback.accept(Pair.of(documentRetriever, document));
            }

            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(DocumentRetriever.class.getName(), goodDocument);
            resolvingExplanation.add(
                    "used normal document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(
                            DocumentRetriever.class.getName()));
        }

        if (!goodDocument && renderingDocumentRetrieverPool != null && shouldMakeRequest(RenderingDocumentRetrieverPool.class.getName())) {
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

                if (goodDocument && retrieverDocumentCallback != null) {
                    retrieverDocumentCallback.accept(Pair.of(renderingDocumentRetriever, document));
                }

                String message = goodDocument ? "success" : "fail";
                updateRequestTracker(RenderingDocumentRetrieverPool.class.getName(), goodDocument);
                resolvingExplanation.add(
                        "used rendering js retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(
                                RenderingDocumentRetrieverPool.class.getName()));
            } catch (Exception throwable) {
                throwable.printStackTrace();
            } finally {
                if (renderingDocumentRetriever != null) {
                    renderingDocumentRetriever.setWaitForElementsMap(Collections.emptyMap());
                    renderingDocumentRetrieverPool.recycle(renderingDocumentRetriever);
                }
            }
        }

        for (JsEnabledDocumentRetriever cloudDocumentRetriever : cloudDocumentRetrievers) {
            if (!goodDocument && cloudDocumentRetriever != null && shouldMakeRequest(cloudDocumentRetriever)) {
                if (thread != null) {
                    thread.setName("Retrieving (" + cloudDocumentRetriever.getClass().getSimpleName() + "): " + url);
                }
                configure(cloudDocumentRetriever);
                document = cloudDocumentRetriever.getWebDocument(url);
                goodDocument = isGoodDocument(document);

                if (goodDocument && retrieverDocumentCallback != null) {
                    retrieverDocumentCallback.accept(Pair.of(cloudDocumentRetriever, document));
                }

                String message = goodDocument ? "success" : "fail";
                updateRequestTracker(cloudDocumentRetriever.getClass().getName(), goodDocument);
                resolvingExplanation.add(
                        "used " + cloudDocumentRetriever.getClass().getSimpleName() + " document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement()
                                + " success count: " + getSuccessfulRequestCount(cloudDocumentRetriever.getClass().getName()));
                cloudDocumentRetriever.setWaitForElementsMap(Collections.emptyMap());
                if (goodDocument) {
                    break;
                }
            }
        }

        if (document != null) {
            callRetrieverCallback(document);
        }

        return document;
    }

    private void configure(JsEnabledDocumentRetriever renderingDocumentRetriever) {
        renderingDocumentRetriever.deleteAllCookies();
        renderingDocumentRetriever.setWaitForElementsMap(getWaitForElementsMap());
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

    private boolean shouldMakeRequest(String renderingDocumentRetrieverName) {
        Integer[] retrieverSettings = failingThresholdAndNumberOfRequestsToSkip.get(renderingDocumentRetrieverName);
        if (retrieverSettings == null) {
            return true;
        }
        Integer[] pair = requestTracker.get(renderingDocumentRetrieverName);
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

    private boolean shouldMakeRequest(WebDocumentRetriever renderingDocumentRetriever) {
        if (renderingDocumentRetriever != null && renderingDocumentRetriever instanceof JsEnabledDocumentRetriever
                && ((JsEnabledDocumentRetriever) renderingDocumentRetriever).requestsLeft() < 1) {
            return false;
        }
        return shouldMakeRequest(renderingDocumentRetriever.getClass().getName());
    }

    public Integer getSuccessfulRequestCount(String retrieverKey) {
        Integer[] integers = requestTracker.get(retrieverKey);
        if (integers == null || integers.length < 3) {
            return null;
        }
        return integers[2];
    }

    public Map<String, Integer[]> getRequestTracker() {
        return requestTracker;
    }

    private boolean isGoodDocument(Document document) {
        if (document == null) {
            return false;
        }
        HttpResult httpResult = (HttpResult) document.getUserData("httpResult");
        if (httpResult != null && httpResult.getStatusCode() == 403) {
            return false;
        }
        String s = HtmlHelper.getInnerXml(document);
        if (!getGoodDocumentIndicatorTexts().isEmpty()) {
            return StringHelper.containsAny(s, getGoodDocumentIndicatorTexts());
        }
        if (StringHelper.containsAny(s, getBadDocumentIndicatorTexts())) {
            return false;
        }
        return s.length() > 500;
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
        for (JsEnabledDocumentRetriever cloudDocumentRetriever : cloudDocumentRetrievers) {
            if (cloudDocumentRetriever != null) {
                originalValue = cloudDocumentRetriever.isUseJsRendering();
                cloudDocumentRetriever.setUseJsRendering(renderJs);
            }
        }
        return originalValue;
    }
}
