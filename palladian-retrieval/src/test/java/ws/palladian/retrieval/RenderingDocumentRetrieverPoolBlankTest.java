package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.CommandPayload;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * A browser goes back into the pool showing {@code about:blank}, so an idle one runs none of the last page's scripts:
 * a news-reader browser parked on one article held an 11.8 GB renderer at 100% CPU for 20 hours. No Chrome is launched.
 */
public class RenderingDocumentRetrieverPoolBlankTest {

    private static final class BlankingRetriever extends RenderingDocumentRetriever {
        boolean loadsBlank = true;
        int blanks;

        BlankingRetriever() {
            super((RemoteWebDriver) null);
        }

        @Override
        public boolean blank() {
            blanks++;
            return loadsBlank;
        }
    }

    private static final class BlankingPool extends RenderingDocumentRetrieverPool {
        BlankingPool(int size) {
            super(DriverManagerType.CHROME, size);
        }

        @Override
        protected RenderingDocumentRetriever createRetriever() {
            return new BlankingRetriever();
        }
    }

    private static BlankingRetriever borrow(RenderingDocumentRetrieverPool pool) {
        RenderingDocumentRetriever retriever = pool.poll(1, TimeUnit.SECONDS);
        assertNotNull("pool should hand out a retriever", retriever);
        return (BlankingRetriever) retriever;
    }

    @Test
    public void recycleBlanksTheBrowserBeforePoolingIt() {
        BlankingPool pool = new BlankingPool(1);
        BlankingRetriever retriever = borrow(pool);

        pool.recycle(retriever);

        assertEquals(1, retriever.blanks);
        assertEquals(1, pool.getRecycledDriverCount());
        assertEquals(0, pool.getReplacedDriverCount());
        assertSame(retriever, borrow(pool));
    }

    /** A browser that cannot even load about:blank is not handed out again. */
    @Test
    public void aBrowserThatCannotBlankIsReplaced() {
        BlankingPool pool = new BlankingPool(1);
        BlankingRetriever retriever = borrow(pool);
        retriever.loadsBlank = false;

        pool.recycle(retriever);

        assertEquals(0, pool.getRecycledDriverCount());
        assertEquals(1, pool.getReplaceCount(RenderingDocumentRetrieverPool.ReplaceReason.BLANK_FAILED));
        assertNotSame(retriever, borrow(pool));
    }

    @Test
    public void aRetrieverWithoutADriverHasNothingToBlank() {
        assertTrue(new RenderingDocumentRetriever((RemoteWebDriver) null).blank());
    }

    /** Records the WebDriver commands it is sent instead of talking to a browser. */
    private static final class RecordingWebDriver extends RemoteWebDriver {
        final List<String> commands = new ArrayList<>();
        boolean pageLoadTimesOut;

        @Override
        protected Response execute(CommandPayload payload) {
            Object url = payload.getParameters().get("url");
            commands.add(payload.getName() + (url == null ? "" : ":" + url));
            if (pageLoadTimesOut && DriverCommand.GET.equals(payload.getName())) {
                throw new TimeoutException("timeout: page load");
            }
            return new Response();
        }
    }

    @Test
    public void blankLoadsAboutBlank() {
        RecordingWebDriver driver = new RecordingWebDriver();
        RenderingDocumentRetriever retriever = new RenderingDocumentRetriever(driver);
        retriever.setDeleteDriverCookiesBeforeUse(false);

        assertTrue(retriever.blank());

        assertEquals(List.of(DriverCommand.GET + ":about:blank"), driver.commands);
    }

    /**
     * The next borrower deletes cookies on the page that is loaded, which is about:blank now, so a pool that deletes
     * cookies before use has them deleted here, while the last page is still loaded.
     */
    @Test
    public void blankDeletesTheLastPagesCookiesFirstWhenThePoolDeletesCookies() {
        RecordingWebDriver driver = new RecordingWebDriver();
        RenderingDocumentRetriever retriever = new RenderingDocumentRetriever(driver);
        retriever.setDeleteDriverCookiesBeforeUse(true);

        assertTrue(retriever.blank());

        assertEquals(List.of(DriverCommand.DELETE_ALL_COOKIES, DriverCommand.GET + ":about:blank"), driver.commands);
    }

    @Test
    public void aBrowserThatDoesNotLoadBlankReportsIt() {
        RecordingWebDriver driver = new RecordingWebDriver();
        driver.pageLoadTimesOut = true;
        assertFalse(new RenderingDocumentRetriever(driver).blank());
    }
}
