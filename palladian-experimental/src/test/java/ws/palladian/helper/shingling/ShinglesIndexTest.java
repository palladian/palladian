package ws.palladian.helper.shingling;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Test for various {@link ShinglesIndex} implementations.
 * </p>
 * 
 * @author Philipp Katz
 */
@RunWith(Parameterized.class)
public class ShinglesIndexTest {

    private final ShinglesIndex index;

    @Parameters
    public static Collection<Object[]> indices() {
        List<Object[]> indices = CollectionHelper.newArrayList();
        indices.add(new Object[] {new ShinglesIndexJava()});
        indices.add(new Object[] {new ShinglesIndexH2()});
        indices.add(new Object[] {new ShinglesIndexJDBM()});
        indices.add(new Object[] {new ShinglesIndexWB()});
        indices.add(new Object[] {new ShinglesIndexLucene()});
        return indices;
    }
    
    public ShinglesIndexTest(ShinglesIndex idx) {
        this.index = idx;
    }

    @Before
    public void setUp() {
        index.openIndex();
    }

    @After
    public void tearDown() {

        // clean up
        index.deleteIndex();

        // XXX why is the following line necessary on Windows?
        FileHelper.cleanDirectory(ShinglesIndexBaseImpl.INDEX_FILE_BASE_PATH);

    }

    @Test
    public void testProcedure() {

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

    }

    @After
    public void cleanup() {
        index.deleteIndex();
    }

}
