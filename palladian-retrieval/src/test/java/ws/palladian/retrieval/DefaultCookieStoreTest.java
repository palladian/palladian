package ws.palladian.retrieval;

import org.junit.Test;
import ws.palladian.helper.collection.CollectionHelper;

import static org.junit.Assert.assertEquals;

public class DefaultCookieStoreTest {
    @Test
    public void testDefaultCookieStore() {
        DefaultCookieStore cookieStore = new DefaultCookieStore();
        cookieStore.addCookie(new ImmutableCookie("key", "old_value", "example.com", "/"));
        cookieStore.addCookie(new ImmutableCookie("key", "new_value", "example.com", "/"));
        assertEquals("new_value", CollectionHelper.getFirst(cookieStore.getCookies()).getValue());
    }
}
