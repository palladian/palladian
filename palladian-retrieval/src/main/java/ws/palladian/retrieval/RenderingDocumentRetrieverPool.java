package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.ResourcePool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Pool rendering document retrievers as instantiating them is time-consuming.
 *
 * @author David Urbansky
 */
public class RenderingDocumentRetrieverPool extends ResourcePool<RenderingDocumentRetriever> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderingDocumentRetrieverPool.class);

    public static final String NO_DELETE_DRIVER_COOKIES = "__no_delete_driver_cookies";
    public static final String PAGE_LOAD_NORMAL = "__page_load_strategy_normal";
    public static final String PAGE_LOAD_EAGER = "__page_load_strategy_eager";

    protected final DriverManagerType driverManagerType;
    protected final org.openqa.selenium.Proxy proxy;
    protected final String userAgent;
    protected final String driverVersionCode;

    // we can pass the binary of the browser to use
    protected String binaryPath;

    protected Set<String> additionalOptions;

    /**
     * Why a borrowed driver had to be destroyed instead of handed back. Counted per reason because
     * "the pool replaced N drivers" on its own says nothing about which defect to fix — and
     * {@code createdDrivers} cannot answer it either: {@code createObject()} runs only from
     * {@code initializePool()} and {@link #replace}, so {@code created == size + replaced} is an
     * identity, not a measurement.
     */
    public enum ReplaceReason {
        /** The driver reference was already gone (a previous hard-kill nulled it). */
        DRIVER_NULL,
        /** The WebDriver session id was null — {@code quit()} had already run. */
        SESSION_NULL,
        /** Something called {@link RenderingDocumentRetriever#markInvalidatedByCallback(String)}. */
        INVALIDATED,
        /** Handing the driver back to the pool threw. */
        RECYCLE_FAILED,
        /** Replaced through the legacy no-reason API. */
        UNSPECIFIED
    }

    private static final AtomicInteger POOL_SEQUENCE = new AtomicInteger(0);

    /** Identifies this pool in the log: several pools, in several JVMs, share one app.log. */
    private final String poolId;

    private final AtomicInteger createdDrivers = new AtomicInteger(0);
    private final AtomicInteger replacedDrivers = new AtomicInteger(0);
    private final AtomicInteger quitFailures = new AtomicInteger(0);
    private final AtomicInteger quitTimeouts = new AtomicInteger(0);
    private final AtomicInteger hardKills = new AtomicInteger(0);

    /** Successful borrows — the denominator {@code createdDrivers} can never provide. */
    private final AtomicInteger borrowedDrivers = new AtomicInteger(0);
    private final AtomicInteger recycledDrivers = new AtomicInteger(0);
    /** Borrow attempts that found the pool empty within the caller's timeout. */
    private final AtomicInteger borrowTimeouts = new AtomicInteger(0);

    private final AtomicInteger[] replaceReasons = newReasonCounters();
    /** Bounded-cardinality histogram of {@link RenderingDocumentRetriever#getInvalidationCause()} labels. */
    private final ConcurrentHashMap<String, AtomicInteger> invalidationCauses = new ConcurrentHashMap<>();

    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService quitExecutor = Executors.newCachedThreadPool();

    // Quit timeouts
    private static final long QUIT_TIMEOUT_SECONDS = 10;

    // Hard-kill behavior
    private static final long TERM_WAIT_MILLIS = 250;
    private static final long KILL_WAIT_MILLIS = 150;
    private static final long MONITOR_INTERVAL_SECONDS = 60;
    private static final long LEAK_REAPER_MIN_AGE_SECONDS = 60;

    public RenderingDocumentRetrieverPool(DriverManagerType driverManagerType, int size) {
        this(driverManagerType, size, null, HttpRetriever.USER_AGENT, null);
    }

    public RenderingDocumentRetrieverPool(DriverManagerType driverManagerType, int size, org.openqa.selenium.Proxy proxy, String userAgent, String driverVersionCode) {
        this(driverManagerType, size, proxy, userAgent, driverVersionCode, null);
    }

    public RenderingDocumentRetrieverPool(DriverManagerType driverManagerType, int size, org.openqa.selenium.Proxy proxy, String userAgent, String driverVersionCode,
            String binaryPath) {
        this(driverManagerType, size, proxy, userAgent, driverVersionCode, binaryPath, null);
    }

    public RenderingDocumentRetrieverPool(DriverManagerType driverManagerType, int size, org.openqa.selenium.Proxy proxy, String userAgent, String driverVersionCode,
            String binaryPath, Set<String> additionalOptions) {
        super(size);
        // must be set before initializePool(): createObject() may already want to name the pool it belongs to
        this.poolId = createPoolId(getClass().getSimpleName(), size);
        this.driverManagerType = driverManagerType;
        this.proxy = proxy;
        this.userAgent = userAgent;
        this.driverVersionCode = driverVersionCode;
        this.binaryPath = binaryPath;
        this.additionalOptions = additionalOptions;
        initializePool();

        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                LOGGER.info("Pool Stats {}: borrowed={}, recycled={}, borrowTimeouts={}, createdDrivers={}, replacedDrivers={} [{}], quitFailures={}, quitTimeouts={}, "
                                + "hardKills={}, invalidationCauses={}", poolId, borrowedDrivers.get(), recycledDrivers.get(), borrowTimeouts.get(), createdDrivers.get(),
                        replacedDrivers.get(), formatReplaceReasons(), quitFailures.get(), quitTimeouts.get(), hardKills.get(), formatInvalidationCauses());
                int reaped = reapLeakedChildlessChromedrivers(ProcessHandle.current(), LEAK_REAPER_MIN_AGE_SECONDS);
                if (reaped > 0) {
                    LOGGER.warn("Reaped {} leaked childless chromedriver process(es)", reaped);
                }
            } catch (Throwable t) {
                LOGGER.warn("Error during rendering pool monitor task", t);
            }
        }, MONITOR_INTERVAL_SECONDS, MONITOR_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // we have to shut down the browsers or the RAM will be used up rather quickly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = 0; i < size; ++i) {
                try {
                    RenderingDocumentRetriever r = pool.poll(10, TimeUnit.SECONDS);
                    closeWithStats(r);
                } catch (Exception e) {
                    LOGGER.warn("Error during pool shutdown hook", e);
                }
            }
            quitExecutor.shutdownNow();
            monitorExecutor.shutdownNow();
        }));
    }

    /**
     * Final on purpose: {@code createdDrivers} is counted here so every subclass is counted too.
     * {@code CloakBrowserDocumentRetrieverPool} used to override {@code createObject()} without calling
     * {@code super}, which left its created count permanently at 0 while its replaced count grew.
     * Subclasses override {@link #createRetriever()} instead.
     */
    @Override
    public final RenderingDocumentRetriever createObject() {
        createdDrivers.incrementAndGet();
        return createRetriever();
    }

    /** Build one retriever for this pool. Override to pool a different {@link RenderingDocumentRetriever} flavour. */
    protected RenderingDocumentRetriever createRetriever() {
        RenderingDocumentRetriever renderingDocumentRetriever = new RenderingDocumentRetriever(driverManagerType, proxy, userAgent, driverVersionCode, binaryPath,
                additionalOptions);

        if (additionalOptions != null && additionalOptions.contains(NO_DELETE_DRIVER_COOKIES)) {
            renderingDocumentRetriever.setDeleteDriverCookiesBeforeUse(false);
        }

        renderingDocumentRetriever.setNoSuchSessionExceptionCallback(e -> {
            // mark as invalid so a new one will be created
            renderingDocumentRetriever.markInvalidatedByCallback("no-such-session-callback");
        });

        return renderingDocumentRetriever;
    }

    @Override
    public RenderingDocumentRetriever poll(long timeout, TimeUnit unit) {
        RenderingDocumentRetriever resource = super.poll(timeout, unit);
        if (resource == null) {
            borrowTimeouts.incrementAndGet();
        } else {
            borrowedDrivers.incrementAndGet();
        }
        return resource;
    }

    @Override
    public RenderingDocumentRetriever acquire() throws Exception {
        RenderingDocumentRetriever resource = super.acquire();
        if (resource != null) {
            borrowedDrivers.incrementAndGet();
        }
        return resource;
    }

    @Override
    public RenderingDocumentRetriever acquire(long timeout, TimeUnit unit) throws Exception {
        RenderingDocumentRetriever resource = super.acquire(timeout, unit);
        if (resource != null) {
            borrowedDrivers.incrementAndGet();
        }
        return resource;
    }

    @Override
    public void recycle(RenderingDocumentRetriever resource) {
        // counted after the hand-back: a full pool throws, and that path becomes a replace, not a recycle
        super.recycle(resource);
        if (resource != null) {
            recycledDrivers.incrementAndGet();
        }
    }

    public void replace(RenderingDocumentRetriever resource) {
        replace(resource, ReplaceReason.UNSPECIFIED);
    }

    /**
     * Destroy a borrowed driver and put a fresh one in its place.
     *
     * @param reason why the driver could not be recycled — counted so the churn can be attributed to a defect
     *               instead of re-derived from stack traces.
     */
    public void replace(RenderingDocumentRetriever resource, ReplaceReason reason) {
        replacedDrivers.incrementAndGet();
        countReplaceReason(reason, resource);
        try {
            closeWithStats(resource);
        } catch (Exception e) {
            LOGGER.warn("Error closing resource during replace()", e);
        }

        RenderingDocumentRetriever newResource = createObject();
        try {
            pool.add(newResource);
        } catch (Exception e) {
            // If we cannot add to the pool (e.g. full), we must close the new resource to avoid leak
            closeWithStats(newResource);
            LOGGER.warn("Could not add new resource to pool during replace()", e);
        }
    }

    private void countReplaceReason(ReplaceReason reason, RenderingDocumentRetriever resource) {
        ReplaceReason effective = reason == null ? ReplaceReason.UNSPECIFIED : reason;
        replaceReasons[effective.ordinal()].incrementAndGet();
        if (effective == ReplaceReason.INVALIDATED && resource != null) {
            String cause = resource.getInvalidationCause();
            invalidationCauses.computeIfAbsent(cause == null ? RenderingDocumentRetriever.INVALIDATION_CAUSE_UNSPECIFIED : cause, k -> new AtomicInteger(0))
                    .incrementAndGet();
        }
    }

    private static AtomicInteger[] newReasonCounters() {
        AtomicInteger[] counters = new AtomicInteger[ReplaceReason.values().length];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = new AtomicInteger(0);
        }
        return counters;
    }

    /**
     * A stable name for this pool instance. All JVMs on a host log into one {@code app.log} and several pools live
     * in each, so a bare stats line cannot be attributed to a pool — which is exactly what stalled the 2026-08-16
     * churn diagnosis. Includes the creating call site, so "which pool is this?" is answered by reading the line.
     */
    private static String createPoolId(String simpleClassName, int size) {
        long pid;
        try {
            pid = ProcessHandle.current().pid();
        } catch (Throwable t) {
            pid = -1;
        }
        return simpleClassName + "#" + POOL_SEQUENCE.incrementAndGet() + " created-by=" + findCreatorFrame() + " size=" + size + " pid=" + pid;
    }

    /** @return {@code SimpleClass.method:line} of the first frame outside the pool machinery, or {@code unknown}. */
    static String findCreatorFrame() {
        for (StackTraceElement frame : new Throwable().getStackTrace()) {
            String className = frame.getClassName();
            if (className.startsWith("java.") || className.startsWith("jdk.")) {
                continue;
            }
            // skip the pool constructors themselves (this class and any subclass such as the CloakBrowser pool)
            if (className.startsWith("ws.palladian.retrieval") && className.endsWith("Pool")) {
                continue;
            }
            int lastDot = className.lastIndexOf('.');
            String simpleName = lastDot < 0 ? className : className.substring(lastDot + 1);
            return simpleName + "." + frame.getMethodName() + ":" + frame.getLineNumber();
        }
        return "unknown";
    }

    /** e.g. {@code INVALIDATED=812, SESSION_NULL=3} — zero-valued reasons are omitted to keep the line readable. */
    String formatReplaceReasons() {
        StringBuilder builder = new StringBuilder();
        for (ReplaceReason reason : ReplaceReason.values()) {
            int count = replaceReasons[reason.ordinal()].get();
            if (count == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(reason).append('=').append(count);
        }
        return builder.length() == 0 ? "none" : builder.toString();
    }

    /** e.g. {@code render-watchdog-timeout=780, fatal-nav=32} — highest first, so the dominant defect leads. */
    String formatInvalidationCauses() {
        if (invalidationCauses.isEmpty()) {
            return "none";
        }
        return invalidationCauses.entrySet().stream().sorted(Comparator.comparingInt((Map.Entry<String, AtomicInteger> e) -> e.getValue().get()).reversed())
                .map(e -> e.getKey() + "=" + e.getValue().get()).collect(Collectors.joining(", "));
    }

    public String getPoolId() {
        return poolId;
    }

    public int getBorrowedDriverCount() {
        return borrowedDrivers.get();
    }

    public int getRecycledDriverCount() {
        return recycledDrivers.get();
    }

    public int getCreatedDriverCount() {
        return createdDrivers.get();
    }

    public int getReplacedDriverCount() {
        return replacedDrivers.get();
    }

    public int getReplaceCount(ReplaceReason reason) {
        return replaceReasons[reason.ordinal()].get();
    }

    public int getInvalidationCauseCount(String cause) {
        AtomicInteger counter = invalidationCauses.get(cause);
        return counter == null ? 0 : counter.get();
    }

    public void closePool() {
        for (int i = 0; i < size; i++) {
            try {
                RenderingDocumentRetriever take = pool.take();
                closeWithStats(take);
            } catch (Exception e) {
                LOGGER.warn("Error closing pool entry", e);
            }
        }
        quitExecutor.shutdownNow();
        monitorExecutor.shutdownNow();
    }

    private void closeWithStats(RenderingDocumentRetriever resource) {
        if (resource == null) {
            return;
        }

        // Capture Chrome PID *before* attempting quit (capabilities are in-memory)
        final Long chromePid = getChromeBrowserPid(resource);

        Future<Boolean> future = quitExecutor.submit(resource::closeAndQuit);
        try {
            Boolean result = future.get(QUIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(result)) {
                quitFailures.incrementAndGet();
                // quit returned "false" => attempt hard kill
                hardKill(resource, chromePid, "closeAndQuit returned false");
            }
        } catch (TimeoutException e) {
            quitTimeouts.incrementAndGet();
            future.cancel(true);
            LOGGER.error("Timeout quitting driver ({}s). Will hard-kill. chromePid={}", QUIT_TIMEOUT_SECONDS, chromePid, e);
            hardKill(resource, chromePid, "quit timeout");
        } catch (Exception e) {
            quitFailures.incrementAndGet();
            LOGGER.error("Error quitting driver. Will hard-kill. chromePid={}", chromePid, e);
            hardKill(resource, chromePid, "quit exception: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Try to read Chrome's PID from capabilities (goog:processID).
     * This should not require a remote roundtrip.
     */
    private Long getChromeBrowserPid(RenderingDocumentRetriever resource) {
        try {
            RemoteWebDriver d = resource.getDriver();
            if (d == null) {
                return null;
            }
            Object raw = d.getCapabilities().getCapability("goog:processID");
            if (raw instanceof Number) {
                return ((Number) raw).longValue();
            }
            if (raw instanceof String) {
                try {
                    return Long.parseLong((String) raw);
                } catch (NumberFormatException ignore) {
                    return null;
                }
            }
        } catch (Exception ignore) {
            // Don't spam logs here; this is best-effort.
        }
        return null;
    }

    /**
     * Hard-kill fallback. Kills the chromedriver/Chrome process tree if known and ensures this retriever cannot be reused.
     */
    private void hardKill(RenderingDocumentRetriever resource, Long chromePid, String reason) {
        hardKills.incrementAndGet();
        ProcessHandle root = resolveKillRoot(chromePid);
        int signaledProcesses = 0;

        try {
            // Ensure pool never reuses this instance
            resource.markInvalidatedByCallback("hard-kill");

            if (root != null) {
                signaledProcesses = terminateProcessTree(root);
            } else {
                LOGGER.warn("Hard-kill requested but no kill root could be resolved (reason={}, chromePid={}).", reason, chromePid);
            }

            try {
                resource.stopDriverService();
            } catch (Throwable t) {
                LOGGER.warn("Could not stop driver service during hard-kill (reason={}, chromePid={})", reason, chromePid, t);
            }
        } finally {
            // Important: if closeAndQuit hung, it may not have nulled driver. Do it here.
            try {
                resource.driver = null; // same package => allowed (driver is protected)
            } catch (Exception ignore) {
                // ignore
            }
        }

        LOGGER.warn("Hard-kill executed (reason={}, chromePid={}, rootPid={}, signaledProcesses={})", reason, chromePid, root != null ? root.pid() : null,
                signaledProcesses);
    }

    static ProcessHandle resolveKillRoot(Long browserPid) {
        if (browserPid == null) {
            return null;
        }
        ProcessHandle browser = ProcessHandle.of(browserPid).orElse(null);
        if (browser == null) {
            return null;
        }
        ProcessHandle parent = browser.parent().orElse(null);
        if (parent != null && parent.pid() > 1 && isChromedriver(parent)) {
            return parent;
        }
        return browser;
    }

    static boolean isChromedriver(ProcessHandle process) {
        if (process == null) {
            return false;
        }
        ProcessHandle.Info info = process.info();
        return containsChromedriver(info.command().orElse(null)) || containsChromedriver(info.commandLine().orElse(null));
    }

    private static boolean containsChromedriver(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("chromedriver");
    }

    static int reapLeakedChildlessChromedrivers(ProcessHandle parent, long minAgeSeconds) {
        if (parent == null) {
            return 0;
        }
        Instant cutoff = Instant.now().minusSeconds(Math.max(0, minAgeSeconds));
        int reaped = 0;
        List<ProcessHandle> children = parent.children().collect(Collectors.toList());
        for (ProcessHandle child : children) {
            if (!isChromedriver(child)) {
                continue;
            }
            // A driver we still own is never a leak — even when it looks childless. A remote-attach driver
            // (e.g. CloakBrowser via debuggerAddress) drives a Chrome in a container and never spawns a local
            // child, so without this guard it was killed 60s after every pool build.
            if (ChromedriverProcessRegistry.isOwned(child)) {
                LOGGER.debug("Not reaping chromedriver pid={} - it belongs to a live driver service", child.pid());
                continue;
            }
            if (child.children().findAny().isPresent()) {
                continue;
            }
            if (!isOlderThan(child, cutoff)) {
                continue;
            }
            LOGGER.warn("Reaping leaked childless chromedriver pid={}", child.pid());
            terminateProcessTree(child);
            reaped++;
        }
        return reaped;
    }

    private static boolean isOlderThan(ProcessHandle process, Instant cutoff) {
        return process.info().startInstant().map(start -> !start.isAfter(cutoff)).orElse(false);
    }

    static int terminateProcessTree(ProcessHandle root) {
        if (root == null) {
            return 0;
        }
        List<ProcessHandle> tree = root.descendants().collect(Collectors.toCollection(ArrayList::new));
        tree.add(root);

        int signaled = signalProcesses(tree, false);
        waitForExit(tree, TERM_WAIT_MILLIS);
        signaled += signalProcesses(tree, true);
        waitForExit(tree, KILL_WAIT_MILLIS);
        return signaled;
    }

    private static int signalProcesses(List<ProcessHandle> processes, boolean force) {
        int signaled = 0;
        for (ProcessHandle process : processes) {
            if (!process.isAlive()) {
                continue;
            }
            try {
                boolean accepted = force ? process.destroyForcibly() : process.destroy();
                if (accepted) {
                    signaled++;
                }
            } catch (SecurityException | UnsupportedOperationException e) {
                LOGGER.warn("Could not {} process pid={}", force ? "force-kill" : "terminate", process.pid(), e);
            }
        }
        return signaled;
    }

    private static void waitForExit(List<ProcessHandle> processes, long millis) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        for (ProcessHandle process : processes) {
            if (!process.isAlive()) {
                continue;
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return;
            }
            try {
                process.onExit().get(remaining, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                LOGGER.debug("Error while waiting for process exit pid={}", process.pid(), e);
            }
        }
    }

    // test drive
    public static void main(String[] args) {
        final RenderingDocumentRetrieverPool pool = new RenderingDocumentRetrieverPool(DriverManagerType.CHROME, 3);

        ExecutorService exec = Executors.newFixedThreadPool(5);
        List<Future<Document>> results = new ArrayList<>();

        List<String> urls = new ArrayList<>();
        urls.add("https://bbc.co.uk");
        urls.add("http://www.nytimes.com/");
        urls.add("http://www.washingtonpost.com/");
        urls.add("http://www.usatoday.com/");
        urls.add("http://www.chron.com/");
        urls.add("http://www.wsj.com/");
        urls.add("http://www.chicagotribune.com/");
        urls.add("http://www.latimes.com/");
        urls.add("http://nypost.com/");
        urls.add("http://www.newsday.com/");
        urls.add("http://www.seattletimes.com/");
        urls.add("http://www.bostonglobe.com/");
        urls.add("http://www.dallasnews.com/");
        urls.add("http://www.tampabay.com/");
        urls.add("https://www.amazon.com/");

        for (String url : urls) {
            Callable<Document> task = () -> {
                RenderingDocumentRetriever retriever = pool.acquire();
                try {
                    return retriever.getWebDocument(url);
                } finally {
                    pool.recycle(retriever);
                }
            };
            results.add(exec.submit(task));
        }

        exec.shutdown();
        try {
            for (Future<Document> result : results) {
                System.out.println(result.get().getDocumentURI());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
