package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the pool's churn instrumentation. No Chrome is launched: the test pool overrides
 * {@link RenderingDocumentRetrieverPool#createRetriever()} to hand out driver-less retrievers, which is only
 * possible because {@code createObject()} (where the created-driver counter lives) is final.
 * <p>
 * What these lock down: {@code createdDrivers == size + replacedDrivers} is an <em>identity</em>, so it can never
 * show how often a borrowed driver is destroyed. Borrows, recycles and per-reason replacements can.
 */
public class RenderingDocumentRetrieverPoolStatsTest {

    /** A pool whose "drivers" are inert — no WebDriverManager, no chromedriver, no Chrome. */
    private static final class DriverlessPool extends RenderingDocumentRetrieverPool {
        DriverlessPool(int size) {
            super(DriverManagerType.CHROME, size);
        }

        @Override
        protected RenderingDocumentRetriever createRetriever() {
            return new RenderingDocumentRetriever((RemoteWebDriver) null);
        }
    }

    private static RenderingDocumentRetriever borrow(RenderingDocumentRetrieverPool pool) {
        RenderingDocumentRetriever retriever = pool.poll(1, TimeUnit.SECONDS);
        assertNotNull("pool should hand out a retriever", retriever);
        return retriever;
    }

    /**
     * The regression this guards: {@code CloakBrowserDocumentRetrieverPool} overrode {@code createObject()} without
     * calling {@code super}, so its created count sat at 0 forever while it replaced drivers.
     */
    @Test
    public void subclassCreationIsCounted() {
        DriverlessPool pool = new DriverlessPool(3);
        assertEquals(3, pool.getCreatedDriverCount());
    }

    @Test
    public void borrowsAndRecyclesAreCounted() {
        DriverlessPool pool = new DriverlessPool(2);
        assertEquals(0, pool.getBorrowedDriverCount());

        pool.recycle(borrow(pool));
        pool.recycle(borrow(pool));

        assertEquals(2, pool.getBorrowedDriverCount());
        assertEquals(2, pool.getRecycledDriverCount());
        assertEquals(0, pool.getReplacedDriverCount());
        // the whole point: created stayed at the initial fill, so the replace RATE is 0/2 and now visible
        assertEquals(2, pool.getCreatedDriverCount());
    }

    @Test
    public void emptyPoolBorrowIsCountedAsTimeoutNotAsBorrow() {
        DriverlessPool pool = new DriverlessPool(1);
        RenderingDocumentRetriever held = borrow(pool);

        assertNull(pool.poll(1, TimeUnit.MILLISECONDS));
        assertEquals(1, pool.getBorrowedDriverCount());

        pool.recycle(held);
    }

    @Test
    public void replaceCountsItsReason() {
        DriverlessPool pool = new DriverlessPool(2);

        pool.replace(borrow(pool), RenderingDocumentRetrieverPool.ReplaceReason.DRIVER_NULL);
        pool.replace(borrow(pool), RenderingDocumentRetrieverPool.ReplaceReason.DRIVER_NULL);

        assertEquals(2, pool.getReplacedDriverCount());
        assertEquals(2, pool.getReplaceCount(RenderingDocumentRetrieverPool.ReplaceReason.DRIVER_NULL));
        assertEquals(0, pool.getReplaceCount(RenderingDocumentRetrieverPool.ReplaceReason.INVALIDATED));
        // the identity that made the old stats line uninformative still holds — that is why it proves nothing
        assertEquals(2 + pool.getReplacedDriverCount(), pool.getCreatedDriverCount());
    }

    @Test
    public void legacyReplaceWithoutReasonIsCountedAsUnspecified() {
        DriverlessPool pool = new DriverlessPool(1);
        pool.replace(borrow(pool));
        assertEquals(1, pool.getReplaceCount(RenderingDocumentRetrieverPool.ReplaceReason.UNSPECIFIED));
    }

    @Test
    public void invalidationCauseIsAggregatedPerLabel() {
        DriverlessPool pool = new DriverlessPool(3);

        for (int i = 0; i < 3; i++) {
            RenderingDocumentRetriever retriever = borrow(pool);
            retriever.markInvalidatedByCallback(i == 2 ? "fatal-nav" : "render-watchdog-timeout");
            pool.replace(retriever, RenderingDocumentRetrieverPool.ReplaceReason.INVALIDATED);
        }

        assertEquals(2, pool.getInvalidationCauseCount("render-watchdog-timeout"));
        assertEquals(1, pool.getInvalidationCauseCount("fatal-nav"));
        // highest first, so the dominant defect leads the log line
        assertEquals("render-watchdog-timeout=2, fatal-nav=1", pool.formatInvalidationCauses());
    }

    /** A non-INVALIDATED replacement has no cause to attribute — it must not pollute the histogram. */
    @Test
    public void nonInvalidatedReplaceDoesNotRecordACause() {
        DriverlessPool pool = new DriverlessPool(1);
        pool.replace(borrow(pool), RenderingDocumentRetrieverPool.ReplaceReason.SESSION_NULL);
        assertEquals("none", pool.formatInvalidationCauses());
    }

    @Test
    public void formattersAreReadableWhenNothingHappened() {
        DriverlessPool pool = new DriverlessPool(1);
        assertEquals("none", pool.formatReplaceReasons());
        assertEquals("none", pool.formatInvalidationCauses());
    }

    @Test
    public void zeroValuedReasonsAreOmittedFromTheLogLine() {
        DriverlessPool pool = new DriverlessPool(1);
        pool.replace(borrow(pool), RenderingDocumentRetrieverPool.ReplaceReason.RECYCLE_FAILED);
        assertEquals("RECYCLE_FAILED=1", pool.formatReplaceReasons());
    }

    /**
     * Four JVMs on skycraft write into one {@code app.log} and each holds several pools, so a stats line without an
     * identity cannot be attributed — which is precisely what stalled the 2026-08-16 churn diagnosis.
     */
    @Test
    public void poolIdCarriesEnoughToAttributeALogLine() {
        DriverlessPool pool = new DriverlessPool(4);
        String poolId = pool.getPoolId();

        assertTrue(poolId, poolId.startsWith("DriverlessPool#"));
        assertTrue(poolId, poolId.contains("size=4"));
        assertTrue(poolId, poolId.contains("pid=" + ProcessHandle.current().pid()));
        assertTrue(poolId, poolId.contains("created-by=RenderingDocumentRetrieverPoolStatsTest."));
    }

    @Test
    public void poolIdsAreUniquePerInstance() {
        assertTrue(!new DriverlessPool(1).getPoolId().equals(new DriverlessPool(1).getPoolId()));
    }

    /** The creator walk must skip the pool machinery itself, or every pool would report the same frame. */
    @Test
    public void creatorFrameSkipsPoolInternals() {
        String frame = RenderingDocumentRetrieverPool.findCreatorFrame();
        assertTrue(frame, frame.startsWith("RenderingDocumentRetrieverPoolStatsTest.creatorFrameSkipsPoolInternals:"));
    }
}
