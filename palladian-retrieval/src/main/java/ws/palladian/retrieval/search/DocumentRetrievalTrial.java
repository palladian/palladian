package ws.palladian.retrieval.search;

import ws.palladian.retrieval.HttpResult;

/**
 * If we fail to parse a URL to a document we pass the work that has already been done (the http request and the cleaned URL) to an error callback that can try to do something with that.
 */
public class DocumentRetrievalTrial {
    private String url = null;
    private HttpResult httpResult = null;

    public DocumentRetrievalTrial(String url, HttpResult httpResult) {
        this.url = url;
        this.httpResult = httpResult;
    }

    public HttpResult getHttpResult() {
        return httpResult;
    }

    public void setHttpResult(HttpResult httpResult) {
        this.httpResult = httpResult;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
