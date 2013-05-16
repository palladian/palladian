package ws.palladian.extraction.entity;

import java.util.regex.Pattern;

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
public class SmileyTagger extends RegExTagger {

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

    private static final Pattern SMILEY_PATTERN = createPattern();

    private static final Pattern createPattern() {
        StringBuilder smileyPatterhRegEx = new StringBuilder();
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_HAPPY)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_HAPPY2)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_WINK)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_WINK2)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_SAD)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_SAD2)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_CRY)).append("|");
        smileyPatterhRegEx.append(StringHelper.escapeForRegularExpression(S_CRY2));
        return Pattern.compile(smileyPatterhRegEx.toString());
    }

    public SmileyTagger() {
        super(SMILEY_PATTERN, SMILEY_TAG_NAME);
    }

}
