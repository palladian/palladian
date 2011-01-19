package tud.iir.extraction.emotion;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class EmotionOccurrence {

    private String emotion = "";
    private Map<String, EmotionWordOccurrence> emotionWordOccurrences;

    public EmotionOccurrence(String emotion) {
        emotionWordOccurrences = new HashMap<String, EmotionWordOccurrence>();
        this.emotion = emotion;
    }

    /** Get the total number of sentences in which one of the emotion words for this emotion appeared in. */
    public int getTotalSentenceCount() {
        int count = 0;

        for (Entry<String, EmotionWordOccurrence> entry : getEmotionWordOccurrences().entrySet()) {
            count += entry.getValue().getUrlSentences().values().size();
        }

        return count;
    }

    public String getEmotion() {
        return emotion;
    }
    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
    public Map<String, EmotionWordOccurrence> getEmotionWordOccurrences() {
        return emotionWordOccurrences;
    }
    public void setEmotionWordOccurrences(Map<String, EmotionWordOccurrence> emotionWordOccurrences) {
        this.emotionWordOccurrences = emotionWordOccurrences;
    }

    @Override
    public String toString() {
        return "EmotionOccurrence [emotion=" + emotion + ", emotionWordOccurrences=" + emotionWordOccurrences + "]";
    }

}
