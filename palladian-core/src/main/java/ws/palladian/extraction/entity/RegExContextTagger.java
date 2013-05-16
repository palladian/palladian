package ws.palladian.extraction.entity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Tagger;

public class RegExContextTagger implements Tagger {

    private final Pattern pattern;
    private final String tagName;
    private final int windowSize;

    public RegExContextTagger(Pattern pattern, String tagName, int windowSize) {
        this.pattern = pattern;
        this.tagName = tagName;
        this.windowSize = windowSize;
    }

    @Override
    public List<ContextAnnotation> getAnnotations(String text) {

        List<ContextAnnotation> annotations = CollectionHelper.newArrayList();

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int startPosition = matcher.start();
            int endPosition = matcher.end();
            String value = matcher.group();
            String leftContext = text.substring(Math.max(0, startPosition - windowSize), startPosition);
            String rightContext = text.substring(endPosition, Math.min(endPosition + windowSize, text.length()));
            annotations.add(new ContextAnnotation(startPosition, value, tagName, leftContext, rightContext));
        }

        return annotations;
    }

}
