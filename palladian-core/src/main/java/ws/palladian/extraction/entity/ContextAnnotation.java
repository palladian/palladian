package ws.palladian.extraction.entity;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotated;

public class ContextAnnotation extends NerAnnotation {

    /** The left context of the annotation */
    private final String leftContext;

    /** The right context of the annotation */
    private final String rightContext;

    public ContextAnnotation(int offset, String entityName, String tagName, String leftContext, String rightContext) {
        super(offset, entityName, tagName);
        this.leftContext = leftContext;
        this.rightContext = rightContext;
    }

    public ContextAnnotation(int offset, String entityName, String tagName) {
        this(offset, entityName, tagName, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    public ContextAnnotation(Annotated annotated) {
        this(annotated.getStartPosition(), annotated.getValue(), annotated.getTag(), StringUtils.EMPTY,
                StringUtils.EMPTY);
    }

    public String getLeftContext() {
        return leftContext;
    }

    public String getRightContext() {
        return rightContext;
    }

    public String[] getLeftContexts() {

        String[] contexts = new String[3];
        contexts[0] = "";
        contexts[1] = "";
        contexts[2] = "";

        String leftContext = getLeftContext();
        String[] words = leftContext.split(" ");
        int wordNumber = 1;
        for (int i = words.length - 1; i >= 0; i--) {

            String token = words[i];
            /*
             * if (DateHelper.containsDate(token)) {
             * token = "DATE";
             * } else
             */if (StringHelper.isNumber(token) || StringHelper.isNumericExpression(token)) {
                token = "NUM";
            }

            if (wordNumber == 1) {
                contexts[0] = token;
                contexts[1] = token;
                contexts[2] = token;
            }

            if (wordNumber == 2) {
                contexts[1] = token + " " + contexts[1];
                contexts[2] = token + " " + contexts[2];
            }

            if (wordNumber == 3) {
                contexts[2] = token + " " + contexts[2];
                break;
            }

            wordNumber++;
        }

        if (words.length < 3) {
            contexts[2] = "";
        }
        if (words.length < 2) {
            contexts[1] = "";
        }

        return contexts;
    }

    public String[] getRightContexts() {

        String[] contexts = new String[3];
        contexts[0] = "";
        contexts[1] = "";
        contexts[2] = "";

        String rightContext = getRightContext();
        String[] words = rightContext.split(" ");
        int wordNumber = 1;
        for (String word : words) {

            String token = word;
            /*
             * if (DateHelper.containsDate(token)) {
             * token = "DATE";
             * } else
             */if (StringHelper.isNumber(token) || StringHelper.isNumericExpression(token)) {
                token = "NUM";
            }

            if (wordNumber == 1) {
                contexts[0] = token;
                contexts[1] = token;
                contexts[2] = token;
            }

            if (wordNumber == 2) {
                contexts[1] = contexts[1] + " " + token;
                contexts[2] = contexts[2] + " " + token;
            }

            if (wordNumber == 3) {
                contexts[2] = contexts[2] + " " + token;
                break;
            }

            wordNumber++;
        }

        if (words.length < 3) {
            contexts[2] = "";
        }
        if (words.length < 2) {
            contexts[1] = "";
        }

        return contexts;
    }

}
