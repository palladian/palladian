package ws.palladian.preprocessing.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Document;

import ws.palladian.extraction.content.PageSentenceExtractor;
import ws.palladian.helper.CountMap;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.Tokenizer;
import ws.palladian.web.SourceRetriever;
import ws.palladian.web.SourceRetrieverManager;
import ws.palladian.web.URLDownloader;

public class InformativenessAssigner {

    private Map<String, Double> tokenFrequencies = new HashMap<String, Double>();
    private Map<String, Double> normalizedTokenFrequencies = new HashMap<String, Double>();
    private CountMap tokenFrequencyMap = new CountMap();

    public void initTokenFrequencyMap() {

        // get texts from web pages
        List<String> texts = getTexts();

        // calculate token frequencies
        int totalTokens = 0;
        for (String text : texts) {
            List<String> tokens = Tokenizer.tokenize(text);

            for (String token : tokens) {
                tokenFrequencyMap.increment(token);
            }

            totalTokens += tokens.size();
        }

        for (Entry<Object, Integer> entry : tokenFrequencyMap.entrySet()) {
            tokenFrequencies.put(entry.getKey().toString(), entry.getValue() / (double) totalTokens);
        }

        // normalize frequency using the token with the highest frequency as upper cap = 1
        double highestValue = 0;
        for (Integer value : tokenFrequencyMap.values()) {
            if (value > highestValue) {
                highestValue = value;
            }
        }

        for (Entry<Object, Integer> entry : tokenFrequencyMap.entrySet()) {
            normalizedTokenFrequencies.put(entry.getKey().toString(), entry.getValue() / highestValue);
        }

    }

    private List<String> getTexts() {
        List<String> texts = new ArrayList<String>();

        SourceRetriever sr = new SourceRetriever();
        sr.setSource(SourceRetrieverManager.BING);
        sr.setResultCount(20);

        List<String> urls = sr.getURLs("and with many in of");

        URLDownloader ud = new URLDownloader();
        ud.add(urls);
        Set<Document> documents = ud.start();

        PageSentenceExtractor pse = new PageSentenceExtractor();

        for (Document document : documents) {
            pse.setDocument(document);
            texts.add(pse.getSentencesString());
        }

        return texts;
    }

    public String tagText(String text) {

        List<String> tokens = Tokenizer.tokenize(text);

        // count the occurrences of the tokens
        CountMap cm = new CountMap();
        for (String token : tokens) {
            cm.increment(token);
        }

        // normalize frequency using the token with the highest frequency as upper cap = 1
        int highestFrequency = 1;
        for (Integer frequency : cm.values()) {
            if (frequency > highestFrequency) {
                highestFrequency = frequency;
            }
        }

        Map<String, Double> informativenessMap = new HashMap<String, Double>();
        for (Entry<Object, Integer> entry : cm.entrySet()) {
            informativenessMap.put(entry.getKey().toString(), entry.getValue() / (double) highestFrequency);
        }

        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            // double informativeness = Math.round(informativenessMap.get(token));
            double informativeness = 1;
            Double frequencyScore = normalizedTokenFrequencies.get(token);
            if (frequencyScore != null) {
                informativeness = frequencyScore;
            }

            double hue = Math.round(255 * informativeness);
            sb.append("<token style=\"color:hsl(").append(hue).append(",100%, 35%)\">");
            sb.append(token);
            sb.append("</token>");
            sb.append(" ");
        }

        return sb.toString();
    }

    public void saveAsHTML(String text, String path) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<title>Informativeness Tagged Text</title>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append(text);
        sb.append("</body>");
        sb.append("</html>");
        
        FileHelper.writeToFile(path, sb);        
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        InformativenessAssigner ia = new InformativenessAssigner();

        ia.initTokenFrequencyMap();

        String text = "Superman is a fictional character, a comic book superhero appearing in publications by DC Comics, widely considered to be an American cultural icon. Created by American writer Jerry Siegel and Canadian-born American artist Joe Shuster in 1932 while both were living in Cleveland, Ohio, and sold to Detective Comics, Inc. (later DC Comics) in 1938, the character first appeared in Action Comics #1 (June 1938) and subsequently appeared in various radio serials, television programs, films, newspaper strips, and video games. With the success of his adventures, Superman helped to create the superhero genre and establish its primacy within the American comic book. The character's appearance is distinctive and iconic: a blue, red and yellow costume, complete with cape, with a stylized 'S' shield on his chest. This shield is now typically used across media to symbolize the character.";
        String taggedText = ia.tagText(text);
        ia.saveAsHTML(taggedText, "data/temp/taggedInformativeness.html");
    }

}
