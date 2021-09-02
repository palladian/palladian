package ws.palladian.retrieval;

public class WaitException {
    private final String selector;
    private final String url;
    private final Exception exception;

    public WaitException(String url, Exception e, String selector) {
        this.url = url;
        this.exception = e;
        this.selector = selector;
    }

    public String getSelector() {
        return selector;
    }

    public String getUrl() {
        return url;
    }

    public Exception getException() {
        return exception;
    }
}