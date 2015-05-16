package ws.palladian.retrieval;

import java.util.List;

/**
 * A cookie store is responsible for storing cookies.
 * 
 * @author pk
 */
public interface CookieStore {

    /**
     * @return All cookies in this store, or an empty List.
     */
    List<Cookie> getCookies();

    /**
     * Adds a cookie to this cookie store.
     * 
     * @param cookie The cookie to add, not <code>null</code>.
     */
    void addCookie(Cookie cookie);

}
