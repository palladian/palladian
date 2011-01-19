package tud.iir.extraction.emotion;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;

public class EntityEmotionDistribution {

    private String entityName = "";

    private Map<String, EmotionOccurrence> emotionOccurrences;

    public EntityEmotionDistribution(String entityName) {
        emotionOccurrences = new HashMap<String, EmotionOccurrence>();
        this.entityName = entityName;
    }

    public void addEmotionOccurrence(String emotionWord, String emotion, String url, String sentence) {
        EmotionOccurrence emotionOccurrence = getEmotionOccurrences().get(emotion);

        if (emotionOccurrence == null) {
            emotionOccurrence = new EmotionOccurrence(emotion);
            getEmotionOccurrences().put(emotion, emotionOccurrence);
        }

        EmotionWordOccurrence emotionWordOccurrence = emotionOccurrence.getEmotionWordOccurrences().get(emotionWord);

        if (emotionWordOccurrence == null) {
            emotionWordOccurrence = new EmotionWordOccurrence(emotionWord);
            emotionOccurrence.getEmotionWordOccurrences().put(emotionWord, emotionWordOccurrence);
        }

        emotionWordOccurrence.addSentence(sentence, url);
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Map<String, EmotionOccurrence> getEmotionOccurrences() {
        return emotionOccurrences;
    }

    public void setEmotionOccurrences(Map<String, EmotionOccurrence> emotionOccurrences) {
        this.emotionOccurrences = emotionOccurrences;
    }

    @Override
    public String toString() {
        return "EntityEmotionDistribution [entityName=" + entityName + ", emotionOccurrences=" + emotionOccurrences
        + "]";
    }

    public Map<String, Double> getDistribution() {
        Map<String, Double> emotionDistribution = new HashMap<String, Double>();

        int totalSentences = 0;
        for (Entry<String, EmotionOccurrence> entry : getEmotionOccurrences().entrySet()) {
            totalSentences += entry.getValue().getTotalSentenceCount();
        }

        for (Entry<String, EmotionOccurrence> entry : getEmotionOccurrences().entrySet()) {
            double value = entry.getValue().getTotalSentenceCount() / (double) totalSentences;
            emotionDistribution.put(entry.getKey(), value);
        }

        return emotionDistribution;
    }

    public String generateGoogleChart() {
        String chart = "https://chart.googleapis.com/chart?cht=p&chs=450x200";
        String labels = "";
        String data = "";
        for (Entry<String, Double> entry : getDistribution().entrySet()) {
            if (labels.length() > 0) {
                labels += "|";
            }
            labels += entry.getKey();

            if (data.length() > 0) {
                data += "|";
            }
            data += MathHelper.round(100 * entry.getValue(), 2) + "%";

        }
        chart += "&chl=" + labels;
        chart += "&chd=t:" + data.replace("|", ",").replace("%", "");
        chart += "&chdl=" + data;

        return chart;
    }

    public void serializeToCsv(String targetPath) {

        StringBuilder csv = new StringBuilder();
        csv.append(getEntityName());

        for (Entry<String, EmotionOccurrence> entry : getEmotionOccurrences().entrySet()) {
            csv.append("\n\n").append(entry.getKey());

            for (Entry<String, EmotionWordOccurrence> entry2 : entry.getValue().getEmotionWordOccurrences().entrySet()) {
                csv.append("\n").append(entry2.getKey());

                for (Entry<String, Set<String>> entry3 : entry2.getValue().getUrlSentences().entrySet()) {
                    for (String sentence : entry3.getValue()) {
                        csv.append(";").append(entry3.getKey()).append(";\"").append(sentence).append("\"\n");
                    }
                }

            }

        }

        FileHelper.writeToFile(targetPath, csv);
    }

}