package ws.palladian.helper.functional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FiltersTest {

    @Test
    public void testAndFilter() {
    	Filter<String> filter1 = Filters.regex("[a-z]+");
    	Filter<String> filter2 = Filters.equal("apple", "banana", "cranberry", "Durian");
    	Filter<String> andFilter = Filters.and(filter1, filter2);
        assertFalse(andFilter.accept("kiwi"));
        assertTrue(andFilter.accept("apple"));
        assertFalse(andFilter.accept("Durian"));
    }
    @Test
    public void testOrFilter() {
    	Filter<String> filter1 = Filters.equal("apple");
    	Filter<String> filter2 = Filters.equal("banana");
    	Filter<String> orFilter = Filters.or(filter1, filter2);
    	assertTrue(orFilter.accept("apple"));
    	assertTrue(orFilter.accept("banana"));
    	assertFalse(orFilter.accept("kiwi"));
    }

}
