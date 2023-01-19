package ws.palladian.retrieval;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.nlp.StringHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for {@link HttpRequest2} instances.
 *
 * @author Philipp Katz
 */
public final class HttpRequest2Builder implements Factory<HttpRequest2> {

    private final HttpMethod method;

    private final String baseUrl;

    private final List<Pair<String, String>> urlParams;

    private final Map<String, String> headers = new HashMap<>();

    private HttpEntity entity;

    public HttpRequest2Builder(HttpMethod method, String url) {
        Validate.notNull(method, "method must not be null");
        Validate.notEmpty(url, "url must not be empty");

        // unencoded spaces are illegal characters in URLs
        url = url.replace(" ", "%20");

        this.method = method;
        this.baseUrl = UrlHelper.parseBaseUrl(url);
        this.urlParams = UrlHelper.parseParams(url);
    }

    public HttpRequest2Builder addUrlParam(String key, String value) {
        Validate.notNull(key, "key must not be null");
        urlParams.add(Pair.of(key, value));
        return this;
    }

    public HttpRequest2Builder addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public HttpRequest2Builder addHeaders(Map<String, String> headers) {
        Validate.notNull(headers, "headers must not be null");
        this.headers.putAll(headers);
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

}
