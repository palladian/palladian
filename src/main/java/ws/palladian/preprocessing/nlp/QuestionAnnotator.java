package ws.palladian.preprocessing.nlp;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.Feature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.featureextraction.Annotation;
import ws.palladian.preprocessing.featureextraction.PositionAnnotation;

public final class QuestionAnnotator implements PipelineProcessor {
    
    public final static String FEAUTRE_IDENTIFIER = "ws.palladian.features.question";

    @Override
    public void process(PipelineDocument document) {
        Feature<List<Annotation>> sentences = (Feature<List<Annotation>>)document.getFeatureVector().get(AbstractSentenceDetector.FEATURE_IDENTIFIER);
        List<Annotation> questions = new ArrayList<Annotation>();
        for (Annotation sentence : sentences.getValue()) {
            String coveredText = sentence.getValue();
            if (coveredText.endsWith("?") || coveredText.toLowerCase().startsWith("what")
                    || coveredText.toLowerCase().startsWith("who") || coveredText.toLowerCase().startsWith("where")
                    || coveredText.toLowerCase().startsWith("how") || coveredText.toLowerCase().startsWith("why")) {
                
                 questions.add(createQuestion(sentence));
            }
        }
        Feature<List<Annotation>> questionsFeature = new Feature<List<Annotation>>(FEAUTRE_IDENTIFIER, questions);
        document.getFeatureVector().add(questionsFeature);
    }
    
    private Annotation createQuestion(Annotation sentence) {
        Annotation ret = new PositionAnnotation(sentence.getDocument(),sentence.getStartPosition(),sentence.getEndPosition());
        return ret;
    }

}
