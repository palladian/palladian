package ws.palladian.extraction.entity;

import java.util.List;
import java.util.regex.Matcher;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * Tag URLs in a text.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class UrlTagger implements Tagger {

    /** The tag name for URLs. */
    public static final String URI_TAG_NAME = "URI";

    @Override
    public List<Annotated> getAnnotations(String text) {
        List<Annotated> annotations = CollectionHelper.newArrayList();

        Matcher matcher = UrlHelper.URL_PATTERN.matcher(text);

        while (matcher.find()) {
            annotations.add(new Annotation(matcher.start(), matcher.group(0), URI_TAG_NAME));
        }

        return annotations;
    }

}