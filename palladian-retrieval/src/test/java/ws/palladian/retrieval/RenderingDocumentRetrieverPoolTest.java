package ws.palladian.retrieval;

import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

            assertEquals("the reaper should have reaped the stand-in chromedriver (" + fixture.diagnosis() + ")", 1, reaped);
            assertProcessDead(fixture.handle());
        }
    }

    @Test
    public void reaperDoesNotKillFreshChildlessChromedriverChildren() throws Exception {
        try (ProcessFixture fixture = ProcessFixture.startChildlessChromedriver()) {
            assertTrue("stand-in chromedriver must be running before the reaper runs (" + fixture.diagnosis() + ")", fixture.handle().isAlive());

            int reaped = RenderingDocumentRetrieverPool.reapLeakedChildlessChromedrivers(ProcessHandle.current(), 3600);

            assertEquals(0, reaped);
            assertTrue("fresh childless chromedriver must survive the age guard (" + fixture.diagnosis() + ")", fixture.handle().isAlive());
        }
    }

    /**
     * A remote-attach driver (CloakBrowser) is childless by design; while we still own its port it must survive the
     * reaper. Without this the whole cloak tier was killed 60s after every pool build.
     */
    @Test
    public void reaperSkipsOwnedChildlessChromedriver() throws Exception {
        int port = 45871;
        try (ProcessFixture fixture = ProcessFixture.startChildlessChromedriverWithPort(port)) {
            ChromedriverProcessRegistry.registerPort(port);
            try {
                int reaped = RenderingDocumentRetrieverPool.reapLeakedChildlessChromedrivers(ProcessHandle.current(), 0);

                assertEquals("an owned chromedriver must not be reaped", 0, reaped);
                assertTrue("owned remote-attach chromedriver must survive", fixture.handle().isAlive());
            } finally {
                ChromedriverProcessRegistry.unregisterPort(port);
            }
        }
    }

    /** Once ownership is given up (service stopped) a surviving chromedriver is a real leak and must still be reaped. */
    @Test
    public void reaperStillKillsUnownedChildlessChromedriverWithPort() throws Exception {
        int port = 45872;
        try (ProcessFixture fixture = ProcessFixture.startChildlessChromedriverWithPort(port)) {
            ChromedriverProcessRegistry.unregisterPort(port); // not owned

            int reaped = RenderingDocumentRetrieverPool.reapLeakedChildlessChromedrivers(ProcessHandle.current(), 0);

            assertEquals("the reaper should have reaped the stand-in chromedriver (" + fixture.diagnosis() + ")", 1, reaped);
            assertProcessDead(fixture.handle());
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
            return startChildlessChromedriver(new String[0]);
        }

        /**
         * A childless, long-lived stand-in for a chromedriver: a {@code #!/bin/bash} script parked in the
         * {@code read} BUILTIN, so it neither forks a child - the childlessness is the whole point of these tests -
         * nor execs another program, which would replace the chromedriver-ish name in the command line.
         * <p>
         * Deliberately neither a symlink to nor a copy of {@code /bin/sleep}. Linux resolves {@code /proc/<pid>/exe}
         * through symlinks and {@link ProcessHandle.Info} then drops argv[0], so a symlinked name is invisible to
         * {@link RenderingDocumentRetrieverPool#isChromedriver}; and a copy dies on the spot wherever coreutils is a
         * multi-call binary, since it dispatches on its own argv[0] and knows no utility called "chromedriver-test".
         * A script keeps its own path in argv[1], which the JDK does report.
         */
        private static ProcessFixture startChildlessChromedriver(String... arguments) throws IOException {
            Path tempDir = Files.createTempDirectory("palladian-chromedriver-test");
            try {
                Assume.assumeTrue("/bin/bash must exist for the childless-chromedriver tests", Files.exists(Paths.get("/bin/bash")));
                Path executable = tempDir.resolve("chromedriver-test");
                Files.writeString(executable, "#!/bin/bash\nread -t 60 _\n");
                Assume.assumeTrue("script must be made executable", executable.toFile().setExecutable(true));
                String[] command = new String[arguments.length + 1];
                command[0] = executable.toString();
                System.arraycopy(arguments, 0, command, 1, arguments.length);
                Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                return new ProcessFixture(tempDir, process);
            } catch (Throwable t) {
                // An Assume abort or launch failure must not leak the temp dir since close() never runs.
                deleteRecursively(tempDir);
                throw t;
            }
        }

        /**
         * A childless "chromedriver" whose command line carries a {@code --port=}, standing in for a remote-attach
         * driver.
         */
        static ProcessFixture startChildlessChromedriverWithPort(int port) throws IOException {
            return startChildlessChromedriver("--port=" + port);
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

        /**
         * What the stand-in process is doing - and, once it has exited, why. A stand-in that dies on its own turns
         * every assertion here into a riddle, so say it out loud instead.
         */
        String diagnosis() {
            if (process.isAlive()) {
                return "pid=" + process.pid() + " alive";
            }
            String output;
            try {
                output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                output = "<output unreadable: " + e.getMessage() + ">";
            }
            return "pid=" + process.pid() + " exited with " + process.exitValue() + (output.isEmpty() ? "" : ", output: " + output);
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
