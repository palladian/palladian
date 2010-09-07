package tud.iir.daterecognition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import tud.iir.web.Crawler;

public class LinkSetCreator {

    public static void main(String[] args) {
        Crawler c = new Crawler();

        String url = null;

        File linkSet = new File(
                "E:/_Uni/_semester15/Beleg/eclipse workspace/toolkit/data/test/webPages/dateExtraction/tests/linkSet/linkSet.txt");
        try {
            FileReader fr = new FileReader(linkSet);
            BufferedReader br = new BufferedReader(fr);
            Random ran = new Random();
            String line;
            HashSet<String> allLines = new HashSet<String>();
            while ((line = br.readLine()) != null) {
                allLines.add(line);

            }
            int urlNumber = ran.nextInt(allLines.size() + 1);
            int count = 0;
            System.out.println(urlNumber);
            Iterator<String> it = allLines.iterator();
            while (it.hasNext()) {
                url = it.next();
                if (count >= urlNumber) {
                    break;
                }
                count++;
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

        if (url == null) {
            // url = "http://www.dmoz.org";
            // url = "http://www.nytimes.com";
            // url =
            // "http://techcrunch.com/2010/09/01/as-facebook-and-orkut-duke-it-out-in-india-sms-gupshup-hits-35-million-users/";
            // url = "http://spreeblick.com";
            // url = "http://www.bangkokpost.com/news/local/194115/schoolboy-killed-in-shooting-in-bangkok";
            // url = "http://www.techeblog.com/";
            // url = "http://www.washingtonpost.com/";
            // url = "http://mashable.com/2010/09/03/nasa-visit-the-sun/";
            url = "http://www.huffingtonpost.com/";
        }
        c.setDocument(url);
        HashSet<String> links = c.getLinks(false, true);
        Random r = new Random();
        int randomIndex;

        while (links.size() < 2000) {
            randomIndex = r.nextInt(links.size());
            Iterator<String> it = links.iterator();
            int counter = 0;
            while (it.hasNext()) {
                url = it.next();
                if (counter == randomIndex) {
                    break;
                }
                counter++;
            }
            c.setDocument(url);
            links.addAll(c.getLinks(false, true));

        }

        try {
            FileWriter fw = new FileWriter(linkSet, true);
            BufferedWriter bw = new BufferedWriter(fw);
            Iterator<String> it = links.iterator();
            while (it.hasNext()) {
                bw.write(it.next() + "\n");
            }
            bw.close();
            fw.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
