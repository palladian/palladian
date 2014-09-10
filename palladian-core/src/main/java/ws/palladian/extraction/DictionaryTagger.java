package ws.palladian.extraction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Tagger;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * Annotates a text based on a given dictionary.
 * 
 * @author pk
 */
public class DictionaryTagger implements Tagger {

    private final Map<String, String> dictionary;

    private final boolean caseSensitive;

    public DictionaryTagger(Set<String> dictionary) {
        this(dictionary, StringUtils.EMPTY);
    }

    public DictionaryTagger(Set<String> dictionary, String tagName) {
        Validate.notNull(dictionary, "dictionary must not be null");
        Validate.notNull(tagName, "tagName must not be empty");
        this.dictionary = CollectionHelper.newLinkedHashMap();
        for (String entry : dictionary) {
            this.dictionary.put(entry, tagName);
        }
        this.caseSensitive = false;
    }

    public DictionaryTagger(Map<String, String> dictionary, boolean caseSensitive) {
        Validate.notNull(dictionary, "dictionary must not be null");
        this.dictionary = new LinkedHashMap<String, String>(dictionary);
        this.caseSensitive = caseSensitive;
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        Annotations<Annotation> annotations = new Annotations<Annotation>();
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        for (Entry<String, String> dictionaryEntry : dictionary.entrySet()) {
            String dictionaryString = dictionaryEntry.getKey();
            String tagName = dictionaryEntry.getValue();
            String patternString = "(?<!\\w)" + Pattern.quote(dictionaryString) + "(?!\\w)";
            Pattern pattern = Pattern.compile(patternString, flags);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                Annotation annotation = new ImmutableAnnotation(matcher.start(), matcher.group(), tagName);
                annotations.add(annotation);
            }
        }
        annotations.removeNested();
        return annotations;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DictionaryTagger [dictionary=");
        builder.append(dictionary);
        if (caseSensitive) {
            builder.append(", caseSensitive");
        }
        builder.append("]");
        return builder.toString();
    }

}
