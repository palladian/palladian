package tud.iir.daterecognition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.knowledge.RegExp;
import tud.iir.web.Crawler;
import tud.iir.web.CrawlerCallback;

public class testCrawler {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // crawlURLwithDate();
        // checkLinkSet();
        // checkURLs();
        evaluateURLwithDate();
    }

    public static void checkURLs() {
        File file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/urlsWithDate2.txt");
        HashMap<String, Integer> urlMap = new HashMap<String, Integer>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                urlMap.put(line, 0);
                if (line.equals("http://www.nytimes.com/interactive/2009/09/14/business/bailout-assessment.html")) {
                    System.out
                            .println("http://www.nytimes.com/interactive/2009/09/14/business/bailout-assessment.html");

                }
            }
            System.out.println(urlMap.size());
            fr.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();// TODO: handle exception
        }
        file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/urlsWithDate.txt");
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                urlMap.put(line, 0);

            }
            System.out.println(urlMap.size());
            fr.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();// TODO: handle exception
        }

        file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/urlsWithDate3.txt");
        try {
            FileWriter wr = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(wr);
            int i = 0;
            for (Entry<String, Integer> e : urlMap.entrySet()) {
                bw.write(e.getKey() + "\n");
                i++;
                if (e.getKey().equals("http://www.nytimes.com/interactive/2009/09/14/business/bailout-assessment.html")) {
                    System.out.println(e.getKey());
                    System.out.println("i:" + i);
                }
            }
            bw.close();
            wr.close();

        } catch (Exception e) {
            e.printStackTrace();// TODO: handle exception
        }

    }

    public static void checkLinkSet() {
        File file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/linkSet.txt");
        HashMap<String, Integer> urlMap = new HashMap<String, Integer>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                urlMap.put(line, 0);
            }
            fr.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();// TODO: handle exception
        }
        int i = 0;
        for (Entry<String, Integer> e : urlMap.entrySet()) {
            System.out.println(i++);
            DateGetter dg = new DateGetter(e.getKey());
            dg.setAllFalse();
            dg.setTechURL(true);
            ArrayList<URLDate> dates = dg.getDate();
            HashMap<URLDate, Double> dateMap = DateEvaluator.evaluateURLDate(dates);
            file = new File(
                    "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/urlsWithDate2.txt");
            try {
                FileWriter fw = new FileWriter(file, true);
                BufferedWriter bw = new BufferedWriter(fw);

                for (Entry<URLDate, Double> date : dateMap.entrySet()) {

                    if (date.getValue() == 1) {

                        bw.write(e.getKey() + "\n");
                        bw.close();
                        fw.close();

                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void crawlURLwithDate() {
        CrawlerCallback cb = new CrawlerCallback() {

            @Override
            public void crawlerCallback(Document document) {
                String url = document.getDocumentURI();
                DateGetter dg = new DateGetter(url);
                dg.setAllFalse();
                dg.setTechURL(true);

                ArrayList<URLDate> dates = dg.getDate();

                HashMap<URLDate, Double> dateMap = DateEvaluator.evaluateURLDate(dates);
                for (Entry<URLDate, Double> e : dateMap.entrySet()) {
                    if (e.getValue() == 1) {

                        File file = new File(
                                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/urlsWithDate2.txt");
                        synchronized (file) {
                            try {
                                FileWriter fw = new FileWriter(file, true);
                                BufferedWriter bw = new BufferedWriter(fw);
                                bw.write(url + "\n");
                                bw.close();
                                fw.close();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }

            }
        };

        Crawler c = new Crawler();
        c.setMaxThreads(500);
        c.addCrawlerCallback(cb);

        String url = "http://www.basicthinking.de/blog/";

        c.startCrawl(url, false, true);
    }

    // all: 4308 match: 3177 2010-09-02 rate: 1.0 <--> rate:1.0 2010-09-02 |highestdate: 1.0 isOtherdate: true
    // |Formats:: YYYY_MM_DD: 2803 same: 2031 YYYY.x.MM.DD: 25 same: 12 YYYY_MM: 1314 same: 1030 YYYY_MMMM_DD_URL: 32
    // same: 29 other: 134 same: 75
    public static Integer countSame = 0;
    public static Integer countAll = 0;

    static int countDATE_URL_D = 0;
    static int countDATE_URL_SPLIT = 0;
    static int countDATE_URL = 0;
    static int countDATE_URL_MMMM_D = 0;
    static int countOtherFormat = 0;
    static int samecountDATE_URL_D = 0;
    static int samecountDATE_URL_SPLIT = 0;
    static int samecountDATE_URL = 0;
    static int samecountDATE_URL_MMMM_D = 0;
    static int samecountOtherFormat = 0;

    public static void evaluateURLwithDate() {
        File file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/otherDateFound.txt");
        ArrayList<String> urls = new ArrayList<String>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                urls.add(line);
            }
            br.close();
            fr.close();
        } catch (Exception ex) {
            ex.printStackTrace();// TODO: handle exception
        }

        DateGetter dg = new DateGetter();
        dg.setTechArchive(false);
        dg.setTechReference(false);
        ArrayList<ExtractedDate> dates;
        DateEvaluator de = new DateEvaluator();
        HashMap<ExtractedDate, Double> evDates;
        HashMap<ExtractedDate, Double> urldate;
        ExtractedDate url;
        file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/statsURLOtherDate.txt");
        for (int i = 0; i < urls.size(); i++) {
            dg.setURL(urls.get(i));

            dates = dg.getDate();

            evDates = de.evaluate(dates);

            urldate = DateArrayHelper.filter(evDates, DateArrayHelper.FILTER_TECH_URL);
            url = DateArrayHelper.getFirstElement(urldate);
            if (url != null) {

                double urlRate = evDates.remove(url);
                double highestRate = 0;
                boolean otherIsHighestDate = false;
                Entry<ExtractedDate, Double> otherDate = null;
                Entry<ExtractedDate, Double>[] list = DateArrayHelper.orderHashMap(evDates, true);
                DateComparator dc = new DateComparator();
                ExtractedDate highestDate = null;
                for (int k = 0; k < list.length; k++) {
                    highestDate = list[0].getKey();
                    highestRate = list[0].getValue();
                    if (dc.compare(url, list[k].getKey(), url.getExactness()) == 0) {
                        if (list[k].getValue() == highestRate) {
                            otherIsHighestDate = true;
                        }
                        otherDate = list[k];
                        break;
                    }

                }
                try {

                    FileWriter fw = new FileWriter(file, true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    String format = url.getFormat();

                    if (format.equalsIgnoreCase(RegExp.DATE_URL_D[1])) {
                        countDATE_URL_D++;
                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
                        countDATE_URL_SPLIT++;

                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL[1])) {

                        countDATE_URL++;

                    } else if (format.equalsIgnoreCase(RegExp.DATE_URL_MMMM_D[1])) {

                        countDATE_URL_MMMM_D++;

                    } else {

                        countOtherFormat++;
                    }

                    countAll++;
                    if (otherIsHighestDate) {
                        countSame++;
                        if (format.equalsIgnoreCase(RegExp.DATE_URL_D[1])) {

                            samecountDATE_URL_D++;

                        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
                            samecountDATE_URL_SPLIT++;

                        } else if (format.equalsIgnoreCase(RegExp.DATE_URL[1])) {

                            samecountDATE_URL++;

                        } else if (format.equalsIgnoreCase(RegExp.DATE_URL_MMMM_D[1])) {

                            samecountDATE_URL_MMMM_D++;

                        } else {
                            samecountOtherFormat++;
                        }
                    } else {/*
                             * File fileNodateFound = new File(
                             * "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/otherDateFound.txt"
                             * );
                             * try {
                             * FileWriter nfw = new FileWriter(fileNodateFound, true);
                             * BufferedWriter nbw = new BufferedWriter(nfw);
                             * nbw.write(urls.get(i) + "\n");
                             * nbw.close();
                             * nfw.close();
                             * } catch (Exception e) {
                             * e.printStackTrace(); // TODO: handle exception
                             * }
                             */

                    }

                    String formatOutput1 = RegExp.DATE_URL_D[1] + ": " + countDATE_URL_D + " same: "
                            + samecountDATE_URL_D;
                    String formatOutput2 = RegExp.DATE_URL_SPLIT[1] + ": " + countDATE_URL_SPLIT + " same: "
                            + samecountDATE_URL_SPLIT;
                    String formatOutput3 = RegExp.DATE_URL[1] + ": " + countDATE_URL + " same: " + samecountDATE_URL;
                    String formatOutput4 = RegExp.DATE_URL_MMMM_D[1] + ": " + countDATE_URL_MMMM_D + " same: "
                            + samecountDATE_URL_MMMM_D;
                    String formatOutput5 = "other" + ": " + countOtherFormat + " same: " + samecountOtherFormat;

                    String formatOutput = " |Formats:: " + formatOutput1 + " " + formatOutput2 + " " + formatOutput3
                            + " " + formatOutput4 + " " + formatOutput5;

                    String otherDateValue = "";
                    String otherDateKey = "";
                    String highestDateKey = "";
                    String highestDateRate = "";
                    String otherDateFormat = "";
                    String highestDateFormat = "";

                    if (otherDate == null) {
                        /*
                         * File fileNodateFound = new File(
                         * "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/noOtherDateFound.txt"
                         * );
                         * try {
                         * FileWriter nfw = new FileWriter(fileNodateFound, true);
                         * BufferedWriter nbw = new BufferedWriter(nfw);
                         * nbw.write(urls.get(i) + "\n");
                         * nbw.close();
                         * nfw.close();
                         * } catch (Exception e) {
                         * e.printStackTrace(); // TODO: handle exception
                         * }
                         */
                    } else {
                        otherDateValue = String.valueOf(otherDate.getValue());
                        otherDateKey = otherDate.getKey().getDateString();
                        otherDateFormat = otherDate.getKey().getFormat();
                    }
                    if (highestDate != null) {
                        highestDateRate = String.valueOf(highestRate);
                        highestDateKey = highestDate.getDateString();
                        highestDateFormat = highestDate.getFormat();
                    }

                    System.out.println("all: " + countAll + " match: " + countSame + " " + url.getNormalizedDate()
                            + " rate: " + urlRate + "  <--> rate:" + highestDateRate + " " + highestDateKey + " "
                            + highestDateFormat + " sameother rate: " + otherDateValue + " " + otherDateKey + " "
                            + otherDateFormat + "\n" + urls.get(i) + "\n");

                    bw.write("all: " + countAll + " match: " + countSame + " " + url.getNormalizedDate() + " rate: "
                            + urlRate + "  <--> rate:" + highestDateRate + " " + highestDateKey + " "
                            + highestDateFormat + " sameother rate: " + otherDateValue + " " + otherDateKey + " "
                            + otherDateFormat + "\n" + urls.get(i) + "\n");

                    bw.close();
                    fw.close();

                } catch (Exception e) {
                    e.printStackTrace();// TODO: handle exception
                }

                /*
                 * System.out.println(url.getNormalizedDate() + " rate: " + urlRate + "  <--> rate:" +
                 * otherDate.getValue()
                 * + " " + otherDate.getKey().getNormalizedDate() + " |highestdate: " + highestRate + " isotherdate: "
                 * + otherIsHighestDate);
                 */

            }
        }

    }

    public static Integer countThreads = 0;

    public static final File file = new File(
            "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/statsURL.txt");

    public static synchronized void addStats(ExtractedDate url, double urlRate, Entry<ExtractedDate, Double> otherDate,
            double highestRate, boolean otherIsHighestDate) {

        try {
            synchronized (file) {

                FileWriter fw = new FileWriter(file, true);
                BufferedWriter bw = new BufferedWriter(fw);
                int temp;
                synchronized (countSame) {
                    if (otherIsHighestDate) {
                        countSame++;
                    }
                    temp = countSame;
                }
                int all;
                synchronized (countAll) {
                    countAll++;
                    all = countAll;
                }
                System.out.println("match: " + temp + url.getNormalizedDate() + " rate: " + urlRate + "  <--> rate:"
                        + otherDate.getValue() + " " + otherDate.getKey().getNormalizedDate() + " |highestdate: "
                        + highestRate + " isotherdate: " + otherIsHighestDate + "\n");
                bw.write("all: " + all + " match: " + temp + " " + url.getNormalizedDate() + " rate: " + urlRate
                        + "  <--> rate:" + otherDate.getValue() + " " + otherDate.getKey().getNormalizedDate()
                        + " |highestdate: " + highestRate + " isotherdate: " + otherIsHighestDate + "\n");
                bw.close();
                fw.close();
            }

        } catch (Exception e) {
            // TODO: handle exception
        }
        /*
         * System.out.println(url.getNormalizedDate() + " rate: " + urlRate + "  <--> rate:" + otherDate.getValue() +
         * " "
         * + otherDate.getKey().getNormalizedDate() + " |highestdate: " + highestRate + " isotherdate: "
         * + otherIsHighestDate);
         */
    }
}
