package ws.palladian.helper.nlp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class JaroWinklerDistanceTest {
    
    private final StringSimilarity stringSim = new JaroWinklerSimilarity();

    @Test
    public void testJaroWinkler() {
        
        // trivial cases
        assertEquals(1, stringSim.getSimilarity("", ""), 0.001);
        assertEquals(0, stringSim.getSimilarity("MARHTA", ""), 0.001);
        assertEquals(0, stringSim.getSimilarity("", "MARHTA"), 0.001);
        assertEquals(1, stringSim.getSimilarity("MARHTA", "MARHTA"), 0.001);

        // data from Wikipedia article
        assertEquals(0.961, stringSim.getSimilarity("MARTHA", "MARHTA"), 0.001);
        assertEquals(0.84, stringSim.getSimilarity("DWAYNE", "DUANE"), 0.001);
        assertEquals(0.813, stringSim.getSimilarity("DIXON", "DICKSONX"), 0.001);

        // data from "Overview of Record Linkage and Current Research Directions",
        // William E. Winkler, 2006, http://www.census.gov/srd/papers/pdf/rrs2006-02.pdf
        // the commented tests are errors in the paper.
        assertEquals(0.982, stringSim.getSimilarity("SHACKLEFORD", "SHACKELFORD"), 0.001);
        assertEquals(0.896, stringSim.getSimilarity("DUNNINGHAM", "CUNNIGHAM"), 0.001);
        assertEquals(0.956, stringSim.getSimilarity("NICHLESON", "NICHULSON"), 0.001);
        assertEquals(0.832, stringSim.getSimilarity("JONES", "JOHNSON"), 0.001);
        assertEquals(0.933, stringSim.getSimilarity("MASSEY", "MASSIE"), 0.001);
        assertEquals(0.922, stringSim.getSimilarity("ABROMS", "ABRAMS"), 0.001);
        // assertEquals(0, stringSim.getSimilarity("HARDIN", "MARTINEZ"), 0.001);
        // assertEquals(0, stringSim.getSimilarity("ITMAN", "SMITH"), 0.001);
        assertEquals(0.926, stringSim.getSimilarity("JERALDINE", "GERALDINE"), 0.001);
        assertEquals(0.961, stringSim.getSimilarity("MARHTA", "MARTHA"), 0.001);
        assertEquals(0.921, stringSim.getSimilarity("MICHELLE", "MICHAEL"), 0.001);
        assertEquals(0.933, stringSim.getSimilarity("JULIES", "JULIUS"), 0.001);
        assertEquals(0.880, stringSim.getSimilarity("TANYA", "TONYA"), 0.001);
        assertEquals(0.840, stringSim.getSimilarity("DWAYNE", "DUANE"), 0.001);
        assertEquals(0.805, stringSim.getSimilarity("SEAN", "SUSAN"), 0.001);
        assertEquals(0.933, stringSim.getSimilarity("JON", "JOHN"), 0.001);
        // assertEquals(0, stringSim.getSimilarity("JON", "JAN"), 0.001);
        
        assertEquals(0.822, stringSim.getSimilarity("cat", "car"), 0.001);
        
        assertEquals(0.989, stringSim.getSimilarity("http://movies.yahoo.com/browse/list/q", "http://movies.yahoo.com/browse/list/r"), 0.001);
        assertEquals(0.989, stringSim.getSimilarity("http://movies.yahoo.com/browse/list/r", "http://movies.yahoo.com/browse/list/q"), 0.001);
    }

}
