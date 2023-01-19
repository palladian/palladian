package ws.palladian.helper.collection;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class RangeMapTest {
    private RangeMap<Double, String> rm;

    @Before
    public void setUp() throws Exception {
        rm = new RangeMap<>();
        rm.put(4.9, "C");
        rm.put(0.9, "A");
        rm.put(3.3, "B");
        rm.put(323.3, "D");
    }

    @Test
    public void testGetValues() throws Exception {
        assertThat(rm.getValues(4.9, ComparisonType.EQUALS), Matchers.hasItem("C"));
        assertThat(rm.getValues(4.9, ComparisonType.EQUALS).size(), Matchers.equalTo(1));

        assertThat(rm.getValues(1.1, ComparisonType.LESS), Matchers.hasItem("A"));
        assertThat(rm.getValues(1.1, ComparisonType.LESS).size(), Matchers.equalTo(1));

        assertThat(rm.getValues(3.3, ComparisonType.LESS_EQUALS), Matchers.hasItems("A", "B"));
        assertThat(rm.getValues(3.3, ComparisonType.LESS_EQUALS).size(), Matchers.equalTo(2));

        assertThat(rm.getValues(4.9, ComparisonType.MORE), Matchers.hasItem("D"));
        assertThat(rm.getValues(4.9, ComparisonType.MORE).size(), Matchers.equalTo(1));

        assertThat(rm.getValues(4.9, ComparisonType.MORE_EQUALS), Matchers.hasItems("D", "C"));
        assertThat(rm.getValues(4.9, ComparisonType.MORE_EQUALS).size(), Matchers.equalTo(2));

    }

    @Test
    public void testGetValuesBetween() throws Exception {
        assertThat(rm.getValuesBetween(0.5, 200.3), Matchers.hasItems("A", "B", "C"));
        assertThat(rm.getValuesBetween(0.5, 200.3).size(), Matchers.equalTo(3));
    }
}