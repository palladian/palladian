package ws.palladian.helper.collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.helper.functional.Filter;

public class FilterChainTest {

    @Test
    public void testFilterChain() {
        RegexFilter filter1 = new RegexFilter("[a-z]+");
        EqualsFilter<String> filter2 = EqualsFilter.create("apple", "banana", "cranberry", "Durian");
        @SuppressWarnings("unchecked")
        Filter<String> chain = new FilterChain<String>(filter1, filter2);
        assertFalse(chain.accept("kiwi"));
        assertTrue(chain.accept("apple"));
        assertFalse(chain.accept("Durian"));
    }

}
