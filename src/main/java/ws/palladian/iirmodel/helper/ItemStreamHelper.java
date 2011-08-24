package ws.palladian.iirmodel.helper;

import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemStream;
import ws.palladian.iirmodel.StreamGroup;
import ws.palladian.iirmodel.StreamSource;
import ws.palladian.iirmodel.StreamVisitor;

/**
 * 
 * @author Philipp Katz
 *
 */
public class ItemStreamHelper {

//    private static final String NEW_LINE = "\n";

    private ItemStreamHelper() {
        // prevent instantiation
    }

//    /**
//     * Get a human readable list from an {@link ItemStream}.
//     * 
//     * @param itemStream
//     */
//    @Deprecated
//    public static void print(ItemStream itemStream) {
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("Source Address: " + itemStream.getSourceAddress()).append(NEW_LINE);
//        sb.append("Stream Source: " + itemStream.getSourceName()).append(NEW_LINE);
//        sb.append("--------------------").append(NEW_LINE);
//
//        List<Item> items = itemStream.getItems();
//        for (Item item : items) {
//            sb.append(item.getTitle() + ": " + item.getLink()).append(NEW_LINE);
//        }
//
//        System.out.println(sb.toString());
//
//    }
    
    public static void print(StreamSource streamSource) {
        streamSource.accept(new PrintVisitor());
    }

    private static final class PrintVisitor implements StreamVisitor {

        @Override
        public void visitItemStream(ItemStream itemStream, int depth) {
            print(itemStream, depth);
        }

        @Override
        public void visitStreamGroup(StreamGroup streamGroup, int depth) {
            print(streamGroup, depth);
        }

        @Override
        public void visitItem(Item item, int depth) {
            System.out.println(getIndent(depth) + item.getTitle());
        }

        private void print(StreamSource streamSource, int depth) {
            System.out.println(getIndent(depth) + streamSource.getSourceName());
        }

        public String getIndent(int depth) {
            String ret = "";
            for (int i = 0; i < depth; i++) {
                ret += " ";
            }
            return ret;
        }

    }
    
    public static void main(String[] args) {
        
        StreamGroup streamGroup = new StreamGroup("SourceforgeNet", "");
        
        StreamGroup streamGroup1 = new StreamGroup("phpMyAdmin", "");
        streamGroup.addChild(streamGroup1);
        
        StreamGroup streamGroup2 = new StreamGroup("Forum", "");
        streamGroup1.addChild(streamGroup2);
        
        StreamGroup streamGroup3 = new StreamGroup("Help", "");
        streamGroup2.addChild(streamGroup3);
        
        ItemStream itemStream = new ItemStream("Thread 1", "");
        streamGroup3.addChild(itemStream);
        
        Item item1 = new Item("", null, "", "item1", null, null, "");
        Item item2 = new Item("", null, "", "item1", null, null, "");
        Item item3 = new Item("", null, "", "item1", null, null, "");
        itemStream.addItem(item1);
        itemStream.addItem(item2);
        itemStream.addItem(item3);
        
        print(streamGroup);
        
    }

}
