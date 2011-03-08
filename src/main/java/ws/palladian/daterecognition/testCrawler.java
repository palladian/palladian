package ws.palladian.daterecognition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.URLDate;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.daterecognition.technique.URLDateGetter;
import ws.palladian.daterecognition.technique.UrlDateRater;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;
import ws.palladian.helper.date.RatedDateComparator;
import ws.palladian.web.Crawler;
import ws.palladian.web.CrawlerCallback;

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
        // evaluation("googleDate-links.txt", "evaluationOutput5.txt");
        // evaluation2("noGoogleDate.txt", "noGoogleDateOutput2.txt");
        // compareOutputs("evaluationOutput4.txt", "evaluationOutput5.txt", "compare.txt");
        compareOutputs("noGoogleDateOutput.txt", "noGoogleDateOutput2.txt", "compare.txt");

        // evaluateHTTP("linkSet.txt", "http_output.txt");
        // orderEvalSet("Evaluation-LinkSet.txt", "googleDate-links.txt");
        // splittOutput("evaluationOutput5.txt", "splitt");
        // splittOutput("noGoogleDateOutput2.txt", "splitt");
        // countStrings("6.txt");
    }

    public static void countStrings(String input) {
        try {
            String pfad = "data/test/dateExtraction/";
            File file = new File(pfad + input);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            HashMap<String, Integer> count = new HashMap<String, Integer>();

            while ((line = br.readLine()) != null) {
                Integer value = count.get(line);
                if (value == null) {
                    value = 1;
                } else {
                    value++;
                }
                count.put(line, value);
            }

            for (Entry<String, Integer> e : count.entrySet()) {
                System.out.println(e.getKey() + " " + e.getValue());
            }

        } catch (IOException e) {
            // TODO: handle exception
        }

    }

    public static void compareOutputs(String input1, String input2, String output) {
        String pfad = "data/test/dateExtraction/";
        HashMap<String, String[]> map = new HashMap<String, String[]>();

        try {
            File file = new File(pfad + input1);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            int i = 0;

            int myError = -1;
            int url = -1;
            while ((line = br.readLine()) != null) {
                String[] temp = line.split(" ");
                if (i == 0) {
                    for (int k = 0; k < temp.length; k++) {
                        if (temp[k].equalsIgnoreCase("myerror")) {
                            myError = k;
                        } else if (temp[k].equalsIgnoreCase("url")) {
                            url = k;

                        }
                    }
                    if (myError == -1 || url == -1) {
                        break;
                    }
                } else {
                    String[] annotation = new String[2];
                    annotation[0] = temp[myError];
                    // System.out.print(annotation[0]);
                    map.put(temp[url], annotation);
                    System.out.print(map.get(temp[url])[0]);
                }
                i++;
            }
            br.close();
            fr.close();

            file = new File(pfad + input2);
            fr = new FileReader(file);
            br = new BufferedReader(fr);

            i = 0;
            myError = -1;
            url = -1;
            System.out.println();
            line = "";
            while ((line = br.readLine()) != null) {
                String[] temp = line.split(" ");
                if (i == 0) {
                    for (int k = 0; k < temp.length; k++) {
                        if (temp[k].equalsIgnoreCase("myerror")) {
                            myError = k;
                        } else if (temp[k].equalsIgnoreCase("url")) {
                            url = k;
                        }
                    }
                    System.out.println(myError + " " + url);
                    if (myError == -1 || url == -1) {
                        break;
                    }
                } else {
                    String[] annotation = new String[2];
                    // System.out.println(temp[url]);

                    if (map.get(temp[url]) == null) {
                        annotation[0] = ".";
                    } else {
                        annotation[0] = map.get(temp[url])[0];
                    }
                    System.out.print(annotation[0]);
                    annotation[1] = temp[myError];
                    // System.out.println(annotation[0] + " " + annotation[1]);
                    map.put(temp[url], annotation);
                }
                i++;
            }
            System.out.println();
            br.close();
            fr.close();

            file = new File(pfad + output);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);

            for (Entry<String, String[]> e : map.entrySet()) {
                String change = ".";
                if (!e.getValue()[0].equals(e.getValue()[1])) {
                    if (e.getValue()[0].equals("-")) {
                        change = "+";
                    } else if (e.getValue()[0].equals(".")) {
                        if (e.getValue()[1].equals("-")) {
                            change = "-";
                        } else {
                            change = "+";
                        }
                    } else {
                        change = "-";
                    }
                }
                line = e.getValue()[0] + " | " + e.getValue()[1] + " | " + change + " | " + e.getKey();
                System.out.println(line);
                bw.write(line + "\n");
            }
            bw.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
            // TODO: handle exception
        }

    }

    public static void splittOutput(String input, String output) {
        ArrayList<String> webDate = new ArrayList<String>();
        ArrayList<String> googleDate = new ArrayList<String>();
        ArrayList<String> myDate = new ArrayList<String>();
        ArrayList<String> googleAnno = new ArrayList<String>();
        ArrayList<String> myAnno = new ArrayList<String>();
        ArrayList<String> url = new ArrayList<String>();
        ArrayList<String> technique = new ArrayList<String>();
        String pfad = "data/test/dateExtraction/";
        try {
            File file = new File(pfad + input);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String line;
            int i = 0;
            int pageDateIndex = -1;
            int googleDateIndex = -1;
            int myDateIndex = -1;
            int googleErrorIndex = -1;
            int myErrorIndex = -1;
            int urlIndex = -1;
            int techIndex = -1;

            while ((line = br.readLine()) != null) {
                if (i == 0) {
                    String[] parts = line.split(" ");
                    for (int k = 0; k < parts.length; k++) {
                        if (parts[k].equalsIgnoreCase("pagedate")) {
                            pageDateIndex = k;
                        } else if (parts[k].equalsIgnoreCase("googeldate")) {
                            googleDateIndex = k;
                        } else if (parts[k].equalsIgnoreCase("mydate")) {
                            myDateIndex = k;
                        } else if (parts[k].equalsIgnoreCase("googleerror")) {
                            googleErrorIndex = k;
                        } else if (parts[k].equalsIgnoreCase("myerror")) {
                            myErrorIndex = k;
                        } else if (parts[k].equalsIgnoreCase("url")) {
                            urlIndex = k;
                        } else if (parts[k].equalsIgnoreCase("technique")) {
                            techIndex = k;
                        }
                    }
                } else {
                    String[] parts = line.split(" ");
                    if (pageDateIndex != -1)
                        webDate.add(parts[pageDateIndex]);
                    if (googleDateIndex != -1)
                        googleDate.add(parts[googleDateIndex]);
                    if (myDateIndex != -1)
                        myDate.add(parts[myDateIndex]);
                    if (googleErrorIndex != -1)
                        googleAnno.add(parts[googleErrorIndex]);
                    if (myErrorIndex != -1)
                        myAnno.add(parts[myErrorIndex]);
                    if (urlIndex != -1)
                        url.add(parts[urlIndex]);
                    if (techIndex != -1)
                        technique.add(parts[techIndex]);
                    System.out.println(parts[urlIndex]);
                }
                i++;
            }
            ArrayList<ArrayList<String>> all = new ArrayList<ArrayList<String>>();
            all.add(webDate);
            all.add(googleDate);
            all.add(myDate);
            all.add(googleAnno);
            all.add(myAnno);
            all.add(url);
            all.add(technique);

            for (i = 0; i < all.size(); i++) {
                file = new File(pfad + i + ".txt");
                FileWriter fw = new FileWriter(file);
                BufferedWriter bw = new BufferedWriter(fw);
                for (int k = 0; k < all.get(i).size(); k++) {
                    String t = "";
                    if (i == 5) {
                        t = k + 1 + " ";
                    }
                    bw.write(t + all.get(i).get(k) + " \n");
                }
                bw.close();
                fw.close();

            }
            br.close();
            fr.close();
            
        }  catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    public static void orderEvalSet(String input, String output) {
        ArrayList<String[]> list = new ArrayList<String[]>();
        String filePath = "data/test/dateExtraction/";
        File file = new File(filePath + input);

        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(" ");
                list.add(parts);
            }

            bufferedReader.close();
            fileReader.close();

            file = new File(filePath + output);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            for (int i = 0; i < list.size(); i++) {
                String[] parts = list.get(i);
                bw.write(parts[1] + " " + parts[2] + " " + parts[0] + "\n");
            }
            bw.close();
            fw.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void addAllURLStats(int begin, int end, String input) {
        ArrayList<String> stats = new ArrayList<String>();
        for (int i = begin; i <= end; i++) {
            File file = new File(
                    "data/test/webPages/dateExtraction/tests/linkSet/"
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
                
                br.close();
                fr.close();
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
                "data/test/webPages/dateExtraction/tests/linkSet/urls_doppelt.txt");
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
                        "data/test/webPages/dateExtraction/tests/linkSet/url_einzeln"
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

                UrlDateRater udr = new UrlDateRater(PageDateType.publish);
                HashMap<URLDate, Double> dateMap = udr.rate(dates);
                for (Entry<URLDate, Double> e : dateMap.entrySet()) {
                    if (e.getValue() == 1) {

                        File file = new File(
                                "data/test/webPages/dateExtraction/tests/linkSet/urlsWithDate2.txt");
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
                "data/test/webPages/dateExtraction/tests/linkSet/"
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
            
            br.close();
            fr.close();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        DateGetter dg = new DateGetter();
        dg.setAllTrue();
        dg.setTechArchive(false);
        dg.setTechReference(false);

        DateEvaluator de = new DateEvaluator(PageDateType.publish);

        ArrayList<ExtractedDate> dgDates;
        HashMap<ExtractedDate, Double> deDates;
        double highestRate;
        URLDate urlDate;
        String urlFormat;
        int urlFormatInt;
        String outputString;
        File outfile = new File(
                "data/test/webPages/dateExtraction/tests/linkSet/"
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

        String filePath = "data/test/dateExtraction/";
        File file = new File(filePath + input);

        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            int i = 0;
            while ((line = bufferedReader.readLine()) != null) {
                if (i > 0) {
                    String[] parts = line.split(" ");
                    ExtractedDate[] dates = new ExtractedDate[2];
                    dates[0] = DateGetterHelper.findDate(parts[0]);
                    dates[1] = DateGetterHelper.findDate(parts[1]);

                    System.out.println(i + " " + dates[0]);
                    inputMap.put(parts[2], dates);
                }
                i++;
            }

            bufferedReader.close();
            fileReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        WebPageDateEvaluator ae = new WebPageDateEvaluator();

        DateComparator dc = new DateComparator();


        ArrayList<String> outputList = new ArrayList<String>();
        String outputString;

        for (Entry<String, ExtractedDate[]> e : inputMap.entrySet()) {
            System.out.println("begin");
            long begin = (new GregorianCalendar()).getTimeInMillis();
            ae.setUrl(e.getKey());
            ae.evaluate();
            ArrayList<ExtractedDate> myDates = ae.getAllBestRatedDate(true);
            ArrayList<ExtractedDate> sameDates = DateArrayHelper.getSameDates(e.getValue()[0], myDates,
                    DateComparator.STOP_DAY);
            ExtractedDate sameDate;

            outputString = e.getValue()[0].getNormalizedDateString() + " | " + e.getValue()[1].getNormalizedDateString() + " | ";

            if (sameDates != null && sameDates.size() > 0) {
                sameDate = sameDates.get(0);
                outputString += sameDate.getNormalizedDate(false);
            } else {
                RatedDateComparator<ExtractedDate> rdc = new RatedDateComparator<ExtractedDate>();
                Collections.sort(myDates, rdc);
                sameDate = myDates.get(0);
                outputString += sameDate.getNormalizedDate(false);
            }

            String googleAnnotation = ".";
            if (dc.compare(e.getValue()[0], e.getValue()[1], DateComparator.STOP_DAY) != 0) {
                googleAnnotation = "-";
            }

            String myAnnotation = ".";
            if (dc.compare(e.getValue()[0], sameDate, DateComparator.STOP_DAY) != 0) {
                myAnnotation = "-";
            } else if (dc.compare(e.getValue()[1], sameDate, DateComparator.STOP_DAY) != 0) {
                myAnnotation = "+";
            }

            outputString += " | " + googleAnnotation + " | " + myAnnotation;
            outputList.add(outputString + " | " + sameDate.getType() + " | " + e.getKey());
            System.out.println(outputString);
            long end = (new GregorianCalendar()).getTimeInMillis();
            System.out.println("end: " + (end - begin));
        }

        file = new File(filePath + output);
        try {
            FileWriter writer = new FileWriter(file);
            BufferedWriter bWriter = new BufferedWriter(writer);

            bWriter.write("PageDate | GoogleDate | MyDate | GoogleError | MyError | Technique | URL" + "\n");

            for (int i = 0; i < outputList.size(); i++) {
                bWriter.write(outputList.get(i) + "\n");
            }
            bWriter.close();
            writer.close();
        } catch (Exception ex) {

        }

    }

    public static void evaluation2(String input, String output) {

        HashMap<String, ExtractedDate> inputMap = new HashMap<String, ExtractedDate>();

        String filePath = "data/test/dateExtraction/";
        File file = new File(filePath + input);

        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            int i = 0;
            while ((line = bufferedReader.readLine()) != null) {
                if (i > 0) {
                    String[] parts = line.split(" ");

                    System.out.println(i + " " + parts[1]);
                    inputMap.put(parts[1], DateGetterHelper.findALLDates(parts[0]).get(0));
                }
                i++;
            }

            bufferedReader.close();
            fileReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        WebPageDateEvaluator ae = new WebPageDateEvaluator();

        DateComparator dc = new DateComparator();

        ArrayList<String> outputList = new ArrayList<String>();
        String outputString;

        for (Entry<String, ExtractedDate> e : inputMap.entrySet()) {
            System.out.println("begin");
            long begin = (new GregorianCalendar()).getTimeInMillis();
            ae.setUrl(e.getKey());
            ae.evaluate();
            ArrayList<ExtractedDate> myDates = ae.getAllBestRatedDate(true);
            ArrayList<ExtractedDate> sameDates = DateArrayHelper.getSameDates(e.getValue(), myDates,
                    DateComparator.STOP_DAY);
            ExtractedDate sameDate;

            outputString = e.getValue().getNormalizedDateString() + " | ";

            if (sameDates != null && sameDates.size() > 0) {
                sameDate = sameDates.get(0);
                outputString += sameDate.getNormalizedDate(false);
            } else {
                RatedDateComparator<ExtractedDate> rdc = new RatedDateComparator<ExtractedDate>();
                Collections.sort(myDates, rdc);
                sameDate = myDates.get(0);
                outputString += sameDate.getNormalizedDate(false);
            }

            String myAnnotation = ".";
            if (dc.compare(e.getValue(), sameDate, DateComparator.STOP_DAY) != 0) {
                myAnnotation = "-";
            }

            outputString += " | " + myAnnotation;
            outputList.add(outputString + " | " + sameDate.getType() + " | " + e.getKey());
            System.out.println(outputString);
            long end = (new GregorianCalendar()).getTimeInMillis();
            System.out.println("end: " + (end - begin));
        }

        file = new File(filePath + output);
        try {
            FileWriter writer = new FileWriter(file);
            BufferedWriter bWriter = new BufferedWriter(writer);

            bWriter.write("PageDate | MyDate | MyError | Technique | URL" + "\n");

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
                "data/test/webPages/dateExtraction/tests/linkSet/evaluationPages.txt");
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
        } catch (IOException ex) {

        }

        file = new File(
                "data/test/webPages/dateExtraction/tests/linkSet/dates.txt");
        ArrayList<String> dates = new ArrayList<String>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                dates.add(line);
            }
            br.close();
            fr.close();
        } catch (IOException ex) {

        }

        file = new File(
                "data/test/webPages/dateExtraction/tests/linkSet/Evaluation-LinkSet.txt");

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
                "data/test/webPages/dateExtraction/tests/linkSet/"
                        + input);

        HashMap<String, Integer> urls = new HashMap<String, Integer>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                urls.put(line, 0);
            }
            
            br.close();
            fr.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println(urls.size());

        DateGetter dg = new DateGetter();
        dg.setAllFalse();
        dg.setTechHTTP(true);

        DateEvaluator de = new DateEvaluator(PageDateType.publish);

        ArrayList<ExtractedDate> dgDates;
        HashMap<ExtractedDate, Double> deDates;

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
                "data/test/webPages/dateExtraction/tests/linkSet/"
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
