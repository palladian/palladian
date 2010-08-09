package tud.iir.classification.controlledtagging;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.log4j.Logger;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.FastWordCorrelationMatrix;
import tud.iir.classification.WordCorrelation;
import tud.iir.classification.WordCorrelationMatrix;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StopWatch;
import tud.iir.helper.Tokenizer;

/**
 * A TF-IDF and tag correlation based tagger using a controlled and weighted vocabulary.
 * 
 * rem: enable assertions for debugging, VM arg -ea
 * 
 * @author Philipp Katz
 * 
 */
public class ControlledTagger implements Serializable {

    private static final long serialVersionUID = -6610563240124430257L;

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(ControlledTagger.class);

    // //////// enumarations for settings ///////////

    public enum TaggingType {
        THRESHOLD, FIXED_COUNT
    }

    public enum TaggingCorrelationType {
        NO_CORRELATIONS, SHALLOW_CORRELATIONS, DEEP_CORRELATIONS
    }

    // //////// default settings ///////////

    private static final float DEFAULT_TFIDF_THRESHOLD = 0.005f;

    private static final int DEFAULT_TAG_COUNT = 10;

    private static final float DEFAULT_CORRELATION_WEIGHT = 50.0f;

    private static final float DEFAULT_PRIOR_WEIGHT = 1.0f;

    // //////// index collections ///////////

    /** Index over all documents with tags, to calculate IDF. Counts how many documents contain a specific tag. */
    private Bag<String> idfIndex = new HashBag<String>();

    /** The controlled vocabulary with all available tags. */
    private Bag<String> tagVocabulary = new HashBag<String>();

    /** The controlled vocabulary with all available tags, in stemmed form. */
    private Bag<String> stemmedTagVocabulary = new HashBag<String>();

    /** Map with stemmed tags and their most common, unstemmed form. For example: softwar > software */
    private Map<String, String> unstemMap = new HashMap<String, String>();

    /** Number of documents in the idf index. */
    private int idfCount = 0;

    /** Number of documents with which the tagger was trained. */
    private int trainCount = 0;

    /** Average occurence a tag in the controlled vocabulary has been assigned. */
    private float averageTagOccurence = 0;

    /**
     * The WordCorrelationMatrix keeps correlations between pairs of tags from the vocabulary to improve tagging
     * accuracy.
     */
    // private WordCorrelationMatrix wcm = new WordCorrelationMatrix();
    private WordCorrelationMatrix wcm = new FastWordCorrelationMatrix();

    /** Flag to indicate that index has changed and data, like stems and correlations have to be re-calculated. */
    private boolean dirtyIndex = false;

    // //////// customizable settings ///////////
    // see their corresponding setters for documentation.

    private TaggingType taggingType = TaggingType.THRESHOLD;

    private TaggingCorrelationType correlationType = TaggingCorrelationType.NO_CORRELATIONS;

    private float tfidfThreshold = DEFAULT_TFIDF_THRESHOLD;

    private int tagCount = DEFAULT_TAG_COUNT;

    private float correlationWeight = DEFAULT_CORRELATION_WEIGHT;

    private float priorWeight = DEFAULT_PRIOR_WEIGHT;

    private boolean fastMode = true;

    // //////// misc. ///////////

    /** The Stemmer. This is not serializable and must be re-created opun de-serialization, see {@link #setup()}. */
    private transient SnowballStemmer stemmer;

    // //////// constuctor /////////

    /** Default constructor. */
    public ControlledTagger() {
        setup();
    }

    // //////// methods ////////////

    private void setup() {
        // re-create the stemmer, which is transient, on initialization/deserialization
        stemmer = new englishStemmer();
    }

    public void train(String text, Bag<String> tags) {

        Bag<String> stemmedTags = stem(tags);

        tagVocabulary.addAll(tags);
        stemmedTagVocabulary.addAll(stemmedTags);

        addToIndex(text);

        if (correlationType != TaggingCorrelationType.NO_CORRELATIONS) {
            // addToWcm(stemmedTags);
            addToWcm(stemmedTags.uniqueSet());
        }

        trainCount++;
        dirtyIndex = true;

    }

    /**
     * Add a list of tags to the WordCorrelationMatrix.
     * 
     * TODO change from Bag to List/Collection.
     * 
     * @param tags
     */
    // private void addToWcm(Bag<String> tags) {
    private void addToWcm(Set<String> tags) {
        String[] tagArray = tags.toArray(new String[0]);

        for (int i = 0; i < tagArray.length; i++) {
            for (int j = i + 1; j < tagArray.length; j++) {
                wcm.updatePair(tagArray[i], tagArray[j]);
            }
        }
    }

    /**
     * Updates the index conditionally, if needed. This allows incremental training.
     */
    private void updateIndex() {

        if (dirtyIndex) {

            LOGGER.info("updating index ...");
            LOGGER.info("# of train documents " + trainCount);
            LOGGER.info("# of documents in idf " + idfCount);

            createUnstemMap();
            calculateAverageTagOccurence();

            StopWatch sw = new StopWatch();
            wcm.makeRelativeScores();
            LOGGER.info("created relative scores for wcm in " + sw.getElapsedTimeString());
            LOGGER.info("# of correlations in wcm " + wcm.getCorrelations().size());
            LOGGER.info("... finished updating index");

            dirtyIndex = false;

        }
    }

    private boolean addToIndex(String text) {
        Bag<String> tokens = extractTags(text);
        idfCount++;
        return idfIndex.addAll(tokens.uniqueSet());
    }

    /**
     * Returns the inverse-document-frequency for the specified tag.
     * 
     * @param tag
     * @return
     */
    private float getInvDocFreq(String tag) {
        int tagCount = idfIndex.getCount(tag);
        // assert tagCount > 0 : tag + " is not in index, index must be built in advance.";
        float result = (float) Math.log10((float) idfCount / (tagCount + 1));
        return result;
    }

    @Deprecated
    public void addToVocabulary(String tag) {
        tagVocabulary.add(tag);
        // calculateAverageTagOccurence();
        dirtyIndex = true;
    }

    @Deprecated
    public int addToVocabulary(Collection<String> tags) {
        int addCount = 0;
        for (String tag : tags) {
            if (tagVocabulary.add(tag)) {
                addCount++;
            }
        }
        // calculateAverageTagOccurence();
        dirtyIndex = true;
        LOGGER.debug("added " + addCount + " tags to vocabulary.");
        return addCount;
    }

    /**
     * Add controlled tagging vocabulary from text file. One line per tag + count. E.g.:
     * 
     * design#26693
     * reference#25222
     * tools#24470
     * ...
     * 
     * TODO use a different separator character, else we can not tag C#
     * TODO remodel this for DeliciousCrawler files.
     * 
     * @param filePath
     * @return
     * @deprecated use {@link #train(String, Bag)} instead.
     */
    @Deprecated
    public int addToVocabularyFromFile(String filePath) {
        final int[] addCount = new int[] { 0 };
        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("#");
                String tagName = split[0];
                int tagCount = Integer.valueOf(split[1]);
                if (tagVocabulary.add(tagName, tagCount)) {
                    addCount[0]++;
                }
            }
        });
        // calculateAverageTagOccurence();
        dirtyIndex = true;
        LOGGER.debug("added " + addCount[0] + " tags to vocabulary. avg. tag occurence: " + averageTagOccurence);
        return addCount[0];
    }

    /**
     * Re(calculates) the average occurence over all tags in the tag vocabulary. This value is used to boost tags with
     * high occurences.
     */
    private void calculateAverageTagOccurence() {
        float result = 0;
        for (String tag : stemmedTagVocabulary.uniqueSet()) {
            result += stemmedTagVocabulary.getCount(tag);
        }
        result /= stemmedTagVocabulary.uniqueSet().size();
        LOGGER.info("average tag occurence " + result);
        averageTagOccurence = result;
    }

    /**
     * Determine, if a stemmed tag is in the vocabulary.
     * 
     * @param tag
     * @return
     */
    private boolean isInVocabulary(String tag) {
        return stemmedTagVocabulary.contains(tag);
    }

    /**
     * Extract tag candidates from the supplied text. Tag candidates are the tokens from the controlled vocabulary which
     * occur in the text.
     * 
     * @param text
     * @return
     */
    private Bag<String> extractTags(String text) {
        Bag<String> extractedTags = new HashBag<String>();

        Bag<String> tokens = tokenize(text);
        Bag<String> stems = stem(tokens);

        for (String stem : stems.uniqueSet()) {
            if (isInVocabulary(stem)) {
                extractedTags.add(stem, stems.getCount(stem));
            }
        }

        // extractedTags = singularPluralNormalization(extractedTags);
        return extractedTags;
    }

    /**
     * Tokenize text by creating uni/bi/tri grams. bi/tri grams allow us to assign tags like "losangeles", when the term
     * "Los Angeles" occurs in the tag.
     * 
     * @param text
     * @return
     */
    private Bag<String> tokenize(String text) {
        String toTokenize = text.toLowerCase();

        Bag<String> tokens = new HashBag<String>();

        tokens.addAll(Tokenizer.tokenize(toTokenize));
        tokens.addAll(tokenizeGrams(toTokenize, 2));
        tokens.addAll(tokenizeGrams(toTokenize, 3));

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
    private List<Tag> assignTags(String text) {

        Bag<String> extractedTags = extractTags(text);
        List<Tag> assignedTags = new LinkedList<Tag>();

        // number of all, non-unique tags we extracted
        int totalTagCount = extractedTags.size();

        // calculate TF-IDF for every unique tag
        for (String tag : extractedTags.uniqueSet()) {

            float termFreq = (float) extractedTags.getCount(tag) / totalTagCount;
            float invDocFreq = getInvDocFreq(tag);
            float termFreqInvDocFreq = termFreq * invDocFreq;

            // prior boosting will boost/weaken the tf-idf values based on the relative count of occurences
            // of this particular tag in the controlled vocabulary. The underlying assumption is, that the probability
            // of correctly assigning a tag which is "popular" is higher.
            // if (usePriors) {
            if (priorWeight != -1) {
                float priorBoost = (float) Math.log10(1.0 + stemmedTagVocabulary.getCount(tag) / averageTagOccurence);
                assert !Float.isInfinite(priorBoost) && !Float.isNaN(priorBoost);
                termFreqInvDocFreq *= priorWeight * priorBoost;
            }

            assert !Float.isInfinite(termFreqInvDocFreq) && !Float.isNaN(termFreqInvDocFreq);

            // LOGGER.trace(tag + " : TF=" + termFreq + " IDF=" + invDocFreq + " TFIDF=" + termFreqInvDocFreq);
            assignedTags.add(new Tag(tag, termFreqInvDocFreq));

        }

        // sort the list
        Collections.sort(assignedTags, new TagComparator());

        LOGGER.trace("ranked tags: " + assignedTags);

        // when using tag correlations, do the re-ranking

        if (correlationType != TaggingCorrelationType.NO_CORRELATIONS && wcm != null && !assignedTags.isEmpty()) {

            // TODO experimental optimization -- we have a huge list with Tags and their weights which have to be
            // checked for correlations, which is expensive. Assumption: We can shrink this list of tags before
            // re-ranking without actually losing any accuracy. This needs to be evaluated thoroughly.
            if (fastMode) {
                if (taggingType == TaggingType.THRESHOLD) {
                    // in contrast to FIXED_COUNT mode, the limit has no big impact here :(
                    // TODO trick: get position of threshold and then use count limit from below?
                    limitToWeight(assignedTags, tfidfThreshold * 0.0001f);
                } else if (taggingType == TaggingType.FIXED_COUNT) {
                    limitToCount(assignedTags, tagCount * 10);
                }
            }

            correlationReRanking(assignedTags);
        }

        LOGGER.trace("re-ranked tags:" + assignedTags);

        // create final tag result set
        // 1) either by removing those tags which are under the defined threshold
        if (taggingType == TaggingType.THRESHOLD) {
            // ListIterator<Tag> iterator = assignedTags.listIterator();
            // while (iterator.hasNext()) {
            // if (iterator.next().weight < tfidfThreshold) {
            // iterator.remove();
            // }
            // }
            limitToWeight(assignedTags, tfidfThreshold);
        }

        // 2) or by keeping a fixed number of top tags.
        else if (taggingType == TaggingType.FIXED_COUNT) {
            // if (assignedTags.size() > tagCount) {
            // assignedTags.subList(tagCount, assignedTags.size()).clear();
            // }
            limitToCount(assignedTags, tagCount);
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
    private void limitToCount(List<Tag> tags, int limit) {
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
    private void limitToWeight(List<Tag> tags, float minWeight) {
        ListIterator<Tag> iterator = tags.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next().getWeight() < minWeight) {
                iterator.remove();
            }
        }
    }

    /**
     * Do a re-ranking based on Tag correlations.
     * 
     * @param assignedTags
     */
    private void correlationReRanking(List<Tag> assignedTags) {

        StopWatch sw = new StopWatch();

        // do a "shallow" re-ranking, only considering top-tag (n)
        if (correlationType == TaggingCorrelationType.SHALLOW_CORRELATIONS) {
            Iterator<Tag> tagIterator = assignedTags.iterator();
            Tag topTag = tagIterator.next();

            while (tagIterator.hasNext()) {
                Tag currentTag = tagIterator.next();

                WordCorrelation correlation = wcm.getCorrelation(topTag.getName(), currentTag.getName());
                if (correlation != null) {
                    // currentTag.weight += correlationWeight * correlation.getRelativeCorrelation();
                    currentTag.increaseWeight((float) (correlationWeight * correlation.getRelativeCorrelation()));
                }
            }
        }

        // do a "deep" re-ranking, considering correlations between each possible combination (n-1) + ... + 1
        else if (correlationType == TaggingCorrelationType.DEEP_CORRELATIONS) {
            Tag[] tagsArray = assignedTags.toArray(new Tag[assignedTags.size()]);
            for (int i = 0; i < tagsArray.length; i++) {
                Tag outerTag = tagsArray[i];
                for (int j = i; j < tagsArray.length; j++) {
                    Tag innerTag = tagsArray[j];
                    WordCorrelation correlation = wcm.getCorrelation(outerTag.getName(), innerTag.getName());
                    if (correlation != null) {
                        float reRanking = (float) ((correlationWeight / tagsArray.length) * correlation
                                .getRelativeCorrelation());
                        assert !Double.isInfinite(reRanking) && !Double.isNaN(reRanking);
                        // innerTag.weight += reRanking;
                        // outerTag.weight += reRanking;
                        innerTag.increaseWeight(reRanking);
                        outerTag.increaseWeight(reRanking);
                    }
                }
            }
        }

        // re-sort the list, as rankings have changed.
        Collections.sort(assignedTags, new TagComparator());

        LOGGER.trace("correlation reranking for " + assignedTags.size() + " in " + sw.getElapsedTimeString());

    }

    public List<Tag> tag(String text) {

        addToIndex(text);
        updateIndex();

        List<Tag> assignedTags = assignTags(text);
        unstem(assignedTags);
        return assignedTags;
    }

    public Map<String, List<Tag>> tag(Collection<String> texts) {
        Map<String, List<Tag>> result = new HashMap<String, List<Tag>>();
        for (String text : texts) {
            addToIndex(text);
        }
        for (String text : texts) {
            result.put(text, tag(text));
        }
        return result;
    }

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
        for (String tag : tagVocabulary.uniqueSet()) {

            String stemmedTag = stem(tag);

            List<String> unstemmedTags = stemUnstemMap.get(stemmedTag);
            if (unstemmedTags == null) {
                unstemmedTags = new ArrayList<String>();
                stemUnstemMap.put(stemmedTag, unstemmedTags);
            }

            unstemmedTags.add(tag);
        }

        // create a new unstemMap
        unstemMap = new HashMap<String, String>();

        // iterate through the temporary map from above and pick unstemmedTag with highest count
        for (Entry<String, List<String>> entry : stemUnstemMap.entrySet()) {

            String stemmedTag = entry.getKey();
            int bestCount = 0;
            String bestCandidate = null;

            for (String unstemmedTag : entry.getValue()) {
                int count = tagVocabulary.getCount(unstemmedTag);
                if (count > bestCount) {
                    bestCandidate = unstemmedTag;
                    bestCount = count;
                }
            }

            // we only need to keep those mappings where the stemmed form is different from the unstemmed
            if (!bestCandidate.equals(stemmedTag)) {
                unstemMap.put(entry.getKey(), bestCandidate);
            }
        }
    }

    private String stem(String unstemmed) {
        stemmer.setCurrent(unstemmed);
        stemmer.stem();
        String stem = stemmer.getCurrent();
        return stem;
    }

    private Bag<String> stem(Bag<String> unstemmed) {
        Bag<String> stemmed = new HashBag<String>();
        for (String tag : unstemmed.uniqueSet()) {
            int tagCount = unstemmed.getCount(tag);
            String tagStemmed = stem(tag);
            stemmed.add(tagStemmed, tagCount);
        }
        return stemmed;
    }

    private String unstem(String stemmed) {
        String unstem = unstemMap.get(stemmed);
        if (unstem == null) {
            unstem = stemmed;
        }
        return unstem;
    }

    private void unstem(List<Tag> stemmedTags) {
        for (Tag tag : stemmedTags) {
            String unstemmed = unstem(tag.getName());
            tag.setName(unstemmed);
        }
    }

    /**
     * Normalize a list of Tags according to the stemming rules. We need this for the evaluation process, as the the
     * test Tags need to be normalized the same way.
     * 
     * @param tags
     */
    Bag<String> normalize(Bag<String> tags) {
        Bag<String> normalizedTags = new HashBag<String>();
        for (String tag : tags.uniqueSet()) {
            String normalizedTag = unstem(stem(tag));
            int tagCount = tags.getCount(tag);
            normalizedTags.add(normalizedTag, tagCount);
        }
        return normalizedTags;
    }

    public void setCorrelationType(TaggingCorrelationType correlationType) {
        this.correlationType = correlationType;
    }

    public void setWordCorrelationMatrix(WordCorrelationMatrix wcm) {
        this.wcm = wcm;
    }

    public void setCorrelationWeight(float correlationWeight) {
        this.correlationWeight = correlationWeight;
    }

    public void setTaggingType(TaggingType taggingType) {
        this.taggingType = taggingType;
    }

    /**
     * Set max. number of tags to assign when in {@link TaggingType#FIXED_COUNT} mode.
     * 
     * @param tagCount
     */
    public void setTagCount(int tagCount) {
        this.tagCount = tagCount;
    }

    /**
     * When enabled, tags from the controlled vocabulary which have a high occurence are preferred.
     * Set to -1 to disable.
     * 
     * @param usePriors
     */
    public void setPriorWeight(float priorWeight) {
        this.priorWeight = priorWeight;
    }

    /**
     * Set the threshold for the TFIDF value when in {@link TaggingType#THRESHOLD} mode.
     * 
     * @param tfidfThreshold
     */
    public void setTfidfThreshold(float tfidfThreshold) {
        this.tfidfThreshold = tfidfThreshold;
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
    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;
    }

    /**
     * Serialize this tagger to disk.
     * 
     * @param filePath
     */
    public void save(String filePath) {
        updateIndex();
        FileHelper.serialize(this, filePath);
    }

    /**
     * Load a safed tagger from disk. This serves as a factory method instead of using the constructor.
     * 
     * @param filePath
     * @return
     */
    public static ControlledTagger load(String filePath) {
        ControlledTagger tagger = (ControlledTagger) FileHelper.deserialize(filePath);
        if (tagger == null) {
            LOGGER.error("could not read from " + filePath + ", starting with new instance.");
            tagger = new ControlledTagger();
        }
        return tagger;
    }

    /**
     * Hook for the deserialization.
     * 
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setup();
    }

    public static void main(String[] args) {

        ControlledTagger tagger = new ControlledTagger();
        tagger.addToVocabularyFromFile("data/delicious_tags_t140.txt");
        tagger.createUnstemMap();
        // tagger.stemVocabulary();
        System.out.println(tagger.unstem("liberti"));

        System.exit(0);

        // Bag<String> test = new HashBag<String>(Arrays.asList(new String[] { "car", "car", "car", "car",
        // "graphicdesign", "cars", "mercedes" }));
        // Bag<String> norm = tagger.singularPluralNormalization(test);
        // System.out.println(norm);

        System.exit(0);

        System.out.println(tagger.tokenize("the quick brown fox fox."));
        // System.out.println(tagger.tokenizeGrams("the quick brown fox", 2));

        System.exit(0);

        String d1 = "If it walks like a duck and quacks like a duck, it must be a duck.";
        String d2 = "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin.";
        String d3 = "Bugs' ascension to stardom also prompted the Warner animators to recast Daffy Duck as the rabbit's rival, intensely jealous and determined to steal back the spotlight while Bugs remained indifferent to the duck's jealousy, or used it to his advantage. This turned out to be the recipe for the success of the duo.";
        String d4 = "6:25 PM 1/7/2007 blog entry: I found this great recipe for Rabbit Braised in Wine on cookingforengineers.com.";
        // String d5 =
        // "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipies for Jiaozi.";
        String d5 = "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipe for Jiaozi.";

        String[] tags = new String[] { "beijing", "dish", "duck", "rabbit", "recipe", "roast" };

        tagger.addToVocabulary(Arrays.asList(tags));
        tagger.tag(Arrays.asList(new String[] { d1, d2, d3, d4, d5 }));

    }

}
