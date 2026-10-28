package ws.palladian.retrieval.cloakbrowser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WindowType;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for the CloakBrowser session's own tab — the fix for the 2026-09-24 outage where every session attached
 * via {@code debuggerAddress} took over a tab already open in the shared browser, so one wedged tab broke every
 * session built after it (fresh JVMs included) until the container was restarted.
 * <p>
 * The driver is a {@link Proxy} that records the WebDriver calls it receives; no browser or container is needed.
 */
public class CloakBrowserOwnTabTest {

    private static final String ATTACHED_TAB = "ATTACHED-TAB";
    private static final String OWN_TAB = "OWN-TAB";

    /** Records calls as "method" or "method:arg"; {@code newWindow} switches the current handle, like Selenium. */
    private static final class RecordingDriver {
        final List<String> calls = new ArrayList<>();
        String currentHandle = ATTACHED_TAB;
        RuntimeException newWindowFailure;

        WebDriver driver() {
            WebDriver.TargetLocator locator = (WebDriver.TargetLocator) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.TargetLocator.class}, (proxy, method, args) -> {
                        calls.add(method.getName() + ":" + args[0]);
                        if ("newWindow".equals(method.getName())) {
                            if (newWindowFailure != null) {
                                throw newWindowFailure;
                            }
                            currentHandle = OWN_TAB;
                        } else if ("window".equals(method.getName())) {
                            currentHandle = (String) args[0];
                        }
                        return driverProxy;
                    });
            driverProxy = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    case "toString":
                        return "RecordingDriver";
                    case "switchTo":
                        return locator;
                    case "getWindowHandle":
                        calls.add("getWindowHandle");
                        return currentHandle;
                    case "getWindowHandles":
                        return Collections.singleton(currentHandle);
                    case "close":
                        calls.add("close:" + currentHandle);
                        return null;
                    default:
                        calls.add(method.getName());
                        return null;
                }
            });
            return driverProxy;
        }

        private WebDriver driverProxy;
    }

    @Test
    public void opensAndSwitchesToItsOwnTab() {
        RecordingDriver recording = new RecordingDriver();
        String handle = CloakBrowserDocumentRetriever.openOwnTab(recording.driver());
        assertEquals(OWN_TAB, handle);
        assertEquals("newWindow:" + WindowType.TAB, recording.calls.get(0));
    }

    /** A pool build must not fail over the tab: the session keeps the tab it attached to, as before. */
    @Test
    public void fallsBackToTheAttachedTabWhenATabCannotBeOpened() {
        RecordingDriver recording = new RecordingDriver();
        recording.newWindowFailure = new WebDriverException("unknown command: new window");
        assertNull(CloakBrowserDocumentRetriever.openOwnTab(recording.driver()));
        assertEquals(ATTACHED_TAB, recording.currentHandle);
    }

    @Test
    public void closesOnlyItsOwnTab() {
        RecordingDriver recording = new RecordingDriver();
        WebDriver driver = recording.driver();
        String handle = CloakBrowserDocumentRetriever.openOwnTab(driver);
        recording.calls.clear();
        recording.currentHandle = ATTACHED_TAB; // e.g. something switched away since

        CloakBrowserDocumentRetriever.closeOwnTab(driver, handle);

        assertEquals(List.of("window:" + OWN_TAB, "close:" + OWN_TAB), recording.calls);
    }

    /** The attached tab belongs to the shared browser (maybe another JVM's session): never close it. */
    @Test
    public void closesNothingWithoutAnOwnTab() {
        RecordingDriver recording = new RecordingDriver();
        CloakBrowserDocumentRetriever.closeOwnTab(recording.driver(), null);
        assertTrue(recording.calls.isEmpty());
    }

    @Test(expected = NoSuchWindowException.class)
    public void closeFailurePropagatesToTheCallerThatLogsIt() {
        CloakBrowserDocumentRetriever.closeOwnTab(failingDriver(), OWN_TAB);
    }

    private WebDriver failingDriver() {
        return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
            throw new NoSuchWindowException("no such window: target window already closed");
        });
    }

    /** Own tab ids as Chrome's target ids look, so the registry records them. */
    private static final String OWN_TARGET = "C1C14BB2826C48B04F16A66A5D28310E";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CloakBrowserTabRegistry registry() {
        CloakBrowserTabRegistry registry = new CloakBrowserTabRegistry(folder.getRoot().toPath());
        registry.register(OWN_TARGET);
        return registry;
    }

    /** The regression: a wedged session could not close its tab through chromedriver, so it stayed open for weeks. */
    @Test
    public void closesThroughTheBrowserWithoutTouchingTheSession() {
        RecordingDriver recording = new RecordingDriver();
        CloakBrowserTabRegistry registry = registry();
        List<String> closedOverDevTools = new ArrayList<>();

        CloakBrowserDocumentRetriever.closeOwnTab(OWN_TARGET, ids -> {
            closedOverDevTools.addAll(ids);
            return Set.copyOf(ids);
        }, recording.driver(), registry);

        assertEquals(List.of(OWN_TARGET), closedOverDevTools);
        assertTrue(recording.calls.isEmpty());
        assertFalse(Files.exists(folder.getRoot().toPath().resolve(OWN_TARGET)));
    }

    @Test
    public void fallsBackToTheSessionWhenTheBrowserCannotBeReached() {
        RecordingDriver recording = new RecordingDriver();
        CloakBrowserTabRegistry registry = registry();

        CloakBrowserDocumentRetriever.closeOwnTab(OWN_TARGET, ids -> Collections.emptySet(), recording.driver(), registry);

        assertEquals(List.of("window:" + OWN_TARGET, "close:" + OWN_TARGET), recording.calls);
        assertFalse(Files.exists(folder.getRoot().toPath().resolve(OWN_TARGET)));
    }

    /** Neither way worked: the next session build closes it. */
    @Test
    public void marksTheTabAbandonedWhenNothingCanCloseIt() throws Exception {
        CloakBrowserTabRegistry registry = registry();
        try {
            CloakBrowserDocumentRetriever.closeOwnTab(OWN_TARGET, ids -> Collections.emptySet(), failingDriver(), registry);
            fail("the failure must still reach closeAndQuit");
        } catch (NoSuchWindowException expected) {
            // logged by the caller
        }
        assertEquals(CloakBrowserTabRegistry.ABANDONED, Files.readString(folder.getRoot().toPath().resolve(OWN_TARGET)));
    }

    @Test
    public void aSessionWithoutItsOwnTabClosesNothing() {
        RecordingDriver recording = new RecordingDriver();
        CloakBrowserDocumentRetriever.closeOwnTab(null, ids -> {
            throw new AssertionError("nothing to close");
        }, recording.driver(), registry());
        assertTrue(recording.calls.isEmpty());
    }
}
