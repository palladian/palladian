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
import ws.palladian.extraction.feature.MapTermCorpus;
import ws.palladian.extraction.feature.NGramCreator;
import ws.palladian.extraction.feature.RegExTokenRemover;
import ws.palladian.extraction.feature.Stemmer;
import ws.palladian.extraction.feature.Stemmer.Mode;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.feature.TfIdfAnnotator;
import ws.palladian.extraction.feature.TokenMetricsCalculator;
import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PerformanceCheckProcessingPipeline;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

public final class TfidfExtractor extends KeyphraseExtractor {
    
    private final ProcessingPipeline pipeline;
    private final MapTermCorpus termCorpus;
    
    public TfidfExtractor() {
        termCorpus = new MapTermCorpus();

        pipeline = new PerformanceCheckProcessingPipeline();
        pipeline.connectToPreviousProcessor(new RegExTokenizer());
        pipeline.connectToPreviousProcessor(new StopTokenRemover(Language.ENGLISH));
        pipeline.connectToPreviousProcessor(new LengthTokenRemover(4));
        pipeline.connectToPreviousProcessor(new RegExTokenRemover("[^A-Za-z0-9-]+"));
        pipeline.connectToPreviousProcessor(new NGramCreator(3));
        pipeline.connectToPreviousProcessor(new Stemmer(Language.ENGLISH, Mode.MODIFY));
        pipeline.connectToPreviousProcessor(new TokenMetricsCalculator());
        pipeline.connectToPreviousProcessor(new DuplicateTokenRemover());
        pipeline.connectToPreviousProcessor(new IdfAnnotator(termCorpus));
        pipeline.connectToPreviousProcessor(new TfIdfAnnotator());
    }

    @Override
    public boolean needsTraining() {
        return true;
    }

    @Override
    public void train(String inputText, Set<String> keyphrases) {
        TextDocument document = new TextDocument(inputText);
        try {
            pipeline.process(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException(e);
        }
        List<PositionAnnotation> annotations = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);
        Set<String> terms = new HashSet<String>();
        for (PositionAnnotation annotation : annotations) {
            // FeatureVector featureVector = annotation.getFeatureVector();
            //String value = featureVector.get(StemmerAnnotator.STEM).getValue();
            String value = annotation.getValue();
            terms.add(value);
        }
        termCorpus.addTermsFromDocument(terms);
    }
    
    @Override
    public void endTraining() {
        System.out.println(pipeline);
    }
    
    @Override
    public void reset() {
        termCorpus.clear();
        super.reset();
    }

    @Override
    public List<Keyphrase> extract(String inputText) {
        TextDocument document = new TextDocument(inputText);
        try {
            pipeline.process(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException();
        }
        return extract(document);
    }

    private List<Keyphrase> extract(PipelineDocument<String> document) {
        List<Keyphrase> ret = new ArrayList<Keyphrase>();
        List<PositionAnnotation> annotations = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);
        List<Pair<String, Double>> keywords = new ArrayList<Pair<String,Double>>();
        for (PositionAnnotation annotation : annotations) {
            String value = annotation.getValue();
            double tfidf = annotation.getFeatureVector().get(NumericFeature.class, TfIdfAnnotator.TFIDF).getValue();
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
