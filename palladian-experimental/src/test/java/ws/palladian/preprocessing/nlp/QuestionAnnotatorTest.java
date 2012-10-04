package ws.palladian.preprocessing.nlp;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.extraction.sentence.LingPipeSentenceDetector;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.Feature;

public class QuestionAnnotatorTest {
    private String fixture = "Who is the nicest question without question mark. The last was! Or was it? How about no.";

    @Test
    public void testQuestionDetection() throws Exception {
        QuestionAnnotator objectOfClassUnderTest = new QuestionAnnotator();
        AbstractSentenceDetector sentenceDetector = new LingPipeSentenceDetector();
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(sentenceDetector);
        pipeline.add(objectOfClassUnderTest);
        
        PipelineDocument<String> document = pipeline.process(new PipelineDocument<String>(fixture));
        @SuppressWarnings("unchecked")
        Feature<List<Annotation<String>>> questions = document.getFeatureVector().getFeature(AnnotationFeature.class, QuestionAnnotator.FEATURE_IDENTIFIER);
        for(Annotation<String> question:questions.getValue()) {
            System.out.println(question);
        }
    }
}
