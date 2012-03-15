package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.ProcessingPipeline;
import ws.palladian.preprocessing.nlp.tokenization.RegExTokenizer;
import ws.palladian.preprocessing.nlp.tokenization.Tokenizer;

public class KeywordAnnotator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDED_FEATURE = "ws.palladian.extraction.keywords";

    private final int numKeywords;

    public KeywordAnnotator() {
        this(10);
    }

    public KeywordAnnotator(int numKeywords) {
        this.numKeywords = numKeywords;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature)featureVector.get(Tokenizer.PROVIDED_FEATURE);
        List<Annotation> tokenList = annotationFeature.getValue();

        Map<String, Double> temp = new HashMap<String, Double>();

        int docLenght = document.getOriginalContent().length();

        for (Annotation annotation : tokenList) {
            FeatureVector tokenFeatureVector = annotation.getFeatureVector();

            // double tfidf = ((NumericFeature) tokenFeatureVector.get(TfIdfAnnotator.PROVIDED_FEATURE)).getValue();
            double df = ((NumericFeature)tokenFeatureVector.get(FrequencyCalculator.PROVIDED_FEATURE)).getValue();
            double spread = ((NumericFeature)tokenFeatureVector.get(TokenSpreadCalculator.PROVIDED_FEATURE)).getValue();
            double uppercaseScore = StringHelper.startsUppercase(annotation.getValue()) ? 1 : 0.5;
            double positionScore = 1. - (double)annotation.getStartPosition() / docLenght;
            // double lengthScore = annotation.getValue().split(" ").length;

            double ranking = df * (spread + 1.0) * uppercaseScore * positionScore;
            temp.put(annotation.getValue(), ranking);
        }

        temp = CollectionHelper.sortByValue(temp, CollectionHelper.DESCENDING);
        List<Keyword> keywordList = new ArrayList<Keyword>();
        for (Entry<String, Double> entry : temp.entrySet()) {
            if (keywordList.size() == numKeywords) {
                break;
            }
            Keyword keyword = new Keyword();
            keyword.score = entry.getValue();
            keyword.value = entry.getKey();
            keywordList.add(keyword);
        }
        Feature<List<Keyword>> keywordFeature = new Feature<List<Keyword>>(PROVIDED_FEATURE, keywordList);
        document.getFeatureVector().add(keywordFeature);
    }
    
    
    public static void main(String[] args) {
        
        
        
        
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new NGramCreator(4));
        pipeline.add(new StopTokenRemover(Language.ENGLISH));
        pipeline.add(new FrequencyCalculator());
        pipeline.add(new TokenSpreadCalculator());
        
        pipeline.add(new KeywordAnnotator());
        
        String originalContent = "Compatibility of systems of linear constraints over the set of natural numbers Criteria of compatibility of a system of linear Diophantine equations, strict inequations, and nonstrict inequations are considered. Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given. These criteria and the corresponding algorithms for constructing a minimal supporting set of solutions can be used in solving all the considered types of systems and systems of mixed types";
        PipelineDocument result = pipeline.process(new PipelineDocument(originalContent));
        
        Feature<List<Keyword>> feature = (Feature<List<Keyword>>)result.getFeatureVector().get(PROVIDED_FEATURE);
        List<Keyword> keywords = feature.getValue();
        for (Keyword keyword : keywords) {
            System.out.println(keyword);
        }
    }

}

class Keyword {
    String value;
    double score;

    @Override
    public String toString() {
        return value + " " + score;
    }
}
