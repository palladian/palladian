package tud.iir.helper.shingling;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test data is from http://codingplayground.blogspot.com/2008/06/shingling-and-text-clustering.html
 * 
 * @author Philipp Katz
 * 
 */
public class ShinglesTest {

    @Test
    public void testShingles() {

        Shingles shingles = new Shingles();

        shingles.addFile("data/test/shingles/cluster1"); // #1
        shingles.addFile("data/test/shingles/cluster2"); // #2
        shingles.addFile("data/test/shingles/cluster3"); // #3
        shingles.addFile("data/test/shingles/cluster3_a"); // #4
        shingles.addFile("data/test/shingles/cluster4"); // #5
        shingles.addFile("data/test/shingles/cluster4_a"); // #6
        shingles.addFile("data/test/shingles/cluster4_b"); // #7
        shingles.addFile("data/test/shingles/cluster4_c"); // #8
        shingles.addFile("data/test/shingles/cluster5"); // #9
        shingles.addFile("data/test/shingles/cluster5_a"); // 10

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
