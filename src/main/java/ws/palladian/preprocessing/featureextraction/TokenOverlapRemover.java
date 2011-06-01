package ws.palladian.preprocessing.featureextraction;

import java.util.Arrays;
import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.ProcessingPipeline;

public class TokenOverlapRemover implements PipelineProcessor {

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException("required feature is missing");
        }
        List<Annotation> annotations = annotationFeature.getValue();
        Annotation[] tokensArray = annotations.toArray(new Annotation[annotations.size()]);
        for (int i = 0; i < tokensArray.length; i++) {
            for (int j = i + 1; j < tokensArray.length; j++) {
                Annotation token1 = tokensArray[i];
                Annotation token2 = tokensArray[j];
                boolean token2overlaps = 
                    token1.getStartPosition() >= token2.getStartPosition() &&
                    token1.getEndPosition() <= token2.getEndPosition();
                if (token2overlaps) {
//                    System.out.println("remove " + token1);
                    annotations.remove(token1);
                }
            }
        }
    }
    
    
    public static void main(String[] args) {
        PipelineDocument d = new PipelineDocument("the quick brown fox");
        
        ProcessingPipeline processingPipeline = new ProcessingPipeline();
        processingPipeline.add(new Tokenizer());
        processingPipeline.add(new NGramCreator(5));
        processingPipeline.add(new ControlledVocabularyFilter(Arrays.asList("quick", "brown", "brown fox")));
        processingPipeline.add(new TokenOverlapRemover());
        
        processingPipeline.process(d);
        AnnotationFeature annotationFeature = (AnnotationFeature) d.getFeatureVector().get(Tokenizer.PROVIDED_FEATURE);
        List<Annotation> annotations = annotationFeature.getValue();
        
        CollectionHelper.print(annotations);
    }

}
