package ws.palladian.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;

public class CategoryEntriesBuilderTest {

    private static final double DELTA = 0.0001;

    @Test
    public void testCategoryEntriesBuilder() {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        builder.set("A", 10);
        builder.set("B", 50);
        builder.set("C", 20);
        builder.add("B", 20);

        CategoryEntries categoryEntries = builder.create();
        assertEquals("B", categoryEntries.getMostLikelyCategory());
        assertEquals(0.1, categoryEntries.getProbability("A"), DELTA);
        assertEquals(0.7, categoryEntries.getProbability("B"), DELTA);
        assertEquals(0.2, categoryEntries.getProbability("C"), DELTA);

        builder = new CategoryEntriesBuilder();
        builder.set("A", 10);
        builder.set("B", 10);
        builder.set("D", 80);
        CategoryEntries categoryEntries2 = builder.create();

        builder = new CategoryEntriesBuilder();
        builder.add(categoryEntries);
        builder.add(categoryEntries2);
        CategoryEntries categoryEntries3 = builder.create();
        assertEquals(0.1, categoryEntries3.getProbability("A"), DELTA);
        assertEquals(0.4, categoryEntries3.getProbability("B"), DELTA);
        assertEquals(0.1, categoryEntries3.getProbability("C"), DELTA);
        assertEquals(0.4, categoryEntries3.getProbability("D"), DELTA);
    }

    @Test
    public void testCategoryEntriesBuilder_negativeValues() {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        builder.set("A", -20);
        builder.set("B", -21);
        builder.set("D", -19);
        CategoryEntries categoryEntries = builder.create();
        assertEquals("D", categoryEntries.getMostLikely().getName());
        assertEquals(0.6833, categoryEntries.getMostLikely().getProbability(), DELTA);
    }

}
