package ws.palladian.core;

import ws.palladian.extraction.location.ClassifiedAnnotation;

import java.util.List;

public interface ClassifyingTagger extends Tagger {
    @Override
    List<ClassifiedAnnotation> getAnnotations(String text);
}
