package ws.palladian.extraction.entity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Tagger;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;

public class RegExTagger implements Tagger {

    private final Pattern pattern;
    private final String tagName;

    public RegExTagger(String pattern, String tagName) {
        this(Pattern.compile(pattern), tagName);
    }

    public RegExTagger(Pattern pattern, String tagName) {
        Validate.notNull(pattern, "pattern must not be null");
        Validate.notNull(tagName, "tagName must not be null");
        this.pattern = pattern;
        this.tagName = tagName;
    }

    @Override
    public final List<Annotation> getAnnotations(String text) {
        List<Annotation> annotations = CollectionHelper.newArrayList();
        String cleanText = StringHelper.replaceProtectedSpace(text);
        Matcher matcher = pattern.matcher(cleanText);
        while (matcher.find()) {
            annotations.add(new ImmutableAnnotation(matcher.start(), matcher.group(), tagName));
        }
        return annotations;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegExTagger [pattern=");
        builder.append(pattern);
        builder.append(", tagName=");
        builder.append(tagName);
        builder.append("]");
        return builder.toString();
    }

}
