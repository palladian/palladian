/**
 * 
 */
package ws.palladian.extraction.date;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * @author Martin Gregor
 * @author Klemens Muthmann
 */
public class WebPageDateEvaluatorTest {
    
    private final DocumentParser htmlParser = ParserFactory.createHtmlParser();

    @Test
    public void testGetBestRatedDate() throws FileNotFoundException, ParserException {
        File file = ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit2.htm");
        Document document = htmlParser.parse(file);
        RatedDate<ExtractedDate> date = WebPageDateEvaluator.getBestDate(document, PageDateType.PUBLISH);
        // FIXME assertEquals("2010-09-02", wpde.getBestRatedDate().getNormalizedDateString());
        
        file = ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit1.htm");
        document = htmlParser.parse(file);
        date = WebPageDateEvaluator.getBestDate(document, PageDateType.PUBLISH);
        assertEquals("2010-08-22", date.getDate().getNormalizedDateString());

        // url = ResourceHelper.getResourcePath("/webPages/dateExtraction/kullin.htm");
        // wpde.setUrl(url);
        // wpde.evaluate();
        // assertEquals("2010-05-28 22:41", wpde.getBestRatedDate().getNormalizedDateString());
    }

//    @Test
//    @Ignore
//    // FIXME
//    public void testGetAllBestRatedDate() throws FileNotFoundException {
//        File file = ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit2.htm");
//        Document document = htmlParser.parse(file);
//        assertEquals(1, wpde.getAllBestRatedDate().size());
//
//        url = ResourceHelper.getResourcePath("/webPages/dateExtraction/zeit1.htm");
//        wpde.setUrl(url);
//        wpde.evaluate();
//        List<ExtractedDate> allBestRatedDate = wpde.getAllBestRatedDate();
//        System.out.println(allBestRatedDate);
//        assertEquals(1, allBestRatedDate.size());
//
//        url = ResourceHelper.getResourcePath("/webPages/dateExtraction/kullin.htm");
//        wpde.setUrl(url);
//        wpde.evaluate();
//        // System.out.println(wpde.getAllDates());
//        assertEquals(1, wpde.getAllBestRatedDate().size());
//
//        url = "http://www.spiegel.de/sport/formel1/0,1518,770789,00.html";
//        wpde.setUrl(url);
//        wpde.evaluate();
//    }

    @Test
    public void testGetAllDates() throws Exception {
        File file = ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit2.htm");
        Document document = htmlParser.parse(file);
        List<RatedDate<ExtractedDate>> dates = WebPageDateEvaluator.getDates(document, PageDateType.PUBLISH);
        assertEquals(2, dates.size());

        file = ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit1.htm");
        document = htmlParser.parse(file);
        dates = WebPageDateEvaluator.getDates(document, PageDateType.PUBLISH);
        assertEquals(5, dates.size());

        file = ResourceHelper.getResourceFile("/webPages/dateExtraction/kullin.htm");
        document = htmlParser.parse(file);
        dates = WebPageDateEvaluator.getDates(document, PageDateType.PUBLISH);
        assertEquals(12, dates.size());
    }

//    @Test
//    public final void testEvaluate() {
//        if (AllTests.ALL_TESTS) {
//            // DateGetter dg = new DateGetter("http://www.zeit.de/karriere/beruf/2010-08/karrierestrategien-frauen");
//            // DateGetter dg = new DateGetter("data/test/webPages/dateExtraction/zeit2.htm");
//            // DateGetter dg = new DateGetter(
//            // "http://www.huffingtonpost.com/2010/09/08/mark-hurds-salary-at-orac_n_708676.html");
//            String url;
//            WebPageDateEvaluator ae = new WebPageDateEvaluator();
//            // Class<?> classResource = DateEvaluatorTest.class;
//            // for(int index = 19; index <= 27; index++){
//            // System.out.println(index);
//            // url = classResource.getResource("/webPages/dateExtraction/tests/page" + index + ".htm").getFile();
//            // // System.out.println(url);
//            // ae.setUrl(url);
//            // ae.evaluate();
//            // // System.out.println(ae.getBestRatedDate().getNormalizedDate());
//            // }
//            url = "http://www.aegypten-online.de/aegypten-themen/blog/artikel/date/2011/05/03/title/nach-tod-von-osama-bin-laden-aegypten-erhoeht-sicherheit-fuer-touristen-00184.htm";
//            // url= "http://www.lvz-online.de/ratgeber/content/30258214_mldg.html";
//            // System.out.println(url);
//            url = "http://www.drivechicago.com/reviews/review.aspx?review=173";
//            url = "http://www.bbv-net.de/lokales/muenster/nachrichten/1541662_Dschihad_heisst_nicht_Terror.html";
//            url = "http://www.journal-frankfurt.de/?src=journal_news_einzel&rubrik=10&id=13070";
//            // url="http://www.aegypten-online.de/aegypten-themen/blog/artikel/date/2011/05/03/title/nach-tod-von-osama-bin-laden-aegypten-erhoeht-sicherheit-fuer-touristen-00184.htm";
//            ae.setUrl(url);
//            ae.evaluate();
//            System.out.println(ae.getBestRatedDate());
//            // System.out.println(ae.getAllDates());
//        }
//    }
    
//    @Test
//    public void testGetContentDates() {
//        if (AllTests.ALL_TESTS) {
//            // final String url = "data/test/webPages/dateExtraction/kullin.htm";
//            // String url =
//            // "http://www.gatorsports.com/article/20100823/ARTICLES/100829802/1136?Title=Meyer-has-concerns-with-season-fast-approaching";
//            // String url = "http://www.truthdig.com/arts_culture/item/20071108_mark_sarvas_on_the_hot_zone/";
//            // String url =
//            // "http://www.scifisquad.com/2010/05/21/fridays-sci-fi-tv-its-a-spy-game-on-stargate-universe?icid=sphere_wpcom_tagsidebar/";
//
//            String url = "http://g4tv.com/games/pc/61502/star-wars-the-old-republic/index/";
//            url = "data/evaluation/daterecognition/webpages/webpage_1292927985086.html";
//            // String url =
//            // "http://www.politicsdaily.com/2010/06/10/harry-reid-ads-tout-jobs-creation-spokesman-calls-sharron-angl/";
//            if (AllTests.ALL_TESTS) {
//                ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
//                // date.addAll(DateGetterHelper
//                // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
//                // date.addAll(DateGetterHelper
//                // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
//                DateGetter dateGetter = new DateGetter(url);
//                List<ExtractedDate> dates = dateGetter.getDate();
//                date.addAll(dates);
//                CollectionHelper.print(date);
//
//            }
//        }
//    }
//
//    // @Ignore
//    @Test
//    public void testGetContentDates2() throws FileNotFoundException {
//        final String url = ResourceHelper.getResourcePath("/webPages/dateExtraction/Bangkok.htm");
//
//            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
//            // date.addAll(DateGetterHelper
//            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
//            // date.addAll(DateGetterHelper
//            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
//            DateGetter dateGetter = new DateGetter(url);
//            List<ExtractedDate> dates = dateGetter.getDate();
//            date.addAll(dates);
//            // DateArrayHelper.printDateArray(date);
//    }
//
//    // @Ignore
//    @Test
//    public void testGetDate() {
//        String url = "src/test/resources/webPages/dateExtraction/alltop.htm";
//        // url = "http://www.zeit.de/2010/36/Wirtschaft-Konjunktur-Deutschland";
//        //url = "http://www.abanet.org/antitrust/committees/intell_property/standardsettingresources.html";
//        if (AllTests.ALL_TESTS) {
//            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
//            DateGetter dateGetter = new DateGetter(url);
//            List<ExtractedDate> dates = dateGetter.getDate();
//            date.addAll(dates);
//            // DateArrayHelper.printDateArray(date, DateType.ContentDate);
//        }
//    }
//
//    @Test
//    public void testGetDate2() {
//        final String url = "http://www.friendfeed.com/share?title=Google+displays+incorrect+dates+from+news+sites&link=http://www.kullin.net/2010/05/google-displays-incorrect-dates-from-news-sites/";
//
//        if (AllTests.ALL_TESTS) {
//            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
//            DateGetter dateGetter = new DateGetter(url);
//            List<ExtractedDate> dates = dateGetter.getDate();
//            date.addAll(dates);
//            // DateArrayHelper.printDateArray(date);
//
//        }
//    }

}
