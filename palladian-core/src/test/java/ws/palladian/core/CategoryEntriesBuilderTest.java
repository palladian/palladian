package ws.palladian.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.junit.Test;

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

        // iteration order should be descending
        Iterator<Category> iterator = categoryEntries.iterator();
        assertEquals("B", iterator.next().getName());
        assertEquals("C", iterator.next().getName());
        assertEquals("A", iterator.next().getName());
        assertFalse(iterator.hasNext());

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
    
    @Test
    public void testCategoryEntriesBuidler_add() {
        CategoryEntriesBuilder builder1 = new CategoryEntriesBuilder();
        builder1.set("B", 50);
        builder1.set("C", 20);

        CategoryEntriesBuilder builder2 = new CategoryEntriesBuilder();
        builder2.set("A", 10);
        builder2.set("C", 20);
        
        CategoryEntries cateegoryEntries = builder1.add(builder2).create();
        assertEquals(0.1, cateegoryEntries.getProbability("A"), DELTA);
        assertEquals(0.5, cateegoryEntries.getProbability("B"), DELTA);
        assertEquals(0.4, cateegoryEntries.getProbability("C"), DELTA);
    }

}
