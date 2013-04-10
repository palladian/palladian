package ws.palladian.classification;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CategoryEntriesMapTest {
    
    @Test
    public void testCategoryEntries() {
        CategoryEntriesMap categoryEntries = new CategoryEntriesMap();
        categoryEntries.set("category1", 0.8);
        categoryEntries.set("category2", 0.1);
        
        assertEquals("category1", categoryEntries.getMostLikelyCategory());
        
        categoryEntries.set("category1", 0.9);
        assertEquals(0.9, categoryEntries.getProbability(categoryEntries.getMostLikelyCategory()), 0);
    }

}
