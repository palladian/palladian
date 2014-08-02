package ws.palladian.extraction;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Tagger;
import ws.palladian.extraction.entity.Annotations;

/**
 * Annotates a text based on a given dictionary.
 * @author pk
 *
 */
public class DictionaryTagger implements Tagger {

    private final Set<String> dictionary;

    public DictionaryTagger(Set<String> dictionary) {
        Validate.notNull(dictionary, "dictionary must not be null");
        this.dictionary = dictionary;
    }

    @Override
    public List<? extends Annotation> getAnnotations(String text) {
        Annotations<Annotation> annotations = new Annotations<Annotation>();
        for (String dictionaryString : dictionary) {
            String patternString = "(?<!\\w)" + Pattern.quote(dictionaryString) + "(?!\\w)";
            Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                annotations.add(new ImmutableAnnotation(matcher.start(), matcher.group()));
            }
        }
        annotations.removeNested();
        return annotations;
    }

}
