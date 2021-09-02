package ws.palladian.retrieval;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class JsEnabledDocumentRetriever extends WebDocumentRetriever {
    protected int timeoutSeconds = 10;
    protected Consumer<WaitException> waitExceptionCallback;

    /**
     * We can configure the retriever to wait for certain elements on certain URLs that match the given pattern.
     */
    protected Map<Pattern, String> waitForElementMap = new HashMap<>();

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Map<Pattern, String> getWaitForElementMap() {
        return waitForElementMap;
    }

    public void setWaitForElementMap(Map<Pattern, String> waitForElementMap) {
        this.waitForElementMap = waitForElementMap;
    }

    public Consumer<WaitException> getWaitExceptionCallback() {
        return waitExceptionCallback;
    }

    public void setWaitExceptionCallback(Consumer<WaitException> waitExceptionCallback) {
        this.waitExceptionCallback = waitExceptionCallback;
    }
}
