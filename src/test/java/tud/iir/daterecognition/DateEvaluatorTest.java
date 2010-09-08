package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Test;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.DateArrayHelper;

public class DateEvaluatorTest {

    @Test
    public final void testEvaluate() {
        // DateGetter dg = new DateGetter("http://www.zeit.de/karriere/beruf/2010-08/karrierestrategien-frauen");
        // DateGetter dg = new DateGetter("data/test/webPages/dateExtraction/zeit2.htm");
        DateGetter dg = new DateGetter(
                "http://www.sueddeutsche.de/wissen/jahre-weltraumteleskop-huebsch-gemacht-hubble-1.935571");
        dg.setTechReference(false);
        dg.setTechArchive(false);
        ArrayList<ExtractedDate> dates = dg.getDate();

        DateEvaluator de = new DateEvaluator();
        HashMap<ExtractedDate, Double> dateMap = de.evaluate(dates);

        // DateArrayHelper.printDateArray(dates);
        Entry<ExtractedDate, Double>[] orderedDates = DateArrayHelper.orderHashMap(dateMap, true);
        DateArrayHelper.printDateMap(orderedDates);

        // assertEquals(dates.size(), dateMap.size());
    }

    @Test
    public final void testEvaluateGroup() {
    }

    @Test
    public final void testEvaluateStructDate() {
    }

    @Test
    public final void testEvaluateHeadDate() {
    }

    @Test
    public final void testEvaluateHTTPDate() {
    }

    @Test
    public final void testEvaluateURLDate() {
    }

    @Test
    public final void testEvaluateContentDate() {
    }

    @Test
    public final void testEvaluateContentDateByNumber() {
    }

}
