package ws.palladian.semantics;

/**
 * A spelling corrected text.
 *
 * @author David Urbansky
 * @since 22-Mar-22 at 10:23
 **/
public class CorrectedText {
    private String correctedText;

    /** Whether a correction has taken place. */
    private boolean corrected;

    /** Whether all the words were known and therefore didn't have to be corrected. */
    private boolean allWordsKnown;

    public String getCorrectedText() {
        return correctedText;
    }

    public void setCorrectedText(String correctedText) {
        this.correctedText = correctedText;
    }

    public boolean isCorrected() {
        return corrected;
    }

    public void setCorrected(boolean corrected) {
        this.corrected = corrected;
    }

    /** Whether the result is probably correct. That doesn't mean it is corrected, all the words could have been in the dictionary so corrected=false but we believe the text is correct. */
    public boolean isConfidentResult() {
        return corrected || allWordsKnown;
    }

    public boolean isAllWordsKnown() {
        return allWordsKnown;
    }

    public void setAllWordsKnown(boolean allWordsKnown) {
        this.allWordsKnown = allWordsKnown;
    }
}
