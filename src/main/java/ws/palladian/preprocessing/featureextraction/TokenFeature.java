package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.Feature;
import ws.palladian.preprocessing.PipelineDocument;

public class TokenFeature extends Feature<List<Token>> {

    private PipelineDocument document;

    public TokenFeature(String name, PipelineDocument document) {
        super(name, new ArrayList<Token>());
        this.document = document;
    }

    public void addToken(Token token) {
        getValue().add(token);
        token.setDocument(getDocument());
    }

    /**
     * @return the document
     */
    public PipelineDocument getDocument() {
        return document;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(PipelineDocument document) {
        this.document = document;
    }

    public String toStringList() {
        StringBuilder sb = new StringBuilder();
        List<Token> tokens = getValue();
        for (Token token : tokens) {
            sb.append(token).append("\n");
        }
        return sb.toString();
    }
    
    public List<Token> getTokens(int startPosition, int endPosition) {
        List<Token> result = new ArrayList<Token>();
        for (Token current : getValue()) {
            if (current.getStartPosition() >= startPosition && current.getEndPosition() <= endPosition) {
                result.add(current);
            }
        }
        return result;
    }

}
