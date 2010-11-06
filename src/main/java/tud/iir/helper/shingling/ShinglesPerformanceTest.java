package tud.iir.helper.shingling;

import java.util.List;

import tud.iir.helper.StopWatch;
import tud.iir.news.FeedDatabase;
import tud.iir.news.FeedItem;

public class ShinglesPerformanceTest {

    private static FeedDatabase fd = FeedDatabase.getInstance();

    public static void main(String[] args) {
        // runTest(new ShinglesIndexJDBM(), 1000);
        runTest(new ShinglesIndexLucene(), 1000);
    }

    private static void runTest(ShinglesIndex index, int limit) {

        Shingles shingles = new Shingles(index);

        final int fetch = 100;
        int offset = 0;
        int foundDups = 0;
        StopWatch sw = new StopWatch();

        do {
            List<FeedItem> entries = fd.getFeedEntries(fetch, offset);

            for (FeedItem feedEntry : entries) {
                boolean isDup = shingles.addDocument(feedEntry.getId(), feedEntry.getText());
                if (isDup) {
                    foundDups++;
                }
            }

            System.out.println(offset + " -> " + sw.getElapsedTime());

            offset += fetch;

        } while (offset < limit);

        shingles.saveIndex();
        index.deleteIndex();

        System.out.println(index.getClass().getName() + " found " + foundDups + " in " + sw.getElapsedTimeString());

    }

}
