package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.ResourcePool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final AtomicInteger createdDrivers = new AtomicInteger(0);
    private final AtomicInteger replacedDrivers = new AtomicInteger(0);
    private final AtomicInteger quitFailures = new AtomicInteger(0);
    private final AtomicInteger quitTimeouts = new AtomicInteger(0);
    private final AtomicInteger hardKills = new AtomicInteger(0);

    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService quitExecutor = Executors.newCachedThreadPool();

    // Quit timeouts
    private static final long QUIT_TIMEOUT_SECONDS = 10;

    // Hard-kill behavior
    private static final long TERM_WAIT_MILLIS = 250;
    private static final long KILL_WAIT_MILLIS = 150;

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
        this.driverManagerType = driverManagerType;
        this.proxy = proxy;
        this.userAgent = userAgent;
        this.driverVersionCode = driverVersionCode;
        this.binaryPath = binaryPath;
        this.additionalOptions = additionalOptions;
        initializePool();

        monitorExecutor.scheduleAtFixedRate(() -> {
            LOGGER.info("Pool Stats: createdDrivers={}, replacedDrivers={}, quitFailures={}, quitTimeouts={}, hardKills={}", createdDrivers.get(), replacedDrivers.get(),
                    quitFailures.get(), quitTimeouts.get(), hardKills.get());
        }, 60, 60, TimeUnit.SECONDS);

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

    @Override
    public RenderingDocumentRetriever createObject() {
        createdDrivers.incrementAndGet();
        RenderingDocumentRetriever renderingDocumentRetriever = new RenderingDocumentRetriever(driverManagerType, proxy, userAgent, driverVersionCode, binaryPath,
                additionalOptions);

        if (additionalOptions != null && additionalOptions.contains(NO_DELETE_DRIVER_COOKIES)) {
            renderingDocumentRetriever.setDeleteDriverCookiesBeforeUse(false);
        }

        renderingDocumentRetriever.setNoSuchSessionExceptionCallback(e -> {
            // mark as invalid so a new one will be created
            renderingDocumentRetriever.markInvalidatedByCallback();
        });

        return renderingDocumentRetriever;
    }

    public void replace(RenderingDocumentRetriever resource) {
        replacedDrivers.incrementAndGet();
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
     * Hard-kill fallback. Kills Chrome PID if known and ensures this retriever cannot be reused.
     */
    private void hardKill(RenderingDocumentRetriever resource, Long chromePid, String reason) {
        hardKills.incrementAndGet();

        try {
            // Ensure pool never reuses this instance
            resource.markInvalidatedByCallback();

            if (chromePid != null) {
                // TERM then KILL
                killPid(chromePid, false);
                sleepQuiet(TERM_WAIT_MILLIS);
                if (isPidAlive(chromePid)) {
                    killPid(chromePid, true);
                    sleepQuiet(KILL_WAIT_MILLIS);
                }
            } else {
                LOGGER.warn("Hard-kill requested but chromePid is null (reason={}).", reason);
            }
        } finally {
            // Important: if closeAndQuit hung, it may not have nulled driver. Do it here.
            try {
                resource.driver = null; // same package => allowed (driver is protected)
            } catch (Exception ignore) {
                // ignore
            }
        }

        LOGGER.warn("Hard-kill executed (reason={}, chromePid={})", reason, chromePid);
    }

    private void killPid(long pid, boolean force) {
        // kill -TERM <pid> or kill -KILL <pid>
        String signal = force ? "-KILL" : "-TERM";
        try {
            Process p = new ProcessBuilder("kill", signal, Long.toString(pid)).redirectErrorStream(true).start();
            drain(p);
            p.waitFor(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.warn("Failed to send {} to pid={}", signal, pid, e);
        }
    }

    private boolean isPidAlive(long pid) {
        // kill -0 <pid> returns 0 if process exists and we have permission
        try {
            Process p = new ProcessBuilder("kill", "-0", Long.toString(pid)).redirectErrorStream(true).start();
            drain(p);
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void drain(Process p) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (br.readLine() != null) {
                // drain
            }
        } catch (Exception ignore) {
        }
    }

    private void sleepQuiet(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
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
