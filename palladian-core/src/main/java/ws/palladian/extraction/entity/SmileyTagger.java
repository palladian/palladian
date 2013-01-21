package ws.palladian.extraction.entity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Tag Smileys in a text.
 * </p>
 * 
 * @author David Urbansky
 * @see http://factoryjoe.com/projects/emoticons/
 * @see http://bscw.rediris.es/pub/bscw.cgi/d3323568/impactoemoticones.pdf
 */
public class SmileyTagger {

    /** The tag name for smileys. */
    public static final String SMILEY_TAG_NAME = "SMILEY";

    private static final String S_HAPPY = ":)";
    private static final String S_HAPPY2 = ":-)";
    private static final String S_WINK = ";)";
    private static final String S_WINK2 = ";-)";
    private static final String S_SAD = ":(";
    private static final String S_SAD2 = ":-(";
    private static final String S_CRY = ";(";
    private static final String S_CRY2 = ";-(";

    private final Pattern smileyPattern;

    public SmileyTagger() {
        StringBuilder smileyPatterhRegEx = new StringBuilder();
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_HAPPY)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_HAPPY2)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_WINK)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_WINK2)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_SAD)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_SAD2)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_CRY)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_CRY2));
        smileyPattern = Pattern.compile(smileyPatterhRegEx.toString());
    }

    public List<Annotation> tagSmileys(String inputText) {

        List<Annotation> annotations = CollectionHelper.newArrayList();

        Matcher matcher = smileyPattern.matcher(inputText);

        while (matcher.find()) {
            Annotation annotation = new Annotation(matcher.start(), matcher.group(0), SMILEY_TAG_NAME);
            annotations.add(annotation);
        }

        return annotations;
    }

    public static void main(String[] args) {
        String text = "This is a nice day :) and the sun shines ;)";
        SmileyTagger smileyTagger = new SmileyTagger();
        List<Annotation> annotations = smileyTagger.tagSmileys(text);
        CollectionHelper.print(annotations);
    }
}
