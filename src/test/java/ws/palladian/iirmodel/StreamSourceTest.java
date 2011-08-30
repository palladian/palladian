package ws.palladian.iirmodel;

import static org.junit.Assert.assertSame;

import org.junit.Test;

import ws.palladian.iirmodel.helper.DefaultStreamVisitor;

public class StreamSourceTest {
    
    @Test
    public void testStreamSourceVisitor() {
        
        StreamGroup streamGroup1 = new StreamGroup("grandma", "grandma");
        StreamGroup streamGroup2 = new StreamGroup("mother", "mother");
        StreamGroup streamGroup3 = new StreamGroup("uncle", "uncle");
        streamGroup1.addChild(streamGroup2);
        streamGroup1.addChild(streamGroup3);
        
        ItemStream itemStream1 = new ItemStream("brother", "brother");
        ItemStream itemStream2 = new ItemStream("sister", "sister");
        streamGroup2.addChild(itemStream1);
        streamGroup2.addChild(itemStream2);
        
        ItemStream itemStream3 = new ItemStream("cousin", "cousin");
        streamGroup3.addChild(itemStream3);
        
        Item item1 = new Item("i1", null, "", "i1", null, null, "");
        Item item2 = new Item("i2", null, "", "i1", null, null, "");
        Item item3 = new Item("i3", null, "", "i1", null, null, "");
        itemStream1.addItem(item1);
        itemStream1.addItem(item2);
        itemStream1.addItem(item3);
        
        final Object[] expectedOrder = { streamGroup1, streamGroup2, itemStream1, item1, item2, item3, itemStream2, streamGroup3, itemStream3 };
        
        streamGroup1.accept(new DefaultStreamVisitor() {
            
            @Override
            public void visitItemStream(ItemStream itemStream, int depth) {
                validate(itemStream);
            }
            
            @Override
            public void visitStreamGroup(StreamGroup streamGroup, int depth) {
                validate(streamGroup);
            }
            
            @Override
            public void visitItem(Item item, int depth) {
                validate(item);
            }
            
            int index = 0;

            private void validate(Object object) {
                assertSame(expectedOrder[index++], object);
            }
        });
        
    }

}
