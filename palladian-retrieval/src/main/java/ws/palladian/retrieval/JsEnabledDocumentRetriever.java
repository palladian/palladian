package ws.palladian.retrieval;

import ws.palladian.helper.collection.CollectionHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class JsEnabledDocumentRetriever extends WebDocumentRetriever {
    protected int timeoutSeconds = 10;
    protected Consumer<WaitException> waitExceptionCallback;

    protected Map<String, String> cookies;

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
}
