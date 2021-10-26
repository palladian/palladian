package ws.palladian.extraction.entity;

import java.util.regex.Pattern;

/**
 * <p>
 * Tag Brackets and Parentheses.
 * </p>
 *
 * @author David Urbansky
 * 26.10.2021
 */
public class BracketTagger extends RegExTagger {
    public static final String BRACKET_TAG_NAME = "BRACKET";

    private static final Pattern BRACKET_PATTERN = Pattern.compile("[(\\[{][^})\\]]+[\\]})]");

    public static BracketTagger INSTANCE = new BracketTagger();

    private BracketTagger() {
        super(BRACKET_PATTERN, BRACKET_TAG_NAME);
    }
}
