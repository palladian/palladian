package ws.palladian.retrieval.wiki;

import java.util.SortedMap;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * A parser for MediaWiki text, which can interpret bold and italic formatting. Because of the beloved apostrophe junge,
 * this task is more complicated than it sounds. See <a
 * href="http://www.mediawiki.org/wiki/Markup_spec/BNF/Inline_text">here</a> for an explanation of the syntax, which
 * also served as foundation for this implementation.
 * 
 * @author pk
 */
public final class MediaWikiFormattingParser {

    /** Apostrophe character. */
    private static final char APOSTROPHE = '\'';
    /** Length of italic toggle. */
    private static final int ITALIC_LENGTH = 2;
    /** Length of bold toggle. */
    private static final int BOLD_LENGTH = 3;
    /** Length of bold-italic toggle. */
    private static final int BOLD_ITALIC_LENGTH = 5;

    /**
     * Callback, which is triggered during parsing.
     * 
     * @author pk
     */
    public static interface ParserCallback {

        void character(char ch);

        void bold();

        void italic();

        void boldItalic();

    }

    public static class ParserAdapter implements ParserCallback {

        @Override
        public void character(char ch) {
        }

        @Override
        public void bold() {
        }

        @Override
        public void italic() {
        }

        @Override
        public void boldItalic() {
        }

    }

    /** The current index in the text. */
    private int parseIdx = 0;

    /** Pre-parsed map with apostrophes (index, numberOfApostrophes). */
    private final SortedMap<Integer, Integer> apostropheMap;

    /** The callback to be triggered. */
    private final ParserCallback callback;

    /** Character array of the text. */
    private final char[] chars;

    /**
     * Parse the given text and produce callbacks in the specified interface.
     * 
     * @param text The text to parse.
     * @param callback Instance of the callback interface.
     */
    public static final void parse(String text, ParserCallback callback) {
        Validate.notNull(text, "text must not be null");
        Validate.notNull(callback, "callback must not be null");
        new MediaWikiFormattingParser(text, callback);
    }

    /** Invoked through {@link #parse(String, ParserCallback)} method. */
    private MediaWikiFormattingParser(String text, ParserCallback callback) {
        this.apostropheMap = createApostropheMap(text);
        this.callback = callback;
        this.chars = text.toCharArray();
        for (; parseIdx < chars.length;) {
            Integer apostrophes = apostropheMap.get(parseIdx);
            if (apostrophes != null) {
                processApostrophes(apostrophes);
            } else if (chars[parseIdx] != APOSTROPHE) {
                parsedCharacter(chars[parseIdx]);
            }
        }
    }

    private void processApostrophes(int count) {
        Validate.isTrue(count > 0, "count must be greater zero");
        switch (count) {
            case 1:
                parsedApostrophe();
                break;
            case 2:
                parsedItalic();
                break;
            case 3:
                if (unbalanced() && parseIdx >= 2) {
                    // did we have three previously?
                    if (earlierBold(parseIdx)) {
                        parsedBold();
                    } else if (chars[parseIdx - 1] != ' ' && chars[parseIdx - 2] == ' ' || //
                            chars[parseIdx - 1] != ' ' && chars[parseIdx - 2] != ' ' || //
                            chars[parseIdx - 1] == ' ') { //
                        parsedApostrophe();
                        parsedItalic();
                    } else {
                        parsedBold();
                    }
                } else {
                    parsedBold();
                }
                break;
            case 4:
                boolean wouldBalance = (boldCount() + 1) % 2 == 0 || italicCount() % 2 == 0;
                if (wouldBalance) {
                    parsedApostrophe();
                    parsedBold();
                } else {
                    parsedApostrophe();
                    parsedApostrophe();
                    parsedItalic();
                }
                break;
            case 5:
                parsedBoldItalic();
                break;
            default: // more than five
                for (int i = 0; i < count - 5; i++) {
                    parsedApostrophe();
                }
                parsedBoldItalic();
                break;
        }
    }

    private void parsedItalic() {
        callback.italic();
        apostropheMap.put(parseIdx, ITALIC_LENGTH);
        parseIdx += ITALIC_LENGTH;
    }

    private void parsedBold() {
        callback.bold();
        apostropheMap.put(parseIdx, BOLD_LENGTH);
        parseIdx += BOLD_LENGTH;
    }

    private void parsedBoldItalic() {
        callback.boldItalic();
        apostropheMap.put(parseIdx, BOLD_ITALIC_LENGTH);
        parseIdx += BOLD_ITALIC_LENGTH;
    }

    private void parsedApostrophe() {
        callback.character(APOSTROPHE);
        apostropheMap.put(parseIdx, 1);
        parseIdx++;
    }

    private void parsedCharacter(char ch) {
        callback.character(ch);
        parseIdx++;
    }

    private boolean earlierBold(int index) {
        boolean earlierSequence = false;
        for (int k = 0; k < index; k++) {
            Integer temp = apostropheMap.get(k);
            if (temp != null && temp == BOLD_LENGTH) {
                earlierSequence = true;
                break;
            }
        }
        return earlierSequence;
    }

    /**
     * Create a map with apostrophe sequences in the given text.
     *
     * @param text The text, not <code>null</code>.
     * @return A map with character index as key, number of following apostrophes as values.
     */
    private static SortedMap<Integer, Integer> createApostropheMap(String text) {
        SortedMap<Integer, Integer> apostropheMap = CollectionHelper.newTreeMap();
        char[] chars = text.toCharArray();
        int numApostrophes = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == APOSTROPHE) {
                numApostrophes++;
            } else if (numApostrophes > 0) {
                apostropheMap.put(i - numApostrophes, numApostrophes);
                numApostrophes = 0;
            }
        }
        if (numApostrophes > 0) {
            apostropheMap.put(chars.length - numApostrophes, numApostrophes);
        }
        return apostropheMap;
    }

    /**
     * @return <code>true</code> if bold or italic toggles are unbalanced.
     */
    private boolean unbalanced() {
        return italicCount() % 2 == 1 && boldCount() % 2 == 1;
    }

    private int boldCount() {
        int boldCount = 0;
        for (Integer numApostrophes : apostropheMap.values()) {
            if (numApostrophes == BOLD_LENGTH || numApostrophes == BOLD_ITALIC_LENGTH) {
                boldCount++;
            }
        }
        return boldCount;
    }

    private int italicCount() {
        int italicCount = 0;
        for (Integer numApostrophes : apostropheMap.values()) {
            if (numApostrophes == ITALIC_LENGTH || numApostrophes == BOLD_ITALIC_LENGTH) {
                italicCount++;
            }
        }
        return italicCount;
    }

}
