package ws.palladian.helper.functional;

import org.junit.Test;

import java.util.function.Predicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FiltersTest {

    @Test
    public void testAndFilter() {
        Predicate<String> filter1 = Predicates.regex("[a-z]+");
        Predicate<String> filter2 = Predicates.equal("apple", "banana", "cranberry", "Durian");
        Predicate<String> andFilter = Predicates.and(filter1, filter2);
        assertFalse(andFilter.test("kiwi"));
        assertTrue(andFilter.test("apple"));
        assertFalse(andFilter.test("Durian"));
    }

    @Test
    public void testOrFilter() {
        Predicate<String> filter1 = Predicates.equal("apple");
        Predicate<String> filter2 = Predicates.equal("banana");
        Predicate<String> orFilter = Predicates.or(filter1, filter2);
        assertTrue(orFilter.test("apple"));
        assertTrue(orFilter.test("banana"));
        assertFalse(orFilter.test("kiwi"));
    }

}
