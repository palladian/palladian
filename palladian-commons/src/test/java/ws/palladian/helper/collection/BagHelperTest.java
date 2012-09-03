package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.junit.Test;


public class BagHelperTest {

    @Test
    public void testGetHighest() {
        Bag<String> bag = new HashBag<String>();
        bag.add("a");
        bag.add("a");
        bag.add("b");
        bag.add("b");
        bag.add("b");
        bag.add("c");

        String highest = BagHelper.getHighest(bag);
        assertEquals("b", highest);
        

        Bag<String> highest2 = BagHelper.getHighest(bag, 2);
        assertEquals(2, highest2.uniqueSet().size());
        assertEquals(3, highest2.getCount("b"));
        assertEquals(2, highest2.getCount("a"));
        

    }

}
