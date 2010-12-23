package tud.iir.extraction.emotion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.helper.Tokenizer;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.WebResult;




public class Analyze {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Analyze.class);

    /* Anlegen der Liste von Emotionen mit dazugehörigen Schlüsselwörtern */
    Map<String, ArrayList<WordEntry>> words = new HashMap<String, ArrayList<WordEntry>>();

    String searchQuery = null;
    SourceRetriever s = new SourceRetriever();
    List<WebResult> webURLs = new ArrayList<WebResult>();

    List<String> urls = new ArrayList<String>();

    /* Neue Liste mit den einzelnen Sätzen der Seiten */
    List<SentenceEntry> listSentence = new ArrayList<SentenceEntry>();
    String content = null;
    PageContentExtractor p = new PageContentExtractor();

    /* Wörter aus den Sätzen, w = Wort für Vergleich, die anderen, die zwei Wörter davor. */
    String vorW = null;
    String vorWW = null;
    String w = null;

    SentenceEntry en;

    boolean testUmlaut;
    boolean testClimax;
    List<WordEntry> emoW = new ArrayList<WordEntry>();

    public Analyze() {

    }

    public void start() {
       /* /* Auslesen aus CSV.Datei */
        words = readCsv();

        /* Produkteingabe */
        //searchQuery = readProduct();

        /* Auslese aus Suchmaschine als Webresults in ArrayList */

       //webURLs = getWebResult();

        /* Die Urls dieser Webresults in eine ArrayList auslesen */
        //urls = getUrls();
       // urls.add("http://www.mobil-talk.de/sony-ericsson-testberichte/213-sony-ericsson-w910i-testbericht-inkl-vielerlivebilder.html")["http://www.mobil-talk.de/sony-ericsson-testberichte/213-sony-ericsson-w910i-testbericht-inkl-vielerlivebilder.html")
        /* Extrahieren der Sätze aus dem Content und abspeichern als Objekt, mit den Parametern Satz/URL in einer Liste. */
        //listSentence = getSentenceAndUrl(); 
       SentenceEntry se = new SentenceEntry("Alles ist schön", "www.michele.de");
       listSentence.add(se);

        /* Vergleich der Wörter */

        /* Liste mit gefundenen Sätzen durchgehen, Umlaute entfernen, in Tokens umwandeln und in ArrayList speichern. */
        for (int h = 0; h < (listSentence.size()); h++) {
            en = listSentence.get(h);
            String n = en.getSentence();
            List<String> tokens = new ArrayList<String>();

            tokens = Tokenizer.tokenize(n);
            //.add("schön");

            /* Liste der Tokens der einzelnen Sätze durchgehen */
            for (int i = 0; i < (tokens.size()); i++) {
                w = tokens.get(i);
                System.out.println(w);
                System.out.println("gehört zu aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

                /* Liste der Emotionensschlüsselwörter durchgehen */
                for (String e : words.keySet()) {
                    emoW = words.get(e);

                    /* Liste der WordEntrys durchgehen und beide Vergleichswörter kleinschreiben */
                    for (int j = 0; j < (emoW.size()); j++) {
                        String vergleich = emoW.get(j).getWord();
                        System.out.println(vergleich);

                        vergleich = vergleich.toLowerCase();
                        w = w.toLowerCase();
                        Integer y = w.length();
                        Integer yy = vergleich.length();

                        boolean testNegation = getNegation(w, vorW, vorWW);
                        if (testNegation == false) {
                            if (testUmlaut == true) {
                                save(j);
                            } else {
                                if (testClimax == true) {
                                    save(j);
                                } else {
                                    /* Abfrage ob Wörter einfach gleich sind */
                                    if (vergleich.equals(w)) {
                                        save(j);
                                    }
                                }
                            }
                        }
                    }
                    /* Wörter vor Token neu abspeichern */
                }
                vorWW = vorW;
                vorW = w;
            }
        }
        /* Ausgabe wie oft welche Emotion vorkam */
        getEmotionsCount();

        /* Ausgabe in CSV.Datei */
        writeToCsv();
    }

    public static void main(String[] args) {

        Analyze analyze = new Analyze();
        analyze.start();
    }

    private Map<String, ArrayList<WordEntry>> readCsv() {
        FileReader myFile = null;
        BufferedReader buff= null;
        String [] values;

        try {
            myFile = new FileReader("config/emotionDictionary_german.csv");
            buff = new BufferedReader(myFile);
            while (true) {
                String line = buff.readLine();
                if (line == null) {
                    break;
                }

                values = line.split(";");

                /*
                 * Abfrage ob Emotion schon vorhanden - ja, dann neues WortObjekt zu der List welche zur Emotion gehört
                 * hinzufgen
                 */
                if (words.containsKey(values[1]) == false) {
                    words.put(values[1], new WordEntryList());

                    words.get(values[1]).add(new WordEntry(values[0], 0, values[1]));
                } else {
                    words.get(values[1]).add(new WordEntry(values[0], 0, values[1]));
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            try {
                buff.close();
                myFile.close();
            } catch (IOException e) {
                System.err.println("Error2 :" + e);
            }
        }
        return words;

    }

    private String readProduct() {
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

    private List<WebResult> getWebResult() {
        s.setResultCount(20);

        s.setLanguage(SourceRetriever.LANGUAGE_GERMAN);

        s.setSource(SourceRetrieverManager.GOOGLE);

        List<WebResult> webURLs = s.getWebResults(searchQuery, 2, false);

        return webURLs;

    }

    private List<String> getUrls() {
        Integer f = webURLs.size();
        String url = null;

        for (int i = 0; i < f; i++) {
            url = webURLs.get(i).getUrl();
            urls.add(url);
            //urls.add("http://www.mobil-talk.de/sony-ericsson-testberichte/213-sony-ericsson-w910i-testbericht-inkl-vielerlivebilder.html");
        }
        return urls;
    }

    private List<SentenceEntry> getSentenceAndUrl() {
        for (int l = 0; l < (urls.size()); l++) {
            String t = urls.get(l);
            content = p.getResultText(t);

            List<String> sentenceUrl = new ArrayList<String>();

            sentenceUrl = Tokenizer.getSentences(content, true); // TODO test

            for (int i = 0; i < sentenceUrl.size(); i++) {
                SentenceEntry se = new SentenceEntry(sentenceUrl.get(i), t);
                listSentence.add(se);
            }
        }
        return listSentence;

    }

    /* Abfrage ob Negation vorhanden */
    private boolean getNegation(String w, String vorW, String vorWW) {
        boolean testNegation = true;
        if (("nicht".equals(vorWW) == false && "ohne".equals(vorWW) == false && "keine".equals(vorW) == false && "nicht"
                .equals(vorW) == false)) {
            testNegation = false;
        }
        return testNegation;
    }

    private void save(int j) {
        emoW.get(j).increment();
        en.addWordEntry(emoW.get(j));
        String r = en.getUrl();
        String b = en.getSentence();
        emoW.get(j).saveSentenceUrl(b, r);
    }

    private boolean getUmlaut(String w, String vergleich, int j, int y, int yy) {
        //boolean testUmlaut = false;
    	if (w.length() > 5) {
            /*
             * Abfrage ob Wort Umlaut enthält und im Plural ist - wie Bäume/Baum - Wenn ja als gefundenes Wort
             * abspeichern
             */
            if ((w.subSequence(1, 3)).equals("ae") || (w.subSequence(1, 3)).equals("oe")
                    || (w.subSequence(1, 3)).equals("ue") && w.endsWith("en") || w.endsWith("es") || w.endsWith("er")
                    || w.endsWith("em")) {
                String qs = w.substring(3, (y - 2));
                String os = vergleich.substring(2, yy);

                if (qs.equals(os) && (w.subSequence(0, 1).equals(w.subSequence(0, 1)))) {
                    testUmlaut = true;
                } else {
                    if (w.endsWith("e") || w.endsWith("s")) {
                        String qu = w.substring(0, (y - 1));
                        if (qu.equals(vergleich)) {
                            testUmlaut = true;
                        }
                    } else {
                        if (w.endsWith("ern")) {
                            String qt = w.substring(0, (y - 3));
                            if (qt.equals(vergleich)) {
                                testUmlaut = true;
                            }
                        }
                    }
                }
            } else {
                testUmlaut = false;
            }
        }
        return testUmlaut;
    }

    /* Abfrage ob Wort im Plural oder gesteigert. Wenn ja, abspeichern */
    private boolean getClimax(String w, String vergleich, int j, int y, int yy) {
        //boolean testClimax = false;
    	if (w.endsWith("er") || w.endsWith("en") || w.endsWith("es") || w.endsWith("em")) {
            String qs = w.substring(0, (y - 2));
            if (qs.equals(vergleich)) {
                testClimax = true;
            }
        } else {
            if (w.endsWith("e") || w.endsWith("s")) {
                String qs = w.substring(0, (y - 1));
                if (qs.equals(vergleich)) {
                    testClimax = true;
                }
            } else {
                if (w.endsWith("ern")) {
                    String qs = w.substring(0, (y - 3));
                    if (qs.equals(vergleich)) {
                        testClimax = true;
                    }
                }else{
                	testClimax = false;
                }
            }
        }
        return testClimax;
    }

    private void getEmotionsCount() {
        for (String j : words.keySet()) {
            List<WordEntry> emoW = words.get(j);
            Integer c = 0;
            for (int i = 0; i < emoW.size(); i++) {
                Integer x = emoW.get(i).getCounter();
                c = c + x;
            }
            System.out.println("Die Emotion " + j + " kam " + c + " mal vor!");
        }
    }

    private void writeToCsv() {
        try {
            FileWriter writer = new FileWriter("e:\\test.csv");
            /* Auslesen der einzelnen Emotionen */
            for (String m : words.keySet()) {
                List<WordEntry> emoWords = words.get(m);
                writer.write(m);
                writer.append('\n');
                writer.append('\n');
                /* Wörter welche mindestens einmal aufgetaucht sind ausgeben mit dazugehörigem Zähler und Satz/URL */
                for (int i = 0; i < emoWords.size(); i++) {
                    if (emoWords.get(i).counter != 0) {
                        writer.write(emoWords.get(i).getWord());
                        writer.append('\n');
                        writer.write(Integer.toString(emoWords.get(i).counter));
                        writer.append('\n');
                        for (String o : emoWords.get(i).getSentenceUrlList().keySet()) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}