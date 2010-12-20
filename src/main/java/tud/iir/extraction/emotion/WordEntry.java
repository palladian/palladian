package tud.iir.extraction.emotion;

import java.util.HashMap;

public class WordEntry {

    public String word;

    public Integer counter;

    public String emotionCategory;

    public HashMap<String, String> sentenceUrlList;

    public WordEntry(String word, int counter, String emotionCategory) {
        this.word = word;
        this.counter = counter;
        this.emotionCategory = emotionCategory;
        sentenceUrlList = new HashMap<String, String>();
    }
    public String getWord(){
        return word;
    }
    public Integer getCounter(){
        return counter;
    }
    public String getEmotionCategory(){

        return emotionCategory;
    }

    @Override
    public String toString() {
        return word + " #" + counter;
    }

    public void increment() {
        counter++;
    }

    public void decrement() {
        counter--;
    }
    public void saveSentenceUrl(String satz, String url){
        sentenceUrlList.put(satz, url);
    }

    public HashMap<String, String> getSentenceUrlList(){
        return sentenceUrlList;
    }

}
