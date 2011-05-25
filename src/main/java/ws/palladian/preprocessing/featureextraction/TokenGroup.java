package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.preprocessing.PipelineDocument;

public class TokenGroup extends Token {

    private static final String TOKEN_SEPARATOR = " ";
    private List<Token> tokens = new ArrayList<Token>();

    public TokenGroup(PipelineDocument document) {
        super(document);
        setStartPosition(-1);
        setEndPosition(-1);
    }

    public void add(Token token) {
        tokens.add(token);
        if (getStartPosition() == -1) {
            setStartPosition(token.getStartPosition());
        }
        setEndPosition(token.getEndPosition());
        
    }

    public List<Token> getTokens() {
        return tokens;
    }

    @Override
    public String getValue() {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append(token.getValue()).append(TOKEN_SEPARATOR);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TokenGroup [getTokens()=");
        builder.append(getTokens());
        builder.append(", getStartPosition()=");
        builder.append(getStartPosition());
        builder.append(", getEndPosition()=");
        builder.append(getEndPosition());
        builder.append(", getValue()=");
        builder.append(getValue());
        builder.append(", getFeatureVector()=");
        builder.append(getFeatureVector());
        builder.append("]");
        return builder.toString();
    }
    
    

}