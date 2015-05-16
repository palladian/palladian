package ws.palladian.retrieval;

import java.util.ArrayList;
import java.util.List;

public final class DefaultCookieStore implements CookieStore {

    private final List<Cookie> cookies = new ArrayList<>();

    @Override
    public List<Cookie> getCookies() {
        return cookies;
    }

    @Override
    public void addCookie(Cookie cookie) {
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
