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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
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

    /**
     * Every retriever this pool created and has not closed yet, <em>borrowed ones included</em>. Closing only what
     * sits idle in the queue left every borrowed driver running when the JVM exited, and a chromedriver whose JVM is
     * gone is reparented to init, where no reaper can ever tell it apart from a live one again.
     */
    private final Set<RenderingDocumentRetriever> openRetrievers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final CountDownLatch closedLatch = new CountDownLatch(1);
    /** Closes that have claimed a retriever and not finished quitting it yet, on whatever thread runs them. */
    private final AtomicInteger closesInFlight = new AtomicInteger(0);

    /** Pools the JVM shutdown hook still has to close. A closed pool leaves, so the hook keeps no dead pool alive. */
    private static final Set<RenderingDocumentRetrieverPool> OPEN_POOLS = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean SHUTDOWN_HOOK_INSTALLED = new AtomicBoolean(false);

    // Quit timeouts
    private static final long QUIT_TIMEOUT_SECONDS = 10;

    /** How long {@link #closePool()} lets borrowed drivers come back on their own before it closes them anyway. */
    private static final long BORROWED_GRACE_MILLIS = 5000;
    /** Upper bound for closing a batch of drivers in parallel: one quit timeout plus the hard-kill that may follow it. */
    private static final long CLOSE_DEADLINE_SECONDS = QUIT_TIMEOUT_SECONDS + 5;
    /**
     * How long anyone waits for one closePool(): idle round, grace, forced round, plus a margin for thread start-up and
     * polling, so a waiter never gives up just before the closer finishes.
     */
    private static final long CLOSE_POOL_WAIT_MILLIS = BORROWED_GRACE_MILLIS + TimeUnit.SECONDS.toMillis(2 * CLOSE_DEADLINE_SECONDS) + 2000;

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
        try {
            initializePool();
        } catch (RuntimeException | Error e) {
            // a pool that fails half-way through its initial fill must not leave the browsers it did start behind
            closePool(0);
            throw e;
        }

        // we have to shut down the browsers or the RAM will be used up rather quickly
        OPEN_POOLS.add(this);
        installShutdownHook();

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
    }

    /**
     * One hook for the whole JVM, not one per pool: hooks run in no particular order, and the final sweep for
     * chromedrivers no pool closed must run after every pool is done, or it kills drivers another pool is still
     * quitting.
     */
    private static void installShutdownHook() {
        if (SHUTDOWN_HOOK_INSTALLED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(RenderingDocumentRetrieverPool::closeAllPoolsAtShutdown, "rendering-pool-shutdown"));
        }
    }

    /**
     * Close every open pool in parallel, then kill any chromedriver tree this JVM still has: one that was started
     * outside a pool, or one whose quit outlived the deadline. Whatever survives this is reparented to init the
     * moment the JVM exits.
     */
    static void closeAllPoolsAtShutdown() {
        closeAllPoolsThenSweep(() -> terminateChromedriverChildren(ProcessHandle.current()));
    }

    /** @param sweep runs once every pool is closed or the wait ran out; answers how many process trees it killed */
    static void closeAllPoolsThenSweep(IntSupplier sweep) {
        List<RenderingDocumentRetrieverPool> pools = new ArrayList<>(OPEN_POOLS);
        List<Thread> closers = new ArrayList<>();
        for (RenderingDocumentRetrieverPool openPool : pools) {
            Thread closer = new Thread(openPool::closePool, "rendering-pool-shutdown-" + openPool.poolId);
            closer.setDaemon(true);
            closer.start();
            closers.add(closer);
        }
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CLOSE_POOL_WAIT_MILLIS);
        for (Thread closer : closers) {
            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            if (remainingMillis <= 0) {
                break;
            }
            try {
                closer.join(remainingMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        int reaped = sweep.getAsInt();
        LOGGER.info("JVM shutdown: closed {} rendering pool(s), killed {} leftover chromedriver process tree(s)", pools.size(), reaped);
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
        RenderingDocumentRetriever retriever = createRetriever();
        if (retriever != null) {
            openRetrievers.add(retriever);
            // callers also use createObject() as a factory and quit the driver themselves (NewsFrontpageDownloader):
            // without this, every such retriever stayed tracked forever and every closePool() waited out the grace
            retriever.setOnClose(() -> openRetrievers.remove(retriever));
        }
        return retriever;
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
        if (closed.get()) {
            // a driver borrowed before closePool() comes back to a pool nobody will drain again
            closeOnce(resource);
            return;
        }
        // counted after the hand-back: a full pool throws, and that path becomes a replace, not a recycle
        super.recycle(resource);
        if (resource != null) {
            recycledDrivers.incrementAndGet();
        }
        takeBackIfClosed(resource);
    }

    /**
     * closePool() may have drained the queue between our check of {@link #closed} and the hand-back. A driver queued
     * after that drain would be closed by the forced round but stay in the queue, and a later poll() would hand out a
     * dead retriever: take it out again and close it here.
     */
    void takeBackIfClosed(RenderingDocumentRetriever resource) {
        if (resource != null && closed.get() && pool.remove(resource)) {
            closeOnce(resource);
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
        if (closed.get()) {
            // a closed pool must not start a browser that nothing would ever close; it replaces nothing, so it is not
            // counted either, which keeps created == size + replaced an identity
            closeOnce(resource);
            return;
        }
        // counted before the close: a hard-kill during it overwrites the invalidation cause with "hard-kill"
        replacedDrivers.incrementAndGet();
        countReplaceReason(reason, resource);
        try {
            closeOnce(resource);
        } catch (Exception e) {
            LOGGER.warn("Error closing resource during replace()", e);
        }
        if (closed.get()) {
            // closePool() ran while the old driver was being closed
            return;
        }

        RenderingDocumentRetriever newResource = createObject();
        if (closed.get()) {
            // closePool() ran while the new browser was starting
            closeOnce(newResource);
            return;
        }
        try {
            pool.add(newResource);
        } catch (Exception e) {
            // If we cannot add to the pool (e.g. full), we must close the new resource to avoid leak
            closeOnce(newResource);
            LOGGER.warn("Could not add new resource to pool during replace()", e);
            return;
        }
        takeBackIfClosed(newResource);
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

    /**
     * Close every driver this pool created, borrowed ones included, and stop the pool. Idle drivers are closed at
     * once; borrowed ones get {@link #BORROWED_GRACE_MILLIS} to come back (a recycle or replace into a closed pool
     * closes them), and whatever is still out after that is closed under its borrower. Bounded: it used to
     * {@code take()} one driver per slot and blocked forever on a driver that was never handed back. A second
     * caller waits for the first instead of closing anything twice.
     */
    public void closePool() {
        closePool(BORROWED_GRACE_MILLIS);
    }

    void closePool(long borrowedGraceMillis) {
        if (!closed.compareAndSet(false, true)) {
            awaitClosed();
            return;
        }
        try {
            List<RenderingDocumentRetriever> idle = new ArrayList<>();
            pool.drainTo(idle);
            closeInParallel(idle);
            // a borrower handing its driver back closes it on its own thread; wait for that close too, not just the claim
            waitUntil(() -> openRetrievers.isEmpty() && closesInFlight.get() == 0, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(borrowedGraceMillis));
            long closeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLOSE_DEADLINE_SECONDS);
            List<RenderingDocumentRetriever> stillOpen = new ArrayList<>(openRetrievers);
            if (!stillOpen.isEmpty()) {
                LOGGER.warn("Closing {} driver(s) of {} that were not handed back within {}ms", stillOpen.size(), poolId, borrowedGraceMillis);
                closeInParallel(stillOpen);
            }
            waitUntil(() -> closesInFlight.get() == 0, closeDeadline);
        } finally {
            // shutdown(), not shutdownNow(): interrupting a quit that is still running loses its chromedriver
            quitExecutor.shutdown();
            monitorExecutor.shutdownNow();
            closedLatch.countDown();
            // only now: the shutdown hook must still see a pool that another thread is closing, and wait for it
            OPEN_POOLS.remove(this);
        }
    }

    private void awaitClosed() {
        try {
            if (!closedLatch.await(CLOSE_POOL_WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Gave up waiting for {} to finish closing", poolId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void waitUntil(BooleanSupplier condition, long deadlineNanos) {
        while (!condition.getAsBoolean() && System.nanoTime() < deadlineNanos) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** Quit drivers side by side: one at a time, a pool of 23 took minutes and the JVM was killed half-way. */
    private void closeInParallel(List<RenderingDocumentRetriever> retrievers) {
        if (retrievers.isEmpty()) {
            return;
        }
        ExecutorService closer = Executors.newFixedThreadPool(retrievers.size(), runnable -> {
            Thread thread = new Thread(runnable, "rendering-pool-close");
            thread.setDaemon(true);
            return thread;
        });
        for (RenderingDocumentRetriever retriever : retrievers) {
            closer.execute(() -> closeOnce(retriever));
        }
        closer.shutdown();
        try {
            if (!closer.awaitTermination(CLOSE_DEADLINE_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.warn("Closing {} driver(s) of {} took longer than {}s", retrievers.size(), poolId, CLOSE_DEADLINE_SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Close a retriever exactly once, however many paths reach it (replace, recycle into a closed pool, closePool,
     * the shutdown hook). Removing it from {@link #openRetrievers} is the claim; a retriever this pool did not create
     * or already closed is left alone.
     */
    private void closeOnce(RenderingDocumentRetriever resource) {
        if (resource == null) {
            return;
        }
        // counted before the claim, so closePool() never sees "nothing open, nothing closing" in between
        closesInFlight.incrementAndGet();
        try {
            if (openRetrievers.remove(resource)) {
                closeWithStats(resource);
            }
        } catch (Throwable t) {
            LOGGER.warn("Error closing a driver of {}", poolId, t);
        } finally {
            closesInFlight.decrementAndGet();
        }
    }

    private void closeWithStats(RenderingDocumentRetriever resource) {
        if (resource == null) {
            return;
        }

        // Capture Chrome PID *before* attempting quit (capabilities are in-memory)
        final Long chromePid = getChromeBrowserPid(resource);

        Future<Boolean> future;
        try {
            future = quitExecutor.submit(resource::closeAndQuit);
        } catch (RejectedExecutionException e) {
            // the pool finished closing while this driver was still out
            hardKill(resource, chromePid, "pool already closed");
            return;
        }
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

    /**
     * Kill every chromedriver child of {@code parent} with its whole tree, owned or not. Only for a JVM that is
     * exiting: there, a driver that is still running is one the pools could not close in time.
     *
     * @return the number of chromedriver trees killed
     */
    static int terminateChromedriverChildren(ProcessHandle parent) {
        if (parent == null) {
            return 0;
        }
        int killed = 0;
        for (ProcessHandle child : parent.children().collect(Collectors.toList())) {
            if (child.isAlive() && isChromedriver(child)) {
                terminateProcessTree(child);
                killed++;
            }
        }
        return killed;
    }

    /**
     * SIGTERM {@code root} and all its descendants, wait briefly, then SIGKILL whatever is left. The descendants are
     * listed before anything is signaled, so a child that is reparented to init when its parent dies is still
     * reached. Public so a process that stops another JVM can clean up that JVM's browsers the same way.
     *
     * @return the number of signals that were accepted
     */
    public static int terminateProcessTree(ProcessHandle root) {
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
