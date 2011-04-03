package ws.palladian.extraction.keyphrase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.log4j.Logger;

import ws.palladian.classification.WordCorrelation;
import ws.palladian.extraction.keyphrase.ControlledTaggerSettings.TaggingCorrelationType;
import ws.palladian.extraction.keyphrase.ControlledTaggerSettings.TaggingType;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.nlp.Tokenizer;
import ws.palladian.preprocessing.scraping.PageContentExtractor;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * A TF-IDF and tag correlation based tagger using a controlled and weighted vocabulary.
 * 
 * rem: enable assertions for debugging, VM arg -ea
 * 
 * @author Philipp Katz
 * 
 */
public class ControlledTagger extends KeyphraseExtractor {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(ControlledTagger.class);

    /** Contains all index collections. */
    private ControlledTaggerIndex index = new ControlledTaggerIndex();

    /** Represents customizable settings. */
    private ControlledTaggerSettings settings = new ControlledTaggerSettings();

    /** Default constructor. */
    public ControlledTagger() {
    }

    @Override
    public void train(String inputText, Set<String> keyphrases, int index) {

        // remove undesired tags (not matching RegEx).
        Set<String> tags = clean(keyphrases);

        Set<String> stemmedTags = stem(tags);

        this.index.getTagVocabulary().addAll(tags);
        this.index.getStemmedTagVocabulary().addAll(stemmedTags);

        if (inputText.length() > 0) {
            addToIdf(inputText);
        }

        if (settings.getCorrelationType() != TaggingCorrelationType.NO_CORRELATIONS) {
            this.index.getWcm().updateGroup(stemmedTags);
        }

        if (tags.size() > 0) {
            this.index.setTrainCount(this.index.getTrainCount() + 1);
            this.index.setDirtyIndex(true);
        }

    }

    // public void train(String text, Bag<String> tags) {
    //
    // // remove undesired tags (not matching RegEx).
    // tags = clean(tags);
    //
    // Bag<String> stemmedTags = stem(tags);
    //
    // index.getTagVocabulary().addAll(tags.uniqueSet());
    // index.getStemmedTagVocabulary().addAll(stemmedTags.uniqueSet());
    //
    // if (text.length() > 0) {
    // addToIdf(text);
    // }
    //
    // if (settings.getCorrelationType() != TaggingCorrelationType.NO_CORRELATIONS) {
    // index.getWcm().updateGroup(stemmedTags.uniqueSet());
    // }
    //
    // if (tags.size() > 0) {
    // index.setTrainCount(index.getTrainCount() + 1);
    // index.setDirtyIndex(true);
    // }
    //
    // }

    /**
     * We only need those tags which actually match with the provided pattern. The rest can be filtered out in advance.
     */
    private Set<String> clean(Set<String> tags) {
        Set<String> result = new HashSet<String>();
        for (String string : tags) {
            if (settings.getTagMatchPattern().matcher(string).matches()) {
                result.add(string);
            }
        }
        return result;
    }

    /**
     * Allows training, only with text. This can be used to build up an initial IDF index.
     * 
     * @param text
     */
    public void train(String text) {
        train(text, new HashSet<String>(), 0);
    }

    /**
     * Allows training, only with tags. Each Bag of tags is considered as one training instance, e.g. one document. This
     * builds up the tag vocabulary and the tag correlations.
     * 
     * @param tags
     */
    public void train(Set<String> tags) {
        train("", tags, 0);
    }

    @Override
    public void endTraining() {
        save("data/models/controlledTagger.ser");
    }
    
    @Override
    public void startExtraction() {
        load("data/models/controlledTagger.ser");
    }

    // public void trainFromFile(String filePath) {
    //
    // LOGGER.debug("training from file " + filePath);
    //
    // final Counter counter = new Counter();
    // FileHelper.performActionOnEveryLine(filePath, new LineAction() {
    //
    // @Override
    // public void performAction(String line, int lineNumber) {
    // String[] split = line.split("#");
    // String text = split[0];
    // Bag<String> tags = new HashBag<String>();
    // for (int i = 1; i < split.length; i++) {
    // tags.add(split[i]);
    // }
    //
    // train(text, tags);
    // counter.increment();
    // if (counter.getCount() % 10 == 0) {
    // LOGGER.info("added " + counter);
    // }
    // }
    // });
    // index.setDirtyIndex(true);
    // LOGGER.debug("added " + counter + " documents to vocabulary. avg. tag occurence: "
    // + index.getAverageTagOccurence());
    //
    // }

    // public void evaluateFromFile(String filePath, final int limit) {
    //
    // LOGGER.debug("evaluation with file " + filePath);
    //
    // final ControlledTaggerEvaluationResult evaluationResult = new ControlledTaggerEvaluationResult();
    // FileHelper.performActionOnEveryLine(filePath, new LineAction() {
    //
    // @Override
    // public void performAction(String line, int lineNumber) {
    // String[] split = line.split("#");
    // String text = split[0];
    // Bag<String> tags = new HashBag<String>();
    // for (int i = 1; i < split.length; i++) {
    // tags.add(split[i]);
    // }
    // Bag<String> tagsNormalized = normalize(tags);
    //
    // List<Tag> assignedTags = tag(text);
    //
    // int correctlyAssigned = 0;
    // for (Tag assignedTag : assignedTags) {
    // for (String realTag : tagsNormalized.uniqueSet()) {
    // if (assignedTag.getName().equals(realTag)) {
    // correctlyAssigned++;
    // }
    // }
    // }
    //
    // int totalAssigned = assignedTags.size();
    // int realCount = tagsNormalized.uniqueSet().size();
    //
    // double precision = (double) correctlyAssigned / totalAssigned;
    // if (Double.isNaN(precision)) {
    // precision = 0;
    // }
    // double recall = (double) correctlyAssigned / realCount;
    //
    // evaluationResult.addTestResult(precision, recall, totalAssigned);
    //
    // System.out.println("doc: " + StringHelper.getFirstWords(text, 10));
    // System.out.println("real tags: " + tagsNormalized);
    // System.out.println("assigned tags: " + assignedTags);
    // System.out.println("pr:" + precision + " rc:" + recall);
    //
    // if (evaluationResult.getTaggedEntryCount() == limit) {
    // breakLineLoop();
    // }
    //
    // }
    // });
    //
    // LOGGER.info("--------------------------");
    // evaluationResult.printStatistics();
    //
    // }

    /**
     * Updates the index conditionally, if needed. This allows incremental training.
     */
    private void updateIndex() {

        if (index.isDirtyIndex()) {

            LOGGER.info("updating index ...");
            LOGGER.info("# of train documents " + index.getTrainCount());
            LOGGER.info("# of documents in idf " + index.getIdfCount());

            createUnstemMap();
            calculateAverageTagOccurence();

            StopWatch sw = new StopWatch();
            index.getWcm().makeRelativeScores();
            LOGGER.info("created relative scores for wcm in " + sw.getElapsedTimeString());
            LOGGER.info("# of correlations in wcm " + index.getWcm().getCorrelations().size());
            LOGGER.info("... finished updating index");

            index.setDirtyIndex(false);

        }
    }

    private boolean addToIdf(String text) {
        Bag<String> tokens = extractTags(text);
        index.setIdfCount(index.getIdfCount() + 1);
        return index.getIdfIndex().addAll(tokens.uniqueSet());
    }

    /**
     * Returns the inverse-document-frequency for the specified tag.
     * 
     * @param tag
     * @return
     */
    private float getInvDocFreq(String tag) {
        int tagCount = index.getIdfIndex().getCount(tag);
        assert tagCount > 0 : tag + " is not in index, index must be built in advance.";
        float result = (float) Math.log10((float) index.getIdfCount() / (tagCount + 1));
        return result;
    }

    // @Deprecated
    // public void addToVocabulary(String tag) {
    // index.getTagVocabulary().add(tag);
    // // calculateAverageTagOccurence();
    // index.setDirtyIndex(true);
    // }

    // @Deprecated
    // public int addToVocabulary(Collection<String> tags) {
    // int addCount = 0;
    // for (String tag : tags) {
    // if (index.getTagVocabulary().add(tag)) {
    // addCount++;
    // }
    // }
    // // calculateAverageTagOccurence();
    // index.setDirtyIndex(true);
    // LOGGER.debug("added " + addCount + " tags to vocabulary.");
    // return addCount;
    // }

    /**
     * Add controlled tagging vocabulary from text file. One line per tag + count. E.g.:
     * 
     * design#26693
     * reference#25222
     * tools#24470
     * ...
     * 
     * @param filePath
     * @return
     * @deprecated use {@link #train(String, Bag)} instead.
     */
    // @Deprecated
    // public int addToVocabularyFromFile(String filePath) {
    // final int[] addCount = new int[] { 0 };
    // FileHelper.performActionOnEveryLine(filePath, new LineAction() {
    //
    // @Override
    // public void performAction(String line, int lineNumber) {
    // String[] split = line.split("#");
    // String tagName = split[0];
    // int tagCount = Integer.valueOf(split[1]);
    // if (index.getTagVocabulary().add(tagName, tagCount)) {
    // addCount[0]++;
    // }
    // }
    // });
    // // calculateAverageTagOccurence();
    // index.setDirtyIndex(true);
    // LOGGER.debug("added " + addCount[0] + " tags to vocabulary. avg. tag occurence: "
    // + index.getAverageTagOccurence());
    // return addCount[0];
    // }

    /**
     * Re(calculates) the average occurence over all tags in the tag vocabulary. This value is used to boost tags with
     * high occurences.
     */
    private void calculateAverageTagOccurence() {
        // float result = 0;
        // for (String tag : index.getStemmedTagVocabulary().uniqueSet()) {
        // result += index.getStemmedTagVocabulary().getCount(tag);
        // }
        Bag<String> stemmedVocabulary = index.getStemmedTagVocabulary();
        int result = stemmedVocabulary.size() / stemmedVocabulary.uniqueSet().size();
        LOGGER.info("average tag occurence " + result);
        index.setAverageTagOccurence(result);
    }

    /**
     * Determine, if a stemmed tag is in the vocabulary and is not a stopword.
     * 
     * @param stemmedTag
     * @return
     */
    protected boolean isAcceptedTag(String stemmedTag) {

        boolean isAccepted = index.getStemmedTagVocabulary().contains(stemmedTag);
        isAccepted = isAccepted && !settings.getStopwords().contains(stemmedTag);
        isAccepted = isAccepted && settings.getTagMatchPattern().matcher(stemmedTag).matches();

        return isAccepted;

    }

    /**
     * Extract tag candidates from the supplied text. Tag candidates are the tokens from the controlled vocabulary which
     * occur in the text.
     * 
     * @param text
     * @return
     */
    protected Bag<String> extractTags(String text) {
        Bag<String> extractedTags = new HashBag<String>();

        Bag<String> tokens = tokenize(text);
        Set<String> stems = stem(tokens.uniqueSet());

        for (String stem : stems) {
            if (isAcceptedTag(stem)) {
                extractedTags.add(stem); // , stems.getCount(stem));
            }
        }

        return extractedTags;
    }

    /**
     * Tokenize text by creating uni/bi/tri grams. bi/tri grams allow us to assign tags like "losangeles", when the term
     * "Los Angeles" occurs in the tag.
     * 
     * @param text
     * @return
     */
    protected Bag<String> tokenize(String text) {
        String toTokenize = text.toLowerCase();

        Bag<String> tokens = new HashBag<String>();

        tokens.addAll(Tokenizer.tokenize(toTokenize)); // unigrams
        for (int i = 2; i <= settings.getPhraseLength(); i++) { // bi-/tri-/...-grams
            tokens.addAll(tokenizeGrams(toTokenize, i));
        }

        return tokens;
    }

    /**
     * Special n-gram implementation, cannot use the one provided in {@link Tokenizer#calculateWordNGrams(String, int)}
     * as I need the individual counts of each gram.
     * 
     * @param text
     * @param size
     * @return
     */
    private List<String> tokenizeGrams(String text, int size) {
        List<String> grams = new ArrayList<String>();
        List<String> tokens = Tokenizer.tokenize(text);
        String[] tokenArray = tokens.toArray(new String[0]);
        for (int i = 0; i < tokenArray.length - size + 1; i++) {
            StringBuilder gram = new StringBuilder();
            for (int j = i; j < i + size; j++) {
                gram.append(tokenArray[j]);
            }
            if (gram.length() > 0) {
                grams.add(gram.toString());
            }
        }
        return grams;
    }

    /**
     * Assignes (stemmed) tags to the supplied text.
     * 
     * @param text
     * @return
     */
    private List<Keyphrase> assignTags(String text) {

        Bag<String> extractedTags = extractTags(text);
        List<Keyphrase> assignedTags = new LinkedList<Keyphrase>();

        // number of all, non-unique tags we extracted
        int totalTagCount = extractedTags.size();

        // calculate TF-IDF for every unique tag
        for (String tag : extractedTags.uniqueSet()) {

            float termFreq = (float) extractedTags.getCount(tag) / totalTagCount;
            float invDocFreq = getInvDocFreq(tag);
            float termFreqInvDocFreq = termFreq * invDocFreq;

            assert !Float.isInfinite(termFreqInvDocFreq) && !Float.isNaN(termFreqInvDocFreq);

            // LOGGER.trace(tag + " : TF=" + termFreq + " IDF=" + invDocFreq + " TFIDF=" + termFreqInvDocFreq);
            assignedTags.add(new Keyphrase(tag, termFreqInvDocFreq));

        }

        // sort the list
        Collections.sort(assignedTags, new KeyphraseComparator());

        LOGGER.trace("ranked tags: " + assignedTags);

        // when using tag correlations, do the re-ranking
        // reRanking only makes sense, if we have at least two tags :)
        if (assignedTags.size() > 1) {
            reRankTags(assignedTags);
        }

        LOGGER.trace("re-ranked tags:" + assignedTags);

        // create final tag result set
        // 1) either by removing those tags which are under the defined threshold
        if (settings.getTaggingType() == TaggingType.THRESHOLD) {
            limitToWeight(assignedTags, settings.getTfidfThreshold());
        }

        // 2) or by keeping a fixed number of top tags.
        else if (settings.getTaggingType() == TaggingType.FIXED_COUNT) {
            limitToCount(assignedTags, settings.getTagCount());
        }

        LOGGER.trace("final tags:" + assignedTags);
        LOGGER.trace("---------------------");
        return assignedTags;
    }

    /**
     * Limit the *sorted* list of tags to the specified count.
     * 
     * @param tags list of tags to limit, must be sorted with descending weights.
     * @param limit
     */
    private void limitToCount(List<Keyphrase> tags, int limit) {
        if (tags.size() > limit) {
            tags.subList(limit, tags.size()).clear();
        }
    }

    /**
     * Limit the list of tags by removing those below specified minWeight.
     * 
     * @param tags
     * @param limit
     */
    private void limitToWeight(List<Keyphrase> tags, float minWeight) {
        ListIterator<Keyphrase> iterator = tags.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next().getWeight() < minWeight) {
                iterator.remove();
            }
        }
    }

    /**
     * Do a re-ranking based on priors + tag correlations.
     * 
     * @param assignedTags in/out: assigned tags, sorted by relevance, descending.
     */
    private void reRankTags(List<Keyphrase> assignedTags) {

        StopWatch sw = new StopWatch();

        // experimental: to normalize the range of the re-ranked tags back to their original range,
        // by keeping the lower/upper bounds, so we keep the general properties of the TF/IDF -- elsewise
        // we will get outliers which are considerably bigger than most of the other tag weights.
        double oldMin = assignedTags.get(0).getWeight();
        double oldMax = assignedTags.get(assignedTags.size() - 1).getWeight();

        // do the prior-based re-ranking
        if (settings.getPriorWeight() != -1) {

            for (Keyphrase tag : assignedTags) {
                int count = index.getStemmedTagVocabulary().getCount(tag.getValue());
                float priorBoost = (float) Math.log10(1.0 + settings.getPriorWeight() * count
                        / index.getAverageTagOccurence()) + 1;
                assert !Float.isInfinite(priorBoost) && !Float.isNaN(priorBoost);
                double weight = tag.getWeight() * priorBoost;
                tag.setWeight(weight);

            }

        }

        Collections.sort(assignedTags, new KeyphraseComparator());

        // Option 1: do a "shallow" re-ranking, only considering top-tag (n)
        float correlationWeight = settings.getCorrelationWeight();
        if (settings.getCorrelationType() == TaggingCorrelationType.SHALLOW_CORRELATIONS) {
            Iterator<Keyphrase> tagIterator = assignedTags.iterator();
            Keyphrase topTag = tagIterator.next();

            while (tagIterator.hasNext()) {
                Keyphrase currentTag = tagIterator.next();

                WordCorrelation correlation = index.getWcm().getCorrelation(topTag.getValue(), currentTag.getValue());
                if (correlation != null) {
                    double newWeight = currentTag.getWeight() + correlationWeight * correlation.getRelativeCorrelation();
                    currentTag.setWeight(newWeight);
                }
            }
        }

        // Option 2: do a "deep" re-ranking, considering correlations between each possible combination
        else if (settings.getCorrelationType() == TaggingCorrelationType.DEEP_CORRELATIONS) {
            Keyphrase[] tagsArray = assignedTags.toArray(new Keyphrase[assignedTags.size()]);

            // experimental:
            // normalization factor; we have (n - 1) + (n - 2) + ... + 1 = n * (n - 1) / 2 re-rankings.
            int numReRanking = tagsArray.length * (tagsArray.length - 1) / 2;

            for (int i = 0; i < tagsArray.length; i++) {
                Keyphrase outerTag = tagsArray[i];
                for (int j = i; j < tagsArray.length; j++) {
                    Keyphrase innerTag = tagsArray[j];
                    WordCorrelation correlation = index.getWcm().getCorrelation(outerTag.getValue(), innerTag.getValue());
                    if (correlation != null) {
                        float reRanking = (float) (correlationWeight / numReRanking * correlation
                                .getRelativeCorrelation());
                        // FIXME why dont we put the numReRanking division outside the loop?

                        assert !Double.isInfinite(reRanking) && !Double.isNaN(reRanking);
                        innerTag.setWeight(innerTag.getWeight() + reRanking);
                        outerTag.setWeight(outerTag.getWeight() + reRanking);
                    }
                }
            }

        }

        // re-sort the list, as ranking weights have changed
        Collections.sort(assignedTags, new KeyphraseComparator());

        // do the scaling back to the original range (see comment above)
        double newMin = assignedTags.get(0).getWeight();
         double newMax = assignedTags.get(assignedTags.size() - 1).getWeight();

         if (newMin != newMax) { // avoid division by zero
            for (Keyphrase tag : assignedTags) {

                // http://de.wikipedia.org/wiki/Normalisierung_(Mathematik)
                double normalizedWeight = (tag.getWeight() - newMin) * (oldMax - oldMin) / (newMax - newMin) + oldMin;
                tag.setWeight(normalizedWeight);

            }
         }

        LOGGER.trace("correlation reranking for " + assignedTags.size() + " in " + sw.getElapsedTimeString());

    }

    @Override
    public List<Keyphrase> extract(String inputText) {

        addToIdf(inputText);
        updateIndex();

        List<Keyphrase> assignedTags = assignTags(inputText);
        unstem(assignedTags);
        return assignedTags;

    }

//    /**
//     * Tag the supplied text.
//     * 
//     * @param text
//     * @return Array with assigned Tags, sorted by weight or empty List. Never <code>null</code>.
//     */
//    public List<Tag> tag(String text) {
//
//        addToIdf(text);
//        updateIndex();
//
//        List<Tag> assignedTags = assignTags(text);
//        unstem(assignedTags);
//        return assignedTags;
//    }
//
//    public Map<String, List<Tag>> tag(Collection<String> texts) {
//        Map<String, List<Tag>> result = new HashMap<String, List<Tag>>();
//        for (String text : texts) {
//            addToIdf(text);
//        }
//        for (String text : texts) {
//            result.put(text, tag(text));
//        }
//        return result;
//    }

    /**
     * Get rid of unnecessary plural tags. If we have the singular form in the vocabulary, we drop the plural, adding
     * the counts to the singular form. Using the vocabulary has the advantage that we avoid nonsense conversions like
     * "mercedes" -> "mercede".
     * 
     * replaced this with general stemming + stem map
     * 
     * @param unnormalized
     * @return
     */
    // Bag<String> singularPluralNormalization(Bag<String> unnormalized) {
    //
    // Bag<String> normalized = new HashBag<String>();
    //
    // // singular/plural normalization
    // for (String tag : unnormalized.uniqueSet()) {
    // int tagCount = unnormalized.getCount(tag);
    // String singularTag = WordTransformer.wordToSingular(tag);
    // if (isInVocabulary(singularTag)) {
    // normalized.add(singularTag, tagCount);
    // } else {
    // normalized.add(tag, tagCount);
    // }
    // }
    //
    // // experimental -- use David's tag normalization, for a little P/R gain.
    // Bag<String> normalized2 = new HashBag<String>();
    // for (String tag : normalized.uniqueSet()) {
    // int tagCount = normalized.getCount(tag);
    // String normalize = DeliciousCrawler.normalizeTag(tag);
    // normalized2.add(normalize, tagCount);
    // }
    //
    // // add gerundium normalization using WordNet.
    // Bag<String> normalized3 = new HashBag<String>();
    // for (String tag : normalized2.uniqueSet()) {
    // int tagCount = normalized2.getCount(tag);
    // String normalize = GerundNormalizer.gerundToInfinitive(tag);
    // normalized3.add(normalize, tagCount);
    // }
    //
    // return normalized;
    // return normalized2;
    // return normalized3;
    // }

    /**
     * Creates the stemmed tag vocabulary. This contains the stemmed forms of all tags in the vocabulary.
     */
    // private void createStemmedVocabulary() {
    //
    // stemmedTagVocabulary = new HashBag<String>();
    //
    // for (String tag : tagVocabulary.uniqueSet()) {
    // String stemmedTag = stem(tag);
    // int tagCount = tagVocabulary.getCount(tag);
    // stemmedTagVocabulary.add(stemmedTag, tagCount);
    // }
    //
    // LOGGER.info("# of tags in unstemmed vocabulary : " + tagVocabulary.uniqueSet().size());
    // LOGGER.info("# of tags in stemmed vocabulary : " + stemmedTagVocabulary.uniqueSet().size());
    // }

    /**
     * Creates the unstemMap. This map is used to transform the stemmed tags, which we use internally back to their
     * nice, readable representations. The stemmer creates tags like "softwar" or "appl". The unstemMap contains the
     * stems and their most common, popular unstemmed form, which is determined from the count in the vocabulary.
     * 
     * Example: The vocabulary contains the tag "iphone" with count 50 and "iphones" with count 25. Both of them are
     * crippled to "iphon" by the stemmer. The unstemMap would contain (iphon > iphone), as "iphone" has a higher count
     * in the vocabulary than "iphones".
     */
    private void createUnstemMap() {

        // temporary map which keeps (stemmedTag, [unstemmedTag1, unstemmedTag2, ...])
        Map<String, List<String>> stemUnstemMap = new HashMap<String, List<String>>();

        // iterate through the vocabulary, fill temporary map
        for (String tag : index.getTagVocabulary().uniqueSet()) {

            String stemmedTag = stem(tag);

            List<String> unstemmedTags = stemUnstemMap.get(stemmedTag);
            if (unstemmedTags == null) {
                unstemmedTags = new ArrayList<String>();
                stemUnstemMap.put(stemmedTag, unstemmedTags);
            }

            unstemmedTags.add(tag);
        }

        // create a new unstemMap
        index.setUnstemMap(new HashMap<String, String>());

        // iterate through the temporary map from above and pick unstemmedTag with highest count
        for (Entry<String, List<String>> entry : stemUnstemMap.entrySet()) {

            String stemmedTag = entry.getKey();
            int bestCount = 0;
            String bestCandidate = null;

            for (String unstemmedTag : entry.getValue()) {
                int count = index.getTagVocabulary().getCount(unstemmedTag);
                if (count > bestCount) {
                    bestCandidate = unstemmedTag;
                    bestCount = count;
                }
            }

            // we only need to keep those mappings where the stemmed form is different from the unstemmed
            if (!bestCandidate.equals(stemmedTag)) {
                index.getUnstemMap().put(entry.getKey(), bestCandidate);
            }
        }
    }

    /**
     * Stem with Snowball.
     * 
     * @param unstemmed
     * @return
     */
    protected String stem(String unstemmed) {
        settings.getStemmer().setCurrent(unstemmed);
        settings.getStemmer().stem();
        String stem = settings.getStemmer().getCurrent();
        return stem;
    }

    /**
     * Stem with Snowball.
     * 
     * @param unstemmed
     * @return
     */
    private Set<String> stem(Set<String> unstemmed) {
        Set<String> stemmed = new HashSet<String>();
        for (String tag : unstemmed) {
            // int tagCount = unstemmed.getCount(tag);
            String tagStemmed = stem(tag);
            // stemmed.add(tagStemmed, tagCount);
            stemmed.add(tagStemmed);
        }
        return stemmed;
    }

    private String unstem(String stemmed) {
        String unstem = index.getUnstemMap().get(stemmed);
        if (unstem == null) {
            unstem = stemmed;
        }
        return unstem;
    }

    private void unstem(List<Keyphrase> stemmedTags) {
        for (Keyphrase tag : stemmedTags) {
            String unstemmed = unstem(tag.getValue());
            tag.setValue(unstemmed);
        }
    }

    /**
     * Normalize a list of Tags according to the stemming rules. We need this for the evaluation process, as the the
     * test Tags need to be normalized the same way.
     * 
     * @param tags
     */
    public Bag<String> normalize(Bag<String> tags) {
        Bag<String> normalizedTags = new HashBag<String>();
        for (String tag : tags.uniqueSet()) {
            String normalizedTag = unstem(stem(tag));
            int tagCount = tags.getCount(tag);
            normalizedTags.add(normalizedTag, tagCount);
        }
        return normalizedTags;
    }

    /**
     * Set the fast mode. This is only relevant when using correlations,
     * {@link TaggingCorrelationType#SHALLOW_CORRELATIONS} or {@link TaggingCorrelationType#DEEP_CORRELATIONS}. When
     * enabled, the potentially huge list of tag candidates will be pruned, before checking their correlations, which is
     * computationally very expensive. The pruned list should still be big enough to account for re-rankings through
     * correlations; I checked this extensively and set the pruned list size very conservatively. Anyway, to be
     * absolutely sure, not to lose any accuracy, we can disable the pruning here. In my experiments, the tagging
     * process took about 20 times longer whitout this optimization.
     * 
     * @param fastMode
     */
    // public void setFastMode(boolean fastMode) {
    // this.fastMode = fastMode;
    // }

    public ControlledTaggerSettings getSettings() {
        return settings;
    }

    public void setSettings(ControlledTaggerSettings settings) {
        this.settings = settings;
    }

    public ControlledTaggerIndex getIndex() {
        return index;
    }

    public void setIndex(ControlledTaggerIndex index) {
        this.index = index;
    }

    /**
     * Serialize this tagger to disk.
     * 
     * @param filePath
     */
    public void save(String filePath) {
        updateIndex();
        StopWatch sw = new StopWatch();
        FileHelper.serialize(index, filePath);
        LOGGER.info("saved index to " + filePath + " in " + sw.getElapsedTimeString());
    }

    /**
     * Load safed tagger index {@link ControlledTaggerIndex} from disk.
     */
    public void load(String filePath) {
        StopWatch sw = new StopWatch();
        LOGGER.info("loading index from " + filePath);
        ControlledTaggerIndex index = FileHelper.deserialize(filePath);
        if (index == null) {
            LOGGER.error("could not read from " + filePath + ", starting with new instance.");
        } else {
            LOGGER.info("loaded index from " + filePath + " in " + sw.getElapsedTimeString());
            this.index = index;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ControlledTagger");
        sb.append("\n").append(index);
        sb.append("\n").append(settings);

        return sb.toString();
    }

    /**
     * Write some statistical information concerning the index.
     */
    public void writeDataToReport() {
        // write IDF index
        // StringBuilder sb = new StringBuilder();
        // for (String tag : index.getIdfIndex().uniqueSet()) {
        // sb.append(tag).append("#");
        // sb.append(index.getIdfIndex().getCount(tag));
        // sb.append("\n");
        // }
        // FileHelper.writeToFile("data/temp/idf_index.txt", sb);
        // write stemmed vocabulary

        Map<String, Integer> idfMap = new HashMap<String, Integer>();
        for (String stemmedTag : index.getIdfIndex().uniqueSet()) {
            String tagName = unstem(stemmedTag);
            int tagCount = index.getIdfIndex().getCount(stemmedTag);
            idfMap.put(tagName, tagCount);
        }

        LinkedHashMap<String, Integer> idfMapSorted = CollectionHelper.sortByValue(idfMap.entrySet(), false);

        StringBuilder idfBuilder = new StringBuilder();
        for (Entry<String, Integer> tagEntry : idfMapSorted.entrySet()) {
            idfBuilder.append(tagEntry.getKey()).append("#");
            idfBuilder.append(tagEntry.getValue());
            idfBuilder.append("\n");
        }

        // write controlled vocabulary
        Map<String, Integer> tagCountMap = new HashMap<String, Integer>();
        for (String stemmedTag : index.getStemmedTagVocabulary().uniqueSet()) {
            String tagName = unstem(stemmedTag);
            int tagCount = index.getStemmedTagVocabulary().getCount(stemmedTag);
            tagCountMap.put(tagName, tagCount);
        }

        LinkedHashMap<String, Integer> sorted = CollectionHelper.sortByValue(tagCountMap.entrySet(), false);

        StringBuilder sb = new StringBuilder();
        for (Entry<String, Integer> tagEntry : sorted.entrySet()) {
            sb.append(tagEntry.getKey()).append("#");
            sb.append(tagEntry.getValue());
            sb.append("\n");
        }

        // sb = new StringBuilder();
        // for (String tag : index.getStemmedTagVocabulary().uniqueSet()) {
        // sb.append(tag).append("#");
        // sb.append(index.getStemmedTagVocabulary().getCount(tag));
        // sb.append("\n");
        // }
        FileHelper.writeToFile("data/temp/idf_index.txt", idfBuilder);
        FileHelper.writeToFile("data/temp/vocabulary_index.txt", sb);
    }

    public static void main(String[] args) throws Exception {

        // ControlledTagger tagger = ControlledTagger.load("data/controlledTagger20000_David_idf.ser");

        // ///////////////////// usage example for documentation ///////////////////////////////

        // set up the ControlledTagger
        final ControlledTagger tagger = new ControlledTagger();

        /*
         * tagger.trainFromFile("/Users/pk/Dropbox/tmp/tagData_shuf_10000aa");
         * tagger.save("data/controlledTagger.ser");
         * System.exit(0);
         */

        tagger.load("data/controlledTagger.ser");
        // tagger.evaluateFromFile("/Users/pk/Dropbox/tmp/tagData_shuf_10000ab", 1000);

        DocumentRetriever c = new DocumentRetriever();
        String result = c.download("http://www.i-funbox.com/");
        result = HTMLHelper.documentToReadableText(result, true);
        tagger.getSettings().setTagCount(20);
        List<Keyphrase> extract = tagger.extract(result);
        System.out.println(extract);

        System.exit(0);

        // all tagging parameters are encapsulated by ControlledTaggerSettings
        // ControlledTaggerSettings taggerSettings = tagger.getSettings();
        // taggerSettings.setCorrelationType(TaggingCorrelationType.NO_CORRELATIONS);

        // create a DeliciousDatasetReader + Filter for training
        /*
         * DeliciousDatasetReader reader = new DeliciousDatasetReader();
         * DatasetFilter filter = new DatasetFilter();
         * filter.addAllowedFiletype("html");
         * filter.setMinUsers(50);
         * filter.setMaxFileSize(600000);
         * reader.setFilter(filter);
         * // train the tagger with 20.000 train documents from the dataset
         * DatasetCallback callback = new DatasetCallback() {
         * @Override
         * public void callback(DatasetEntry entry) {
         * String content = FileHelper.readFileToString(entry.getPath());
         * content = HTMLHelper.htmlToString(content, true);
         * tagger.train(content, entry.getTags());
         * }
         * };
         * reader.read(callback, 20000);
         * // save the model for later usage
         * tagger.save("data/models/controlledTaggerModel2.ser");
         * // ControlledTagger tagger = new ControlledTagger();
         * // tagger.addToVocabularyFromFile("data/delicious_tags_t140.txt");
         * // tagger.createUnstemMap();
         * // // tagger.stemVocabulary();
         * // System.out.println(tagger.unstem("liberti"));
         * System.exit(0);
         */

        // load the trained model, this takes some time.
        // if you will tag multiple documents, be sure to move this outside the loop!
        tagger.load("data/models/controlledTaggerModel2.ser");

        // assign tags according to a web page's content
        PageContentExtractor extractor = new PageContentExtractor();
        String content = extractor
                .getResultText("http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars");
        List<Keyphrase> assignedTags = tagger.extract(content);

        // print the assigned tags
        CollectionHelper.print(assignedTags);

        // Bag<String> test = new HashBag<String>(Arrays.asList(new String[] { "car", "car", "car", "car",
        // "graphicdesign", "cars", "mercedes" }));
        // Bag<String> norm = tagger.singularPluralNormalization(test);
        // System.out.println(norm);

        System.exit(0);

        System.out.println(tagger.tokenize("the quick brown fox fox."));
        // System.out.println(tagger.tokenizeGrams("the quick brown fox", 2));

        System.exit(0);

        // String d1 = "If it walks like a duck and quacks like a duck, it must be a duck.";
        // String d2 =
        // "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin.";
        // String d3 =
        // "Bugs' ascension to stardom also prompted the Warner animators to recast Daffy Duck as the rabbit's rival, intensely jealous and determined to steal back the spotlight while Bugs remained indifferent to the duck's jealousy, or used it to his advantage. This turned out to be the recipe for the success of the duo.";
        // String d4 =
        // "6:25 PM 1/7/2007 blog entry: I found this great recipe for Rabbit Braised in Wine on cookingforengineers.com.";
        // // String d5 =
        // //
        // "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipies for Jiaozi.";
        // String d5 =
        // "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipe for Jiaozi.";
        //
        // String[] tags = new String[] { "beijing", "dish", "duck", "rabbit", "recipe", "roast" };

        // tagger.addToVocabulary(Arrays.asList(tags));
        // tagger.tag(Arrays.asList(new String[] { d1, d2, d3, d4, d5 }));

    }

    @Override
    public String getExtractorName() {
        return "ControlledTagger";
    }

    @Override
    public boolean needsTraining() {
        return true;
    }

}

class KeyphraseComparator implements Comparator<Keyphrase> {

    private short sign = 1;

    /**
     * Create new descending TagComparator.
     */
    public KeyphraseComparator() {
        this(true);
    }

    /**
     * Create new TagComparator.
     * 
     * @param descending if true, Tags are sorted descendingly by their weights, false ascendingly.
     */
    public KeyphraseComparator(boolean descending) {
        if (descending) {
            sign = -1;
        }
    }

    @Override
    public int compare(Keyphrase t1, Keyphrase t2) {
        return new Double(t1.getWeight()).compareTo(t2.getWeight()) * sign;
    }

    
}
