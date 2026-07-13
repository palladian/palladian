package ws.palladian.retrieval;

import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class RenderingDocumentRetrieverPoolTest {
    private static final long PROCESS_WAIT_MILLIS = 5000;

    @Test
    public void closeAndQuitStopsDriverServiceWhenQuitThrows() {
        RecordingRetriever retriever = new RecordingRetriever(new ThrowingRemoteWebDriver());

        assertFalse(retriever.closeAndQuit());

        assertTrue("driver service must be stopped even when quit throws", retriever.stopDriverServiceCalled);
        assertNull("driver reference must be cleared after close", retriever.getDriver());
    }

    @Test
    public void resolveKillRootPrefersChromedriverParentOfBrowserProcess() throws Exception {
        try (ProcessFixture fixture = ProcessFixture.startChromedriverShellWithChild()) {
            ProcessHandle browser = waitForChild(fixture.handle());

            ProcessHandle root = RenderingDocumentRetrieverPool.resolveKillRoot(browser.pid());

            assertNotNull(root);
            assertEquals("chromedriver parent must be the kill root", fixture.handle().pid(), root.pid());
        }
    }

    @Test
    public void terminateProcessTreeKillsRootAndDescendants() throws Exception {
        try (ProcessFixture fixture = ProcessFixture.startChromedriverShellWithChild()) {
            ProcessHandle root = fixture.handle();
            ProcessHandle child = waitForChild(root);

            int signaled = RenderingDocumentRetrieverPool.terminateProcessTree(root);

            assertTrue("at least one process should have been signaled", signaled > 0);
            assertProcessDead(child);
            assertProcessDead(root);
        }
    }

    @Test
    public void reaperKillsOldChildlessChromedriverChildren() throws Exception {
        try (ProcessFixture fixture = ProcessFixture.startChildlessChromedriver()) {
            assertTrue(RenderingDocumentRetrieverPool.isChromedriver(fixture.handle()));

            int reaped = RenderingDocumentRetrieverPool.reapLeakedChildlessChromedrivers(ProcessHandle.current(), 0);

            assertEquals(1, reaped);
            assertProcessDead(fixture.handle());
        }
    }

    @Test
    public void reaperDoesNotKillFreshChildlessChromedriverChildren() throws Exception {
        try (ProcessFixture fixture = ProcessFixture.startChildlessChromedriver()) {
            int reaped = RenderingDocumentRetrieverPool.reapLeakedChildlessChromedrivers(ProcessHandle.current(), 3600);

            assertEquals(0, reaped);
            assertTrue("fresh childless chromedriver must survive the age guard", fixture.handle().isAlive());
        }
    }

    private static ProcessHandle waitForChild(ProcessHandle parent) throws InterruptedException {
        long deadline = System.currentTimeMillis() + PROCESS_WAIT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            Optional<ProcessHandle> child = parent.children().findFirst();
            if (child.isPresent()) {
                return child.get();
            }
            Thread.sleep(25);
        }
        fail("Timed out waiting for child process of pid=" + parent.pid());
        return null;
    }

    private static void assertProcessDead(ProcessHandle process) throws InterruptedException {
        long deadline = System.currentTimeMillis() + PROCESS_WAIT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                return;
            }
            Thread.sleep(25);
        }
        fail("Process still alive pid=" + process.pid());
    }

    private static final class ThrowingRemoteWebDriver extends RemoteWebDriver {
        @Override
        public void close() {
            throw new WebDriverException("close failed");
        }

        @Override
        public void quit() {
            throw new WebDriverException("quit failed");
        }
    }

    private static final class RecordingRetriever extends RenderingDocumentRetriever {
        boolean stopDriverServiceCalled;

        RecordingRetriever(RemoteWebDriver driver) {
            super(driver);
        }

        @Override
        protected void stopDriverService() {
            stopDriverServiceCalled = true;
        }
    }

    private static final class ProcessFixture implements AutoCloseable {
        private final Path tempDir;
        private final Process process;

        private ProcessFixture(Path tempDir, Process process) {
            this.tempDir = tempDir;
            this.process = process;
        }

        static ProcessFixture startChromedriverShellWithChild() throws IOException {
            Path tempDir = Files.createTempDirectory("palladian-chromedriver-test");
            try {
                Path executable = createNamedScript(tempDir, "chromedriver-test", "#!/bin/sh\nsleep 60 & wait\n");
                Process process = new ProcessBuilder(executable.toString()).redirectErrorStream(true).start();
                return new ProcessFixture(tempDir, process);
            } catch (Throwable t) {
                // An Assume abort or launch failure must not leak the temp dir since close() never runs.
                deleteRecursively(tempDir);
                throw t;
            }
        }

        static ProcessFixture startChildlessChromedriver() throws IOException {
            Path tempDir = Files.createTempDirectory("palladian-chromedriver-test");
            try {
                Path executable = createNamedSymlink(tempDir, "chromedriver-test", "/bin/sleep");
                Process process = new ProcessBuilder(executable.toString(), "60").redirectErrorStream(true).start();
                return new ProcessFixture(tempDir, process);
            } catch (Throwable t) {
                // An Assume abort or launch failure must not leak the temp dir since close() never runs.
                deleteRecursively(tempDir);
                throw t;
            }
        }

        private static Path createNamedSymlink(Path tempDir, String name, String target) throws IOException {
            Path targetPath = Paths.get(target);
            Assume.assumeTrue(target + " must exist for process-tree tests", Files.exists(targetPath));
            Path executable = tempDir.resolve(name);
            try {
                Files.createSymbolicLink(executable, targetPath);
            } catch (UnsupportedOperationException | IOException | SecurityException e) {
                Assume.assumeNoException("symbolic links are required for process-tree tests", e);
            }
            return executable;
        }

        private static Path createNamedScript(Path tempDir, String name, String content) throws IOException {
            Assume.assumeTrue("/bin/sh must exist for process-tree tests", Files.exists(Paths.get("/bin/sh")));
            Path executable = tempDir.resolve(name);
            Files.writeString(executable, content);
            Assume.assumeTrue("script must be made executable", executable.toFile().setExecutable(true));
            return executable;
        }

        ProcessHandle handle() {
            return process.toHandle();
        }

        @Override
        public void close() {
            RenderingDocumentRetrieverPool.terminateProcessTree(handle());
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            deleteRecursively(tempDir);
        }

        private static void deleteRecursively(Path tempDir) {
            try {
                if (!Files.exists(tempDir)) {
                    return;
                }
                Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
        }
    }
}
