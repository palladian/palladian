package ws.palladian.retrieval;

import java.util.Map;

public class ImmutableHttpRequest2 implements HttpRequest2 {

    private final String url;
    private final HttpMethod method;
    private final Map<String, String> headers;
    private final HttpEntity entity;

    /** Instantiated by {@link HttpRequest2Builder}. */
    ImmutableHttpRequest2(String url, HttpMethod method, Map<String, String> headers, HttpEntity entity) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.entity = entity;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public HttpEntity getEntity() {
        return entity;
    }

}
