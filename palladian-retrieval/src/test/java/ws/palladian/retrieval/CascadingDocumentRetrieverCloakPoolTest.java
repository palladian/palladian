package ws.palladian.retrieval;

import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertNull;

/**
 * Verifies the hot-swappable cloak-pool accessors on {@link CascadingDocumentRetriever}. Offline: the cascade is built
 * with {@code null} local pools, so only the getter and the null-safe branch of the setter are exercised. The non-null
 * swap path needs a real rendering pool (which launches Chrome) and is verified via WebKnox's RetrieverService health
 * daemon plus manual prod checks.
 */
public class CascadingDocumentRetrieverCloakPoolTest {

    /** Minimal cloud-retriever stub so the cascade has at least one (non-pool) candidate. */
    private static final class NoopCloud extends JsEnabledDocumentRetriever {
        @Override
        public Document getWebDocument(String url) {
            return null;
        }

        @Override
        public int requestsLeft() {
            return Integer.MAX_VALUE;
        }
    }

    @Test
    public void cloakPoolStartsNullAndSetterIsNullSafe() {
        // casts disambiguate the (DocumentRetriever, pool, pool, JsEnabledDocumentRetriever...) constructor
        CascadingDocumentRetriever cascade = new CascadingDocumentRetriever(
                (DocumentRetriever) null, (RenderingDocumentRetrieverPool) null, (RenderingDocumentRetrieverPool) null, new NoopCloud());
        try {
            assertNull("cascade built with a null cloak pool must report null", cascade.getCloakBrowserDocumentRetrieverPool());
            cascade.setCloakBrowserDocumentRetrieverPool(null); // disabling the stealth stage must not throw
            assertNull("setting null keeps the stealth stage disabled", cascade.getCloakBrowserDocumentRetrieverPool());
        } finally {
            cascade.close();
        }
    }
}
