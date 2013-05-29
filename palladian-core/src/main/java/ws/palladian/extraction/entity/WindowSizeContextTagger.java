package ws.palladian.extraction.entity;

import java.util.regex.Pattern;

public class WindowSizeContextTagger extends ContextTagger {

    private final int windowSize;

    public WindowSizeContextTagger(Pattern pattern, String tagName, int windowSize) {
        super(pattern, tagName);
        this.windowSize = windowSize;

    }

    @Override
    protected String getRightContext(String rightString) {
        return rightString.substring(0, Math.min(windowSize, rightString.length()));
    }

    @Override
    protected String getLeftContext(String leftString) {
        return leftString.substring(Math.max(0, leftString.length() - windowSize), leftString.length());
    }

}
