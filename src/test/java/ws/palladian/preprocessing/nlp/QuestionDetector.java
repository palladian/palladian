package ws.palladian.preprocessing.nlp;

import java.util.List;

import org.junit.Test;

import ws.palladian.model.features.Feature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.ProcessingPipeline;
import ws.palladian.preprocessing.featureextraction.Token;

public class QuestionDetector {
    private String fixture = "Who is the nicest question without question mark. The last was! Or was it? How about no.";

    @Test
    public void testQuestionDetection() throws Exception {
        QuestionAnnotator objectOfClassUnderTest = new QuestionAnnotator();
        AbstractSentenceDetector sentenceDetector = new LingPipeSentenceDetector();
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(sentenceDetector);
        pipeline.add(objectOfClassUnderTest);
        
        PipelineDocument document = pipeline.process(new PipelineDocument(fixture));
        Feature<List<Token>> questions = (Feature<List<Token>>)document.getFeatureVector().get(QuestionAnnotator.FEAUTRE_IDENTIFIER);
        for(Token question:questions.getValue()) {
            System.out.println(question);
        }
    }
}
