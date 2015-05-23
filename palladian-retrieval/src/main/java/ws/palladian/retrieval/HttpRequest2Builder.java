package ws.palladian.retrieval;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.nlp.StringHelper;

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

    public HttpRequest2Builder setBasicAuth(String username, String password) {
        StringBuilder temp = new StringBuilder();
        temp.append(username != null ? username : StringUtils.EMPTY);
        temp.append(':');
        temp.append(password != null ? password : StringUtils.EMPTY);
        String authString = "Basic " + StringHelper.encodeBase64(temp.toString());
        return addHeader("Authorization", authString);
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
