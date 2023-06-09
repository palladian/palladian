package ws.palladian.retrieval;

import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import ws.palladian.helper.collection.CollectionHelper;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

final class ApacheCookieStoreAdapter implements CookieStore {

    private final ws.palladian.retrieval.CookieStore adapted;

    ApacheCookieStoreAdapter(ws.palladian.retrieval.CookieStore adapted) {
        this.adapted = adapted;
    }

    @Override
    public void addCookie(Cookie cookie) {
        try {
            adapted.addCookie(new ImmutableCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath()));
        } catch (Exception e) {
            // come on, broken cookies must not hold us back, we can eat cake instead
            e.printStackTrace();
        }
    }

    @Override
    public List<Cookie> getCookies() {
        return CollectionHelper.convertList(adapted.getCookies(), new Function<ws.palladian.retrieval.Cookie, Cookie>() {
            @Override
            public Cookie apply(ws.palladian.retrieval.Cookie input) {
                BasicClientCookie cookie = new BasicClientCookie(input.getName(), input.getValue());
                cookie.setDomain(input.getDomain());
                cookie.setPath(cookie.getPath());
                return cookie;
            }
        });
    }

    @Override
    public boolean clearExpired(Date date) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

}
