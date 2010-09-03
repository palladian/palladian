package tud.iir.daterecognition;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.web.Crawler;
import tud.iir.web.CrawlerCallback;

public class testContentDateCrawler {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final Integer[] stats = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Crawler c = new Crawler();
        CrawlerCallback crawlerCallback = new CrawlerCallback() {

            @Override
            public void crawlerCallback(Document document) {
                synchronized (stats) {
                    stats[0]++;
                }
                String url = document.getDocumentURI();
                DateGetter dg = new DateGetter(url);
                dg.setAllFalse();
                dg.setTechHTMLContent(true);
                ArrayList<ExtractedDate> dates = dg.getDate();

                DateEvaluator de = new DateEvaluator();
                HashMap<ExtractedDate, Double> dateMap = de.evaluate(dates);

            }
        };
        File file = new File("E:\\_Uni\\_semester15\\Beleg\\eclipse workspace\\logger\\stats.txt");
        file.delete();
        file = new File("E:\\_Uni\\_semester15\\Beleg\\eclipse workspace\\logger\\ausgabe.txt");
        file.delete();
        c.addCrawlerCallback(crawlerCallback);

        c.setMaxThreads(500);
        c.startCrawl("http://techcrunch.com/2010/08/26/facebook-friend-lists/", false, true);
        // c.startCrawl("http://www.truthdig.com/arts_culture/item/20071108_mark_sarvas_on_the_hot_zone/", false, true);
        // c.startCrawl("http://www.nytimes.com/", false, true);
        // c.startCrawl("http://www.dmoz.org/", false, true);

    }
}
