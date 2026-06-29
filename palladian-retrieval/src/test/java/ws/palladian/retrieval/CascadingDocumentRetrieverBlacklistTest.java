package ws.palladian.retrieval;

import org.junit.Test;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that {@link CascadingDocumentRetriever} never reaches its (paid/metered) cloud retrievers for a
 * blacklisted domain, but still does for a non-blacklisted one.
 *
 * <p>Offline: the cascade is built with {@code null} local retriever/pools so the only candidate is a recording
 * stub cloud retriever, and the test asserts on whether that stub was invoked.
 */
public class CascadingDocumentRetrieverBlacklistTest {

    /** A cloud retriever stub that records every URL it is asked to fetch (and returns nothing). */
    private static final class RecordingCloudRetriever extends JsEnabledDocumentRetriever {
        final List<String> requestedUrls = new ArrayList<>();

        @Override
        public Document getWebDocument(String url) {
            requestedUrls.add(url);
            return null;
        }

        @Override
        public int requestsLeft() {
            return Integer.MAX_VALUE;
        }
    }

    private static CascadingDocumentRetriever cascadeWith(RecordingCloudRetriever stub) {
        // casts disambiguate the (DocumentRetriever, pool, pool, JsEnabledDocumentRetriever...) constructor
        return new CascadingDocumentRetriever((DocumentRetriever) null, (RenderingDocumentRetrieverPool) null, (RenderingDocumentRetrieverPool) null, stub);
    }

    @Test
    public void blacklistedDomainSkipsCloudRetrievers() {
        RecordingCloudRetriever stub = new RecordingCloudRetriever();
        CascadingDocumentRetriever cascade = cascadeWith(stub);
        try {
            cascade.addBlacklistDomain("toyota-4runner.org");

            Document result = cascade.getWebDocument("https://www.toyota-4runner.org/thread/1");

            assertNull("blacklisted domain must not yield a paid-fetched document", result);
            assertTrue("cloud retriever must NOT be called for a blacklisted domain", stub.requestedUrls.isEmpty());
        } finally {
            cascade.close();
        }
    }

    @Test
    public void nonBlacklistedDomainStillUsesCloudRetrievers() {
        RecordingCloudRetriever stub = new RecordingCloudRetriever();
        CascadingDocumentRetriever cascade = cascadeWith(stub);
        try {
            cascade.addBlacklistDomain("toyota-4runner.org");

            cascade.getWebDocument("https://www.allrecipes.com/recipe/123");

            assertTrue("cloud retriever must be called for a non-blacklisted domain", stub.requestedUrls.contains("https://www.allrecipes.com/recipe/123"));
        } finally {
            cascade.close();
        }
    }

    @Test
    public void perRetrieverBlacklistIsAlsoHonored() {
        RecordingCloudRetriever stub = new RecordingCloudRetriever();
        CascadingDocumentRetriever cascade = cascadeWith(stub);
        try {
            // blacklist set on the individual cloud retriever (not the cascade)
            stub.addBlacklistDomain("toyota-4runner.org");

            assertNull(cascade.getWebDocument("https://forums.toyota-4runner.org/x"));
            assertTrue("individually blacklisted retriever must be skipped", stub.requestedUrls.isEmpty());

            cascade.getWebDocument("https://www.allrecipes.com/recipe/123");
            assertTrue(stub.requestedUrls.contains("https://www.allrecipes.com/recipe/123"));
        } finally {
            cascade.close();
        }
    }
}
