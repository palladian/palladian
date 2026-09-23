package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.w3c.dom.Document;
import ws.palladian.retrieval.parser.ParserFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * A Cloudflare managed challenge is auto-solving for a browser that passes it: the stealth stage
 * gets "Verification successful. Waiting for … to respond" and the page then navigates to the
 * origin. Reading the DOM right after the interstitial's own load event — which is what a render
 * does — hands the cascade the interstitial, which it rejects AND answers by skipping every local
 * rendering stage for that domain for an hour.
 * <p>
 * These tests drive the real {@code tryRenderingPool} path with a scripted retriever, so what is
 * pinned is the cascade's behaviour, not a list. Each test uses its own host because the
 * interactive-challenge skip map is static — JVM-wide, shared by every cascade.
 */
public class CascadingDocumentRetrieverAutoSolvingChallengeTest {

    /** The interstitial as a stealth browser sees it once the check has passed — text taken from a real conrad.de render. */
    private static final String CLOUDFLARE_INTERSTITIAL = "<html><head><title>Just a moment...</title></head><body>"
            + "<div>www.shop.example</div><h1>Performing security verification</h1>"
            + "<p>This website uses a security service to protect against malicious bots. This page is displayed while the "
            + "website verifies you are not a bot.</p><p>Verification successful. Waiting for www.shop.example to respond</p>"
            + "<script src=\"https://challenges.cloudflare.com/cdn-cgi/challenge-platform/h/g/orchestrate/chl_page/v1\"></script>"
            + "<noscript>Enable JavaScript and cookies to continue</noscript>"
            + "<footer>Ray ID: a3f97f75ec0d3668 Performance and Security by Cloudflare</footer></body></html>";

    /** The same interstitial reduced to its title, to pin that the title alone is recognised. */
    private static final String TITLE_ONLY_INTERSTITIAL = "<html><head><title>Just a moment...</title></head><body>"
            + "<p>Enable JavaScript and cookies to continue</p></body></html>";

    /** A real product page, well past the cascade's 500-character floor. */
    private static String productPage(String extraMarkup) {
        StringBuilder sb = new StringBuilder("<html><head><title>Werkzeugkoffer 113-teilig | Shop</title></head><body><h1>Werkzeugkoffer</h1>");
        for (int i = 0; i < 20; i++) {
            sb.append("<p>Technische Daten Zeile ").append(i).append(": VDE-geprüft, isoliert bis 1000 V, Chrom-Vanadium-Stahl.</p>");
        }
        sb.append("<div class=\"price\">249,99 €</div>").append(extraMarkup).append("</body></html>");
        return sb.toString();
    }

    private static Document parse(String html) throws Exception {
        return ParserFactory.createHtmlParser().parse(html);
    }

    /** Answers {@code getSessionId()} from a field without a browser, so the pool recycles instead of replacing. */
    private static final class OfflineDriver extends RemoteWebDriver {
        @Override
        public SessionId getSessionId() {
            return new SessionId("session-1");
        }
    }

    /**
     * Returns {@code first} from the render, then {@code afterWait} from the re-read, and reports
     * whatever {@code resolves} says from the challenge wait — recording what it was asked to wait on.
     */
    private static final class ScriptedRetriever extends RenderingDocumentRetriever {
        final Document first;
        final Document afterWait;
        final boolean resolves;
        final AtomicInteger renders = new AtomicInteger();
        final AtomicInteger waits = new AtomicInteger();
        final List<String> markersWaitedOn = new ArrayList<>();

        ScriptedRetriever(Document first, Document afterWait, boolean resolves) {
            super(new OfflineDriver());
            this.first = first;
            this.afterWait = afterWait;
            this.resolves = resolves;
        }

        @Override
        public Document getWebDocument(String url) {
            renders.incrementAndGet();
            return first;
        }

        @Override
        public boolean awaitChallengeResolution(List<String> challengeMarkers, int maxWaitSeconds) {
            waits.incrementAndGet();
            markersWaitedOn.addAll(challengeMarkers);
            return resolves;
        }

        @Override
        public Document getCurrentWebDocument() {
            return afterWait;
        }

        @Override
        public void deleteAllCookies() {
            // no browser behind the offline driver
        }
    }

    /**
     * A one-slot pool holding a {@link ScriptedRetriever}. The parent constructor fills the queue before
     * any subclass field exists, so the retriever comes in through a static — tests are not run in parallel.
     */
    private static final class ScriptedPool extends RenderingDocumentRetrieverPool {
        static Supplier<RenderingDocumentRetriever> next;

        ScriptedPool() {
            super(DriverManagerType.CHROME, 1);
        }

        @Override
        protected RenderingDocumentRetriever createRetriever() {
            return next.get();
        }
    }

    /** A cascade whose only local stage is the stealth pool, with the interstitial marked bad as a caller would. */
    private static CascadingDocumentRetriever stealthOnlyCascade(ScriptedRetriever retriever) {
        ScriptedPool.next = () -> retriever;
        CascadingDocumentRetriever cascade = new CascadingDocumentRetriever(null, null, new ScriptedPool());
        cascade.setBadDocumentIndicatorTexts(new ArrayList<>(Arrays.asList("<title>Just a moment...</title>", "Performing security verification")));
        return cascade;
    }

    @Test
    public void aCloudflareInterstitialIsWaitedOutAndTheResolvedPageIsAccepted() throws Exception {
        Document resolved = parse(productPage(""));
        ScriptedRetriever retriever = new ScriptedRetriever(parse(CLOUDFLARE_INTERSTITIAL), resolved, true);
        CascadingDocumentRetriever cascade = stealthOnlyCascade(retriever);
        try {
            Document doc = cascade.getRenderedWebDocument("https://waits-out.example/p/1");

            assertEquals("the cascade must wait on the live driver instead of rejecting the interstitial", 1, retriever.waits.get());
            assertSame("the page behind the interstitial is the result, not the interstitial", resolved, doc);
            assertEquals("counted as a stealth-stage success", Integer.valueOf(1),
                    cascade.getRequestTracker().get(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY)[2]);
        } finally {
            cascade.close();
        }
    }

    @Test
    public void aResolvedInterstitialDoesNotSkipTheDomainForTheNextFetch() throws Exception {
        ScriptedRetriever retriever = new ScriptedRetriever(parse(CLOUDFLARE_INTERSTITIAL), parse(productPage("")), true);
        CascadingDocumentRetriever cascade = stealthOnlyCascade(retriever);
        try {
            cascade.getRenderedWebDocument("https://stays-open.example/p/1");
            cascade.getRenderedWebDocument("https://stays-open.example/p/2");

            assertEquals("a challenge the browser passed is not a reason to stop rendering that shop for an hour", 2, retriever.renders.get());
        } finally {
            cascade.close();
        }
    }

    @Test
    public void anInterstitialThatNeverClearsIsStillAMissAndSkipsTheDomain() throws Exception {
        Document stillChallenged = parse(CLOUDFLARE_INTERSTITIAL);
        ScriptedRetriever retriever = new ScriptedRetriever(stillChallenged, stillChallenged, false);
        CascadingDocumentRetriever cascade = stealthOnlyCascade(retriever);
        try {
            cascade.getRenderedWebDocument("https://never-clears.example/p/1");
            cascade.getRenderedWebDocument("https://never-clears.example/p/2");

            assertEquals("waited once, then the old behaviour: the domain is skipped", 1, retriever.renders.get());
            assertEquals(Integer.valueOf(1), cascade.getRequestTracker().get(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY)[0]);
        } finally {
            cascade.close();
        }
    }

    /**
     * The wait ends when every marker it was given is gone. {@code challenges.cloudflare.com} also serves the
     * Turnstile widget in ordinary login forms, so waiting on it would run the whole window out on a real page.
     */
    @Test
    public void theWaitEndsOnInterstitialOnlyMarkersNotOnTheTurnstileHost() throws Exception {
        ScriptedRetriever retriever = new ScriptedRetriever(parse(CLOUDFLARE_INTERSTITIAL), parse(productPage("")), true);
        CascadingDocumentRetriever cascade = stealthOnlyCascade(retriever);
        try {
            cascade.getRenderedWebDocument("https://markers.example/p/1");

            assertTrue(retriever.markersWaitedOn.contains("Performing security verification"));
            assertFalse(retriever.markersWaitedOn.contains("challenges.cloudflare.com"));
        } finally {
            cascade.close();
        }
    }

    @Test
    public void theInterstitialIsRecognisedByItsTitleAlone() throws Exception {
        assertTrue(CascadingDocumentRetriever.isAutoSolvingChallenge(parse(TITLE_ONLY_INTERSTITIAL)));
    }

    @Test
    public void theInterstitialIsRecognisedByItsBodyCopy() throws Exception {
        assertTrue(CascadingDocumentRetriever.isAutoSolvingChallenge(parse(CLOUDFLARE_INTERSTITIAL)));
    }

    @Test
    public void aProductPageWithAnEmbeddedTurnstileWidgetIsNotAChallenge() throws Exception {
        Document page = parse(productPage("<form><div class=\"cf-turnstile\"></div>"
                + "<script src=\"https://challenges.cloudflare.com/turnstile/v0/api.js\"></script></form>"));
        assertNotNull(page);
        assertFalse("a newsletter form with a Turnstile widget must not trigger a ten-second wait",
                CascadingDocumentRetriever.isAutoSolvingChallenge(page));
    }
}
