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
 * The request tracker used to hold only the consecutive failure run, which every success resets, so a stage that failed on half of its pages showed
 * {@code failed=0} after its last success. Attempts that never reached the stage (no driver came free, the session was gone, the render crashed) were not
 * counted at all. The totals count all of them.
 */
public class CascadingDocumentRetrieverRequestStatsTest {

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

    private static CascadingDocumentRetriever.RequestStats stats(CascadingDocumentRetriever cascade, String key) {
        return cascade.getRequestStats().get(key);
    }

    @Test
    public void aSuccessResetsTheFailureRunButNotTheTotals() throws Exception {
        CountingCloud cloud = new CountingCloud(2, goodDocument());
        CascadingDocumentRetriever cascade = cloudOnly(cloud);
        for (int i = 0; i < 3; i++) {
            cascade.getWebDocument("https://totals.example/p/" + i);
        }

        CascadingDocumentRetriever.RequestStats stats = stats(cascade, CountingCloud.class.getName());
        assertEquals(3, stats.getRequests());
        assertEquals("the failures before the success are still counted", 2, stats.getFailed());
        assertEquals(1, stats.getSuccessful());
        assertEquals(0, stats.getConsecutiveFailed());
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

        CascadingDocumentRetriever.RequestStats stats = stats(cascade, CountingCloud.class.getName());
        assertEquals(3, cloud.requests.get());
        assertEquals(3, stats.getRequests());
        assertEquals(3, stats.getFailed());
        assertEquals(1, stats.getConsecutiveFailed());
        assertEquals("the end of the pause resets the pause count", 0, stats.getPauseSkipped());
        assertEquals("but not the total", 3, stats.getSkipped());

        // the first three fields keep the format and meaning of the old tracker row
        String summary = cascade.getUsageSummaryMessage();
        assertTrue(summary, summary.contains(CountingCloud.class.getName()
                + " -> [failed=1, skipped=0, successful=0, requests=3, totalFailed=3, totalSkipped=3, pollTimeouts=0, brokenSessions=0]"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void theDeprecatedTrackerIsASnapshotInTheOldLayout() throws Exception {
        CountingCloud cloud = new CountingCloud(1, goodDocument());
        CascadingDocumentRetriever cascade = cloudOnly(cloud);
        cascade.getWebDocument("https://snapshot.example/p/1");
        cascade.getWebDocument("https://snapshot.example/p/2");
        cascade.getWebDocument("https://snapshot.example/p/3");

        Integer[] row = cascade.getRequestTracker().get(CountingCloud.class.getName());
        assertEquals(Integer.valueOf(0), row[0]);
        assertEquals(Integer.valueOf(0), row[1]);
        assertEquals(Integer.valueOf(2), row[2]);
    }

    @Test
    public void aPollTimeoutIsAFailureThatDoesNotPauseTheStage() {
        CascadingDocumentRetriever cascade = renderingOnly(new ExhaustedPool());
        cascade.getRenderedWebDocument("https://exhausted.example/p/1");

        CascadingDocumentRetriever.RequestStats stats = stats(cascade, RENDERING_KEY);
        assertEquals(1, stats.getPollTimeouts());
        assertEquals(1, stats.getRequests());
        assertEquals(1, stats.getFailed());
        assertEquals(0, stats.getConsecutiveFailed());
    }

    @Test
    public void everyAttemptOnADeadSessionIsCounted() {
        ScriptedPool.next = DeadSessionRetriever::new;
        CascadingDocumentRetriever cascade = renderingOnly(new ScriptedPool());
        cascade.getRenderedWebDocument("https://dead-session.example/p/1");

        CascadingDocumentRetriever.RequestStats stats = stats(cascade, RENDERING_KEY);
        assertEquals("the first attempt and its retry", 2, stats.getBrokenSessions());
        assertEquals(2, stats.getRequests());
        assertEquals(2, stats.getFailed());
        assertEquals(0, stats.getConsecutiveFailed());
    }

    @Test
    public void aCrashedRenderIsCounted() {
        ScriptedPool.next = CrashingRetriever::new;
        CascadingDocumentRetriever cascade = renderingOnly(new ScriptedPool());
        cascade.getRenderedWebDocument("https://crash.example/p/1");

        CascadingDocumentRetriever.RequestStats stats = stats(cascade, RENDERING_KEY);
        assertEquals(1, stats.getRequests());
        assertEquals(1, stats.getFailed());
        assertEquals(0, stats.getConsecutiveFailed());
    }
}
