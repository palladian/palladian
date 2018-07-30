package ws.palladian.helper.functional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.function.Predicate;

import org.junit.Test;

public class FiltersTest {

    @Test
    public void testAndFilter() {
    	Predicate<String> filter1 = Filters.regex("[a-z]+");
    	Predicate<String> filter2 = Filters.equal("apple", "banana", "cranberry", "Durian");
    	Predicate<String> andFilter = Filters.and(filter1, filter2);
        assertFalse(andFilter.test("kiwi"));
        assertTrue(andFilter.test("apple"));
        assertFalse(andFilter.test("Durian"));
    }
    @Test
    public void testOrFilter() {
    	Predicate<String> filter1 = Filters.equal("apple");
    	Predicate<String> filter2 = Filters.equal("banana");
    	Predicate<String> orFilter = Filters.or(filter1, filter2);
    	assertTrue(orFilter.test("apple"));
    	assertTrue(orFilter.test("banana"));
    	assertFalse(orFilter.test("kiwi"));
    }

}
