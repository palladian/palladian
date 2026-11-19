package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.w3c.dom.Document;
import ws.palladian.retrieval.parser.ParserFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The request tracker only holds the consecutive failure run, which every success resets, so a stage that failed on half of its pages showed
 * {@code failed=0} after its last success. Attempts that never reached the stage (no driver came free, the session was gone, the render crashed) were not
 * counted at all. The request totals count all of them.
 */
public class CascadingDocumentRetrieverRequestTotalsTest {

    private static final String RENDERING_KEY = RenderingDocumentRetrieverPool.class.getName();

    private static Document goodDocument() throws Exception {
        StringBuilder sb = new StringBuilder("<html><head><title>Product</title></head><body>");
        for (int i = 0; i < 20; i++) {
            sb.append("<p>Specification line ").append(i).append(": insulated up to 1000 V, chrome vanadium steel.</p>");
        }
        return ParserFactory.createHtmlParser().parse(sb.append("</body></html>").toString());
    }

    /** Fails until {@code failures} requests were made, then answers with a good document. */
    private static final class CountingCloud extends JsEnabledDocumentRetriever {
        final AtomicInteger requests = new AtomicInteger();
        final int failures;
        final Document document;

        CountingCloud(int failures, Document document) {
            this.failures = failures;
            this.document = document;
        }

        @Override
        public Document getWebDocument(String url) {
            return requests.incrementAndGet() > failures ? document : null;
        }

        @Override
        public int requestsLeft() {
            return Integer.MAX_VALUE;
        }
    }

    /** A pool without a browser, handing out whatever {@link #next} supplies. Tests are not run in parallel. */
    private static class ScriptedPool extends RenderingDocumentRetrieverPool {
        static Supplier<RenderingDocumentRetriever> next = () -> new RenderingDocumentRetriever((RemoteWebDriver) null);

        ScriptedPool() {
            super(DriverManagerType.CHROME, 1);
        }

        @Override
        protected RenderingDocumentRetriever createRetriever() {
            return next.get();
        }
    }

    /** No driver ever comes free. */
    private static final class ExhaustedPool extends ScriptedPool {
        @Override
        public RenderingDocumentRetriever poll(long timeout, TimeUnit unit) {
            return null;
        }
    }

    /** Behaves like a driver whose session the hub dropped: the no-such-session callback invalidates it and no document comes back. */
    private static final class DeadSessionRetriever extends RenderingDocumentRetriever {
        DeadSessionRetriever() {
            super((RemoteWebDriver) null);
        }

        @Override
        public Document getWebDocument(String url) {
            markInvalidatedByCallback("no-such-session-callback");
            return null;
        }

        @Override
        public void deleteAllCookies() {
        }
    }

    private static final class CrashingRetriever extends RenderingDocumentRetriever {
        CrashingRetriever() {
            super((RemoteWebDriver) null);
        }

        @Override
        public Document getWebDocument(String url) {
            throw new IllegalStateException("render crashed");
        }

        @Override
        public void deleteAllCookies() {
        }
    }

    private final List<RenderingDocumentRetrieverPool> pools = new ArrayList<>();
    private final List<CascadingDocumentRetriever> cascades = new ArrayList<>();

    @After
    public void close() {
        cascades.forEach(CascadingDocumentRetriever::close);
        pools.forEach(RenderingDocumentRetrieverPool::closePool);
        ScriptedPool.next = () -> new RenderingDocumentRetriever((RemoteWebDriver) null);
    }

    private CascadingDocumentRetriever cloudOnly(JsEnabledDocumentRetriever cloud) {
        CascadingDocumentRetriever cascade = new CascadingDocumentRetriever((DocumentRetriever) null, (RenderingDocumentRetrieverPool) null,
                (RenderingDocumentRetrieverPool) null, cloud);
        cascades.add(cascade);
        return cascade;
    }

    private CascadingDocumentRetriever renderingOnly(ScriptedPool pool) {
        pools.add(pool);
        CascadingDocumentRetriever cascade = new CascadingDocumentRetriever((DocumentRetriever) null, pool, (RenderingDocumentRetrieverPool) null);
        cascade.setPollTimeoutSeconds(0);
        cascades.add(cascade);
        return cascade;
    }

    @Test
    public void aSuccessResetsTheFailureRunButNotTheTotals() throws Exception {
        CountingCloud cloud = new CountingCloud(2, goodDocument());
        CascadingDocumentRetriever cascade = cloudOnly(cloud);
        for (int i = 0; i < 3; i++) {
            cascade.getWebDocument("https://totals.example/p/" + i);
        }

        String key = CountingCloud.class.getName();
        CascadingDocumentRetriever.RequestTotals totals = cascade.getRequestTotals().get(key);
        assertEquals(3, totals.getRequests());
        assertEquals("the failures before the success are still counted", 2, totals.getFailed());
        assertEquals(Integer.valueOf(0), cascade.getConsecutiveFailedRequestCount(key));
        assertEquals(Integer.valueOf(1), cascade.getSuccessfulRequestCount(key));
    }

    @Test
    public void skipsAreCountedAcrossPauses() {
        CountingCloud cloud = new CountingCloud(Integer.MAX_VALUE, null);
        CascadingDocumentRetriever cascade = cloudOnly(cloud);
        cascade.pauseFailingRetriever(CountingCloud.class.getName(), 2, 3);
        // 2 failures pause the stage, 3 requests are skipped, the 6th ends the pause and fails again
        for (int i = 0; i < 6; i++) {
            cascade.getWebDocument("https://pause.example/p/" + i);
        }

        CascadingDocumentRetriever.RequestTotals totals = cascade.getRequestTotals().get(CountingCloud.class.getName());
        assertEquals(3, cloud.requests.get());
        assertEquals(3, totals.getRequests());
        assertEquals(3, totals.getFailed());
        assertEquals("the end of the pause must not reset the skip total", 3, totals.getSkipped());

        // the tracker's own bracket keeps its format, the totals follow it
        String summary = cascade.getUsageSummaryMessage();
        assertTrue(summary, summary.contains(CountingCloud.class.getName()
                + " -> [failed=1, skipped=0, successful=0] totals [requests=3, failed=3, skipped=3, pollTimeouts=0, brokenSessions=0]"));
    }

    @Test
    public void aStageThatWasNeverAskedReportsZeroTotals() {
        CascadingDocumentRetriever cascade = cloudOnly(new CountingCloud(0, null));
        assertTrue(cascade.getRequestTotals().isEmpty());
        String summary = cascade.getUsageSummaryMessage();
        assertTrue(summary, summary.contains("-> [failed=0, skipped=0, successful=0] totals [requests=0, failed=0, skipped=0, pollTimeouts=0, brokenSessions=0]"));
    }

    @Test
    public void aPollTimeoutIsAFailureThatDoesNotPauseTheStage() {
        CascadingDocumentRetriever cascade = renderingOnly(new ExhaustedPool());
        cascade.getRenderedWebDocument("https://exhausted.example/p/1");

        CascadingDocumentRetriever.RequestTotals totals = cascade.getRequestTotals().get(RENDERING_KEY);
        assertEquals(1, totals.getPollTimeouts());
        assertEquals(1, totals.getRequests());
        assertEquals(1, totals.getFailed());
        assertEquals(Integer.valueOf(0), cascade.getConsecutiveFailedRequestCount(RENDERING_KEY));
    }

    @Test
    public void everyAttemptOnADeadSessionIsCounted() {
        ScriptedPool.next = DeadSessionRetriever::new;
        CascadingDocumentRetriever cascade = renderingOnly(new ScriptedPool());
        cascade.getRenderedWebDocument("https://dead-session.example/p/1");

        CascadingDocumentRetriever.RequestTotals totals = cascade.getRequestTotals().get(RENDERING_KEY);
        assertEquals("the first attempt and its retry", 2, totals.getBrokenSessions());
        assertEquals(2, totals.getRequests());
        assertEquals(2, totals.getFailed());
        assertEquals(Integer.valueOf(0), cascade.getConsecutiveFailedRequestCount(RENDERING_KEY));
    }

    @Test
    public void aCrashedRenderIsCounted() {
        ScriptedPool.next = CrashingRetriever::new;
        CascadingDocumentRetriever cascade = renderingOnly(new ScriptedPool());
        cascade.getRenderedWebDocument("https://crash.example/p/1");

        CascadingDocumentRetriever.RequestTotals totals = cascade.getRequestTotals().get(RENDERING_KEY);
        assertEquals(1, totals.getRequests());
        assertEquals(1, totals.getFailed());
        assertEquals(Integer.valueOf(0), cascade.getConsecutiveFailedRequestCount(RENDERING_KEY));
    }
}
