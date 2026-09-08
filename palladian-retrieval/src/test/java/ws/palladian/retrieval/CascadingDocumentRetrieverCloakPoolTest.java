package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the hot-swappable cloak-pool accessors on {@link CascadingDocumentRetriever} and the ROLE-based tracker key
 * of the stealth stage.
 *
 * <p>The stage's key used to be {@code cloakBrowserPool.getClass().getName()}. Because the parameter is typed as the
 * <em>parent</em> {@link RenderingDocumentRetrieverPool}, a caller passing that parent type produced a key
 * byte-identical to the regular rendering stage's — silently merging both stages into one counter row and one skip
 * state, which would let a {@code pauseFailingRetriever} throttle meant for the Chrome pool also skip the stealth pool
 * and (because a skip resets the failure run without a success) blind WebKnox's dead-tier watchdog. The key is now
 * {@link CascadingDocumentRetriever#CLOAK_POOL_TRACKER_KEY}, independent of the concrete pool class.
 *
 * <p>Chrome-free: {@link DriverlessPool} overrides {@code createRetriever()} to pool a driver-less
 * {@link RenderingDocumentRetriever}, so a real (non-null) pool can be attached without launching a browser.
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

    /**
     * A real pool instance that launches no browser. Its class name differs from both
     * {@code RenderingDocumentRetrieverPool} and {@code CloakBrowserDocumentRetrieverPool}, which is exactly what makes
     * it a regression probe: under the old {@code getClass().getName()} keying the tracker would gain a
     * {@code …DriverlessPool} row instead of the role key.
     */
    private static final class DriverlessPool extends RenderingDocumentRetrieverPool {
        DriverlessPool() {
            super(DriverManagerType.CHROME, 1);
        }

        @Override
        protected RenderingDocumentRetriever createRetriever() {
            return new RenderingDocumentRetriever((RemoteWebDriver) null);
        }
    }

    /**
     * Pools created by a test, closed in {@link #closePools()}. A pool owns a monitor scheduler, a quit executor and
     * a JVM shutdown hook (all per-instance), so leaking them would leave threads logging Pool Stats for the rest of
     * the surefire fork.
     */
    private final List<RenderingDocumentRetrieverPool> pools = new ArrayList<>();

    private DriverlessPool newPool() {
        DriverlessPool pool = new DriverlessPool();
        pools.add(pool);
        return pool;
    }

    @After
    public void closePools() {
        pools.forEach(RenderingDocumentRetrieverPool::closePool);
        pools.clear();
    }

    private static CascadingDocumentRetriever cascade(RenderingDocumentRetrieverPool cloakPool) {
        // casts disambiguate the (DocumentRetriever, pool, pool, JsEnabledDocumentRetriever...) constructor
        return new CascadingDocumentRetriever((DocumentRetriever) null, (RenderingDocumentRetrieverPool) null, cloakPool, new NoopCloud());
    }

    @Test
    public void cloakPoolStartsNullAndSetterIsNullSafe() {
        CascadingDocumentRetriever cascade = cascade(null);
        try {
            assertNull("cascade built with a null cloak pool must report null", cascade.getCloakBrowserDocumentRetrieverPool());
            cascade.setCloakBrowserDocumentRetrieverPool(null); // disabling the stealth stage must not throw
            assertNull("setting null keeps the stealth stage disabled", cascade.getCloakBrowserDocumentRetrieverPool());
            assertFalse("a cascade without a cloak pool must not register a cloak row", cascade.getRequestTracker().containsKey(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY));
        } finally {
            cascade.close();
        }
    }

    /**
     * The invariant the collision fix rests on: the stealth stage's key can never equal another stage's key, because it
     * is not derived from any instance.
     */
    @Test
    public void cloakKeyCannotCollideWithAnotherStagesKey() {
        assertNotEquals("cloak key must differ from the rendering stage's key", RenderingDocumentRetrieverPool.class.getName(),
                CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY);
        assertNotEquals("cloak key must differ from the plain-HTTP stage's key", DocumentRetriever.class.getName(),
                CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY);
    }

    @Test
    public void cloakTierIsTrackedByRoleNotByPoolClass() {
        DriverlessPool pool = newPool();
        CascadingDocumentRetriever cascade = cascade(pool);
        try {
            assertSame(pool, cascade.getCloakBrowserDocumentRetrieverPool());
            assertTrue("the constructor must register the cloak stage under its role key", cascade.getRequestTracker().containsKey(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY));
            assertFalse("the tracker key must not be derived from the concrete pool class", cascade.getRequestTracker().containsKey(DriverlessPool.class.getName()));
            assertFalse("a parent-typed cloak pool must not collide with the rendering stage's key",
                    cascade.getRequestTracker().containsKey(RenderingDocumentRetrieverPool.class.getName()));
            assertEquals("exactly the plain/cloud/cloak stages are tracked (no regular rendering pool was passed)", 2, cascade.getRequestTracker().size());
            assertEquals(Integer.valueOf(0), cascade.getSuccessfulRequestCount(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY));
        } finally {
            cascade.close();
        }
    }

    /** Same guarantee for a pool attached AFTER construction (the health-daemon hot-swap path). */
    @Test
    public void hotSwappedCloakPoolIsTrackedByRole() {
        CascadingDocumentRetriever cascade = cascade(null);
        try {
            DriverlessPool pool = newPool();
            cascade.setCloakBrowserDocumentRetrieverPool(pool);
            assertSame(pool, cascade.getCloakBrowserDocumentRetrieverPool());
            assertTrue(cascade.getRequestTracker().containsKey(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY));
            assertFalse(cascade.getRequestTracker().containsKey(DriverlessPool.class.getName()));
        } finally {
            cascade.close();
        }
    }

    /**
     * A throttle on the rendering stage must not reach the stealth stage. Under the colliding key it did — and since a
     * skip resets the consecutive-failure run without a success, it also blinded the dead-tier watchdog.
     */
    @Test
    public void pausingTheRenderingStageDoesNotPauseTheCloakStage() {
        CascadingDocumentRetriever cascade = cascade(newPool());
        try {
            // throttle the Chrome pool, then push it over the threshold
            cascade.pauseFailingRetriever(RenderingDocumentRetrieverPool.class.getName(), 1, 5);
            cascade.getRequestTracker().put(RenderingDocumentRetrieverPool.class.getName(), new Integer[]{3, 0, 0});

            assertFalse("the throttled Chrome pool must be skipped", cascade.shouldMakeRequest(RenderingDocumentRetrieverPool.class.getName()));
            assertTrue("the stealth stage must be unaffected by the Chrome pool's throttle",
                    cascade.shouldMakeRequest(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY));
            assertFalse("and it must not have inherited a throttle entry either",
                    cascade.getFailingThresholdAndNumberOfRequestsToSkip().containsKey(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY));

            // the skip must also not have touched the stealth stage's failure run — a reset without a success is
            // exactly what blinds WebKnox's dead-tier watchdog
            assertEquals(Integer.valueOf(0), cascade.getConsecutiveFailedRequestCount(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY));
        } finally {
            cascade.close();
        }
    }

    /**
     * The report has to say which cascade it belongs to and which implementation is serving the stealth stage — the
     * only way to identify an unnamed cascade's report was to reverse-engineer its retriever key set.
     */
    @Test
    public void summaryIdentifiesCascadeAndCloakImplementation() {
        CascadingDocumentRetriever cascade = cascade(newPool());
        try {
            cascade.setName("unit-test");
            assertEquals("unit-test", cascade.getName());

            String summary = cascade.getUsageSummaryMessage();
            assertTrue("header must carry the cascade name: " + summary, summary.contains("CascadingDocumentRetriever tracker status [unit-test]:"));
            assertTrue("the pre-naming header literal must survive so existing log queries keep matching: " + summary,
                    summary.contains("CascadingDocumentRetriever tracker status"));
            assertTrue("cloak row must be annotated with the attached pool class: " + summary,
                    summary.contains(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY + " [DriverlessPool]"));

            // detaching keeps the counters (and therefore the row) but must say the stage is currently unserved
            cascade.setCloakBrowserDocumentRetrieverPool(null);
            String detached = cascade.getUsageSummaryMessage();
            assertTrue("detached cloak stage must be labelled as such: " + detached,
                    detached.contains(CascadingDocumentRetriever.CLOAK_POOL_TRACKER_KEY + " [detached]"));
        } finally {
            cascade.close();
        }
    }

    @Test
    public void unnamedCascadeKeepsTheBareHeader() {
        CascadingDocumentRetriever cascade = cascade(null);
        try {
            assertTrue(cascade.getUsageSummaryMessage().startsWith("CascadingDocumentRetriever tracker status:"));
        } finally {
            cascade.close();
        }
    }
}
