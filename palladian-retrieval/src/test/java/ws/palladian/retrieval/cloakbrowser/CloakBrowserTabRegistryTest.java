package ws.palladian.retrieval.cloakbrowser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The registry decides which tabs in the shared CloakBrowser may be closed. Closing a live session's tab breaks a
 * request in another JVM, and never closing anything is what left 206 tabs in skycraft's browser, so both directions
 * are pinned here. No browser is needed: the "closer" records what it was asked to close.
 */
public class CloakBrowserTabRegistryTest {

    private static final String TAB = "C1C14BB2826C48B04F16A66A5D28310E";
    private static final String OTHER_TAB = "EA29EA3F492D0CF0D1F149CA6C1772F6";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path directory;
    private CloakBrowserTabRegistry registry;
    private final List<String> closeRequests = new ArrayList<>();

    @Before
    public void setUp() {
        directory = folder.getRoot().toPath().resolve("127.0.0.1_9222");
        registry = new CloakBrowserTabRegistry(directory);
    }

    /** Closes everything it is asked to. */
    private Function<Collection<String>, Set<String>> closer() {
        return ids -> {
            closeRequests.addAll(ids);
            return new HashSet<>(ids);
        };
    }

    private void writeEntry(String targetId, String content) throws IOException {
        Files.createDirectories(directory);
        Files.write(directory.resolve(targetId), content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void leavesALiveSessionsTabOpen() {
        registry.register(TAB);
        assertEquals(0, registry.sweep(Set.of(TAB), closer()));
        assertTrue(closeRequests.isEmpty());
        assertTrue(Files.exists(directory.resolve(TAB)));
    }

    @Test
    public void closesAnAbandonedTabAndForgetsIt() {
        registry.abandon(TAB);
        assertEquals(1, registry.sweep(Set.of(TAB, OTHER_TAB), closer()));
        assertEquals(List.of(TAB), closeRequests);
        assertFalse(Files.exists(directory.resolve(TAB)));
    }

    /** A JVM that was killed never released its tab. */
    @Test
    public void closesTheTabOfADeadProcess() throws IOException {
        writeEntry(TAB, Long.MAX_VALUE + " 0");
        assertEquals(1, registry.sweep(Set.of(TAB), closer()));
        assertEquals(List.of(TAB), closeRequests);
    }

    @Test
    public void neverTouchesATabNobodyRegistered() {
        // the browser's startup tab, a tab from before the registry, or a sibling's tab created a moment ago
        registry.abandon(TAB);
        registry.sweep(Set.of(TAB, OTHER_TAB), closer());
        assertEquals(List.of(TAB), closeRequests);
    }

    /** Another JVM may register a tab created after this sweep listed the browser's tabs. */
    @Test
    public void keepsALiveOwnersEntryEvenIfItsTabWasNotListed() {
        registry.register(TAB);
        registry.sweep(Collections.emptySet(), closer());
        assertTrue(Files.exists(directory.resolve(TAB)));
    }

    @Test
    public void forgetsAGoneSessionsEntryWhoseTabIsAlreadyClosed() {
        registry.abandon(TAB);
        assertEquals(0, registry.sweep(Set.of(OTHER_TAB), closer()));
        assertTrue(closeRequests.isEmpty());
        assertFalse(Files.exists(directory.resolve(TAB)));
    }

    /** The next sweep tries again. */
    @Test
    public void keepsTheEntryWhenTheCloseFails() {
        registry.abandon(TAB);
        assertEquals(0, registry.sweep(Set.of(TAB), ids -> Collections.emptySet()));
        assertTrue(Files.exists(directory.resolve(TAB)));
    }

    @Test
    public void releaseForgetsTheTab() {
        registry.register(TAB);
        registry.release(TAB);
        assertFalse(Files.exists(directory.resolve(TAB)));
    }

    @Test
    public void sweepingAnEmptyHostClosesNothing() {
        assertEquals(0, registry.sweep(Set.of(TAB), closer()));
        assertTrue(closeRequests.isEmpty());
    }

    @Test
    public void onlyTargetIdsBecomeFiles() throws IOException {
        registry.register("../escape");
        registry.register(TAB);
        assertFalse(Files.exists(folder.getRoot().toPath().resolve("escape")));
        try (var entries = Files.list(directory)) {
            assertEquals(List.of(TAB), entries.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    public void thisProcessIsAnOwner() {
        assertFalse(CloakBrowserTabRegistry.isStale(CloakBrowserTabRegistry.ownerOf(ProcessHandle.current())));
    }

    @Test
    public void aReusedPidIsNotTheOwner() {
        assertTrue(CloakBrowserTabRegistry.isStale(ProcessHandle.current().pid() + " 12345"));
    }

    /** Each JVM derives start times from the boot time it read; a clock step in between must not orphan a live tab. */
    @Test
    public void aClockStepDoesNotMakeALiveOwnerLookGone() {
        long start = ProcessHandle.current().info().startInstant().orElseThrow().toEpochMilli();
        assertFalse(CloakBrowserTabRegistry.isStale(ProcessHandle.current().pid() + " " + (start - 5_000)));
        assertFalse(CloakBrowserTabRegistry.isStale(ProcessHandle.current().pid() + " " + (start + 5_000)));
    }

    @Test
    public void anEntryItCannotReadCountsAsOwned() {
        assertFalse(CloakBrowserTabRegistry.isStale(""));
        assertFalse(CloakBrowserTabRegistry.isStale("garbage"));
        assertFalse(CloakBrowserTabRegistry.isStale("12 x"));
        assertTrue(CloakBrowserTabRegistry.isStale(CloakBrowserTabRegistry.ABANDONED));
    }

    @Test
    public void entriesAreKeptPerDebuggerAddress() {
        Path tmp = Path.of(System.getProperty("java.io.tmpdir"));
        assertEquals(tmp.resolve("palladian-cloakbrowser-tabs").resolve("127.0.0.1_9222"),
                CloakBrowserTabRegistry.forAddress("127.0.0.1:9222").directory());
    }
}
