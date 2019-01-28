package ws.palladian.retrieval;

import io.github.bonigarcia.wdm.DriverManagerType;
import org.w3c.dom.Document;
import ws.palladian.helper.ResourcePool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Pool rendering document retrievers as instantiating them is time consuming.
 * @author David Urbansky
 */
public class RenderingDocumentRetrieverPool extends ResourcePool<RenderingDocumentRetriever> {
    private DriverManagerType driverManagerType;

    public RenderingDocumentRetrieverPool(DriverManagerType driverManagerType, int size) {
        super(size);
        this.driverManagerType = driverManagerType;
        initializePool();
    }

    @Override
    protected RenderingDocumentRetriever createObject() {
        return new RenderingDocumentRetriever(driverManagerType);
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
