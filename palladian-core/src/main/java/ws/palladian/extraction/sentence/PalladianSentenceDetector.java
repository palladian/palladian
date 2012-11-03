package ws.palladian.extraction.sentence;

import java.util.List;

import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.PositionAnnotation;

public class PalladianSentenceDetector extends AbstractSentenceDetector {

    @Override
    public AbstractSentenceDetector detect(String text) {

        TextDocument document = new TextDocument(text);
        List<PositionAnnotation> sentences = Tokenizer.getSentences(document);

        PositionAnnotation[] sentencesArray = new PositionAnnotation[sentences.size()];
        setSentences(sentences.toArray(sentencesArray));

        return this;
    }

}