package tud.iir.extraction.emotion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EmotionWordOccurrence {

    private String word = "";

    private Map<String, Set<String>> urlSentences;

    public EmotionWordOccurrence(String word) {
        urlSentences = new HashMap<String, Set<String>>();
        this.word = word;
    }

    public void addSentence(String sentence, String url) {
        Set<String> sentences = getUrlSentences().get(url);

        if (sentences == null) {
            sentences = new HashSet<String>();
            getUrlSentences().put(url, sentences);
        }

        sentences.add(sentence);
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Map<String, Set<String>> getUrlSentences() {
        return urlSentences;
    }

    public void setUrlSentences(Map<String, Set<String>> urlSentences) {
        this.urlSentences = urlSentences;
    }

    @Override
    public String toString() {
        return "EmotionWordOccurrence [word=" + word + ", urlSentences=" + urlSentences + "]";
    }

}
