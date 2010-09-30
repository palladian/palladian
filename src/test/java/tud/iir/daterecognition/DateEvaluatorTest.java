package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.junit.Test;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.knowledge.RegExp;

public class DateEvaluatorTest {

    @Test
    public final void testEvaluate() {
        // DateGetter dg = new DateGetter("http://www.zeit.de/karriere/beruf/2010-08/karrierestrategien-frauen");
        // DateGetter dg = new DateGetter("data/test/webPages/dateExtraction/zeit2.htm");
        // DateGetter dg = new DateGetter(
        // "http://www.huffingtonpost.com/2010/09/08/mark-hurds-salary-at-orac_n_708676.html");
        String url;
        // url =
        url = "http://www.xbitlabs.com/news/mobile/display/20071129151636_Apple_to_Release_iPhone_3G_in_2008_AT_T_Executive.html";
        // "http://www.huffingtonpost.com/2010/09/07/ex-cia-electric-drill-contractor-training-operatives_n_708085.html";
        DateGetter dg = new DateGetter(url);
        dg.setTechReference(false);
        dg.setTechArchive(false);

        ArrayList<ExtractedDate> dates = dg.getDate();

        // DateEvaluator de = new DateEvaluator();
        DateRater de = new DateRater(url, false);
        HashMap<ExtractedDate, Double> dateMap = de.rate(dates);

        // DateArrayHelper.printDateArray(dates);
        Entry<ExtractedDate, Double>[] orderedDates = DateArrayHelper.orderHashMap(dateMap, true);
        DateArrayHelper.printDateMap(orderedDates);

        // assertEquals(dates.size(), dateMap.size());
    }

    @Ignore
    @Test
    public final void testDeployMetaDates() {
        ExtractedDate meta1 = new ExtractedDate("2010-07-02 22:15", RegExp.DATE_ISO8601_YMD_T[1]);
        ExtractedDate meta2 = new ExtractedDate("2010-07-02 22:15", RegExp.DATE_ISO8601_YMD_T[1]);
        ExtractedDate meta3 = new ExtractedDate("2010-07-02 23:00", RegExp.DATE_ISO8601_YMD_T[1]);
        ExtractedDate meta4 = new ExtractedDate("2010-07-04", RegExp.DATE_ISO8601_YMD[1]);

        ExtractedDate date1 = new ExtractedDate("2010-07-02 22:15", RegExp.DATE_ISO8601_YMD_T[1]);
        ExtractedDate date2 = new ExtractedDate("2010-07-02", RegExp.DATE_ISO8601_YMD[1]);
        ExtractedDate date3 = new ExtractedDate("2010-07-04", RegExp.DATE_ISO8601_YMD[1]);
        ExtractedDate date4 = new ExtractedDate("2010-07-02", RegExp.DATE_ISO8601_YMD[1]);
        ExtractedDate date5 = new ExtractedDate("2010-07-02 23:15", RegExp.DATE_ISO8601_YMD_T[1]);

        HashMap<ExtractedDate, Double> metaDates = new HashMap<ExtractedDate, Double>();
        HashMap<ExtractedDate, Double> dates = new HashMap<ExtractedDate, Double>();

        metaDates.put(meta1, 1.0);
        metaDates.put(meta2, 0.8);
        metaDates.put(meta3, 0.7);
        metaDates.put(meta4, 0.0);

        dates.put(date1, 0.0);
        dates.put(date2, 0.0);
        dates.put(date3, 0.4);
        dates.put(date4, 0.8);
        dates.put(date5, 0.0);

        DateArrayHelper.printDateMap(dates);
        dates = DateRater.deployMetaDates(metaDates, dates);
        System.out.println("________________________________");
        DateArrayHelper.printDateMap(dates);

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
