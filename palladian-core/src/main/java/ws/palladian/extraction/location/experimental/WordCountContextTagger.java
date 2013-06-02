package ws.palladian.extraction.location.experimental;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.entity.ContextTagger;

public class WordCountContextTagger extends ContextTagger {

    private final int numWords;
    private final Pattern leftPatten;
    private final Pattern rightPattern;

    public WordCountContextTagger(Pattern pattern, String tagName, int numWords) {
        super(pattern, tagName);
        Validate.isTrue(numWords >= 0, "numWords must be greater/equal zero.");
        this.numWords = numWords;
        this.leftPatten = Pattern.compile(String.format("(\\w+[^\\w]{1,5}){0,%s}$", numWords));
        this.rightPattern = Pattern.compile(String.format("^([^\\w]{1,5}\\w+){0,%s}", numWords));
    }

    @Override
    protected String getRightContext(String rightString) {
        Matcher matcher = rightPattern.matcher(rightString);
        if (matcher.find()) {
            return matcher.group();
        }
        return StringUtils.EMPTY;
    }

    @Override
    protected String getLeftContext(String leftString) {
        Matcher matcher = leftPatten.matcher(leftString);
        if (matcher.find()) {
            return matcher.group();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WordCountContextTagger [numWords=");
        builder.append(numWords);
        builder.append("]");
        return builder.toString();
    }

}
