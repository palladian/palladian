package ws.palladian.helper.shingling;

import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;

/**
 * Test for various ShinglesIndex implementations.
 * 
 * @author Philipp Katz
 * 
 */
public class ShinglesIndexTest {

    @Test
    public void testShinglesIndexJava() {
        testProcedure(new ShinglesIndexJava());
    }

    @Test
    public void testShinglesIndexH2() {
        testProcedure(new ShinglesIndexH2());
    }

    @Test
    public void testShinglesIndexJDBM() {
        testProcedure(new ShinglesIndexJDBM());
    }

    @Test
    public void testShinglesIndexWB() {
        testProcedure(new ShinglesIndexWB());
    }

    @Test
    public void testShinglesIndexLucene() {
        ShinglesIndexLucene shinglesIndexLucene = new ShinglesIndexLucene();
        testProcedure(shinglesIndexLucene);
        testProcedure(shinglesIndexLucene);
    }

    private void testProcedure(ShinglesIndex index) {

        index.openIndex();

//        assertEquals("we need to start with an empty index", 0, index.getNumberOfDocuments());

        Set<Long> sketch = new HashSet<Long>(Arrays.asList(28372738L, 30948342L, -12093182L));
        Set<Long> sketch2 = new HashSet<Long>(Arrays.asList(28372738L, 30948342L, -12093182L, 18327378L));
        Set<Long> sketch3 = new HashSet<Long>(Arrays.asList(39892238L, 58979347L, 18337847L, 34673743L));

        index.addDocument(1, sketch);
        index.addDocument(2, sketch2);
        index.addDocument(3, sketch);
        index.addDocument(4, sketch3);

        assertEquals(3, index.getDocumentsForHash(28372738L).size());
        assertEquals(3, index.getDocumentsForHash(30948342L).size());
        assertEquals(2, (int) index.getDocumentsForHash(18327378L).iterator().next()); // should return doc no. 2
        assertEquals(4, (int) index.getDocumentsForHash(34673743L).iterator().next()); // should return doc no. 4

        Map<Integer, Set<Long>> documentsForSketch = index.getDocumentsForSketch(sketch);
        assertEquals(3, documentsForSketch.size());
        assertEquals(3, documentsForSketch.get(1).size());
        assertEquals(4, documentsForSketch.get(2).size());

        assertEquals(4, index.getNumberOfDocuments());

        index.addDocumentSimilarity(1, 2);
        index.addDocumentSimilarity(1, 3);
        index.addDocumentSimilarity(2, 4);

        assertEquals(2, index.getSimilarDocuments(1).size());
        assertEquals(4, (int) index.getSimilarDocuments(2).iterator().next()); // should return doc no. 4
        assertEquals(2, index.getSimilarDocuments().size());

        index.saveIndex();

        // clean up
        index.deleteIndex();

        // XXX why is the following line necessary on Windows?
        FileHelper.cleanDirectory(ShinglesIndexBaseImpl.INDEX_FILE_BASE_PATH);
    }

}
