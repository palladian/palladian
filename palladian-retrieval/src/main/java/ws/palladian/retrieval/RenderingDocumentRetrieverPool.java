package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.ResourcePool;

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

    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService quitExecutor = Executors.newCachedThreadPool();

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
            LOGGER.info("Pool Stats: createdDrivers={}, replacedDrivers={}, quitFailures={}, quitTimeouts={}", createdDrivers.get(), replacedDrivers.get(), quitFailures.get(),
                    quitTimeouts.get());
        }, 60, 60, TimeUnit.SECONDS);

        // we have to shut down the browsers or the RAM will be used up rather quickly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = 0; i < size; ++i) {
                try {
                    RenderingDocumentRetriever r = pool.poll(10, TimeUnit.SECONDS);
                    closeWithStats(r);
                } catch (Exception e) {
                    e.printStackTrace();
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

        if (additionalOptions != null) {
            if (additionalOptions.contains(NO_DELETE_DRIVER_COOKIES)) {
                renderingDocumentRetriever.setDeleteDriverCookiesBeforeUse(false);
            }
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
            e.printStackTrace();
        }
        RenderingDocumentRetriever newResource = createObject();
        try {
            pool.add(newResource);
        } catch (Exception e) {
            // If we cannot add to the pool (e.g. full), we must close the new resource to avoid leak
            closeWithStats(newResource);
            e.printStackTrace();
        }
    }

    public void closePool() {
        for (int i = 0; i < size; i++) {
            try {
                RenderingDocumentRetriever take = pool.take();
                closeWithStats(take);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        quitExecutor.shutdownNow();
        monitorExecutor.shutdownNow();
    }

    private void closeWithStats(RenderingDocumentRetriever resource) {
        if (resource == null) {
            return;
        }
        Future<Boolean> future = quitExecutor.submit(resource::closeAndQuit);
        try {
            Boolean result = future.get(30, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(result)) {
                quitFailures.incrementAndGet();
            }
        } catch (TimeoutException e) {
            quitTimeouts.incrementAndGet();
            future.cancel(true);
            LOGGER.error("Timeout quitting driver", e);
        } catch (Exception e) {
            quitFailures.incrementAndGet();
            LOGGER.error("Error quitting driver", e);
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
                    Document webDocument = retriever.getWebDocument(url);
                    return webDocument;
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
