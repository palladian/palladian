package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.w3c.dom.Document;
import ws.palladian.helper.ResourcePool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Pool rendering document retrievers as instantiating them is time-consuming.
 *
 * @author David Urbansky
 */
public class RenderingDocumentRetrieverPool extends ResourcePool<RenderingDocumentRetriever> {
    private final DriverManagerType driverManagerType;
    private final org.openqa.selenium.Proxy proxy;
    private final String userAgent;
    private final String driverVersionCode;

    // we can pass the binary of the browser to use
    private String binaryPath;

    private Set<String> additionalOptions;

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

        // we have to shut down the browsers or the RAM will be used up rather quickly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = 0; i < size; ++i) {
                try {
                    pool.take().closeAndQuit();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    @Override
    protected RenderingDocumentRetriever createObject() {
        return new RenderingDocumentRetriever(driverManagerType, proxy, userAgent, driverVersionCode, binaryPath, additionalOptions);
    }

    public void replace(RenderingDocumentRetriever resource) {
        try {
            resource.closeAndQuit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        pool.add(createObject());
    }

    public void closePool() {
        for (int i = 0; i < pool.size(); i++) {
            try {
                pool.take().closeAndQuit();
            } catch (Exception e) {
                e.printStackTrace();
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
                Document webDocument = retriever.getWebDocument(url);
                pool.recycle(retriever);
                return webDocument;
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
