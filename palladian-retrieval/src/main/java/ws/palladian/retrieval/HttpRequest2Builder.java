package ws.palladian.retrieval;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.functional.Factory;

/**
 * Builder for {@link HttpRequest2} instances.
 * 
 * @author pk
 */
public final class HttpRequest2Builder implements Factory<HttpRequest2> {

    private final HttpMethod method;

    private final String baseUrl;

    private final Map<String, String> urlParams;

    private final Map<String, String> headers = new HashMap<>();

    private HttpEntity entity;

    public HttpRequest2Builder(HttpMethod method, String url) {
        Validate.notNull(method, "method must not be null");
        Validate.notEmpty(url, "url must not be empty");
        this.method = method;
        this.baseUrl = parseBaseUrl(url);
        this.urlParams = parseParams(url);
    }

    public HttpRequest2Builder addUrlParam(String key, String value) {
        urlParams.put(key, value);
        return this;
    }

    public HttpRequest2Builder addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public HttpRequest2Builder setEntity(HttpEntity entity) {
        this.entity = entity;
        return this;
    }

    @Override
    public HttpRequest2 create() {
        return new ImmutableHttpRequest2(createFullUrl(baseUrl, urlParams), method, headers, entity);
    }

    // utility methods

    static String parseBaseUrl(String url) {
        int questionIdx = url.indexOf("?");
        return questionIdx != -1 ? url.substring(0, questionIdx) : url;
    }

    static Map<String, String> parseParams(String url) {
        Map<String, String> params = new HashMap<>();

        int questionIdx = url.indexOf("?");
        if (questionIdx == -1) { // no parameters in URL
            return params;
        }

        String paramSubString = url.substring(questionIdx + 1);
        String[] paramSplit = paramSubString.split("&");
        for (String param : paramSplit) {
            String[] keyValue = param.split("=");
            params.put(keyValue[0], keyValue[1]);
        }
        return params;
    }

    static String createFullUrl(String baseUrl, Map<String, String> urlParams) {
        StringBuilder builder = new StringBuilder();
        builder.append(baseUrl);
        boolean first = true;
        for (Entry<String, String> keyValue : urlParams.entrySet()) {
            builder.append(first ? '?' : '&');
            first = false;
            builder.append(keyValue.getKey());
            builder.append('=');
            builder.append(keyValue.getValue());
        }
        return builder.toString();
    }

}
