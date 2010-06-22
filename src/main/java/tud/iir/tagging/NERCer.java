package tud.iir.tagging;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.Dictionary;
import tud.iir.classification.Term;
import tud.iir.classification.page.WebPageClassifier;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;

public class NERCer implements Serializable {

    private static final Logger logger = Logger.getLogger(NERCer.class);

    private static final long serialVersionUID = -8793232373094322955L;

    // pattern candidates in the form of: prefix (TODO: ENTITY suffix)
    private TreeMap<String, CategoryEntries> patternCandidates = null;

    // patterns in the form of: prefix (TODO ENTITY suffix)
    private HashMap<String, CategoryEntries> patterns = null;

    // a dictionary that holds a term vector of words that appear frequently close to the entities
    private Dictionary dictionary = null;

    // the connector to the knowledge base
    private transient KnowledgeBaseCommunicatorInterface kbCommunicator = null;

    public NERCer() {
        patternCandidates = new TreeMap<String, CategoryEntries>();
        patterns = new HashMap<String, CategoryEntries>();
        dictionary = new Dictionary("NERC dictionary", WebPageClassifier.FIRST);
    }

    public EntityList getTrainingEntities(double percentage) {
        if (kbCommunicator == null) {
            logger.debug("could not get training entities because no KnowledgeBaseCommunicator has been defined");
            return new EntityList();
        }
        return kbCommunicator.getTrainingEntities(percentage);
    }

    public void trainRecognizer(String trainingText) {
        // get candidates
        HashSet<String> entityCandidates = StringTagger.getTaggedEntities(trainingText);
        entityCandidates = StringHelper.trim(entityCandidates);

        // verify candidates using the knowledge base
        EntityList kbVerifiedEntities = verifyEntitiesWithKB(entityCandidates);

        // learn patterns
        createPatternCandidates(trainingText, kbVerifiedEntities);

        // update the dictionary
        updateDictionary(trainingText, kbVerifiedEntities);
    }

    public void trainRecognizer(HashSet<String> trainingTexts) {
        int c = 1;
        int totalTexts = trainingTexts.size();
        for (String trainingText : trainingTexts) {
            logger.info("training with training text number " + (c++) + " of " + totalTexts);
            trainRecognizer(trainingText);
        }
    }

    public void finishTraining() {
        // now that we have seen all training texts and have the pattern candidates we can calculate the patterns
        calculatePatterns();

        logger.info("serializing NERCer");
        FileHelper.serialize(this, "data/models/NERCer.model");

        logger.info("dictionary size: " + dictionary.size());
    }

    public void load() {
        NERCer n = (NERCer) FileHelper.deserialize("data/models/NERCer.model");
        this.dictionary = n.dictionary;
        this.kbCommunicator = n.kbCommunicator;
        this.patterns = n.patterns;
    }

    private void createPatternCandidates(String text, EntityList entities) {

        // get all pre- and suffixes
        for (RecognizedEntity entity : entities) {

            Pattern pat = Pattern.compile(entity.getName());
            Matcher m = pat.matcher(text);

            while (m.find()) {
                // String sentence = StringHelper.getSentence(text, m.start());

                // use prefixes only
                String[] windowWords = getWindowWords(text, m.start(), m.end(), true, false);

                StringBuilder patternCandidate = new StringBuilder();
                for (String word : windowWords) {
                    patternCandidate.append(StringHelper.trim(word)).append(" ");
                }

                CategoryEntries categoryEntries = patternCandidates.get(patternCandidate.toString());
                if (categoryEntries == null) {
                    categoryEntries = new CategoryEntries();
                    categoryEntries.addAll(entity.getCategoryEntries());
                }

                patternCandidates.put(patternCandidate.toString(), categoryEntries);
            }
        }
    }

    private void calculatePatterns() {

        // patternCandidates.put("1334abcdefg", null);
        // patternCandidates.put("0334abcdefg", null);
        // patternCandidates.put("262323abcde235", null);
        // patternCandidates.put("232323abcde235", null);
        // patternCandidates.put("abvasdfertaay", null);
        // patternCandidates.put("abcasdfertaay", null);

        logger.info("calculate patterns");
        int minPatternLength = 7;

        // keep information about frequency of possible patterns
        LinkedHashMap<String, Integer> possiblePatterns = new LinkedHashMap<String, Integer>();

        // in each iteration the common strings of the last level are compared
        Set<String> currentLevelPatternCandidatesSet = patternCandidates.keySet();
        LinkedHashSet<String> currentLevelPatternCandidates = new LinkedHashSet<String>();
        for (String pc : currentLevelPatternCandidatesSet) {
            if (pc.length() >= minPatternLength)
                currentLevelPatternCandidates.add(pc);
        }

        logger.info(currentLevelPatternCandidates.size() + " pattern candidates");

        LinkedHashSet<String> nextLevelPatternCandidates = null;

        for (int level = 0; level < 3; level++) {
            nextLevelPatternCandidates = new LinkedHashSet<String>();

            logger.info("level " + level + ", " + currentLevelPatternCandidates.size() + " pattern candidates");

            int it1Position = 0;
            Iterator<String> iterator1 = currentLevelPatternCandidates.iterator();
            while (iterator1.hasNext()) {

                String patternCandidate1 = iterator1.next();
                patternCandidate1 = StringHelper.reverseString(patternCandidate1);
                it1Position++;

                Iterator<String> iterator2 = currentLevelPatternCandidates.iterator();
                int it2Position = 0;

                while (iterator2.hasNext()) {

                    // jump to the position after iterator1
                    if (it2Position < it1Position) {
                        iterator2.next();
                        it2Position++;
                        continue;
                    }

                    String patternCandidate2 = iterator2.next();
                    patternCandidate2 = StringHelper.reverseString(patternCandidate2);

                    // logger.info("get longest common string:");
                    // logger.info(patternCandidate1);
                    // logger.info(patternCandidate2);
                    String possiblePattern = StringHelper.getLongestCommonString(patternCandidate1, patternCandidate2, false, false);
                    possiblePattern = StringHelper.reverseString(possiblePattern);

                    if (possiblePattern.length() < minPatternLength)
                        continue;

                    Integer c = possiblePatterns.get(possiblePattern);
                    if (c == null) {
                        possiblePatterns.put(possiblePattern, 1);
                    } else {
                        possiblePatterns.put(possiblePattern, c + 1);
                    }
                    nextLevelPatternCandidates.add(possiblePattern);

                }
            }
            currentLevelPatternCandidates = nextLevelPatternCandidates;
        }

        possiblePatterns = CollectionHelper.sortByValue(possiblePatterns.entrySet());
        // CollectionHelper.print(possiblePatterns);

        // use only patterns longer or equal 7 characters
        logger.info("filtering patterns");
        for (Entry<String, Integer> pattern : possiblePatterns.entrySet()) {
            if (pattern.getKey().length() >= minPatternLength) {

                // Categories categories = null;
                CategoryEntries categoryEntries = new CategoryEntries();

                // find out which categories the original pattern candidates belonged to
                for (Entry<String, CategoryEntries> originalPatternCandidate : patternCandidates.entrySet()) {
                    if (originalPatternCandidate.getKey().toLowerCase().indexOf(pattern.getKey().toLowerCase()) > -1) {
                        categoryEntries = originalPatternCandidate.getValue();
                        break;
                    }
                }

                if (categoryEntries == null)
                    categoryEntries = new CategoryEntries();

                for (CategoryEntry c : categoryEntries) {
                    c.addAbsoluteRelevance(pattern.getValue().doubleValue());
                }
                patterns.put(pattern.getKey(), categoryEntries);
            }
        }

        logger.info("calculated " + patterns.size() + " patterns:");
        for (Entry<String, CategoryEntries> pattern : patterns.entrySet()) {
            logger.info(" " + pattern);
        }

    }

    /**
     * Update the dictionary.
     * 
     * @param text The text that is used to search the given entities.
     * @param entities The entities that appear in the given text.
     */
    private void updateDictionary(String text, EntityList entities) {

        // words farther away from the entity get lower score, score = degradeFactor^distance, 1.0 = no degration
        //double degradeFactor = 1.0;

        for (RecognizedEntity entity : entities) {

            Pattern pat = Pattern.compile(entity.getName());
            Matcher m = pat.matcher(text);

            while (m.find()) {
                // String sentence = StringHelper.getSentence(text, m.start());

                String[] windowWords = getWindowWords(text, m.start(), m.end());

                for (String word : windowWords) {
                    word = StringHelper.trim(word);
                    if (word.length() < 3)
                        continue;
                    Term t = new Term(word);
                    for (CategoryEntry categoryEntry : entity.getCategoryEntries()) {
                        dictionary.updateWord(t, categoryEntry.getCategory().getName(), 1.0);
                    }
                }
            }

        }
    }

    private String[] getWindowWords(String text, int startIndex, int endIndex, boolean capturePrefix, boolean captureSuffix) {

        // get all words around entities within window (number of characters)
        int windowSize = 30;

        String prefix = "";
        if (capturePrefix)
            prefix = text.substring(Math.max(0, startIndex - windowSize), startIndex);

        String suffix = "";
        if (captureSuffix)
            suffix = text.substring(endIndex, Math.min(endIndex + windowSize, text.length()));

        String context = prefix + suffix;
        String[] words = context.split("\\s");

        return words;
    }

    private String[] getWindowWords(String text, int startIndex, int endIndex) {
        return getWindowWords(text, startIndex, endIndex, true, true);
    }

    public EntityList recognizeEntities(String text) {

        EntityList recognizedEntities = new EntityList();

        HashSet<String> entityCandidates = StringTagger.getTaggedEntities(text);

        EntityList kbVerifiedEntities = verifyEntitiesWithKB(entityCandidates);

        EntityList patternVerifiedEntities = verifyEntitiesWithPattern(entityCandidates, text);

        EntityList dictionaryVerifiedEntities = verifyEntitiesWithDictionary(entityCandidates, text);

        recognizedEntities.addAll(kbVerifiedEntities);
        recognizedEntities.addAll(patternVerifiedEntities);
        recognizedEntities.addAll(dictionaryVerifiedEntities);

        return recognizedEntities;
    }

    private EntityList verifyEntitiesWithKB(HashSet<String> entityCandidates) {
        EntityList recognizedEntities = new EntityList();

        for (String entityCandidate : entityCandidates) {
            entityCandidate = StringHelper.trim(entityCandidate);
            CategoryEntries categoryEntries = kbCommunicator.categoryEntriesInKB(entityCandidate);
            if (categoryEntries != null) {
                recognizedEntities.add(new RecognizedEntity(entityCandidate, categoryEntries, 1.0));
            }
        }

        return recognizedEntities;
    }

    private EntityList verifyEntitiesWithPattern(HashSet<String> entityCandidates, String text) {
        EntityList recognizedEntities = new EntityList();

        for (String entityCandidate : entityCandidates) {

            for (Entry<String, CategoryEntries> pattern : patterns.entrySet()) {
                Pattern pat = Pattern.compile(pattern.getKey() + entityCandidate);
                Matcher m = pat.matcher(text);

                if (m.find()) {
                    CategoryEntries categoryEntries = new CategoryEntries();
                    for (CategoryEntry categoryEntry : pattern.getValue()) {
                        categoryEntries.add(categoryEntry);
                    }
                    RecognizedEntity re = new RecognizedEntity(entityCandidate, categoryEntries, 0.8);
                    recognizedEntities.add(re);
                }
            }
        }

        return recognizedEntities;
    }

    private EntityList verifyEntitiesWithDictionary(HashSet<String> entityCandidates, String text) {
        EntityList recognizedEntities = new EntityList();

        for (String entityCandidate : entityCandidates) {

            Pattern pat = Pattern.compile(entityCandidate);
            Matcher m = pat.matcher(text);

            while (m.find()) {
                // String sentence = StringHelper.getSentence(text, m.start());

                String[] windowWords = getWindowWords(text, m.start(), m.end());

                // Category category = dictionary.getMostLikelyCategory(windowWords,0);
                CategoryEntries categories = dictionary.getCategoryEntries(windowWords);
                // TODO use this: recognizedEntities.add(new RecognizedEntity(entityCandidate, categories, category.getRelevance()));
                recognizedEntities.add(new RecognizedEntity(entityCandidate, categories, 0.2));
            }

        }

        return recognizedEntities;
    }

    public KnowledgeBaseCommunicatorInterface getKbCommunicator() {
        return kbCommunicator;
    }

    public void setKbCommunicator(KnowledgeBaseCommunicatorInterface kbCommunicator) {
        this.kbCommunicator = kbCommunicator;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // NERCer nercer1 = new NERCer();
        // nercer1.calculatePatterns();
        // if (true) return;

        // // training can be done with NERCLearner also
        NERCer nercer = new NERCer();
        HashSet<String> trainingTexts = new HashSet<String>();
        trainingTexts.add("Australia is a country and a continent at the same time. New Zealand is also a country but not a continent");
        trainingTexts
                .add("Many countries, such as Germany and Great Britain, have a strong economy. Other countries such as Iceland and Norway are in the north and have a smaller population");
        trainingTexts.add("In south Europe, a nice country named Italy is formed like a boot.");
        trainingTexts.add("In the western part of Europe, the is a country named Spain which is warm.");
        trainingTexts.add("Bruce Willis is an actor, Jim Carrey is an actor too, but Trinidad is a country name and and actor name as well.");
        trainingTexts.add("In west Europe, a warm country named Spain has good seafood.");
        trainingTexts.add("Another way of thinking of it is to drive to another coutry and have some fun.");

        // set the kb communicator that knows the entities
        nercer.setKbCommunicator(new TestKnowledgeBaseCommunicator());

        // train
        nercer.trainRecognizer(trainingTexts);
        nercer.finishTraining();

        // use the trained model to recognize entities in a text
        EntityList recognizedEntities = null;
        recognizedEntities = nercer
                .recognizeEntities("In the north of Europe, there is a country called Sweden which is not far from Norway, there is also a country named Scotland in the north of Europe. But also Denzel Washington is an actor.");

        CollectionHelper.print(recognizedEntities);
    }

}