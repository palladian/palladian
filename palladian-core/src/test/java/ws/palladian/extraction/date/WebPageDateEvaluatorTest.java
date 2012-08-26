/**
 * 
 */
package ws.palladian.extraction.date;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.io.ResourceHelper;

/**
 * @author Martin Gregor
 * @author Klemens Muthmann
 */
public class WebPageDateEvaluatorTest {

    /**
     * Test method for {@link ws.palladian.daterecognition.WebPageDateEvaluatorOld#getBestRatedDate()}.
     * @throws FileNotFoundException
     */
    @Test
    public void testGetBestRatedDate() throws FileNotFoundException {
            String url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit2.htm");
        WebPageDateEvaluator wpde = new WebPageDateEvaluator(PageDateType.PUBLISH);
            wpde.setUrl(url);
            wpde.evaluate();
            // FIXME assertEquals("2010-09-02", wpde.getBestRatedDate().getNormalizedDateString());

            url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit1.htm");
            wpde.setUrl(url);
            wpde.evaluate();
            assertEquals("2010-08-22", wpde.getBestRatedDate().getNormalizedDateString());

            // url = ResourceHelper.getResourcePath("/webPages/dateExtraction/kullin.htm");
            // wpde.setUrl(url);
            // wpde.evaluate();
            // assertEquals("2010-05-28 22:41", wpde.getBestRatedDate().getNormalizedDateString());
    }

    /**
     * @throws FileNotFoundException
     */
    @Test
    @Ignore // FIXME
    public void testGetAllBestRatedDate() throws FileNotFoundException {
        String url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit2.htm");
        WebPageDateEvaluator wpde = new WebPageDateEvaluator(PageDateType.PUBLISH);
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(1, wpde.getAllBestRatedDate().size());

        url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit1.htm");
        wpde.setUrl(url);
        wpde.evaluate();
        List<ExtractedDate> allBestRatedDate = wpde.getAllBestRatedDate();
        System.out.println(allBestRatedDate);
        assertEquals(1, allBestRatedDate.size());

        url = ResourceHelper.getResourcePath("/webPages/dateExtraction/kullin.htm");
        wpde.setUrl(url);
        wpde.evaluate();
        // System.out.println(wpde.getAllDates());
        assertEquals(1, wpde.getAllBestRatedDate().size());
        
        url = "http://www.spiegel.de/sport/formel1/0,1518,770789,00.html";
        wpde.setUrl(url);
        wpde.evaluate();
    }

    /**
     * Test method for {@link ws.palladian.daterecognition.WebPageDateEvaluatorOld#getAllDates()}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetAllDates() throws Exception {
        String url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit2.htm");
        WebPageDateEvaluator wpde = new WebPageDateEvaluator(PageDateType.PUBLISH);
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(2, wpde.getAllDates().size());

        url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit1.htm");
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(5, wpde.getAllDates().size());

        url = ResourceHelper.getResourcePath("/webPages/dateExtraction/kullin.htm");
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(12, wpde.getAllDates().size());
    }
}
