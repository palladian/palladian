package ws.palladian.helper.collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;

public class FilterChainTest {

    @Test
    public void testFilterChain() {
        Filter<String> filter1 = Filters.regex("[a-z]+");
        Filter<String> filter2 = Filters.equal("apple", "banana", "cranberry", "Durian");
        @SuppressWarnings("unchecked")
        Filter<String> chain = new FilterChain<String>(filter1, filter2);
        assertFalse(chain.accept("kiwi"));
        assertTrue(chain.accept("apple"));
        assertFalse(chain.accept("Durian"));
    }

}
