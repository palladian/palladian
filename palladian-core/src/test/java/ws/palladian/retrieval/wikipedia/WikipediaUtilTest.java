package ws.palladian.retrieval.wikipedia;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WikipediaUtilTest {

    @Test
    public void testCleanName() {
        assertEquals("Theater District", WikipediaUtil.cleanTitle("Theater District (San Francisco, California)"));
        assertEquals("Oregon", WikipediaUtil.cleanTitle("Oregon, Illinois"));
        assertEquals("West Seneca", WikipediaUtil.cleanTitle("West Seneca (town), New York"));
        assertEquals("Capital of the Cocos Islands", WikipediaUtil.cleanTitle("Capital of the Cocos (Keeling) Islands"));
    }

}
