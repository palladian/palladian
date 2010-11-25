package tud.iir.classification.controlledtagging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.log4j.Logger;

import tud.iir.classification.WordCorrelation;
import tud.iir.classification.controlledtagging.ControlledTaggerSettings.TaggingCorrelationType;
import tud.iir.classification.controlledtagging.ControlledTaggerSettings.TaggingType;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetCallback;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetEntry;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetFilter;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StopWatch;
import tud.iir.helper.Tokenizer;
import tud.iir.web.Crawler;

/**
 * A TF-IDF and tag correlation based tagger using a controlled and weighted vocabulary.
 * 
 * rem: enable assertions for debugging, VM arg -ea
 * 
 * @author Philipp Katz
 * 
 */
public class ControlledTagger {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(ControlledTagger.class);

    // //////// index collections ///////////
    // TODO we could move some methods from here to the Index class.
    private ControlledTaggerIndex index = new ControlledTaggerIndex();

    // /////// customizable settings ///////
    private ControlledTaggerSettings settings = new ControlledTaggerSettings();

    /** Default constructor. */
    public ControlledTagger() {
        // setup();
    }

    // //////// methods ////////////

    // private void setup() {
    // // re-create the stemmer, which is transient, on initialization/deserialization
    // settings.setStemmer(new englishStemmer());
    // }

    public void train(String text, Bag<String> tags) {

        // remove undesired tags (not matching RegEx).
        tags = clean(tags);
        
        Bag<String> stemmedTags = stem(tags);

        index.getTagVocabulary().addAll(tags); // XXX this should be the unique set?
        index.getStemmedTagVocabulary().addAll(stemmedTags); // XXX dto.

        if (text.length() > 0) {
            addToIdf(text);
        }

        if (settings.getCorrelationType() != TaggingCorrelationType.NO_CORRELATIONS) {
            // addToWcm(stemmedTags.uniqueSet());
            index.getWcm().updateGroup(stemmedTags.uniqueSet());
        }

        if (tags.size() > 0) {
            index.setTrainCount(index.getTrainCount() + 1);
            index.setDirtyIndex(true);
        }

    }

    /**
     * We only need those tags which actually match with the provided pattern. The rest can be filtered out in advance.
     */
    private Bag<String> clean(Bag<String> tags) {
        Bag<String> result = new HashBag<String>();
        for (String string : tags.uniqueSet()) {
            if (settings.getTagMatchPattern().matcher(string).matches()) {
                result.add(string, tags.getCount(string));
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
        train(text, new HashBag<String>());
    }

    /**
     * Allows training, only with tags. Each Bag of tags is considered as one training instance, e.g. one document. This
     * builds up the tag vocabulary and the tag correlations.
     * 
     * @param tags
     */
    public void train(Bag<String> tags) {
        train("", tags);
    }

//    /**
//     * Add a list of tags to the WordCorrelationMatrix, for a set with size n, we will add
//     * <code>(n - 1) + (n - 2) + ... + 1 = (n * (n - 1)) / 2</code> correlations.
//     * 
//     * TODO move this to the CorrelationMatrix?
//     * 
//     * @param tags
//     */
//    private void addToWcm(Set<String> tags) {
//        String[] tagArray = tags.toArray(new String[tags.size()]);
//
//        for (int i = 0; i < tagArray.length; i++) {
//            for (int j = i + 1; j < tagArray.length; j++) {
//                index.getWcm().updatePair(tagArray[i], tagArray[j]);
//            }
//        }
//    }

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

    @Deprecated
    public void addToVocabulary(String tag) {
        index.getTagVocabulary().add(tag);
        // calculateAverageTagOccurence();
        index.setDirtyIndex(true);
    }

    @Deprecated
    public int addToVocabulary(Collection<String> tags) {
        int addCount = 0;
        for (String tag : tags) {
            if (index.getTagVocabulary().add(tag)) {
                addCount++;
            }
        }
        // calculateAverageTagOccurence();
        index.setDirtyIndex(true);
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
                if (index.getTagVocabulary().add(tagName, tagCount)) {
                    addCount[0]++;
                }
            }
        });
        // calculateAverageTagOccurence();
        index.setDirtyIndex(true);
        LOGGER.debug("added " + addCount[0] + " tags to vocabulary. avg. tag occurence: "
                + index.getAverageTagOccurence());
        return addCount[0];
    }

    /**
     * Re(calculates) the average occurence over all tags in the tag vocabulary. This value is used to boost tags with
     * high occurences.
     */
    private void calculateAverageTagOccurence() {
        float result = 0;
        // TODO cant we write result = stemmedTagVocabulary.size() / stemmedTagVocabulary.uniqueSet().size(); ?
        for (String tag : index.getStemmedTagVocabulary().uniqueSet()) {
            result += index.getStemmedTagVocabulary().getCount(tag);
        }
        result /= index.getStemmedTagVocabulary().uniqueSet().size();
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

        // return stemmedTagVocabulary.contains(tag);
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
        Bag<String> stems = stem(tokens);

        for (String stem : stems.uniqueSet()) {
            if (isAcceptedTag(stem)) {
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
    protected Bag<String> tokenize(String text) {
        String toTokenize = text.toLowerCase();

        Bag<String> tokens = new HashBag<String>();

        tokens.addAll(Tokenizer.tokenize(toTokenize)); // unigrams
        for (int i = 2; i <= settings.getPhraseLength(); i++) { // bi-/tri-/...-grams
            tokens.addAll(tokenizeGrams(toTokenize, i));            
        }
        
        //tokens.addAll(tokenizeGrams(toTokenize, 2));
        //tokens.addAll(tokenizeGrams(toTokenize, 3));

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

            // moved this to the reRanking method, as the normalization also applies.

            // if (usePriors) {
            // if (priorWeight != -1) {
            // float priorBoost = (float) Math.log10(1.0 + index.getStemmedTagVocabulary().getCount(tag)
            // / index.getAverageTagOccurence());
            // assert !Float.isInfinite(priorBoost) && !Float.isNaN(priorBoost);
            // termFreqInvDocFreq *= priorWeight * priorBoost;
            // }

            assert !Float.isInfinite(termFreqInvDocFreq) && !Float.isNaN(termFreqInvDocFreq);

            // LOGGER.trace(tag + " : TF=" + termFreq + " IDF=" + invDocFreq + " TFIDF=" + termFreqInvDocFreq);
            assignedTags.add(new Tag(tag, termFreqInvDocFreq));

        }

        // sort the list
        Collections.sort(assignedTags, new TagComparator());

        LOGGER.trace("ranked tags: " + assignedTags);

        // when using tag correlations, do the re-ranking

        // if (correlationType != TaggingCorrelationType.NO_CORRELATIONS && index.getWcm() != null
        // && !assignedTags.isEmpty()) {

        // if (!assignedTags.isEmpty()) {
        // reRanking only makes sense, if we have at least two tags :)
        if (assignedTags.size() > 1) {

            // TODO experimental optimization -- we have a huge list with Tags and their weights which have to be
            // checked for correlations, which is expensive. Assumption: We can shrink this list of tags before
            // re-ranking without actually losing any accuracy. This needs to be evaluated thoroughly.

            // this is obsolete with FastWordCorrelationMatrix :)

            // if (fastMode) {
            // if (taggingType == TaggingType.THRESHOLD) {
            // // in contrast to FIXED_COUNT mode, the limit has no big impact here :(
            // // TODO trick: get position of threshold and then use count limit from below?
            // limitToWeight(assignedTags, tfidfThreshold * 0.0001f);
            // } else if (taggingType == TaggingType.FIXED_COUNT) {
            // limitToCount(assignedTags, tagCount * 10);
            // }
            // }

            reRankTags(assignedTags);
        }

        LOGGER.trace("re-ranked tags:" + assignedTags);

        // create final tag result set
        // 1) either by removing those tags which are under the defined threshold
        if (settings.getTaggingType() == TaggingType.THRESHOLD) {
            // ListIterator<Tag> iterator = assignedTags.listIterator();
            // while (iterator.hasNext()) {
            // if (iterator.next().weight < tfidfThreshold) {
            // iterator.remove();
            // }
            // }
            limitToWeight(assignedTags, settings.getTfidfThreshold());
        }

        // 2) or by keeping a fixed number of top tags.
        else if (settings.getTaggingType() == TaggingType.FIXED_COUNT) {
            // if (assignedTags.size() > tagCount) {
            // assignedTags.subList(tagCount, assignedTags.size()).clear();
            // }
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
     * Do a re-ranking based on priors + tag correlations.
     * 
     * @param assignedTags in/out: assigned tags, sorted by relevance, descending.
     */
    private void reRankTags(List<Tag> assignedTags) {

        StopWatch sw = new StopWatch();

        // experimental: to normalize the range of the re-ranked tags back to their original range,
        // by keeping the lower/upper bounds, so we keep the general properties of the TF/IDF -- elsewise
        // we will get outliers which are considerably bigger than most of the other tag weights.
        float oldMin = assignedTags.get(0).getWeight();
        float oldMax = assignedTags.get(assignedTags.size() - 1).getWeight();

        // do the prior-based re-ranking
        if (settings.getPriorWeight() != -1) {

            for (Tag tag : assignedTags) {
                int count = index.getStemmedTagVocabulary().getCount(tag.getName());
                // float priorBoost = (float) Math.log10(1.0 + priorWeight * count / index.getAverageTagOccurence());
                float priorBoost = (float) Math.log10(1.0 + settings.getPriorWeight() * count
                        / index.getAverageTagOccurence()) + 1;
                assert !Float.isInfinite(priorBoost) && !Float.isNaN(priorBoost);
                float weight = tag.getWeight() * priorBoost;
                // weight *= priorWeight * priorBoost;
                tag.setWeight(weight);

            }

        }

        Collections.sort(assignedTags, new TagComparator());

        // Option 1: do a "shallow" re-ranking, only considering top-tag (n)
        if (settings.getCorrelationType() == TaggingCorrelationType.SHALLOW_CORRELATIONS) {
            Iterator<Tag> tagIterator = assignedTags.iterator();
            Tag topTag = tagIterator.next();

            while (tagIterator.hasNext()) {
                Tag currentTag = tagIterator.next();

                WordCorrelation correlation = index.getWcm().getCorrelation(topTag.getName(), currentTag.getName());
                if (correlation != null) {
                    // currentTag.weight += correlationWeight * correlation.getRelativeCorrelation();
                    currentTag.increaseWeight((float) (settings.getCorrelationWeight() * correlation
                            .getRelativeCorrelation()));
                }
            }
        }

        // Option 2: do a "deep" re-ranking, considering correlations between each possible combination
        else if (settings.getCorrelationType() == TaggingCorrelationType.DEEP_CORRELATIONS) {
            Tag[] tagsArray = assignedTags.toArray(new Tag[assignedTags.size()]);

            // experimental:
            // normalization factor; we have (n - 1) + (n - 2) + ... + 1 = n * (n - 1) / 2 re-rankings.
            int numReRanking = tagsArray.length * (tagsArray.length - 1) / 2;

            for (int i = 0; i < tagsArray.length; i++) {
                Tag outerTag = tagsArray[i];
                for (int j = i; j < tagsArray.length; j++) {
                    Tag innerTag = tagsArray[j];
                    WordCorrelation correlation = index.getWcm().getCorrelation(outerTag.getName(), innerTag.getName());
                    if (correlation != null) {
                        // float reRanking = (float) ((correlationWeight / tagsArray.length) *
                        // correlation.getRelativeCorrelation());
                        float reRanking = (float) ((settings.getCorrelationWeight() / numReRanking) * correlation
                                .getRelativeCorrelation());
                        // FIXME why dont we put the numReRanking division outside the loop?

                        assert !Double.isInfinite(reRanking) && !Double.isNaN(reRanking);
                        // innerTag.weight += reRanking;
                        // outerTag.weight += reRanking;
                        innerTag.increaseWeight(reRanking);
                        outerTag.increaseWeight(reRanking);
                    }
                }
            }

        }

        // re-sort the list, as ranking weights have changed
        Collections.sort(assignedTags, new TagComparator());

        // do the scaling back to the original range (see comment above)
        float newMin = assignedTags.get(0).getWeight();
        float newMax = assignedTags.get(assignedTags.size() - 1).getWeight();

        if (newMin != newMax) { // avoid division by zero
            for (Tag tag : assignedTags) {

                // http://de.wikipedia.org/wiki/Normalisierung_(Mathematik)
                float normalizedWeight = (tag.getWeight() - newMin) * ((oldMax - oldMin) / (newMax - newMin)) + oldMin;
                tag.setWeight(normalizedWeight);

            }
        }

        LOGGER.trace("correlation reranking for " + assignedTags.size() + " in " + sw.getElapsedTimeString());

    }

    /**
     * Tag the supplied text.
     * 
     * @param text
     * @return Array with assigned Tags, sorted by weight or empty List. Never <code>null</code>.
     */
    public List<Tag> tag(String text) {

        addToIdf(text);
        updateIndex();

        List<Tag> assignedTags = assignTags(text);
        unstem(assignedTags);
        return assignedTags;
    }

    public Map<String, List<Tag>> tag(Collection<String> texts) {
        Map<String, List<Tag>> result = new HashMap<String, List<Tag>>();
        for (String text : texts) {
            addToIdf(text);
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
        String unstem = index.getUnstemMap().get(stemmed);
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
    //Bag<String> normalize(Bag<String> tags) {
    public    Bag<String> normalize(Bag<String> tags) {
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

    /**
     * Serialize this tagger to disk.
     * 
     * @param filePath
     */
    public void save(String filePath) {
        updateIndex();
        StopWatch sw = new StopWatch();
        // FileHelper.serialize(this, filePath);
        FileHelper.serialize(index, filePath);
        LOGGER.info("saved index to " + filePath + " in " + sw.getElapsedTimeString());
    }

    /**
     * Load a safed tagger from disk. This serves as a factory method instead of using the constructor.
     * 
     * @param filePath
     * @return
     */
    /*
     * public static ControlledTagger load(String filePath) {
     * StopWatch sw = new StopWatch();
     * ControlledTagger tagger = (ControlledTagger) FileHelper.deserialize(filePath);
     * if (tagger == null) {
     * LOGGER.error("could not read from " + filePath + ", starting with new instance.");
     * tagger = new ControlledTagger();
     * } else {
     * LOGGER.info("loaded index from " + filePath + " in " + sw.getElapsedTimeString());
     * }
     * return tagger;
     * }
     */

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
    
    public void setIndex(ControlledTaggerIndex index) {
        this.index = index;
    }

    /**
     * Hook for the deserialization.
     * 
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    /*
     * private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
     * in.defaultReadObject();
     * setup();
     * }
     */

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ControlledTagger");
        // sb.append("\nsize idfIndex=").append(index.getIdfIndex().uniqueSet().size());
        // sb.append("\nsize tagVocabulary=").append(index.getTagVocabulary().uniqueSet().size());
        // sb.append("\nsize stemmedTagVocabulary=").append(index.getStemmedTagVocabulary().uniqueSet().size());
        // sb.append("\nsize unstemMap=").append(index.getUnstemMap().size());
        // sb.append("\nsize wcm=").append(index.getWcm().getCorrelations().size());
        // sb.append("\nidfCount=").append(index.getIdfCount());
        // sb.append("\ntrainCount=").append(index.getTrainCount());
        // sb.append("\navergateTagOccurence=").append(index.getAverageTagOccurence());
        // sb.append("\ndirtyIndex=").append(index.isDirtyIndex());

        sb.append("\n").append(index);

//        sb.append("\ntaggingType=").append(settings.getTaggingType());
//        sb.append("\ncorrelationType=").append(settings.getCorrelationType());
//        sb.append("\ntfidfThreshold=").append(settings.getTfidfThreshold());
//        sb.append("\ntagCount=").append(settings.getTagCount());
//        sb.append("\ncorrelationWeight=").append(settings.getCorrelationWeight());
//        sb.append("\npriorWeight=").append(settings.getPriorWeight());
        // sb.append("\nfastMode=").append(fastMode);
        
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

        
        
        /////////////////////// usage example for documentation ///////////////////////////////
        
        // set up the ControlledTagger
        final ControlledTagger tagger = new ControlledTagger();
        
        // all tagging parameters are encapsulated by ControlledTaggerSettings
        ControlledTaggerSettings taggerSettings = tagger.getSettings();
        //taggerSettings.setCorrelationType(TaggingCorrelationType.NO_CORRELATIONS);
        
        // create a DeliciousDatasetReader + Filter for training
        /*DeliciousDatasetReader reader = new DeliciousDatasetReader();
        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        filter.setMaxFileSize(600000);
        reader.setFilter(filter);
        
        // train the tagger with 20.000 train documents from the dataset
        DatasetCallback callback = new DatasetCallback() {
            @Override
            public void callback(DatasetEntry entry) {
                String content = FileHelper.readFileToString(entry.getPath());
                content = HTMLHelper.htmlToString(content, true);
                tagger.train(content, entry.getTags());
            }
        };
        reader.read(callback, 20000);
        
        // save the model for later usage
        tagger.save("data/models/controlledTaggerModel2.ser");

        // ControlledTagger tagger = new ControlledTagger();
        // tagger.addToVocabularyFromFile("data/delicious_tags_t140.txt");
        // tagger.createUnstemMap();
        // // tagger.stemVocabulary();
        // System.out.println(tagger.unstem("liberti"));

        System.exit(0);*/
        
        // load the trained model, this takes some time.
        // if you will tag multiple documents, be sure to move this outside the loop!
        tagger.load("data/models/controlledTaggerModel2.ser");
        
        // assign tags according to a web page's content
        PageContentExtractor extractor = new PageContentExtractor();
        String content = extractor.getResultText("http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars");
        List<Tag> assignedTags = tagger.tag(content);
        
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
