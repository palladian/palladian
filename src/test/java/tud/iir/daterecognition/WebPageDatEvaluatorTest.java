/**
 * 
 */
package tud.iir.daterecognition;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tud.iir.control.AllTests;
import tud.iir.helper.DateArrayHelper;

/**
 * @author Martin Gregor
 * 
 */
public class WebPageDatEvaluatorTest {

    /**
     * Test method for {@link tud.iir.daterecognition.WebPageDateEvaluator#getBestRatedDate()}.
     */

    @Test
    public void testGetBestRatedDate() {
    	if(AllTests.ALL_TESTS){
	        String url = "data/test/webPages/dateExtraction/zeit2.htm";
	        WebPageDateEvaluator wpde = new WebPageDateEvaluator();
	        wpde.setUrl(url);
	        wpde.evaluate();
	        assertEquals("2010-09-02 06:00:00", wpde.getBestRatedDate().getNormalizedDateString());
	
	        url = "data/test/webPages/dateExtraction/zeit1.htm";
	        wpde.setUrl(url);
	        wpde.evaluate();
	        assertEquals("2010-08-22", wpde.getBestRatedDate().getNormalizedDateString());
	
	        url = "data/test/webPages/dateExtraction/kullin.htm";
	        wpde.setUrl(url);
	        wpde.evaluate();
	        assertEquals("2010-05-28 22:41", wpde.getBestRatedDate().getNormalizedDateString());
    	}
    }

    /**
     * Test method for {@link tud.iir.daterecognition.WebPageDateEvaluator#getAllBestRatedDate()}.
     */
    @Test
    public void testGetAllBestRatedDate() {
        String url = "data/test/webPages/dateExtraction/zeit2.htm";
        WebPageDateEvaluator wpde = new WebPageDateEvaluator();
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(2, wpde.getAllBestRatedDate().size());

        url = "data/test/webPages/dateExtraction/zeit1.htm";
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(5, wpde.getAllBestRatedDate().size());

        url = "data/test/webPages/dateExtraction/kullin.htm";
        wpde.setUrl(url);
        wpde.evaluate();
        DateArrayHelper.printDateArray(wpde.getAllDates());
        assertEquals(2, wpde.getAllBestRatedDate().size());
    }

    /**
     * Test method for {@link tud.iir.daterecognition.WebPageDateEvaluator#getAllDates()}.
     */
    @Test
    public void testGetAllDates() {
        String url = "data/test/webPages/dateExtraction/zeit2.htm";
        WebPageDateEvaluator wpde = new WebPageDateEvaluator();
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(9, wpde.getAllDates().size());

        url = "data/test/webPages/dateExtraction/zeit1.htm";
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(44, wpde.getAllDates().size());

        url = "data/test/webPages/dateExtraction/kullin.htm";
        wpde.setUrl(url);
        wpde.evaluate();
        DateArrayHelper.printDateArray(wpde.getAllDates());
        assertEquals(107, wpde.getAllDates().size());
    }

}
