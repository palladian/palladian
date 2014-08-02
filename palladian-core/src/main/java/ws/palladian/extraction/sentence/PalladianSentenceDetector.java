package ws.palladian.extraction.sentence;

import java.util.List;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.constants.Language;

public final class PalladianSentenceDetector extends AbstractSentenceDetector {

    @Override
    public List<Annotation> getAnnotations(String text) {
        return Tokenizer.getAnnotatedSentences(text, Language.ENGLISH);
    }

}