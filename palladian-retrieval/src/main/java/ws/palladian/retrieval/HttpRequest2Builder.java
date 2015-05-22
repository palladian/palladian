package ws.palladian.retrieval;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
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
        this.urlParams = UrlHelper.parseParams(url);
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
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(baseUrl);
        if (urlParams.size() > 0) {
            urlBuilder.append('?');
            urlBuilder.append(UrlHelper.createParameterString(urlParams));
        }
        return new ImmutableHttpRequest2(urlBuilder.toString(), method, headers, entity);
    }

    // utility methods

    static String parseBaseUrl(String url) {
        int questionIdx = url.indexOf("?");
        return questionIdx != -1 ? url.substring(0, questionIdx) : url;
    }

}
