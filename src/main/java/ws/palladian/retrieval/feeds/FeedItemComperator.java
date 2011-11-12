package ws.palladian.retrieval.feeds;

import java.util.Comparator;

/**
 * Compare two {@link FeedItem}s by their correctedPublishDate.
 * 
 * @author Sandro Reichert
 */
public class FeedItemComperator implements Comparator<FeedItem> {

    /**
     * Compare two {@link FeedItem}s by their correctedPublishDate.
     * 
     * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater
     *         than the second. If first {@link FeedItem} has no correctedPublishDate, -1 is returned, if second is
     *         null, 1.
     */
    public int compare(FeedItem item1, FeedItem item2) {
        if (item1.getCorrectedPublishedDate() == null) {
            return -1;
        }
        if (item2.getCorrectedPublishedDate() == null) {
            return 1;
        }
        return item1.getCorrectedPublishedDate().compareTo(item2.getCorrectedPublishedDate());
    }

    // public static void main(String[] args) {
    // FeedItem item1 = new FeedItem();
    // item1.setCorrectedPublishedDate(new Date());
    // ThreadHelper.deepSleep(3000);
    // FeedItem item2 = new FeedItem();
    // item2.setCorrectedPublishedDate(new Date());
    // FeedItemComperator comp = new FeedItemComperator();
    // System.out.println(comp.compare(item1, item2));
    // }

}
