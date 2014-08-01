package ws.palladian.helper.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.junit.Test;

import ws.palladian.helper.functional.ActionIteratorAdapter;
import ws.palladian.helper.functional.Consumer;

public class ActionIteratorAdapterTest {

    @Test
    public void testActionIteratorAdapter() {
        new ActionIteratorAdapter<String>() {
            @Override
            protected void produce(Consumer<String> action) {
                action.process("one");
                action.process("two");
                action.process("three");
                action.process("four");
                action.process("five");
            }

            @Override
            protected void consume(Iterable<String> iterable) {
                Iterator<String> iterator1 = iterable.iterator();
                Iterator<String> iterator2 = iterable.iterator();
                assertEquals("one", iterator1.next());
                assertEquals("one", iterator2.next());
                assertEquals("two", iterator1.next());
                assertEquals("two", iterator2.next());
                assertEquals("three", iterator1.next());
                assertEquals("three", iterator2.next());
                assertEquals("four", iterator1.next());
                assertEquals("four", iterator2.next());
                assertEquals("five", iterator1.next());
                assertEquals("five", iterator2.next());
                assertFalse(iterator1.hasNext());
                assertFalse(iterator2.hasNext());
            }

        };

    }

}
