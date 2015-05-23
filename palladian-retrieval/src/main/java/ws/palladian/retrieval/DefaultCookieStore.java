package ws.palladian.retrieval;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class DefaultCookieStore implements CookieStore {

    private final Set<Cookie> cookies = new HashSet<>();

    @Override
    public Collection<Cookie> getCookies() {
        return cookies;
    }

    @Override
    public void addCookie(Cookie cookie) {
        // remove old cookie first; value might have changed; not considered for #equals
        cookies.remove(cookie);
        cookies.add(cookie);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DefaultCookieStore [cookies=");
        builder.append(cookies);
        builder.append("]");
        return builder.toString();
    }

}
