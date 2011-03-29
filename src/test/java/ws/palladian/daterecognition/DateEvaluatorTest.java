package ws.palladian.daterecognition;

import java.util.HashMap;

import org.junit.Test;

import ws.palladian.control.AllTests;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateArrayHelper;

public class DateEvaluatorTest {

    @Test
    public final void testEvaluate() {
        if (AllTests.ALL_TESTS) {
            // DateGetter dg = new DateGetter("http://www.zeit.de/karriere/beruf/2010-08/karrierestrategien-frauen");
            // DateGetter dg = new DateGetter("data/test/webPages/dateExtraction/zeit2.htm");
            // DateGetter dg = new DateGetter(
            // "http://www.huffingtonpost.com/2010/09/08/mark-hurds-salary-at-orac_n_708676.html");
            String url;
            // url =
            url = "http://www.dailymail.co.uk/tvshowbiz/article-533432/Patrick-Swayze-smoking-despite-diagnosed-pancreatic-cancer.html";
            url = "http://www.nytimes.com/1990/04/10/science/in-alchemists-notes-clues-to-modern-chemistry.html?pagewanted=2";
            // "http://www.huffingtonpost.com/2010/09/07/ex-cia-electric-drill-contractor-training-operatives_n_708085.html";
            url = "http://classactiondefense.jmbm.com/2009/06/bofa_class_action_defense_case.html";
            url = "http://www.smh.com.au/articles/2006/11/21/1163871393154.html";
            //url = "http://www.tmcnet.com/news/2006/03/29/1517705.htm";
            WebPageDateEvaluator ae = new WebPageDateEvaluator();
            ae.setUrl(url);
            ae.evaluate();
            // DateArrayHelper.printDateArray(dates);
            DateArrayHelper.printDateArray(ae.getAllDates());
          
			ae.getBestRatedDate().getNormalizedDate();
		
            // assertEquals(dates.size(), dateMap.size());
        }
    }

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
        dates = DateEvaluator.deployMetaDates(metaDates, dates);
        System.out.println("________________________________");
        DateArrayHelper.printDateMap(dates);

    }

}
