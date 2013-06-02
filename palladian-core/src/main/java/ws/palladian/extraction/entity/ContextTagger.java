package ws.palladian.extraction.entity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Tagger;

public abstract class ContextTagger implements Tagger {

    private final Pattern pattern;
    private final String tagName;

    public ContextTagger(Pattern pattern, String tagName) {
        Validate.notNull(pattern, "pattern must not be null");
        Validate.notNull(tagName, "tagName must not be null");
        this.pattern = pattern;
        this.tagName = tagName;
    }

    @Override
    public List<ContextAnnotation> getAnnotations(String text) {
        List<ContextAnnotation> annotations = CollectionHelper.newArrayList();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int startPosition = matcher.start();
            int endPosition = matcher.end();
            String value = matcher.group();
            String leftContext = getLeftContext(text.substring(0, startPosition));
            String rightContext = getRightContext(text.substring(endPosition));
            annotations.add(new ContextAnnotation(startPosition, value, tagName, leftContext, rightContext));
        }
        return annotations;
    }

    protected abstract String getRightContext(String rightString);

    protected abstract String getLeftContext(String leftString);

}