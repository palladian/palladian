package ws.palladian.extraction.keyphrase;


public class Token {
    
    private String stemmedValue;
    private String unstemmedValue;
    private int textPosition;
    private int sentencePosition;
    private int sentenceNumber;
    private String posTag;
    
    public Token() {
    }
    public String getStemmedValue() {
        return stemmedValue;
    }
    public void setStemmedValue(String stemmedValue) {
        this.stemmedValue = stemmedValue;
    }
    public String getUnstemmedValue() {
        return unstemmedValue;
    }
    public void setUnstemmedValue(String unstemmedValue) {
        this.unstemmedValue = unstemmedValue;
    }
    public int getTextPosition() {
        return textPosition;
    }
    public void setTextPosition(int textPosition) {
        this.textPosition = textPosition;
    }
    public int getSentencePosition() {
        return sentencePosition;
    }
    public void setSentencePosition(int sentencePosition) {
        this.sentencePosition = sentencePosition;
    }
    public int getSentenceNumber() {
        return sentenceNumber;
    }
    public void setSentenceNumber(int sentenceNumber) {
        this.sentenceNumber = sentenceNumber;
    }
    public String getPosTag() {
        return posTag;
    }
    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Token [unstemmedValues=");
        builder.append(unstemmedValue);
        builder.append(", stemmedValue=");
        builder.append(stemmedValue);
        builder.append(", textPosition=");
        builder.append(textPosition);
        // builder.append(", sentencePosition=");
        // builder.append(sentencePosition);
        // builder.append(", sentenceNumber=");
        // builder.append(sentenceNumber);
        builder.append(", posTag=");
        builder.append(posTag);
        builder.append("]");
        return builder.toString();
    }
    
}
