package ws.palladian.extraction.entity;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AbstractAnnotation;

public class ContextAnnotation extends AbstractAnnotation {

    /** The category of the instance, null if not classified. */
    private CategoryEntriesMap tags = new CategoryEntriesMap();

    /** The start index of the annotation in the annotated text. */
    private int offset;

    /** The annotated entity. */
    private String value;

    /** The left context of the annotation */
    private final String leftContext;

    /** The right context of the annotation */
    private final String rightContext;

    public ContextAnnotation(int offset, String value, String tag, String leftContext, String rightContext) {
        this.offset = offset;
        this.value = value;
        tags.set(tag, 1);
        this.leftContext = leftContext;
        this.rightContext = rightContext;
    }

    public ContextAnnotation(int offset, String entityName, String tagName) {
        this(offset, entityName, tagName, EMPTY, EMPTY);
    }

    public ContextAnnotation(Annotation annotation) {
        this(annotation.getStartPosition(), annotation.getValue(), annotation.getTag(), EMPTY, EMPTY);
    }

    @Override
    public int getStartPosition() {
        return offset;
    }

    public void setStartPosition(int offset) {
        this.offset = offset;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getTag() {
        return tags.getMostLikelyCategory();
    }

    public CategoryEntries getTags() {
        return tags;
    }

    public void setTags(CategoryEntries tags) {
        this.tags = new CategoryEntriesMap(tags);
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
