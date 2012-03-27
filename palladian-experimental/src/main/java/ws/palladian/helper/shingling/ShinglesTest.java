package ws.palladian.helper.shingling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import ws.palladian.helper.shingling.Shingles;

/**
 * Test data is from http://codingplayground.blogspot.com/2008/06/shingling-and-text-clustering.html
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * 
 */
public class ShinglesTest {

    @Test
    public void testShingles() {

        Shingles shingles = new Shingles();

        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster1").getFile()); // #1
        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster2").getFile()); // #2
        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster3").getFile()); // #3
        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster3_a").getFile()); // #4
        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster4").getFile()); // #5
        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster4_a").getFile()); // #6
        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster4_b").getFile()); // #7
        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster4_c").getFile()); // #8
        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster5").getFile()); // #9
        shingles.addFile(ShinglesTest.class.getResource("/shingles/cluster5_a").getFile()); // 10

        // System.out.println(shingles.getSimilarityReport());

        Map<Integer, Set<Integer>> similarDocuments = shingles.getSimilarDocuments();

        assertNull(similarDocuments.get(1)); // #1 has no similarities
        assertNull(similarDocuments.get(2)); // #2 has no similarities

        Iterator<Integer> tmp = similarDocuments.get(3).iterator(); // similarity between #3 and #4
        assertEquals((int) tmp.next(), 4);

        tmp = similarDocuments.get(5).iterator(); // similarity between #5, #6, #7, #8
        assertEquals((int) tmp.next(), 6);
        assertEquals((int) tmp.next(), 7);
        assertEquals((int) tmp.next(), 8);

        tmp = similarDocuments.get(9).iterator(); // similarity between #9 and #10
        assertEquals((int) tmp.next(), 10);

    }

}
