package ws.palladian.extraction.keyphrase.extractors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.extraction.feature.DuplicateTokenRemover;
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
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PerformanceCheckProcessingPipeline;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.TextAnnotationFeature;

public final class TfidfExtractor extends KeyphraseExtractor {
    
    private final ProcessingPipeline trainingPipeline;
    private final ProcessingPipeline extractionPipeline;
    private final TermCorpus termCorpus;
    
    public TfidfExtractor() {
        termCorpus = new TermCorpus();

        // trainingPipeline = new ProcessingPipeline();
        trainingPipeline = new PerformanceCheckProcessingPipeline();
        trainingPipeline.add(new RegExTokenizer());
        trainingPipeline.add(new StopTokenRemover(Language.ENGLISH));
        trainingPipeline.add(new LengthTokenRemover(4));
        trainingPipeline.add(new RegExTokenRemover("[^A-Za-z0-9-]+"));
        trainingPipeline.add(new NGramCreator(3));
        trainingPipeline.add(new StemmerAnnotator(Language.ENGLISH, Mode.MODIFY));
        trainingPipeline.add(new TokenMetricsCalculator());
        trainingPipeline.add(new DuplicateTokenRemover());

        // extractionPipeline has the same steps as trainingPipeline,
        // plus idf and tf-idf annotation
        extractionPipeline = new ProcessingPipeline(trainingPipeline);
        extractionPipeline.add(new IdfAnnotator(termCorpus));
        extractionPipeline.add(new TfIdfAnnotator());
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
        TextAnnotationFeature feature = document.getFeatureVector().get(RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> annotations = feature.getValue();
        Set<String> terms = new HashSet<String>();
        for (Annotation<String> annotation : annotations) {
            // FeatureVector featureVector = annotation.getFeatureVector();
            //String value = featureVector.get(StemmerAnnotator.STEM).getValue();
            String value = annotation.getValue();
            terms.add(value);
        }
        termCorpus.addTermsFromDocument(terms);
    }
    
    @Override
    public void endTraining() {
        System.out.println(trainingPipeline);
    }
    
    @Override
    public void reset() {
        termCorpus.reset();
        super.reset();
    }

    @Override
    public List<Keyphrase> extract(String inputText) {
        TextDocument document = new TextDocument(inputText);
        try {
            extractionPipeline.process(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException();
        }
        return extract(document);
    }

    private List<Keyphrase> extract(PipelineDocument<String> document) {
        List<Keyphrase> ret = new ArrayList<Keyphrase>();
        TextAnnotationFeature feature = document.getFeatureVector().get(RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> annotations = feature.getValue();
        List<Pair<String, Double>> keywords = new ArrayList<Pair<String,Double>>();
        for (Annotation<String> annotation : annotations) {
            String value = annotation.getValue();
            double tfidf = annotation.getFeature(TfIdfAnnotator.PROVIDED_FEATURE_DESCRIPTOR).getValue();
            keywords.add(new ImmutablePair<String, Double>(value, tfidf));
        }
        Collections.sort(keywords, new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> p1, Pair<String, Double> p2) {
                return p2.getRight().compareTo(p1.getRight());
            }
        });
        for (Pair<String, Double> keyword : keywords) {
            ret.add(new Keyphrase(keyword.getLeft(), keyword.getRight()));
            if (ret.size() >= getKeyphraseCount()) {
                break;
            }
        }
        return ret;
    }

    @Override
    public String getExtractorName() {
        return "TfIdfExtractor";
    }

}
