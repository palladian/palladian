package ws.palladian.extraction.entity;

import java.util.regex.Pattern;

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
        StringBuilder smileyPatternRegEx = new StringBuilder();
        smileyPatternRegEx.append(Pattern.quote(S_HAPPY)).append("|");
        smileyPatternRegEx.append(Pattern.quote(S_HAPPY2)).append("|");
        smileyPatternRegEx.append(Pattern.quote(S_WINK)).append("|");
        smileyPatternRegEx.append(Pattern.quote(S_WINK2)).append("|");
        smileyPatternRegEx.append(Pattern.quote(S_SAD)).append("|");
        smileyPatternRegEx.append(Pattern.quote(S_SAD2)).append("|");
        smileyPatternRegEx.append(Pattern.quote(S_CRY)).append("|");
        smileyPatternRegEx.append(Pattern.quote(S_CRY2));
        return Pattern.compile(smileyPatternRegEx.toString());
    }

    public SmileyTagger() {
        super(SMILEY_PATTERN, SMILEY_TAG_NAME);
    }

}
