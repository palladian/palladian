package ws.palladian.extraction.sentence;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.processing.features.Annotation;

public final class PalladianSentenceDetector extends AbstractSentenceDetector {

    @Override
    public List<Annotation> getAnnotations(String text) {
        return Tokenizer.getSentences(text, StringUtils.EMPTY);
    }

}