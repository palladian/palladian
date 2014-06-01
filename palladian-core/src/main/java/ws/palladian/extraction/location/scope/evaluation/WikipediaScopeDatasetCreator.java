package ws.palladian.extraction.location.scope.evaluation;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ThreadHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.wikipedia.WikipediaPage;
import ws.palladian.retrieval.wikipedia.WikipediaPageReference;
import ws.palladian.retrieval.wikipedia.WikipediaUtil;

/**
 * <p>
 * Create a scope detection dataset from random Wikipedia articles.
 * </p>
 * 
 * @author pk
 * 
 */
public class WikipediaScopeDatasetCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaScopeDatasetCreator.class);

    /** Number of threads for simultaneous fetching. */
    private static final int NUM_TREADS = 10;

    private static final String WIKIPEDIA_EN = "http://en.wikipedia.org/w";

    private static final String OUTPUT_DIR = "/Users/pk/Desktop/wikipediaScopeDataset";

    private static final int MAX_SUBSEQUENT_ERROR_COUNT = 10;

    private static final AtomicInteger counter = new AtomicInteger();

    private static final AtomicInteger coordinateCounter = new AtomicInteger();

    private static final AtomicInteger errorCounter = new AtomicInteger();

    private static final class RequestThread extends Thread {

        @Override
        public void run() {
            for (;;) {

                try {

                    WikipediaPageReference reference = WikipediaUtil.retrieveRandomArticle(WIKIPEDIA_EN);
                    counter.incrementAndGet();

                    if (reference.getTitle().toLowerCase().startsWith("list of")) {
                        continue;
                    }

                    WikipediaPage article = WikipediaUtil.retrieveArticle(WIKIPEDIA_EN, reference.getTitle());
                    GeoCoordinate coordinate = article.getCoordinate();
                    if (coordinate != null) {
                        // System.out.println(article.getTitle() + " -> " + coordinate);

                        String articleText = article.getMarkup();

                        // save the article
                        String normalizedTitle = article.getTitle().replaceAll("\\s", "_").replace(';', '_')
                                .replace('/', '_').replaceAll("_+", "_");
                        String fileName = normalizedTitle + ".mediawiki";
                        String filePath = OUTPUT_DIR + "/" + fileName;
                        FileHelper.writeToFile(filePath, articleText);

                        // save the coordinates to file
                        String line = fileName + ";" + coordinate.getLatitude() + ";" + coordinate.getLongitude()
                                + "\n";
                        FileHelper.appendFile(OUTPUT_DIR + "/_coordinates.csv", line);

                        coordinateCounter.incrementAndGet();
                        errorCounter.set(0);
                        System.out.println(counter.get() + " requests sent, coordinate fraction: "
                                + (double)coordinateCounter.get() / counter.get());
                    }

                } catch (IllegalStateException e) {
                    if (errorCounter.incrementAndGet() == MAX_SUBSEQUENT_ERROR_COUNT) {
                        throw e;
                    }
                    LOGGER.warn("Encountered {}, waiting for some seconds, {} subsequent errors so far", e.toString(),
                            errorCounter);
                    ThreadHelper.deepSleep(TimeUnit.SECONDS.toMillis(30));
                }
            }
        }

    }

    public static void main(String[] args) {
        for (int i = 0; i < NUM_TREADS; i++) {
            new RequestThread().start();
        }
    }

}
