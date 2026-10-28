package ws.palladian.retrieval.cloakbrowser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Who owns which tab in a shared CloakBrowser, so the tabs of sessions that are gone can be closed.
 * <p>
 * The container's browser lives for weeks and is shared by every JVM on its host, and Chrome cannot tell an abandoned
 * tab from a live one (every page reports {@code attached}, chromedriver attaches to all of them). So each session
 * records its own tab here: one file per tab, named by its target id, holding the owner JVM's pid and start time, or
 * {@link #ABANDONED} once both ways of closing it failed. A sweep closes the tabs whose owner is dead or that were
 * abandoned. A tab nobody registered (the browser's startup tab, anything opened before this registry) is never
 * touched, which keeps the browser's last tab, and a tab another JVM is registering this very moment, safe.
 * <p>
 * Every client of a container runs on its host (the port binds 127.0.0.1), so the files sit in
 * {@code java.io.tmpdir}, one directory per debugger address. Registry I/O never fails a session: errors are logged.
 */
final class CloakBrowserTabRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloakBrowserTabRegistry.class);

    /** Entry of a tab whose session ended without closing it. */
    static final String ABANDONED = "abandoned";

    /**
     * A start time further off than this is another process that got the same pid. Generous on purpose: each JVM
     * derives start times from the boot time it read once, which moves with every clock step, and mistaking a live
     * owner for a gone one closes its tab on every sweep, while missing a reused pid leaks one tab.
     */
    private static final long START_TIME_TOLERANCE_MILLIS = TimeUnit.MINUTES.toMillis(10);

    /** Chrome's target ids are hex; anything else never becomes a file name. */
    private static final Pattern TARGET_ID = Pattern.compile("[A-Za-z0-9-]{1,128}");

    private final Path directory;

    CloakBrowserTabRegistry(Path directory) {
        this.directory = directory;
    }

    static CloakBrowserTabRegistry forAddress(String debuggerAddress) {
        return new CloakBrowserTabRegistry(Paths.get(System.getProperty("java.io.tmpdir"), "palladian-cloakbrowser-tabs",
                debuggerAddress.replaceAll("[^A-Za-z0-9._-]", "_")));
    }

    Path directory() {
        return directory;
    }

    void register(String targetId) {
        write(targetId, ownerOf(ProcessHandle.current()));
    }

    void abandon(String targetId) {
        write(targetId, ABANDONED);
    }

    void release(String targetId) {
        if (isValid(targetId)) {
            try {
                Files.deleteIfExists(directory.resolve(targetId));
            } catch (IOException e) {
                LOGGER.warn("CloakBrowser tab registry: could not release {}: {}", targetId, e.toString());
            }
        }
    }

    /**
     * Close the open tabs whose owner is gone and forget them; forget gone owners' tabs that are no longer open.
     * Entries of live owners are left alone, open or not: their owner releases them.
     *
     * @param openPageIds the browser's open tabs now.
     * @param closer      closes tabs by id and answers which are closed now.
     * @return how many tabs it closed.
     */
    int sweep(Set<String> openPageIds, Function<Collection<String>, Set<String>> closer) {
        List<String> toClose = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                String targetId = entry.getFileName().toString();
                if (!isValid(targetId) || !isStale(new String(Files.readAllBytes(entry), StandardCharsets.UTF_8))) {
                    continue;
                }
                if (openPageIds.contains(targetId)) {
                    toClose.add(targetId);
                } else {
                    release(targetId);
                }
            }
        } catch (IOException e) {
            // no directory yet: nobody registered a tab on this host
            if (Files.isDirectory(directory)) {
                LOGGER.warn("CloakBrowser tab registry: could not read {}: {}", directory, e.toString());
            }
            return 0;
        }
        if (toClose.isEmpty()) {
            return 0;
        }
        Set<String> closed = closer.apply(toClose);
        for (String targetId : closed) {
            release(targetId);
        }
        return closed.size();
    }

    /**
     * Whether an entry's session is gone: marked abandoned, or its owner process is dead. An entry it cannot read (a
     * write in progress, a foreign file) counts as owned, since closing a live session's tab is worse than a leak.
     * Package-private + static so it is unit-testable.
     */
    static boolean isStale(String entry) {
        String content = entry.trim();
        if (ABANDONED.equals(content)) {
            return true;
        }
        String[] parts = content.split(" ");
        if (parts.length != 2) {
            return false;
        }
        long pid;
        long startMillis;
        try {
            pid = Long.parseLong(parts[0]);
            startMillis = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        Optional<ProcessHandle> owner = ProcessHandle.of(pid);
        if (owner.isEmpty() || !owner.get().isAlive()) {
            return true;
        }
        if (startMillis <= 0) {
            return false;
        }
        // the same pid, but a process that started at another time: the owner died and its pid was reused
        return owner.get().info().startInstant().map(start -> Math.abs(start.toEpochMilli() - startMillis) > START_TIME_TOLERANCE_MILLIS)
                .orElse(false);
    }

    /** Package-private + static so it is unit-testable. */
    static String ownerOf(ProcessHandle process) {
        return process.pid() + " " + process.info().startInstant().map(Instant::toEpochMilli).orElse(0L);
    }

    private void write(String targetId, String content) {
        if (!isValid(targetId)) {
            LOGGER.warn("CloakBrowser tab registry: not a target id, not recorded: {}", targetId);
            return;
        }
        try {
            Files.createDirectories(directory);
            // written aside and moved in, so a sweep in another JVM never reads half an entry
            Path temp = Files.createTempFile(directory, ".", ".tmp");
            Files.write(temp, content.getBytes(StandardCharsets.UTF_8));
            Files.move(temp, directory.resolve(targetId), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.warn("CloakBrowser tab registry: could not record {} as {}: {}", targetId, content, e.toString());
        }
    }

    private static boolean isValid(String targetId) {
        return targetId != null && TARGET_ID.matcher(targetId).matches();
    }
}
