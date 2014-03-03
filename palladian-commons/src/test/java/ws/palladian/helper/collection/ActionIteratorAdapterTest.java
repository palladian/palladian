package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.junit.Test;

import ws.palladian.helper.io.Action;

public class ActionIteratorAdapterTest {

    @Test
    public void testActionIteratorAdapter() {
        new ActionIteratorAdapter<String>() {
            @Override
            protected void produce(Action<String> action) {
                action.process("one");
                action.process("two");
                action.process("three");
                action.process("four");
                action.process("five");
            }

            @Override
            protected void consume(Iterator<String> iterator) {
                assertEquals("one", iterator.next());
                assertEquals("two", iterator.next());
                assertEquals("three", iterator.next());
                assertEquals("four", iterator.next());
                assertEquals("five", iterator.next());
                assertFalse(iterator.hasNext());
            }

        };
    }

}
