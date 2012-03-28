package ws.palladian.helper.shingling;

import java.util.List;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.StopWatch;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

public class ShinglesPerformanceTest {

    private static FeedDatabase fd = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig());

    public static void main(String[] args) {
        runTest(new ShinglesIndexJDBM(), 10000);
        // runTest(new ShinglesIndexLucene(), 10000);
    }

    private static void runTest(ShinglesIndex index, int limit) {

        Shingles shingles = new Shingles(index);

        final int fetch = 100;
        int offset = 0;
        int foundDups = 0;
        int lastFetch = 0;
        StopWatch sw = new StopWatch();

        do {
            List<FeedItem> entries = fd.getFeedItems(fetch, offset);
            lastFetch = entries.size();

            for (FeedItem feedEntry : entries) {
                boolean isDup = shingles.addDocument(feedEntry.getId(), feedEntry.getText());
                if (isDup) {
                    foundDups++;
                }
            }

            // System.out.println(offset + " -> " + sw.getElapsedTime());
            System.out.println(sw.getElapsedTime());

            offset += fetch;

        } while (offset < limit && lastFetch > 0);

        shingles.saveIndex();
        index.deleteIndex();

        System.out.println(index.getClass().getName() + " found " + foundDups + " in " + sw.getElapsedTimeString());

    }

}
