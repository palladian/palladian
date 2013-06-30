package ws.palladian.retrieval;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

public final class HttpRequest {

    // XXX support further HTTP methods
    public enum HttpMethod {
        GET, POST, HEAD
    }

    private final String url;
    private final HttpMethod method;
    private final Map<String, String> headers;
    private final Map<String, String> parameters;

    public HttpRequest(HttpMethod method, String url) {
        Validate.notNull(method, "method must not be null");
        Validate.notEmpty(url, "url must not be empty");
        
        this.method = method;
        this.url = url;
        this.headers = CollectionHelper.newHashMap();
        this.parameters = CollectionHelper.newHashMap();
    }

    public HttpRequest(HttpMethod method, String url, Map<String, String> headers, Map<String, String> parameters) {
        Validate.notNull(method, "method must not be null");
        Validate.notEmpty(url, "url must not be empty");
        Validate.notNull(headers, "headers must not be null");
        Validate.notNull(parameters, "parameters must not be null");
        
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.parameters = parameters;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public void addHeader(String key, Object value) {
        Validate.notEmpty(key, "key must not be empty");
        Validate.notNull(value, "value must not be null");
        
        headers.put(key, value.toString());
    }

    public void addParameter(String key, Object value) {
        Validate.notEmpty(key, "key must not be empty");
        Validate.notNull(value, "value must not be null");
        
        parameters.put(key, value.toString());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HttpRequest [url=");
        builder.append(url);
        builder.append(", method=");
        builder.append(method);
        builder.append(", headers=");
        builder.append(headers);
        builder.append(", parameters=");
        builder.append(parameters);
        builder.append("]");
        return builder.toString();
    }

}
