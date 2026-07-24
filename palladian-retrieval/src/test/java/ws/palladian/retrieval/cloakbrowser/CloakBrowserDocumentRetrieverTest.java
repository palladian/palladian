package ws.palladian.retrieval.cloakbrowser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure unit tests for {@link CloakBrowserDocumentRetriever}'s chromedriver-selection helpers — the fix for the
 * 2026-07 outage where {@code WebDriverManager} silently used a driver older than CloakBrowser's Chrome
 * (major 146), breaking every CDP session. No container / Selenium needed; only the (file-system) selection
 * and version-compare logic is exercised.
 */
public class CloakBrowserDocumentRetrieverTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File makeChromium(String version) throws IOException {
        File dir = folder.newFolder("chromium-" + version);
        File driver = new File(dir, "chromedriver");
        assertTrue(driver.createNewFile());
        return driver;
    }

    @Test
    public void selectsMatchingMajorOverHigherNonMatching() throws IOException {
        makeChromium("142.0.7444.61");           // wrong major
        File match = makeChromium("146.0.7680.177.5");
        makeChromium("150.0.1000.1");            // higher, but wrong major
        assertEquals(match.getAbsolutePath(), CloakBrowserDocumentRetriever.selectChromedriver(folder.getRoot(), "146"));
    }

    @Test
    public void picksHighestFullVersionWithinMatchingMajor() throws IOException {
        makeChromium("146.0.7680.177.3");
        File newer = makeChromium("146.0.7680.177.5");
        assertEquals(newer.getAbsolutePath(), CloakBrowserDocumentRetriever.selectChromedriver(folder.getRoot(), "146"));
    }

    @Test
    public void fallsBackToHighestWhenNoMajorMatch() throws IOException {
        makeChromium("142.0.7444.61");
        File highest = makeChromium("144.0.1.1");
        assertEquals(highest.getAbsolutePath(), CloakBrowserDocumentRetriever.selectChromedriver(folder.getRoot(), "146"));
    }

    @Test
    public void ignoresDirsWithoutChromedriver() throws IOException {
        folder.newFolder("chromium-146.0.0.0"); // dir present but no chromedriver binary
        File real = makeChromium("142.0.7444.61");
        assertEquals(real.getAbsolutePath(), CloakBrowserDocumentRetriever.selectChromedriver(folder.getRoot(), "146"));
    }

    @Test
    public void nullWhenNoChromiumDirs() throws IOException {
        folder.newFolder("unrelated");
        assertNull(CloakBrowserDocumentRetriever.selectChromedriver(folder.getRoot(), "146"));
    }

    @Test
    public void nullWhenDirMissing() {
        assertNull(CloakBrowserDocumentRetriever.selectChromedriver(new File(folder.getRoot(), "does-not-exist"), "146"));
    }

    @Test
    public void explicitPathWinsOverDir() {
        assertEquals("/opt/cd/chromedriver",
                CloakBrowserDocumentRetriever.resolveDriverPath("/opt/cd/chromedriver", "/opt/cloakbrowser", "146"));
    }

    @Test
    public void resolveReturnsNullWhenNothingConfigured() {
        assertNull(CloakBrowserDocumentRetriever.resolveDriverPath(null, null, "146"));
        assertNull(CloakBrowserDocumentRetriever.resolveDriverPath("  ", "", "146"));
    }

    @Test
    public void resolveUsesDirWhenNoExplicitPath() throws IOException {
        File match = makeChromium("146.0.7680.177.5");
        assertEquals(match.getAbsolutePath(),
                CloakBrowserDocumentRetriever.resolveDriverPath(null, folder.getRoot().getAbsolutePath(), "146"));
    }

    @Test
    public void versionCompare() {
        assertTrue(CloakBrowserDocumentRetriever.compareVersions("146.0.7680.177.5", "146.0.7680.177.3") > 0);
        assertEquals(0, CloakBrowserDocumentRetriever.compareVersions("146.0.1", "146.0.1.0"));
        assertTrue(CloakBrowserDocumentRetriever.compareVersions("142.0", "146.0") < 0);
    }
}
