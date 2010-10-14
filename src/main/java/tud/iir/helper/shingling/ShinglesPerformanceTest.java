package tud.iir.helper.shingling;

import java.util.List;

import tud.iir.helper.StopWatch;
import tud.iir.news.FeedDatabase;
import tud.iir.news.FeedEntry;

public class ShinglesPerformanceTest {

    public static void main(String[] args) {
        // ShinglesIndex index = new ShinglesIndexJava();
        // ShinglesIndex index = new ShinglesIndexJDBM();
        ShinglesIndex index = new ShinglesIndexLucene();

        Shingles shingles = new Shingles(index);

        FeedDatabase fd = FeedDatabase.getInstance();

        final int limit = 100;
        int offset = 0;
        int foundDups = 0;
        StopWatch sw = new StopWatch();

        do {
            List<FeedEntry> entries = fd.getFeedEntries(limit, offset);

            for (FeedEntry feedEntry : entries) {

                boolean isDup = shingles.addDocument(feedEntry.getId(), feedEntry.getText());
                // /System.out.println(feedEntry.getId() + " is duplicate : " + isDup);
                if (isDup) {
                    foundDups++;
                }

            }

            System.out.println(offset + " -> " + sw.getElapsedTime());

            offset += limit;

        } while (offset < 10000);

        shingles.saveIndex();

        System.out.println("found " + foundDups + " in " + sw.getElapsedTimeString());

        // JDBM ---> found 84 in 19s:783ms
        // Lucene ---> found 84 in 1m:51s:238ms

    }

}
