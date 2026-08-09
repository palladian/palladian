package ws.palladian.retrieval;

import org.junit.Test;
import org.openqa.selenium.WebDriverException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RenderingDocumentRetriever#isFatalWebDriverError(Throwable)}. A lost browser must be
 * classified as fatal so the cascade calls {@code pool.replace()} instead of {@code pool.recycle()} — otherwise a
 * dead retriever is handed out forever (the CloakBrowser tier served 0 good documents across thousands of requests
 * because {@code UnreachableBrowserException} was not recognised here).
 */
public class RenderingDocumentRetrieverFatalErrorTest {

    /** Same simple name as Selenium's exception, so the name-based branch is exercised without depending on it. */
    private static final class UnreachableBrowserException extends WebDriverException {
        UnreachableBrowserException(String message) {
            super(message);
        }
    }

    @Test
    public void unreachableBrowserMessageIsFatal() {
        WebDriverException e = new WebDriverException("Error communicating with the remote browser. It may have died.");
        assertTrue(RenderingDocumentRetriever.isFatalWebDriverError(e));
    }

    @Test
    public void unreachableBrowserExceptionTypeIsFatalEvenWithoutMessage() {
        assertTrue(RenderingDocumentRetriever.isFatalWebDriverError(new UnreachableBrowserException(null)));
    }

    @Test
    public void previouslyKnownFatalErrorsStillFatal() {
        assertTrue(RenderingDocumentRetriever.isFatalWebDriverError(new WebDriverException("chrome not reachable")));
        assertTrue(RenderingDocumentRetriever.isFatalWebDriverError(new WebDriverException("invalid session id")));
        assertTrue(RenderingDocumentRetriever.isFatalWebDriverError(new WebDriverException("tab crashed")));
    }

    @Test
    public void benignWebDriverErrorIsNotFatal() {
        assertFalse(RenderingDocumentRetriever.isFatalWebDriverError(new WebDriverException("no such element: nope")));
    }

    @Test
    public void nonWebDriverExceptionIsNotFatal() {
        assertFalse(RenderingDocumentRetriever.isFatalWebDriverError(new RuntimeException("Error communicating with the remote browser. It may have died.")));
        assertFalse(RenderingDocumentRetriever.isFatalWebDriverError(null));
    }
}
