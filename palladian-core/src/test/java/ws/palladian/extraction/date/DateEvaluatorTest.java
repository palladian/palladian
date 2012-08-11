package ws.palladian.extraction.date;

import org.junit.Test;

import ws.palladian.control.AllTests;

public class DateEvaluatorTest {

    @Test
    public final void testEvaluate() {
        if (AllTests.ALL_TESTS) {
            // DateGetter dg = new DateGetter("http://www.zeit.de/karriere/beruf/2010-08/karrierestrategien-frauen");
            // DateGetter dg = new DateGetter("data/test/webPages/dateExtraction/zeit2.htm");
            // DateGetter dg = new DateGetter(
            // "http://www.huffingtonpost.com/2010/09/08/mark-hurds-salary-at-orac_n_708676.html");
            String url;
            WebPageDateEvaluator ae = new WebPageDateEvaluator();
//            Class<?> classResource = DateEvaluatorTest.class;
//            for(int index = 19; index <= 27; index++){
//            	System.out.println(index);
//	            url = classResource.getResource("/webPages/dateExtraction/tests/page" + index + ".htm").getFile();
////	            System.out.println(url);
//	            ae.setUrl(url);
//	            ae.evaluate();
////	            System.out.println(ae.getBestRatedDate().getNormalizedDate());
//            }
            url = "http://www.aegypten-online.de/aegypten-themen/blog/artikel/date/2011/05/03/title/nach-tod-von-osama-bin-laden-aegypten-erhoeht-sicherheit-fuer-touristen-00184.htm";
//            url= "http://www.lvz-online.de/ratgeber/content/30258214_mldg.html";
//            System.out.println(url);
            url= "http://www.drivechicago.com/reviews/review.aspx?review=173";
            url= "http://www.bbv-net.de/lokales/muenster/nachrichten/1541662_Dschihad_heisst_nicht_Terror.html";
            url="http://www.journal-frankfurt.de/?src=journal_news_einzel&rubrik=10&id=13070";
//            url="http://www.aegypten-online.de/aegypten-themen/blog/artikel/date/2011/05/03/title/nach-tod-von-osama-bin-laden-aegypten-erhoeht-sicherheit-fuer-touristen-00184.htm";
            ae.setUrl(url);
            ae.evaluate();
            System.out.println(ae.getBestRatedDate());
//            System.out.println(ae.getAllDates());
        }
    }

   

}
