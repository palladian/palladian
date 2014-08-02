package ws.palladian.extraction.entity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Tagger;
import ws.palladian.helper.collection.CollectionHelper;

public class RegExTagger implements Tagger {

    private final Pattern pattern;
    private final String tagName;

    public RegExTagger(Pattern pattern, String tagName) {
        this.pattern = pattern;
        this.tagName = tagName;
    }

    @Override
    public final List<Annotation> getAnnotations(String text) {
        List<Annotation> annotations = CollectionHelper.newArrayList();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            annotations.add(new ImmutableAnnotation(matcher.start(), matcher.group(), tagName));
        }
        return annotations;
    }

}
