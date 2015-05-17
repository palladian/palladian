package ws.palladian.retrieval;

import java.util.Map;

/**
 * A HTTP request.
 * 
 * @author pk
 */
public interface HttpRequest2 {

    /**
     * @return The full, absolute URL of the request (including URL parameters, if any), not <code>null</code>.
     */
    String getUrl();

    /**
     * @return The HTTP method, not <code>null</code>.
     */
    HttpMethod getMethod();

    /**
     * @return The HTTP headers, or empty Map, not <code>null</code>.
     */
    Map<String, String> getHeaders();

    /**
     * @return The HTTP body, or <code>null</code> in case the request has no body (GET, HEAD).
     */
    HttpEntity getEntity();

}
