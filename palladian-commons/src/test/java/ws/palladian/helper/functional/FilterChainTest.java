package ws.palladian.helper.functional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FilterChainTest {

    @Test
    public void testFilterChain() {
        Filter<String> filter1 = Filters.regex("[a-z]+");
        Filter<String> filter2 = Filters.equal("apple", "banana", "cranberry", "Durian");
        @SuppressWarnings("unchecked")
        Filter<String> chain = Filters.chain(filter1, filter2);
        assertFalse(chain.accept("kiwi"));
        assertTrue(chain.accept("apple"));
        assertFalse(chain.accept("Durian"));
    }

}
