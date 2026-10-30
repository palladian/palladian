package ws.palladian.retrieval;

import ws.palladian.helper.collection.CollectionHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class JsEnabledDocumentRetriever extends WebDocumentRetriever {
    protected int timeoutSeconds = 10;
    protected Consumer<WaitException> waitExceptionCallback;

    protected Map<String, String> cookies;

    protected boolean useJsRendering = false;

    /**
     * Domains whose content is complete in the raw HTML, so they are fetched without JavaScript rendering even when
     * {@link #useJsRendering} is on (rendering is the pricier request type at the cloud services; Oxylabs bills it by
     * the GB, subresources included). Stored as registered domains like the blacklist, so an entry covers every
     * sub-domain. See {@link #shouldRenderJs(String)}.
     */
    private final Set<String> noRenderDomains = new HashSet<>();

    /**
     * We can configure the retriever to wait for certain elements on certain URLs that match the given pattern.
     */
    protected Map<Pattern, Set<String>> waitForElementsMap = new HashMap<>();

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Deprecated
    public Map<Pattern, String> getWaitForElementMap() {
        Map<Pattern, String> waitForElementMap = new HashMap<>();
        for (Map.Entry<Pattern, Set<String>> entry : waitForElementsMap.entrySet()) {
            waitForElementMap.put(entry.getKey(), CollectionHelper.getFirst(entry.getValue()));
        }
        return waitForElementMap;
    }

    @Deprecated
    public void setWaitForElementMap(Map<Pattern, String> waitForElementMap) {
        this.waitForElementsMap = new HashMap<>();
        for (Map.Entry<Pattern, String> entry : waitForElementMap.entrySet()) {
            this.waitForElementsMap.put(entry.getKey(), Collections.singleton(entry.getValue()));
        }
    }

    public Map<Pattern, Set<String>> getWaitForElementsMap() {
        return waitForElementsMap;
    }

    public void setWaitForElementsMap(Map<Pattern, Set<String>> waitForElementsMap) {
        this.waitForElementsMap = waitForElementsMap;
    }

    public Consumer<WaitException> getWaitExceptionCallback() {
        return waitExceptionCallback;
    }

    public void setWaitExceptionCallback(Consumer<WaitException> waitExceptionCallback) {
        this.waitExceptionCallback = waitExceptionCallback;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public void deleteAllCookies() {
        this.cookies = null;
    }

    /** Get how many requests are left. */
    public abstract int requestsLeft();

    public Set<String> getWaitConditionsForUrl(String url) {
        for (Map.Entry<Pattern, Set<String>> entry : waitForElementsMap.entrySet()) {
            if (entry.getKey().matcher(url).find()) {
                return entry.getValue();
            }
        }
        return Collections.emptySet();
    }

    public boolean isUseJsRendering() {
        return useJsRendering;
    }

    public void setUseJsRendering(boolean useJsRendering) {
        this.useJsRendering = useJsRendering;
    }

    /**
     * Never render JavaScript for this domain, in retrievers that decide rendering through
     * {@link #shouldRenderJs(String)} (the metered cloud proxies: Oxylabs, BrightData, Zyte, Crawlbase, ScrapingBee,
     * ScraperAPI). Retrievers that always render, such as PhantomJS Cloud, ignore the list. The argument is normalized
     * like {@link #addBlacklistDomain(String)}: bare domain, sub-domain / www host or full URL; {@code null}/blank is
     * ignored.
     */
    public void addNoRenderDomain(String domain) {
        String normalized = toRegisteredDomain(domain);
        if (normalized != null) {
            noRenderDomains.add(normalized);
        }
    }

    public Set<String> getNoRenderDomains() {
        return noRenderDomains;
    }

    /**
     * Whether the request for this URL renders JavaScript. Retrievers ask this per URL instead of reading
     * {@link #useJsRendering}, so the no-render list needs no per-request toggling.
     *
     * @return {@code true} if {@link #isUseJsRendering()} is on and the URL's registered domain is not a
     * {@link #addNoRenderDomain(String) no-render domain}; an undeterminable domain renders.
     */
    public boolean shouldRenderJs(String url) {
        return useJsRendering && !isListedDomain(noRenderDomains, url);
    }
}
