package ws.palladian.extraction.date;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.control.AllTests;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.io.ResourceHelper;

public class DateGetterTest {
    






    @Test
    public void testGetContentDates() {
        if (AllTests.ALL_TESTS) {
            // final String url = "data/test/webPages/dateExtraction/kullin.htm";
            // String url =
            // "http://www.gatorsports.com/article/20100823/ARTICLES/100829802/1136?Title=Meyer-has-concerns-with-season-fast-approaching";
            // String url = "http://www.truthdig.com/arts_culture/item/20071108_mark_sarvas_on_the_hot_zone/";
            // String url =
            // "http://www.scifisquad.com/2010/05/21/fridays-sci-fi-tv-its-a-spy-game-on-stargate-universe?icid=sphere_wpcom_tagsidebar/";

            String url = "http://g4tv.com/games/pc/61502/star-wars-the-old-republic/index/";
            url = "data/evaluation/daterecognition/webpages/webpage_1292927985086.html";
            // String url =
            // "http://www.politicsdaily.com/2010/06/10/harry-reid-ads-tout-jobs-creation-spokesman-calls-sharron-angl/";
            if (AllTests.ALL_TESTS) {
                ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
                // date.addAll(DateGetterHelper
                // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
                // date.addAll(DateGetterHelper
                // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
                DateGetter dateGetter = new DateGetter(url);
                dateGetter.setTechHTMLContent(true);
                List<ExtractedDate> dates = dateGetter.getDate();
                date.addAll(dates);
                CollectionHelper.print(date);

            }
        }
    }

    // @Ignore
    @Test
    public void testGetContentDates2() throws FileNotFoundException {
        final String url = ResourceHelper.getResourcePath("/webpages/dateExtraction/Bangkok.htm");

            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html"));
            // date.addAll(DateGetterHelper
            // .getStructureDate("http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu"));
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setTechHTMLContent(true);
            List<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            // DateArrayHelper.printDateArray(date);
    }

    // @Ignore
    @Test
    public void testGetDate() {
        String url = "src/test/resources/webPages/dateExtraction/alltop.htm";
        // url = "http://www.zeit.de/2010/36/Wirtschaft-Konjunktur-Deutschland";
        //url = "http://www.abanet.org/antitrust/committees/intell_property/standardsettingresources.html";
        if (AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            List<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            // DateArrayHelper.printDateArray(date, DateType.ContentDate);
        }
    }

    @Test
    public void testGetDate2() {
        final String url = "http://www.friendfeed.com/share?title=Google+displays+incorrect+dates+from+news+sites&link=http://www.kullin.net/2010/05/google-displays-incorrect-dates-from-news-sites/";

        if (AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setTechHTMLContent(true);
            List<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            // DateArrayHelper.printDateArray(date);

        }
    }

}
