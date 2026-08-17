package ws.palladian.retrieval;

import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the invalidation-cause label carried by {@link RenderingDocumentRetriever}.
 * <p>
 * Without it, every replaced driver looks identical in the pool stats — there are nine call sites that invalidate
 * a driver, so "INVALIDATED=106247" would name the symptom and none of the causes.
 */
public class RenderingDocumentRetrieverInvalidationCauseTest {

    private static RenderingDocumentRetriever retriever() {
        return new RenderingDocumentRetriever((RemoteWebDriver) null);
    }

    @Test
    public void freshRetrieverHasNoCause() {
        RenderingDocumentRetriever retriever = retriever();
        assertFalse(retriever.isInvalidatedByCallback());
        assertNull(retriever.getInvalidationCause());
    }

    @Test
    public void causeIsRecorded() {
        RenderingDocumentRetriever retriever = retriever();
        retriever.markInvalidatedByCallback("render-watchdog-timeout");
        assertTrue(retriever.isInvalidatedByCallback());
        assertEquals("render-watchdog-timeout", retriever.getInvalidationCause());
    }

    /** The first cause is the defect; anything that follows is a consequence of the driver already being broken. */
    @Test
    public void firstCauseWins() {
        RenderingDocumentRetriever retriever = retriever();
        retriever.markInvalidatedByCallback("render-watchdog-timeout");
        retriever.markInvalidatedByCallback("hard-kill");
        assertEquals("render-watchdog-timeout", retriever.getInvalidationCause());
    }

    @Test
    public void legacyNoArgCallStillInvalidatesAndIsLabelled() {
        RenderingDocumentRetriever retriever = retriever();
        retriever.markInvalidatedByCallback();
        assertTrue(retriever.isInvalidatedByCallback());
        assertEquals(RenderingDocumentRetriever.INVALIDATION_CAUSE_UNSPECIFIED, retriever.getInvalidationCause());
    }

    @Test
    public void nullCauseFallsBackToUnspecified() {
        RenderingDocumentRetriever retriever = retriever();
        retriever.markInvalidatedByCallback(null);
        assertEquals(RenderingDocumentRetriever.INVALIDATION_CAUSE_UNSPECIFIED, retriever.getInvalidationCause());
    }

    /** Pooled drivers are reused, so a stale cause must not survive into the next borrow. */
    @Test
    public void clearInvalidationClearsBothFlagAndCause() {
        RenderingDocumentRetriever retriever = retriever();
        retriever.markInvalidatedByCallback("fatal-nav");
        retriever.clearInvalidation();
        assertFalse(retriever.isInvalidatedByCallback());
        assertNull(retriever.getInvalidationCause());

        retriever.markInvalidatedByCallback("fatal-wait-elements");
        assertEquals("fatal-wait-elements", retriever.getInvalidationCause());
    }
}
