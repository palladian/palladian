package ws.palladian.iirmodel.helper;

import java.util.List;

import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemStream;

/**
 * 
 * @author Philipp Katz
 *
 */
public class ItemStreamHelper {

    private static final String NEW_LINE = "\n";

    private ItemStreamHelper() {
        // prevent instantiation
    }

    /**
     * Get a human readable list from an {@link ItemStream}.
     * 
     * @param itemStream
     */
    public static void print(ItemStream itemStream) {

        StringBuilder sb = new StringBuilder();
        sb.append("Channel Name: " + itemStream.getChannelName()).append(NEW_LINE);
        sb.append("Source Address: " + itemStream.getSourceAddress()).append(NEW_LINE);
        sb.append("Stream Source: " + itemStream.getStreamSource()).append(NEW_LINE);
        sb.append("--------------------").append(NEW_LINE);

        List<Item> items = itemStream.getItems();
        for (Item item : items) {
            sb.append(item.getTitle() + ": " + item.getLink()).append(NEW_LINE);
        }

        System.out.println(sb.toString());

    }

}
