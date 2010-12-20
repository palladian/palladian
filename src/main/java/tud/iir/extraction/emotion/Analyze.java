package tud.iir.extraction.emotion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.helper.Tokenizer;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.WebResult;




public class Analyze {

    public static void main(String[] args) {
        /* Anlegen der Liste von Emotionen mit dazugeh�rigen Schl�sselw�rtern*/
        HashMap<String, ArrayList<WordEntry>> words = new HashMap<String,ArrayList<WordEntry>>();
        /* Auslesen aus CSV.Datei*/
        FileReader myFile= null;
        BufferedReader buff= null;
        String [] values;

        try {
            myFile =new FileReader("src/main/java/emotionanalyzing/wort2.csv");
            buff =new BufferedReader(myFile);
            while (true) {
                String line = buff.readLine();
                if (line == null) {
                    break;
                }

                values = line.split(";");

                /*Abfrage ob Emotion schon vorhanden - ja, dann neues WortObjekt zu der List welche zur Emotion geh�rt hinzuf�gen*/
                if(words.containsKey(values[1])== false){
                    words.put(values[1], new WordEntryList());

                    words.get(values[1]).add(new WordEntry(values[0], 0, values[1]));
                }else{
                    words.get(values[1]).add(new WordEntry(values[0], 0, values[1]));
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error2 :"+e);
        } finally {
            try{
                buff.close();
                myFile.close();
            }catch (IOException e) {
                System.err.println("Error2 :"+e);
            }
        }

        SourceRetriever s = new SourceRetriever();
        String searchQuery = null;

        BufferedReader bin = new BufferedReader(
                new InputStreamReader(System.in));

        System.out.println("Bitte geben Sie das Produkt ein: ");

        try {
            searchQuery = bin.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Meinungen zu folgendem Produkt werden gesucht: " + searchQuery + "  ");
        /*Auslese aus Suchmaschine als Webresults in ArrayList*/
        s.setResultCount(20);

        s.setLanguage(SourceRetriever.LANGUAGE_GERMAN);

        s.setSource(SourceRetrieverManager.GOOGLE);

        List<WebResult> webURLs = s.getWebResults(searchQuery, 2, false);

        /*Die Urls dieser Webresults in eine ArrayList auslesen*/
        Integer f = webURLs.size();
        String url = null;
        ArrayList<String> urls = new ArrayList<String>();
        for (int i = 0; i<f; i++){
            url = webURLs.get(i).getUrl();
            urls.add(url);
        }

        String content = null;
        PageContentExtractor p = new PageContentExtractor();

        /* Neue Liste mit den einzelnen S�tzen der Seiten*/
        List<SentenceEntry> listSentence = new ArrayList<SentenceEntry>();

        /* Extrahieren der S�tze aus dem Content und abspeichern als Objekt, mit den Parametern Satz/URL in einer Liste. */
        for (int l = 0; l<urls.size(); l++){
            String t = urls.get(l);
            content = p.getResultText(t);

            List<String> sentenceUrl = new ArrayList<String>();

            sentenceUrl = Tokenizer.getSentences(content);

            for (int i = 0; i<sentenceUrl.size(); i++){
                SentenceEntry se = new SentenceEntry(sentenceUrl.get(i), t);
                listSentence.add(se);
            }
        }

        /*W�rter aus den S�tzen, w = Wort f�r Vergleich, die anderen, die zwei W�rter davor.*/
        String vorW = null;
        String vorWW = null;
        String vorWWW = null;
        String w = null;
        /*Vergleich der W�rter*/

        /*Liste mit gefundenen S�tzen durchgehen, Umlaute entfernen, in Tokens umwandeln und in ArrayList speichern. */
        for (int h = 0; h<listSentence.size(); h++){
            SentenceEntry en = listSentence.get(h);
            String n = en.getSentence();
            List<String> tokens = new ArrayList<String>();
            n = n.replaceAll("�", "ue");
            n = n.replaceAll("�", "Ue");
            n = n.replaceAll("�", "oe");
            n = n.replaceAll("�", "Oe");
            n = n.replaceAll("�", "ae");
            n = n.replaceAll("�", "Ae");
            n = n.replaceAll("�", "ss");
            tokens = Tokenizer.tokenize(n);

            /*Liste der Tokens der einzelnen S�tze durchgehen*/
            for (int i = 0; i<tokens.size(); i++){
                w = tokens.get(i);
                /*Liste der Emotionensschl�sselw�rter durchgehen*/
                for (String e : words.keySet()){
                    List<WordEntry> emoW = new ArrayList<WordEntry>();
                    emoW = words.get(e);

                    /*Liste der WordEntrys durchgehen und beide Vergleichsw�rter kleinschreiben*/
                    for (int j = 0; j<emoW.size(); j++){
                        String vergleich = emoW.get(j).getWord();
                        vergleich = vergleich.toLowerCase();
                        w = w.toLowerCase();
                        Integer y = w.length();
                        Integer yy = vergleich.length();

                        /*Abfrage ob Negation vorhanden*/
                        if("nicht".equals(vorWW) == false && "ohne".equals(vorWW)== false && "keine".equals(vorW) == false && "nicht".equals(vorW) == false){

                            if(w.length()>5){
                                /*Abfrage ob Wort Umlaut enth�lt und im Plural ist - wie B�ume/Baum - Wenn ja als gefundenes Wort abspeichern*/
                                if (w.subSequence(1,3).equals("ae") || w.subSequence(1,3).equals("oe") || w.subSequence(1,3).equals("ue") && w.endsWith("en") || w.endsWith("es") || w.endsWith("er") || w.endsWith("em")){
                                    String qs = w.substring(3, (y-2));
                                    String os = vergleich.substring(2, yy);

                                    if (qs.equals(os) && w.subSequence(0,1).equals(w.subSequence(0,1))){
                                        emoW.get(j).increment();
                                        en.addWordEntry(emoW.get(j));
                                        String r = en.getUrl();
                                        String b = en.getSentence();
                                        emoW.get(j).saveSentenceUrl(b, r);
                                    }
                                    else{
                                        if(w.endsWith("e") || w.endsWith("s")){
                                            String qu = w.substring(0, (y-1));
                                            if (qu.equals(vergleich)){
                                                emoW.get(j).increment();
                                                en.addWordEntry(emoW.get(j));
                                                String r = en.getUrl();
                                                String b = en.getSentence();
                                                emoW.get(j).saveSentenceUrl(b, r);
                                            }
                                        }else{
                                            if(w.endsWith("ern")){
                                                String qt = w.substring(0, (y-3));
                                                if (qt.equals(vergleich)){
                                                    emoW.get(j).increment();
                                                    en.addWordEntry(emoW.get(j));
                                                    String r = en.getUrl();
                                                    String b = en.getSentence();
                                                    emoW.get(j).saveSentenceUrl(b, r);
                                                }
                                            }
                                        }
                                    }
                                }
                            }else{
                                /*Abfrage ob Wort im Plural oder gesteigert. Wenn ja, abspeichern*/
                                if (w.endsWith("er") || w.endsWith("en") || w.endsWith("es") || w.endsWith("em")){
                                    String qs = w.substring(0, (y-2));
                                    if (qs.equals(vergleich)){
                                        emoW.get(j).increment();
                                        en.addWordEntry(emoW.get(j));
                                        String r = en.getUrl();
                                        String b = en.getSentence();
                                        emoW.get(j).saveSentenceUrl(b, r);
                                    }
                                }
                                else {
                                    /*Abfrage ob Wort im Plural oder gesteigert. Wenn ja, abspeichern*/
                                    if(w.endsWith("e") || w.endsWith("s")){
                                        String qs = w.substring(0, (y-1));
                                        if (qs.equals(vergleich)){
                                            emoW.get(j).increment();
                                            en.addWordEntry(emoW.get(j));
                                            String r = en.getUrl();
                                            String b = en.getSentence();
                                            emoW.get(j).saveSentenceUrl(b, r);
                                        }
                                    }else{
                                        /*Abfrage ob Wort im Plural oder gesteigert. Wenn ja, abspeichern*/
                                        if (w.endsWith("ern")){
                                            String qs = w.substring(0, (y-3));
                                            if (qs.equals(vergleich)){
                                                emoW.get(j).increment();
                                                en.addWordEntry(emoW.get(j));
                                                String r = en.getUrl();
                                                String b = en.getSentence();
                                                emoW.get(j).saveSentenceUrl(b, r);
                                            }
                                        }else{
                                            /*Abfrage ob W�rter einfach gleich sind*/
                                            if(vergleich.equals(w)){
                                                emoW.get(j).increment();
                                                en.addWordEntry(emoW.get(j));
                                                String r = en.getUrl();
                                                String b = en.getSentence();
                                                emoW.get(j).saveSentenceUrl(b, r);
                                            }
                                        }
                                    }
                                }
                            }
                        }	//erste Klammer von if
                    }
                }
                /*W�rter vor Token neu abspeichern*/
                vorWWW = vorWW;
                vorWW = vorW;
                vorW = w;
            }
        }
        /*Ausgabe wie oft welche Emotion vorkam*/
        for (String j : words.keySet()){
            List<WordEntry> emoW = words.get(j);
            Integer c = 0;
            for (int i = 0; i < emoW.size(); i++){
                Integer x = emoW.get(i).getCounter();
                c = c + x;
            }
            System.out.println("Die Emotion " + j + " kam " + c + " mal vor!");
        }
        /*Ausgabe in CSV.Datei*/
        try
        {
            FileWriter writer = new FileWriter("e:\\test.csv");
            /*Auslesen der einzelnen Emotionen*/
            for (String n : words.keySet()){
                List<WordEntry> emoWords = words.get(n);
                writer.write(n);
                writer.append('\n');
                writer.append('\n');
                /*W�rter welche mindestens einmal aufgetaucht sind ausgeben mit dazugeh�rigem Z�hler und Satz/URL*/
                for (int i = 0; i<emoWords.size(); i++){
                    if (emoWords.get(i).counter != 0){
                        writer.write(emoWords.get(i).getWord());
                        writer.append('\n');
                        writer.write(Integer.toString(emoWords.get(i).counter));
                        writer.append('\n');
                        for (String m : emoWords.get(i).getSentenceUrlList().keySet()){
                            writer.write(m);
                            writer.append('\n');
                            writer.write(emoWords.get(i).getSentenceUrlList().get(m));
                            writer.append('\n');
                            writer.append('\n');
                        }
                    }
                }
            }

            writer.flush();
            writer.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}