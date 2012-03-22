package ws.palladian.extraction.sentence;

import java.util.List;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.feature.Annotation;
import ws.palladian.extraction.feature.PositionAnnotation;
import ws.palladian.extraction.token.Tokenizer;

public class PalladianSentenceDetector extends AbstractSentenceDetector {

    private static final long serialVersionUID = 2651862330142408510L;

    @Override
    public AbstractSentenceDetector detect(String text) {

        List<String> sentences = Tokenizer.getSentences(text);
        
        Annotation[] sentencesAnnotations = new Annotation[sentences.size()];
        PipelineDocument document = new PipelineDocument(text);
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