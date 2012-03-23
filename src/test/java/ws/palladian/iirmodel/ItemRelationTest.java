package ws.palladian.iirmodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

public class ItemRelationTest {
    
    @Test
    public void testSaveRelation() {
        Item testItem1 = new Item("testItem1", null, "http://testSource.de/testItem", "testItem", new Date(),
                new Date(), "testItemText");
        Item testItem2 = new Item("testItem2", null, "http://testSource.de/testItem2", "testItem2", new Date(),
                new Date(), "testItemText");
        RelationType relationType = new RelationType("cause", "cause");
        
        ItemRelation undirectedRelation1 = new UndirectedItemRelation(testItem1, testItem2, relationType, null);
        ItemRelation undirectedRelation2 = new UndirectedItemRelation(testItem2, testItem1, relationType, null);
        assertTrue(undirectedRelation1.equals(undirectedRelation2));
        
        ItemRelation directedRelation1 = new DirectedItemRelation(testItem1, testItem2, relationType, null);
        ItemRelation directedRelation2 = new DirectedItemRelation(testItem2, testItem1, relationType, null);
        assertFalse(directedRelation1.equals(directedRelation2));
    }

}
