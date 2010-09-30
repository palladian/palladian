package tud.iir.daterecognition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.daterecognition.technique.URLDateGetter;
import tud.iir.helper.DateArrayHelper;
import tud.iir.knowledge.RegExp;

public class EvalURL {
    String input;
    String output;

    public EvalURL(String input, String output) {
        this.input = input;
        this.output = output;
    }

    public void start(int begin, int end) {

        for (int i = begin; i <= end; i++) {
            EvalURLThread t = new EvalURLThread(input.replaceAll(".txt", i + ".txt"), output.replaceAll(".txt", i
                    + ".txt"));
            t.start();
        }

    }

    class EvalURLThread extends Thread {
        String input;
        String output;

        public EvalURLThread(String input, String output) {
            this.input = input;
            this.output = output;

        }

        public Integer countSame = 0;
        public Integer countAll = 0;

        int countDATE_URL_D = 0;
        int countDATE_URL_SPLIT = 0;
        int countDATE_URL = 0;
        int countDATE_URL_MMMM_D = 0;
        int countOtherFormat = 0;
        int samecountDATE_URL_D = 0;
        int samecountDATE_URL_SPLIT = 0;
        int samecountDATE_URL = 0;
        int samecountDATE_URL_MMMM_D = 0;
        int samecountOtherFormat = 0;

        public void run() {
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
            URLDate urlDate = null;
            String urlFormat;
            int urlFormatInt;
            String outputString;
            File outfile = new File(
                    "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/"
                            + output);
            try {
                // outfile.delete();
            } catch (Exception ex) {
            }

            for (int i = 0; i < urls.size(); i++) {
                System.out.println(i);
                URLDateGetter udg = new URLDateGetter();
                udg.setUrl(urls.get(i));
                ArrayList<URLDate> urldDates = new ArrayList<URLDate>();
                urldDates = udg.getDates();
                if (urldDates.size() > 0) {
                    urlDate = urldDates.get(0);
                }
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
    }

}
