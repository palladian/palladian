package ws.palladian.retrieval;

import java.util.Collection;

/**
 * A cookie store is responsible for storing cookies.
 *
 * @author Philipp Katz
 */
public interface CookieStore {

    /**
     * @return All cookies in this store, or an empty List.
     */
    Collection<Cookie> getCookies();

    /**
     * Adds a cookie to this cookie store.
     *
     * @param cookie The cookie to add, not <code>null</code>.
     */
    void addCookie(Cookie cookie);

}
