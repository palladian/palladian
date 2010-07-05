package tud.iir.classification.query;

import java.util.ArrayList;
import java.util.TreeMap;

import tud.iir.helper.StringHelper;

public class QueryWord {

    public static int LEFT = 0;
    public static int RIGHT = 1;

    private String rootWord;
    private ArrayList<TreeMap<String, Integer>> wordsBefore;
    private ArrayList<TreeMap<String, Integer>> wordsAfter;
    private double occurrenceThreshold = 0.6;

    // number of words before and after the query word to consider as a part of the entity name
    private int adjacentWords = 4;

    public QueryWord(String rootWord) {
        wordsBefore = new ArrayList<TreeMap<String, Integer>>();
        wordsAfter = new ArrayList<TreeMap<String, Integer>>();

        for (int i = 0; i < adjacentWords; i++) {
            TreeMap<String, Integer> wordList = new TreeMap<String, Integer>();
            wordsBefore.add(wordList);
            wordList = new TreeMap<String, Integer>();
            wordsAfter.add(wordList);
        }
        setRootWord(rootWord);
    }

    public String getRootWord() {
        return rootWord;
    }

    public void setRootWord(String rootWord) {
        this.rootWord = rootWord;
    }

    public void addWord(String word, int leftRight, int position) {
        TreeMap<String, Integer> wordList = null;
        if (leftRight == LEFT) {
            wordList = wordsBefore.get(position);
        } else if (leftRight == RIGHT) {
            wordList = wordsAfter.get(position);
        } else {
            return;
        }

        word = StringHelper.trim(word);

        if (wordList.containsKey(word)) {
            int count = wordList.get(word);
            count++;
            wordList.put(word, count);
        } else {
            wordList.put(word, 1);
        }
    }

    /**
     * Try to create a full entity name.
     * 
     * @return
     */
    public String getFullEntityName() {
        String entityName = "";

        for (int i = 0; i < adjacentWords; i++) {
            TreeMap<String, Integer> wordList = wordsBefore.get(i);
            if (wordList.size() == 0) {
                continue;
            }
            double occurrenceFrequency = wordList.firstEntry().getValue() / (double) wordList.size();
            if (occurrenceFrequency >= occurrenceThreshold) {
                entityName += wordList.firstKey() + " ";
            }
        }

        entityName += getRootWord();

        for (int i = 0; i < adjacentWords; i++) {
            TreeMap<String, Integer> wordList = wordsAfter.get(i);
            if (wordList.size() == 0) {
                continue;
            }
            double occurrenceFrequency = wordList.firstEntry().getValue() / (double) wordList.size();
            if (occurrenceFrequency >= occurrenceThreshold) {
                entityName += wordList.firstKey() + " ";
            }
        }

        return entityName.trim();
    }

}
