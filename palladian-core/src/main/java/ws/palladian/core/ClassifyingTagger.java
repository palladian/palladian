package ws.palladian.core;

import java.util.List;

import ws.palladian.extraction.location.ClassifiedAnnotation;

public interface ClassifyingTagger extends Tagger {

    @Override
    public List<ClassifiedAnnotation> getAnnotations(String text);

}
