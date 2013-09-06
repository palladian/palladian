package ws.palladian.extraction.entity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.Annotation;

public class RegExTagger implements Tagger {

    private final Pattern pattern;
    private final String tagName;

    public RegExTagger(Pattern pattern, String tagName) {
        this.pattern = pattern;
        this.tagName = tagName;
    }

    @Override
    public final List<Annotated> getAnnotations(String text) {
        List<Annotated> annotations = CollectionHelper.newArrayList();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            annotations.add(new Annotation(matcher.start(), matcher.group(), tagName));
        }
        return annotations;
    }

}
