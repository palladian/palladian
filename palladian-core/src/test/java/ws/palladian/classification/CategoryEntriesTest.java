package ws.palladian.classification;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CategoryEntriesTest {
    
    @Test
    public void testCategoryEntries() {
        CategoryEntries categoryEntries = new CategoryEntries();
        categoryEntries.add(new CategoryEntry("category1", 0.8));
        categoryEntries.add(new CategoryEntry("category2", 0.1));
        
        assertEquals(2, categoryEntries.size());
        assertEquals("category1", categoryEntries.getMostLikelyCategoryEntry().getName());
        
        categoryEntries.add(new CategoryEntry("category1", 0.9));
        assertEquals(0.9, categoryEntries.getMostLikelyCategoryEntry().getProbability(), 0);
    }

}
