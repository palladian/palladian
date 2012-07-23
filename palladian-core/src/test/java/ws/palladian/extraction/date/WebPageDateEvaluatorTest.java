/**
 * 
 */
package ws.palladian.extraction.date;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.junit.Test;

import weka.classifiers.Classifier;
import weka.core.SerializationHelper;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.helper.Cache;
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
            WebPageDateEvaluator wpde = new WebPageDateEvaluator();
            wpde.setUrl(url);
            wpde.evaluate();
            assertEquals("2010-09-02", wpde.getBestRatedDate().getNormalizedDateString());

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
     * Test method for {@link ws.palladian.daterecognition.WebPageDateEvaluatorOld#getAllBestRatedDate()}.
     * @throws FileNotFoundException 
     */
    @Test
    public void testGetAllBestRatedDate() throws FileNotFoundException {
        String url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit2.htm");
        WebPageDateEvaluator wpde = new WebPageDateEvaluator();
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(1, wpde.getAllBestRatedDate().size());

        url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit1.htm");
        wpde.setUrl(url);
        wpde.evaluate();
        assertEquals(1, wpde.getAllBestRatedDate().size());

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

        // we need to load the model into the cache for the test case
        InputStream stream = WebPageDateEvaluatorTest.class.getResourceAsStream("/model/pubClassifierFinal.model");
        Classifier classifier = (Classifier) SerializationHelper.read(stream);
        Cache.getInstance().putDataObject(ContentDateRater.DATE_CLASSIFIER_IDENTIFIER, classifier);

        String url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit2.htm");
        WebPageDateEvaluator wpde = new WebPageDateEvaluator();
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
