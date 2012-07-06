package ws.palladian.extraction.sentence;

import java.util.List;

import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.PositionAnnotation;

public class PalladianSentenceDetector extends AbstractSentenceDetector {

    private static final long serialVersionUID = 2651862330142408510L;

    @Override
    public AbstractSentenceDetector detect(String text) {

        List<String> sentences = Tokenizer.getSentences(text);
        
        @SuppressWarnings("unchecked")
        Annotation<String>[] sentencesAnnotations = new Annotation[sentences.size()];
        PipelineDocument<String> document = new PipelineDocument<String>(text);
        int i = 0;
        int lastOffset = -1;
        for (String sentence : sentences) {
            int startPosition = text.indexOf(sentence, lastOffset);
            sentencesAnnotations[i++] = new PositionAnnotation(document, startPosition, startPosition + sentence.length());
            lastOffset = startPosition + sentence.length();
        }
        
        setSentences(sentencesAnnotations);
        
        return null;
    }
    
}