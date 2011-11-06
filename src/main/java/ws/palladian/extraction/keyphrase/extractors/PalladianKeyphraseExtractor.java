package ws.palladian.extraction.keyphrase.extractors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.tartarus.snowball.ext.porterStemmer;

import ws.palladian.classification.page.Stopwords;
import ws.palladian.classification.page.Stopwords.Predefined;
import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PerformanceCheckProcessingPipeline;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.ProcessingPipeline;
import ws.palladian.preprocessing.featureextraction.Annotation;
import ws.palladian.preprocessing.featureextraction.AnnotationFeature;
import ws.palladian.preprocessing.featureextraction.AnnotationGroup;
import ws.palladian.preprocessing.featureextraction.CountCalculator;
import ws.palladian.preprocessing.featureextraction.DuplicateTokenRemover;
import ws.palladian.preprocessing.featureextraction.FrequencyCalculator;
import ws.palladian.preprocessing.featureextraction.NGramCreator;
import ws.palladian.preprocessing.featureextraction.RegExTokenRemover;
import ws.palladian.preprocessing.featureextraction.StemmerAnnotator;
import ws.palladian.preprocessing.featureextraction.StopTokenRemover;
import ws.palladian.preprocessing.featureextraction.TermCorpus;
import ws.palladian.preprocessing.featureextraction.TermCorpusBuilder;
import ws.palladian.preprocessing.featureextraction.TokenRemover;
import ws.palladian.preprocessing.featureextraction.TokenSpreadCalculator;
import ws.palladian.preprocessing.featureextraction.Tokenizer;

/**
 * 
 * @author Philipp Katz
 * 
 */
public class PalladianKeyphraseExtractor extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PalladianKeyphraseExtractor.class);
    
    private final ProcessingPipeline pipeline;
    
    private List<Annotation> trainingAnnotations = new ArrayList<Annotation>();

    private TermCorpus corpus;


    @SuppressWarnings("serial")
    public PalladianKeyphraseExtractor() {
        
        pipeline = new PerformanceCheckProcessingPipeline();
        
        final Stopwords stopwords = new Stopwords(Predefined.EN);
        
        pipeline.add(new Tokenizer());
        pipeline.add(new RegExTokenRemover("\\p{Punct}"));
        pipeline.add(new RegExTokenRemover(".{1,2}"));
        pipeline.add(new NGramCreator(2, 4));
        
        // remove those NGrams, which start or end with a stopword; this
        // helps to reduce the number of training instances by about 50 percent
        pipeline.add(new TokenRemover() {
            
            @Override
            protected boolean remove(Annotation annotation) {
                if (annotation instanceof AnnotationGroup) {
                    AnnotationGroup group = (AnnotationGroup) annotation;
                    List<Annotation> annotations = group.getAnnotations();
                    Annotation firstAnnotation = annotations.get(0);
                    if (stopwords.contains(firstAnnotation.getValue())) {
                        return true;
                    }
                    Annotation lastAnnotation = annotations.get(annotations.size() - 1);
                    if (stopwords.contains(lastAnnotation.getValue())) {
                        return true;
                    }
                }
                return false;
            }
        });
        
        // remove Tokens with many numbers
        pipeline.add(new TokenRemover() {
            
            @Override
            protected boolean remove(Annotation annotation) {
                String value = annotation.getValue();
                int numberLetters = StringHelper.letterCount(value);
                return ((float) numberLetters / value.length() < 0.5);
            }
        });
        
        // remove Token which start/end with punctation
        pipeline.add(new TokenRemover() {
            
            @Override
            protected boolean remove(Annotation annotation) {
                if (annotation instanceof AnnotationGroup) {
                    AnnotationGroup group = (AnnotationGroup) annotation;
                    List<Annotation> annotations = group.getAnnotations();
                    Annotation firstAnnotation = annotations.get(0);
                    if (firstAnnotation.getValue().matches("\\p{Punct}")) {
                        return true;
                    }
                    Annotation lastAnnotation = annotations.get(annotations.size() - 1);
                    if (lastAnnotation.getValue().matches("\\p{Punct}")) {
                        return true;
                    }
                }
                return false;
            }
        });
        
        pipeline.add(new StopTokenRemover(Predefined.EN));
        pipeline.add(new StemmerAnnotator(new porterStemmer()));
        
        pipeline.add(new CountCalculator());
        pipeline.add(new TokenSpreadCalculator());
        pipeline.add(new FrequencyCalculator());
        pipeline.add(new PhrasenessAnnotator());
        
        
        pipeline.add(new DuplicateTokenRemover());
        corpus = new TermCorpus();
        pipeline.add(new TermCorpusBuilder(corpus));
        
        // additional features to extract
        // count
        // uppercaseCount, uppercasePercentage
        // totalUppercaseCount, totalUppercasePercentage
        // firstPosition, relativeFirstPosition
        // lastPosition, relativeLastPosition
        // correlationStats: Sum, Max, Min, Mean, Count
        // posTag
        // wordCount
        // frequency
        // inverseDocumentFrequency
        // frequencyInverseDocumentFrequency
        // spread, relativeSpread
        // length
        // prior ("keyphraseness")
    }

    @Override
    public void startTraining() {
        
    }
    
    int totalAnnotations = 0;
    int mappedAnnotations = 0;

    @Override
    public void train(String inputText, Set<String> keyphrases, int index) {
        
        // System.out.println("inTrain");
        System.out.println(keyphrases);
        
        PipelineDocument document = pipeline.process(new PipelineDocument(inputText));
        // LOGGER.debug(pipeline);
        
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature tokenList = (AnnotationFeature)featureVector.get(Tokenizer.PROVIDED_FEATURE);

        int positiveCounter = 0;
        
        // mark positive candidates, i.e. those which were manually assigned in the training data
        // TODO need to consider stems and composite terms here
        for (Annotation annotation : tokenList.getValue()) {
            boolean isCandidate = keyphrases.contains(annotation.getValue().toLowerCase());
            isCandidate = isCandidate || keyphrases.contains(annotation.getValue().replace(" ", "").toLowerCase());
            isCandidate = isCandidate || keyphrases.contains(((String) annotation.getFeatureVector().get(StemmerAnnotator.PROVIDED_FEATURE).getValue()).toLowerCase());
            isCandidate = isCandidate || keyphrases.contains(((String) annotation.getFeatureVector().get(StemmerAnnotator.PROVIDED_FEATURE).getValue()).replace(" ", "").toLowerCase());
            
            annotation.getFeatureVector().add(new Feature<Boolean>("isKeyphrase", isCandidate));
            if (isCandidate) {
                // System.out.println("** positive ** " + annotation);
                positiveCounter++;
            }
        }
        
        LOGGER.debug("# tokens " + tokenList.getValue().size());
        LOGGER.debug("# positive examples " + positiveCounter);
        LOGGER.debug("detected positive examples from training " + (float) positiveCounter / keyphrases.size());
        // LOGGER.debug(tokenList.toStringList());
        totalAnnotations += keyphrases.size();
        mappedAnnotations += positiveCounter;
        
        // trainingAnnotations.addAll(tokenList.getValue());
        // createFeatureList(tokenList);

    }
    

    private void createFeatureList(AnnotationFeature tokenList) {
        
        List<Annotation> annotations = tokenList.getValue();
        for (Annotation annotation : annotations) {
            FeatureVector featureVector = annotation.getFeatureVector();
            Feature<?>[] valueArray = featureVector.toValueArray();
            for (Feature<?> feature : valueArray) {
                System.out.println(feature.getName() + " " + feature.getValue());
            }
        }
        
        // TODO Auto-generated method stub
        
    }

    @Override
    public void endTraining() {
        
        System.out.println("----");
        System.out.println(pipeline);
        
        corpus.save("/Users/pk/Desktop/corpus.txt");
        
        
        System.out.println("detection coverage " + (float) mappedAnnotations / totalAnnotations);

        
//        // XXX
//        corpus.makeRelativeScores();
//
//        // keep the CSV training data in memory for now
//        StringBuilder csvBuilder = new StringBuilder();
//
//        // Set<String> featureNames = trainData.iterator().next().getFeatures().keySet();
//        Map<String, Object> features = trainDocuments.iterator().next().iterator().next().getFeatures();
//        Map<String, Double> numericFeatures = getNumericFeatures(features);
//        //Set<String> featureNames = features.keySet();
//        Set<String> featureNames = numericFeatures.keySet();
//        // trainData.append("#");
//        csvBuilder.append(StringUtils.join(featureNames, ";")).append("\n");
//
//        // write all values
//        for (DocumentModel trainData : trainDocuments) {
//            
//            trainData.calculateCorrelations();
//            
//            for (Candidate candidate : trainData) {
//                // Collection<Object> featureValues = candidate.getFeatures().values();
//                Collection<Double> featureValues = getNumericFeatures(candidate.getFeatures()).values();
//                csvBuilder.append(StringUtils.join(featureValues, ";")); //.append("\n");
//                // csvBuilder.append(";").append(candidate.getFeatures().get("positive")).append(";").append("\n");
//                csvBuilder.append("\n");
//            }
//        }
//
//        final String trainDataPath = "data/temp/KeyphraseExtractorTraining.csv";
//        FileHelper.writeToFile(trainDataPath, csvBuilder);
//        ///// trainData.clear();
//        trainDocuments.clear();
//
//        // save memory; this is necessary, as the corpus consumes great amounts of memory, but
//        // fortunately we don't need the corpus for the training process
//        saveCorpus();
//        corpus = null;
//
//        // train a new Classifier using the CSV data from above
//        classifier.trainClassifier(trainDataPath, true);
//
//        // save the trained classifier
//        saveClassifier();
//
//        // load the corpus again which has been removed from memory
//        loadCorpus();

    }

//    private void saveCorpus() {
//        //String filePath = settings.getModelPath() + "/corpus.ser";
//        String filePath = settings.getModelPath() + "/corpus.ser.gz";
//
//        LOGGER.info("saving corpus to " + filePath + " ...");
//        StopWatch sw = new StopWatch();
//        // corpus.makeRelativeScores(); // XXX
//        FileHelper.serialize(corpus, filePath);
//        LOGGER.info("saved corpus in " + sw.getElapsedTimeString());
//    }

    @Override
    public void startExtraction() {
        
    }

    @Override
    public List<Keyphrase> extract(String text) {

//        addToCorpus(text);
//
//        DocumentModel candidates = createDocumentModel(text);
//        
//        /// XXX
//        candidates.calculateCorrelations();
//
//        // eliminate undesired candidates in advance
//        ListIterator<Candidate> listIterator = candidates.listIterator();
//        while (listIterator.hasNext()) {
//            Candidate candidate = listIterator.next();
//
//            boolean ignore = settings.getStopwords().contains(candidate.getValue());
//            ignore = ignore || !settings.getPattern().matcher(candidate.getValue()).matches();
//            ignore = ignore || (settings.isControlledMode() && candidate.getPrior() == 0);
//            ignore = ignore || candidate.getCount() < settings.getMinOccurenceCount();
//
//            if (ignore) {
//                listIterator.remove();
//            }
//        }
//
//        // perform the regression for ranking the candidates
//        classify(candidates);
//
//        // Collections.sort(candidates, new CandidateComparator());
//        // for (Candidate candidate : candidates) {
//        // LOGGER.debug(candidate.getRegressionValue() + "\t" + candidate);
//        // }
//
//        // do the correlation based re-ranking
//        reRankCandidates(candidates);
//
//        // create the final result, take the top n candidates
//        limitResult(candidates);
//
//        // return candidates;
//        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();
//        for (Candidate candidate : candidates) {
//            keyphrases.add(new Keyphrase(candidate.getValue(), candidate.getRegressionValue()));
//        }
//
//        return keyphrases;
        
        return Collections.emptyList();

    }

//    /**
//     * Reduces the {@link Candidate}s in the DocumentModel (e. g. the list of {@link Candidate}s) using the specified
//     * {@link AssignmentMode}.
//     * 
//     * @param candidates
//     */
//    private void limitResult(DocumentModel candidates) {
//
//        // sort the candidates by regression value
//        Collections.sort(candidates, new CandidateComparator());
//
//        // ListIterator for manipulating the list
//        ListIterator<Candidate> listIterator = candidates.listIterator();
//
//        switch (settings.getAssignmentMode()) {
//
//            // assign a maximum number of keyphrases as result
//            case FIXED_COUNT:
//                if (candidates.size() > settings.getKeyphraseCount()) {
//                    candidates.subList(settings.getKeyphraseCount(), candidates.size()).clear();
//                }
//                break;
//
//            // assign all keyphrases which have a weight above the specified threshold
//            case THRESHOLD:
//                while (listIterator.hasNext()) {
//                    if (listIterator.next().getRegressionValue() <= settings.getKeyphraseThreshold()) {
//                        listIterator.remove();
//                    }
//                }
//                break;
//
//            // first assign a maximum number of keyphrases (FIXED_COUNT), but if there are more keyphrases
//            // with weights above the specified threshold, we assign more than the specified count
//            case COMBINED:
//                while (listIterator.hasNext()) {
//                    Candidate next = listIterator.next();
//                    boolean moreThanLimit = listIterator.nextIndex() > settings.getKeyphraseCount();
//                    boolean lessThanThreshold = next.getRegressionValue() <= settings.getKeyphraseThreshold();
//                    if (moreThanLimit && lessThanThreshold) {
//                        listIterator.remove();
//                    }
//                }
//                break;
//        }
//
//    }

//    /**
//     * Performs a correlation based re-ranking of the supplied {@link Candidate}s. The re-ranking strategies are
//     * explained in Diploma thesis <i>NewsSeecr -- Clustering und Ranking von Nachrichten zu Named Entities aus
//     * Newsfeeds</i>, Philipp Katz, 2010. The re-ranking should basically be obsolete now by using the classification
//     * based approach.
//     * 
//     * The re-ranking factor can be adjusted using {@link KeyphraseExtractorSettings#setCorrelationWeight(float)}. This
//     * number is quite ad-hoc and has to be determined experimentally.
//     * 
//     * TODO add length (e.g. num-of-terms-) re-ranking?
//     * 
//     * @param candidates
//     */
//    private void reRankCandidates(DocumentModel candidates) {
//
//        StopWatch sw = new StopWatch();
//
//        if (candidates.isEmpty()) {
//            return;
//        }
//
//        Collections.sort(candidates, new CandidateComparator());
//        
//        ////////////////////////////////////////////////////////
//
//        // experimental: to normalize the range of the re-ranked tags back to their original range,
//        // by keeping the lower/upper bounds, so we keep the general properties of the TF/IDF -- elsewise
//        // we will get outliers which are considerably bigger than most of the other tag weights.
//        double oldMin = candidates.get(0).getRegressionValue();
//        double oldMax = candidates.get(candidates.size() - 1).getRegressionValue();
//
//        // Option 1: do a "shallow" re-ranking, only considering top-tag (n)
//        if (settings.getReRankingMode() == ReRankingMode.SHALLOW_CORRELATION_RERANKING) {
//            Iterator<Candidate> candidateIterator = candidates.iterator();
//            Candidate topCandidate = candidateIterator.next();
//
//            while (candidateIterator.hasNext()) {
//                Candidate currentCandidate = candidateIterator.next();
//
//                WordCorrelation correlation = corpus.getCorrelation(topCandidate, currentCandidate);
//                if (correlation != null) {
//                    currentCandidate.increaseRegressionValue(settings.getCorrelationWeight()
//                            * correlation.getRelativeCorrelation());
//                }
//            }
//        }
//
//        // Option 2: do a "deep" re-ranking, considering correlations between each possible combination
//        else if (settings.getReRankingMode() == ReRankingMode.DEEP_CORRELATION_RERANKING) {
//            Candidate[] candidatesArray = candidates.toArray(new Candidate[candidates.size()]);
//
//            // experimental:
//            // normalization factor; we have (n - 1) + (n - 2) + ... + 1 = n * (n - 1) / 2 re-rankings.
//            int numReRanking = candidatesArray.length * (candidatesArray.length - 1) / 2;
//            // FIX-ME why dont we put the numReRanking division outside the loop?
//            float factor = settings.getCorrelationWeight() / numReRanking;
//
//            for (int i = 0; i < candidatesArray.length; i++) {
//                Candidate candidate1 = candidatesArray[i];
//
//                for (int j = i; j < candidatesArray.length; j++) {
//                    Candidate candidate2 = candidatesArray[j];
//
//                    WordCorrelation correlation = corpus.getCorrelation(candidate1, candidate2);
//                    if (correlation != null) {
//                        float reRanking = (float) (factor * correlation.getRelativeCorrelation());
//
//                        assert !Double.isInfinite(reRanking);
//                        assert !Double.isNaN(reRanking);
//
//                        candidate1.increaseRegressionValue(reRanking);
//                        candidate2.increaseRegressionValue(reRanking);
//                    }
//                }
//            }
//        }
//
//        // re-sort the list, as ranking weights have changed
//        Collections.sort(candidates, new CandidateComparator());
//
//        // do the scaling back to the original range (see comment above)
//        double newMin = candidates.get(0).getRegressionValue();
//        double newMax = candidates.get(candidates.size() - 1).getRegressionValue();
//
//        if (newMin != newMax) { // avoid division by zero
//            for (Candidate candidate : candidates) {
//
//                // http://de.wikipedia.org/wiki/Normalisierung_(Mathematik)
//                double current = candidate.getRegressionValue();
//                double normalized = (current - newMin) * ((oldMax - oldMin) / (newMax - newMin)) + oldMin;
//                candidate.setRegressionValue(normalized);
//
//            }
//        }
//
//        LOGGER.trace("correlation reranking for " + candidates.size() + " in " + sw.getElapsedTimeString());
//
//    }

//    /**
//     * Stem a term or a phrase using the specified stemmer. For phrases (e. g. multiple terms separated by space
//     * characters) each single term is stemmed, stopwords removed and the all final stems sorted alphabetically.
//     * 
//     * @param unstemmed
//     * @return
//     */
//    private String stem(String unstemmed) {
//        
//        List<String> result = new ArrayList<String>();
//        SnowballStemmer stemmer = settings.getStemmer();
//        Set<String> stopwords = settings.getStopwords();
//
//        // stem each part of the phrase
//        String[] parts = unstemmed.toLowerCase().split(" ");
//        for (String part : parts) {
//            stemmer.setCurrent(part);
//            stemmer.stem();
//            String current = stemmer.getCurrent();
//            if (!stopwords.contains(current)) {
//                result.add(current);
//            }
//        }
//        
//        // sort stems alphabetically
//        Collections.sort(result);
//        return StringUtils.join(result, " ");
//
//    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        
        
        PalladianKeyphraseExtractor extractor = new PalladianKeyphraseExtractor();
        extractor.train("the quick brown fox jumps over the lazy dog", new HashSet<String>(Arrays.asList("fox", "dog", "lazy dog", "brown fox")), 0);
        System.exit(0);

        String d1 = "If it walks like a duck and quacks like a duck, it must be a duck.";
        String d2 = "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin.";
        String d3 = "Bugs' ascension to stardom also prompted the Warner animators to recast Daffy Duck as the rabbit's rival, intensely jealous and determined to steal back the spotlight while Bugs remained indifferent to the duck's jealousy, or used it to his advantage. This turned out to be the recipe for the success of the duo.";
        String d4 = "6:25 PM 1/7/2007 blog entry: I found this great recipe for Rabbit Braised in Wine on cookingforengineers.com.";
        // String d5 =
        // "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipies for Jiaozi.";
        String d5 = "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipe for Jiaozi.";
        String text2 = "the quick brown fox jumps over the lazy dog. the quick brown fox. brownfox. brownfox. brownfox. brownfox. brownfox.";
        // String text = "apple apple apples apples";
        // String text = "Apple sells phones called iPhones. The iPhone is a smart phone. Smart phones are great!";
        // String text = "iPhones iPhone iPhones";
    }

    @Override
    public boolean needsTraining() {
        return true;
    }

    @Override
    public String getExtractorName() {
        return "Palladian Keyphrase Extractor";
    }

}
