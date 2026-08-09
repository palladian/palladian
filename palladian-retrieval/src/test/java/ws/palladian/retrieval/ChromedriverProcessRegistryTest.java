package ws.palladian.retrieval;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriverService;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure unit tests for {@link ChromedriverProcessRegistry} — the ownership bookkeeping that stops the pool's leak
 * reaper from killing a remote-attach (CloakBrowser) chromedriver, which is permanently "childless" by design.
 */
public class ChromedriverProcessRegistryTest {

    @After
    public void tearDown() {
        ChromedriverProcessRegistry.clear();
    }

    @Test
    public void parsesPortFromEqualsForm() {
        assertEquals(Integer.valueOf(26214), ChromedriverProcessRegistry.parsePort("/opt/cloakbrowser/chromedriver --port=26214"));
    }

    @Test
    public void parsesPortFromSpaceForm() {
        assertEquals(Integer.valueOf(9515), ChromedriverProcessRegistry.parsePort("chromedriver --port 9515 --whitelisted-ips="));
    }

    @Test
    public void returnsNullWhenNoPortPresent() {
        assertNull(ChromedriverProcessRegistry.parsePort("/bin/sleep 60"));
        assertNull(ChromedriverProcessRegistry.parsePort(null));
    }

    @Test
    public void rejectsOutOfRangePort() {
        assertNull(ChromedriverProcessRegistry.parsePort("chromedriver --port=99999"));
        assertNull(ChromedriverProcessRegistry.parsePort("chromedriver --port=0"));
        // must be rejected, not truncated to 12345 (which would mark an unrelated port as owned)
        assertNull(ChromedriverProcessRegistry.parsePort("chromedriver --port=123456"));
        assertNull(ChromedriverProcessRegistry.parsePort("chromedriver --port=99999999999999999999"));
    }

    @Test
    public void registeredPortIsOwnedUntilUnregistered() {
        assertFalse(ChromedriverProcessRegistry.isOwnedPort(31337));

        ChromedriverProcessRegistry.registerPort(31337);
        assertTrue("a live driver service must be recognised as owned", ChromedriverProcessRegistry.isOwnedPort(31337));

        // giving up ownership makes a surviving process reapable again (a real leak)
        ChromedriverProcessRegistry.unregisterPort(31337);
        assertFalse(ChromedriverProcessRegistry.isOwnedPort(31337));
    }

    @Test
    public void unrelatedPortIsNotOwned() {
        ChromedriverProcessRegistry.registerPort(4444);
        assertFalse(ChromedriverProcessRegistry.isOwnedPort(4445));
    }

    /**
     * Locks the production wiring, not just the port seams: registration reads the port from
     * {@link org.openqa.selenium.chrome.ChromeDriverService#getUrl()} on an UNSTARTED service (the URL is built in
     * DriverService's constructor), and unregistration must find the same port after the fact. If a Selenium upgrade
     * ever changes that lifecycle, register() silently no-ops and the reaper resumes killing the cloak tier — this
     * test is what fails first.
     */
    @Test
    public void registersAndUnregistersViaRealDriverService() throws Exception {
        File executable = File.createTempFile("chromedriver-registry-test", ".bin");
        try {
            Assume.assumeTrue("temp file must be executable for this test", executable.setExecutable(true) && executable.canExecute());
            ChromeDriverService service = new ChromeDriverService.Builder().usingDriverExecutable(executable).usingPort(45873).build();

            ChromedriverProcessRegistry.register(service);
            assertTrue("port of a real (unstarted) driver service must be owned", ChromedriverProcessRegistry.isOwnedPort(45873));

            ChromedriverProcessRegistry.unregister(service);
            assertFalse("port must be reapable again after unregister", ChromedriverProcessRegistry.isOwnedPort(45873));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            executable.delete();
        }
    }
}
