package tud.iir.daterecognition;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.DateComparator;
import tud.iir.knowledge.RegExp;
import tud.iir.web.Crawler;
import tud.iir.web.CrawlerCallback;

public class testCrawler {

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
                dg.setTechURL(true);
                ExtractedDate urlDate = (ExtractedDate) (dg.getDate()).get(0);

                if (urlDate != null) {
                    if (DateEvaluatorHelper.isDateInRange(urlDate)) {
                        synchronized (stats) {
                            stats[1]++;
                        }
                        ExtractedDate highestDate = null;

                        String format = urlDate.getFormat();
                        if (!format.equalsIgnoreCase(RegExp.DATE_URL[1])
                                && !format.equalsIgnoreCase(RegExp.DATE_URL_D[1])
                                && !format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
                            synchronized (stats) {
                                stats[2]++;
                            }
                            DateGetter dgContent = new DateGetter(url, document);
                            dgContent.setAllFalse();
                            dgContent.setTechHTMLContent(true);

                            ArrayList<ExtractedDate> content = dgContent.getDate();
                            DateComparator dc = new DateComparator();
                            ArrayList<ExtractedDate> sameDate = dc.getEqualDate(urlDate, content);
                            if (sameDate != null) {
                                DateEvaluator de = new DateEvaluator();
                                HashMap<ExtractedDate, Double> contentMap = de.evaluate(sameDate);
                                contentMap = DateEvaluatorHelper.getHighestRate(contentMap);

                                for (Entry<ExtractedDate, Double> e : contentMap.entrySet()) {
                                    highestDate = e.getKey();
                                    double rate = -1;
                                    if (highestDate != null) {
                                        rate = e.getValue();
                                        synchronized (stats) {
                                            stats[3]++;
                                            if (rate == 1) {
                                                stats[4]++;
                                            }
                                            if (rate < 1 && rate > 0.79) {
                                                stats[5]++;
                                            }
                                            if (rate < .80 && rate > .49) {
                                                stats[6]++;
                                            }
                                            if (rate < .50 && rate > .24) {
                                                stats[7]++;
                                            }
                                            if (rate < .25 && rate > 0) {
                                                stats[8]++;
                                            }
                                            if (rate == 0) {
                                                stats[9]++;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                            // boolean existHighestDate = highestDate != null;

                            /*
                             * try {
                             * String statsString;
                             * synchronized (stats) {
                             * statsString = "AllURLs: " + stats[0] + " AllDates: " + stats[1] + " Dates rest: "
                             * + stats[2] + " Dates content: " + stats[3] + " rate100: " + stats[4]
                             * + " rate80: " + stats[5] + " rate50: " + stats[6] + " rate25: " + stats[7]
                             * + " rate>0: " + stats[8] + " rate0: " + stats[9];
                             * }
                             * File file = new File(
                             * "E:\\_Uni\\_semester15\\Beleg\\eclipse workspace\\logger\\ausgabe.txt");
                             * synchronized (file) {
                             * FileWriter fwirter = new FileWriter(file, true);
                             * BufferedWriter bw = new BufferedWriter(fwirter);
                             * if (highestDate != null) {
                             * bw
                             * .write(urlDate.getDateString() + " --- " + highestDate.getDateString()
                             * + "\n");
                             * }
                             * bw.write(statsString + "\n");
                             * bw.close();
                             * }
                             * } catch (Exception e) {
                             * e.printStackTrace();
                             * }
                             */
                        }
                    }
                    System.out.println("AllURLs: " + stats[0] + " AllDates: " + stats[1] + " Dates rest: " + stats[2]
                            + " Dates content: " + stats[3] + " rate100: " + stats[4] + " rate80: " + stats[5]
                            + " rate50: " + stats[6] + " rate25: " + stats[7] + " rate>0: " + stats[8] + " rate0: "
                            + stats[9]);
                }
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
