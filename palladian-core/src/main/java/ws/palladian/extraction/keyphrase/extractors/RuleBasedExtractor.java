package ws.palladian.extraction.keyphrase.extractors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.extraction.feature.DuplicateTokenRemover;
import ws.palladian.extraction.feature.HtmlCleaner;
import ws.palladian.extraction.feature.IdfAnnotator;
import ws.palladian.extraction.feature.LengthTokenRemover;
import ws.palladian.extraction.feature.NGramCreator;
import ws.palladian.extraction.feature.RegExTokenRemover;
import ws.palladian.extraction.feature.StemmerAnnotator;
import ws.palladian.extraction.feature.StemmerAnnotator.Mode;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.feature.TermCorpus;
import ws.palladian.extraction.feature.TfIdfAnnotator;
import ws.palladian.extraction.feature.TokenMetricsCalculator;
import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.extraction.keyphrase.features.AdditionalFeatureExtractor;
import ws.palladian.extraction.keyphrase.temp.CooccurrenceMatrix;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PerformanceCheckProcessingPipeline;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.BasicFeatureVectorImpl;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;
public final class RuleBasedExtractor extends KeyphraseExtractor {
    
    private final ProcessingPipeline trainingPipeline;
    private final ProcessingPipeline extractionPipeline;
    private final TermCorpus termCorpus;
    private final TermCorpus keyphraseCorpus;
    private final CooccurrenceMatrix<String> cooccurrenceMatrix;
    private StemmerAnnotator stemmer;

    public RuleBasedExtractor() {
        termCorpus = new TermCorpus();
        keyphraseCorpus = new TermCorpus();
        cooccurrenceMatrix = new CooccurrenceMatrix<String>();

        trainingPipeline = new PerformanceCheckProcessingPipeline();
        trainingPipeline.add(new HtmlCleaner());
        trainingPipeline.add(new RegExTokenizer());
        trainingPipeline.add(new StopTokenRemover(Language.ENGLISH));
        trainingPipeline.add(new LengthTokenRemover(4));
        trainingPipeline.add(new RegExTokenRemover("[^A-Za-z0-9-]+"));
        stemmer = new StemmerAnnotator(Language.ENGLISH, Mode.MODIFY);
        trainingPipeline.add(stemmer);
        trainingPipeline.add(new DuplicateTokenRemover());

        // extractionPipeline has the same steps as trainingPipeline,
        // plus idf and tf-idf annotation
        extractionPipeline = new ProcessingPipeline();
        extractionPipeline.add(new HtmlCleaner());
        extractionPipeline.add(new RegExTokenizer());
        extractionPipeline.add(new StopTokenRemover(Language.ENGLISH));
        extractionPipeline.add(new LengthTokenRemover(4));
        extractionPipeline.add(new RegExTokenRemover("[^A-Za-z0-9-]+"));
        extractionPipeline.add(stemmer);
        extractionPipeline.add(new NGramCreator(3, StemmerAnnotator.UNSTEM));
        extractionPipeline.add(new TokenMetricsCalculator());
        extractionPipeline.add(new DuplicateTokenRemover());
        extractionPipeline.add(new IdfAnnotator(termCorpus));
        extractionPipeline.add(new TfIdfAnnotator());
//        extractionPipeline.add(new PhrasenessAnnotator());
        extractionPipeline.add(new AdditionalFeatureExtractor());
    }

    @Override
    public boolean needsTraining() {
        return true;
    }

    @Override
    public void train(String inputText, Set<String> keyphrases) {
        TextDocument document = new TextDocument(inputText);
        try {
            trainingPipeline.process(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException(e);
        }
        List<PositionAnnotation> annotations = document.get(ListFeature.class, BaseTokenizer.PROVIDED_FEATURE);
        Set<String> terms = new HashSet<String>();
        for (PositionAnnotation annotation : annotations) {
            terms.add(annotation.getValue());
        }
        termCorpus.addTermsFromDocument(terms);
        keyphraseCorpus.addTermsFromDocument(stem(keyphrases));
        cooccurrenceMatrix.addAll(stem(keyphrases));
    }

    private Set<String> stem(Set<String> items) {
        Set<String> ret = new HashSet<String>();
        for (String item : items) {
            List<String> value = new ArrayList<String>();
            for (String token : item.split("\\s")) {
                value.add(stemmer.stem(token).toLowerCase());
            }
            ret.add(StringUtils.join(value, " "));
        }
        return ret;
    }

    @Override
    public void endTraining() {
        System.out.println(trainingPipeline);
        System.out.println(keyphraseCorpus);
    }

    @Override
    public void reset() {
        termCorpus.reset();
        keyphraseCorpus.reset();
        cooccurrenceMatrix.reset();
        super.reset();
    }

    @Override
    public List<Keyphrase> extract(String inputText) {
        TextDocument document = new TextDocument(inputText);
        try {
            extractionPipeline.process(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException(e);
        }
        return extract(document);
    }

    private List<Keyphrase> extract(PipelineDocument<String> document) {
        List<PositionAnnotation> annotations =  document.get(ListFeature.class, BaseTokenizer.PROVIDED_FEATURE);
        List<Keyphrase> keywords = new ArrayList<Keyphrase>();
        for (PositionAnnotation annotation : annotations) {
            String value = annotation.getValue();
            BasicFeatureVectorImpl annotationFeatureVector = annotation.getFeatureVector();
            double frequency = annotationFeatureVector.get(NumericFeature.class, TokenMetricsCalculator.FREQUENCY).getValue();
//            double phraseness = annotationFeatureVector.get(PhrasenessAnnotator.GENERALIZED_DICE).getValue();
            double prior = (double)(keyphraseCorpus.getCount(value) + 1) / keyphraseCorpus.getNumTerms();
            double posPenalty = annotationFeatureVector.get(NumericFeature.class, TokenMetricsCalculator.FIRST).getValue() > 0.1 ? 0 : 1;
            double spreadPenalty = annotationFeatureVector.get(NumericFeature.class, TokenMetricsCalculator.SPREAD).getValue() < 0.25 ? 0 : 1;
            double termLength = value.split(" ").length;
            double idf;
//            if (annotation instanceof AnnotationGroup) {
//                idf = Math.log10(termCorpus.getNumDocs());
//            } else {
                idf = annotationFeatureVector.get(NumericFeature.class, IdfAnnotator.PROVIDED_FEATURE).getValue();
//            }
            double score = frequency * idf /* phraseness */ * prior * posPenalty * spreadPenalty * Math.pow(termLength, 2);
            keywords.add(new Keyphrase(value, score));

        }
        
        // improves f1 on citeulike180, degrades on semeval
        // synthetesize(keywords);
        
        // improves f1 on semeval
        // reRankOverlaps(keywords);
        
        // improves f1 on semeval
        // reRankCooccurrences(keywords);
        Collections.sort(keywords);
        
        
        if (keywords.size() > getKeyphraseCount()) {
            keywords.subList(getKeyphraseCount(), keywords.size()).clear();
        }
        return keywords;
    }

    private int synthetesize(List<Keyphrase> keywords) {
        Collections.sort(keywords);
        Set<String> keyValues = new HashSet<String>();
        for (String string : keyValues) {
            keyValues.add(string);
        }
        Map<String,Keyphrase> synthetesized = CollectionHelper.newHashMap();
        int subSize = (int) Math.sqrt(keywords.size());
        for (Keyphrase keyphrase : keywords.subList(0, subSize)) {
            List<Pair<String,Double>> highestPairs = cooccurrenceMatrix.getHighest(keyphrase.getValue(), 5);
            for (Pair<String, Double> pair : highestPairs) {
                
            String value = pair.getLeft();
            Double weight = pair.getRight() * 1;
            if (keyValues.contains(value)) {
                continue;
            }
            if (weight < 0.01) {
                continue;
            }
            if (cooccurrenceMatrix.getCount(keyphrase.getValue(), value) < 2) {
                continue;
            }
            Keyphrase synthetesizedKeyphrase;
            if (synthetesized.containsKey(value)) {
                synthetesizedKeyphrase= synthetesized.get(value);
                synthetesizedKeyphrase.setWeight(synthetesizedKeyphrase.getWeight() + keyphrase.getWeight() * weight);
            } else {
                synthetesizedKeyphrase=new Keyphrase(value);
                synthetesizedKeyphrase.setWeight(keyphrase.getWeight() * weight);
                synthetesized.put(value, synthetesizedKeyphrase);
            }
            }
        }
        keywords.addAll(synthetesized.values());
        return synthetesized.size();
    }

    /**
     * <p>
     * Re-calculate the weight of the list of {@link Keyphrase}s, based on their overlap. Reduce the weights of those
     * candidates which are contained in another candidate. E.g. list contains <code>web</code> with weight
     * <code>0.5</code> and <code>web browser</code> with weight <code>0.7</code>, then weight of <code>web</code> if
     * re-calculated to <code>0.7 - 0.5 = 0.2</code>.
     * </p>
     * 
     * @param keywords
     */
    private void reRankOverlaps(List<Keyphrase> keywords) {
        for (Keyphrase keyphrase1 : keywords) {
            if (keyphrase1.getWeight() > 0) {
                for (Keyphrase keyphrase2 : keywords) {
                    if (keyphrase1.getValue().equals(keyphrase2.getValue())) {
                        continue;
                    }
                    if (keyphrase1.getValue().contains(keyphrase2.getValue())) {
                        keyphrase2.setWeight(keyphrase2.getWeight() - keyphrase1.getWeight());
                    }
                }
            }
        }
    }
    
    private void reRankCooccurrences(List<Keyphrase> keywords) {
        
        int size = keywords.size();
        int x = (int) Math.sqrt(size);
        Collections.sort(keywords);

        
        for (Keyphrase k1 : keywords.subList(0, x)) {
            String value1 = k1.getValue();
            for (Keyphrase k2 : keywords.subList(0, x)) {
                String value2 = k2.getValue();
                if (value1.equals(value2)) {
                    continue;
                }
                double condProb = cooccurrenceMatrix.getConditionalProbabilityLaplace(value2, value1) * 10;
                double newWeight = k1.getWeight() + k2.getWeight() * condProb;
                k1.setWeight(newWeight);
            }
        }
    }

    @Override
    public String getExtractorName() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getExtractorName();
    }
}
