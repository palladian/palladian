package ws.palladian.retrieval;

import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the per-instance domain blacklist added to {@link WebDocumentRetriever}
 * ({@link WebDocumentRetriever#addBlacklistDomain(String)} / {@link WebDocumentRetriever#shouldExtract(String)}).
 * Offline: it uses a no-op concrete retriever and only exercises the (URL-only) matching logic.
 */
public class WebDocumentRetrieverBlacklistTest {

    /** Minimal concrete retriever; getWebDocument is irrelevant here, only the blacklist logic is tested. */
    private static WebDocumentRetriever newRetriever() {
        return new WebDocumentRetriever() {
            @Override
            public Document getWebDocument(String url) {
                return null;
            }
        };
    }

    @Test
    public void emptyBlacklistAllowsEverything() {
        WebDocumentRetriever retriever = newRetriever();
        assertTrue(retriever.getBlacklistedDomains().isEmpty());
        assertTrue(retriever.shouldExtract("https://www.toyota-4runner.org/thread/1"));
    }

    @Test
    public void blacklistMatchesRegisteredDomainPlusSubdomainsAndWww() {
        WebDocumentRetriever retriever = newRetriever();
        retriever.addBlacklistDomain("toyota-4runner.org");

        assertFalse(retriever.shouldExtract("https://toyota-4runner.org/"));
        assertFalse(retriever.shouldExtract("https://www.toyota-4runner.org/thread/1"));
        assertFalse(retriever.shouldExtract("https://forums.toyota-4runner.org/some/deep/path?x=1"));

        // an unrelated domain must still be fetchable
        assertTrue(retriever.shouldExtract("https://www.allrecipes.com/recipe/123"));
    }

    @Test
    public void blacklistEntryIsNormalizedFromWwwSubdomainOrFullUrl() {
        WebDocumentRetriever fromWww = newRetriever();
        fromWww.addBlacklistDomain("www.toyota-4runner.org");
        assertFalse(fromWww.shouldExtract("https://toyota-4runner.org/x"));

        WebDocumentRetriever fromUrl = newRetriever();
        fromUrl.addBlacklistDomain("https://forums.toyota-4runner.org/board/5");
        assertFalse(fromUrl.shouldExtract("https://www.toyota-4runner.org/y"));
    }

    @Test
    public void matchingIsCaseInsensitive() {
        WebDocumentRetriever retriever = newRetriever();
        retriever.addBlacklistDomain("Toyota-4Runner.ORG");
        assertFalse(retriever.shouldExtract("https://WWW.TOYOTA-4RUNNER.ORG/Thread"));
    }

    @Test
    public void multiLevelSuffixDomainMatches() {
        WebDocumentRetriever retriever = newRetriever();
        retriever.addBlacklistDomain("amazon.co.uk");
        assertFalse(retriever.shouldExtract("https://www.amazon.co.uk/dp/B000"));
        assertTrue(retriever.shouldExtract("https://www.amazon.com/dp/B000"));
    }

    @Test
    public void nullAndBlankInputsAreSafe() {
        WebDocumentRetriever retriever = newRetriever();
        retriever.addBlacklistDomain(null);
        retriever.addBlacklistDomain("   ");
        assertTrue(retriever.getBlacklistedDomains().isEmpty());

        // with a non-empty blacklist, an undeterminable URL is allowed (never block on uncertainty)
        retriever.addBlacklistDomain("toyota-4runner.org");
        assertTrue(retriever.shouldExtract(null));
        assertTrue(retriever.shouldExtract(""));
        assertTrue(retriever.shouldExtract("not a url"));
    }
}
