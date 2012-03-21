package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.FeatureVector;

public abstract class TokenRemover implements PipelineProcessor {

    public TokenRemover() {
        super();
    }

    protected abstract boolean remove(Annotation annotation);

    @Override
    public final void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new RuntimeException("required feature is missing");
        }
        List<Annotation> annotations = annotationFeature.getValue();
        
        // create a new List, as removing many items from an existing one is terribly expensive
        // (unless we were using a LinkedList, what we do not want)
        List<Annotation> resultTokens = new ArrayList<Annotation>();
        for (Iterator<Annotation> tokenIterator = annotations.iterator(); tokenIterator.hasNext();) {
            Annotation annotation = tokenIterator.next();
            if (!remove(annotation)) {
                resultTokens.add(annotation);
            }
        }
        annotationFeature.setValue(resultTokens);
        
//        for (Iterator<Annotation> tokenIterator = tokens.iterator(); tokenIterator.hasNext();) {
//            Annotation token = tokenIterator.next();
//            String tokenValue = token.getValue();
//            if (remove(tokenValue)) {
//                tokenIterator.remove();
//            }
//        }
    }

}