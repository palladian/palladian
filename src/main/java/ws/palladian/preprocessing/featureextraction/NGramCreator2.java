package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class NGramCreator2 implements PipelineProcessor {
    private static final long serialVersionUID = 1L;
    private final int minLength;
    private final int maxLength;

    public NGramCreator2() {
        this(2, 2);
    }

    public NGramCreator2(int maxLength) {
        this(2, maxLength);
    }

    public NGramCreator2(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> annotations = annotationFeature.getValue();
        List<AnnotationGroup> gramTokens = new ArrayList<AnnotationGroup>();
        for (int i = minLength; i <= maxLength; i++) {
            List<AnnotationGroup> nGramTokens = createNGrams(document, annotations, i);
            gramTokens.addAll(nGramTokens);
        }
        annotations.addAll(gramTokens);
    }

    private List<AnnotationGroup> createNGrams(PipelineDocument document, List<Annotation> annotations, int length) {
        List<AnnotationGroup> gramTokens = new ArrayList<AnnotationGroup>();
        Annotation[] tokensArray = annotations.toArray(new Annotation[annotations.size()]);
        for (int i = 0; i < tokensArray.length - length + 1; i++) {
            AnnotationGroup gramToken = new AnnotationGroup(document);
            for (int j = i; j < i + length; j++) {
                gramToken.add(tokensArray[j]);
            }
            gramTokens.add(gramToken);
        }
        return gramTokens;
    }

}