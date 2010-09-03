package tud.iir.daterecognition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;

import tud.iir.daterecognition.dates.ExtractedDate;

public class TestEvaluator {

    /**
     * @param args
     */
    public static void main(String[] args) {
        DateGetterExecuter dgc = new DateGetterExecuter();
        dgc.execute();

    }

}

class DateGetterExecuter {
    int maxThreads = 500;
    int actThread = 0;

    private static HashSet<String> readLinkSet(String pathName) {
        File linkSet = new File(pathName);
        HashSet<String> allLines = new HashSet<String>();
        try {
            FileReader fr = new FileReader(linkSet);
            BufferedReader br = new BufferedReader(fr);
            Random ran = new Random(100);
            String line;
            while ((line = br.readLine()) != null) {
                allLines.add(line);
            }
            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return allLines;

    }

    public void execute() {
        HashSet<String> linkSet = readLinkSet("E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/linkSet.txt");

        Iterator<String> urlIT = linkSet.iterator();

        DateEvaluatorCounter dec = new DateEvaluatorCounter();
        while (urlIT.hasNext()) {
            String url = urlIT.next();
            DateGetterThread dgt = new DateGetterThread(url, dec, this);
            dgt.start();
            actThread++;
            while (actThread > maxThreads) {

            }
            System.out.println("next thread" + actThread);
        }
    }

    public void decreaseThreadCount() {
        actThread--;
    }
}

class DateGetterThread extends Thread {
    public String url;
    DateEvaluatorCounter dec;
    DateGetterExecuter caller;

    DateGetterThread(String url, DateEvaluatorCounter dec, DateGetterExecuter caller) {
        this.url = url;
        this.dec = dec;
        this.caller = caller;
    }

    public void run() {
        DateGetter dg = new DateGetter();
        dg.setTechReference(false);
        dg.setTechArchive(false);
        dg.setURL(url);
        ArrayList<ExtractedDate> dates = dg.getDate();

        DateEvaluator de = new DateEvaluator();
        HashMap<ExtractedDate, Double> evaluatedDates = de.evaluate(dates);
        int countRate1 = 0;
        int countRate5 = 0;
        int countURLTech = 0;
        int countHTTP = 0;
        int countHead = 0;
        int counCont = 0;
        int counStruc = 0;
        for (Entry<ExtractedDate, Double> e : evaluatedDates.entrySet()) {
            Double rate = e.getValue();
            if (rate == 1) {
                switch (e.getKey().getType()) {
                    case ExtractedDate.TECH_URL:
                        countURLTech++;
                        break;
                    case ExtractedDate.TECH_HTTP_HEADER:
                        countHTTP++;
                        break;
                    case ExtractedDate.TECH_HTML_HEAD:
                        countHead++;
                        break;
                    case ExtractedDate.TECH_HTML_STRUC:
                        counStruc++;
                        break;
                    case ExtractedDate.TECH_HTML_CONT:
                        counCont++;
                        break;
                }
                countRate1++;
            } else if (rate > 0.49) {
                countRate5++;
            }
        }
        dec.addCounts(1, evaluatedDates.size(), countRate1, countRate5, countURLTech, countHTTP, countHead, counCont,
                counStruc);
        caller.decreaseThreadCount();

    }
}

class DateEvaluatorCounter {
    public int countRate1 = 0;
    public int countRate5 = 0;
    public int countDates = 0;
    public int countURL = 0;

    public int countURLTech = 0;
    public int countHTTP = 0;
    public int countHead = 0;
    public int countCont = 0;
    public int counStruc = 0;

    public synchronized void addCounts(int countURL, int countDates, int countRate1, int countRate5, int countURLTech,
            int countHTTP, int countHead, int counCont, int counStruc) {
        this.countURL += countURL;
        this.countRate1 += countRate1;
        this.countRate5 += countRate5;
        this.countDates += countDates;
        this.countURLTech += countURLTech;
        this.countHTTP += countHTTP;
        this.countHead += countHead;
        this.countCont += counCont;
        this.counStruc += counStruc;

        System.out.println("URLs: " + this.countURL + " Dates: " + this.countDates + " rate1: " + getcountRate1()
                + " rate0.5: " + getcountRate5() + " URL: " + this.countURLTech + " HTTP: " + this.countHTTP
                + " Head: " + this.countHead + " Struct: " + this.counStruc + " Cont: " + this.countCont);

    }

    public synchronized int getcountRate1() {
        return countRate1;

    }

    public synchronized int getcountRate5() {
        return countRate5;

    }

    public synchronized int getcountURL() {
        return countURL;

    }
}
