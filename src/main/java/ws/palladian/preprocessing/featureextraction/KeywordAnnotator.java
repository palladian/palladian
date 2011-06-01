package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class KeywordAnnotator implements PipelineProcessor {
    
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
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        List<Annotation> tokenList = annotationFeature.getValue();
        
        Map<String, Double> temp = new HashMap<String, Double>();
        
        int docLenght = document.getOriginalContent().length();
        
        for (Annotation annotation : tokenList) {
            
            // if (!(token instanceof AnnotationGroup)){continue;}
            
//            System.err.println(token);
            FeatureVector tokenFeatureVector = annotation.getFeatureVector();
            
//             double tfidf = ((NumericFeature) tokenFeatureVector.get(TfIdfAnnotator.PROVIDED_FEATURE)).getValue();
            double df = ((NumericFeature) tokenFeatureVector.get(FrequencyCalculator.PROVIDED_FEATURE)).getValue();
            double spread = ((NumericFeature) tokenFeatureVector.get(TokenSpreadCalculator.PROVIDED_FEATURE)).getValue();
            double uppercaseScore = StringHelper.startsUppercase(annotation.getValue()) ? 1 : 0.5;
            double positionScore = 1. - (double) annotation.getStartPosition() / docLenght;
            double lengthScore = annotation.getValue().split(" ").length;
            
//            double ranking = tfidf * (spread + 1.0) * /*uppercaseScore * */ positionScore;
//             double ranking = tfidf * (spread + 1.0) * uppercaseScore * positionScore; // * lengthScore; // XXX no length score in german            
            // double ranking = df * (spread + 1.0) * uppercaseScore * positionScore;            
        double ranking = df * /* df **/ (spread + 1.0) * uppercaseScore * positionScore * lengthScore * lengthScore /** lengthScore*/;            
//        double ranking = df * /* df **/ (spread + 1.0) * uppercaseScore * positionScore; // * lengthScore /** lengthScore*/;            
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

}

class Keyword {
    String value;
    double score;
    @Override
    public String toString() {
        return value + " " + score;
    }
}
