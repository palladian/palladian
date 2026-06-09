package ws.palladian.retrieval;

import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.NoSuchSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Retrieve a document using different document retrievers.
 *
 * @author David Urbansky
 */
public class CascadingDocumentRetriever extends JsEnabledDocumentRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(CascadingDocumentRetriever.class);

    private final ExecutorService trackerLogExecutor;

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

    private String userAgent;
    private final DocumentRetriever documentRetriever;
    private final RenderingDocumentRetrieverPool renderingDocumentRetrieverPool;
    /**
     * Optional second rendering pool that runs <em>after</em> the regular rendering pool but
     * <em>before</em> the cloud retrievers. Intended for stealthier setups (e.g. CloakBrowser)
     * which are heavier/slower but more likely to bypass bot management. Either pool may be
     * {@code null}, so callers can run only-cloak, only-rendering, or both side-by-side.
     */
    private final RenderingDocumentRetrieverPool cloakBrowserDocumentRetrieverPool;
    private final List<JsEnabledDocumentRetriever> cloudDocumentRetrievers = new ArrayList<>();

    /**
     * Domains where the rendering retriever returned a page with an interactive challenge
     * (e.g. Cloudflare Turnstile, Vercel checkpoint). Mapped to the timestamp when the domain
     * was added so entries can expire after {@link #INTERACTIVE_CHALLENGE_SKIP_DURATION_MS}.
     */
    private static final Map<String, Long> interactiveChallengeSkipDomains = new ConcurrentHashMap<>();

    /**
     * How long (ms) to skip the rendering retriever for a domain after an interactive challenge
     * was detected. Default: 1 hour.
     */
    private static final long INTERACTIVE_CHALLENGE_SKIP_DURATION_MS = TimeUnit.HOURS.toMillis(1);

    /**
     * Substrings that indicate the page contains an <em>interactive</em> challenge that a headless
     * browser cannot solve. When the rendering retriever returns a document matching any of these,
     * the domain is added to {@link #interactiveChallengeSkipDomains}.
     */
    private static final List<String> INTERACTIVE_CHALLENGE_INDICATORS = Arrays.asList("<title>Just a moment...</title>", "challenges.cloudflare.com", "Verify you are human",
            "Vercel Security Checkpoint", "captcha-delivery.com", "<title>CAPTCHA page</title>", "<title>Captcha Interception</title>",
            "Please complete the security check to access", "Please complete a security check to continue", "<title>Are you a human?</title>", "<title>Bot or Not?</title>",
            "Verifying you are human. This may take a few seconds", "_Incapsula_Resource");

    /**
     * Substrings that indicate the page contains an <em>auto-solving</em> bot-management challenge
     * (primarily Akamai Bot Manager interstitials). Such pages usually clear themselves within a
     * few seconds via a <code>location.reload(true)</code> issued by the sensor script once the
     * challenge token is accepted. For these we do NOT skip the rendering retriever — instead we
     * give the live driver a short additional wait window and re-read the document.
     */
    private static final List<String> AUTO_SOLVING_CHALLENGE_INDICATORS = Arrays.asList("sec-if-cpt-container", "techlab-cdn.com", "scf-akamai-logo-sec-abc",
            "Powered and protected by", "sec-bc-tile-container");

    /**
     * Maximum time (seconds) we wait on the live driver for an auto-solving challenge to resolve
     * before re-reading the page. Intentionally separate from (and added on top of) the normal
     * render watchdog timeout, because the browser has already loaded once — we are only waiting
     * for the sensor round-trip + reload.
     */
    private int autoSolvingChallengeWaitSeconds = 10;

    // separate executor used only for applying a hard timeout to rendering work
    private static final ExecutorService RENDER_WATCHDOG_EXEC = Executors.newCachedThreadPool(new ThreadFactory() {
        private final ThreadFactory delegate = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = delegate.newThread(r);
            t.setName("render-watchdog-" + t.getId());
            t.setDaemon(true);
            return t;
        }
    });

    public CascadingDocumentRetriever(DocumentRetriever documentRetriever, RenderingDocumentRetrieverPool retrieverPool, JsEnabledDocumentRetriever... cloudDocumentRetrievers) {
        this(documentRetriever, retrieverPool, null, cloudDocumentRetrievers);
    }

    public CascadingDocumentRetriever(JsEnabledDocumentRetriever... cloudDocumentRetrievers) {
        this(null, null, null, cloudDocumentRetrievers);
    }

    public CascadingDocumentRetriever(DocumentRetriever documentRetriever, RenderingDocumentRetrieverPool retrieverPool, RenderingDocumentRetrieverPool cloakBrowserPool,
            JsEnabledDocumentRetriever... cloudDocumentRetrievers) {
        this(Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cascading-retriever-tracker-log");
            t.setDaemon(true);
            return t;
        }), documentRetriever, retrieverPool, cloakBrowserPool, cloudDocumentRetrievers);
        ((ScheduledExecutorService) trackerLogExecutor).scheduleAtFixedRate(this::logTrackerInfo, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Full constructor allowing both a regular rendering pool and an additional stealth-rendering
     * pool (e.g. CloakBrowser). Any of the three local retrievers may be {@code null}.
     *
     * @param trackerLogExecutor      the executor service that periodically logs usage information
     * @param documentRetriever       plain HTTP retriever (cheap fast path), may be {@code null}
     * @param retrieverPool           regular rendering pool (e.g. headless Chrome), may be {@code null}
     * @param cloakBrowserPool        additional stealth rendering pool (e.g. CloakBrowser), may be {@code null}
     * @param cloudDocumentRetrievers cloud-based retrievers tried last
     */
    public CascadingDocumentRetriever(ExecutorService trackerLogExecutor, DocumentRetriever documentRetriever, RenderingDocumentRetrieverPool retrieverPool, RenderingDocumentRetrieverPool cloakBrowserPool,
            JsEnabledDocumentRetriever... cloudDocumentRetrievers) {
        this.documentRetriever = documentRetriever;
        this.renderingDocumentRetrieverPool = retrieverPool;
        this.cloakBrowserDocumentRetrieverPool = cloakBrowserPool;

        this.cloudDocumentRetrievers.addAll(Arrays.asList(cloudDocumentRetrievers));

        if (this.documentRetriever != null) {
            requestTracker.put(DocumentRetriever.class.getName(), new Integer[]{0, 0, 0});
        }
        if (this.renderingDocumentRetrieverPool != null) {
            requestTracker.put(RenderingDocumentRetrieverPool.class.getName(), new Integer[]{0, 0, 0});
        }
        if (this.cloakBrowserDocumentRetrieverPool != null) {
            requestTracker.put(this.cloakBrowserDocumentRetrieverPool.getClass().getName(), new Integer[]{0, 0, 0});
        }
        for (JsEnabledDocumentRetriever cloudDocumentRetriever : cloudDocumentRetrievers) {
            if (cloudDocumentRetriever != null) {
                requestTracker.put(cloudDocumentRetriever.getClass().getName(), new Integer[]{0, 0, 0});
            }
        }

        this.trackerLogExecutor = trackerLogExecutor;
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

    public JsonObject getJsonObject(String url) {
        JsonObject jsonObject = null;
        String text = tryGetPlainTextWithSimpleRetriever(url);
        if (!StringHelper.nullOrEmpty(text)) {
            jsonObject = JsonObject.tryParse(text);
        }
        if (jsonObject == null) {
            text = tryGetPlainTextWithCloudRetrievers(url);
            if (!StringHelper.nullOrEmpty(text)) {
                jsonObject = JsonObject.tryParse(text);
            }
        }
        return jsonObject;
    }

    public JsonArray getJsonArray(String url) {
        JsonArray jsonArray = null;
        String text = tryGetPlainTextWithSimpleRetriever(url);
        if (!StringHelper.nullOrEmpty(text)) {
            jsonArray = JsonArray.tryParse(text);
        }
        if (jsonArray == null) {
            text = tryGetPlainTextWithCloudRetrievers(url);
            if (!StringHelper.nullOrEmpty(text)) {
                jsonArray = JsonArray.tryParse(text);
            }
        }
        return jsonArray;
    }

    public String getPlainText(String url) {
        String text = tryGetPlainTextWithSimpleRetriever(url);

        if (StringHelper.nullOrEmpty(text)) {
            text = tryGetPlainTextWithCloudRetrievers(url);
        }
        return text;
    }

    private String tryGetPlainTextWithSimpleRetriever(String url) {
        String text = null;
        if (documentRetriever != null) {
            text = documentRetriever.getText(url);
        }
        return text;
    }

    private String tryGetPlainTextWithCloudRetrievers(String url) {
        String text = null;
        for (JsEnabledDocumentRetriever cloudDocumentRetriever : cloudDocumentRetrievers) {
            if (cloudDocumentRetriever instanceof BrightDataDocumentRetriever) {
                text = cloudDocumentRetriever.getText(url);
                if (!StringHelper.nullOrEmpty(text)) {
                    break;
                }
            }
        }
        return text;
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

    public Document getRenderedWebDocument(String url) {
        return getWebDocument(url, null, null, null, true);
    }

    public Document getWebDocument(String url, Consumer<Pair<WebDocumentRetriever, Document>> retrieverDocumentCallback) {
        return getWebDocument(url, null, null, retrieverDocumentCallback);
    }

    public Document getRenderedWebDocument(String url, Consumer<Pair<WebDocumentRetriever, Document>> retrieverDocumentCallback) {
        return getWebDocument(url, null, null, retrieverDocumentCallback, true);
    }

    @Override
    public Document getWebDocument(String url, Thread thread) {
        return getWebDocument(url, null, thread);
    }

    @Override
    public void close() {
        if (trackerLogExecutor != null) {
            trackerLogExecutor.shutdownNow();
        }
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
        return getWebDocument(url, resolvingExplanation, thread, retrieverDocumentCallback, false);
    }

    public Document getWebDocument(String url, List<String> resolvingExplanation, Thread thread, Consumer<Pair<WebDocumentRetriever, Document>> retrieverDocumentCallback,
            boolean mustBeRendering) {
        if (resolvingExplanation == null) {
            resolvingExplanation = new ArrayList<>();
        }

        StopWatch stopWatch = new StopWatch();
        Document document = null;

        // try normal document retriever
        boolean goodDocument = false;
        if (documentRetriever != null && !mustBeRendering && shouldMakeRequest(documentRetriever) && !shouldSkipLocalRetrievalForDomain(url)) {
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

            // if the rendering retriever got an interactive challenge page, remember the
            // domain so we skip rendering and go straight to cloud retrievers next time
            if (!goodDocument) {
                checkAndMarkInteractiveChallenge(url, document);
            }

            String message = goodDocument ? "success" : "fail";
            updateRequestTracker(DocumentRetriever.class.getName(), goodDocument);
            resolvingExplanation.add(
                    "used normal document retriever: " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(
                            DocumentRetriever.class.getName()));
            LOGGER.info("Made request with DocumentRetriever to " + url + " - goodDocument: " + goodDocument + " - time: " + stopWatch.getElapsedTimeString());
        }

        if (!goodDocument && cloakBrowserDocumentRetrieverPool != null && shouldMakeRequest(cloakBrowserDocumentRetrieverPool.getClass().getName())
                && !shouldSkipLocalRetrievalForDomain(url)) {
            String label = cloakBrowserDocumentRetrieverPool.getClass().getSimpleName();
            RenderingPoolResult r = tryRenderingPool(cloakBrowserDocumentRetrieverPool, cloakBrowserDocumentRetrieverPool.getClass().getName(), label, url, thread, stopWatch,
                    resolvingExplanation, retrieverDocumentCallback);
            if (r.document != null) {
                document = r.document;
            }
            goodDocument = r.goodDocument;
        }

        if (!goodDocument && renderingDocumentRetrieverPool != null && shouldMakeRequest(RenderingDocumentRetrieverPool.class.getName()) && !shouldSkipLocalRetrievalForDomain(
                url)) {
            RenderingPoolResult r = tryRenderingPool(renderingDocumentRetrieverPool, RenderingDocumentRetrieverPool.class.getName(), "rendering js retriever", url, thread,
                    stopWatch, resolvingExplanation, retrieverDocumentCallback);
            if (r.document != null) {
                document = r.document;
            }
            goodDocument = r.goodDocument;
        }

        for (JsEnabledDocumentRetriever cloudDocumentRetriever : cloudDocumentRetrievers) {
            if (!goodDocument && cloudDocumentRetriever != null && shouldMakeRequest(cloudDocumentRetriever)) {
                if (thread != null) {
                    thread.setName("Retrieving (" + cloudDocumentRetriever.getClass().getSimpleName() + "): " + url);
                }
                configure(cloudDocumentRetriever);
                StopWatch stopWatch1 = new StopWatch();
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

                LOGGER.info("Made request with " + cloudDocumentRetriever.getClass().getSimpleName() + " to " + url + " - goodDocument: " + goodDocument + " - time: "
                        + stopWatch1.getElapsedTimeString());

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

    /** Holds the outcome of a single rendering-pool attempt cascade. */
    private static final class RenderingPoolResult {
        Document document;
        boolean goodDocument;
    }

    /**
     * Run the rendering branch against the given pool. Encapsulates the watchdog timeout,
     * Akamai-style auto-solving challenge wait, and the broken-session one-time retry that
     * are shared by both the regular rendering pool and the optional CloakBrowser pool.
     */
    private RenderingPoolResult tryRenderingPool(RenderingDocumentRetrieverPool pool, String trackerKey, String label, String url, Thread thread, StopWatch stopWatch,
            List<String> resolvingExplanation, Consumer<Pair<WebDocumentRetriever, Document>> retrieverDocumentCallback) {
        RenderingPoolResult result = new RenderingPoolResult();
        boolean retried = false;

        for (int attempt = 0; attempt < 2; attempt++) { // bounded: at most 2 attempts
            RenderingDocumentRetriever renderingDocumentRetriever = null;
            boolean retryDueToBroken = false;

            try {
                if (thread != null) {
                    thread.setName("Retrieving (" + label + "): " + url);
                }
                renderingDocumentRetriever = pool.poll(3, TimeUnit.SECONDS);
                if (renderingDocumentRetriever == null) {
                    LOGGER.warn("Could not acquire rendering document retriever ({}) from pool for {}", label, url);
                    break;
                }

                configure(renderingDocumentRetriever);

                final RenderingDocumentRetriever rdrFinal = renderingDocumentRetriever;
                Future<Document> future = RENDER_WATCHDOG_EXEC.submit(() -> rdrFinal.getWebDocument(url));

                try {
                    // hard-stop protection: if Selenium hangs, we stop waiting and replace the driver.
                    result.document = future.get(getTimeoutSeconds(), TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    future.cancel(true);
                    rdrFinal.markInvalidatedByCallback();
                    retryDueToBroken = false;
                    throw new RuntimeException("Render watchdog timeout after " + getTimeoutSeconds() + "s for " + url, te);
                }

                result.goodDocument = isGoodDocument(result.document);

                // Auto-solving challenge (Akamai etc.) — wait on live driver and re-read.
                if (!result.goodDocument && isAutoSolvingChallenge(result.document)) {
                    LOGGER.info("Auto-solving challenge detected for {} ({}) — waiting up to {}s on live driver", url, label, autoSolvingChallengeWaitSeconds);
                    StopWatch waitStopWatch = new StopWatch();
                    boolean resolved = rdrFinal.awaitChallengeResolution(AUTO_SOLVING_CHALLENGE_INDICATORS, autoSolvingChallengeWaitSeconds);
                    try {
                        result.document = rdrFinal.getCurrentWebDocument();
                    } catch (Exception e) {
                        LOGGER.debug("Could not re-read document after challenge wait", e);
                    }
                    result.goodDocument = isGoodDocument(result.document);
                    resolvingExplanation.add(
                            "auto-solving challenge " + (resolved && result.goodDocument ? "resolved" : "unresolved") + " after " + waitStopWatch.getElapsedTimeString());
                    LOGGER.info("Auto-solving challenge wait for {} ({}) — resolved: {}, goodDocument: {}, time: {}", url, label, resolved, result.goodDocument,
                            waitStopWatch.getElapsedTimeString());
                }

                if (!result.goodDocument) {
                    checkAndMarkInteractiveChallenge(url, result.document);
                }

                retryDueToBroken = (result.document == null && renderingDocumentRetriever.isInvalidatedByCallback());

                if (!retryDueToBroken) {
                    if (result.goodDocument && retrieverDocumentCallback != null) {
                        retrieverDocumentCallback.accept(Pair.of(renderingDocumentRetriever, result.document));
                    }

                    String message = result.goodDocument ? "success" : "fail";
                    updateRequestTracker(trackerKey, result.goodDocument);
                    resolvingExplanation.add(
                            "used " + label + ": " + message + " in " + stopWatch.getElapsedTimeStringAndIncrement() + " success count: " + getSuccessfulRequestCount(trackerKey));
                }
            } catch (NoSuchSessionException nse) {
                retryDueToBroken = true;
                LOGGER.warn("Rendering retriever ({}) session invalid; will retry once with a fresh instance for {}", label, url);
            } catch (Exception throwable) {
                throwable.printStackTrace();
            } finally {
                if (renderingDocumentRetriever != null) {
                    renderingDocumentRetriever.setWaitForElementsMap(Collections.emptyMap());
                    try {
                        boolean sessionGone = renderingDocumentRetriever.getDriver() == null || renderingDocumentRetriever.getDriver().getSessionId() == null
                                || renderingDocumentRetriever.isInvalidatedByCallback();

                        if (sessionGone) {
                            pool.replace(renderingDocumentRetriever);
                        } else {
                            pool.recycle(renderingDocumentRetriever);
                        }
                    } catch (Exception ex) {
                        pool.replace(renderingDocumentRetriever);
                    } finally {
                        renderingDocumentRetriever.clearInvalidation();
                    }
                }
            }

            LOGGER.info("Made request with " + label + " to " + url + " - goodDocument: " + result.goodDocument + " - time: " + stopWatch.getElapsedTimeString());

            if (retryDueToBroken && !retried) {
                retried = true;
                continue;
            }
            break;
        }
        return result;
    }

    private void updateRequestTracker(String retrieverKey, boolean goodDocument) {
        Integer[] pair = requestTracker.get(retrieverKey);
        if (pair == null) {
            return;
        }
        if (!goodDocument) {
            pair[0]++;
        } else {
            pair[0] = 0;
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

    public Map<String, Integer[]> getFailingThresholdAndNumberOfRequestsToSkip() {
        return failingThresholdAndNumberOfRequestsToSkip;
    }

    /**
     * Check whether the rendering retriever should be skipped for the given URL because
     * a previous attempt returned an interactive challenge page for that domain.
     */
    private boolean shouldSkipLocalRetrievalForDomain(String url) {
        String domain = UrlHelper.getDomain(url, false, false);
        if (domain == null) {
            return false;
        }
        Long addedAt = interactiveChallengeSkipDomains.get(domain);
        if (addedAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - addedAt > INTERACTIVE_CHALLENGE_SKIP_DURATION_MS) {
            interactiveChallengeSkipDomains.remove(domain);
            LOGGER.info("Interactive-challenge skip expired for domain: {}", domain);
            return false;
        }
        LOGGER.info("Skipping rendering retriever for domain {} (interactive challenge detected previously)", domain);
        return true;
    }

    /**
     * If the document contains indicators of an interactive challenge (Cloudflare Turnstile,
     * Vercel checkpoint, etc.), mark the domain so the rendering retriever is skipped for
     * subsequent requests.
     * <p>
     * Auto-solving challenges (Akamai Bot Manager, see {@link #AUTO_SOLVING_CHALLENGE_INDICATORS})
     * are intentionally NOT included here — they usually clear themselves given a bit of wait
     * time on the live driver, so we keep the rendering retriever eligible for the domain.
     */
    private void checkAndMarkInteractiveChallenge(String url, Document document) {
        if (document == null) {
            return;
        }
        String html = HtmlHelper.getInnerXml(document);
        if (StringHelper.containsAny(html, INTERACTIVE_CHALLENGE_INDICATORS)) {
            String domain = UrlHelper.getDomain(url, false, false);
            if (domain != null) {
                interactiveChallengeSkipDomains.put(domain, System.currentTimeMillis());
                LOGGER.info("Interactive challenge detected for domain: {} — rendering retriever will be skipped for this domain", domain);
            }
        }
    }

    /**
     * Detect whether the document looks like an auto-solving bot-management challenge page
     * (Akamai Bot Manager interstitial, etc.) that should resolve itself if we just wait a bit
     * longer on the live driver.
     */
    private boolean isAutoSolvingChallenge(Document document) {
        if (document == null) {
            return false;
        }
        String html = HtmlHelper.getInnerXml(document);
        return StringHelper.containsAny(html, AUTO_SOLVING_CHALLENGE_INDICATORS);
    }

    public int getAutoSolvingChallengeWaitSeconds() {
        return autoSolvingChallengeWaitSeconds;
    }

    public void setAutoSolvingChallengeWaitSeconds(int autoSolvingChallengeWaitSeconds) {
        this.autoSolvingChallengeWaitSeconds = autoSolvingChallengeWaitSeconds;
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
            boolean good = StringHelper.containsAny(s, getGoodDocumentIndicatorTexts());
            if (good) {
                return true;
            }
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

    public Document getWithFakeUserAgent(String url) {
        setFakeUserAgent();
        Document document = documentRetriever.getWebDocument(url);
        resetFakeUserAgent();
        return document;
    }

    private void setFakeUserAgent() {
        userAgent = documentRetriever.getGlobalHeaders().get("User-Agent");
        documentRetriever.getGlobalHeaders().put("User-Agent", "PostmanRuntime/7.51.0");
    }

    private void resetFakeUserAgent() {
        if (userAgent != null) {
            documentRetriever.getGlobalHeaders().put("User-Agent", userAgent);
        }
    }

    public String getUsageSummaryMessage() {
        StringBuilder sb = new StringBuilder("CascadingDocumentRetriever tracker status:\n");

        sb.append("  requestTracker:\n");
        for (Map.Entry<String, Integer[]> entry : requestTracker.entrySet()) {
            Integer[] v = entry.getValue();
            sb.append("    ").append(entry.getKey()).append(" -> [failed=").append(v[0]).append(", skipped=").append(v[1]).append(", successful=").append(v[2]).append("]\n");
        }

        sb.append("  failingThresholdAndNumberOfRequestsToSkip:\n");
        for (Map.Entry<String, Integer[]> entry : failingThresholdAndNumberOfRequestsToSkip.entrySet()) {
            Integer[] v = entry.getValue();
            sb.append("    ").append(entry.getKey()).append(" -> [failingThreshold=").append(v[0]).append(", numberOfRequestsToSkip=").append(v[1]).append("]\n");
        }
        return sb.toString();
    }

    private void logTrackerInfo() {
        try {
            LOGGER.info(getUsageSummaryMessage());
        } catch (Exception e) {
            LOGGER.warn("Failed to log tracker info", e);
        }
    }
}
