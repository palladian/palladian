package tud.iir.extraction.emotion;

import java.util.ArrayList;
import java.util.List;

public class SentenceEntry {

    public String sentence;

    public String url;

    public ArrayList<WordEntry> wordsFound;

    public SentenceEntry(String sentence, String url){
        this.sentence = sentence;
        this.url = url;
        wordsFound = new ArrayList<WordEntry>();
    }
    public String getSentence(){
        return sentence;
    }
    public List<WordEntry> getWordsFound(){
        return wordsFound;
    }
    public String getUrl(){
        return url;
    }
    public void addWordEntry(WordEntry vergleich){
        wordsFound.add(vergleich);
    }

}
