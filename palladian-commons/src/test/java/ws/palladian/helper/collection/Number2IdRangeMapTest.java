package ws.palladian.helper.collection;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class Number2IdRangeMapTest {
    @Rule
    public final ErrorCollector collector = new ErrorCollector();

    private Number2IdRangeMap rm;

    @Before
    public void setUp() throws Exception {
        rm = new Number2IdRangeMap();
        rm.put(4.9, 3);
        rm.put(0.9, 1);
        rm.put(3.3, 2);
        rm.put(323.3, 4);
    }

    @Test
    public void testGetValues() {
        collector.checkThat(rm.getValues(4.9, ComparisonType.EQUALS), Matchers.hasItem(3));
        collector.checkThat(rm.getValues(4.9, ComparisonType.EQUALS).size(), Matchers.equalTo(1));

        collector.checkThat(rm.getValues(1.1, ComparisonType.LESS), Matchers.hasItem(1));
        collector.checkThat(rm.getValues(1.1, ComparisonType.LESS).size(), Matchers.equalTo(1));

        collector.checkThat(rm.getValues(3.3, ComparisonType.LESS_EQUALS), Matchers.hasItems(1, 2));
        collector.checkThat(rm.getValues(3.3, ComparisonType.LESS_EQUALS).size(), Matchers.equalTo(2));

        collector.checkThat(rm.getValues(4.9, ComparisonType.MORE), Matchers.hasItem(4));
        collector.checkThat(rm.getValues(4.9, ComparisonType.MORE).size(), Matchers.equalTo(1));

        collector.checkThat(rm.getValues(4.9, ComparisonType.MORE_EQUALS), Matchers.hasItems(3, 4));
        collector.checkThat(rm.getValues(4.9, ComparisonType.MORE_EQUALS).size(), Matchers.equalTo(2));

    }

    @Test
    public void testGetValuesBetween() throws Exception {
        collector.checkThat(rm.getValuesBetween(0.5, 200.3), Matchers.hasItems(1, 2, 3));
        collector.checkThat(rm.getValuesBetween(0.5, 200.3).size(), Matchers.equalTo(3));
    }
}