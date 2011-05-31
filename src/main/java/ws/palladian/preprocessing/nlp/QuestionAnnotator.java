package ws.palladian.preprocessing.nlp;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.Feature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.featureextraction.Token;

public final class QuestionAnnotator implements PipelineProcessor {
    
    public final static String FEAUTRE_IDENTIFIER = "ws.palladian.features.question";

    @Override
    public void process(PipelineDocument document) {
        Feature<List<Token>> sentences = (Feature<List<Token>>)document.getFeatureVector().get(AbstractSentenceDetector.FEATURE_IDENTIFIER);
        List<Token> questions = new ArrayList<Token>();
        for (Token sentence : sentences.getValue()) {
            String coveredText = sentence.getValue();
            if (coveredText.endsWith("?") || coveredText.toLowerCase().startsWith("what")
                    || coveredText.toLowerCase().startsWith("who") || coveredText.toLowerCase().startsWith("where")
                    || coveredText.toLowerCase().startsWith("how") || coveredText.toLowerCase().startsWith("why")) {
                
                 questions.add(createQuestion(sentence));
            }
        }
        Feature<List<Token>> questionsFeature = new Feature<List<Token>>(FEAUTRE_IDENTIFIER, questions);
        document.getFeatureVector().add(questionsFeature);
    }
    
    private Token createQuestion(Token sentence) {
        Token ret = new Token(sentence.getDocument(),sentence.getStartPosition(),sentence.getEndPosition());
        return ret;
    }

}
