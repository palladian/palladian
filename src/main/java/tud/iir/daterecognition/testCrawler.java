package tud.iir.daterecognition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.daterecognition.technique.URLDateGetter;
import tud.iir.daterecognition.technique.UrlDateRater;
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
        // evaluateURLwithDate("url_einzeln.txt", "stats_url_output.txt");
        // EvalURL evalURL = new EvalURL("url_einzeln.txt", "stats_url_output.txt");
        // evalURL.start(1, 98);

        // addAllURLStats(1, 98, "stats_url_output.txt");

        // merge();
        evaluation("Evaluation-LinkSet.txt", "evaluationOutput.txt");

        // evaluateHTTP("linkSet.txt", "http_output.txt");

    }

    public static void addAllURLStats(int begin, int end, String input) {
        ArrayList<String> stats = new ArrayList<String>();
        for (int i = begin; i <= end; i++) {
            File file = new File(
                    "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/"
                            + input.replaceAll(".txt", i + ".txt"));
            try {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                String line = "";
                String lastLine = "";
                String preLastLine = "";
                int index = 1;
                while (line != null) {

                    preLastLine = lastLine;
                    lastLine = line;

                    line = br.readLine();
                    index++;

                }
                System.out.println(index);
                System.out.println(preLastLine + lastLine);
                stats.add(preLastLine + lastLine);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        // CountAll: 3 SameAll: 3
        // URL_D: 1 sameURL_D: 1 URL: 2 sameURL: 2 URL_MMMM: 0 sameURL_MMMM: 0
        // URL_split: 0 sameURL_split: 0 other: 0 sameOther: 0

        int countAll = 0;
        int sameAll = 0;

        int countURL_D = 0;
        int sameURL_D = 0;
        int countURL = 0;
        int sameURL = 0;
        int countURL_MMMM = 0;
        int sameURL_MMMM = 0;
        int countURL_split = 0;
        int sameURL_split = 0;
        int countOther = 0;
        int sameOther = 0;
        int temp;
        for (int i = 0; i < stats.size(); i++) {
            String line = stats.get(i);
            int countAllBegin = line.indexOf("CountAll: ");
            int sameAllBegin = line.indexOf("SameAll: ");
            int countURL_DBegin = line.indexOf("URL_D: ");
            int sameURL_DBegin = line.indexOf("sameURL_D: ");
            int countURLBegin = line.indexOf("URL: ");
            int sameURLBegin = line.indexOf("sameURL: ");
            int countURL_MMMMBegin = line.indexOf("URL_MMMM: ");
            int sameURL_MMMMBegin = line.indexOf("sameURL_MMMM: ");
            int countURL_splitBegin = line.indexOf("URL_split: ");
            int sameURL_splitBegin = line.indexOf("sameURL_split: ");
            int countOtherBegin = line.indexOf("other: ");
            int sameOtherBegin = line.indexOf("sameOther: ");

            temp = Integer.valueOf(line.substring(countAllBegin + ("CountAll: ").length(), sameAllBegin - 1));
            countAll += temp;
            temp = Integer.valueOf(line.substring(sameAllBegin + ("SameAll: ").length(), countURL_DBegin - 1));
            sameAll += temp;
            temp = Integer.valueOf(line.substring(countURL_DBegin + ("URL_D: ").length(), sameURL_DBegin - 1));
            countURL_D += temp;
            temp = Integer.valueOf(line.substring(sameURL_DBegin + ("sameURL_D: ").length(), countURLBegin - 1));
            sameURL_D += temp;
            temp = Integer.valueOf(line.substring(countURLBegin + ("URL: ").length(), sameURLBegin - 1));
            countURL += temp;
            temp = Integer.valueOf(line.substring(sameURLBegin + ("sameURL: ").length(), countURL_MMMMBegin - 1));
            sameURL += temp;
            temp = Integer.valueOf(line.substring(countURL_MMMMBegin + ("URL_MMMM: ").length(), sameURL_MMMMBegin - 1));
            countURL_MMMM += temp;
            temp = Integer.valueOf(line.substring(sameURL_MMMMBegin + ("sameURL_MMMM: ").length(),
                    countURL_splitBegin - 1));
            sameURL_MMMM += temp;
            temp = Integer.valueOf(line.substring(countURL_splitBegin + ("URL_split: ").length(),
                    sameURL_splitBegin - 1));
            countURL_split += temp;
            temp = Integer.valueOf(line.substring(sameURL_splitBegin + ("sameURL_split: ").length(),
                    countOtherBegin - 1));
            sameURL_split += temp;
            temp = Integer.valueOf(line.substring(countOtherBegin + ("other: ").length(), sameOtherBegin - 1));
            countOther += temp;
            temp = Integer.valueOf(line.substring(sameOtherBegin + ("sameOther: ").length()));
            sameOther += temp;
        }
        String outputString = "";
        outputString += "CountAll: " + countAll + " SameAll: " + sameAll + "\n";
        outputString += " URL_D: " + countURL_D + " sameURL_D: " + sameURL_D;
        outputString += " URL: " + countURL + " sameURL: " + sameURL;
        outputString += " URL_MMMM: " + countURL_MMMM + " sameURL_MMMM: " + sameURL_MMMM;
        outputString += " URL_split: " + countURL_split + " sameURL_split: " + sameURL_split;
        outputString += " other: " + countOther + " sameOther: " + sameOther;
        outputString += "\n";
        System.out.println(outputString);

    }

    public static void checkLinkSet() {
        File file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/urls_doppelt.txt");
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
        int count = 1;
        for (Entry<String, Integer> e : urlMap.entrySet()) {
            try {
                file = new File(
                        "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/url_einzeln"
                                + count + ".txt");

                FileWriter fw = new FileWriter(file, true);
                BufferedWriter bw = new BufferedWriter(fw);
                URLDate urlDate;

                URLDateGetter udg = new URLDateGetter();
                udg.setUrl(e.getKey());
                urlDate = udg.getFirstDate();

                if (urlDate != null) {
                    if (DateRaterHelper.isDateInRange(urlDate)) {
                        System.out.println(urlDate.getDateString());
                        bw.write(e.getKey() + "\n");
                    }
                }

                bw.close();
                fw.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.out.println(i++);
            if (i == 100) {
                count++;
                i = 0;
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

                UrlDateRater udr = new UrlDateRater();
                HashMap<URLDate, Double> dateMap = udr.rate(dates);
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

    public static void evaluateURLwithDate(String input, String output) {

        File file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/"
                        + input);
        ArrayList<String> urls = new ArrayList<String>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String line;

            while ((line = br.readLine()) != null) {
                urls.add(line);
            }
            System.out.println(urls.size());

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        DateGetter dg = new DateGetter();
        dg.setAllTrue();
        dg.setTechArchive(false);
        dg.setTechReference(false);

        DateRater de = new DateRater();

        ArrayList<ExtractedDate> dgDates;
        HashMap<ExtractedDate, Double> deDates;
        double highestRate;
        URLDate urlDate;
        String urlFormat;
        int urlFormatInt;
        String outputString;
        File outfile = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/"
                        + output);
        try {
            outfile.delete();
        } catch (Exception ex) {
        }

        for (int i = 0; i < urls.size(); i++) {
            System.out.println(i);
            URLDateGetter udg = new URLDateGetter();
            udg.setUrl(urls.get(i));
            urlDate = udg.getFirstDate();
            System.out.println(urls.get(i));
            urlFormat = urlDate.getFormat();
            dg.setURL(urls.get(i));
            dgDates = dg.getDate();

            deDates = de.rate(dgDates);

            deDates = DateArrayHelper.getExacterDates(deDates, urlDate.getExactness());
            highestRate = DateArrayHelper.getHighestRate(deDates);
            dgDates = DateArrayHelper.getRatedDates(deDates, highestRate);
            dgDates = DateArrayHelper.getSameDates(urlDate, dgDates, urlDate.getExactness());

            if (urlFormat.equalsIgnoreCase(RegExp.DATE_URL_D[1])) {
                urlFormatInt = 1;
            } else if (urlFormat.equalsIgnoreCase(RegExp.DATE_URL[1])) {
                urlFormatInt = 2;
            } else if (urlFormat.equalsIgnoreCase(RegExp.DATE_URL_MMMM_D[1])) {
                urlFormatInt = 3;
            } else if (urlFormat.equalsIgnoreCase(RegExp.DATE_URL_SPLIT[1])) {
                urlFormatInt = 4;
            } else {
                urlFormatInt = 5;
            }

            switch (urlFormatInt) {
                case 1:
                    countDATE_URL_D++;
                    break;
                case 2:
                    countDATE_URL++;
                    break;
                case 3:
                    countDATE_URL_MMMM_D++;
                    break;
                case 4:
                    countDATE_URL_SPLIT++;
                    break;
                case 5:
                    countOtherFormat++;
                    break;
            }
            countAll++;

            if (dgDates.size() > 0) {
                switch (urlFormatInt) {
                    case 1:
                        samecountDATE_URL_D++;
                        break;
                    case 2:
                        samecountDATE_URL++;
                        break;
                    case 3:
                        samecountDATE_URL_MMMM_D++;
                        break;
                    case 4:
                        samecountDATE_URL_SPLIT++;
                        break;
                    case 5:
                        samecountOtherFormat++;
                        break;
                }
                countSame++;
            }

            outputString = urls.get(i) + "\n";
            outputString += "CountAll: " + countAll + " SameAll: " + countSame + "\n";
            outputString += " URL_D: " + countDATE_URL_D + " sameURL_D: " + samecountDATE_URL_D;
            outputString += " URL: " + countDATE_URL + " sameURL: " + samecountDATE_URL;
            outputString += " URL_MMMM: " + countDATE_URL_MMMM_D + " sameURL_MMMM: " + samecountDATE_URL_MMMM_D;
            outputString += " URL_split: " + countDATE_URL_SPLIT + " sameURL_split: " + samecountDATE_URL_SPLIT;
            outputString += " other: " + countOtherFormat + " sameOther: " + samecountOtherFormat;
            outputString += "\n";

            try {
                FileWriter fw = new FileWriter(outfile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                System.out.println(outputString);
                bw.write(outputString);
                bw.close();
                fw.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    public static void evaluation(String input, String output) {

        HashMap<String, ExtractedDate[]> inputMap = new HashMap<String, ExtractedDate[]>();

        String filePath = "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/";
        File file = new File(filePath + input);

        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            int i = 0;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(" ");
                ExtractedDate[] dates = new ExtractedDate[2];
                dates[0] = DateGetterHelper.findDate(parts[1]);
                dates[1] = DateGetterHelper.findDate(parts[2]);
                System.out.println(i++ + " " + dates[0]);
                inputMap.put(parts[0], dates);
            }

            bufferedReader.close();
            fileReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        DateGetter dg = new DateGetter();
        dg.setAllTrue();
        dg.setTechReference(false);
        dg.setTechArchive(false);

        DateRater de = new DateRater();

        DateComparator dc = new DateComparator();

        ArrayList<ExtractedDate> datesList = new ArrayList<ExtractedDate>();
        HashMap<ExtractedDate, Double> datesMap = new HashMap<ExtractedDate, Double>();

        ArrayList<String> outputList = new ArrayList<String>();
        String outputString;

        for (Entry<String, ExtractedDate[]> e : inputMap.entrySet()) {
            System.out.println("begin");
            long begin = (new GregorianCalendar()).getTimeInMillis();
            dg.setURL(e.getKey());
            datesList = dg.getDate();
            datesMap = de.rate(datesList);

            datesMap = DateArrayHelper.getExacterDates(datesMap, DateComparator.STOP_DAY);
            double highestRate = DateRaterHelper.getHighestRateValue(datesMap);
            datesList = DateArrayHelper.getRatedDates(datesMap, highestRate);
            datesList = dc.getEqualDate(e.getValue()[0], datesList);
            outputString = e.getValue()[0].getNormalizedDate() + " " + e.getValue()[1].getNormalizedDate() + " ";
            String anmerkung = null;
            if (datesList.size() > 0) {
                outputString += datesList.get(0).getNormalizedDate(false);
            } else {
                outputString += DateArrayHelper.getFirstElement(DateRaterHelper.getHighestRate(datesMap))
                        .getNormalizedDate(false);
                anmerkung = "-";
            }

            if (anmerkung == null) {
                anmerkung = ".";
                if (dc.compare(e.getValue()[0], e.getValue()[1]) != 0) {
                    anmerkung = "+";
                }
            }
            outputString += " " + anmerkung;
            outputList.add(outputString + " " + e.getKey());
            System.out.println(outputString);
            long end = (new GregorianCalendar()).getTimeInMillis();
            System.out.println("end: " + (end - begin));
        }

        file = new File(filePath + output);
        try {
            FileWriter writer = new FileWriter(file);
            BufferedWriter bWriter = new BufferedWriter(writer);

            for (int i = 0; i < outputList.size(); i++) {
                bWriter.write(outputList.get(i) + "\n");
            }
            bWriter.close();
            writer.close();
        } catch (Exception ex) {

        }

    }

    public static void merge() {
        File file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/evaluationPages.txt");
        ArrayList<String> urls = new ArrayList<String>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                urls.add(line);
            }
        } catch (Exception ex) {

        }

        file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/dates.txt");
        ArrayList<String> dates = new ArrayList<String>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                dates.add(line);
            }
        } catch (Exception ex) {

        }

        file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/Evaluation-LinkSet.txt");

        try {
            FileWriter fr = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fr);

            for (int i = 0; i < urls.size(); i++) {
                bw.write((urls.get(i) + " " + dates.get(i) + "\n").replaceAll("  ", " "));
            }
            bw.close();
            fr.close();

        } catch (Exception ex) {

        }

    }

    public static void evaluateHTTP(String input, String output) {
        File file = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/"
                        + input);

        HashMap<String, Integer> urls = new HashMap<String, Integer>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                urls.put(line, 0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println(urls.size());

        DateGetter dg = new DateGetter();
        dg.setAllFalse();
        dg.setTechHTTP(true);

        DateRater de = new DateRater();

        ArrayList<ExtractedDate> dgDates = new ArrayList<ExtractedDate>();
        HashMap<ExtractedDate, Double> deDates = new HashMap<ExtractedDate, Double>();

        int countAll = 0;
        int countHTTP = 0;

        for (Entry<String, Integer> e : urls.entrySet()) {
            dg.setURL(e.getKey());
            dgDates = dg.getDate();

            deDates = de.rate(dgDates);
            deDates = DateArrayHelper.filter(deDates, DateArrayHelper.FILTER_TECH_HTTP_HEADER);
            countAll++;

            if (deDates.size() > 0) {
                double rate = DateArrayHelper.getHighestRate(deDates);
                if (rate > 0) {
                    countHTTP++;
                }
            }
            System.out.println(countAll + " - " + countHTTP);
        }

        File outputfile = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/"
                        + output);

        try {
            FileWriter fw = new FileWriter(outputfile);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write("All: " + countAll + " has HTTP: " + countHTTP);

            bw.close();
            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
