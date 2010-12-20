package tud.iir.emotion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.emotion.WordEntryList;
import tud.iir.helper.Tokenizer;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.WebResult;




public class Analyze {
    /* Anlegen der Liste von Emotionen mit dazugehörigen Schlüsselwörtern*/
    static HashMap<String, ArrayList<WordEntry>> words = new HashMap<String,ArrayList<WordEntry>>();

    static String searchQuery = null;
    static SourceRetriever s = new SourceRetriever();
    static ArrayList<WebResult> webURLs = new ArrayList<WebResult>();

    static ArrayList<String> urls = new ArrayList<String>();

    /* Neue Liste mit den einzelnen Sätzen der Seiten*/
    static ArrayList<SentenceEntry> listSentence = new ArrayList<SentenceEntry>();
    static String content = null;
    static PageContentExtractor p = new PageContentExtractor();

    /*Wörter aus den Sätzen, w = Wort für Vergleich, die anderen, die zwei Wörter davor.*/
    static String vorW = null;
    static String vorWW = null;
    static String w = null;

    static SentenceEntry en;

    static boolean testNegation;
    static boolean testUmlaut;
    static boolean testClimax;
    static List<WordEntry> emoW = new ArrayList<WordEntry>();

    public static void main(String[] args) {

        /* Auslesen aus CSV.Datei*/
        words = readCsv();

        /*Produkteingabe*/
        searchQuery = readProduct();

        /*Auslese aus Suchmaschine als Webresults in ArrayList*/

        webURLs = getWebResult();

        /*Die Urls dieser Webresults in eine ArrayList auslesen*/
        urls = getUrls();

        /* Extrahieren der Sätze aus dem Content und abspeichern als Objekt, mit den Parametern Satz/URL in einer Liste. */
        listSentence = getSentenceAndUrl();


        /*Vergleich der Wörter*/

        /*Liste mit gefundenen Sätzen durchgehen, Umlaute entfernen, in Tokens umwandeln und in ArrayList speichern. */
        for (int h = 0; h<(listSentence.size()); h++){
            en = listSentence.get(h);
            String n = en.getSentence();
            List<String> tokens = new ArrayList<String>();
            n = replace(n);
            tokens = Tokenizer.tokenize(n);

            /*Liste der Tokens der einzelnen Sätze durchgehen*/
            for (int i = 0; i<(tokens.size()); i++){
                w = tokens.get(i);
                /*Liste der Emotionensschlüsselwörter durchgehen*/
                for (String e : words.keySet()){
                    emoW = words.get(e);

                    /*Liste der WordEntrys durchgehen und beide Vergleichswörter kleinschreiben*/
                    for (int j = 0; j<(emoW.size()); j++){
                        String vergleich = emoW.get(j).getWord();
                        vergleich = vergleich.toLowerCase();
                        w = w.toLowerCase();
                        Integer y = w.length();
                        Integer yy = vergleich.length();


                        testNegation = getNegation(w, vorW, vorWW);
                        if(testNegation == false){
                            if (testUmlaut == true){
                                save(j);
                            }else{
                                if (testClimax == true){
                                    save(j);
                                }else{
                                    /*Abfrage ob Wörter einfach gleich sind*/
                                    if(vergleich.equals(w)){
                                        save(j);
                                    }
                                }
                            }
                        }
                    }
                    /*Wörter vor Token neu abspeichern*/
                }
                vorWW = vorW;
                vorW = w;
            }
        }
        /*Ausgabe wie oft welche Emotion vorkam*/
        getEmotionsCount();

        /*Ausgabe in CSV.Datei*/
        writeToCsv();

    }


    public static HashMap<String, ArrayList<WordEntry>> readCsv(){
        FileReader myFile= null;
        BufferedReader buff= null;
        String [] values;

        try {
            myFile =new FileReader("src/main/java/tud/iir/emotion/wort2.csv");
            buff =new BufferedReader(myFile);
            while (true) {
                String line = buff.readLine();
                if (line == null) {
                    break;
                }

                values = line.split(";");

                /*Abfrage ob Emotion schon vorhanden - ja, dann neues WortObjekt zu der List welche zur Emotion gehört hinzufgen*/
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
        return words;

    }
    public static String readProduct(){
        BufferedReader bin = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Bitte geben Sie das Produkt ein: ");

        try {
            searchQuery = bin.readLine();
        } catch (IOException e) {

            e.printStackTrace();
        }
        System.out.println("Meinungen zu folgendem Produkt werden gesucht: " + searchQuery + "  ");

        return searchQuery;
    }
    public static ArrayList<WebResult> getWebResult(){
        s.setResultCount(20);

        s.setLanguage(SourceRetriever.LANGUAGE_GERMAN);

        s.setSource(SourceRetrieverManager.GOOGLE);

        ArrayList<WebResult> webURLs = s.getWebResults(searchQuery, 2, false);

        return webURLs;

    }
    public static ArrayList<String> getUrls(){
        Integer f = webURLs.size();
        String url = null;

        for (int i = 0; i<f; i++){
            url = webURLs.get(i).getUrl();
            urls.add(url);
        }
        return urls;
    }
    public static ArrayList<SentenceEntry> getSentenceAndUrl(){
        for (int l = 0; l<(urls.size()); l++){
            String t = urls.get(l);
            content = p.getResultText(t);

            List<String> sentenceUrl = new ArrayList<String>();

            sentenceUrl = Tokenizer.getSentences(content);

            for (int i = 0; i<sentenceUrl.size(); i++){
                SentenceEntry se = new SentenceEntry(sentenceUrl.get(i), t);
                listSentence.add(se);
            }
        }
        return listSentence;

    }
    /*Abfrage ob Negation vorhanden*/
    public static boolean getNegation(String w, String vorW, String vorWW){
        if(("nicht".equals(vorWW) == false && "ohne".equals(vorWW)== false && "keine".equals(vorW) == false && "nicht".equals(vorW) == false)){
            testNegation = false;
        }
        return testNegation;
    }
    public static String replace(String n){
        n = n.replaceAll("ü", "ue");
        n = n.replaceAll("Ü", "Ue");
        n = n.replaceAll("ö", "oe");
        n = n.replaceAll("Ö", "Oe");
        n = n.replaceAll("ä", "ae");
        n = n.replaceAll("Ä", "Ae");
        n = n.replaceAll("ß", "ss");
        return n;
    }
    public static void save(int j){
        emoW.get(j).increment();
        en.addWordEntry(emoW.get(j));
        String r = en.getUrl();
        String b = en.getSentence();
        emoW.get(j).saveSentenceUrl(b, r);
    }
    public boolean getUmlaut(String w, String vergleich, int j, int y, int yy){
        if(w.length()>5){
            /*Abfrage ob Wort Umlaut enthält und im Plural ist - wie Bäume/Baum - Wenn ja als gefundenes Wort abspeichern*/
            if ((w.subSequence(1,3)).equals("ae") || (w.subSequence(1,3)).equals("oe") || (w.subSequence(1,3)).equals("ue") && w.endsWith("en") || w.endsWith("es") || w.endsWith("er") || w.endsWith("em")){
                String qs = w.substring(3, (y-2));
                String os = vergleich.substring(2, yy);

                if (qs.equals(os) && (w.subSequence(0,1).equals(w.subSequence(0,1)))){
                    testUmlaut = true;
                }
                else{
                    if(w.endsWith("e") || w.endsWith("s")){
                        String qu = w.substring(0, (y-1));
                        if (qu.equals(vergleich)){
                            testUmlaut = true;
                        }
                    }else{
                        if(w.endsWith("ern")){
                            String qt = w.substring(0, (y-3));
                            if (qt.equals(vergleich)){
                                testUmlaut = true;
                            }
                        }
                    }
                }
            }else{
                testUmlaut = false;
            }
        }
        return testUmlaut;
    }

    /*Abfrage ob Wort im Plural oder gesteigert. Wenn ja, abspeichern*/
    public boolean getClimax(String w, String vergleich, int j, int y, int yy){
        if (w.endsWith("er") || w.endsWith("en") || w.endsWith("es") || w.endsWith("em")){
            String qs = w.substring(0, (y-2));
            if (qs.equals(vergleich)){
                testClimax = true;
            }
        }else {
            if(w.endsWith("e") || w.endsWith("s")){
                String qs = w.substring(0, (y-1));
                if (qs.equals(vergleich)){
                    testClimax = true;
                }
            }else{
                if (w.endsWith("ern")){
                    String qs = w.substring(0, (y-3));
                    if (qs.equals(vergleich)){
                        testClimax = true;
                    }
                }
            }
        }
        return testClimax;
    }
    public static void getEmotionsCount(){
        for (String j : words.keySet()){
            List<WordEntry> emoW = words.get(j);
            Integer c = 0;
            for (int i = 0; i < emoW.size(); i++){
                Integer x = emoW.get(i).getCounter();
                c = c + x;
            }
            System.out.println("Die Emotion " + j + " kam " + c + " mal vor!");
        }
    }
    public static void writeToCsv(){
        try
        {
            FileWriter writer = new FileWriter("e:\\test.csv");
            /*Auslesen der einzelnen Emotionen*/
            for (String m : words.keySet()){
                List<WordEntry> emoWords = words.get(m);
                writer.write(m);
                writer.append('\n');
                writer.append('\n');
                /*Wörter welche mindestens einmal aufgetaucht sind ausgeben mit dazugehörigem Zähler und Satz/URL*/
                for (int i = 0; i<emoWords.size(); i++){
                    if (emoWords.get(i).counter != 0){
                        writer.write(emoWords.get(i).getWord());
                        writer.append('\n');
                        writer.write(Integer.toString(emoWords.get(i).counter));
                        writer.append('\n');
                        for (String o : emoWords.get(i).getSentenceUrlList().keySet()){
                            writer.write(o);
                            writer.append('\n');
                            writer.write(emoWords.get(i).getSentenceUrlList().get(o));
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