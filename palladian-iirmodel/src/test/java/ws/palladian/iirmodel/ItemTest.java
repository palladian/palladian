package ws.palladian.iirmodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

public class ItemTest {

    @Test
    public void testEquals() {

        ItemStream stream1 = new ItemStream("stream1", "http://source.de/stream1");
        Author author = new Author("a1", 10, 2, 5, new Date(), stream1.getSourceAddress());

        // item1 and changedItem1 are "equal",
        // because they are in the same ItemStream and have the same sourceInternalIdentifier
        Item item1 = new Item("i1", author, "http://testSource.de/testStream/i1", "i1", new Date(), new Date(),
                "i1text", null);
        Item item2 = new Item("i2", author, "http://testSource.de/testStream/i2", "i2", new Date(), new Date(),
                "i2text", item1);
        Item changedItem1 = new Item("i1", author, "http://testSource.de/testStream/i1", "i1changed", new Date(),
                new Date(), "i1changedText", null);
        stream1.addItem(item1);
        stream1.addItem(item2);
        stream1.addItem(changedItem1);

        assertFalse(item1.equals(item2));
        assertTrue(item1.equals(changedItem1));

        // item21 and item1 are not equal, because they are in different streams
        ItemStream stream2 = new ItemStream("stream2", "http://source.de/stream2");
        Item item21 = new Item("i1", author, "http://testSource.de/testStream/i1", "i1", new Date(), new Date(),
                "i1text", null);
        stream2.addItem(item21);

        assertFalse(item21.equals(item1));

    }

}
