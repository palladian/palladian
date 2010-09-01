package tud.iir.helper;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import tud.iir.daterecognition.DateEvaluator;
import tud.iir.daterecognition.DateEvaluatorHelper;
import tud.iir.daterecognition.DateGetter;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.knowledge.RegExp;
import tud.iir.web.Crawler;
import tud.iir.web.CrawlerCallback;

public class ArrayHelpterTest {
    @Test
    public void removeNullElementsTest() {
        ArrayList<String> array = new ArrayList<String>();
        String temp = null;
        array.add(temp);
        temp = "1";
        array.add(temp);
        temp = "2";
        array.add(temp);
        temp = null;
        array.add(temp);
        temp = "3";
        array.add(temp);
        temp = null;
        array.add(temp);
        temp = "4";
        array.add(temp);
        temp = null;
        array.add(temp);
        array = ArrayHelper.removeNullElements(array);
        assertEquals(4, array.size());
        for (int i = 0; i < array.size(); i++) {
            assertEquals(i + 1, Integer.parseInt(array.get(i)));
        }
    }

    @Ignore
    @Test
    public void testTest() {
        Crawler c = new Crawler();
        final ArrayList<ExtractedDate> dates = new ArrayList<ExtractedDate>();
        final ArrayList<Object> count = new ArrayList<Object>();
        final HashMap<ExtractedDate[], Double> checkedDates = new HashMap<ExtractedDate[], Double>();
        CrawlerCallback crawlerCallback = new CrawlerCallback() {

            @Override
            public void crawlerCallback(Document document) {
                String url = document.getDocumentURI();

                DateGetter dg = new DateGetter();
                dg.setAllFalse();
                dg.setTechURL(true);
                dg.setURL(url);
                ArrayList<ExtractedDate> temp = dg.getDate();

                for (int j = 0; j < temp.size(); j++) {
                    if (DateEvaluatorHelper.isDateInRange(temp.get(j))) {
                        synchronized (dates) {
                            dates.add(temp.get(j));
                        }
                        String format = temp.get(j).getFormat();
                        if (!format.equalsIgnoreCase(RegExp.DATE_URL[1])
                                && !format.equalsIgnoreCase(RegExp.DATE_URL_D[1])
                                && !format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
                            DateGetter dgContent = new DateGetter(temp.get(j).getUrl());
                            dgContent.setAllFalse();
                            dgContent.setTechHTMLContent(true);
                            ArrayList<ExtractedDate> content = dgContent.getDate();
                            DateEvaluator de = new DateEvaluator();
                            HashMap<ExtractedDate, Double> tempMap = de.evaluate(content);
                            tempMap = DateEvaluatorHelper.getHighestRate(tempMap);
                            ExtractedDate highestDate = null;
                            double rate = 0;
                            for (Entry<ExtractedDate, Double> e : tempMap.entrySet()) {
                                highestDate = e.getKey();
                                rate = e.getValue();
                                break;
                            }
                            synchronized (checkedDates) {
                                ExtractedDate[] checkedArray = { temp.get(j), highestDate };
                                checkedDates.put(checkedArray, rate);
                            }
                        }
                    }
                }
                synchronized (count) {
                    count.add(new Object());
                }

            }
        };
        c.addCrawlerCallback(crawlerCallback);

        c.setStopCount(5000);
        c.setMaxThreads(500);

        c.startCrawl("http://www.nytimes.com/", false, true);

        ArrayList<ExtractedDate> yyyy_mm_dd = DateArrayHelper.filterFormat(dates, RegExp.DATE_URL_D[1]);
        ArrayList<ExtractedDate> yyyy_mm = DateArrayHelper.filterFormat(dates, RegExp.DATE_URL[1]);
        ArrayList<ExtractedDate> yyyy_mm_dd_splitt = DateArrayHelper.filterFormat(dates, RegExp.DATE_URL_SPLIT[1]);
        double prozent1 = Math.round(Double.valueOf(yyyy_mm_dd.size()) / Double.valueOf(dates.size()) * 1000) / 10;
        double prozent2 = Math.round(Double.valueOf(yyyy_mm.size()) / Double.valueOf(dates.size()) * 1000) / 10;
        double prozent3 = Math.round(Double.valueOf(yyyy_mm_dd_splitt.size()) / Double.valueOf(dates.size()) * 1000) / 10;
        double prozent4 = Math.round((Double.valueOf(dates.size() - yyyy_mm_dd_splitt.size() - yyyy_mm_dd.size()
                - yyyy_mm.size()))
                / Double.valueOf(dates.size()) * 1000) / 10;
        ArrayList<ExtractedDate> onlyRemain = DateArrayHelper.removeFormat(dates, RegExp.DATE_URL_D[1]);
        onlyRemain = DateArrayHelper.removeFormat(onlyRemain, RegExp.DATE_URL[1]);

        int countGt80 = 0;
        int countIs100 = 0;
        int countHasContDate = 0;
        try {
            File output = new File("E:\\_Uni\\_semester15\\Beleg\\eclipse workspace\\logger\\ausgabe.txt");

            FileWriter outWriter = new FileWriter(output);
            BufferedWriter bw = new BufferedWriter(outWriter);

            for (Entry<ExtractedDate[], Double> e : checkedDates.entrySet()) {
                ExtractedDate[] dateArray = e.getKey();
                Double rate = e.getValue();
                bw.write("Rate: " + rate + " url: " + dateArray[0].getUrl() + "\n");
                bw.write(dateArray[0].toString() + "\n");
                if (rate != null && dateArray[0] != null && dateArray[1] != null) {
                    countHasContDate++;
                    if (rate == 1) {
                        countIs100++;
                    } else if (rate > 0.79) {
                        countGt80++;
                    }
                    bw.write(dateArray[1].toString() + "\n");
                }
                bw.write("---------------------------------------------------------------------------------------\n");
            }
            try {
                bw.write("count pages: " + count.size() + " found dates: " + dates.size() + "\n");
                bw.write("yyyy/mm/dd: " + yyyy_mm_dd.size() + "(" + prozent1 + "%)" + " yyyy/mm: " + yyyy_mm.size()
                        + "(" + prozent2 + "%)" + " yyyy/.../mm/dd: " + yyyy_mm_dd_splitt.size() + "(" + prozent3
                        + "%)" + " rest: "
                        + (dates.size() - yyyy_mm_dd_splitt.size() - yyyy_mm_dd.size() - yyyy_mm.size()) + "("
                        + prozent4 + "%)" + "\n");
                bw.write("Found Contentdates: " + countHasContDate + " rate over 79: " + countGt80 + " rate is 100: "
                        + countIs100 + "\n");
                bw.close();
            } catch (Exception e) {
                e.printStackTrace();
                bw.close();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
