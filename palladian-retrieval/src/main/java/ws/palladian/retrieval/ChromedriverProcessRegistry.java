package ws.palladian.retrieval;

import org.openqa.selenium.chrome.ChromeDriverService;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the local ports of the chromedriver processes Palladian currently <em>owns</em>, so
 * {@link RenderingDocumentRetrieverPool#reapLeakedChildlessChromedrivers(ProcessHandle, long)} can tell a genuinely
 * leaked chromedriver from a live one it must not touch.
 * <p>
 * <b>Why this exists.</b> The leak reaper kills every chromedriver child of this JVM that has no child process of its
 * own and is older than a minute — the assumption being "a chromedriver without a Chrome underneath it is a leak".
 * That assumption breaks for a <em>remote-attach</em> driver: {@link ws.palladian.retrieval.cloakbrowser.CloakBrowserDocumentRetriever}
 * attaches to a Chrome running in a Docker container via {@code debuggerAddress}, so it never spawns a local Chrome
 * child and is therefore permanently "childless". Before this registry, such a driver was killed exactly 60s after
 * every pool build, which silently disabled the whole CloakBrowser tier (0 successes over thousands of requests).
 * <p>
 * A port is registered when the {@link ChromeDriverService} is created and unregistered when it is stopped, so a
 * driver that really did leak (service stopped, process survived) is still reaped as before.
 *
 * @author David Urbansky
 */
public final class ChromedriverProcessRegistry {
    /**
     * Matches {@code --port=1234} and {@code --port 1234} in a process command line. The digit run is captured in
     * full (not capped at 5) so an over-long value like {@code --port=123456} is REJECTED by the range check below
     * instead of being silently truncated to a different, wrong port.
     */
    private static final Pattern PORT_PATTERN = Pattern.compile("--port[=\\s]+(\\d+)");

    private static final Set<Integer> OWNED_PORTS = ConcurrentHashMap.newKeySet();

    private ChromedriverProcessRegistry() {
        // static only
    }

    /** Remember the port of a driver service we own. No-op when the port cannot be determined. */
    public static void register(ChromeDriverService service) {
        Integer port = portOf(service);
        if (port != null) {
            OWNED_PORTS.add(port);
        }
    }

    /** Forget a driver service we no longer own; its chromedriver becomes reapable again. */
    public static void unregister(ChromeDriverService service) {
        Integer port = portOf(service);
        if (port != null) {
            OWNED_PORTS.remove(port);
        }
    }

    public static boolean isOwnedPort(int port) {
        return OWNED_PORTS.contains(port);
    }

    /**
     * @return {@code true} if this process is a chromedriver listening on a port we own. When the command line is not
     * readable the port is unknown and this returns {@code false} — i.e. the reaper falls back to its previous
     * behaviour rather than letting a real leak survive.
     */
    static boolean isOwned(ProcessHandle process) {
        if (process == null) {
            return false;
        }
        Integer port = parsePort(process.info().commandLine().orElse(null));
        return port != null && isOwnedPort(port);
    }

    /** Extract the {@code --port} value from a command line. Package-private + pure for testing. */
    static Integer parsePort(String commandLine) {
        if (commandLine == null) {
            return null;
        }
        Matcher matcher = PORT_PATTERN.matcher(commandLine);
        if (!matcher.find()) {
            return null;
        }
        try {
            int port = Integer.parseInt(matcher.group(1));
            return port > 0 && port <= 65535 ? port : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer portOf(ChromeDriverService service) {
        if (service == null) {
            return null;
        }
        try {
            URL url = service.getUrl();
            int port = url == null ? -1 : url.getPort();
            return port > 0 ? port : null;
        } catch (Exception e) {
            return null; // best effort — never let bookkeeping break driver setup/teardown
        }
    }

    // -- test support ------------------------------------------------------------------------------------------

    static void registerPort(int port) {
        OWNED_PORTS.add(port);
    }

    static void unregisterPort(int port) {
        OWNED_PORTS.remove(port);
    }

    static void clear() {
        OWNED_PORTS.clear();
    }
}
